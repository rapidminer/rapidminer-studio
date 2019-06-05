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

import java.util.ArrayList;
import java.util.Comparator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.TreeMap;
import java.util.UUID;

import org.apache.commons.collections4.CollectionUtils;
import org.apache.commons.lang.StringUtils;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonIgnore;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.rapidminer.tools.ValidationUtil;
import com.rapidminer.connection.valueprovider.ValueProvider;
import com.rapidminer.tools.container.Pair;


/**
 * Implementation of {@link ConnectionConfiguration}. New instances can be created using the {@link ConnectionConfigurationBuilder}.
 *
 * @author Jan Czogalla
 * @since 9.3
 */
public class ConnectionConfigurationImpl implements ConnectionConfiguration {

	private static final Comparator<Pair<String, String>> PAIR_COMPARATOR = Comparator.<Pair<String, String>, String>comparing(Pair::getFirst).thenComparing(Pair::getSecond);

	String name;
	private String description = "";
	List<String> tags = new ArrayList<>();

	private String type;
	private String id = UUID.randomUUID().toString();

	List<ValueProvider> valueProviders = new ArrayList<>();
	List<ConfigurationParameterGroup> keys = new ArrayList<>();
	List<PlaceholderParameter> placeholders = new ArrayList<>();

	private Map<String, ConfigurationParameter> keyMap = new TreeMap<>();
	private Map<String, ConfigurationParameter> placeholderKeyMap = new TreeMap<>();
	private Map<String, ValueProvider> valueProviderMap = new LinkedHashMap<>();

	/**
	 * Minimal constructor
	 */
	@JsonCreator
	public ConnectionConfigurationImpl(@JsonProperty(value = "name", required = true) String name,
									   @JsonProperty(value = "type", required = true) String type) {
		this.setName(name);
		this.setType(type);
	}

	@Override
	public String getName() {
		return name;
	}

	/**
	 * Sets the name. Is only used during creation (either programmatically or when parsing from Json). The name
	 * must not be {@code null} or empty.
	 */
	private void setName(String name) {
		this.name = ValidationUtil.requireNonEmptyString(name, "name");
	}

	@Override
	public String getDescription() {
		return description;
	}

	@Override
	public void setDescription(String description) {
		this.description = StringUtils.stripToEmpty(description);
	}

	@Override
	public List<String> getTags() {
		return new ArrayList<>(tags);
	}

	@Override
	public void setTags(List<String> tags) {
		this.tags = ValidationUtil.stripToEmptyList(tags, s -> !s.isEmpty());
	}

	@Override
	public String getType() {
		return type;
	}

	/**
	 * Sets the type. Is only used during creation (either programmatically or when parsing from Json). The type
	 * must not be {@code null} or empty.
	 */
	private void setType(String type) {
		this.type = ValidationUtil.requireNonEmptyString(type, "type");
	}

	@Override
	public String getId() {
		return id;
	}

	/**
	 * Sets the id. Is only used during creation (either programmatically or when parsing from Json). The id
	 * must not be {@code null} or empty.
	 */
	private void setId(String id) {
		this.id = ValidationUtil.requireNonEmptyString(id, "id");
	}

	@Override
	public List<ValueProvider> getValueProviders() {
		return new ArrayList<>(valueProviders);
	}

	@Override
	@JsonIgnore
	public Map<String, ValueProvider> getValueProviderMap() {
		ensureProviderMap();
		return new LinkedHashMap<>(valueProviderMap);
	}

	private synchronized void ensureProviderMap() {
		if (valueProviderMap.isEmpty() && !valueProviders.isEmpty()) {
			valueProviders.forEach(vp -> valueProviderMap.putIfAbsent(vp.getName(), vp));
		}
	}


	/**
	 * Sets the list of value providers. Is only used during creation (either programmatically or when parsing from Json).
	 */
	void setValueProviders(List<ValueProvider> valueProviders) {
		this.valueProviders = ValidationUtil.stripToEmptyList(valueProviders);
	}

	@Override
	public List<ConfigurationParameterGroup> getKeys() {
		return new ArrayList<>(keys);
	}

	@Override
	@JsonIgnore
	public Map<String, ConfigurationParameter> getKeyMap() {
		ensureKeyMap();
		return new TreeMap<>(keyMap);
	}

	private synchronized void ensureKeyMap() {
		if (keyMap.isEmpty() && !keys.isEmpty()) {
			populateKeyMap(keys, keyMap);
		}
	}

	private void populateKeyMap(List<ConfigurationParameterGroup> configGroups, Map<String, ConfigurationParameter> configMap) {
		configGroups.stream().flatMap(cg -> cg.getParameters().stream().map(cp -> new Pair<>(cg.getGroup() + '.' + cp.getName(), cp)))
				.forEach(p -> configMap.putIfAbsent(p.getFirst(), p.getSecond()));
	}

	/**
	 * Sets the list of parameter groups. Is only used during creation (either programmatically or when parsing from Json).
	 */
	void setKeys(List<ConfigurationParameterGroup> keys) {
		keys = ValidationUtil.stripToEmptyList(keys);
		checkKeysForDuplicates(keys, placeholders, "keys");
		this.keys = keys;
		this.keyMap.clear();
	}

	@Override
	public List<PlaceholderParameter> getPlaceholders() {
		return new ArrayList<>(placeholders);
	}

	@Override
	@JsonIgnore
	public Map<String, ConfigurationParameter> getPlaceholderKeyMap() {
		ensurePlaceholderKeyMap();
		return new TreeMap<>(placeholderKeyMap);
	}

	private synchronized void ensurePlaceholderKeyMap() {
		if (placeholderKeyMap.isEmpty() && !placeholders.isEmpty()) {
			populatePlaceholderKeyMap(placeholders, placeholderKeyMap);
		}
	}

	private void populatePlaceholderKeyMap(List<PlaceholderParameter> configGroups, Map<String, ConfigurationParameter> configMap) {
		configGroups.stream().map(acp -> new Pair<>(acp.getGroup() + '.' + acp.getName(), acp))
				.forEach(p -> configMap.putIfAbsent(p.getFirst(), p.getSecond()));
	}

	/**
	 * Sets the list of placeholder parameters. Is only used during creation (either programmatically or when parsing from Json).
	 */
	void setPlaceholders(List<PlaceholderParameter> placeholders) {
		placeholders = ValidationUtil.stripToEmptyList(placeholders);
		checkKeysForDuplicates(keys, placeholders, "placeholders");
		this.placeholders = placeholders;
		this.placeholderKeyMap.clear();
	}

	@Override
	public boolean isPlaceholder(String key) {
		ensurePlaceholderKeyMap();
		return placeholderKeyMap.containsKey(key);
	}

	@Override
	@JsonIgnore
	public List<String> getAllParameterKeys() {
		ensureKeyMap();
		ensurePlaceholderKeyMap();
		ArrayList<String> parameterKeys = new ArrayList<>(keyMap.size() + placeholderKeyMap.size());
		parameterKeys.addAll(keyMap.keySet());
		parameterKeys.addAll(placeholderKeyMap.keySet());
		return parameterKeys;
	}

	@Override
	public ConfigurationParameter getParameter(String key) {
		ensureKeyMap();
		ensurePlaceholderKeyMap();
		return keyMap.getOrDefault(key, placeholderKeyMap.get(key));
	}

	@Override
	public boolean equals(Object o) {
		if (this == o) {
			return true;
		}
		if (o == null || getClass() != o.getClass()) {
			return false;
		}
		ConnectionConfigurationImpl that = (ConnectionConfigurationImpl) o;
		return Objects.equals(id, that.id) &&
				Objects.equals(type, that.type) &&
				Objects.equals(description, that.description) &&
				CollectionUtils.isEqualCollection(tags, that.tags) &&
				CollectionUtils.isEqualCollection(valueProviders, that.valueProviders) &&
				CollectionUtils.isEqualCollection(keys, that.keys) &&
				CollectionUtils.isEqualCollection(placeholders, that.placeholders);
	}

	@Override
	public int hashCode() {
		return Objects.hash(id, description, tags, type, valueProviders, keys, placeholders);
	}

	/**
	 * Checks keys and placeholders for duplicates.
	 */
	private void checkKeysForDuplicates(List<ConfigurationParameterGroup> keys, List<PlaceholderParameter> placeholders, String name) {
		List<Pair<String, String>> allParameters = new ArrayList<>();
		keys.forEach(group -> group.getParameters().forEach(p -> allParameters.add(new Pair<>(group.getGroup(), p.getName()))));
		placeholders.stream().map(p -> new Pair<>(p.getGroup(), p.getName())).forEach(allParameters::add);
		ValidationUtil.noDuplicatesAllowed(allParameters, PAIR_COMPARATOR, name);
	}

}
