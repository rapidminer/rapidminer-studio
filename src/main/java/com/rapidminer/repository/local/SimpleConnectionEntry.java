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
package com.rapidminer.repository.local;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.FileReader;
import java.io.IOException;
import java.util.logging.Level;

import com.rapidminer.connection.ConnectionInformation;
import com.rapidminer.connection.ConnectionInformationContainerIOObject;
import com.rapidminer.connection.ConnectionInformationSerializer;
import com.rapidminer.connection.configuration.ConnectionConfiguration;
import com.rapidminer.operator.Annotations;
import com.rapidminer.operator.IOObject;
import com.rapidminer.operator.ports.metadata.ConnectionInformationMetaData;
import com.rapidminer.operator.ports.metadata.MetaData;
import com.rapidminer.repository.ConnectionEntry;
import com.rapidminer.repository.Entry;
import com.rapidminer.repository.RepositoryException;
import com.rapidminer.tools.LogService;


/**
 * The Repository {@link Entry} containing {@link ConnectionInformation}.
 *
 * @author Andreas Timm, Jan Czogalla
 * @since 9.3
 */
public class SimpleConnectionEntry extends SimpleIOObjectEntry implements ConnectionEntry {

	/** The .properties entry for the {@link com.rapidminer.connection.configuration.ConnectionConfiguration#getType connection type} */
	private static final String PROPERTY_CONNECTION_TYPE = "connection-type";
	/** The cached {@link com.rapidminer.connection.configuration.ConnectionConfiguration#getType connection type} */
	private String cachedConnectionType;

	/**
	 * Construct an instance to access the {@link ConnectionInformation}
	 *
	 * @param name
	 * 		filename of this {@link Entry}, used when reading and writing the {@link ConnectionInformation}, {@link MetaData} and {@link Annotations}
	 * @param containingFolder
	 * 		parent {@link com.rapidminer.repository.Folder}
	 * @param repository
	 * 		the {@link com.rapidminer.repository.Repository} this {@link Entry} belongs to
	 * @throws RepositoryException
	 * 		if initializing an empty {@link SimpleConnectionEntry} failed
	 */
	public SimpleConnectionEntry(String name, SimpleFolder containingFolder, LocalRepository repository) throws RepositoryException {
		super(name, containingFolder, repository);
	}

	@Override
	public String getSuffix() {
		return CON_SUFFIX;
	}


	@Override
	public String getDefaultDescription() {
		return "Connection entry.";
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
		// first try from properties file
		cachedConnectionType = getProperty(PROPERTY_CONNECTION_TYPE);
		if (cachedConnectionType != null) {
			return cachedConnectionType;
		}
		// if not yet defined, retrieve it from meta data and store in properties
		try {
			cachedConnectionType = ((ConnectionInformationMetaData) retrieveMetaData()).getConnectionType();
			if (cachedConnectionType != null) {
				putProperty(PROPERTY_CONNECTION_TYPE, cachedConnectionType);
			}
			return cachedConnectionType;
		} catch (RepositoryException e) {
			return null;
		}
	}

	@Override
	protected String getMetaDataSuffix() {
		return CON_MD_SUFFIX;
	}

	@Override
	protected void writeDataToFile(IOObject data, FileOutputStream fos) throws IOException, RepositoryException {
		if (data instanceof ConnectionInformationContainerIOObject) {
			ConnectionInformation connectionInformation = ((ConnectionInformationContainerIOObject) data).getConnectionInformation();
			ConnectionInformationSerializer.LOCAL.serialize(connectionInformation, fos);
		} else {
			throw new IOException("Mismatched IOObject, expected connection but was " + data.getClass());
		}
	}

	@Override
	protected void writeMetaDataToFile(MetaData md, FileOutputStream fos) throws IOException {
		if (md instanceof ConnectionInformationMetaData) {
			ConnectionConfiguration configuration = ((ConnectionInformationMetaData) md).getConfiguration();
			ConnectionInformationSerializer.LOCAL.writeJson(fos, configuration);
		}
	}

	@Override
	protected MetaData readMetaDataObject(File metaDataFile) throws IOException, ClassNotFoundException {
		try (FileReader fr = new FileReader(getMetaDataFile())) {
			return new ConnectionInformationMetaData(ConnectionInformationSerializer.LOCAL.loadConfiguration(fr));
		}
	}

	@Override
	protected IOObject readDataFromFile(FileInputStream fis) throws IOException {
		return new ConnectionInformationContainerIOObject(
				ConnectionInformationSerializer.LOCAL.loadConnection(fis, getLocation()));
	}

	@Override
	protected void checkMetaDataFile() {
		final File metaDataFile = getMetaDataFile();
		if (!metaDataFile.exists()) {
			IOObject data;
			try {
				data = retrieveData(null);
			} catch (RepositoryException e) {
				// data is not there anymore, we can ignore this and also not need to care about MD anymore now
				return;
			}

			try {
				MetaData md = MetaData.forIOObject(data);
				// Save MetaData
				try (FileOutputStream fos = new FileOutputStream(metaDataFile)) {
					writeMetaDataToFile(md, fos);
				}
			} catch (IOException e) {
				LogService.getRoot().log(Level.WARNING, "Cannot write meta data to '" + metaDataFile + "': " + e, e);
			}
		}
	}
}
