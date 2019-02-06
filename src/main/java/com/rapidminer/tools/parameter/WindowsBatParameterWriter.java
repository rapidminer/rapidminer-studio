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
package com.rapidminer.tools.parameter;

import com.rapidminer.tools.FileSystemService;

import java.io.BufferedWriter;
import java.io.File;
import java.io.IOException;
import java.io.PrintWriter;
import java.util.Map;
import java.util.Map.Entry;
import java.util.logging.Level;
import java.util.logging.Logger;


/**
 * A writer that will export the defined parameters that are preStart Parameters to a config file in
 * the user directory that is used by the batch start script to set Environment Variables before
 * actual JVM starts.
 * 
 * @author Sebastian Land
 */
public class WindowsBatParameterWriter implements ParameterWriter {

	public static final String CONFIG_FILE_NAME = "config.win.bat";

	private static final Logger LOGGER = Logger.getLogger(WindowsBatParameterWriter.class.getSimpleName());

	@Override
	public void writeParameters(Map<String, Parameter> parameters) {
		File configFile = new File(FileSystemService.getUserRapidMinerDir(), CONFIG_FILE_NAME);
		try (BufferedWriter writer = new BufferedWriter(new PrintWriter(configFile))) {
			for (Entry<String, Parameter> entry : parameters.entrySet()) {
				Parameter parameter = entry.getValue();
				if (parameter.getValue() != null) {
					if (parameter.isDefined() && parameter.getScope().isPreStartParameter()) {
						String envName = parameter.getScope().getPreStartName();
						if (parameter.getScope().isModifyingPreStartParameter()) {
							writer.append("SET " + envName + "=%" + envName + "%;" + parameter.getValue());
						} else {
							writer.append("SET " + envName + "=" + parameter.getValue());
						}
					}
				}
			}
		} catch (IOException e) {
			LOGGER.log(Level.WARNING, "Could not write config file. Reason: ", e);
		}
	}
}
