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

import java.io.IOException;
import java.io.InputStream;
import java.net.URL;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.util.HashMap;
import java.util.Map;
import java.util.Properties;

import com.rapidminer.tools.LogService;


/**
 * Provides the parameters of the provided URL or file path
 *
 * @author Jonas Wilms-Pfau
 * @since 9.0.0
 */
class FileParameterProvider implements ParameterProvider {

	/**
	 * Location of the file
	 */
	private final String fileLocation;

	/**
	 * I18N relies on the ParameterService
	 */
	private static final String ERROR_NOT_LOADED_TITLE = "Administrative restrictions configuration broken";
	private static final String ERROR_NOT_LOADED_MESSAGE = "<html><div style='width:500;'>Your system administrator has tried to restrict some features of RapidMiner Studio. Unfortunately, the restriction configuration cannot be read. Studio will not be able to start for security reasons until this issue is resolved.</div></html>";
	private static final String ERROR_NOT_LOADED_LOG = "Could not read file \"%s\"";

	private static final String ERROR_INVALID_ENCODING_TITLE = ERROR_NOT_LOADED_TITLE;
	private static final String ERROR_INVALID_ENCODING_MESSAGE = "<html><div style='width:500;'>Your system administrator has tried to restrict some features of RapidMiner Studio. Unfortunately, the restriction configuration is broken. Studio will not be able to start for security reasons until this issue is resolved.</div></html>";
	private static final String ERROR_INVALID_ENCODING_LOG = "The settings file \"%s\" contains an invalid unicode character, learn more at https://docs.oracle.com/javase/8/docs/api/java/util/Properties.html";

	/**
	 * Creates a new FileParameterProvider for the given location
	 *
	 * @param fileLocation either a URL or a file path
	 */
	FileParameterProvider(String fileLocation){
		this.fileLocation = fileLocation;
	}

	@Override
	public Map<String, String> readProperties() {
		if (fileLocation == null) {
			return null;
		}
		try (InputStream in = openStream(fileLocation.trim())) {
			Map<String, String> result = new HashMap<>();
			Properties newProperties = new Properties();
			newProperties.load(in);
			newProperties.forEach((key, value) -> {
				if (key instanceof String && value instanceof String) {
					result.put((String) key, (String) value);
				}
			});
			LogService.getRoot().fine(() -> String.format("Successfully enforced settings from \"%s\".", fileLocation));
			return result;
		} catch (IllegalArgumentException iae) {
			//invalid unicode character
			throw new ProvidedConfigurationException(String.format(ERROR_INVALID_ENCODING_LOG, fileLocation), iae, ERROR_INVALID_ENCODING_TITLE, ERROR_INVALID_ENCODING_MESSAGE);
		} catch (IOException e) {
			throw new ProvidedConfigurationException(String.format(ERROR_NOT_LOADED_LOG, fileLocation), e, ERROR_NOT_LOADED_TITLE, ERROR_NOT_LOADED_MESSAGE);
		}
	}

	/**
	 * Tries to open the file as a local file or URL
	 *
	 * @param file location of the file to open
	 * @return {@link InputStream} of the file
	 * @throws IOException
	 * 		in case the stream could not be opened
	 */
	private static InputStream openStream(String file) throws IOException {
		InputStream input;
		try {
			input = Files.newInputStream(Paths.get(file));
		} catch (Exception e) {
			try {
				input = new URL(file).openStream();
			} catch (Exception mu) {
				mu.addSuppressed(e);
				throw mu;
			}
		}
		return input;
	}
}
