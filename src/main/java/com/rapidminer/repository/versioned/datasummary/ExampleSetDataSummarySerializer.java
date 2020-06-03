/**
 * Copyright (C) 2001-2020 by RapidMiner and the contributors
 *
 * Complete list of developers available at our web site:
 *
 * http://rapidminer.com
 *
 * This program is free software: you can redistribute it and/or modify it under the terms of the GNU Affero General
 * Public License as published by the Free Software Foundation, either version 3 of the License, or (at your option) any
 * later version.
 *
 * This program is distributed in the hope that it will be useful, but WITHOUT ANY WARRANTY; without even the implied
 * warranty of MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the GNU Affero General Public License for more
 * details.
 *
 * You should have received a copy of the GNU Affero General Public License along with this program. If not, see
 * http://www.gnu.org/licenses/.
 */
package com.rapidminer.repository.versioned.datasummary;

import java.io.IOException;
import java.nio.file.Path;

import com.rapidminer.operator.ports.metadata.ExampleSetMetaData;
import com.rapidminer.repository.versioned.IOObjectFileTypeHandler;
import com.rapidminer.storage.hdf5.ExampleSetHdf5Writer;
import com.rapidminer.storage.hdf5.Hdf5ExampleSetReader;
import com.rapidminer.versioning.repository.DataSummary;

/**
 * {@link DataSummarySerializer} for {@link com.rapidminer.example.ExampleSet ExampleSets}, i.e. for (de)serializing
 * {@link ExampleSetMetaData}. Utilizes {@link ExampleSetHdf5Writer} and {@link Hdf5ExampleSetReader} and uses the
 * new hdf5 suffix {@value IOObjectFileTypeHandler#DATA_TABLE_FILE_ENDING}.
 *
 * @author Jan Czogalla
 * @since 9.7
 */
public enum ExampleSetDataSummarySerializer implements DataSummarySerializer {

	INSTANCE;

	@Override
	public String getSuffix() {
		return IOObjectFileTypeHandler.DATA_TABLE_FILE_ENDING;
	}

	@Override
	public Class<? extends DataSummary> getSummaryClass() {
		return ExampleSetMetaData.class;
	}

	@Override
	public void serialize(Path path, DataSummary dataSummary) throws IOException {
		if (!(dataSummary instanceof ExampleSetMetaData)) {
			// noop
			return;
		}
		new ExampleSetHdf5Writer((ExampleSetMetaData) dataSummary).write(path);
	}

	@Override
	public DataSummary deserialize(Path path) throws IOException {
		return Hdf5ExampleSetReader.readMetaData(path);
	}
}
