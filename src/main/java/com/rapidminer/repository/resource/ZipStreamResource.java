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
package com.rapidminer.repository.resource;

import java.io.IOException;
import java.util.zip.ZipInputStream;

import com.rapidminer.repository.Repository;
import com.rapidminer.repository.RepositoryException;


/**
 * Resources in the zip file format which can be used inside the {@link Repository}.
 *
 * @author Marcel Michel
 * @since 7.0.0
 */
public interface ZipStreamResource {

	/**
	 * The title of the resource.
	 *
	 * @return a human readable title
	 */
	public String getTitle();

	/**
	 * The description of the resource.
	 *
	 * @return a human readable description
	 */
	public String getDescription();

	/**
	 * The root path inside the zip resource.
	 *
	 * @return a path as {@link String} (e.g. subfolder/) or {@code null}
	 */
	public String getStreamPath();

	/**
	 * The input stream of the ZIP resource.
	 *
	 * @return the stream as {@link ZipInputStream}
	 * @throws IOException
	 *             if resource cannot loaded
	 * @throws RepositoryException
	 *             if resource cannot loaded
	 */
	public ZipInputStream getStream() throws IOException, RepositoryException;
}
