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

import com.fasterxml.jackson.databind.annotation.JsonDeserialize;


/**
 * An extension of {@link ConfigurationParameter} that adds a group key to single out specific parameters in a {@link ConnectionConfiguration}.
 *
 * @author Jan Czogalla
 * @see ConnectionConfigurationBuilder#withPlaceholders(List)
 * @see ConnectionConfigurationBuilder#withPlaceholder(PlaceholderParameter)
 * @since 9.3
 */
@JsonDeserialize(as = PlaceholderParameterImpl.class)
public interface PlaceholderParameter extends ConfigurationParameter {

	/** Get the group key of this parameter */
	String getGroup();
}
