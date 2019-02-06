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

import java.util.Collection;
import java.util.List;

import com.rapidminer.gui.properties.SettingsItem.Type;
import com.rapidminer.parameter.ParameterHandler;


/**
 * An interface that specifies how a collection of {@link SettingsItem SettingsItems} can behave.
 * Extracted from {@link SettingsItems}/{@link AbstractSettingsItemProvider}.
 *
 * @author Peter Hellinger, Jan Czogalla
 * @since 9.1
 *
 */
public interface SettingsItemProvider {

	/** Removes empty subgroups and groups */
	void clean();

	/** Checks if a key is known. */
	boolean containsKey(String key);

	/** Returns item or <code>null</code>, if key is unknown. */
	SettingsItem get(String key);

	/**
	 * Returns the value of the given key. Returns {@code null} for unknown or non-parameter keys.
	 *
	 * @since 9.1
	 */
	String getValue(String key);

	/**
	 * Check if enforced settings exist
	 *
	 * @since 9.1
	 */
	default boolean hasEnforcedSettings() {
		return false;
	}

	/**
	 * Check if the given settings key is enforced
	 *
	 * @since 9.1
	 */

	default boolean isSettingEnforced(String key) {
		return false;
	}

	/** Returns all items of the specified type. */
	List<SettingsItem> getItems(Type type);

	/** Returns all known keys. */
	Collection<String> getKeys();

	/**
	 * Adds an item to the map of used SettingsItem objects. Checks for <code>null</code> and empty
	 * values of the parameters.
	 */
	void put(String key, SettingsItem item);

	/**
	 * Removes item from {@link SettingsItems} and the {@link SettingsItem} internal hierarchy.
	 *
	 * @param key
	 *            Key of item
	 * @throws IllegalStateException
	 *             If key is not known or related item has children
	 */
	void remove(String key);

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
	void remove(String key, boolean removeFromSettingsItems, boolean removeFromHierarchy);

	/** Removes all elements of type parameter, whose keys are not contained in keepKeys */
	void removeParameterInverse(Collection<String> keepKeys);

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
	SettingsItem createAndAddItem(String key, Type type);

	/**
	 * Loads the grouping and returns whether it was successful. Subsequent calls to {@link #isGroupingLoaded()}
	 * will then return the same value.
	 *
	 * @since 9.1
	 */
	boolean loadGrouping();

	/**
	 * Returns true if the XML file was parsed successfully. Returns false if not.
	 *
	 * @since 9.1
	 */
	boolean isGroupingLoaded();

	/**
	 * Applies the values of the given parameter handler to the items
	 *
	 * @since 9.1
	 */
	void applyValues(ParameterHandler parameterHandler);

	/**
	 * Saves the settings
	 *
	 * @since 9.1
	 */
	void saveSettings();
}