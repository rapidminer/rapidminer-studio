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
package com.rapidminer.repository.local;

import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;

import com.rapidminer.repository.BlobEntry;
import com.rapidminer.repository.RepositoryException;


/**
 * Reference on BLOB entries in the repository.
 * 
 * @author Simon Fischer, Jan Czogalla
 */
public class SimpleBlobEntry extends SimpleDataEntry implements BlobEntry {

	public SimpleBlobEntry(String name, SimpleFolder containingFolder, LocalRepository localRepository) throws RepositoryException {
		super(name, containingFolder, localRepository);
		// create physical file here, otherwise it will not really exist and for example cause
		// errors in the Binary Import Wizard
		if (!getDataFile().exists()) {
			try {
				getDataFile().createNewFile();
			} catch (IOException e) {
				throw new RepositoryException(e);
			}
		}
	}

	@Override
	public String getSuffix() {
		return BLOB_SUFFIX;
	}

	@Override
	public String getMimeType() {
		return getProperty("mimetype");
	}

	@Override
	public InputStream openInputStream() throws RepositoryException {
		try {
			return new FileInputStream(getDataFile());
		} catch (FileNotFoundException e) {
			throw new RepositoryException("Cannot open stream from '" + getDataFile() + "': " + e, e);
		}
	}

	@Override
	public OutputStream openOutputStream(String mimeType) throws RepositoryException {
		putProperty("mimetype", mimeType);
		try {
			return new FileOutputStream(getDataFile());
		} catch (IOException e) {
			throw new RepositoryException("Cannot open stream from '" + getDataFile() + "': " + e, e);
		}
	}
}
