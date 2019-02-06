/**
 * Copyright (C) 2001-2019 by RapidMiner and the contributors
 * 
 * Complete list of developers available at our web site:
 * 
 * http://rapidminer.com
 * 
 * This program is free software: you can redistribute it and/or modify it under the terms of the
 * GNU Affero General Public License as published by the Free Software Foundation, either version 3
 * of the License, or (at your option) any later version.
 * 
 * This program is distributed in the hope that it will be useful, but WITHOUT ANY WARRANTY; without
 * even the implied warranty of MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the GNU
 * Affero General Public License for more details.
 * 
 * You should have received a copy of the GNU Affero General Public License along with this program.
 * If not, see http://www.gnu.org/licenses/.
*/
package com.rapidminer.tools;

import com.rapidminer.RapidMiner;

import java.io.IOException;
import java.io.OutputStream;
import java.util.logging.FileHandler;
import java.util.logging.Level;
import java.util.logging.Logger;
import java.util.logging.SimpleFormatter;


/**
 * <p>
 * Utility class providing static methods for logging.<br>
 * Parameters read from the XML process configuration file:
 * </p>
 * <ul>
 * <li>logfile (filename or "stdout" or "stderr")</li>
 * <li>logverbosity (possible values are in {@link LogService#LOG_VERBOSITY_NAMES}</li>
 * </ul>
 * 
 * <p>
 * Beside the <b>local</b> log service associated with a concrete process and which will be
 * automatically initialized during the setup phase, one <b>global</b> log service exist which is
 * used for generic log messages not bound to the operators used in a process. This global log
 * service is usually initialized to log messages on system out (at least during the basic
 * initialization phase of RapidMiner). After the basic intialization phase, the global messages
 * will be presented in the message viewer (if the RapidMiner GUI is used) or still printed to
 * system out or in any other stream defined via the method
 * {@link #initGlobalLogging(OutputStream, int)}. Alternatively, one could also define an
 * environment variable named {@link RapidMiner#PROPERTY_RAPIDMINER_GLOBAL_LOG_FILE}.
 * </p>
 * 
 * <p>
 * Usually, operators should only use the log verbosities MINIMUM for messages with a low priority
 * and STATUS for normal information messages. In rare cases, the verbosity level NOTE could be used
 * for operators stating some message more important then STATUS (hence the user should see the
 * message for the default log verbosity level of INIT) but not as important then WARNING. The
 * verbosity levels WARNING, EXCEPTION, and ERROR should be used in error cases. All other log
 * verbosity levels should only be used by internal RapidMiner classes and not by user written
 * operators.
 * </p>
 * 
 * <p>
 * We recommend to set the parameter for the log verbosity level to INIT for the process design
 * phase (eventually STATUS for debugging) and to the log verbosity level WARNING in the production
 * phase. This way it is ensured that not too many logging messages are produced in the production
 * phase.
 * </p>
 * 
 * <p>
 * Log messages can be formatted by using the following macros:
 * </p>
 * <ul>
 * <li>&quot;$b&quot; and &quot;^b&quot; start and end bold mode respectively</li>
 * <li>&quot;$i&quot; and &quot;^i&quot; start and end italic mode respectively</li>
 * <li>&quot;$m&quot; and &quot;^m&quot; start and end monospace mode respectively</li>
 * <li>&quot;$n&quot; and &quot;^n&quot; start and end note color mode respectively</li>
 * <li>&quot;$w&quot; and &quot;^w&quot; start and end warning color mode respectively</li>
 * <li>&quot;$e&quot; and &quot;^e&quot; start and end error color mode respectively</li>
 * </ul>
 * 
 * @author Ingo Mierswa
 */
public class LogService extends WrapperLoggingHandler {

	// -------------------- Verbosity Level --------------------

	/** Indicates an unknown verbosity level. */
	public static final int UNKNOWN_LEVEL = -1;

	/**
	 * Indicates the lowest log verbosity. Should only be used for very detailed but not necessary
	 * logging.
	 */
	public static final int MINIMUM = 0;

	/**
	 * Indicates log messages concerning in- and output. Should only be used by the class Operator
	 * itself and not by its subclasses.
	 */
	public static final int IO = 1;

	/** The default log verbosity for all logging purposes of operators. */
	public static final int STATUS = 2;

	/**
	 * Only the most important logging messaged should use this log verbosity. Currently used only
	 * by the LogService itself.
	 */
	public static final int INIT = 3;

	/**
	 * Use this log verbosity for logging of important notes, i.e. things less important than
	 * warnings but important enough to see for all not interested in the detailed status messages.
	 */
	public static final int NOTE = 4;

	/** Use this log verbosity for logging of warnings. */
	public static final int WARNING = 5;

	/** Use this log verbosity for logging of errors. */
	public static final int ERROR = 6;

	/**
	 * Use this log verbosity for logging of fatal errors which will stop process running somewhere
	 * in the future.
	 */
	public static final int FATAL = 7;

	/**
	 * Normally this log verbosity should not be used by operators. Messages with this verbosity
	 * will always be displayed.
	 */
	public static final int MAXIMUM = 8;

	/** For switching off logging during testing. */
	public static final int OFF = 9;

	public static final String LOG_VERBOSITY_NAMES[] = { "all", "io", "status", "init", "notes", "warning", "error",
			"fatal", "almost_none", "off" };

	/** The prefix used to indicate the global logger. */
	public static final String GLOBAL_PREFIX = "$gG^g";

	private static final Logger GLOBAL_LOGGER = Logger.getLogger("com.rapidminer",
			"com.rapidminer.resources.i18n.LogMessages");
	private static final LogService GLOBAL_LOGGING = new LogService(GLOBAL_LOGGER);

	// ------ methods for init -------

	private LogService(Logger logger) {
		super(logger);

		// setup a log file if the execution mode can access the filesystem (e.g. not for RA)
		// we want our logfile to look the same regardless of any user settings, so ignore
		// a possible user logging property config file
		if (RapidMiner.getExecutionMode().canAccessFilesystem()) {
			try {
				FileHandler logFileHandler = new FileHandler(FileSystemService.getLogFile().getAbsolutePath(), false);
				logFileHandler.setLevel(Level.ALL);
				logFileHandler.setFormatter(new SimpleFormatter());
				LogService.getRoot().addHandler(logFileHandler);
			} catch (IOException e) {
				LogService.getRoot().log(Level.WARNING, "com.rapidminer.logservice.logfile.failed_to_init", e.getMessage());
			}
		}
	}

	/**
	 * Returns the global logging. If no logging was otherwise create, this method creates the
	 * default standard out log service if no log file was defined in the property
	 * {@link RapidMiner#PROPERTY_RAPIDMINER_GLOBAL_LOG_FILE}. Alternatively, developers can invoke
	 * the method {@link #initGlobalLogging(OutputStream, int)}.
	 * 
	 * @deprecated use {@link #getRoot()} instead
	 */
	@Deprecated
	public static LogService getGlobal() {
		return GLOBAL_LOGGING;
	}

	public static Logger getRoot() {
		return GLOBAL_LOGGER;
	}

	// private void addConsole(Level level) {
	// StreamHandler handler = new ConsoleHandler();
	// handler.setLevel(Level.ALL);
	// getRoot().setLevel(level);
	// getRoot().addHandler(handler);
	// }

	public void setVerbosityLevel(int level) {
		getRoot().setLevel(LEVELS[level]);
	}

	/**
	 * The methods in {@link Logger} do not provide a means to pass an exception AND I18N arguments,
	 * so this method provides a shortcut for this.
	 */
	public static void log(Logger logger, Level level, Throwable exception, String i18NKey, Object... arguments) {
		logger.log(level, I18N.getMessage(logger.getResourceBundle(), i18NKey, arguments), exception);
	}

	/**
	 * @deprecated Use {@link Logger#isLoggable(Level)}
	 */
	@Deprecated
	public boolean isSufficientLogVerbosity(int level) {
		return GLOBAL_LOGGER.isLoggable(LEVELS[level]);
	}

	// -------------------- Methoden zum Protokollieren --------------------

	/**
	 * Writes the message to the output stream if the verbosity level is high enough.
	 * 
	 * @deprecated please do not use this log method any longer, use the method
	 *             {@link #log(String, int)} instead
	 */
	@Deprecated
	public static void logMessage(String message, int verbosityLevel) {
		getGlobal().log(message, verbosityLevel);
	}
}
