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
package com.rapidminer.repository.internal.remote;

import com.rapidminer.repository.CustomRepositoryRegistry;
import com.rapidminer.repository.RepositoryException;


/**
 * The registry which holds the current {@link RemoteRepositoryFactory}.
 *
 * @author Nils Woehler
 * @since 6.5.0
 *
 */
public enum RemoteRepositoryFactoryRegistry {

	INSTANCE;

	private RemoteRepositoryFactory remoteRepo = null;

	/**
	 * @return the current {@link RemoteRepositoryFactory}. Might return <code>null</code> in case
	 *         no factory has been registered yet.
	 */
	public RemoteRepositoryFactory get() {
		return remoteRepo;
	}

	/**
	 * Sets the current {@link RemoteRepositoryFactory}.
	 *
	 * @param remoteRepoFactory
	 *            the factory to set
	 * @throws RepositoryException
	 *             in case a factory for the same class or XML tag is already registered
	 */
	public void set(RemoteRepositoryFactory remoteRepoFactory) throws RepositoryException {
		if (remoteRepoFactory == null) {
			throw new IllegalArgumentException("Factory must not be null");
		}
		CustomRepositoryRegistry.INSTANCE.register(remoteRepoFactory);
		this.remoteRepo = remoteRepoFactory;
	}

}
