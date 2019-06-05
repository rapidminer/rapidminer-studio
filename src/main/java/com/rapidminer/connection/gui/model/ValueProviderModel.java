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

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.function.Function;
import java.util.stream.Collectors;

import com.rapidminer.connection.valueprovider.ValueProvider;
import com.rapidminer.connection.valueprovider.ValueProviderParameter;

import javafx.beans.property.SimpleStringProperty;
import javafx.beans.property.StringProperty;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;

/**
 * Observable {@link ValueProvider} implementation which is only used for the UI.
 *
 * @author Jonas Wilms-Pfau
 * @since 9.3
 */
public class ValueProviderModel implements ValueProvider {

	private final String type;
	private final StringProperty name = new SimpleStringProperty();
	private final ObservableList<ValueProviderParameterModel> parameters = FXCollections.observableArrayList();

	/**
	 * Copy constructor
	 *
	 * @param valueProviderModel
	 * 		the original value provider model
	 */
	ValueProviderModel(ValueProvider valueProviderModel) {
		this.type = valueProviderModel.getType();
		this.name.set(valueProviderModel.getName());
		for (ValueProviderParameter parameter : valueProviderModel.getParameters()) {
			this.parameters.add(new ValueProviderParameterModel(parameter));
		}
	}

	/**
	 * Creates a new ValueProviderModel
	 *
	 * @param name
	 * 		the name of the value provider
	 * @param type
	 * 		the type of the value provider
	 * @param parameters
	 * 		the parameters of the value provider
	 */
	ValueProviderModel(String name, String type, List<ValueProviderParameterModel> parameters) {
		this.type = type;
		this.name.setValue(name);
		this.parameters.setAll(parameters);
	}

	@Override
	public String getType() {
		return type;
	}

	@Override
	public String getName() {
		return name.get();
	}

	/**
	 * @return observable name property
	 */
	public StringProperty nameProperty() {
		return name;
	}

	/**
	 * Updates the name of the value provider
	 *
	 * @param name
	 * 		the new name of the value provider
	 */
	public void setName(String name) {
		this.name.set(name);
	}

	@Override
	public List<ValueProviderParameter> getParameters() {
		return new ArrayList<>(parameters);
	}

	/**
	 * @return observable parameters property
	 */
	public ObservableList<ValueProviderParameterModel> parametersProperty() {
		return parameters;
	}

	@Override
	public Map<String, ValueProviderParameter> getParameterMap() {
		return parameters.stream().collect(Collectors.toMap(ValueProviderParameter::getName, Function.identity()));
	}

	/**
	 * Replaces the current parameters with the given ones
	 *
	 * @param parameters
	 * 		the new parameters
	 */
	public void setParameters(List<ValueProviderParameterModel> parameters) {
		this.parameters.setAll(parameters);
	}

	/**
	 * Creates a copy of the data <strong>without the listeners</strong>
	 *
	 * @return a copy without the listeners
	 */
	public ValueProviderModel copyDataOnly() {
		return new ValueProviderModel(this);
	}
}
