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

/**
 * Proxy test class to create an instance of the {@link ConnectionParameterModel}
 *
 * @author Andreas Timm
 * @since 9.3
 */
public class MyConnectionParameterModel extends PlaceholderParameterModel {

	public MyConnectionParameterModel(ConnectionModel parent, String groupName, String name, String value, boolean isEncrypted, String injectorName, boolean isEnabled) {
		super(parent, groupName, name, value, isEncrypted, injectorName, isEnabled);
	}

	@Override
	public boolean isEditable() {
		return true;
	}

	public String getType() {
		return "";
	}
}
