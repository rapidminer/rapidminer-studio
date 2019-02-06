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

import java.util.Arrays;
import java.util.LinkedList;
import java.util.List;
import java.util.Locale;
import java.util.function.Supplier;
import java.util.logging.Level;

import org.apache.commons.lang.StringUtils;

import com.rapidminer.RapidMiner;
import com.rapidminer.parameter.ParameterType;
import com.rapidminer.tools.I18N;
import com.rapidminer.tools.LogService;
import com.rapidminer.tools.ParameterService;


/**
 * An instance of this class represents either a group, a sub-group or a parameter of the
 * {@link SettingsDialog}.
 *
 * Parent/child relations are completely handled in the constructors.
 *
 * @author Adrian Wilke, Jan Czogalla
 */
public class SettingsItem {

	private String groupKey;

	/**
	 * Regarding the {@link SettingsDialog}, this represents either a tab (GROUP), a heading in a
	 * tab (SUB_GROUP), or a setting by a parameter (PARAMETER)
	 */
	public enum Type {
		GROUP, SUB_GROUP, PARAMETER
	}

	public static final String DEFAULT_PARAMETER_PREFIX = "rapidminer.";

	/** Key of properties file */
	private String key;

	/** The type of this item */
	private Type type;

	/** Parent of current element */
	private SettingsItem parent;

	/** Children of current element */
	private List<SettingsItem> children = new LinkedList<>();

	/** Indicates, if this item has been used in the settings dialog */
	private boolean usedInDialog = false;

	/**
	 * Sets object variables and updates parent/child relations.
	 *  @param key
	 *            The element's key
	 * @param parent
	 *            Parent SettingsItem or <code>null</code>, if there is none
 * @param type
	 */
	public SettingsItem(String key, SettingsItem parent, Type type) {
		this(null, key, parent, type);
	}

	/**
	 * Sets object variables and updates parent/child relations.
	 *
	 * @param groupKey The element's group's key
	 * @param key
	 *            The element's key
	 * @param parent
	 *            Parent SettingsItem or <code>null</code>, if there is none
	 * @param type
	 *            The type of the item
	 */
	public SettingsItem(String groupKey, String key, SettingsItem parent, Type type) {
		if (key == null || key.isEmpty()) {
			throw new IllegalArgumentException(
					"Settings item has no Key." + (parent == null ? "" : " Parent: " + parent.toString()));
		}
		if (type == null) {
			throw new IllegalArgumentException(
					"Settings item has no type." + (parent == null ? "" : " Parent: " + parent.toString()));
		}

		this.groupKey = groupKey;
		this.key = key;
		this.parent = parent;
		this.type = type;

		if (parent != null) {
			parent.addChild(this);
		}
	}

	/**
	 * Adds item to the list of children.
	 */
	public void addChild(SettingsItem child) {
		children.add(child);
	}

	/**
	 * Returns all children of this item.
	 */
	public List<SettingsItem> getChildren() {
		return children;
	}

	/**
	 * Returns all children of the specified type.
	 */
	public List<SettingsItem> getChildren(Type type) {
		return getChildren(type, null);
	}

	/**
	 * Returns all children in regard of the specified type and filter.
	 */
	public List<SettingsItem> getChildren(Type type, String filter) {
		List<SettingsItem> childs = new LinkedList<>();
		for (SettingsItem child : children) {
			if (child.type.equals(type) && isInFilter(child, filter)) {
				childs.add(child);
			}
		}
		return childs;
	}

	/**
	 * Checks if the given child matches the filter.
	 */
	public boolean isInFilter(SettingsItem child, String filter) {
		if (!child.type.equals(Type.PARAMETER)) {
			return true;
		}
		filter = StringUtils.stripToNull(filter);
		if (filter == null) {
			return true;
		}
		String[] filterTokens = filter.split(" ");
		for (int i = 0; i < filterTokens.length; i++) {
			filterTokens[i] = filterTokens[i].toLowerCase(Locale.ENGLISH);
		}
		List<Supplier<String>> stringProviders = Arrays.asList(child::getKey, child::getTitle, child::getDescription);
		return stringProviders.stream().map(Supplier::get).filter(s -> s != null && !s.isEmpty())
				.map(s -> s.toLowerCase(Locale.ENGLISH)).anyMatch(s -> Arrays.stream(filterTokens).anyMatch(s::contains));
	}

	/**
	 * Returns the corresponding i18n string if the key was found or an empty String, if the key was
	 * not found.
	 *
	 * Logs a warning message, if the i18n description could not be found.
	 */
	public String getDescription() {
		String description = I18N.getSettingsMessage(key, I18N.SettingsType.DESCRIPTION);
		if (description.equals(key + I18N.SettingsType.DESCRIPTION)) {

			// If the strings are equal, no description is available.
			// Show warning, if in debug mode
			if (Boolean.parseBoolean(ParameterService.getParameterValue(RapidMiner.PROPERTY_RAPIDMINER_GENERAL_DEBUGMODE))) {
				LogService.getRoot().log(Level.WARNING,
						"com.rapidminer.gui.properties.SettingsItem.no_i18n_description_error", key);
			}

			return "";
		} else {
			return description;
		}
	}

	/**
	 * Returns the group key. Or <code>null</code>, if the group key is not known.
	 */
	public String getGroupKey() {
		return groupKey;
	}

	/**
	 * Returns the hierarchy of the current item and its children. Contains one item per line.
	 */
	public String getHierarchy() {
		return getHierarchy(0).toString();
	}

	/**
	 * Returns the hierarchy of the current item and its children. Contains one item per line.
	 *
	 * @param indent
	 *            The indent in number of white spaces
	 */
	private StringBuilder getHierarchy(int indent) {
		StringBuilder sb = new StringBuilder();
		for (int i = 0; i < indent; i++) {
			sb.append(" ");
		}
		if (type.equals(Type.PARAMETER)) {
			sb.append("[");
		}
		sb.append(getKey());
		sb.append(" | ");
		sb.append(getTitle());
		if (type.equals(Type.PARAMETER)) {
			sb.append("]");
		}
		sb.append(System.lineSeparator());
		for (SettingsItem settingsItem : children) {
			sb.append(settingsItem.getHierarchy(indent + 1));
		}
		return sb;
	}

	/**
	 * Returns the properties key. This identifies the current item.
	 */
	public String getKey() {
		return key;
	}

	/**
	 * Returns the type of the defined parameter identified by its ID. Returns <code>null</code> if
	 * its key is unknown.
	 * @deprecated since 9.1; use {@link SettingsPropertyPanel#getParameterType(SettingsItem)} instead
	 */
	@Deprecated
	public ParameterType getParameterType() {
		return ParameterService.getParameterType(key);
	}

	/**
	 * Returns the parent of this item or <code>null</code>, if no parent item exists.
	 */
	public SettingsItem getParent() {
		return parent;
	}

	/**
	 * Returns a i18n string if found or the key if not found.
	 *
	 * Logs a warning message, if a i18n string could not be found and the debug mode is activated.
	 *
	 * If this item is of type group and there is no i18n specified, the group-key is used and
	 * formatted.
	 */
	public String getTitle() {
		String title = I18N.getSettingsMessage(key, I18N.SettingsType.TITLE);
		if (!title.equals(key + I18N.SettingsType.TITLE)) {
			return title;
		}

		// Show warning, if in debug mode
		if (Boolean.parseBoolean(ParameterService.getParameterValue(RapidMiner.PROPERTY_RAPIDMINER_GENERAL_DEBUGMODE))) {
			LogService.getRoot().log(Level.WARNING, "com.rapidminer.gui.properties.SettingsItem.no_i18n_title_error",
					key);
		}

		// Do not use the '.title' suffix
		title = key;

		if (getType() == Type.SUB_GROUP) {
			// Remove 'rapidminer.settings.subgroup.'
			int prefixLength = SettingsXmlHandler.SUBGROUP_PREFIX.length();
			if (title.length() > prefixLength) {
				title = title.substring(prefixLength);
			}
			// Remove group name
			int index = title.indexOf('.');
			if (index != -1) {
				title = title.substring(index);
			}
			// Replacements
			title = title.replace('.', ' ');
		} else if (getType() != Type.GROUP){
			// Remove 'rapidminer.'
			if (title.startsWith(DEFAULT_PARAMETER_PREFIX)) {
				title = title.substring(DEFAULT_PARAMETER_PREFIX.length());
			}
			// Remove group prefix
			if (groupKey != null && title.startsWith(groupKey + '.')) {
				title = title.substring(groupKey.length() + 1);
			}
			title = title.replace('.', ' ');
		}
		title = title.replace('_', ' ');
		title = title.trim();
		return title.isEmpty() ? title : Character.toUpperCase(title.charAt(0)) + title.substring(1, title.length());
	}

	/**
	 * Returns the type of this items.
	 */
	public Type getType() {
		return type;
	}

	/**
	 * Returns <code>true</code> if this item has already been added to the settings dialog. This is
	 * useful to check, if a sub-group (represented by an heading in a dialog tab) has already been
	 * added.
	 */
	public boolean isUsedInDialog() {
		return usedInDialog;
	}

	/** Sets if this item has been added to the settings dialog. */
	public void setUsedInDialog(boolean usedInDialog) {
		this.usedInDialog = usedInDialog;
	}

	/**
	 * Returns the settings hierarchy of the current element. Every element is represented by its
	 * key.
	 */
	@Override
	public String toString() {
		StringBuilder sb = new StringBuilder();
		if (parent != null) {
			sb.append(parent.toString());
			sb.append(" | ");
		}
		if (type == Type.PARAMETER) {
			sb.append("[");
		}
		sb.append(key);
		if (type == Type.PARAMETER) {
			sb.append("]");
		}
		return sb.toString();
	}
}
