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

import java.util.Objects;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.rapidminer.tools.ValidationUtil;


/**
 * Implementation of {@link PlaceholderParameter} and a subclass of {@link ConfigurationParameterImpl}.
 *
 * @author Jan Czogalla
 * @since 9.3
 */
public class PlaceholderParameterImpl extends ConfigurationParameterImpl implements PlaceholderParameter {

	private String group;

	/** Minimal constructor */
	@JsonCreator
	public PlaceholderParameterImpl(@JsonProperty(value = "name", required = true) String name,
									@JsonProperty(value = "group", required = true) String group) {
		this(name, group, null);
	}

	/** Constructor for enabled parameters. Only {@code name} and {@code group} are mandatory here. */
	public PlaceholderParameterImpl(String name, String group, String injectorName) {
		this(name, null, group, false, injectorName, true);
	}

	/** Full constructor. Only {@code name} and {@code group} are mandatory here. */
	public PlaceholderParameterImpl(String name, String value, String group, boolean encrypted, String injectorName, boolean enabled) {
		super(name, value, encrypted, injectorName, enabled);
		setGroup(group);
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
	public boolean equals(Object o) {
		if (this == o) {
			return true;
		}
		if (!super.equals(o)) {
			return false;
		}
		PlaceholderParameterImpl that = (PlaceholderParameterImpl) o;
		return Objects.equals(group, that.group);
	}

	@Override
	public int hashCode() {
		return Objects.hash(super.hashCode(), group);
	}
}