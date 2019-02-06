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


/**
 * A repository that may not be connected by default and has a listener mechanism so anyone can get notified about connection changes.
 *
 * @author Marco Boeck
 * @since 8.1
 */
public interface ConnectionRepository extends Repository {

	/**
	 * Registers a {@link ConnectionListener}.
	 *
	 * @param listener
	 *            the {@link ConnectionListener} to register
	 */
	void addConnectionListener(ConnectionListener listener);

	/**
	 * Removes a registered {@link ConnectionListener}.
	 *
	 * @param listener
	 *            the {@link ConnectionListener} to remove
	 */
	void removeConnectionListener(ConnectionListener listener);

	/**
	 * @return whether the {@link ConnectionRepository} is connected (online) or disconnected (offline).
	 */
	boolean isConnected();
}
