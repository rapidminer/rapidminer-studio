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

import java.io.IOException;
import java.net.URI;
import java.net.URISyntaxException;
import java.util.Collection;
import java.util.HashSet;
import java.util.Iterator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.logging.Level;
import java.util.stream.Collectors;
import javax.xml.parsers.ParserConfigurationException;

import org.xml.sax.SAXException;

import com.rapidminer.gui.properties.SettingsItem.Type;
import com.rapidminer.tools.LogService;


/**
 * Base implementation of a {@link SettingsItemProvider}. Extracted from {@link SettingsItems}.
 *
 * @author Adrian Wilke, Peter Hellinger, Jan Czogalla
 * @since 9.1
 */
public abstract class AbstractSettingsItemProvider implements SettingsItemProvider {

	/** Maps keys to SettingsItem objects */
	private Map<String, SettingsItem> itemMap = new LinkedHashMap<>();
	/** Was settings groups loaded successfully */
	private boolean isGroupingLoaded = false;

	@Override
	public void clean() {
		cleanEmptyCategory(Type.SUB_GROUP);
		cleanEmptyCategory(Type.GROUP);
	}

	@Override
	public boolean containsKey(String key) {
		return itemMap.containsKey(key);
	}

	@Override
	public SettingsItem get(String key) {
		return itemMap.get(key);
	}

	@Override
	public List<SettingsItem> getItems(Type type) {
		return itemMap.values().stream().filter(settingsItem -> settingsItem.getType() == type).collect(Collectors.toList());
	}

	@Override
	public Collection<String> getKeys() {
		return new HashSet<>(itemMap.keySet());
	}

	@Override
	public boolean loadGrouping() {
		try {
			itemMap = new SettingsXmlHandler().parse(getGroupDefinitions());
			isGroupingLoaded = true;
		} catch (ParserConfigurationException | SAXException | IOException | URISyntaxException e) {
			LogService.getRoot().log(Level.WARNING, "com.rapidminer.gui.properties.AbstractSettingsItemProvider.parse_xml_error",
					new String[]{ SettingsXmlHandler.SETTINGS_XML_FILE, e.getMessage()});
			isGroupingLoaded = false;
			// Must not throw an exception, as the settings work without the structure inside XML.
		}
		return isGroupingLoaded;
	}

	@Override
	public void put(String key, SettingsItem item) {
		if (key == null || key.isEmpty()) {
			throw new IllegalArgumentException("Key not set");
		}
		if (item == null) {
			throw new IllegalArgumentException("No item for key " + key);
		}
		itemMap.put(key, item);
	}

	@Override
	public void remove(String key) {
		remove(key, true, true);
	}

	@Override
	public void remove(String key, boolean removeFromSettingsItems, boolean removeFromHierarchy) {
		if (!itemMap.containsKey(key)) {
			throw new IllegalStateException("Could not remove settings item " + key + " as it is not known");
		}

		if (removeFromHierarchy) {
			SettingsItem item = itemMap.get(key);
			if (item.getChildren().isEmpty()) {
				if (item.getParent() != null) {
					item.getParent().getChildren().remove(item);
				}
			} else {
				throw new IllegalStateException("Could not remove settings item " + key + " as it has children");
			}
		}

		if (removeFromSettingsItems) {
			itemMap.remove(key);
		}
	}

	@Override
	public void removeParameterInverse(Collection<String> keepKeys) {
		Iterator<Entry<String, SettingsItem>> itemMapIterator = itemMap.entrySet().iterator();
		while (itemMapIterator.hasNext()) {
			Entry<String, SettingsItem> itemMapEntry = itemMapIterator.next();
			String key = itemMapEntry.getKey();
			if (!keepKeys.contains(key)) {
				SettingsItem item = itemMapEntry.getValue();
				if (item.getType().equals(Type.PARAMETER)) {
					try {
						remove(key, false, true);
						itemMapIterator.remove();
					} catch (IllegalStateException e) {
						LogService.getRoot().log(Level.WARNING,
								"com.rapidminer.gui.properties.AbstractSettingsItemProvider.remove_item_error", e);
					}
				}
			}
		}
	}

	@Override
	public boolean isGroupingLoaded() {
		return isGroupingLoaded;
	}

	/**
	 * Specifies a {@link URI} to an XML file holding the settings.
	 *
	 * @since 9.1
	 */
	protected abstract URI getGroupDefinitions() throws URISyntaxException;

	/**
	 * Cleans the given category.
	 *
	 * @param category the category to clean; one of {@link Type#SUB_GROUP} or Type#GROUP
	 * @since 9.1
	 */
	private void cleanEmptyCategory(Type category) {
		if (category == Type.PARAMETER) {
			return;
		}
		for (SettingsItem item : getItems(category)) {
			if (item.getChildren().isEmpty()) {
				try {
					remove(item.getKey());
				} catch (RuntimeException runtimeException) {
					// This would be a failure in the settings structure and should not happen.
					// The log is to notice an improbable occurrence.
					LogService.getRoot().log(Level.WARNING, "com.rapidminer.gui.properties.AbstractSettingsItemProvider.remove_item_error",
							runtimeException);
				}
			}
		}
	}
}
