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

import com.rapidminer.connection.ConnectionInformation;
import com.rapidminer.connection.ConnectionInformationContainerIOObject;
import com.rapidminer.connection.ConnectionInformationSerializer;
import com.rapidminer.operator.IOObject;
import com.rapidminer.operator.ports.metadata.ConnectionInformationMetaData;
import com.rapidminer.operator.ports.metadata.MetaData;
import com.rapidminer.repository.ConnectionEntry;
import com.rapidminer.repository.RepositoryException;


/**
 * Resource based {@link com.rapidminer.connection.ConnectionInformation}, read only
 *
 * @author Andreas Timm
 * @since 9.3
 */
public class ResourceConnectionEntry extends ResourceIOObjectEntry implements ConnectionEntry {

	ResourceConnectionEntry(ResourceFolder parent, String name, String resource, ResourceRepository repository) {
		super(parent, name, resource, repository);
	}

	@Override
	protected String getSuffix() {
		return CON_SUFFIX;
	}

	@Override
	protected String getMetaDataSuffix() {
		return CON_MD_SUFFIX;
	}

	@Override
	protected IOObject readDataObject(InputStream in) throws IOException {
		return new ConnectionInformationContainerIOObject(
				ConnectionInformationSerializer.LOCAL.loadConnection(in, getLocation()));
	}

	@Override
	protected MetaData readMetaDataObject(InputStream in) throws IOException {
		return new ConnectionInformationMetaData(ConnectionInformationSerializer.LOCAL.loadConfiguration(in));
	}

	@Override
	public void storeConnectionInformation(ConnectionInformation connectionInformation) throws RepositoryException {
		throw new RepositoryException("This is a read-only sample connection entry. Cannot store connection here.");
	}

	@Override
	public String getConnectionType() {
		try {
			return ((ConnectionInformationMetaData) retrieveMetaData()).getConnectionType();
		} catch (RepositoryException e) {
			return null;
		}
	}
}
