/**
 * Copyright (C) 2001-2016 by RapidMiner and the contributors
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

import com.rapidminer.repository.BlobEntry;
import com.rapidminer.repository.Folder;
import com.rapidminer.repository.RepositoryException;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;


/**
 * Reference on BLOB entries in the repository.
 * 
 * @author Simon Fischer
 */
public class SimpleBlobEntry extends SimpleDataEntry implements BlobEntry {

	private static final String BLOB_SUFFIX = ".blob";

	SimpleBlobEntry(String name, SimpleFolder containingFolder, LocalRepository localRepository) throws RepositoryException {
		super(name, containingFolder, localRepository);
		// create physical file here, otherwise it will not really exist and for example cause
		// errors in the Binary Import Wizard
		if (!getFile().exists()) {
			try {
				getFile().createNewFile();
			} catch (IOException e) {
				throw new RepositoryException(e.getMessage());
			}
		}
	}

	private File getFile() {
		return new File(((SimpleFolder) getContainingFolder()).getFile(), getName() + BLOB_SUFFIX);
	}

	@Override
	public long getDate() {
		return getFile().lastModified();
	}

	@Override
	public long getSize() {
		return getFile().length();
	}

	@Override
	public void delete() throws RepositoryException {
		getFile().delete();
		super.delete();
	}

	@Override
	protected void handleRename(String newName) throws RepositoryException {
		renameFile(getFile(), newName);
	}

	@Override
	protected void handleMove(Folder newParent, String newName) throws RepositoryException {
		moveFile(getFile(), ((SimpleFolder) newParent).getFile(), newName, BLOB_SUFFIX);
	}

	@Override
	public String getType() {
		return BlobEntry.TYPE_NAME;
	}

	@Override
	public String getMimeType() {
		return getProperty("mimetype");
	}

	@Override
	public InputStream openInputStream() throws RepositoryException {
		try {
			return new FileInputStream(getFile());
		} catch (FileNotFoundException e) {
			throw new RepositoryException("Cannot open stream from '" + getFile() + "': " + e, e);
		}
	}

	@Override
	public OutputStream openOutputStream(String mimeType) throws RepositoryException {
		putProperty("mimetype", mimeType);
		try {
			return new FileOutputStream(getFile());
		} catch (IOException e) {
			throw new RepositoryException("Cannot open stream from '" + getFile() + "': " + e, e);
		}
	}
}
