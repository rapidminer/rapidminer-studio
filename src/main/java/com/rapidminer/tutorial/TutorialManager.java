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
package com.rapidminer.tutorial;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.util.Properties;
import java.util.logging.Level;

import com.rapidminer.tools.FileSystemService;
import com.rapidminer.tools.LogService;


/**
 * Manages the property file for the tutorials.
 *
 * @author Marcel Michel
 * @since 7.0.0
 */
public enum TutorialManager {

	/**
	 * The {@link TutorialManager} instance
	 */
	INSTANCE;

	private static final String PROPERTIES_TUTORIALS = "tutorials.properties";

	private Properties properties;

	private TutorialManager() {
		loadProperties();
	}

	/**
	 * Puts a property to the tutorial property file.
	 */
	public void putProperty(String key, String value) {
		properties.put(key, value);
		saveProperties();
	}

	/**
	 * @return the value of the property key, may return <code>null</code>
	 */
	public String getProperty(String key) {
		Object result = properties.get(key);
		return result != null ? String.valueOf(result) : null;
	}

	/**
	 * Checks if the tutorial with the given identifier has been completed.
	 *
	 * @param tutorialIdentifier
	 *            the identification of the tutorial
	 * @return {@code true} if the tutorial has been completed otherwise {@code false}
	 */
	public boolean hasCompletedTutorial(String tutorialIdentifier) {
		if (tutorialIdentifier == null) {
			throw new IllegalArgumentException("tutorialIdentifier must not be null!");
		}
		String value = getProperty(tutorialIdentifier);
		if (value != null) {
			return value.equals("true");
		} else {
			return false;
		}
	}

	/**
	 * Sets the the tutorial with the given identifier to completed.
	 *
	 * @param tutorialIdentifier
	 *            the identification of the tutorial
	 */
	public void completedTutorial(String tutorialIdentifier) {
		if (tutorialIdentifier == null) {
			throw new IllegalArgumentException("tutorialIdentifier must not be null!");
		}
		putProperty(tutorialIdentifier, "true");
	}

	/**
	 * Loads the property file.
	 */
	private void loadProperties() {
		this.properties = new Properties();
		File file = FileSystemService.getUserConfigFile(PROPERTIES_TUTORIALS);
		try {
			if (!file.exists() && !file.createNewFile()) {
				throw new IOException("Error creating oboarding.properties file");
			}
			try (FileInputStream fs = new FileInputStream(file)) {
				properties.load(fs);
			}
		} catch (IOException e) {
			LogService.getRoot().log(Level.INFO, "com.rapidminer.gui.license.onboarding.properties_load_error", e);
		}
	}

	/**
	 * Saves the property file.
	 */
	private void saveProperties() {
		File file = FileSystemService.getUserConfigFile(PROPERTIES_TUTORIALS);
		try (FileOutputStream fs = new FileOutputStream(file)) {
			properties.store(fs, null);
		} catch (IOException e) {
			LogService.getRoot().log(Level.INFO, "com.rapidminer.gui.license.onboarding.properties_save_error", e);
		}
	}

}
