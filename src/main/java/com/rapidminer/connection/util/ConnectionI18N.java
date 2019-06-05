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
package com.rapidminer.connection.util;

import java.util.Collections;
import java.util.HashMap;
import java.util.Map;
import java.util.Objects;
import javax.swing.Icon;

import com.rapidminer.connection.valueprovider.ValueProvider;
import com.rapidminer.connection.valueprovider.ValueProviderParameter;
import com.rapidminer.gui.tools.IconSize;
import com.rapidminer.gui.tools.SwingTools;
import com.rapidminer.tools.I18N;


/**
 * Utility class for getting connection related I18N entries.
 * <p>
 * <strong>Note:</strong> type normally is of the format extension_id:type, which does not work for i18n.
 * Therefore, wherever in this i18n the type is used, its colon is replaced by a dot before looking it up, e.g. {@code
 * jdbc_connectors.jdbc.host.label}
 * </p>
 *
 * @author Jonas Wilms-Pfau
 * @since 9.3
 */
public final class ConnectionI18N {

	/**
	 * Delimiter character for keys
	 */
	public static final String KEY_DELIMITER = ".";

	/**
	 * Label key suffix
	 */
	public static final String LABEL_SUFFIX = "label";

	/**
	 * Tip key suffix
	 */
	public static final String TIP_SUFFIX = "tip";

	/**
	 * Icon key suffix
	 */
	public static final String ICON_SUFFIX = "icon";

	/**
	 * Prompt key suffix (prompt is the gray text in an empty text field)
	 */
	public static final String PROMPT_SUFFIX = "prompt";

	/**
	 * Connection key prefix
	 */
	public static final String CONNECTION_PREFIX = "gui.label.connection";

	/**
	 * Connection type key prefix
	 */
	public static final String TYPE_PREFIX = "gui.label.connection.type";

	/**
	 * ValueProvider type key prefix
	 */
	public static final String VALUE_PROVIDER_TYPE_PREFIX = "gui.label.connection.valueprovider.type";

	/**
	 * ValueProvider parameter prefix
	 */
	public static final String VALUE_PROVIDER_PARAMETER_PREFIX = "gui.label.connection.valueprovider.parameter";

	/**
	 * Connection group key prefix
	 */
	public static final String GROUP_PREFIX = "gui.label.connection.group";

	/**
	 * Connection parameter key prefix
	 */
	public static final String PARAMETER_PREFIX = "gui.label.connection.parameter";

	/**
	 * Fallback connection icon
	 */
	public static final String CONNECTION_ICON = I18N.getGUILabel("connection.type.unknown.icon");

	/**
	 * stores the icons for all connection types
	 */
	private static final Map<String, Icon> ICON_CONNECTION_TYPE_MAP = Collections.synchronizedMap(new HashMap<>());


	/**
	 * Prevent utility class instantiation.
	 */
	private ConnectionI18N() {
		throw new AssertionError("Utility class");
	}

	/**
	 * Returns the name of the image for the given connection type.
	 * <p>
	 * This requires a GUI.properties entry in the form of {@code gui.label.connection.type.{type}.icon} which resolves
	 * to filename.png
	 * </p>
	 * <p>
	 * <strong>Note:</strong> type normally is of the format extension_id:type, which does not work for i18n.
	 * Therefore, the colon is replaced by a dot before looking it up, e.g. {@code jdbc_connectors.jdbc.host.label}
	 * </p>
	 *
	 * @param type
	 * 		the {@link com.rapidminer.connection.configuration.ConnectionConfiguration#getType() type}
	 * @return the image file name, or {@link #CONNECTION_ICON} if not set
	 */
	public static String getConnectionIconName(String type) {
		// Check if a .icon entry exists
		String imageName = I18N.getGUIMessageOrNull(String.join(KEY_DELIMITER, TYPE_PREFIX, replaceColon(type), ICON_SUFFIX));
		if (imageName == null) {
			imageName = CONNECTION_ICON;
		}
		return imageName;
	}

	/**
	 * Returns the image for the given connection type.
	 * <p>
	 * This requires a GUI.properties entry in the form of gui.label.connection.type.{type}.icon which resolves to
	 * "filename.png", which must be in the icons/16/ resource folder
	 * </p>
	 * <p>
	 * <strong>Note:</strong> type normally is of the format extension_id:type, which does not work for i18n.
	 * Therefore, the colon is replaced by a dot before looking it up, e.g. {@code jdbc_connectors.jdbc.host.label}
	 * </p>
	 *
	 * @param type
	 * 		the {@link com.rapidminer.connection.configuration.ConnectionConfiguration#getType() type}
	 * @param iconSize
	 * 		the requested icon size. Connections icons can be retrieved in 16px, 24px, and 48px
	 * @return the defined icon, or the {@link #CONNECTION_ICON}
	 */
	public static Icon getConnectionIcon(String type, IconSize iconSize) {
		String sizePrefix = iconSize.getSize() + "/";
		return ICON_CONNECTION_TYPE_MAP.computeIfAbsent(sizePrefix + type,
				k -> SwingTools.createIcon(sizePrefix + getConnectionIconName(type)));
	}

	/**
	 * Returns the icon name for the group of the given type.
	 * <p>
	 * This requires a GUI.properties entry in the form of gui.label.connection.group.{type}.{group}.icon which resolves
	 * to "filename.png", which must be in the icons/16/ resource folder
	 * </p>
	 * <p>
	 * <strong>Note:</strong> type normally is of the format extension_id:type, which does not work for i18n.
	 * Therefore, the colon is replaced by a dot before looking it up, e.g. {@code jdbc_connectors.jdbc.host.label}
	 * </p>
	 *
	 * @param type
	 * 		the {@link com.rapidminer.connection.configuration.ConnectionConfiguration#getType() type}
	 * @param group
	 * 		the {@link com.rapidminer.connection.configuration.ConfigurationParameterGroup#getGroup() group name}
	 * @return the image file name, or {@code null} if not set
	 */
	public static String getGroupIconName(String type, String group) {
		return getGroupName(type, group, ICON_SUFFIX, null);
	}

	/**
	 * Returns the icon for the group of the given type.
	 * <p>
	 * This requires a GUI.properties entry in the form of gui.label.connection.group.{type}.{group}.icon which resolves
	 * to "filename.png", which must be in the icons/16/ resource folder
	 * </p>
	 * <p>
	 * <strong>Note:</strong> type normally is of the format extension_id:type, which does not work for i18n.
	 * Therefore, the colon is replaced by a dot before looking it up, e.g. {@code jdbc_connectors.jdbc.host.label}
	 * </p>
	 *
	 * @param type
	 * 		the {@link com.rapidminer.connection.configuration.ConnectionConfiguration#getType() type}
	 * @param group
	 * 		the {@link com.rapidminer.connection.configuration.ConfigurationParameterGroup#getGroup() group name}
	 * @return the image, or {@code null} if not set
	 */
	public static Icon getGroupIcon(String type, String group) {
		String image = getGroupIconName(type, group);
		if (image == null) {
			return null;
		}
		return SwingTools.createIcon("icons/16/" + image);
	}

	/**
	 * Returns the I18N name for the given connection type.
	 *
	 * <p>
	 * This requires a GUI.properties entry in the form of gui.label.connection.type.{type}.label, otherwise the raw
	 * type string is returned.
	 * </p>
	 * <p>
	 * <strong>Note:</strong> type normally is of the format extension_id:type, which does not work for i18n.
	 * Therefore, the colon is replaced by a dot before looking it up, e.g. {@code jdbc_connectors.jdbc.host.label}
	 * </p>
	 *
	 * @param type
	 * 		the {@link com.rapidminer.connection.configuration.ConnectionConfiguration#getType() type}
	 * @return the i18n type label, or {@code type}
	 */
	public static String getTypeName(String type) {
		if (type == null) {
			return null;
		}
		return Objects.toString(I18N.getGUIMessageOrNull(String.join(KEY_DELIMITER, TYPE_PREFIX, replaceColon(type), LABEL_SUFFIX)), type);
	}

	/**
	 * Returns the I18N name for the given value provider type.
	 *
	 * <p>
	 * This requires a GUI.properties entry in the form of gui.label.connection.valueprovider.type.{type}.label,
	 * otherwise the raw type string is returned.
	 * </p>
	 *
	 * @param type
	 * 		the {@link ValueProvider#getType()}  type}
	 * @param arguments
	 * 		optional arguments for message formatter
	 * @return the I18N type name, or type
	 */
	public static String getValueProviderTypeName(String type, Object... arguments) {
		return Objects.toString(I18N.getGUIMessageOrNull(String.join(KEY_DELIMITER, VALUE_PROVIDER_TYPE_PREFIX, replaceColon(type), LABEL_SUFFIX), arguments), type);
	}

	/**
	 * Returns the I18N name for the given value provider type.
	 *
	 * <p>
	 * This requires a GUI.properties entry in the form of gui.label.connection.valueprovider.parameter.{valueprovidertype}.parameter, otherwise the raw valueprovidertype string is returned.
	 * </p>
	 *
	 * @param valueProviderType
	 * 		the {@link ValueProvider#getType()} type}
	 * @param parameter
	 * 		the parameter that needs an I18N name
	 * @return the I18N type name, or type
	 */
	public static String getValueProviderParameterName(String valueProviderType, String parameter) {
		return Objects.toString(I18N.getGUIMessageOrNull(String.join(KEY_DELIMITER, VALUE_PROVIDER_PARAMETER_PREFIX, replaceColon(valueProviderType), KEY_DELIMITER, parameter)), parameter);
	}

	/**
	 * Returns the I18N GUI message for the following key:
	 * <p>
	 * {@code gui.label.connection.group.{type}.{group}.{suffix}}
	 * </p>
	 * <p>
	 * <strong>Note:</strong> type normally is of the format extension_id:type, which does not work for i18n.
	 * Therefore, the colon is replaced by a dot before looking it up, e.g. {@code jdbc_connectors.jdbc.host.label}
	 * </p>
	 *
	 * @param type
	 * 		the {@link com.rapidminer.connection.configuration.ConnectionConfiguration#getType() type}
	 * @param group
	 * 		the {@link com.rapidminer.connection.configuration.ConfigurationParameterGroup#getGroup() group name}
	 * @param suffix
	 * 		the suffix to use, e.g. {@value #LABEL_SUFFIX}, {@value #TIP_SUFFIX} or {@value #ICON_SUFFIX}
	 * @param nullDefault
	 * 		the default value to use, might be {@code null}
	 * @return the i18n entry for the key, or {@code nullDefault}
	 */
	public static String getGroupName(String type, String group, String suffix, String nullDefault) {
		String key = String.join(KEY_DELIMITER, GROUP_PREFIX, replaceColon(type), group, suffix);
		return Objects.toString(I18N.getGUIMessageOrNull(key), nullDefault);
	}

	/**
	 * Returns the I18N GUI label for the following key:
	 * <p>
	 * {@code gui.label.connection.parameter.{type}.{fullyQualifiedKey}.label }
	 * </p>
	 * <p>
	 * <strong>Note:</strong> type normally is of the format extension_id:type, which does not work for i18n.
	 * Therefore,
	 * the colon is replaced by a dot before looking it up, e.g. {@code jdbc_connectors.jdbc.host.label}
	 * </p>
	 *
	 * @param type
	 * 		the {@link com.rapidminer.connection.configuration.ConnectionConfiguration#getType() type}
	 * @param fullyQualifiedKey
	 * 		the fully qualified ({@code group.parameter}) parameter key
	 * @param nullDefault
	 * 		the default value to use, might be {@code null}
	 * @return the internationalized parameterName or {@code parameterName}, if no I18N entry exist
	 */
	public static String getParameterName(String type, String fullyQualifiedKey, String nullDefault) {
		String[] split = fullyQualifiedKey.split("\\.", 2);
		if (split.length != 2) {
			return nullDefault;
		}
		String group = split[0];
		String parameter = split[1];
		return getParameterName(type, group, parameter, nullDefault);
	}

	/**
	 * Returns the I18N GUI label for the following key:
	 * <p>
	 * {@code gui.label.connection.parameter.{type}.{parameterGroup}.{parameterName}.label }
	 * </p>
	 * <p>
	 * <strong>Note:</strong> type normally is of the format extension_id:type, which does not work for i18n.
	 * Therefore,
	 * the colon is replaced by a dot before looking it up, e.g. {@code jdbc_connectors.jdbc.host.label}
	 * </p>
	 *
	 * @param type
	 * 		the {@link com.rapidminer.connection.configuration.ConnectionConfiguration#getType() type}
	 * @param group
	 * 		the {@link com.rapidminer.connection.configuration.ConfigurationParameterGroup#getGroup() group name}
	 * @param parameterName
	 * 		the {@link ValueProviderParameter#getName() parameter name}
	 * @param nullDefault
	 * 		the default value to use, might be {@code null}
	 * @return the i18n entry for the key, or {@code nullDefault}
	 */
	public static String getParameterName(String type, String group, String parameterName, String nullDefault) {
		String key = String.join(KEY_DELIMITER, PARAMETER_PREFIX, replaceColon(type), group, parameterName, LABEL_SUFFIX);
		return Objects.toString(I18N.getGUIMessageOrNull(key), nullDefault);
	}

	/**
	 * Returns the I18N GUI tooltip for the following key:
	 * <p>
	 * {@code gui.label.connection.parameter.{type}.{parameterGroup}.{parameterName}.tip }
	 * </p>
	 * <p>
	 * <strong>Note:</strong> type normally is of the format extension_id:type, which does not work for i18n.
	 * Therefore, the colon is replaced by a dot before looking it up, e.g. {@code jdbc_connectors.jdbc.host.label}
	 * </p>
	 *
	 * @param type
	 * 		the {@link com.rapidminer.connection.configuration.ConnectionConfiguration#getType() type}
	 * @param group
	 * 		the {@link com.rapidminer.connection.configuration.ConfigurationParameterGroup#getGroup() group name}
	 * @param parameterName
	 * 		the {@link ValueProviderParameter#getName() parameter name}
	 * @param nullDefault
	 * 		the default value to use, might be {@code null}
	 * @return the i18n entry for the key, or {@code nullDefault}
	 */
	public static String getParameterTooltip(String type, String group, String parameterName, String nullDefault) {
		String key = String.join(KEY_DELIMITER, PARAMETER_PREFIX, replaceColon(type), group, parameterName, TIP_SUFFIX);
		return Objects.toString(I18N.getGUIMessageOrNull(key), nullDefault);
	}

	/**
	 * Returns the I18N GUI prompt for the following key:
	 * <p>
	 * {@code gui.label.connection.parameter.{type}.{parameterGroup}.{parameterName}.prompt }
	 * </p>
	 * <p>
	 * <strong>Note:</strong> type normally is of the format extension_id:type, which does not work for i18n.
	 * Therefore, the colon is replaced by a dot before looking it up, e.g. {@code jdbc_connectors.jdbc.host.label}
	 * </p>
	 *
	 * @param type
	 * 		the {@link com.rapidminer.connection.configuration.ConnectionConfiguration#getType() type}
	 * @param group
	 * 		the {@link com.rapidminer.connection.configuration.ConfigurationParameterGroup#getGroup() group name}
	 * @param parameterName
	 * 		the {@link ValueProviderParameter#getName() parameter name}
	 * @param nullDefault
	 * 		the default value to use, might be {@code null}
	 * @return the i18n entry for the key, or {@code nullDefault}
	 */
	public static String getParameterPrompt(String type, String group, String parameterName, String nullDefault) {
		String key = String.join(KEY_DELIMITER, PARAMETER_PREFIX, replaceColon(type), group, parameterName, PROMPT_SUFFIX);
		return Objects.toString(I18N.getGUIMessageOrNull(key), nullDefault);
	}

	/**
	 * Returns a connection gui message label
	 *
	 * @param key
	 * 		The part between {@value CONNECTION_PREFIX} and {@value LABEL_SUFFIX}
	 * @param arguments
	 * 		optional arguments for message formatter
	 * @return the formatted string for the given key, or the key if no entry exists
	 */
	public static String getConnectionGUILabel(String key, Object... arguments) {
		return Objects.toString(getConnectionGUIMessageOrNull(key + KEY_DELIMITER + LABEL_SUFFIX, arguments), key);
	}

	/**
	 * Returns a connection gui message
	 *
	 * @param key
	 * 		the part after {@value CONNECTION_PREFIX}
	 * @param arguments
	 * 		i18n arguments
	 * @return the formatted string for the given key, or the key if no entry exists
	 */
	public static String getConnectionGUIMessage(String key, Object... arguments) {
		return Objects.toString(getConnectionGUIMessageOrNull(key, arguments), key);
	}

	/**
	 * Returns a connection gui message or {@code null}
	 *
	 * @param key
	 * 		the part after {@value CONNECTION_PREFIX}
	 * @param arguments
	 * 		i18n arguments
	 * @return the formatted string for the given key, or {@code null} if no entry exists
	 */
	public static String getConnectionGUIMessageOrNull(String key, Object... arguments) {
		return I18N.getGUIMessageOrNull(CONNECTION_PREFIX + KEY_DELIMITER + key, arguments);
	}

	/**
	 * Returns a validation error message from the given i18n key fragment or the key itself if no i18n is found.
	 *
	 * @param errorKey
	 * 		the i18n error key. Will become part of a composite key before it is being looked up in the GUI.properties file:
	 *        {@code gui.label.connection.validation.i18nkey = {0} lorem ipsum}.
	 * @param type
	 * 		the type of the connection
	 * @param group
	 * 		the group the parameter that failed validation is in
	 * @param parameterKey
	 * 		the key of the parameter that failed validation
	 * @return the i18n value or the key itself if no i18n is found
	 */
	public static String getValidationErrorMessage(String errorKey, String type, String group, String parameterKey) {
		return getConnectionGUIMessage(errorKey, getParameterName(type, group, parameterKey, parameterKey));
	}

	/**
	 * Replaces the first colon in the type with a dot.
	 *
	 * @param type
	 * 		the type, never {@code null}
	 * @return the type where the first (there should only be one) color is replaced by  dot
	 */
	private static String replaceColon(String type) {
		return type.replace(':', '.');
	}
}
