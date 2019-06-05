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
import java.io.InputStream;
import java.io.OutputStream;
import java.util.Properties;
import java.util.logging.Level;

import com.rapidminer.repository.BlobEntry;
import com.rapidminer.repository.RepositoryException;
import com.rapidminer.tools.LogService;


/**
 * A read-only resource based {@link BlobEntry}.
 *
 * @author Jan Czogalla
 * @since 9.0
 */
public class ResourceBlobEntry extends ResourceDataEntry implements BlobEntry {

	private String mimeType;

	protected ResourceBlobEntry(ResourceFolder parent, String name, String resource, ResourceRepository repository) {
		super(parent, name, resource, repository);
	}

	@Override
	public InputStream openInputStream() throws RepositoryException {
		return getResourceStream(BLOB_SUFFIX);
	}

	@Override
	public OutputStream openOutputStream(String mimeType) throws RepositoryException {
		throw new RepositoryException("Repository is read only.");
	}

	@Override
	public String getMimeType() {
		if (mimeType == null) {
			try (InputStream in = getResourceStream(PROPERTIES_SUFFIX)) {
				Properties props = new Properties();
				props.loadFromXML(in);
				mimeType = props.getProperty("mimetype");
			} catch (IOException | RepositoryException e) {
				LogService.getRoot().log(Level.WARNING,
						"com.rapidminer.repository.resource.ZipResourceBlob.failed_to_load_mimetype", getName());
			}
		}
		return mimeType;
	}

	@Override
	public String getDescription() {
		return getName();
	}
}
