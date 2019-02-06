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
package com.rapidminer.gui.tools.logging;

import java.util.List;
import java.util.logging.Level;

import javax.swing.Icon;

import com.rapidminer.gui.tools.ProgressThread;
import com.rapidminer.tools.Observable;
import com.rapidminer.tools.ProgressListener;


/**
 * Interface for a log which encapsulates functionality that is required by the logging GUI. See
 * {@link LogViewer}.
 *
 * <p>
 * <strong>Note:</strong> The name of each model has to be unique to differentiate between log
 * files!
 * </p>
 *
 * @author Sabrina Kirstein, Marco Boeck
 */
public interface LogModel extends Observable<List<LogEntry>> {

	/**
	 * Indicates if a log either <strong>pushes</strong> new entries itself to the GUI (via a
	 * listener which is registered to the model) or if it needs to only perform an update when
	 * requested.
	 *
	 */
	public enum LogMode {
		/**
		 * logs of this type only display new entries in the GUI when an update was requested. See
		 * {@link AbstractPullLogModel} for details.
		 */
		PULL,

		/**
		 * logs of this type display new entries in the GUI as soon as they are added. See
		 * {@link AbstractPushLogModel} for details.
		 */
		PUSH;
	};

	/** the max allowed length of the model name */
	public static final int MAX_NAME_LENGTH = 75;

	/**
	 * Determines if the log is closable, i.e. the user has the option to close the log by clicking
	 * on a close button in the GUI.
	 *
	 * @return if the log is closable
	 */
	public boolean isClosable();

	/**
	 * Returns the name of the log. The name has to be unique, because it is used to differentiate
	 * log files, a time stamp may be a good option. Note that it must not exceed
	 * {@value #MAX_NAME_LENGTH} characters.
	 *
	 * @return the name of the log
	 */
	public String getName();

	/**
	 * Returns the icon of the log. Size must be 16x16 pixel.
	 *
	 * @return the icon displayed next to the name in the list of open logs. Can be
	 *         <code>null</code>!
	 */
	public Icon getIcon();

	/**
	 * Returns an unmodifiable list of the log entries of the model.
	 *
	 * @return
	 */
	public List<LogEntry> getLogEntries();

	/**
	 * Returns the log mode of the model.
	 *
	 * @return the mode of the log model; either {@link LogMode#PULL} or {@link LogMode#PUSH}
	 */
	public LogMode getLogMode();

	/**
	 * Appends log entries to the list of log entries of the model.
	 */
	public void addLogEntries(List<LogEntry> logEntries);

	/**
	 * Clears the log. After calling this, {@link #getLogEntries()} is expected to be empty until
	 * new entries are added via {@link #addLogEntries(List)}.
	 */
	public void clearLog();

	/**
	 * Requests an update of the log entries. This method is called from inside a
	 * {@link ProgressThread}, so time consuming tasks here are perfectly fine. Not required for
	 * {@link LogMode#PUSH} logs.
	 * <p>
	 * After this has been called, {@link #getLogEntries()} is expected to return the latest entries
	 * for {@link LogMode#PULL} logs.
	 * </p>
	 *
	 * @param progress
	 *            used to indicate the progress of the update by calling
	 *            {@link ProgressListener#setCompleted(int)}.
	 *            <p>
	 *            Valid progress values range from <code>0 - 100</code>.
	 *            </p>
	 * @throws LogUpdateException
	 *             if the update fails
	 */
	public void updateEntries(ProgressListener progress) throws LogUpdateException;

	/**
	 * The current log level. Default is {@link Level#INFO}. Be sure to log everything in your
	 * implementation regardless of the log level, as the filtering on the selected log level is
	 * done automatically by the {@link LogViewer}.
	 *
	 * @return the log level
	 */
	public Level getLogLevel();

	/**
	 * Sets the new log level. Will be considered by the {@link LogViewer} so only entries with a
	 * level equal or higher than the specified one are displayed.
	 *
	 * @param level
	 */
	public void setLogLevel(Level level);

}
