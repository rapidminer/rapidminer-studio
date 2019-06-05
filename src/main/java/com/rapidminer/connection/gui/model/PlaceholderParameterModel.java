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

import com.rapidminer.connection.gui.components.InjectedParameterPlaceholderLabel;

import javafx.beans.property.StringProperty;


/**
 * PlaceholderParameterModel, maps to {@link com.rapidminer.connection.configuration.PlaceholderParameter}
 *
 * <p>For components rendering this model, it is important to respect the isEncrypted and isInjected state
 * <ul>
 *     <li>It is required to not display the value of an encrypted parameter in plain text!</li>
 *     <li>Injected Parameters should be identifiable, see {@link InjectedParameterPlaceholderLabel InjectedParameterPlaceholderLabel}</li>
 * </ul>
 * </p>
 *
 * @author Jonas Wilms-Pfau
 * @since 9.3
 */
public class PlaceholderParameterModel extends ConnectionParameterModel {

	/**
	 * Copy constructor
	 *
	 * @param parent
	 * 		the new parent connection
	 * @param parameter
	 * 		the original parameter
	 */
	PlaceholderParameterModel(ConnectionModel parent, ConnectionParameterModel parameter) {
		super(new ConnectionParameterGroupModel(parent, parameter.getGroupName()), parameter);
	}

	/**
	 * Creates a new placeholder parameter
	 *
	 * @param parent
	 * 		the parent connection
	 * @param groupName
	 * 		the group name
	 * @param name
	 * 		the name of the parameter
	 * @param value
	 * 		the value of the parameter
	 * @param isEncrypted
	 * 		if the parameter is encrypted
	 * @param injectorName
	 * 		the name of the injector or {@code null}
	 * @param isEnabled
	 * 		if the parameter is enabled
	 */
	PlaceholderParameterModel(ConnectionModel parent, String groupName, String name, String value, boolean isEncrypted, String injectorName, boolean isEnabled) {
		super(new ConnectionParameterGroupModel(parent, groupName), name, value, isEncrypted, injectorName, isEnabled);
	}

	/**
	 * @return observable group name
	 */
	public StringProperty groupNameProperty() {
		return groupName;
	}

	/**
	 * Updates the group name
	 *
	 * @param groupName
	 * 		the new group name
	 */
	public void setGroupName(String groupName) {
		this.groupName.set(groupName);
	}

	@Override
	public PlaceholderParameterModel copyDataOnly() {
		return new PlaceholderParameterModel(getParent().getParent(), this);
	}

}
