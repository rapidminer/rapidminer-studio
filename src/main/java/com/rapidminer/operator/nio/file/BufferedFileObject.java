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

import java.io.ByteArrayInputStream;
import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.nio.Buffer;

import com.rapidminer.operator.OperatorException;


/**
 * Simple implementation of a {@link FileObject} backed by a {@link Buffer}.
 *
 * @author Nils Woehler
 *
 */
public class BufferedFileObject extends FileObject {

	private static final long serialVersionUID = 1L;

	private byte[] buffer;
	private transient File file = null;

	public BufferedFileObject(byte[] buffer) {
		this.buffer = buffer;
	}

	@Override
	public ByteArrayInputStream openStream() {
		return new ByteArrayInputStream(buffer);
	}

	/**
	 * Returns the related file. If the file not exists, it creates a temporary file first.
	 */
	@Override
	public File getFile() throws OperatorException {
		if (file == null) {
			try {
				file = File.createTempFile("rm_file_", ".dump");
				try (FileOutputStream fos = new FileOutputStream(file)) {
					fos.write(this.buffer);
				}
				file.deleteOnExit();
			} catch (IOException e) {
				throw new OperatorException("303", e, file, e.getMessage());
			}
			return file;
		} else {
			return file;
		}
	}

	/**
	 * Returns the length of the used buffer.
	 */
	@Override
	public long getLength() throws OperatorException {
		return this.buffer.length;
	}

	@Override
	public String toString() {
		return file != null ? "Buffered file stored in temporary file: " + file.getAbsolutePath() : "Memory buffered file";
	}

	@Override
	protected void finalize() throws Throwable {
		if (file != null) {
			file.delete();
		}
		super.finalize();
	}

}
