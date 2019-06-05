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

import java.util.Comparator;

import com.fasterxml.jackson.annotation.JsonPropertyOrder;
import com.fasterxml.jackson.databind.annotation.JsonDeserialize;

/**
 * Interface for a simple key/value pair that could be encrypted. The only mutable part is the value.
 * <p>
 * This interface has a Jackson implementation: {@link ValueProviderParameterImpl}
 *
 * @author Jan Czogalla
 * @since 9.3
 */
@JsonDeserialize(as = ValueProviderParameterImpl.class)
@JsonPropertyOrder(value = {"name", "encrypted", "enabled", "value"})
public interface ValueProviderParameter {

	/**
	 * A comparator to test for uniqueness. While {@link #equals(Object)} might be used for general purposes,
	 * parameters must be unique by name for all intends and purposes.
	 */
	Comparator<ValueProviderParameter> UNIQUE_NAME_COMPARATOR = Comparator.comparing(ValueProviderParameter::getName);

	/** Get the name of this parameter */
	String getName();

	/** Get the value of this parameter */
	String getValue();

	/**
	 * Set the value of this parameter; can be set to {@code null}.
	 *
	 * @param value
	 * 		the value; will be stripped to {@code null}
	 */
	void setValue(String value);

	/** Whether this parameter is encrypted */
	boolean isEncrypted();

	/** Whether this parameter is enabled */
	boolean isEnabled();

	/**
	 * Set the enabled status of this parameter.
	 *
	 * @param enabled
	 * 		whether the parameter is enabled
	 */
	void setEnabled(boolean enabled);
}
