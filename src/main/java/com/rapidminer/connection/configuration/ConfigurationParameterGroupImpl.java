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

import java.util.ArrayList;
import java.util.List;
import java.util.Objects;

import org.apache.commons.collections4.CollectionUtils;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.rapidminer.tools.ValidationUtil;


/**
 * Implementation of {@link ConfigurationParameterGroup}.
 *
 * @author Jan Czogalla
 * @since 9.3
 */
public class ConfigurationParameterGroupImpl implements ConfigurationParameterGroup {

	private String group;
	private List<ConfigurationParameter> parameters;

	/** Minimal constructor. Both arguments must not be {@code null} or empty. */
	@JsonCreator
	public ConfigurationParameterGroupImpl(@JsonProperty(value = "group", required = true) String group,
										   @JsonProperty(value = "parameters", required = true) List<ConfigurationParameter> parameters) {
		this.setGroup(group);
		this.setParameters(parameters);
	}

	@Override
	public String getGroup() {
		return group;
	}

	/**
	 * Sets the group key. Is only used during creation (either programmatically or when parsing from Json). The group key
	 * must not be {@code null} or empty and must not contain a dot.
	 */
	private void setGroup(String group) {
		this.group = ValidationUtil.requireNoDot(ValidationUtil.requireNonEmptyString(group, "group"), "group");
	}

	@Override
	public List<ConfigurationParameter> getParameters() {
		return new ArrayList<>(parameters);
	}

	/**
	 * Sets the list of parameters. Is only used during creation (either programmatically or when parsing from Json).
	 * The list must not be {@code null}, or only filled with {@code null} elements.
	 */
	private void setParameters(List<ConfigurationParameter> parameters) {
		parameters = ValidationUtil.stripToEmptyList(parameters);
		ValidationUtil.noDuplicatesAllowed(parameters, UNIQUE_NAME_COMPARATOR, "parameters");
		this.parameters = parameters;
	}

	@Override
	public boolean equals(Object o) {
		if (this == o) {
			return true;
		}
		if (o == null || getClass() != o.getClass()) {
			return false;
		}
		ConfigurationParameterGroupImpl that = (ConfigurationParameterGroupImpl) o;
		return Objects.equals(group, that.group) &&
				CollectionUtils.isEqualCollection(parameters, that.parameters);
	}

	@Override
	public int hashCode() {
		return Objects.hash(group, parameters);
	}
}
