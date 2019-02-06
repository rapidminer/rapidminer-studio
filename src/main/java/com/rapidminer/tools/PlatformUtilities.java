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

import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;
import java.util.HashSet;
import java.util.Locale;
import java.util.Properties;
import java.util.Set;
import java.util.logging.Level;
import java.util.logging.Logger;


/**
 * Helper class to get platform information.
 *
 * @author Nils Woehler
 */
@SuppressWarnings({ "PMD.AvoidSynchronizedAtMethodLevel", "PMD.TooManyMethods", "PMD.GodClass" })
public final class PlatformUtilities {

	private static final String PLATFORM_PROPERTIES = "conf/platform.properties";
	private static final String PLATFORM_PROPERTY_KEY = "platform";
	private static final String GRADLE_PROPERTIES = "gradle.properties";
	private static final String VERSION_PROPERTY_KEY = "version";
	private static final String REVISION_PROPERTY_KEY = "revision";

	/**
	 * The name of the RapidMiner Studio Launcher Jar
	 */
	private static final String RAPIDMINER_STUDIO_LAUNCHER_JAR = "rapidminer-studio-launcher.jar";

	/**
	 * A regular expression that matches any version of the RapidMiner Studio Core Jar
	 */
	private static final String RAPIDMINER_STUDIO_CORE_REGEX = ".+rapidminer-studio-core-\\d.*\\.jar";

	/**
	 * Elements potentially added to the library path by build tools.
	 */
	private static final Set<String> BUILD_PATH_ELEMENTS  = new HashSet<>(4);
	static {
		BUILD_PATH_ELEMENTS.add("build");
		BUILD_PATH_ELEMENTS.add("out");
		BUILD_PATH_ELEMENTS.add("classes");
		BUILD_PATH_ELEMENTS.add("rapidminer-studio-core");
		BUILD_PATH_ELEMENTS.add("rapidminer-studio-commons");
	}

	/**
	 * The suffix of a path from where the tests are started via Gradle.<br/>
	 * (Gradle copies created jars to '$PROJECT/target/libs')
	 */
	private static final String TEST_ENVIRONMENT_SUFFIX = File.separatorChar + "target" + File.separatorChar + "libs";

	private static final Logger LOGGER = Logger.getLogger(PlatformUtilities.class.getSimpleName());

	/** The name of the property indicating the home directory of RapidMiner. */
	public static final String PROPERTY_RAPIDMINER_HOME = "rapidminer.home";

	/**
	 * All available release platforms.
	 */
	public enum Platform {
		/**
		 * Platform independent release
		 */
		ANY,
		/**
		 * Windows 32-bit
		 */
		WIN32,
		/**
		 * Windows 64-bit
		 */
		WIN64,
		/**
		 * Apple OS X
		 */
		OSX
	}

	/**
	 * Cache for current release platform.
	 */
	private static Platform currentPlatform = null;

	/**
	 * Cache for version of current release.
	 */
	private static String currentVersion = null;

	/**
	 * Cache for revision of current release.
	 */
	private static String currentRevision = null;

	private static boolean isInitialized = false;

	private static final Object INIT_VERSION_LOCK = new Object();

	/**
	 * Utility class constructor.
	 */
	private PlatformUtilities() {
		throw new AssertionError();
	}

	/**
	 * Initializes the PlatformUtilities caches
	 */
	public static synchronized void initialize() {
		if (!isInitialized) {
			// silently ensure that the RapidMiner Home system property is set
			ensureRapidMinerHomeSet(Level.OFF);
			initializeReleasePlatform();
			initializeReleaseVersion();
			initializeReleaseRevision();
			isInitialized = true;
		}
	}

	/**
	 * @return the platform the RapidMiner Studio release was built for. Will return
	 *         <code>null</code> in case platform was not defined.
	 */
	public static synchronized Platform getReleasePlatform() {
		if (!isInitialized) {
			initialize();
		}
		return currentPlatform;
	}

	/**
	 * Loads release platform from properties file.
	 */
	private static void initializeReleasePlatform() {
		String platformProperty = readConfigProperty(PLATFORM_PROPERTIES, PLATFORM_PROPERTY_KEY);
		if (platformProperty == null) {
			logInfo("Release platform not defined.");
		} else {
			currentPlatform = Platform.valueOf(platformProperty.toUpperCase(Locale.UK));
			logInfo("Release platform: " + currentPlatform);
		}
	}

	/**
	 * @return the current version of the platform release as {@link String}.
	 */
	public static synchronized String getReleaseVersion() {
		if (!isInitialized) {
			initialize();
		}
		return currentVersion;
	}

	/**
	 * Initializes the current version by reading the version.properties file
	 */
	private static void initializeReleaseVersion() {
		synchronized (INIT_VERSION_LOCK) {
			currentVersion = readResourceProperty(VERSION_PROPERTY_KEY);
			if (currentVersion == null) {
				logInfo("Could not read current version from resources. Looking for 'gradle.properties'...");
				currentVersion = readConfigProperty(GRADLE_PROPERTIES, VERSION_PROPERTY_KEY);
				if (currentVersion == null) {
					throw new IllegalStateException("Could not initialize RapidMiner Studio version from properties file");
				}
			}
		}
	}

	/**
	 * @return the current version of the platform release as {@link String}.
	 */
	public static synchronized String getReleaseRevision() {
		if (!isInitialized) {
			initialize();
		}
		return currentRevision;
	}

	/**
	 * Initializes the current version by reading the version.properties file
	 */
	private static void initializeReleaseRevision() {
		synchronized (INIT_VERSION_LOCK) {
			currentRevision = readResourceProperty(REVISION_PROPERTY_KEY);
			if (currentRevision == null) {
				logInfo("Could not read current revision from resources.");
			}
		}
	}

	/**
	 * @return the system property 'rapidminer.home'.
	 */
	public static synchronized String getRapidMinerHome() {
		return System.getProperty(PROPERTY_RAPIDMINER_HOME);
	}

	/**
	 * Reads a value from a .properties file stored in the resources of this JAR.
	 */
	private static String readResourceProperty(String propertyKey) {
		Properties props = new Properties();
		try (InputStream resourceAsStream = PlatformUtilities.class.getResourceAsStream("version.properties")) {
			if (resourceAsStream == null) {
				logInfo("Version resource file not found at 'com/rapidminer/tools/version.properties'.");
				return null;
			} else {
				props.load(resourceAsStream);
			}
		} catch (IOException e) {
			logWarn("Error reading version properties from resources file!" + e.getLocalizedMessage());
		}
		return props.getProperty(propertyKey);
	}

	/**
	 * Reads a value from a .properties file stored relative to the folder specified by the system
	 * property 'rapidminer.home'.
	 */
	private static String readConfigProperty(String relativePath, String propertyKey) {
		String home = getRapidMinerHome();
		if (home == null) {
			logWarn("Property 'rapidminer.home' not set. Cannot read property file '" + relativePath + "'");
			return null;
		}
		File propertyFile = new File(home, relativePath);
		if (propertyFile.canRead()) {
			Properties props = new Properties();
			try (InputStreamReader reader = new InputStreamReader(new FileInputStream(propertyFile), StandardCharsets.UTF_8)) {
				props.load(reader);
			} catch (IOException e) {
				logWarn("Error reading properties file! " + e.getLocalizedMessage());
			}
			return props.getProperty(propertyKey);
		} else {
			logWarn("Property file (" + propertyFile + ") not found or not readable!");
			return null;
		}
	}

	/**
	 * Ensures that the environment variable 'rapidminer.home' is set by calling
	 * {@link #ensureRapidMinerHomeSet(Level)} with {@link Level#INFO}.
	 */
	public static void ensureRapidMinerHomeSet() {
		ensureRapidMinerHomeSet(Level.INFO);
	}

	/**
	 * Ensures that the environment variable 'rapidminer.home' is set by searching for RapidMiner
	 * Jars in classpath and build dir.
	 *
	 * @param logLevel
	 *            the {@link Level} to log method informations
	 */
	public static synchronized void ensureRapidMinerHomeSet(final Level logLevel) {
		LOGGER.setLevel(logLevel);
		if (getRapidMinerHome() == null) {
			logInfo("Property " + PROPERTY_RAPIDMINER_HOME + " is not set. Guessing.");
			if (!searchInClassPath()) {
				try {
					logInfo("Property " + PROPERTY_RAPIDMINER_HOME
							+ " not found via search in Classpath. Searching in build directory.");
					searchInBuildDir();
				} catch (Throwable e) {  // NOPMD
					// important: not only URI Syntax Exception since the
					// program must not crash
					// in any case!!!
					// For example: RapidNet integration as applet into
					// Siebel would cause
					// problem with new File(...)
					logSevere("Failed to locate 'rapidminer.home'! Cause: " + e.getLocalizedMessage());
				}
			}
		} else {
			logInfo(PROPERTY_RAPIDMINER_HOME + " is '" + getRapidMinerHome() + "'.");
		}
	}

	/**
	 * Checks the Java Classpath for rapidminer studio launcher or core jars.
	 *
	 * @return
	 */
	private static boolean searchInClassPath() {
		logInfo("Searching in Java classpath for RapidMiner Studio jars...");
		String classpath = System.getProperty("java.class.path");
		String[] pathComponents = classpath.split(File.pathSeparator);

		for (int i = 0; i < pathComponents.length; i++) {
			String path = pathComponents[i].trim();
			/*
			 * Search for either 'rapidminer-studio-launcher.jar' or for
			 * 'rapidminer-studio-core-x.x.xxx.jar'
			 */
			if (path.matches(RAPIDMINER_STUDIO_CORE_REGEX) || path.endsWith(RAPIDMINER_STUDIO_LAUNCHER_JAR)) {
				File jar = new File(path).getAbsoluteFile();
				logInfo("Trying parent directory of '" + jar + "'...");

				// Retrieve the directory the jar is placed in
				File dir = jar.getParentFile();
				if (dir == null) {
					logSevere("Failed to retrieve 'rapidminer.home'. Parent of jar is not a directory!");
				} else {
					dir = retrieveRMHomeFromLibraryDir(dir);
					if (dir != null && dir.isDirectory()) {
						logInfo("Gotcha! 'rapidminer.home' is: " + dir);

						System.setProperty(PROPERTY_RAPIDMINER_HOME, dir.getAbsolutePath());
						return true;
					} else {
						logSevere("Failed to retrieve 'rapidminer.home'. Parent of jar directory is not a directory!");
					}
				}
			}
		}
		return false;
	}

	/**
	 * Searches for 'rapidminer.home' assuming a development environment. Start looking for
	 * 'rapidminer-studio' folder.
	 */
	private static void searchInBuildDir() {
		String url = PlatformUtilities.class.getProtectionDomain().getCodeSource().getLocation().getPath();
		if (url == null) {
			logSevere("Failed to locate 'rapidminer.home'. Could not locate base directory of classes!");
		} else {
			// Use parent file, not the JAR itself
			File buildDir = new File(url).getParentFile();
			logInfo("Trying base directory of classes (build) '" + buildDir + "' ...");

			// check if buildDir is a directory
			if (buildDir != null && buildDir.isDirectory()) {

				File rmHome = retrieveRMHomeFromLibraryDir(buildDir);

				if (rmHome != null && rmHome.isDirectory()) {

					// We have found rmHome
					logInfo("Gotcha! rapidminer.home is: " + rmHome);
					try {
						System.setProperty(PROPERTY_RAPIDMINER_HOME, rmHome.getCanonicalPath());
					} catch (IOException e) {
						System.setProperty(PROPERTY_RAPIDMINER_HOME, rmHome.getAbsolutePath());
					}
				} else {
					logSevere("Failed to locate 'rapidminer.home'! Parent dir of build directory does not exist.");
				}
			} else {
				logSevere("Failed to locate 'rapidminer.home'! Build directory does not exists or isn't a directory.");
			}
		}
	}

	/**
	 *
	 * @param buildDir
	 * @return
	 */
	private static File retrieveRMHomeFromLibraryDir(File buildDir) {
		/*
		 * Search for the project directory of the current build. If the search was started via
		 * Gradle, we are in 'target/libs/'. If it was started via an IDE we are in 'build/' or
		 * 'bin/'.
		 */
		File projectDir = null;
		if (buildDir.getAbsolutePath().endsWith(TEST_ENVIRONMENT_SUFFIX)) {
			// If it was started via Gradle, go two levels up so we're in the
			// project's directory.
			logInfo("Library located in 'target/libs/'. Assuming Gradle test task invocation.");
			projectDir = buildDir.getParentFile().getParentFile();
		} else {
			// Otherwise assume that the search was started via IDE or from productive environment
			// and use the library directory parent
			projectDir = buildDir.getParentFile();

		}

		// Check if we are in a development or test environment
		while (projectDir != null && BUILD_PATH_ELEMENTS.contains(projectDir.getName())) {
			projectDir = projectDir.getParentFile();
		}

		return projectDir;
	}

	private static void logInfo(String msg) {
		LOGGER.log(Level.INFO, msg);
	}

	private static void logWarn(String msg) {
		LOGGER.log(Level.WARNING, msg);
	}

	private static void logSevere(String msg) {
		LOGGER.log(Level.SEVERE, msg);
	}

}
