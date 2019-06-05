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

import java.util.List;
import java.util.Map;

import com.fasterxml.jackson.databind.annotation.JsonDeserialize;


/**
 * Interface for a value provider. Consists of a unique name/type combination
 * (per {@link com.rapidminer.connection.configuration.ConnectionConfiguration ConnectionConfiguration})
 * and a (possibly empty) list of {@link ValueProviderParameter ValueProviderParameters}. These parameters must be unique
 * as defined by {@link ValueProviderParameter#UNIQUE_NAME_COMPARATOR}.
 * <p>
 * Value providers can be used to inject values into parameters of a {@link com.rapidminer.connection.configuration.ConnectionConfiguration ConnectionConfiguration}.
 * The actual functionality is provided in an implementation of {@link com.rapidminer.connection.valueprovider.handler.ValueProviderHandler ValueProviderHandler}
 * that was registered with the {@link com.rapidminer.connection.valueprovider.handler.ValueProviderHandlerRegistry ValueProviderHandlerRegistry}.
 *
 * @author Jan Czogalla
 * @since 9.3
 */
@JsonDeserialize(as = ValueProviderImpl.class)
public interface ValueProvider {

	/** Get the name */
	String getName();

	/** Get the type */
	String getType();

	/** Get a copied list of the parameters */
	List<ValueProviderParameter> getParameters();

	/** Get a copy of the map of fully qualified parameter keys to parameters */
	Map<String, ValueProviderParameter> getParameterMap();
}
