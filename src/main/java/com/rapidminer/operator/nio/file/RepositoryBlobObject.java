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
package com.rapidminer.operator.nio.file;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;

import com.rapidminer.operator.OperatorException;
import com.rapidminer.repository.BlobEntry;
import com.rapidminer.repository.Entry;
import com.rapidminer.repository.RepositoryException;
import com.rapidminer.repository.RepositoryLocation;
import com.rapidminer.tools.Tools;


/**
 *
 * Simple implementation of a {@link FileObject} backed by a {@link RepositoryLocation}. The
 * repository entry has to be of type 'blob'.
 *
 * @author Nils Woehler, Marius Helf
 *
 */
public class RepositoryBlobObject extends FileObject {

	private static final long serialVersionUID = 1L;

	private RepositoryLocation location;
	private File file = null;

	public RepositoryBlobObject(RepositoryLocation location) {
		super();
		this.location = location;
	}

	@Override
	public InputStream openStream() throws OperatorException {
		BlobEntry blobEntry;
		try {
			Entry entry = location.locateEntry();
			if (entry != null) {
				if (entry instanceof BlobEntry) {
					blobEntry = (BlobEntry) entry;
					return blobEntry.openInputStream();
				} else {
					throw new OperatorException("942", null, location.getAbsoluteLocation(), "blob", entry.getType());
				}
			} else {
				throw new OperatorException("312", null, location.getAbsoluteLocation(), "entry does not exist");
			}
		} catch (RepositoryException e) {
			throw new OperatorException("319", e, location.getAbsoluteLocation());
		}

	}

	@Override
	public File getFile() throws OperatorException {
		if (file == null) {
			try {
				file = File.createTempFile("rm_file_", ".dump");
				FileOutputStream fos = new FileOutputStream(file);
				BlobEntry blobEntry;
				try {
					Entry entry = location.locateEntry();
					if (entry != null) {
						if (entry instanceof BlobEntry) {
							blobEntry = (BlobEntry) entry;
						} else {
							throw new OperatorException("942", null, location.getAbsoluteLocation(), "blob", entry.getType());
						}
					} else {
						throw new OperatorException("312", null, location.getAbsoluteLocation(), "entry does not exist");
					}

					InputStream in = blobEntry.openInputStream();
					try {
						Tools.copyStreamSynchronously(in, fos, true);
						file.deleteOnExit();
					} finally {
						in.close();
					}
				} finally {
					fos.close();
				}
			} catch (IOException e) {
				throw new OperatorException("303", e, file, e.getMessage());
			} catch (RepositoryException e) {
				throw new OperatorException("319", e, location.getAbsoluteLocation());
			}
		}
		return file;
	}

	/**
	 * Firstly, this method calls {@link #getFile()}. As the file usually exists in the repository,
	 * it is simply accessed. If it does not exist, a temporary file is created. Secondly, the
	 * length of the file is returned.
	 */
	@Override
	public long getLength() throws OperatorException {
		// There is no easier way to receive the length, as the underlying BlobEntry only supports
		// an InputStream
		return getFile().length();
	}

	@Override
	public String toString() {
		return file != null ? "Repository location stored in temporary file: " + file.getAbsolutePath()
				: "Repository location: " + location.getAbsoluteLocation();
	}

	@Override
	protected void finalize() throws Throwable {
		if (file != null) {
			file.delete();
		}
		super.finalize();
	}

}
