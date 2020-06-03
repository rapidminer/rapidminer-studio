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
import java.io.OutputStream;

import com.rapidminer.example.ExampleSet;
import com.rapidminer.operator.IOObject;
import com.rapidminer.operator.Operator;
import com.rapidminer.operator.tools.IOObjectSerializer;
import com.rapidminer.repository.RepositoryException;
import com.rapidminer.tools.ProgressListener;
import com.rapidminer.versioning.repository.exceptions.RepositoryFileException;
import com.rapidminer.versioning.repository.exceptions.RepositoryImmutableException;

/**
 * The IOObject Entry for the versioned repository. Supports reading and writing an {@link IOObject} using the {@link com.rapidminer.operator.tools.IOObjectSerializer}
 *
 * @author Andreas Timm
 * @since 9.7
 */
public class BasicIOObjectEntry extends AbstractIOObjectEntry<IOObject> {

    /**
     * Create a new instance using {@link com.rapidminer.repository.Repository#createIOObjectEntry(String, IOObject, Operator, ProgressListener)}
     *
     * @param name     full filename of the file without a path: "foo.bar"
     * @param parent   {@link BasicFolder} is required
     * @param dataType class of the datatype this Entry contains
     */
    protected BasicIOObjectEntry(String name, BasicFolder parent, Class<IOObject> dataType) {
        super(name, parent, dataType);
    }

    @Override
    public synchronized Class<? extends IOObject> getObjectClass() {
        if (dataClass == null && getSize() > 0) {
            dataClass = IOObjectClassDetector.findClass(this);
        }
        return dataClass;
    }

    @Override
    protected void setIOObjectData(IOObject data) throws RepositoryFileException, RepositoryImmutableException,
            RepositoryException {
        if (data instanceof ExampleSet) {
            throw new RepositoryException("Data must not be ExampleSet!");
        }
        setData(data);
    }

    @Override
    protected IOObject read(InputStream load) throws IOException {
        Object deserialized = IOObjectSerializer.getInstance().deserialize(load);
        IOObject ioObject = (IOObject) deserialized;
        dataClass = ioObject.getClass();
        return ioObject;
    }

    @Override
    protected void write(IOObject data) throws IOException, RepositoryImmutableException {
        dataClass = data.getClass();

        try (OutputStream os = getOutputStream()) {
            IOObjectSerializer.getInstance().serialize(os, data);
        }
    }
}
