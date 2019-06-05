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
package com.rapidminer.connection.valueprovider;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.TreeMap;
import java.util.stream.Collectors;

import org.apache.commons.collections4.CollectionUtils;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonIgnore;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.rapidminer.tools.ValidationUtil;


/**
 * The implementation of {@link ValueProvider}.
 *
 * @author Jan Czogalla
 * @since 9.3
 */
public class ValueProviderImpl implements ValueProvider {

	private String name;
	private String type;
	private List<ValueProviderParameter> parameters = new ArrayList<>();
	private Map<String, ValueProviderParameter> parameterMap = new TreeMap<>();

	/**
	 * Minimal constructor
	 */
	@JsonCreator
	public ValueProviderImpl(@JsonProperty(value = "name", required = true) String name,
							 @JsonProperty(value = "type", required = true) String type) {
		this(name, type, null);
	}

	/**
	 * Full constructor
	 */
	public ValueProviderImpl(String name, String type, List<ValueProviderParameter> parameters) {
		setName(name);
		setType(type);
		setParameters(parameters);
	}

	@Override
	public String getName() {
		return name;
	}

	/**
	 * Sets the name of this value provider. Must be neither {@code null} nor empty.
	 */
	private void setName(String name) {
		this.name = ValidationUtil.requireNonEmptyString(name, "name");
	}

	@Override
	public String getType() {
		return type;
	}

	/**
	 * Sets the type of this value provider. Must be neither {@code null} nor empty.
	 */
	private void setType(String type) {
		this.type = ValidationUtil.requireNonEmptyString(type, "type");
	}

	@Override
	public List<ValueProviderParameter> getParameters() {
		return new ArrayList<>(parameters);
	}

	/**
	 * Sets the list of parameters of this value provider. Can be either {@code null} or empty.
	 */
	private void setParameters(List<ValueProviderParameter> parameters) {
		this.parameters = ValidationUtil.noDuplicatesAllowed(ValidationUtil.stripToEmptyList(parameters),
				ValueProviderParameter.UNIQUE_NAME_COMPARATOR, "parameters");
		parameterMap.clear();
	}

	@Override
	@JsonIgnore
	public Map<String, ValueProviderParameter> getParameterMap() {
		ensureParameterMap();
		return new TreeMap<>(parameterMap);
	}

	private synchronized void ensureParameterMap() {
		if (parameterMap.isEmpty() && !parameters.isEmpty()) {
			parameters.forEach(p -> parameterMap.put(p.getName(), p));
		}
	}

	@Override
	public boolean equals(Object o) {
		if (this == o) {
			return true;
		}
		if (o == null || getClass() != o.getClass()) {
			return false;
		}
		ValueProviderImpl that = (ValueProviderImpl) o;
		return Objects.equals(name, that.name) &&
				Objects.equals(type, that.type) &&
				CollectionUtils.isEqualCollection(parameters, that.parameters);
	}

	@Override
	public int hashCode() {
		return Objects.hash(name, type, parameters);
	}

	@Override
	public String toString() {
		String parameterString = "";
		if (!parameters.isEmpty()) {
			parameterString = "\n" + parameters.stream().map(Object::toString).collect(Collectors.joining("\n"));
		}
		return "Value provider " + name + " of type " + type + parameterString;
	}
}
