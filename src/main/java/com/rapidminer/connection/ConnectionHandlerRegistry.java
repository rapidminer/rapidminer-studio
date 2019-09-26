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
package com.rapidminer.connection;

import com.rapidminer.connection.util.GenericHandlerRegistry;
import com.rapidminer.connection.util.GenericRegistrationEventListener;
import com.rapidminer.operator.OperatorVersion;
import com.rapidminer.tools.mail.connection.MailConnectionHandler;


/**
 * Registry for {@link ConnectionHandler ConnectionHandlers}. Handlers can be registered and unregistered,
 * searched by type and (un)registrations can be observed. See {@link GenericHandlerRegistry} for further details.
 *
 * @author Jan Czogalla
 * @since 9.3
 */
public final class ConnectionHandlerRegistry extends GenericHandlerRegistry<ConnectionHandler> {

	public static final OperatorVersion BEFORE_NEW_CONNECTION_MANAGEMENT = new OperatorVersion(9, 2, 1);

	private static final ConnectionHandlerRegistry INSTANCE = new ConnectionHandlerRegistry();

	static {
		for (MailConnectionHandler handler : MailConnectionHandler.values()) {
			getInstance().registerHandler(handler);
		}
	}

	/**
	 * Singleton class, no instantiation allowed except for internal purpose
	 */
	private ConnectionHandlerRegistry() {
	}

	/**
	 * Get the instance of this singleton
	 */
	public static ConnectionHandlerRegistry getInstance() {
		return INSTANCE;
	}

	@Override
	@SuppressWarnings("unchecked")
	protected <G extends GenericRegistrationEventListener<ConnectionHandler>, L extends G> Class<G> getListenerClass(L listener) {
		return (Class<G>) (listener == null || listener instanceof ConnectionHandlerRegistryListener ?
				ConnectionHandlerRegistryListener.class : listener.getClass());
	}

	@Override
	protected String getRegistryType() {
		return "connection";
	}
}
