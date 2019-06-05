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
package com.rapidminer.connection;

import java.io.IOException;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.ObjectReader;
import com.fasterxml.jackson.databind.ObjectWriter;
import com.rapidminer.tools.ValidationUtil;

/**
 * Builder for {@link ConnectionStatistics}. Can create a new instance based on an existing {@link ConnectionStatistics}
 *
 * @author Jonas Wilms-Pfau
 * @since 9.3
 */
class ConnectionStatisticsBuilder {

	private ConnectionStatistics object;

	private static final ObjectWriter writer;
	private static final ObjectReader reader;

	static {
		ObjectMapper mapper = new ObjectMapper();
		reader = mapper.reader(ConnectionStatisticsImpl.class);
		writer = mapper.writerWithType(ConnectionStatisticsImpl.class);
	}


	/**
	 * Create a builder based on an existing {@link ConnectionStatistics}
	 */
	ConnectionStatisticsBuilder(ConnectionStatistics original) throws IOException {
		ValidationUtil.requireNonNull(original, "original connection statistics");
		object = reader.readValue(writer.writeValueAsBytes(original));
	}

	/**
	 * Build and return the new {@link ConnectionStatistics}. Afterwards, the builder becomes invalid.
	 */
	public ConnectionStatistics build() {
		ConnectionStatistics statistics = object;
		object = null;
		return statistics;
	}
}
