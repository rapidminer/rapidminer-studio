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
package com.rapidminer.repository.resource;

import java.io.IOException;
import java.util.zip.ZipEntry;
import java.util.zip.ZipInputStream;

import com.rapidminer.repository.Repository;
import com.rapidminer.repository.RepositoryException;


/**
 * Resources in the zip file format which can be used inside the {@link Repository}.
 *
 * @author Marcel Michel, Jan Czogalla
 * @since 7.0.0
 */
public interface ZipStreamResource {

	/**
	 * The title of the resource.
	 *
	 * @return a human readable title
	 */
	String getTitle();

	/**
	 * The description of the resource.
	 *
	 * @return a human readable description
	 */
	String getDescription();

	/**
	 * The root path inside the zip resource.
	 *
	 * @return a path as {@link String} (e.g. subfolder/) or {@code null}
	 */
	String getStreamPath();

	/**
	 * The input stream of the ZIP resource.
	 *
	 * @return the stream as {@link ZipInputStream}
	 * @throws IOException
	 *             if resource cannot loaded
	 * @throws RepositoryException
	 *             if resource cannot loaded
	 */
	ZipInputStream getStream() throws IOException, RepositoryException;

	/**
	 * Searches this {@link ZipStreamResource} for the given entry with the specified suffix. Will return a {@link ZipInputStream}
	 * to that entry if it exists. Will throw a {@link RepositoryException} if the entry can not be found or an error occurs
	 *
	 * @param searchedEntry
	 * 		the entry name to search for
	 * @param resource
	 * 		the full resource name for if an error occurs
	 * @param suffix
	 * 		the suffix of the entry to search
	 * @return the input stream of the found {@link ZipEntry}
	 * @throws RepositoryException
	 * 		if any error occurs or the entry can not be found
	 * @since 9.0
	 */
	default ZipInputStream getStream(String searchedEntry, String resource, String suffix) throws RepositoryException {
		searchedEntry += suffix;
		resource += suffix;
		try {
			ZipInputStream zip = getStream();
			ZipEntry entry;
			String streamPath = getStreamPath();
			boolean nonNullStreamPath = streamPath != null;
			while ((entry = zip.getNextEntry()) != null) {
				String name = entry.getName();
				if (entry.isDirectory()
						|| name.replaceFirst("/", "").contains("/")
						|| nonNullStreamPath && !name.startsWith(streamPath)) {
					continue;
				}
				String entryName = name;
				if (nonNullStreamPath) {
					entryName = entryName.replaceFirst(streamPath, "");
				}
				if (searchedEntry.equals(entryName)) {
					return zip;
				}
			}
			throw new RepositoryException("Missing resource: '" + resource + "'");
		} catch (IOException e) {
			throw new RepositoryException("IO error reading '" + resource + "': " + e.getMessage(), e);
		}
	}
}
