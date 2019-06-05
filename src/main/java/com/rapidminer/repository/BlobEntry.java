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
package com.rapidminer.repository;

import java.io.InputStream;
import java.io.OutputStream;


/**
 * A byte blob with no specified contents.
 * 
 * @author Simon Fischer
 * */
public interface BlobEntry extends DataEntry {

	String TYPE_NAME = "blob";
	String BLOB_SUFFIX = ".blob";

	@Override
	default String getType() {
		return TYPE_NAME;
	}

	/**
	 * Opens a stream to read from this entry.
	 *
	 * @throws RepositoryException
	 */
	InputStream openInputStream() throws RepositoryException;

	/**
	 * Opens a stream to this blob, setting its mime type to the given value.
	 *
	 * @return TODO
	 */
	OutputStream openOutputStream(String mimeType) throws RepositoryException;

	String getMimeType();

}
