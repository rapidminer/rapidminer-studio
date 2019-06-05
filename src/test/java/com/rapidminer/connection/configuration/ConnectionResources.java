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
package com.rapidminer.connection.configuration;

import java.io.File;
import java.net.URL;
import java.nio.file.Path;


/**
 * A value collection for resource lookup for the connection management test
 *
 * @author Andreas Timm
 * @since 9.3
 */
public class ConnectionResources {

	public static final String RAPIDMINER_CONNECTION = "/com/rapidminer/connection/";
	public static final Path RESOURCE_PATH = new File("src/test/resources" + RAPIDMINER_CONNECTION).toPath();

	public static final URL ENCODING_TEST_RESOURCE = ConnectionResources.class.getResource(RAPIDMINER_CONNECTION +
			"encoding-test.txt");
	public static final URL EMPTY_JAR = ConnectionResources.class.getResource(RAPIDMINER_CONNECTION +
			"empty.jar");
}
