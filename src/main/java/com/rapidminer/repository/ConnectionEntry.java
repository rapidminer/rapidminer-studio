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
package com.rapidminer.repository;

import com.rapidminer.connection.ConnectionInformation;
import com.rapidminer.tools.PasswordInputCanceledException;


/**
 * General interface for connection entries.
 *
 * @author Andreas Timm
 * @since 9.3
 */
public interface ConnectionEntry extends IOObjectEntry {

	String TYPE_NAME = "connection";
	// file extensions
	String CON_SUFFIX = ".conninfo";
	String CON_MD_SUFFIX = ".connmd";

	@Override
	default String getType() {
		return TYPE_NAME;
	}

	/**
	 * Storing the {@link ConnectionInformation} associated with this entry.
	 *
	 * @param connectionInformation
	 * 		an up-to-date {@link ConnectionInformation}  object
	 * @throws RepositoryException
	 * 		if storing did not succeed preserve some information for the user
	 */
	void storeConnectionInformation(ConnectionInformation connectionInformation) throws RepositoryException;

	/**
	 * Returns {@code true} if the connection is editable
	 * <p>Warning: Calling this method might be very expensive, only call if really necessary.</p>
	 *
	 * @return {@code true} if the connection is editable
	 */
	default boolean isEditable() throws RepositoryException, PasswordInputCanceledException {
		return !isReadOnly();
	}

	/**
	 * Returns the cached {@link com.rapidminer.connection.configuration.ConnectionConfiguration#getType connection type}
	 *
	 * <p>This method does not block, but return {@code null} if the connection type is not yet known.</p>
	 *
	 * @return the {@link com.rapidminer.connection.configuration.ConnectionConfiguration#getType connection type}, or {@code null}
	 */
	String getConnectionType();
}
