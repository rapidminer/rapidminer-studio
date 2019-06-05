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
package com.rapidminer.connection.gui.model;

import java.util.Collections;
import java.util.List;
import java.util.stream.Collectors;

import com.rapidminer.connection.valueprovider.ValueProvider;


/**
 * Container for the data that is altered in the {@link com.rapidminer.connection.gui.InjectParametersDialog}
 *
 * @author Andreas Timm
 * @since 9.3
 */
public class InjectParametersModel {

	// the data to work with:
	// original parameters are those initially set or actively changed and accepted in the UI
	private final List<ConnectionParameterModel> originalParameters;
	// parameters are being changed through the UI
	private final List<ConnectionParameterModel> parameters;
	// list of value providers to choose from
	private final List<ValueProvider> valueProviders;

	/**
	 * Create an instance with some data. Will use empty lists in case one is missing.
	 *
	 * @param parameters
	 * 		the parameters to be edited
	 * @param valueProviders
	 * 		all available valueProviders to choose from
	 */
	public InjectParametersModel(List<ConnectionParameterModel> parameters, List<ValueProvider> valueProviders) {
		this.originalParameters = parameters != null ? parameters.stream().map(ConnectionParameterModel::copyDataOnly).collect(Collectors.toList()) : Collections.emptyList();
		this.parameters = copyParameters(originalParameters);
		this.valueProviders = valueProviders != null ? valueProviders : Collections.emptyList();
	}

	/**
	 * Create a copy of the given {@link ConnectionParameterModel} list
	 *
	 * @param copyMe
	 * 		list to be copied,
	 * @return a new list with new instances that are created using {@link ConnectionParameterModel#copyDataOnly()}
	 */
	private List<ConnectionParameterModel> copyParameters(List<ConnectionParameterModel> copyMe) {
		return copyMe.stream().map(ConnectionParameterModel::copyDataOnly).collect(Collectors.toList());
	}

	/**
	 * The parameters to edit or an empty list.
	 *
	 * @return the parameters list to edit or an empty list, never {@code null}
	 */
	public List<ConnectionParameterModel> getParameters() {
		return parameters;
	}

	/**
	 * The available {@link ValueProvider ValueProviders} or an empty list.
	 *
	 * @return the available {@link ValueProvider ValueProviders} list or an empty list, never {@code null}
	 */
	public List<ValueProvider> getValueProviders() {
		return valueProviders;
	}

	/**
	 * Call me if changes to the parameters should be used
	 */
	public void setChangedParameters() {
		originalParameters.clear();
		originalParameters.addAll(copyParameters(parameters));
	}

	/**
	 * Call me if changes to the parameters should be used
	 */
	public void resetChangedParameters() {
		parameters.clear();
		parameters.addAll(copyParameters(originalParameters));
	}
}
