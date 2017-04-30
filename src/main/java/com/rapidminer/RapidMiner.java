/**
 * Copyright (C) 2001-2017 by RapidMiner and the contributors
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
package com.rapidminer;

import java.awt.Frame;
import java.awt.Image;
import java.io.BufferedReader;
import java.io.File;
import java.io.FileReader;
import java.io.FileWriter;
import java.io.IOException;
import java.io.PrintWriter;
import java.util.HashSet;
import java.util.LinkedList;
import java.util.List;
import java.util.Scanner;
import java.util.Set;
import java.util.Vector;
import java.util.logging.Level;

import com.rapidminer.core.license.ActionStatisticsLicenseManagerListener;
import com.rapidminer.core.license.ProductConstraintManager;
import com.rapidminer.gui.RapidMinerGUI;
import com.rapidminer.gui.properties.SettingsItems;
import com.rapidminer.gui.renderer.RendererService;
import com.rapidminer.gui.safemode.SafeMode;
import com.rapidminer.gui.tools.SplashScreen;
import com.rapidminer.gui.tools.VersionNumber;
import com.rapidminer.io.process.XMLImporter;
import com.rapidminer.license.AlreadyRegisteredException;
import com.rapidminer.license.InvalidProductException;
import com.rapidminer.license.License;
import com.rapidminer.license.location.LicenseLoadingException;
import com.rapidminer.license.location.LicenseLocation;
import com.rapidminer.license.product.Product;
import com.rapidminer.operator.IOObject;
import com.rapidminer.operator.IOObjectMap;
import com.rapidminer.operator.ProcessRootOperator;
import com.rapidminer.operator.learner.CapabilityProvider;
import com.rapidminer.parameter.ParameterType;
import com.rapidminer.parameter.ParameterTypeBoolean;
import com.rapidminer.parameter.ParameterTypeCategory;
import com.rapidminer.parameter.ParameterTypeDirectory;
import com.rapidminer.parameter.ParameterTypeInt;
import com.rapidminer.parameter.ParameterTypePassword;
import com.rapidminer.parameter.ParameterTypeString;
import com.rapidminer.repository.RepositoryManager;
import com.rapidminer.test.asserter.AsserterFactoryRapidMiner;
import com.rapidminer.test_utils.RapidAssert;
import com.rapidminer.tools.FileSystemService;
import com.rapidminer.tools.GlobalAuthenticator;
import com.rapidminer.tools.I18N;
import com.rapidminer.tools.I18N.SettingsType;
import com.rapidminer.tools.LogService;
import com.rapidminer.tools.OperatorService;
import com.rapidminer.tools.ParameterService;
import com.rapidminer.tools.PlatformUtilities;
import com.rapidminer.tools.ProgressListener;
import com.rapidminer.tools.ProxySettings;
import com.rapidminer.tools.Tools;
import com.rapidminer.tools.WebServiceTools;
import com.rapidminer.tools.XMLException;
import com.rapidminer.tools.XMLSerialization;
import com.rapidminer.tools.cipher.CipherTools;
import com.rapidminer.tools.cipher.KeyGenerationException;
import com.rapidminer.tools.cipher.KeyGeneratorTool;
import com.rapidminer.tools.config.ConfigurationManager;
import com.rapidminer.tools.plugin.Plugin;
import com.rapidminer.tools.usagestats.UsageStatistics;


/**
 * Main program. Entry point for command line programm, GUI and wrappers. Please note that
 * applications which use RapidMiner as a data mining library will have to invoke one of the init
 * methods provided by this class before applying processes or operators. Several init methods exist
 * and choosing the correct one with optimal parameters might drastically reduce runtime and / or
 * initialization time.
 *
 * @author Ingo Mierswa, Adrian Wilke
 */
public class RapidMiner {

	public static final String SYSTEM_ENCODING_NAME = "SYSTEM";

	/** URL prefix of URLs that can be opened with RapidMiner. */
	public static final String RAPIDMINER_URL_PREFIX = "rapidminer://";

	public static enum ExitMode {
		NORMAL, ERROR, RELAUNCH
	}

	private static final int RELAUNCH_EXIT_CODE = 2;

	/** Indicates how RapidMiner is being executed. */
	public enum ExecutionMode {
		/** It is unknown how RM was invoked. */
		UNKNOWN(true, false, false, true),
		/** RM is executed using {@link RapidMinerCommandLine#main(String[])}. */
		COMMAND_LINE(true, true, false, true),
		/** RM is executed using {@link RapidMinerGUI#main(String[])}. */
		UI(false, true, true, true),
		/** RM is running inside an application server. */
		APPSERVER(true, false, false, false),
		/** RM is running as an applet inside a browser. */
		APPLET(false, true, true, false),
		/** RM is embedded into another program. */
		EMBEDDED_WITH_UI(false, true, false, false),
		/** RM is embedded into another program. */
		EMBEDDED_WITHOUT_UI(true, true, false, false),
		/** RM is embedded into an applet. */
		EMBEDDED_AS_APPLET(false, false, false, false),
		/** RM is running within Java Web Start. */
		WEBSTART(false, true, true, true),
		/** We are executing unit tests. */
		TEST(true, false, false, true);

		private final boolean isHeadless;
		private final boolean canAccessFilesystem;
		private final boolean hasMainFrame;
		private final boolean loadManagedExtensions;

		private ExecutionMode(final boolean isHeadless, final boolean canAccessFilesystem, final boolean hasMainFrame,
				final boolean loadManagedExtensions) {
			this.isHeadless = isHeadless;
			this.canAccessFilesystem = canAccessFilesystem;
			this.hasMainFrame = hasMainFrame;
			this.loadManagedExtensions = loadManagedExtensions;
		}

		public boolean isHeadless() {
			return isHeadless;
		}

		public boolean canAccessFilesystem() {
			return canAccessFilesystem;
		}

		public boolean hasMainFrame() {
			return hasMainFrame;
		}

		public boolean isLoadingManagedExtensions() {
			return loadManagedExtensions;
		}
	}

	private static ExecutionMode executionMode = ExecutionMode.UNKNOWN;
	private static final VersionNumber VERSION = new RapidMinerVersion();
	private static boolean assertersInitialized = false;

	// --- GENERAL PROPERTIES ---

	/**
	 * The name of the property indicating the version of RapidMiner (read only).
	 */
	public static final String PROPERTY_RAPIDMINER_VERSION = "rapidminer.version";

	/**
	 * Enables special features for developers: Validate process action, operator doc editor, etc.
	 */
	public static final String PROPERTY_DEVELOPER_MODE = "rapidminer.developermode";

	/**
	 * The name of the property indicating the path to a additional operator description XML
	 * file(s). If more than one, then the files have to be separated using the File.pathSeparator
	 * character.
	 */
	public static final String PROPERTY_RAPIDMINER_OPERATORS_ADDITIONAL = "rapidminer.operators.additional";

	/**
	 * The name of the property indicating the path to additional ioobjects description XML file(s).
	 * If more than one, then the files have to be separated using the File.pathSeparator character.
	 */
	public static final String PROPERTY_RAPIDMINER_OBJECTS_ADDITIONAL = "rapidminer.objects.additional";

	/**
	 * The name of the property indicating the path to an RC file (settings).
	 */
	public static final String PROPERTY_RAPIDMINER_RC_FILE = "rapidminer.rcfile";

	/**
	 * The name of the property indicating the path to the global logging file.
	 */
	public static final String PROPERTY_RAPIDMINER_GLOBAL_LOG_FILE = "rapidminer.global.logging.file";

	/**
	 * The name of the property indicating the path to the global logging file.
	 */
	public static final String PROPERTY_RAPIDMINER_GLOBAL_LOG_VERBOSITY = "rapidminer.global.logging.verbosity";

	// Webstart properties
	public static final String PROPERTY_HOME_REPOSITORY_URL = "rapidminer.homerepository.url";
	public static final String PROPERTY_HOME_REPOSITORY_USER = "rapidminer.homerepository.user";
	public static final String PROPERTY_HOME_REPOSITORY_PASSWORD = "rapidminer.homerepository.password";

	// --- INIT PROPERTIES ---

	/** A file path to an operator description XML file. */
	public static final String PROPERTY_RAPIDMINER_INIT_OPERATORS = "rapidminer.init.operators";

	public static final String PROPERTY_RAPIDMINER_GENERAL_LOCALE_LANGUAGE = "rapidminer.general.locale.language";
	public static final String PROPERTY_RAPIDMINER_GENERAL_LOCALE_COUNTRY = "rapidminer.general.locale.country";
	public static final String PROPERTY_RAPIDMINER_GENERAL_LOCALE_VARIANT = "rapidminer.general.locale.variant";

	/**
	 * Boolean parameter indicating if the plugins should be initialized at all.
	 */
	public static final String PROPERTY_RAPIDMINER_INIT_PLUGINS = "rapidminer.init.plugins";

	/** A file path to the directory containing the plugin Jar files. */
	public static final String PROPERTY_RAPIDMINER_INIT_PLUGINS_LOCATION = "rapidminer.init.plugins.location";

	// --- OTHER PROPERTIES ---

	/**
	 * The property name for &quot;The number of fraction digits of formatted numbers.&quot;
	 */
	public static final String PROPERTY_RAPIDMINER_GENERAL_FRACTIONDIGITS_NUMBERS = "rapidminer.general.fractiondigits.numbers";

	/**
	 * The property name for &quot;The number of fraction digits of formatted percent values.&quot;
	 */
	public static final String PROPERTY_RAPIDMINER_GENERAL_FRACTIONDIGITS_PERCENT = "rapidminer.general.fractiondigits.percent";

	/**
	 * The name of the property indicating the maximum number of attributes stored for shortened
	 * meta data transformation.
	 */
	public static final String PROPERTY_RAPIDMINER_GENERAL_MAX_META_DATA_ATTRIBUTES = "rapidminer.general.md_attributes_limit";

	/**
	 * The name of the property indicating the maximum number of nominal values to store for meta
	 * data transformation.
	 */
	public static final String PROPERTY_RAPIDMINER_GENERAL_MAX_NOMINAL_VALUES = "rapidminer.general.md_nominal_values_limit";

	/**
	 * The name of the property defining how many lines are read for guessing values types for input
	 * operations without defined value type.
	 */
	public static final String PROPERTY_RAPIDMINER_GENERAL_MAX_TEST_ROWS = "rapidminer.general.max_rows_used_for_guessing";

	/**
	 * The maximum depth a nested "Execute Process" chain can have. If exceeded, the process is
	 * aborted. See {@link Process#getDepth()} for more details.
	 */
	public static final String PROPERTY_RAPIDMINER_GENERAL_MAX_PROCESS_NESTING_DEPTH = "rapidminer.general.max_process_execution_nesting_depth";

	/**
	 * The property name for &quot;Path to external Java editor. %f is replaced by filename and %l
	 * by the linenumber.&quot;
	 */
	public static final String PROPERTY_RAPIDMINER_TOOLS_EDITOR = "rapidminer.tools.editor";

	/**
	 * The property specifying the method to send mails. Either SMTP or sendmail.
	 */
	public static final String PROPERTY_RAPIDMINER_TOOLS_MAIL_METHOD = "rapidminer.tools.mail.method";
	public static final String[] PROPERTY_RAPIDMINER_TOOLS_MAIL_METHOD_VALUES = { "sendmail", "SMTP" };
	public static final int PROPERTY_RAPIDMINER_TOOLS_MAIL_METHOD_SENDMAIL = 0;
	public static final int PROPERTY_RAPIDMINER_TOOLS_MAIL_METHOD_SMTP = 1;

	/**
	 * Property specifying the email address to which mails are sent if no email address is
	 * specified in the {@link ProcessRootOperator}.
	 */
	public static final String PROPERTY_RAPIDMINER_TOOLS_MAIL_DEFAULT_RECIPIENT = "rapidminer.tools.mail.default_recipient";

	/**
	 * The default value of the minimum time a process must run such that it sends a notification
	 * mail upon completion.
	 */
	public static final String PROPERTY_RAPIDMINER_TOOLS_MAIL_DEFAULT_PROCESS_DURATION_FOR_MAIL = "rapidminer.tools.mail.process_duration_for_mail";

	/**
	 * The property name for &quot;Path to sendmail. Used for email notifications.&quot;
	 */
	public static final String PROPERTY_RAPIDMINER_TOOLS_SENDMAIL_COMMAND = "rapidminer.tools.sendmail.command";

	/**
	 * The property name for &quot;The smtp host. Used for email notifications.&quot;
	 */
	public static final String PROPERTY_RAPIDMINER_TOOLS_SMTP_HOST = "rapidminer.tools.smtp.host";

	/**
	 * The property name for &quot;The smtp port. Used for email notifications.&quot;
	 */
	public static final String PROPERTY_RAPIDMINER_TOOLS_SMTP_PORT = "rapidminer.tools.smtp.port";

	/**
	 * The property name for the &quot;SMTP user. Used for email notifications.&quot;
	 */
	public static final String PROPERTY_RAPIDMINER_TOOLS_SMTP_USER = "rapidminer.tools.smtp.user";

	/**
	 * The property name for the &quot;SMTP pssword (is necessary). Used for email
	 * notifications.&quot;
	 */
	public static final String PROPERTY_RAPIDMINER_TOOLS_SMTP_PASSWD = "rapidminer.tools.smtp.passwd";

	/**
	 * If set to true, the query builders and database assistants and query_builders show only
	 * standard tables (no views and system tables).
	 */
	public static final String PROPERTY_RAPIDMINER_TOOLS_DB_ONLY_STANDARD_TABLES = "rapidminer.tools.db.assist.show_only_standard_tables";

	/**
	 * The property name for the JDBC login timeout setting. This is a global setting in the
	 * DriverManager and may be used differently by the JDBC drivers.
	 */
	public static final String PROPERTY_RAPIDMINER_TOOLS_DB_LOGIN_TIMEOUT = "rapidminer.tools.db.login.timeout";
	public static final int DB_LOGIN_TIMEOUT_DEFAULT = 60;

	/**
	 * The property name for &quot;Use unix special characters for logfile highlighting (requires
	 * new RapidMiner instance).&quot;
	 */
	public static final String PROPERTY_RAPIDMINER_GENERAL_LOGFILE_FORMAT = "rapidminer.general.logfile.format";

	/**
	 * The property name for &quot;Indicates if RapidMiner should be used in debug mode (print
	 * exception stacks and shows more technical error messages)&quot;
	 */
	public static final String PROPERTY_RAPIDMINER_GENERAL_DEBUGMODE = "rapidminer.general.debugmode";

	/** The name of the property indicating the default encoding for files. */
	public static final String PROPERTY_RAPIDMINER_GENERAL_DEFAULT_ENCODING = "rapidminer.general.encoding";

	/**
	 * The name of the property indicating the preferred globally used time zone.
	 */
	public static final String PROPERTY_RAPIDMINER_GENERAL_TIME_ZONE = "rapidminer.general.timezone";

	/**
	 * The maximum number of working threads that should be used by processes.
	 */
	public static final String PROPERTY_RAPIDMINER_GENERAL_NUMBER_OF_THREADS = "rapidminer.general.number_of_threads";

	/**
	 * The maximum number of working threads that should be used by processes.
	 */
	public static final String PROPERTY_RAPIDMINER_GENERAL_NUMBER_OF_PROCESSES = "rapidminer.general.number_of_processes";

	/**
	 * The name of the property indicating whether beta features should be activated.
	 */
	public static final String PROPERTY_RAPIDMINER_UPDATE_BETA_FEATURES = "rapidminer.update.beta_features";

	/**
	 * The name of the property indicating whether or not additional permissions should be enabled
	 */
	public static final String PROPERTY_RAPIDMINER_UPDATE_ADDITIONAL_PERMISSIONS = "rapidminer.init.additional_permissions";

	// --- INIT PROPERTIES ---

	/**
	 * this property can be used to limit the maximum amount of memory RM Studio will use (in MB)
	 */
	public static final String PROPERTY_RAPIDMINER_MAX_MEMORY = "maxMemory";

	public static final String PROPERTY_RAPIDMINER_PROXY_MODE = "rapidminer.proxy.mode";
	public static final String PROPERTY_RAPIDMINER_PROXY_EXCLUDE = "rapidminer.proxy.exclude";

	public final static String RAPIDMINER_PROXY_MODE_SYSTEM = "System proxy";
	public final static String RAPIDMINER_PROXY_MODE_DIRECT = "Direct (no proxy)";
	public final static String RAPIDMINER_PROXY_MODE_MANUAL = "Manual proxy configuration";

	public final static String[] RAPIDMINER_PROXY_MODES = { RAPIDMINER_PROXY_MODE_SYSTEM, RAPIDMINER_PROXY_MODE_DIRECT,
			RAPIDMINER_PROXY_MODE_MANUAL };

	/**
	 * These settings are the JVM Params with a prepended rapidminer.proxy
	 */
	public static final String PROPERTY_RAPIDMINER_HTTP_PROXY_HOST = "rapidminer.proxy.http.proxyHost";
	public static final String PROPERTY_RAPIDMINER_HTTP_PROXY_PORT = "rapidminer.proxy.http.proxyPort";
	public static final String PROPERTY_RAPIDMINER_HTTP_PROXY_NON_PROXY_HOSTS = "rapidminer.proxy.http.nonProxyHosts";

	public static final String PROPERTY_RAPIDMINER_HTTPS_PROXY_HOST = "rapidminer.proxy.https.proxyHost";
	public static final String PROPERTY_RAPIDMINER_HTTPS_PROXY_PORT = "rapidminer.proxy.https.proxyPort";

	public static final String PROPERTY_RAPIDMINER_FTP_PROXY_HOST = "rapidminer.proxy.ftp.proxyHost";
	public static final String PROPERTY_RAPIDMINER_FTP_PROXY_PORT = "rapidminer.proxy.ftp.proxyPort";
	public static final String PROPERTY_RAPIDMINER_SOCKS_PROXY_HOST = "rapidminer.proxy.socksProxyHost";
	public static final String PROPERTY_RAPIDMINER_SOCKS_PROXY_PORT = "rapidminer.proxy.socksProxyPort";
	public static final String PROPERTY_RAPIDMINER_SOCKS_VERSION = "rapidminer.proxy.socksProxyVersion";
	public final static String[] RAPIDMINER_SOCKS_VERSIONS = { "Version 4", "Version 5" };

	public static final String PROCESS_FILE_EXTENSION = "rmp";

	/**
	 * This map of {@link IOObject}s is used to remember {@link IOObject}s during the runtime of
	 * RapidMiner Studio (default state to remember {@link IOObject}s of {@link Process}es)
	 */
	private static IOObjectMap ioObjectCache;

	static {
		System.setProperty(PROPERTY_RAPIDMINER_VERSION, RapidMiner.getLongVersion());
		ParameterService.setParameterValue(PROPERTY_RAPIDMINER_VERSION, RapidMiner.getLongVersion());

		parameterTypesDescription = new HashSet<>();

		// set default language to english
		String[] default_language = new String[1];
		default_language[0] = "eng";

		// scan language definitons. skip comments
		Vector<String> languages = new Vector<>();
		Scanner scanLanguageDefs = new Scanner(
				RapidMiner.class.getResourceAsStream("/com/rapidminer/resources/i18n/language_definitions.txt"));
		try {
			while (scanLanguageDefs.hasNextLine()) {
				String nextLine = scanLanguageDefs.nextLine();
				if (!nextLine.contains("#")) {
					languages.add(nextLine);
				}
			}
		} finally {
			scanLanguageDefs.close();
		}

		// if there is less then one language, take default language
		if (languages.size() < 1) {
			registerParameter(
					new ParameterTypeCategory(PROPERTY_RAPIDMINER_GENERAL_LOCALE_LANGUAGE, "", default_language, 0));

		} else {
			// save vector as array
			int idx = 0;
			String[] languageArray = new String[languages.size()];
			for (String lang : languages) {
				languageArray[idx] = lang;
				++idx;
			}
			registerParameter(new ParameterTypeCategory(PROPERTY_RAPIDMINER_GENERAL_LOCALE_LANGUAGE, "", languageArray, 0));
		}

		registerParameter(
				new ParameterTypeInt(PROPERTY_RAPIDMINER_GENERAL_FRACTIONDIGITS_NUMBERS, "", 0, Integer.MAX_VALUE, 3));
		registerParameter(
				new ParameterTypeInt(PROPERTY_RAPIDMINER_GENERAL_FRACTIONDIGITS_PERCENT, "", 0, Integer.MAX_VALUE, 2));
		registerParameter(
				new ParameterTypeInt(PROPERTY_RAPIDMINER_GENERAL_MAX_NOMINAL_VALUES, "", 0, Integer.MAX_VALUE, 100));
		registerParameter(new ParameterTypeInt(PROPERTY_RAPIDMINER_GENERAL_MAX_TEST_ROWS, "", 0, Integer.MAX_VALUE, 100));
		registerParameter(
				new ParameterTypeInt(PROPERTY_RAPIDMINER_GENERAL_MAX_PROCESS_NESTING_DEPTH, "", 0, Integer.MAX_VALUE, 100));
		registerParameter(new ParameterTypeBoolean(PROPERTY_RAPIDMINER_GENERAL_LOGFILE_FORMAT, "", false));
		registerParameter(new ParameterTypeBoolean(PROPERTY_RAPIDMINER_GENERAL_DEBUGMODE, "", false));
		registerParameter(new ParameterTypeString(PROPERTY_RAPIDMINER_GENERAL_DEFAULT_ENCODING, "", SYSTEM_ENCODING_NAME));
		registerParameter(new ParameterTypeCategory(PROPERTY_RAPIDMINER_GENERAL_TIME_ZONE, "", Tools.getAllTimeZones(),
				Tools.SYSTEM_TIME_ZONE));
		registerParameter(
				new ParameterTypeBoolean(CapabilityProvider.PROPERTY_RAPIDMINER_GENERAL_CAPABILITIES_WARN, "", false));
		registerParameter(new ParameterTypeInt(PROPERTY_RAPIDMINER_GENERAL_NUMBER_OF_THREADS, "", 0, Integer.MAX_VALUE, 0));
		registerParameter(
				new ParameterTypeInt(PROPERTY_RAPIDMINER_GENERAL_NUMBER_OF_PROCESSES, "", 0, Integer.MAX_VALUE, 0));
		registerParameter(new ParameterTypeString(PROPERTY_RAPIDMINER_TOOLS_EDITOR, "", true));
		registerParameter(new ParameterTypeCategory(PROPERTY_RAPIDMINER_TOOLS_MAIL_METHOD, "",
				PROPERTY_RAPIDMINER_TOOLS_MAIL_METHOD_VALUES, PROPERTY_RAPIDMINER_TOOLS_MAIL_METHOD_SMTP));
		registerParameter(new ParameterTypeString(PROPERTY_RAPIDMINER_TOOLS_MAIL_DEFAULT_RECIPIENT, "", true));
		registerParameter(new ParameterTypeInt(PROPERTY_RAPIDMINER_TOOLS_MAIL_DEFAULT_PROCESS_DURATION_FOR_MAIL, "", 0,
				Integer.MAX_VALUE, 30));

		registerParameter(new ParameterTypeString(PROPERTY_RAPIDMINER_TOOLS_SENDMAIL_COMMAND, "", true));
		registerParameter(new ParameterTypeString(PROPERTY_RAPIDMINER_TOOLS_SMTP_HOST, "", true));
		registerParameter(new ParameterTypeString(PROPERTY_RAPIDMINER_TOOLS_SMTP_PORT, "", true));
		registerParameter(new ParameterTypeString(PROPERTY_RAPIDMINER_TOOLS_SMTP_USER, "", true));
		registerParameter(new ParameterTypePassword(PROPERTY_RAPIDMINER_TOOLS_SMTP_PASSWD, ""));

		registerParameter(new ParameterTypeBoolean(PROPERTY_RAPIDMINER_TOOLS_DB_ONLY_STANDARD_TABLES, "", true));
		registerParameter(new ParameterTypeInt(PROPERTY_RAPIDMINER_TOOLS_DB_LOGIN_TIMEOUT, "", 0, Integer.MAX_VALUE,
				DB_LOGIN_TIMEOUT_DEFAULT));

		registerParameter(new ParameterTypeBoolean(PROPERTY_RAPIDMINER_INIT_PLUGINS, "", true));
		registerParameter(new ParameterTypeDirectory(PROPERTY_RAPIDMINER_INIT_PLUGINS_LOCATION, "", true));

		// System parameter types
		registerParameter(new ParameterTypeInt(PROPERTY_RAPIDMINER_MAX_MEMORY, "", 384, Integer.MAX_VALUE, true), "system");

		registerParameter(new ParameterTypeInt(WebServiceTools.WEB_SERVICE_TIMEOUT, "", 1, Integer.MAX_VALUE, 20000),
				"system");
		registerParameter(new ParameterTypeBoolean(RapidMiner.PROPERTY_RAPIDMINER_UPDATE_BETA_FEATURES, "", false));
		registerParameter(new ParameterTypeBoolean(RapidMiner.PROPERTY_RAPIDMINER_UPDATE_ADDITIONAL_PERMISSIONS, "", false));

		// initialize the state of IOObjects
		ioObjectCache = new IOObjectMap();
	}

	private static InputHandler inputHandler = new ConsoleInputHandler();

	private static SplashScreen splashScreen;

	private static final List<Runnable> shutdownHooks = new LinkedList<>();

	private static final List<Runnable> startupHooks = new LinkedList<>();

	private static boolean isInitiated = false;

	private static final Set<ParameterType> parameterTypesDescription;

	private static boolean performedInitialSettings = false;

	public static String getShortVersion() {
		return VERSION.getShortVersion();
	}

	public static String getLongVersion() {
		return VERSION.getLongVersion();
	}

	public static VersionNumber getVersion() {
		return VERSION;
	}

	/**
	 * Returns the global IOObject cache which is used by operators to remember and recall IOObjects
	 * in simulated app sessions
	 *
	 * @return the global IOObject cache
	 */
	public static IOObjectMap getGlobalIOObjectCache() {
		return ioObjectCache;
	}

	/**
	 * @deprecated Use {@link #readProcessFile(File)} instead
	 */
	@Deprecated
	public static Process readExperimentFile(final File experimentfile)
			throws XMLException, IOException, InstantiationException, IllegalAccessException {
		return readProcessFile(experimentfile);
	}

	public static Process readProcessFile(final File processFile)
			throws XMLException, IOException, InstantiationException, IllegalAccessException {
		return readProcessFile(processFile, null);
	}

	public static Process readProcessFile(final File processFile, final ProgressListener progressListener)
			throws XMLException, IOException, InstantiationException, IllegalAccessException {
		try {
			LogService.getRoot().log(Level.FINE, "com.rapidminer.RapidMiner.reading_process_file", processFile);
			if (!processFile.exists() || !processFile.canRead()) {
				LogService.getRoot().log(Level.SEVERE, "com.rapidminer.RapidMiner.reading_process_definition_file_error",
						processFile);
			}
			return new Process(processFile, progressListener);
		} catch (XMLException e) {
			throw new XMLException(processFile.getName() + ":" + e.getMessage(), e);
		}
	}

	/**
	 * Initializes RapidMiner with the default {@link RMProduct} and default {@link LicenseLocation}
	 * pointing to the RapidMiner user-folder. During initialization, the following system
	 * properties are evaluated. All can be specified in one of the RapidMiner configuration files,
	 * by using {@link System#setProperty(String, String)}, or by passing a <code>-Dkey=value</code>
	 * to the Java executable.
	 *
	 * <ul>
	 * <li>rapidminer.init.operators (file path)</li>
	 * <li>rapidminer.init.plugins (true or false)</li>
	 * <li>rapidminer.init.plugins.location (directory path)</li>
	 * <li>rapidminer.init.weka (true or false)</li>
	 * <li>rapidminer.init.jdbc.lib (true or false)</li>
	 * <li>rapidminer.init.jdbc.lib.location (directory path)</li>
	 * <li>rapidminer.init.jdbc.classpath (true or false)</li>
	 * </ul>
	 */
	public static void init() {
		init(null, null);
	}

	/**
	 * Same as {@link #init()} but allows to specify a {@link Product} and a {@link LicenseLocation}
	 * . The provided {@link Product} needs to contain all constraints defined in
	 * {@link RMConstraint}. <br/>
	 * <br/>
	 * If product is <code>null</code> the default product from {@link ProductConstraintManager}
	 * will be used. If {@link LicenseLocation} is <code>null</code> the default
	 * {@link LicenseLocation} from {@link ProductConstraintManager} will be used.
	 */
	public static void init(final Product product, final LicenseLocation licenseLocation) {

		RapidMiner.splashMessage("init_i18n");
		I18N.getErrorBundle();

		// ensure rapidminer.home is set
		RapidMiner.splashMessage("rm_home");
		PlatformUtilities.ensureRapidMinerHomeSet(Level.INFO);

		RapidMiner.splashMessage("init_parameter_service");
		// check if this version is started for the first time
		performInitialSettings();
		ParameterService.init();

		// initializing networking tools
		GlobalAuthenticator.init();
		initializeProxy();

		// do initial license check
		RapidMiner.splashMessage("license_check");

		// initialize product constraint manager
		try {
			if (!ProductConstraintManager.INSTANCE.isInitialized()) {
				ProductConstraintManager.INSTANCE.initialize(licenseLocation, product);
			}
		} catch (IllegalAccessException | AlreadyRegisteredException | LicenseLoadingException | InvalidProductException e) {
			// should never happen
			throw new RuntimeException("Product constraint manager could not be initialized!", e);
		}

		// show product name, version, edition and registered to
		License activeLicense = ProductConstraintManager.INSTANCE.getActiveLicense();
		RapidMiner.splashLicense(activeLicense);

		// install action statistics license event listener
		ProductConstraintManager.INSTANCE.registerLicenseManagerListener(ActionStatisticsLicenseManagerListener.INSTANCE);

		// init repositories
		RapidMiner.splashMessage("init_repository");
		RepositoryManager.init();

		// parse settings xml (before plugins are initialized)
		SettingsItems.INSTANCE.parseStudioXml();

		// generate encryption key if necessary
		if (!CipherTools.isKeyAvailable()) {
			RapidMiner.splashMessage("gen_key");
			try {
				KeyGeneratorTool.createAndStoreKey();
			} catch (KeyGenerationException e) {
				LogService.getRoot().log(Level.WARNING, I18N.getMessage(LogService.getRoot().getResourceBundle(),
						"com.rapidminer.RapidMiner.generating_encryption_key_error", e.getMessage()), e);
			}
		}
		UsageStatistics.getInstance(); // initializes as a side effect

		// registering operators
		RapidMiner.splashMessage("register_plugins");
		Plugin.initAll();
		Plugin.initPluginSplashTexts(RapidMiner.splashScreen);

		RapidMiner.splashMessage("init_ops");
		OperatorService.init();

		// init custom repositories after extension initialization
		RepositoryManager.initCustomRepositories();

		RapidMiner.splashMessage("xml_transformer");
		XMLImporter.init();

		RapidMiner.splashMessage("init_configurables");
		ConfigurationManager.getInstance().initialize();

		// initialize renderers
		RapidMiner.splashMessage("init_renderers");
		RendererService.init();

		// initialize xml serialization
		RapidMiner.splashMessage("xml_serialization");
		XMLSerialization.init(Plugin.getMajorClassLoader());

		if (executionMode == ExecutionMode.TEST) {
			initAsserters();
		}

		initSettingsDescriptions();

		started();
	}

	/**
	 * Sets descriptions for settings-parameters registered in static initializers. Has to be called
	 * after all settings-parameters have been created.
	 *
	 * The methods {@link #registerParameter(ParameterType)} and
	 * {@link #registerParameter(ParameterType, String)} can be used to register settings-parameters
	 * and remember them to add the I18n description here.
	 *
	 * The I18n description has to be set after the registration of parameters, as the I18n would
	 * initialize the parameter service at a point of time where the I18n description of the
	 * parameter has not been set.
	 */
	public static void initSettingsDescriptions() {
		for (ParameterType parameterType : RapidMiner.parameterTypesDescription) {
			parameterType.setDescription(I18N.getSettingsMessage(parameterType.getKey(), SettingsType.DESCRIPTION));
		}
	}

	/**
	 * This method initializes RapidMiner's and all installed Plugin Asserters that will be used by
	 * {@link RapidAssert}. CAUTION: This function has to be called AFTER {@link #init()}.
	 */
	public static void initAsserters() {
		if (!assertersInitialized) {
			LogService.getRoot().log(Level.INFO, "Initializing Asserters...");
			Plugin.initPluginTests();
			RapidAssert.ASSERTER_REGISTRY.registerAllAsserters(new AsserterFactoryRapidMiner());
			assertersInitialized = true;
		}
	}

	public static SplashScreen showSplash() {
		return showSplash(null);
	}

	public static SplashScreen showSplash(final Image productLogo) {
		RapidMiner.splashScreen = new SplashScreen(getShortVersion(), productLogo);
		RapidMiner.splashScreen.showSplashScreen();
		return RapidMiner.splashScreen;
	}

	public static void hideSplash() {
		RapidMiner.splashScreen.dispose();
	}

	/** Displays the message with 18n key gui.splash.messageKey. */
	public static void splashMessage(final String messageKey) {
		performInitialSettings();
		if (RapidMiner.splashScreen != null) {
			RapidMiner.splashScreen.setMessage(I18N.getMessage(I18N.getGUIBundle(), "gui.splash." + messageKey));
		} else {
			LogService.getRoot().config(I18N.getMessage(I18N.getGUIBundle(), "gui.splash." + messageKey));
		}
	}

	/** Displays the edition and registered to info. */
	public static void splashLicense(final License license) {
		if (RapidMiner.splashScreen != null) {
			RapidMiner.splashScreen.setLicense(license);
		}
	}

	/** Displays the formatted message with 18n key gui.splash.messageKey. */
	public static void splashMessage(final String messageKey, final Object... args) {
		performInitialSettings();
		if (RapidMiner.splashScreen != null) {
			RapidMiner.splashScreen.setMessage(I18N.getMessage(I18N.getGUIBundle(), "gui.splash." + messageKey, args));
		}
	}

	private static void performInitialSettings() {
		if (performedInitialSettings) {
			return;
		}
		boolean firstStart = false;
		boolean versionChanged = false;
		VersionNumber lastVersionNumber = null;
		VersionNumber currentVersionNumber = new VersionNumber(getLongVersion());

		File lastVersionFile = new File(FileSystemService.getUserRapidMinerDir(), "lastversion");
		if (!lastVersionFile.exists()) {
			firstStart = true;
		} else {
			String versionString = null;
			BufferedReader in = null;
			try {
				in = new BufferedReader(new FileReader(lastVersionFile));
				versionString = in.readLine();
			} catch (IOException e) {
				LogService.getRoot().log(Level.WARNING, I18N.getMessage(LogService.getRoot().getResourceBundle(),
						"com.rapidminer.RapidMiner.reading_global_version_file_error"), e);
			} finally {
				if (in != null) {
					try {
						in.close();
					} catch (IOException e) {
						LogService.getRoot().log(Level.WARNING, I18N.getMessage(LogService.getRoot().getResourceBundle(),
								"com.rapidminer.RapidMiner.closing_stream_error", lastVersionFile), e);
					}
				}
			}

			if (versionString != null) {
				lastVersionNumber = new VersionNumber(versionString);
				if (currentVersionNumber.compareTo(lastVersionNumber) > 0) {
					firstStart = true;
				}
				if (currentVersionNumber.compareTo(lastVersionNumber) != 0) {
					versionChanged = true;
				}
			} else {
				firstStart = true;
			}
		}

		// init this version (workspace etc.)
		if (firstStart) {
			performFirstInitialization(lastVersionNumber, currentVersionNumber);
		}

		if (firstStart || versionChanged) {
			// write version file
			writeLastVersion(lastVersionFile);
		}

		performedInitialSettings = true;
	}

	private static void writeLastVersion(final File versionFile) {
		PrintWriter out = null;
		try {
			out = new PrintWriter(new FileWriter(versionFile));
			out.println(getLongVersion());
		} catch (IOException e) {
			LogService.getRoot().log(Level.WARNING, I18N.getMessage(LogService.getRoot().getResourceBundle(),
					"com.rapidminer.RapidMiner.writing_current_version_error"), e);
		} finally {
			if (out != null) {
				out.close();
			}
		}
	}

	private static void performFirstInitialization(final VersionNumber lastVersion, final VersionNumber currentVersion) {
		if (currentVersion != null) {
			LogService.getRoot().log(Level.INFO, "com.rapidminer.RapidMiner.performing_upgrade",
					new Object[] { lastVersion != null ? " from version " + lastVersion : "", currentVersion });
		}

	}

	public static SplashScreen getSplashScreen() {
		performInitialSettings();
		return RapidMiner.splashScreen;
	}

	public static Frame getSplashScreenFrame() {
		performInitialSettings();
		if (RapidMiner.splashScreen != null) {
			return RapidMiner.splashScreen.getSplashScreenFrame();
		} else {
			return null;
		}
	}

	public static void setInputHandler(final InputHandler inputHandler) {
		RapidMiner.inputHandler = inputHandler;
	}

	public static InputHandler getInputHandler() {
		return inputHandler;
	}

	public synchronized static void addShutdownHook(final Runnable runnable) {
		shutdownHooks.add(runnable);
	}

	/**
	 * Adds the given {@link Runnable} to the list of hooks which will be executed after initiation
	 * of RapidMiner. If RapidMiner is already initiated the given {@link Runnable} will be executed
	 * immediately.
	 */
	public synchronized static void addStartupHook(final Runnable runnable) {
		if (isInitiated) {
			runStartupHook(runnable);
		}
		startupHooks.add(runnable);
	}

	private synchronized static void started() {
		for (Runnable runnable : startupHooks) {
			runStartupHook(runnable);
		}
		isInitiated = true;
	}

	public static boolean isInitialized() {
		return isInitiated;
	}

	private static void runStartupHook(final Runnable runnable) {
		try {
			runnable.run();
		} catch (Exception e) {
			LogService.getRoot().log(Level.WARNING, I18N.getMessage(LogService.getRoot().getResourceBundle(),
					"com.rapidminer.RapidMiner.executing_startup_hook_error", e.getMessage()), e);
		}
	}

	public synchronized static void quit(final ExitMode exitMode) {
		for (Runnable hook : shutdownHooks) {
			try {
				hook.run();
			} catch (Exception e) {
				LogService.getRoot().log(Level.WARNING, I18N.getMessage(LogService.getRoot().getResourceBundle(),
						"com.rapidminer.RapidMiner.executing_shotdown_hook_error", e.getMessage()), e);
			}
		}
		try {
			Runtime.getRuntime().runFinalization();
		} catch (Exception e) {
			LogService.getRoot().log(Level.WARNING, I18N.getMessage(LogService.getRoot().getResourceBundle(),
					"com.rapidminer.RapidMiner.error_during_finalization", e.getMessage()), e);
		}
		isInitiated = false;
		switch (exitMode) {
			case NORMAL:
				launchComplete();
				System.exit(0);
				break;
			case RELAUNCH:
				launchComplete();
				relaunch();
				break;
			case ERROR:
			default: // error
				System.exit(1);
				break;
		}
	}

	public static void relaunch() {
		LogService.getRoot().info("RapidMiner will be restarted...");
		System.exit(RELAUNCH_EXIT_CODE);
	}

	private static void launchComplete() {
		SafeMode safeMode = RapidMinerGUI.getSafeMode();
		if (safeMode != null) {
			safeMode.launchComplete();
		}
	}

	/**
	 * Registers parameter type at {@link ParameterService} and puts it in
	 * {@link RapidMiner#parameterTypesDescription}.
	 *
	 * The descriptions will be set by {@link #initSettingsDescriptions()} later.
	 */
	public static void registerParameter(ParameterType parameterType) {
		ParameterService.registerParameter(parameterType);
		RapidMiner.parameterTypesDescription.add(parameterType);
	}

	/**
	 * Registers parameter type at {@link ParameterService} and puts it in
	 * {@link RapidMiner#parameterTypesDescription}.
	 *
	 * The descriptions will be set by {@link #initSettingsDescriptions()} later.
	 */
	public static void registerParameter(ParameterType parameterType, String group) {
		ParameterService.registerParameter(parameterType, group);
		RapidMiner.parameterTypesDescription.add(parameterType);
	}

	public static ExecutionMode getExecutionMode() {
		return executionMode;
	}

	public static void setExecutionMode(final ExecutionMode executionMode) {
		RapidMiner.executionMode = executionMode;
	}

	private static void initializeProxy() {
		ProxySettings.init();
		// parameters for proxy settings
		registerParameter(new ParameterTypeCategory(RapidMiner.PROPERTY_RAPIDMINER_PROXY_MODE, "",
				RapidMiner.RAPIDMINER_PROXY_MODES, 0), "proxy");
		// Global exclusion list (applies on all NON_PROXY_HOSTS)
		registerParameter(new ParameterTypeString(PROPERTY_RAPIDMINER_PROXY_EXCLUDE, "", true), "proxy");
		// HTTP
		registerParameter(new ParameterTypeString(PROPERTY_RAPIDMINER_HTTP_PROXY_HOST, "", true), "proxy");
		registerParameter(new ParameterTypeInt(PROPERTY_RAPIDMINER_HTTP_PROXY_PORT, "", 0, 65535, true), "proxy");
		// HTTPS
		registerParameter(new ParameterTypeString(PROPERTY_RAPIDMINER_HTTPS_PROXY_HOST, "", true), "proxy");
		registerParameter(new ParameterTypeInt(PROPERTY_RAPIDMINER_HTTPS_PROXY_PORT, "", 0, 65535, true), "proxy");
		// FTP
		registerParameter(new ParameterTypeString(PROPERTY_RAPIDMINER_FTP_PROXY_HOST, "", true), "proxy");
		registerParameter(new ParameterTypeInt(PROPERTY_RAPIDMINER_FTP_PROXY_PORT, "", 0, 65535, true), "proxy");

		registerParameter(new ParameterTypeString(PROPERTY_RAPIDMINER_SOCKS_PROXY_HOST, "", true), "proxy");
		registerParameter(new ParameterTypeInt(PROPERTY_RAPIDMINER_SOCKS_PROXY_PORT, "", 0, 65535, true), "proxy");
		registerParameter(new ParameterTypeCategory(PROPERTY_RAPIDMINER_SOCKS_VERSION, "", RAPIDMINER_SOCKS_VERSIONS, 1),
				"proxy");
		ProxySettings.apply();
	}

	/**
	 * Returns a set of {@link ParameterType}s for the RapidMiner system properties.
	 *
	 * @deprecated Use {@link #getRapidMinerProperties()} instead
	 */
	@Deprecated
	public static Set<ParameterType> getYaleProperties() {
		return getRapidMinerProperties();
	}

	/**
	 * Use {@link ParameterService#getDefinedParameterTypes()} instead. Returns a set of
	 * {@link ParameterType}s for the RapidMiner system properties.
	 */
	@Deprecated
	public static Set<ParameterType> getRapidMinerProperties() {
		return ParameterService.getDefinedParameterTypes();
	}

	/**
	 * @deprecated Use {@link #ParameterService.registerParameter(ParameterType)} instead
	 */
	@Deprecated
	public static void registerYaleProperty(final ParameterType type) {
		ParameterService.registerParameter(type);
	}

	/**
	 * Please use {@link ParameterService#registerParameter(ParameterType)} instead.
	 *
	 * This registers a property with the name of the given ParameterType. For convenience the
	 * property is of this type, for offering the user a reasonable interface.
	 */
	@Deprecated
	public static void registerRapidMinerProperty(final ParameterType type) {
		ParameterService.registerParameter(type);
	}

	/**
	 * This method is deprecated and remains only for compatiblity reasons. Please refer to
	 * {@link ParameterService#getParameterValue(String)} instead.
	 *
	 * This method will return the value of an registered RapidMiner Property or null if no property
	 * is known with the given identifier.
	 *
	 * @param property
	 *            The identifier of the property
	 * @return the String value of the property or null if property is unknown.
	 */
	@Deprecated
	public static String getRapidMinerPropertyValue(final String property) {
		return ParameterService.getParameterValue(property);
	}

	/**
	 * This method will set the given property to the given value. Please use
	 * {@link ParameterService#setParameterValue(String, String)} instead of this method.
	 */
	@Deprecated
	public static void setRapidMinerPropertyValue(final String property, final String value) {
		ParameterService.setParameterValue(property, value);
	}
}
