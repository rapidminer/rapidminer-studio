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
package com.rapidminer.tools.config;

import java.io.File;
import java.io.IOException;
import java.util.Collections;
import java.util.Map;
import java.util.logging.Level;

import org.w3c.dom.Document;

import com.rapidminer.RapidMiner;
import com.rapidminer.io.process.XMLTools;
import com.rapidminer.tools.FileSystemService;
import com.rapidminer.tools.I18N;
import com.rapidminer.tools.LogService;
import com.rapidminer.tools.container.Pair;


/**
 * Stores configurations in files (one file per {@link Configurator}.
 *
 * @author Simon Fischer
 */
public class ClientConfigurationManager extends ConfigurationManager {

	@Override
	protected Map<Pair<Integer, String>, Map<String, String>> loadAllParameters(
			AbstractConfigurator<?> configurator) throws ConfigurationException {

		final File file = getConfigFile(configurator);
		if (!file.exists()) {
			LogService.getRoot().log(Level.INFO,
					"com.rapidminer.tools.config.ClientConfigurationManager.no_configuration_file_found",
					configurator.getName());
			return Collections.emptyMap();
		}
		try {
			return fromXML(XMLTools.parse(file), configurator);
		} catch (Exception e) {
			throw new ConfigurationException("Failed to read configuration file '" + file.getAbsolutePath() + "' for "
					+ configurator.getName() + ": " + e, e);
		}
	}

	/**
	 * Returns the config file in which is source or destination for the data.
	 *
	 * @since 9.0.3
	 */
	private static File getConfigFile(AbstractConfigurator<? extends Configurable> configurator) {
		if (RapidMiner.ExecutionMode.TEST == RapidMiner.getExecutionMode()) {
			try {
				final File rapidMinerHome = FileSystemService.getRapidMinerHome();
				return new File(rapidMinerHome, getFileName(configurator));
			} catch (IOException e) {
				LogService.getRoot().warning(e.getMessage());
			}
		}
		return FileSystemService.getUserConfigFile(getFileName(configurator));
	}

	/**
	 * Set up the file name for a configurable configuration file
	 *
	 * @param configurator
	 * 		may not be null, its type Id is used for the file name
	 * @return a file name like "configurable-twitter.xml"
	 * @since 9.0.3
	 */
	private static String getFileName(AbstractConfigurator<? extends Configurable> configurator) {
		return "configurable-" + configurator.getTypeId() + ".xml";
	}

	@Override
	public void saveConfiguration(String typeId) throws ConfigurationException {
		AbstractConfigurator<? extends Configurable> configurator = getAbstractConfigurator(typeId);
		try {
			Document xml = getConfigurablesAsXML(configurator, true);
			File file = getConfigFile(configurator);
			XMLTools.stream(xml, file, null);
		} catch (Exception e) {
			LogService.getRoot().log(
					Level.WARNING,
					I18N.getMessage(LogService.getRoot().getResourceBundle(),
							"com.rapidminer.tools.config.ClientConfigurationManager.saving_configurations_error",
							configurator.getName(), e), e);
			throw new ConfigurationException("Failed to save configuration file: " + e, e);
		}
	}

}
