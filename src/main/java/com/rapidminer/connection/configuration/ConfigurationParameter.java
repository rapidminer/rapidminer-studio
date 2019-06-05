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

import com.fasterxml.jackson.annotation.JsonIgnore;
import com.fasterxml.jackson.databind.annotation.JsonDeserialize;
import com.rapidminer.connection.valueprovider.ValueProviderParameter;


/**
 * An extension of {@link ValueProviderParameter} that adds the possibility to mark the parameter as being injected.
 * {@link ConfigurationParameter ConfigurationParameters} can be grouped together using a {@link ConfigurationParameterGroup}.
 * These parameters are like normal parameters for a {@link ConnectionConfiguration}.
 *
 * @author Jan Czogalla
 * @since 9.3
 */
@JsonDeserialize(as = ConfigurationParameterImpl.class)
public interface ConfigurationParameter extends ValueProviderParameter {

	/**
	 * Whether this parameter's value should be injected by a
	 * {@link com.rapidminer.connection.valueprovider.ValueProvider ValueProvider}.
	 *
	 * @return by default, checks if {@link #getInjectorName()} is not {@code null}
	 */
	@JsonIgnore
	default boolean isInjected() {
		return getInjectorName() != null;
	}

	/**
	 * Returns the name of the {@link com.rapidminer.connection.valueprovider.ValueProvider ValueProvider} that should
	 * be used for injection. Will return {@code null} if either {@link #isInjected()} returns {@code false}
	 * or the first matching value provider should be used.
	 *
	 * @return the name of the value provider or {@code null}
	 */
	String getInjectorName();

	/**
	 * Set the name of {@link com.rapidminer.connection.valueprovider.ValueProvider ValueProvider} that should
	 * be used for injection. Can be set to {@code null} to indicate that the first matching value provider should be used.
	 *
	 * @param injectorName
	 * 		the name of the {@link com.rapidminer.connection.valueprovider.ValueProvider ValueProvider}
	 */
	void setInjectorName(String injectorName);
}
