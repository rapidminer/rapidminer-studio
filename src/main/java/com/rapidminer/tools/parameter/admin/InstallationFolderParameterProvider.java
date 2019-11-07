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
package com.rapidminer.tools.parameter.admin;

import java.io.File;
import java.net.URI;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.Map;

import com.rapidminer.RapidMiner;
import com.rapidminer.tools.LogService;
import com.rapidminer.tools.ParameterService;
import com.rapidminer.tools.SystemInfoUtilities;
import com.rapidminer.tools.SystemInfoUtilities.OperatingSystem;


/**
 * Administrators can provide a read-only {@value ParameterService#RAPIDMINER_CONFIG_FILE_NAME} file in the installation directory.
 *
 * @author Jonas Wilms-Pfau
 * @since 9.0.0
 */
class InstallationFolderParameterProvider implements ParameterProvider {

	/**
	 * Indicates if current OS is OSX
	 */
	private static final boolean IS_OSX = SystemInfoUtilities.getOperatingSystem() == OperatingSystem.OSX;

	/**
	 * Used to detect if started from an IDE
	 */
	private static final String JAR = ".jar";

	/**
	 * Used to detect if started from MAC .app
	 */
	private static final Path MAC_PATH = Paths.get("Contents", "Resources", "RapidMiner-Studio");

	/**
	 * Location of the settings file in the installation directory
	 */
	private static final File SETTINGS_FILE = getAdminSettingsFile();

	@Override
	public Map<String, String> readProperties() {
		// Check if the settings file exists
		if (SETTINGS_FILE == null || !SETTINGS_FILE.exists()) {
			return null;
		}
		LogService.getRoot().fine(() -> String.format("Trying to enforce settings from \"%s\".", SETTINGS_FILE));
		return new FileParameterProvider(SETTINGS_FILE.toString()).readProperties();
	}

	/**
	 * Returns the path to the admin settings file
	 *
	 * @return path to config or null
	 */
	private static File getAdminSettingsFile() {
		try {
			URI jarLocation = ParameterEnforcer.class.getProtectionDomain().getCodeSource().getLocation().toURI();
			Path installationFolder = coreJarToInstallationFolder(Paths.get(jarLocation));
			if (installationFolder != null) {
				return installationFolder.resolve(ParameterService.RAPIDMINER_CONFIG_FILE_NAME).toFile();
			}
		} catch (Exception e) {
			// ignore
		}
		return null;
	}

	/**
	 * Translates from the rapidminer-studio-core.jar up to the installation directory OS dependent.
	 * This returns the "RapidMiner Studio.app" folder on Mac OS X and the "RapidMiner Studio" folder on other OS.
	 *
	 * @param path
	 * 		path to the .jar
	 * @return path of the installation folder if installed or the path of the class
	 */
	private static Path coreJarToInstallationFolder(Path path) {
		// started from an IDE or can't access file system
		if (!path.toString().endsWith(JAR) || !RapidMiner.getExecutionMode().canAccessFilesystem()) {
			return null;
		}
		//         2          1               0
		// RapidMiner Studio/lib/rapidminer-studio-core.jar
		Path studioPath = path.getParent().getParent();

		// OSX could also run the platform independent version
		if (IS_OSX && studioPath.endsWith(MAC_PATH)) {
			//             3             2        1            0
			// RapidMiner Studio.app/Contents/Resources/RapidMiner-Studio
			return studioPath.getParent().getParent().getParent();
		}

		return studioPath;
	}

}
