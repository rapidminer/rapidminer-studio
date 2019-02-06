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
package com.rapidminer.studio.io.data.internal.file.binary;

import com.rapidminer.core.io.data.DataSet;
import com.rapidminer.core.io.data.DataSetException;
import com.rapidminer.core.io.data.DataSetMetaData;
import com.rapidminer.core.io.data.source.DataSource;
import com.rapidminer.core.io.data.source.DataSourceConfiguration;
import com.rapidminer.core.io.data.source.FileDataSource;


/**
 * A {@link DataSource} for binary files.
 *
 * @author Michael Knopf
 */
class BinaryDataSource extends FileDataSource {

	@Override
	public DataSet getData() throws DataSetException {
		throw new DataSetException("Data not available for binary import.");
	}

	@Override
	public DataSet getPreview(int maxPreviewSize) throws DataSetException {
		throw new DataSetException("Preview not available for binary import.");
	}

	@Override
	public DataSetMetaData getMetadata() throws DataSetException {
		throw new DataSetException("Meta data not available for binary import.");
	}

	@Override
	public DataSourceConfiguration getConfiguration() {
		// not supported
		return null;
	}

	@Override
	public void configure(DataSourceConfiguration configuration) throws DataSetException {
		// not supported
	}

	@Override
	public void close() throws DataSetException {}

}
