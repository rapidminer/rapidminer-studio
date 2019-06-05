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

import static com.rapidminer.connection.valueprovider.ValueProviderParameter.UNIQUE_NAME_COMPARATOR;

import java.io.IOException;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.ObjectReader;
import com.fasterxml.jackson.databind.ObjectWriter;
import com.rapidminer.connection.ConnectionInformationSerializer;
import com.rapidminer.tools.ValidationUtil;
import com.rapidminer.connection.valueprovider.ValueProvider;


/**
 * Builder for {@link ConnectionConfiguration ConnectionConfigurations}. Instances cannot be reused.
 *
 * @author Jan Czogalla
 * @since 9.3
 */
public class ConnectionConfigurationBuilder {

	private ConnectionConfigurationImpl object;

	private static final ObjectReader reader;
	private static final ObjectWriter writer;

	static {
		ObjectMapper mapper = ConnectionInformationSerializer.getRemoteObjectMapper();
		reader = mapper.reader(ConnectionConfigurationImpl.class);
		writer = mapper.writerWithType(ConnectionConfigurationImpl.class);
	}

	/**
	 * Minimal constructor
	 *
	 * @param name
	 * 		the name; must be neither {@code null} nor empty
	 * @param type
	 * 		the type; must be neither {@code null} nor empty
	 */
	public ConnectionConfigurationBuilder(String name, String type) {
		object = new ConnectionConfigurationImpl(name, type);
	}

	/**
	 * Create a builder based on an existing {@link ConnectionConfiguration}.
	 *
	 * @param original
	 * 		the original configuration
	 * @throws IOException
	 * 		if the configuration could not be parsed with jackson
	 * @throws IllegalArgumentException
	 * 		if parameter original is {@code null} or empty
	 */
	public ConnectionConfigurationBuilder(ConnectionConfiguration original) throws IOException {
		ValidationUtil.requireNonNull(original, "original connection configuration");
		// create a copy using jackson
		object = reader.readValue(writer.writeValueAsBytes(original));
	}

	/**
	 * Create a builder based on an existing {@link ConnectionConfiguration}, but with a new name.
	 *
	 * @param original
	 * 		the original configuration
	 * @param newName
	 * 		the new name for the configuration
	 * @throws IOException
	 * 		if the configuration could not be parsed with jackson
	 * @throws IllegalArgumentException
	 * 		if parameter original is {@code null} or empty
	 */
	public ConnectionConfigurationBuilder(ConnectionConfiguration original, String newName) throws IOException {
		this(original);
		object.name = ValidationUtil.requireNonEmptyString(newName, "new name");
	}

	/**
	 * Set the description for the object to be built.
	 *
	 * @param description
	 * 		the description; will be stripped to an empty string
	 */
	public ConnectionConfigurationBuilder withDescription(String description) {
		object.setDescription(description);
		return this;
	}

	/**
	 * Set the tags for the object to be built.
	 *
	 * @param tags
	 * 		the tags; will be {@link ValidationUtil#stripToEmptyList(List, java.util.function.Predicate) stripped to an empty list}
	 */
	public ConnectionConfigurationBuilder withTags(List<String> tags) {
		object.setTags(tags);
		return this;
	}

	/**
	 * Add a tag to the list for the object to be built.
	 *
	 * @param tag
	 * 		a tag to add; must be neither {@code null} nor empty
	 */
	public ConnectionConfigurationBuilder withTag(String tag) {
		object.tags.add(ValidationUtil.requireNonEmptyString(tag, "tag"));
		return this;
	}

	/**
	 * Set the value providers for the object to be built.
	 *
	 * @param valueProviders
	 * 		the list of value providers; will be {@link ValidationUtil#stripToEmptyList(List) stripped to an empty list}
	 */
	public ConnectionConfigurationBuilder withValueProviders(List<ValueProvider> valueProviders) {
		object.setValueProviders(valueProviders);
		return this;
	}

	/**
	 * Add a value provider to the list for the object to be built.
	 *
	 * @param valueProvider
	 * 		a value provider to add; must not be {@code null}
	 */
	public ConnectionConfigurationBuilder withValueProvider(ValueProvider valueProvider) {
		object.valueProviders.add(ValidationUtil.requireNonNull(valueProvider, "value provider"));
		return this;
	}

	/**
	 * Set the parameter groups for the object to be built.
	 *
	 * @param keys
	 * 		a map of group key/list of parameters
	 */
	public ConnectionConfigurationBuilder withKeys(Map<String, List<ConfigurationParameter>> keys) {
		if (keys != null) {
			object.setKeys(keys.entrySet().stream().map(e -> new ConfigurationParameterGroupImpl(e.getKey(), e.getValue())).collect(Collectors.toList()));
		}
		return this;
	}

	/**
	 * Add a parameter group to the list for the object to be built. Keys will be merged if the group already exists.
	 *
	 * @param group
	 * 		the group the keys belong to; must be neither {@code null} nor empty
	 * @param keys
	 * 		the list of keys for the given group; must not be {@code null} and contain at least one non-{@code null} element
	 */
	public ConnectionConfigurationBuilder withKeys(String group, List<ConfigurationParameter> keys) {
		List<ConfigurationParameter> strippedKeys = ValidationUtil.stripToEmptyList(keys);
		additiveCheckForDuplicates(group, strippedKeys, "keys");
		object.keys.stream().filter(cg -> cg.getGroup().equals(group)).findFirst().ifPresent(g -> {
			strippedKeys.addAll(g.getParameters());
			object.keys.remove(g);
		});
		ConfigurationParameterGroupImpl configGroup = new ConfigurationParameterGroupImpl(group, strippedKeys);
		object.keys.add(configGroup);
		return this;
	}

	/**
	 * Set the list of placeholder parameters for the object to be built.
	 *
	 * @param placeholders
	 * 		the list of placeholders; must not be {@code null} and contain at least one non-{@code null} element
	 */
	public ConnectionConfigurationBuilder withPlaceholders(List<PlaceholderParameter> placeholders) {
		object.setPlaceholders(placeholders);
		return this;
	}

	/**
	 * Add a placeholder parameter to the list for the object to be built.
	 *
	 * @param placeholder
	 * 		the placeholder to add; must not be {@code null}
	 */
	public ConnectionConfigurationBuilder withPlaceholder(PlaceholderParameter placeholder) {
		ValidationUtil.requireNonNull(placeholder, "placeholder");
		additiveCheckForDuplicates(placeholder.getGroup(), Collections.singletonList(placeholder), "placeholder");
		object.placeholders.add(placeholder);
		return this;
	}

	/**
	 * Build and return the new {@link ConnectionConfiguration}. Afterwards, the builder becomes invalid.
	 */
	public ConnectionConfiguration build() {
		ConnectionConfiguration configuration = object;
		object = null;
		return configuration;
	}

	/**
	 * Checks for duplicates in case of additive methods
	 *
	 * @see #withKeys(String, List)
	 * @see #withPlaceholder(PlaceholderParameter)
	 */
	private void additiveCheckForDuplicates(String group, List<ConfigurationParameter> keys, String name) {
		List<PlaceholderParameter> placeholderList = object.placeholders.stream().filter(p -> p.getGroup().equals(group)).collect(Collectors.toList());
		List<ConfigurationParameter> keyList = object.keys.stream().filter(cpg -> cpg.getGroup().equals(group)).flatMap(cg -> cg.getParameters().stream()).collect(Collectors.toList());
		ValidationUtil.noDuplicatesAllowed(keys, UNIQUE_NAME_COMPARATOR, name, placeholderList, keyList);
	}
}
