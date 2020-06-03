/**
 * Copyright (C) 2001-2020 by RapidMiner and the contributors
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
package com.rapidminer.repository.versioned;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;

import com.rapidminer.connection.ConnectionInformation;
import com.rapidminer.connection.ConnectionInformationContainerIOObject;
import com.rapidminer.connection.ConnectionInformationSerializer;
import com.rapidminer.connection.configuration.ConnectionConfiguration;
import com.rapidminer.operator.IOObject;
import com.rapidminer.operator.Operator;
import com.rapidminer.operator.ports.metadata.ConnectionInformationMetaData;
import com.rapidminer.operator.ports.metadata.MetaData;
import com.rapidminer.repository.ConnectionEntry;
import com.rapidminer.repository.Entry;
import com.rapidminer.repository.RepositoryException;
import com.rapidminer.tools.ProgressListener;
import com.rapidminer.versioning.repository.DataSummary;
import com.rapidminer.versioning.repository.exceptions.DataRetrievalException;
import com.rapidminer.versioning.repository.exceptions.RepositoryFileException;
import com.rapidminer.versioning.repository.exceptions.RepositoryImmutableException;


/**
 * The Repository {@link Entry} containing {@link ConnectionInformation}.
 *
 * @author Marco Boeck
 * @since 9.7
 */
public class BasicConnectionEntry extends BasicDataEntry<ConnectionInformation> implements ConnectionEntry {

	/** The cached {@link ConnectionConfiguration#getType connection type} */
	private String cachedConnectionType;


	/**
	 * Create a new instance using {@link com.rapidminer.repository.Repository#createConnectionEntry(String, ConnectionInformation)}
	 *
	 * @param name     full filename of the file without a path: "db.conninfo"
	 * @param parent   {@link BasicFolder} is required
	 */
	public BasicConnectionEntry(String name, BasicFolder parent) {
		super(name, parent, ConnectionInformation.class);
	}

	@Override
	public void storeConnectionInformation(ConnectionInformation connectionInformation) throws RepositoryException {
		if (connectionInformation == null) {
			throw new RepositoryException("No connection provided for storing");
		}
		storeData(new ConnectionInformationContainerIOObject(connectionInformation), null, null);
	}

	@Override
	public String getConnectionType() {
		if (cachedConnectionType != null) {
			return cachedConnectionType;
		}
		// if not yet defined, retrieve it from meta data and store in properties
		try {
			ConnectionInformationMetaData metaData = (ConnectionInformationMetaData) retrieveMetaData();
			if (metaData != null) {
				cachedConnectionType = metaData.getConnectionType();
			}
			return cachedConnectionType;
		} catch (RepositoryException e) {
			return null;
		}
	}

	@Override
	public IOObject retrieveData(ProgressListener l) throws RepositoryException {
		if (l != null) {
			l.setTotal(100);
			l.setCompleted(10);
		}
		try {
			return new ConnectionInformationContainerIOObject(getData());
		} catch (DataRetrievalException e) {
			throw new RepositoryException("Cannot load data from '" + getName() + "': " + e, e);
		}
	}

	/** Same as {@link #retrieveDataSummary()} */
	@Override
	public MetaData retrieveMetaData() throws RepositoryException {
		return (MetaData) retrieveDataSummary();
	}

	/** Checks whether the given {@link DataSummary} is {@link MetaData} compatible with {@link ConnectionInformation} */
	@Override
	protected boolean checkDataSummary(DataSummary dataSummary) {
		return dataSummary instanceof ConnectionInformationMetaData;
	}

	@Override
	public Class<? extends IOObject> getObjectClass() {
		return ConnectionInformationContainerIOObject.class;
	}

	@Override
	public void storeData(IOObject data, Operator callingOperator, ProgressListener l) throws RepositoryException {
		if (data == null) {
			throw new RepositoryException("ConnectionInformationContainerIOObject to store must not be null!");
		}
		if (!(data instanceof ConnectionInformationContainerIOObject)) {
			throw new RepositoryException("Can only store ConnectionInformationContainerIOObject!");
		}
		if (l != null) {
			l.setTotal(100);
			l.setCompleted(10);
		}

		try {
			setData(((ConnectionInformationContainerIOObject) data).getConnectionInformation());
		} catch (RepositoryFileException | RepositoryImmutableException e) {
			throw new RepositoryException(e);
		}
		if (l != null) {
			l.complete();
		}
	}

	@Override
	protected ConnectionInformation read(InputStream load) throws IOException {
		return ConnectionInformationSerializer.INSTANCE.loadConnection(load, getLocation(), getRepositoryAdapter().getEncryptionContext());
	}

	@Override
	protected void write(ConnectionInformation data) throws IOException, RepositoryImmutableException {
		try (OutputStream os = getOutputStream()) {
			ConnectionInformationSerializer.INSTANCE.serialize(data, os, getRepositoryAdapter().getEncryptionContext());
		}
	}
}
