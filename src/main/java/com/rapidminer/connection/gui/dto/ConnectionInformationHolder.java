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
package com.rapidminer.connection.gui.dto;

import java.io.IOException;

import com.rapidminer.connection.ConnectionHandlerRegistry;
import com.rapidminer.connection.ConnectionInformation;
import com.rapidminer.connection.ConnectionInformationBuilder;
import com.rapidminer.connection.ConnectionInformationContainerIOObject;
import com.rapidminer.connection.configuration.ConnectionConfigurationBuilder;
import com.rapidminer.repository.Repository;
import com.rapidminer.tools.ValidationUtil;
import com.rapidminer.repository.ConnectionEntry;
import com.rapidminer.repository.RepositoryException;
import com.rapidminer.repository.RepositoryLocation;
import com.rapidminer.tools.PasswordInputCanceledException;


/**
 * Wrapper object containing the connection information and its location.
 *
 * @author Jonas Wilms-Pfau
 * @since 9.3
 */
public final class ConnectionInformationHolder {

	private final RepositoryLocation location;
	private final ConnectionInformation connection;
	private final ConnectionInformation original;
	private final boolean isEditable;
	private final String connectionType;

	/**
	 * Creates a new ConnectionInformationHolder
	 *
	 * @param connection
	 * 		the connection
	 * @param location
	 * 		the location of the connection
	 * @param isEditable
	 * 		if the connection is editable
	 * @throws IOException
	 * 		in case the connection could not be copied
	 */
	private ConnectionInformationHolder(ConnectionInformation connection, RepositoryLocation location, boolean isEditable) throws IOException {
		this(connection, null, location, isEditable);
	}

	/**
	 * Creates a new ConnectionInformationHolder
	 *
	 * @param connection
	 * 		the connection
	 * @param name
	 * 		the name of the connection
	 * @param location
	 * 		the location of the connection
	 * @param isEditable
	 * 		if the connection is editable
	 * @throws IOException
	 * 		in case the connection could not be copied
	 */
	private ConnectionInformationHolder(ConnectionInformation connection, String name, RepositoryLocation location, boolean isEditable) throws IOException {
		ValidationUtil.requireNonNull(connection, "connection");
		ValidationUtil.requireNonNull(location, "repository location");
		this.location = location;

		ConnectionInformationBuilder builder;
		if (name != null) {
			builder = new ConnectionInformationBuilder(connection)
					.updateConnectionConfiguration(new ConnectionConfigurationBuilder(connection.getConfiguration(), name).build());
		} else {
			builder = new ConnectionInformationBuilder(connection);
		}
		try {
			// set repo for conn info
			Repository repository = location.getRepository();
			builder.inRepository(repository);
			connection = new ConnectionInformationBuilder(connection).inRepository(repository).build();
		} catch (RepositoryException e) {
			// ignore
		}
		this.connection = connection;
		this.original = builder.build();
		this.isEditable = isEditable;
		this.connectionType = connection.getConfiguration().getType();
	}

	/**
	 * @return the stored connection information
	 */
	public ConnectionInformation getConnectionInformation() {
		return connection;
	}

	/**
	 * @return the location of the connection
	 */
	public RepositoryLocation getLocation() {
		return location;
	}

	/**
	 * @return {@code true} if the connection has changed
	 */
	public boolean hasChanged(ConnectionInformation connection) {
		return !original.equals(connection);
	}

	/**
	 * @return {@code true} if the connection is editable
	 */
	public boolean isEditable() {
		return isEditable;
	}

	/**
	 * @return the connection type
	 */
	public String getConnectionType() {
		return connectionType;
	}

	/**
	 * Creates a new ConnectionInformationHolder from a {@link ConnectionEntry}
	 *
	 * @param entry
	 * 		The connection entry
	 * @return a newly created ConnectionInformationHolder
	 * @throws RepositoryException
	 * 		in case the retrieval of the connection failed
	 * @throws IOException
	 * 		in case the connection could not be copied
	 */
	public static ConnectionInformationHolder from(ConnectionEntry entry) throws RepositoryException, IOException, PasswordInputCanceledException {
		return new ConnectionInformationHolder(((ConnectionInformationContainerIOObject) entry.retrieveData(null))
				.getConnectionInformation(), entry.getLocation(), entry.isEditable());
	}

	/**
	 * Creates a new ConnectionInformationHolder from an existing {@link ConnectionInformation}.
	 *
	 * @param connection
	 * 		the connection to base the holder on
	 * @param name
	 * 		the name of the connection
	 * @param location
	 * 		the location of the connection
	 */
	public static ConnectionInformationHolder from(ConnectionInformation connection, String name, RepositoryLocation location) throws IOException {
		return new ConnectionInformationHolder(connection, name, location, true);
	}

	/**
	 * Creates a new ConnectionInformation of the given name and type
	 *
	 * @param name
	 * 		the name of the connection
	 * @param type
	 * 		the type of the connection
	 * @param location
	 * 		the future location of the connection
	 * @return a new holder containing an empty connection object and the location
	 * @throws IOException
	 * 		in case the connection could not be copied
	 */
	public static ConnectionInformationHolder createNewConnection(String name, String type, RepositoryLocation location) throws IOException {
		ConnectionInformation connection = ConnectionHandlerRegistry.getInstance().getHandler(type).createNewConnectionInformation(name);
		return new ConnectionInformationHolder(connection, name, location, true);
	}

}
