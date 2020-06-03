/**
 * Copyright (C) 2001-2020 by RapidMiner and the contributors
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
package com.rapidminer.repository.versioned;

import java.io.IOException;
import java.io.InputStream;
import java.nio.file.Path;

import com.rapidminer.example.ExampleSet;
import com.rapidminer.operator.IOObject;
import com.rapidminer.operator.Operator;
import com.rapidminer.operator.ports.metadata.ExampleSetMetaData;
import com.rapidminer.operator.ports.metadata.MetaData;
import com.rapidminer.repository.RepositoryException;
import com.rapidminer.storage.hdf5.ExampleSetHdf5Writer;
import com.rapidminer.storage.hdf5.Hdf5ExampleSetReader;
import com.rapidminer.tools.ProgressListener;
import com.rapidminer.versioning.repository.DataSummary;
import com.rapidminer.versioning.repository.exceptions.RepositoryFileException;
import com.rapidminer.versioning.repository.exceptions.RepositoryImmutableException;


/**
 * The ExampleSet Entry for the versioned repository. Supports reading and writing an {@link ExampleSet} using the
 * {@link Hdf5ExampleSetReader} and {@link ExampleSetHdf5Writer}.
 *
 * @author Gisa Meier
 * @since 9.7
 */
public class BasicExampleSetEntry extends AbstractIOObjectEntry<ExampleSet> {

    /**
     * Create a new instance using {@link com.rapidminer.repository.Repository#createIOObjectEntry(String, IOObject,
     * Operator, ProgressListener)}
     *
     * @param name
     * 		full filename of the file without a path: "foo.bar"
     * @param parent
     *        {@link BasicFolder} is required
     */
    protected BasicExampleSetEntry(String name, BasicFolder parent) {
        super(name, parent, ExampleSet.class);
        dataClass = getDataType();
    }

    @Override
    protected ExampleSet read(InputStream load) throws IOException {
        Path filePath = getParent().getRepository().getFilePath(this);
        return Hdf5ExampleSetReader.read(filePath);
    }

    @Override
    protected void write(ExampleSet exampleSet) throws IOException, RepositoryImmutableException {
        Path filePath = getParent().getRepository().getFilePath(this);
        new ExampleSetHdf5Writer(exampleSet).write(filePath);
    }

    @Override
    protected void setIOObjectData(IOObject data) throws RepositoryFileException, RepositoryImmutableException,
            RepositoryException {
        if (!(data instanceof ExampleSet)) {
            throw new RepositoryException("Data must be ExampleSet!");
        }
        setData((ExampleSet) data);
    }

    /** Checks whether the given {@link DataSummary} is {@link MetaData} compatible with {@link ExampleSet} */
    @Override
    protected boolean checkDataSummary(DataSummary dataSummary) {
        if (!(dataSummary instanceof MetaData)) {
            return false;
        }
        return dataSummary instanceof ExampleSetMetaData
                || ExampleSet.class.isAssignableFrom(((MetaData) dataSummary).getObjectClass());
    }
}
