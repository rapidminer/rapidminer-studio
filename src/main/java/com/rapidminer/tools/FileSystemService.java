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
package com.rapidminer.tools;

import static com.rapidminer.tools.ParameterService.PROPERTY_RAPIDMINER_SRC_ROOT;
import static com.rapidminer.tools.ParameterService.RAPIDMINER_CONFIG_FILE_NAME;

import java.io.File;
import java.io.IOException;
import java.util.logging.Level;


/**
 * This service offers methods for accessing the file system. For example to get the current
 * RapidMiner directory, used home directory and several else.
 *
 * @author Sebastian Land
 */
public class FileSystemService {

	/** folder in which extensions have their workspace */
	private static final String RAPIDMINER_EXTENSIONS_FOLDER = "extensions";
	/** folder in which extensions get their own folder to work with files */
	private static final String RAPIDMINER_EXTENSIONS_WORKSPACE_FOLDER = "workspace";
	/** folder which can be used to share data between extensions */
	private static final String RAPIDMINER_SHARED_DATA = "shared data";

	public static final String RAPIDMINER_USER_FOLDER = ".RapidMiner";

	/** Returns the main user configuration file. */
	public static File getMainUserConfigFile() {
		return FileSystemService.getUserConfigFile(RAPIDMINER_CONFIG_FILE_NAME);
	}

	/** Returns the memory configuration file containing the max memory. */
	public static File getMemoryConfigFile() {
		return new File(getUserRapidMinerDir(), "memory");
	}

	/** Returns the RapidMiner log file. */
	public static File getLogFile() {
		return new File(getUserRapidMinerDir(), "rapidminer-studio.log");
	}

	/**
	 * Returns the configuration file in the user dir {@link #RAPIDMINER_USER_FOLDER}.
	 */
	public static File getUserConfigFile(String name) {
		String configName = name;
		return new File(getUserRapidMinerDir(), configName);
	}

	public static File getUserRapidMinerDir() {
		File homeDir = new File(System.getProperty("user.home"));
		File userHomeDir = new File(homeDir, RAPIDMINER_USER_FOLDER);
		File extensionsWorkspaceRootFolder = new File(userHomeDir, RAPIDMINER_EXTENSIONS_FOLDER);
		File extensionsWorkspaceFolder = new File(extensionsWorkspaceRootFolder, RAPIDMINER_EXTENSIONS_WORKSPACE_FOLDER);
		File sharedDataDir = new File(userHomeDir, RAPIDMINER_SHARED_DATA);

		if (!userHomeDir.exists()) {
			LogService.getRoot().log(Level.CONFIG, "com.rapidminer.tools.FileSystemService.creating_directory", userHomeDir);
			boolean result = userHomeDir.mkdir();
			if (!result) {
				LogService.getRoot().log(Level.WARNING,
						"com.rapidminer.tools.FileSystemService.creating_home_directory_error", userHomeDir);
			}
		}
		if (!extensionsWorkspaceRootFolder.exists()) {
			LogService.getRoot().log(Level.CONFIG, "com.rapidminer.tools.FileSystemService.creating_directory",
					extensionsWorkspaceRootFolder);
			boolean result = extensionsWorkspaceRootFolder.mkdir();
			if (!result) {
				LogService.getRoot().log(Level.WARNING,
						"com.rapidminer.tools.FileSystemService.creating_home_directory_error",
						extensionsWorkspaceRootFolder);
			}
		}
		if (!extensionsWorkspaceFolder.exists()) {
			LogService.getRoot().log(Level.CONFIG, "com.rapidminer.tools.FileSystemService.creating_directory",
					extensionsWorkspaceFolder);
			boolean result = extensionsWorkspaceFolder.mkdir();
			if (!result) {
				LogService.getRoot().log(Level.WARNING,
						"com.rapidminer.tools.FileSystemService.creating_home_directory_error", extensionsWorkspaceFolder);
			}
		}
		if (!sharedDataDir.exists()) {
			LogService.getRoot().log(Level.CONFIG, "com.rapidminer.tools.FileSystemService.creating_directory",
					sharedDataDir);
			boolean result = sharedDataDir.mkdir();
			if (!result) {
				LogService.getRoot().log(Level.WARNING,
						"com.rapidminer.tools.FileSystemService.creating_home_directory_error", sharedDataDir);
			}
		}
		return userHomeDir;
	}

	/**
	 * Returns the folder for which an extension has read/write/delete permissions. The folder is
	 * located in the {@link #getUserRapidMinerDir()} folder.
	 *
	 * @param extensionId
	 *            the key of the extension, e.g. {@code rmx_myextension}
	 * @return a file with the working directory for the given extension id, never {@code null}
	 */
	public static File getPluginRapidMinerDir(String extensionId) {
		File userHomeDir = getUserRapidMinerDir();
		File extensionFolder = new File(userHomeDir, "extensions/workspace/" + extensionId);
		if (!extensionFolder.exists()) {
			extensionFolder.mkdir();
		}

		return extensionFolder;
	}

	public static File getRapidMinerHome() throws IOException {
		String property = System.getProperty(PlatformUtilities.PROPERTY_RAPIDMINER_HOME);
		if (property == null) {
			throw new IOException("Property " + PlatformUtilities.PROPERTY_RAPIDMINER_HOME + " is not set");
		}
		// remove any line breaks that snuck in for some reason
		property = property.replaceAll("\\r|\\n", "");
		return new File(property);
	}

	public static File getLibraryFile(String name) throws IOException {
		File home = getRapidMinerHome();
		return new File(home, "lib" + File.separator + name);
	}

	public static File getSourceRoot() {
		String srcName = System.getProperty(PROPERTY_RAPIDMINER_SRC_ROOT);
		if (srcName == null) {
			LogService.getRoot().log(Level.WARNING, "com.rapidminer.tools.FileSystemService.property_not_set",
					PROPERTY_RAPIDMINER_SRC_ROOT);
			return null;
		} else {
			return new File(srcName);
		}
	}

	public static File getSourceFile(String name) {
		File root = getSourceRoot();
		if (root == null) {
			return null;
		} else {
			return new File(new File(root, "src"), name);
		}
	}

	public static File getSourceResourceFile(String name) {
		File root = getSourceRoot();
		if (root == null) {
			return null;
		} else {
			return new File(new File(root, "resources"), name);
		}
	}

}
