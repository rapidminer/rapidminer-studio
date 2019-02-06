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
package com.rapidminer.studio.io.data.internal.file;

import java.io.IOException;
import java.util.Properties;

import com.rapidminer.core.io.data.source.DataSource;


/**
 * Utilities for the file {@link DataSource} test classes (e.g. UTF-8 string loading from propertie
 * files).
 *
 * @author Nils Woehler
 *
 */
public class FileDataSourceTestUtils {

	private static final Properties PROPERTIES = new Properties();

	static {
		try {
			PROPERTIES.load(FileDataSourceTestUtils.class.getResourceAsStream("utf8Terms.properties"));
		} catch (IOException e) {
			// write the error, will show up in tests when calling getters
			e.printStackTrace();
		}
	}

	/**
	 * @return the UTF-8 label stored under the 'labelHeader' key
	 */
	public static String getUtf8Label() {
		if (!PROPERTIES.containsKey("labelHeader")) {
			throw new IllegalArgumentException("Label header not part of utf8 properties file");
		}
		return PROPERTIES.getProperty("labelHeader");
	}

	/**
	 * @return the UTF-8 label stored under the 'entryUTF8' key
	 */
	public static String getUtf8Entry() {
		if (!PROPERTIES.containsKey("entryUTF8")) {
			throw new IllegalArgumentException("entryUTF8 not part of utf8 properties file");
		}
		return PROPERTIES.getProperty("entryUTF8");
	}

	/**
	 * @return the UTF-8 label stored under the 'entryWindows' key
	 */
	public static String getWindowsEntry() {
		if (!PROPERTIES.containsKey("entryWindows")) {
			throw new IllegalArgumentException("entryWindows not part of utf8 properties file");
		}
		return PROPERTIES.getProperty("entryWindows");
	}

}
