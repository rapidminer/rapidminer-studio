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
import java.util.Map;

import com.fasterxml.jackson.databind.annotation.JsonDeserialize;


/**
 * A group of {@link ConfigurationParameter ConfigurationParameters}. Consists of a non-{@code null}, non-empty group key
 * and a non-empty list of parameters. A {@link ConnectionConfiguration} can have several groups to distinguish between
 * different parameters.
 *
 * @author Jan Czogalla
 * @see ConnectionConfigurationBuilder#withKeys(Map)
 * @see ConnectionConfigurationBuilder#withKeys(String, List)
 * @since 9.3
 */
@JsonDeserialize(as = ConfigurationParameterGroupImpl.class)
public interface ConfigurationParameterGroup {

	/** Get the group key */
	String getGroup();

	/** Get a copy of the list of parameters */
	List<ConfigurationParameter> getParameters();
}
