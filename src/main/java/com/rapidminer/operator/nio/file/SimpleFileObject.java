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
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.InputStream;

import com.rapidminer.operator.OperatorException;


/**
 * Simple implementation of a {@link FileObject} backed by a {@link File}.
 *
 * @author Nils Woehler
 *
 */
public class SimpleFileObject extends FileObject {

	private static final long serialVersionUID = 1L;

	private File file;

	public SimpleFileObject(File file) {
		super();
		this.file = file;
	}

	@Override
	public InputStream openStream() throws OperatorException {
		try {
			return new FileInputStream(file);
		} catch (FileNotFoundException e) {
			throw new OperatorException("301", e, file);
		}
	}

	@Override
	public File getFile() {
		return file;
	}

	/**
	 * Returns the size of the related file in number of bytes. Returns 0, if the file does not
	 * exist.
	 */
	@Override
	public long getLength() throws OperatorException {
		return file.length();
	}

	@Override
	public String toString() {
		return "File: " + getFile().getAbsolutePath();
	}

}
