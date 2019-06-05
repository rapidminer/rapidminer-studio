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
package com.rapidminer.gui.properties;

import java.net.URI;
import java.net.URISyntaxException;
import java.util.Collection;

import com.rapidminer.gui.properties.SettingsItem.Type;
import com.rapidminer.parameter.ParameterHandler;
import com.rapidminer.parameter.ParameterType;
import com.rapidminer.parameter.Parameters;
import com.rapidminer.tools.ParameterService;
import com.rapidminer.tools.Tools;


/**
 * This singleton implementation of {@link AbstractSettingsItemProvider} handles {@link SettingsItem}s for general Studio settings.
 * It contains a map, which maps settings keys to {@link SettingsItem}s. Additionally to some default map methods and helper methods are provided.
 *
 * @author Adrian Wilke, Peter Hellinger, Jan Czogalla
 */
public final class SettingsItems extends AbstractSettingsItemProvider {

	public static final SettingsItems INSTANCE = new SettingsItems();

	/** Singleton: Empty private constructor */
	private SettingsItems() {}

	@Override
	public String getValue(String key) {
		return ParameterService.getParameterValue(key);
	}

	/**
	 * Creates a SettingsItem and adds it to the item map. Parent items of the type GROUP are also
	 * generated.
	 *
	 * @param key
	 *            The key of the item
	 * @param type
	 *            The type of the item
	 *
	 * @return The created SettingsItem
	 */
	@Override
	public SettingsItem createAndAddItem(String key, Type type) {
		if (key == null || key.isEmpty()) {
			throw new IllegalArgumentException("Settings item has no key.");
		}

		SettingsItem parent = null;

		// If type is parameter, choose or create the related group
		String groupKey = ParameterService.getGroupKey(key);
		if (type == Type.PARAMETER) {
			if (!containsKey(groupKey)) {
				parent = createAndAddItem(groupKey, Type.GROUP);
			} else {
				parent = get(groupKey);
			}
		}

		// Create new SettingsItem
		SettingsItem settingsItem = new SettingsItem(groupKey, key, parent, type);

		// Add to SettingsItems
		put(key, settingsItem);

		return settingsItem;
	}

	/** @see ParameterService#setParameterValue(String, String) */
	@Override
	public void applyValues(ParameterHandler parameterHandler) {
		Parameters parameters = parameterHandler.getParameters();
		Collection<String> parameterKeys = ParameterService.getParameterKeys();
		for (String k : parameters.getDefinedKeys()) {
			if (parameterKeys.contains(k)) {
				ParameterType type = parameters.getParameterType(k);
				String value = parameters.getParameterOrNull(k);
				if (type != null && value != null) {
					value = type.toString(value);
				}
				ParameterService.setParameterValue(k, value);
			}
		}
	}

	/** @see ParameterService#saveParameters()  */
	@Override
	public void saveSettings() {
		ParameterService.saveParameters();
	}

	/** @see ParameterService#hasEnforcedValues() */
	@Override
	public boolean hasEnforcedSettings() {
		return ParameterService.hasEnforcedValues();
	}

	/** @see ParameterService#isValueEnforced(String) */
	@Override
	public boolean isSettingEnforced(String key) {
		return ParameterService.isValueEnforced(key);
	}

	@Override
	protected URI getGroupDefinitions() throws URISyntaxException {
		return Tools.getResource(SettingsXmlHandler.SETTINGS_XML_FILE).toURI();
	}

	/**
	 * Parses XML file of RapidMiner Studio with settings structure.
	 *
	 * Sets result of {@link #isStudioXmlParsedSuccessfully()} to the return value.
	 *
	 * @return {@code true}, iff the XML was parsed successfully.
	 * @deprecated since 9.1; use {@link #loadGrouping()} instead
	 */
	@Deprecated
	public boolean parseStudioXml() {
		return loadGrouping();
	}

	/**
	 * Returns true if the XML file was parsed successfully. Returns false if not.
	 * @deprecated since 9.1; use {@link #isGroupingLoaded()} instead
	 */
	@Deprecated
	public boolean isStudioXmlParsedSuccessfully() {
		return isGroupingLoaded();
	}
}
