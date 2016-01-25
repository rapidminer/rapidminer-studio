/**
 * Copyright (C) 2001-2016 by RapidMiner and the contributors
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
import java.net.URISyntaxException;
import java.util.Collection;
import java.util.HashSet;
import java.util.Iterator;
import java.util.LinkedHashMap;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.logging.Level;

import javax.xml.parsers.ParserConfigurationException;

import org.xml.sax.SAXException;

import com.rapidminer.gui.properties.SettingsItem.Type;
import com.rapidminer.tools.LogService;
import com.rapidminer.tools.ParameterService;
import com.rapidminer.tools.Tools;


/**
 * This singleton instance handles {SettingItem}s. It contains a map, which maps settings keys to
 * {@link SettingsItem}s. Additionally to some default map methods and helper methods are provided.
 *
 * @author Adrian Wilke
 */
public enum SettingsItems {

	INSTANCE;

	/** Maps keys to SettingsItem objects */
	private Map<String, SettingsItem> itemMap = new LinkedHashMap<>();

	/** Was XML of RapidMiner Studio parsed successfully? */
	private boolean studioXmlParsed = false;

	/** Singleton: Empty private constructor */
	private SettingsItems() {}

	/** Removes empty subgroups and groups */
	public void clean() {
		List<SettingsItem> mapItems = getItems(Type.SUB_GROUP);
		Iterator<SettingsItem> mapIterator = mapItems.iterator();
		while (mapIterator.hasNext()) {
			SettingsItem item = mapIterator.next();
			if (item.getChildren().isEmpty()) {
				try {
					remove(item.getKey());
				} catch (RuntimeException runtimeException) {
					// This would be a failure in the settings structure and should not happen.
					// The log is to notice an improbable occurrence.
					LogService.getRoot().log(Level.WARNING, "com.rapidminer.gui.properties.SettingsItems.remove_item_error",
							runtimeException);
				}
			}
		}
		mapItems = getItems(Type.GROUP);
		mapIterator = mapItems.iterator();
		while (mapIterator.hasNext()) {
			SettingsItem item = mapIterator.next();
			if (item.getChildren().isEmpty()) {
				try {
					remove(item.getKey());
				} catch (RuntimeException runtimeException) {
					// This would be a failure in the settings structure and should not happen.
					// The log is to notice an improbable occurrence.
					LogService.getRoot().log(Level.WARNING, "com.rapidminer.gui.properties.SettingsItems.remove_item_error",
							runtimeException);
				}
			}
		}
	};

	/** Checks if a key is known. */
	public boolean containsKey(String key) {
		return itemMap.containsKey(key);
	}

	/** Returns item or <code>null</code>, if key is unknown. */
	public SettingsItem get(String key) {
		return itemMap.get(key);
	}

	/** Returns all items of the specified type. */
	public List<SettingsItem> getItems(Type type) {
		List<SettingsItem> settingsItems = new LinkedList<>();
		for (SettingsItem settingsItem : itemMap.values()) {
			if (settingsItem.getType().equals(type)) {
				settingsItems.add(settingsItem);
			}
		}
		return settingsItems;
	}

	/** Returns all known keys. */
	public Collection<String> getKeys() {
		return new HashSet<String>(itemMap.keySet());
	}

	/**
	 * Parses XML file of RapidMiner Studio with settings structure.
	 *
	 * Sets {@link SettingsItems#studioXmlParsed} to the return value.
	 *
	 * @return <code>true</code>, if the XML was parsed successfully. <code>false</code>, if not.
	 */
	public boolean parseStudioXml() {
		try {
			itemMap = new SettingsXmlHandler().parse(Tools.getResource(SettingsXmlHandler.SETTINGS_XML_FILE).toURI());
			studioXmlParsed = true;
			return true;
		} catch (ParserConfigurationException | SAXException | IOException | URISyntaxException e) {
			String[] params = new String[2];
			params[0] = SettingsXmlHandler.SETTINGS_XML_FILE;
			params[1] = e.getMessage();
			LogService.getRoot().log(Level.WARNING, "com.rapidminer.gui.properties.SettingsItems.parse_xml_error", params);
			studioXmlParsed = false;
			return false;
			// Must not throw an exception, as the settings work without the structure inside XML.
		}
	}

	/**
	 * Adds an item to the map of used SettingsItem objects. Checks for <code>null</code> and empty
	 * values of the parameters.
	 */
	public void put(String key, SettingsItem item) {
		if (key == null || key.isEmpty()) {
			throw new IllegalArgumentException("Key not set");
		}
		if (item == null) {
			throw new IllegalArgumentException("No item for key " + key);
		}
		itemMap.put(key, item);
	}

	/**
	 * Removes item from {@link SettingsItems} and the {@link SettingsItem} internal hierarchy.
	 *
	 * @param key
	 *            Key of item
	 * @throws IllegalStateException
	 *             If key is not known or related item has children
	 */
	public void remove(String key) throws IllegalStateException {
		remove(key, true, true);
	}

	/**
	 * Removes item from {@link SettingsItems} and/or the {@link SettingsItem} internal hierarchy.
	 * The execution of the remove operation can be set for each of the both data structures.
	 *
	 * @param key
	 *            Key of item
	 * @param removeFromSettingsItems
	 *            Remove from {@link SettingsItems} data structure
	 * @param removeFromHierarchy
	 *            Removes from {@link SettingsItem} internal hierarchy
	 *
	 * @throws IllegalStateException
	 *             If key is not known or related item has children
	 */
	public void remove(String key, boolean removeFromSettingsItems, boolean removeFromHierarchy)
			throws IllegalStateException {

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

	/** Removes all elements of type parameter, whose keys are not contained in keepKeys */
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
								"com.rapidminer.gui.properties.SettingsItems.remove_item_error", e);
					}
				}
			}
		}
	}

	/** Returns true if the XML file was parsed successfully. Returns false if not. */
	public boolean isStudioXmlParsedSuccessfully() {
		return studioXmlParsed;
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
	public static SettingsItem createAndAddItem(String key, Type type) {
		if (key == null || key.isEmpty()) {
			throw new IllegalArgumentException("Settings item has no key.");
		}

		SettingsItem parent = null;

		// If type is parameter, choose or create the related group
		if (type == Type.PARAMETER) {
			String groupKey = ParameterService.getGroupKey(key);
			if (!INSTANCE.containsKey(groupKey)) {
				parent = createAndAddItem(groupKey, Type.GROUP);
			} else {
				parent = INSTANCE.get(groupKey);
			}
		}

		// Create new SettingsItem
		SettingsItem settingsItem = new SettingsItem(key, parent, type);

		// Add to SettingsItems
		INSTANCE.put(key, settingsItem);

		return settingsItem;
	}
}
