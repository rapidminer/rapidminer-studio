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

import java.util.Map;

import com.rapidminer.tools.LogService;


/**
 * Provides the parameters of the URL or file path stored in the {@value ENFORCE_CONFIG_ENV} system environment variable.
 *
 * @author Jonas Wilms-Pfau
 * @since 9.0.0
 */
class SystemEnvironmentParameterProvider implements ParameterProvider {

	/**
	 * Name of the environment variable that holds the path or URL to the config file.
	 */
	public static final String ENFORCE_CONFIG_ENV = "RAPIDMINER_ENFORCE_CONFIG";

	/**
	 * Path to the config file. (Environment variables shouldn't change during runtime)
	 */
	private static final String PATH = System.getenv(ENFORCE_CONFIG_ENV);

	@Override
	public Map<String, String> readProperties() {
		if (PATH == null) {
			return null;
		}
		LogService.getRoot().fine(() -> String.format("Trying to enforce settings from environment variable %s=%s.", ENFORCE_CONFIG_ENV, PATH));
		return new FileParameterProvider(PATH).readProperties();
	}
}
