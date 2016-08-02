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
import java.util.zip.ZipEntry;
import java.util.zip.ZipInputStream;

import com.rapidminer.repository.RepositoryException;
import com.rapidminer.tools.Tools;


/**
 * Class for a process that is associated with a {@link ZipStreamResource}.
 *
 * @author Gisa Schaefer, Marcel Michel
 * @since 7.0.0
 */
public class ZipResourceProcessEntry extends ResourceProcessEntry {

	private final ZipStreamResource zipStream;

	protected ZipResourceProcessEntry(ResourceFolder parent, String name, String resource, ResourceRepository repository,
			ZipStreamResource zipStream) {
		super(parent, name, resource, repository);
		this.zipStream = zipStream;
	}

	@Override
	public String retrieveXML() throws RepositoryException {
		try (ZipInputStream zip = zipStream.getStream()) {
			ZipEntry entry;
			while ((entry = zip.getNextEntry()) != null) {
				if (entry.isDirectory() || entry.getName().replaceFirst("/", "").contains("/")) {
					continue;
				}
				if (zipStream.getStreamPath() != null && !entry.getName().startsWith(zipStream.getStreamPath())) {
					continue;
				}
				String entryName;
				if (zipStream.getStreamPath() != null) {
					entryName = entry.getName().replaceFirst(zipStream.getStreamPath(), "");
				} else {
					entryName = entry.getName();
				}
				if ((getName() + ".rmp").equals(entryName)) {
					return Tools.readTextFile(zip);
				}
			}
			throw new RepositoryException("Missing resource: " + getResource() + ".rmp");
		} catch (IOException e) {
			throw new RepositoryException("IO error reading " + getResource() + ": " + e.getMessage());
		}

	}

	@Override
	public String getDescription() {
		return zipStream.getDescription();
	}

}
