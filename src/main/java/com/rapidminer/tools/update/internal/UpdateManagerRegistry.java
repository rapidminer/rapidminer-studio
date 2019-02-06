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
package com.rapidminer.tools.update.internal;

import java.io.IOException;
import java.net.URISyntaxException;


/**
 * This registry holds the current {@link UpdateManagerFactory} instance which creates
 * {@link UpdateManager} instances.
 * <p>
 * This is an internal class and might be changed or removed without any further notice.
 *
 * @author Nils Woehler
 * @since 6.5.0
 *
 */
public enum UpdateManagerRegistry {

	/**
	 * The registry instance.
	 */
	INSTANCE;

	private UpdateManagerFactory factory = null;

	/**
	 * Creates a new {@link UpdateManager} via the registered {@link UpdateManagerFactory} and
	 * returns the new instance.
	 *
	 * @return the {@link UpdateManager}
	 * @throws URISyntaxException
	 *             in case the UpdateManager URI has a syntax error
	 * @throws IOException
	 *             in case the update manager cannot communicate with the server
	 */

	public synchronized UpdateManager get() throws URISyntaxException, IOException {
		if (factory == null) {
			throw new IllegalStateException("No UpdateManagerFactory registered.");
		}
		return factory.create();
	}

	/**
	 * Registers a new {@link UpdateManagerFactory} instance and replaces the old one.
	 *
	 * @param factory
	 *            the new instance
	 */
	synchronized void register(UpdateManagerFactory factory) {
		if (this.factory != null) {
			throw new IllegalStateException("Registering a factory is allowed only once.");
		}
		this.factory = factory;
	}
}
