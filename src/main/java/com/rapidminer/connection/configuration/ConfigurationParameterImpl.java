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

import org.apache.commons.lang.StringUtils;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.rapidminer.connection.valueprovider.ValueProviderParameterImpl;

/**
 * Implementation of {@link ConfigurationParameter} and subclass of {@link ValueProviderParameterImpl}.
 * Injected parameters will always return a {@code null} value, and will not allow to set the value while injected.
 * The old value is kept though, to be reused if the injected flag changes.
 *
 * @author Jan Czogalla
 * @since 9.3
 */
public class ConfigurationParameterImpl extends ValueProviderParameterImpl implements ConfigurationParameter {

	private String injectorName;

	/** Minimal constructor */
	@JsonCreator
	public ConfigurationParameterImpl(@JsonProperty(value = "name", required = true) String name) {
		super(name);
	}

	/** Key/value constructor. {@code value} cannot be {@code null} or empty here. */
	public ConfigurationParameterImpl(String name, String value) {
		super(name, value);
	}

	/** Constructor for enabled parameters. Only {@code name} is mandatory here. */
	public ConfigurationParameterImpl(String name, String value, boolean encrypted) {
		super(name, value, encrypted);
	}

	/** Full constructor. Only {@code name} is mandatory here. */
	public ConfigurationParameterImpl(String name, String value, boolean encrypted, String injectorName, boolean enabled) {
		super(name, value, encrypted, enabled);
		setInjectorName(injectorName);
	}

	/** @return the value iff {@link #isInjected()} returns {@code false}, {@code null} otherwise */
	@Override
	public String getValue() {
		return isInjected() ? null : super.getValue();
	}

	/**
	 * {@inheritDoc}
	 * <p>
	 * Will do nothing if the parameter should be injected.
	 */
	@Override
	public void setValue(String value) {
		if (isInjected()) {
			return;
		}
		super.setValue(value);
	}

	@Override
	public String getInjectorName() {
		return injectorName;
	}

	@Override
	public void setInjectorName(String injectorName) {
		this.injectorName = StringUtils.trimToNull(injectorName);
		if (this.injectorName != null) {
			super.setValue(null);
		}
	}

	@Override
	public boolean equals(Object o) {
		if (this == o) {
			return true;
		}
		if (!super.equals(o)) {
			return false;
		}
		ConfigurationParameterImpl that = (ConfigurationParameterImpl) o;
		return Objects.equals(injectorName, that.injectorName);
	}

	@Override
	public int hashCode() {
		return Objects.hash(super.hashCode(), injectorName);
	}
}
