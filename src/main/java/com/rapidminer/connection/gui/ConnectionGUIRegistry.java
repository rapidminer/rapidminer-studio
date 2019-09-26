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
package com.rapidminer.connection.gui;

import java.util.HashMap;
import java.util.Map;

import com.rapidminer.tools.ValidationUtil;
import com.rapidminer.tools.Tools;
import com.rapidminer.tools.mail.connection.MailConnectionHandler;
import com.rapidminer.tools.mail.connection.gui.MailConnectionGUI;


/**
 * Registry for Connection GUIs. Every connection type must register his editing GUI here.
 *
 * @author Jonas Wilms-Pfau
 * @since 9.3
 */
public enum ConnectionGUIRegistry {

	INSTANCE;

	private final Map<String, ConnectionGUIProvider> providers = new HashMap<>();

	static {
		for (MailConnectionHandler handler : MailConnectionHandler.values()) {
			INSTANCE.registerGUIProvider(MailConnectionGUI::new, handler.getType());
		}
	}


	/**
	 * Registers a provider for a connection type
	 *
	 * @param provider
	 * 		the provider for the type
	 * @param connectionType
	 * 		the connection type
	 * @return {@code false} if another provider is already registered for the connectionType
	 */
	public boolean registerGUIProvider(ConnectionGUIProvider provider, String connectionType) {
		ValidationUtil.requireNonNull(provider, "provider");
		ValidationUtil.requireNonNull(connectionType, "connectionType");
		return null == providers.putIfAbsent(connectionType, provider);
	}

	/**
	 * Unregisters a provider for a connection type
	 *
	 * @param provider
	 * 		the provider for the type
	 * @param connectionType
	 * 		the connection type
	 * @return {@code true} if the handler was successfully unregistered
	 */
	public boolean unregisterGUIProvider(ConnectionGUIProvider provider, String connectionType) {
		ValidationUtil.requireNonNull(provider, "provider");
		ValidationUtil.requireNonNull(connectionType, "connectionType");
		return providers.remove(connectionType, provider);
	}

	/**
	 * Returns the ConnectionGUIProvider for the given type or {@code null} if none is set
	 * <p>Internal API, only available to signed Extensions.</p>
	 * @param connectionType
	 * 		the connection type
	 * @return the registered {@link ConnectionGUIProvider} for the type
	 * @throws UnsupportedOperationException if the caller is not signed
	 */
	public ConnectionGUIProvider getGUIProvider(String connectionType) {
		Tools.requireInternalPermission();
		return providers.get(connectionType);
	}

}
