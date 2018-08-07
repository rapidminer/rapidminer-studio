/**
 * Copyright (C) 2001-2018 by RapidMiner and the contributors
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
import java.nio.file.Paths;
import java.util.List;
import java.util.zip.ZipEntry;
import java.util.zip.ZipInputStream;

import com.rapidminer.repository.DataEntry;
import com.rapidminer.repository.Folder;
import com.rapidminer.repository.RepositoryException;


/**
 * Class for a folder in the repository that is associated with a {@link ZipStreamResource} and
 * contains the process and input data for this template.
 *
 * @author Gisa Schaefer, Marcel Michel, Jan Czogalla
 * @since 7.0.0
 */
public class ZipResourceFolder extends ResourceFolder {

	private final ZipStreamResource zipStream;

	protected ZipResourceFolder(ResourceFolder parent, String name, ZipStreamResource zipStream, String parentPath,
			ResourceRepository repository) {
		super(parent, name, parentPath + "/" + name, repository);
		this.zipStream = zipStream;
	}

	@Override
	protected void ensureLoaded(List<Folder> folders, List<DataEntry> data) throws RepositoryException {
		try (ZipInputStream zip = zipStream.getStream()) {
			ZipEntry entry;
			while ((entry = zip.getNextEntry()) != null) {
				if (entry.isDirectory() || entry.getName().replaceFirst("/", "").contains("/")) {
					continue;
				}
				if (zipStream.getStreamPath() != null && !entry.getName().startsWith(zipStream.getStreamPath())) {
					continue;
				}
				String entryName = entry.getName();
				String dataEntryName = Paths.get(entryName).getFileName().toString().split("\\.")[0];
				if (entryName.endsWith(".ioo")) {
					data.add(new ZipResourceIOObjectEntry(this, dataEntryName, getPath() + "/" + dataEntryName,
							getRepository(), zipStream));
				} else if (entryName.endsWith(".rmp")) {
					data.add(new ZipResourceProcessEntry(this, dataEntryName, getPath() + "/" + dataEntryName,
							getRepository(), zipStream));
				} else if (entryName.endsWith(".blob")) {
					data.add(new ZipResourceBlobEntry(this, dataEntryName, getPath() + "/" + dataEntryName,
							getRepository(), zipStream));
				}
			}
		} catch (IOException e) {
			throw new RepositoryException("Cannot load data from '" + getResource() + ": " + e, e);
		}
	}
}
