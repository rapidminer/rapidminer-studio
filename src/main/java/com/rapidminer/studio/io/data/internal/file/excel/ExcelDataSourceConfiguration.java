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
package com.rapidminer.studio.io.data.internal.file.excel;

import java.util.Arrays;
import java.util.HashMap;
import java.util.Map;

import com.rapidminer.core.io.data.source.DataSourceConfiguration;
import com.rapidminer.operator.nio.model.ExcelResultSetConfiguration;


/**
 * The {@link DataSourceConfiguration} for the {@link ExcelDataSource}.
 *
 * @author Nils Woehler
 * @since 7.0.0
 *
 */
final class ExcelDataSourceConfiguration implements DataSourceConfiguration {

	public static final String HEADER_ROW_INDEX_KEY = ExcelResultSetConfiguration.EXCEL_HEADER_ROW_INDEX;

	private final Map<String, String> parameters = new HashMap<>();

	ExcelDataSourceConfiguration(ExcelDataSource dataSource) {
		parameters.put(HEADER_ROW_INDEX_KEY, String.valueOf(dataSource.getHeaderRowIndex()));

		// add parameters from excel result set configuration
		dataSource.getResultSetConfiguration().storeConfiguration(parameters);
	}

	@Override
	public String getVersion() {
		return "1";
	}

	@Override
	public Map<String, String> getParameters() {
		return parameters;
	}

	@Override
	public String toString() {
		return Arrays.toString(getParameters().entrySet().toArray());
	}
}
