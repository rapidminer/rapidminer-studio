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
package com.rapidminer.connection.configuration;

import java.util.List;
import java.util.Map;

import com.fasterxml.jackson.databind.annotation.JsonDeserialize;
import com.rapidminer.connection.valueprovider.ValueProvider;


/**
 * An interface for a general connection configuration. A configuration consists of at least a name, a type and an (unique) id,
 * which also should make it uniquely identifiable.
 * <p>
 * Additional information can be added, like a description or tags.
 * <p>
 * Optional configuration elements are a list of {@link ValueProvider ValueProviders}, {@link ConfigurationParameterGroup ConfigurationParameterGroups}
 * and {@link PlaceholderParameter PlaceholderConfigurationParameters}. The keys and placeholders must be unique
 * as defined by {@link com.rapidminer.connection.valueprovider.ValueProviderParameter#UNIQUE_NAME_COMPARATOR ValueProviderParameter.UNIQUE_NAME_COMPARATOR}.
 * <p>
 * New instances can be created using a {@link ConnectionConfigurationBuilder}.
 *
 * @author Jan Czogalla
 * @since 9.3
 */
@JsonDeserialize(as = ConnectionConfigurationImpl.class)
public interface ConnectionConfiguration {

	/** Gets the name */
	String getName();

	/** Gets the description */
	String getDescription();

	/**
	 * Sets the description
	 *
	 * @param description
	 * 		the description; will be stripped to an empty string
	 */
	void setDescription(String description);

	/** Gets a copy of the list of tags */
	List<String> getTags();

	/**
	 * Sets the list of tags
	 *
	 * @param tags
	 * 		the tags; will be {@link com.rapidminer.tools.ValidationUtil#stripToEmptyList(List, java.util.function.Predicate) stripped to an empty list}
	 */
	void setTags(List<String> tags);

	/** Gets the type */
	String getType();

	/** Gets the id */
	String getId();

	/** Gets a copy of the list of {@link ValueProvider ValueProviders} */
	List<ValueProvider> getValueProviders();

	/** Gets a copy of the map of value provider names to value providers */
	Map<String, ValueProvider> getValueProviderMap();

	/** Gets a copy of the list of {@link ConfigurationParameterGroup ConfigurationParameterGroups} */
	List<ConfigurationParameterGroup> getKeys();

	/** Gets a copy of the map of fully qualified parameter keys to parameters */
	Map<String, ConfigurationParameter> getKeyMap();

	/** Gets a copy of the list of {@link PlaceholderParameter PlaceholderConfigurationParameters} */
	List<PlaceholderParameter> getPlaceholders();

	/** Gets a copy of the map of fully qualified placeholder parameters to parameters */
	Map<String, ConfigurationParameter> getPlaceholderKeyMap();

	/** Checks wether the specified fully qualified key represents a placeholder parameter. */
	boolean isPlaceholder(String key);

	/** Gets a list of all qualified keys, both normal keys and placeholders; changes on this list have no effect on this configuration */
	List<String> getAllParameterKeys();

	/**
	 * Gets the {@link ConfigurationParameter} associated with the specified fully qualified key;
	 * will return {@code null} for invalid keys. The returned {@link ConfigurationParameter} is modifiable
	 */
	ConfigurationParameter getParameter(String key);

	/**
	 * Gets the value of the {@link ConfigurationParameter} represented by the specified fully qualified key if present.
	 * May return {@code null} when there is no parameter with that key, the value is not set or the value is to be
	 * injected.
	 *
	 * @see ConfigurationParameter#getValue()
	 */
	default String getValue(String key) {
		ConfigurationParameter parameter = getParameter(key);
		return parameter == null ? null : parameter.getValue();
	}

	/**
	 * Checks whether the value associated with the specified fully qualified key is set.
	 *
	 * @return by default, returns {@code true} iff {@link #getValue(String)} does not return {@code null}
	 */
	default boolean isValueSet(String key) {
		return getValue(key) != null;
	}

	/**
	 * Checks whether the {@link ConfigurationParameter} represented by the specified fully qualified key is to be injected.
	 *
	 * @return by default, returns {@code true} iff a parameter is associated with the key and
	 * {@link ConfigurationParameter#isInjected()} returns {@code true}
	 */
	default boolean isValueInjected(String key) {
		ConfigurationParameter parameter = getParameter(key);
		return parameter != null && parameter.isInjected();
	}

	/**
	 * Checks whether the {@link ConfigurationParameter} represented by the specified fully qualified key is encrypted.
	 *
	 * @return by default, returns {@code true} iff a parameter is associated with the key and
	 * {@link ConfigurationParameter#isEncrypted()} returns {@code true}
	 */
	default boolean isValueEncrypted(String key) {
		ConfigurationParameter parameter = getParameter(key);
		return parameter != null && parameter.isEncrypted();
	}
}
