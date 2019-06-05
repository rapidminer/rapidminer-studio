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

import javafx.collections.FXCollections;
import javafx.collections.ObservableList;


/**
 * ConnectionParameterGroupModel, converts to {@link com.rapidminer.connection.configuration.ConfigurationParameterGroup ConfigurationParameterGroup}.
 *
 * @author Jonas Wilms-Pfau
 * @since 9.3
 */
public class ConnectionParameterGroupModel {

	private final String name;
	private final ObservableList<ConnectionParameterModel> parameters = FXCollections.synchronizedObservableList(FXCollections.observableArrayList());
	private final ConnectionModel parent;

	/**
	 * Copy constructor
	 *
	 * @param parent
	 * 		the new parent connection
	 * @param group
	 * 		the original group
	 */
	ConnectionParameterGroupModel(ConnectionModel parent, ConnectionParameterGroupModel group) {
		this.name = group.name;
		this.parent = parent;
		for (ConnectionParameterModel parameter : group.parameters) {
			this.parameters.add(new ConnectionParameterModel(this, parameter));
		}
	}

	/**
	 * Creates a new parameter group
	 *
	 * @param parent
	 * 		the new parent
	 * @param name
	 * 		the
	 */
	ConnectionParameterGroupModel(ConnectionModel parent, String name) {
		this.parent = parent;
		this.name = name;
	}

	/**
	 * @return the name of the parameter group
	 */
	public String getName() {
		return name;
	}

	/**
	 * @return immutable observable list
	 */
	public ObservableList<ConnectionParameterModel> getParameters() {
		return FXCollections.unmodifiableObservableList(parameters);
	}

	/**
	 * Adds a parameter to this group
	 *
	 * @param name
	 * 		the name of the parameter
	 * @param value
	 * 		the value of the parameter
	 * @param isEncrypted
	 * 		if the parameter is encrypted
	 * @param injectorName
	 * 		the name of the value provider
	 * @param isEnabled
	 * 		if the parameter is enabled
	 * @return {@code true} in case the parameter was added, {@code false} if it was modified
	 */
	public synchronized boolean addOrSetParameter(String name, String value, boolean isEncrypted, String injectorName, boolean isEnabled) {
		ConnectionParameterModel parameter = getParameter(name);

		if (parameter != null) {
			parameter.setValue(value);
			parameter.setEncrypted(isEncrypted);
			parameter.setInjectorName(injectorName);
			parameter.setEnabled(isEnabled);
			return false;
		}

		return parameters.add(new ConnectionParameterModel(this, name, value, isEncrypted, injectorName, isEnabled));
	}

	/**
	 * Gets a parameter by its name
	 *
	 * @param parameterName
	 * 		the parameter name
	 * @return the parameter for this name, or {@code null}
	 */
	public ConnectionParameterModel getParameter(String parameterName) {
		for (ConnectionParameterModel parameter : parameters) {
			if (parameter.getName().equals(parameterName)) {
				return parameter;
			}
		}
		return null;
	}

	/**
	 * Removes a parameter
	 *
	 * @param parameterName
	 * 		the parameter name
	 * @return {@code true} if the parameter was removed
	 */
	public boolean removeParameter(String parameterName) {
		return parameters.removeIf(p -> p.getName().equals(parameterName));
	}

	/**
	 * Creates a copy of the data <strong>without the listeners</strong>
	 *
	 * @return a copy without the listeners
	 */
	public ConnectionParameterGroupModel copyDataOnly() {
		return new ConnectionParameterGroupModel(this.parent, this);
	}

	/**
	 * @return the connection this parameter group belongs to
	 */
	ConnectionModel getParent() {
		return parent;
	}

}
