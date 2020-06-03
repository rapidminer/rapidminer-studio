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
package com.rapidminer.repository;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.nio.file.Path;


/**
 * An entry which is of any type not directly known to RapidMiner Studio. It is treated simply as a stream of bytes, with no special meaning.
 * Not to be confused with the legacy {@link BlobEntry}.
 *
 * @author Marco Boeck
 * @since 9.7
 */
public interface BinaryEntry extends DataEntry {

    String TYPE_NAME = "external_file";


    @Override
    default String getType() {
        return TYPE_NAME;
    }

    /**
     * The suffix for this binary file, without the dot (e.g. {@code py}). See {@link
     * RepositoryTools#getSuffixFromFilename(String)}. Note that it can have any sort of capitalization, as this is
     * taken from the file system!
     *
     * @return the suffix, never {@code null} but can be empty if it is a file without as suffix
     */
    String getSuffix();

    /**
     * Opens a stream to read from this entry.
     * <p>
     * <strong>Attention:</strong> Do not forget to close this stream again in all cases once you are done!
     * </p>
     *
     * @throws RepositoryException if something goes wrong
     */
    InputStream openInputStream() throws RepositoryException;

    /**
     * Opens an output stream to write to this entry.
     * <p>
     * <strong>Attention:</strong> Do not forget to close this stream again in all cases once you are done!
     * </p>
     *
     * @return the stream, never {@code null}
     */
    OutputStream openOutputStream() throws RepositoryException;

    /**
     * A path to this entry.
     *
     * @return the path, never {@code null}
     * @throws IOException if this entry cannot be accessed with a path
     */
    Path toPath() throws IOException;
}