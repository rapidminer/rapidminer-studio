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
import java.io.InputStream;
import java.io.OutputStream;
import java.util.Properties;
import java.util.logging.Level;
import java.util.zip.ZipEntry;
import java.util.zip.ZipInputStream;

import com.rapidminer.repository.BlobEntry;
import com.rapidminer.repository.RepositoryException;
import com.rapidminer.tools.LogService;


/**
 * Class for a BlobEntry that is associated with a {@link ZipStreamResource}.
 *
 * @author Marcel Michel
 * @since 7.0.0
 */
public class ZipResourceBlobEntry extends ResourceDataEntry implements BlobEntry {

	private final ZipStreamResource zipStream;

	private String mimeType;

	protected ZipResourceBlobEntry(ResourceFolder parent, String name, String resource, ResourceRepository repository,
			ZipStreamResource zipStream) {
		super(parent, name, resource, repository);
		this.zipStream = zipStream;
	}

	@Override
	public String getType() {
		return BlobEntry.TYPE_NAME;
	}

	@Override
	public String getDescription() {
		return getName();
	}

	@Override
	public InputStream openInputStream() throws RepositoryException {
		try {
			ZipInputStream zip = zipStream.getStream();
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
				if ((getName() + ".blob").equals(entryName)) {
					return zip;
				}
			}
			throw new RepositoryException("Missing resource: " + getResource() + ".blob");
		} catch (IOException e) {
			throw new RepositoryException("IO error reading " + getResource() + ": " + e.getMessage());
		}
	}

	@Override
	public OutputStream openOutputStream(String mimeType) throws RepositoryException {
		throw new RepositoryException("Repository is read only.");
	}

	@Override
	public String getMimeType() {
		if (mimeType == null) {
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
					if ((getName() + ".properties").equals(entryName)) {
						Properties props = new Properties();
						props.loadFromXML(zip);
						mimeType = props.getProperty("mimetype");
						break;
					}
				}
			} catch (IOException | RepositoryException e) {
				LogService.getRoot().log(Level.WARNING,
						"com.rapidminer.repository.resource.ZipResourceBlob.failed_to_load_mimetype", getName());
				return null;
			}
		}
		return mimeType;
	}

}
