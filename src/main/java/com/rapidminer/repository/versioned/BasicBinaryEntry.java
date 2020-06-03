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

import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.nio.file.Path;

import org.apache.commons.io.IOUtils;

import com.rapidminer.repository.BinaryEntry;
import com.rapidminer.repository.MalformedRepositoryLocationException;
import com.rapidminer.repository.RepositoryException;
import com.rapidminer.repository.RepositoryLocation;
import com.rapidminer.repository.RepositoryLocationBuilder;
import com.rapidminer.tools.io.NotifyingOutputStreamWrapper;
import com.rapidminer.versioning.repository.DataSummary;
import com.rapidminer.versioning.repository.GeneralFile;
import com.rapidminer.versioning.repository.exceptions.DataRetrievalException;
import com.rapidminer.versioning.repository.exceptions.RepositoryFileException;
import com.rapidminer.versioning.repository.exceptions.RepositoryImmutableException;
import com.rapidminer.versioning.repository.exceptions.RepositoryNamingException;


/**
 * A binary entry, used if no other RapidMiner Entry type handles the files. More or less a unknown file with some
 * basic handling.
 *
 * @author Andreas Timm, Marco Boeck
 * @since 9.7
 */
public class BasicBinaryEntry extends BasicDataEntry<InputStream> implements BinaryEntry {


    BasicBinaryEntry(GeneralFile<?> entry, BasicFolder parent) {
        this(entry.getFullName(), parent);
    }

    protected BasicBinaryEntry(String name, BasicFolder parent) {
        super(name, parent, InputStream.class);
    }

    @Override
    public InputStream openInputStream() throws RepositoryException {
        try {
            return getParent().getRepository().load(this);
        } catch (Exception e) {
            throw new RepositoryException(e);
        }
    }

    /**
     * Binary entries cannot be retrieved like this, they need access via {@link #openInputStream()} and {@link
     * #openOutputStream()}! Calling this method will always throw an exception.
     *
     * @throws DataRetrievalException will always be thrown!
     */
    @Override
    public InputStream getData() throws DataRetrievalException {
        throw new DataRetrievalException(new UnsupportedOperationException("Binary entry cannot be retrieved directly!"));
    }

    /**
     * As opposed to the data entry, the binary entry needs its suffix!
     * {@inheritDoc}
     */
    @Override
    public RepositoryLocation getLocation() {
        try {
            String path = getPath();
            if (path.startsWith(String.valueOf(RepositoryLocation.SEPARATOR))) {
                path = path.substring(1);
            }
            return new RepositoryLocationBuilder().withFailIfDuplicateIOObjectExists(false).
                    withExpectedDataEntryType(getClass()).buildFromPathComponents(
                            getRepositoryAdapter().getName(), path.split(String.valueOf(RepositoryLocation.SEPARATOR)));
        } catch (MalformedRepositoryLocationException e) {
            // cannot happen
            throw new RuntimeException(e);
        }
    }

    /**
     * As opposed to the data entry, the binary entry does not need to add its suffix manually!
     * {@inheritDoc}
     */
    @Override
    public boolean rename(String newName) throws RepositoryException {
        try {
            getRepositoryAdapter().getGeneralRepository().renameFile(this, newName, true);
            return true;
        } catch (RepositoryImmutableException | RepositoryNamingException | RepositoryFileException e) {
            throw new RepositoryException(e);
        }
    }

    @Override
    public String getName() {
        // binary entries need the full name (prefix . suffix), as they're otherwise unidentifiable
        return getFullName();
    }

    @Override
    protected InputStream read(InputStream load) throws IOException {
        return load;
    }

    @Override
    protected void write(InputStream data) throws IOException, RepositoryImmutableException {
        try (OutputStream os = getOutputStream()) {
            IOUtils.copy(data, os);
        }
    }

    @Override
    public OutputStream openOutputStream() throws RepositoryException {
        try {
            return new NotifyingOutputStreamWrapper(getOutputStream(),
                    () -> getRepositoryAdapter().getGeneralRepository().update(this));
        } catch (FileNotFoundException | RepositoryImmutableException e) {
            throw new RepositoryException(e);
        }
    }

    @Override
    public Path toPath() {
        return getRepositoryAdapter().getRealPath(this);
    }

    @Override
    protected boolean checkDataSummary(DataSummary dataSummary) {
        return false;
    }
}
