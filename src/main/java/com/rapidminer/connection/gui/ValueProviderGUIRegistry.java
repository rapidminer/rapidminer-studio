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

import com.rapidminer.connection.DefaultValueProviderGUI;
import com.rapidminer.connection.valueprovider.handler.MacroValueProviderGUI;
import com.rapidminer.connection.valueprovider.handler.MacroValueProviderHandler;
import com.rapidminer.tools.Tools;
import com.rapidminer.tools.ValidationUtil;


/**
 * Registry for {@link com.rapidminer.connection.valueprovider.handler.ValueProviderHandler} configuration GUIs.
 * If the configuration is more than simple key value combinations it has to register its own provider for configuration
 * of complex setups.
 *
 * @author Andreas Timm
 * @since 9.3
 */
public enum ValueProviderGUIRegistry {
	INSTANCE;

	/**
	 * The default GUI provider to configure the parameters of a {@link com.rapidminer.connection.valueprovider.ValueProvider ValueProvider}
	 */
	private static final ValueProviderGUIProvider DEFAULT_VP_GUI_PROVIDER = new DefaultValueProviderGUI();

	ValueProviderGUIRegistry() {
		registerGUIProvider(new MacroValueProviderGUI(), MacroValueProviderHandler.TYPE);
	}


	/**
	 * all the registered {@link ValueProviderGUIProvider ValueProviderGUIProviders}
	 */
	private final Map<String, ValueProviderGUIProvider> providers = new HashMap<>();

	/**
	 * Registers a provider for a value provider type
	 *
	 * @param provider
	 * 		the provider for the type
	 * @param valueProviderType
	 * 		the value provider type
	 * @return {@code false} if another provider is already registered for the valueProviderType
	 */
	public boolean registerGUIProvider(ValueProviderGUIProvider provider, String valueProviderType) {
		ValidationUtil.requireNonNull(provider, "provider");
		ValidationUtil.requireNonNull(valueProviderType, "valueProviderType");
		return null == providers.putIfAbsent(valueProviderType, provider);
	}

	/**
	 * Unregisters a provider for a value provider type
	 *
	 * @param provider
	 * 		the provider for the type
	 * @param valueProviderType
	 * 		the value provider type
	 * @return {@code true} if the handler was successfully unregistered
	 */
	public boolean unregisterGUIProvider(ValueProviderGUIProvider provider, String valueProviderType) {
		ValidationUtil.requireNonNull(provider, "provider");
		ValidationUtil.requireNonNull(valueProviderType, "valueProviderType");
		return providers.remove(valueProviderType, provider);
	}

	/**
	 * Returns the component for the given type or {@code null} if none is set
	 * <p>Internal API, only available to signed Extensions.</p>
	 *
	 * @param valueProviderType
	 * 		the value provider type
	 * @return a proxy for the registered {@link ValueProviderGUIProvider} or the default renderer
	 */
	public ValueProviderGUIProvider getGUIProvider(String valueProviderType) {
		return new ProxyValueProviderGUIProvider(providers.getOrDefault(valueProviderType, DEFAULT_VP_GUI_PROVIDER));
	}

}
