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
package com.rapidminer.connection.gui;

import java.awt.Window;

import javax.swing.JComponent;

import com.rapidminer.connection.ConnectionInformation;
import com.rapidminer.connection.gui.model.ConnectionModel;
import com.rapidminer.connection.gui.model.ConnectionParameterGroupModel;
import com.rapidminer.repository.RepositoryLocation;

/**
 * Default {@link ConnectionGUI} implementation in case the type was not registered.
 *
 * <p>Does only display the info panel. Not editable.</p>
 *
 * @author Jonas Wilms-Pfau
 * @since 9.3
 */
public class UnknownConnectionTypeGUI extends DefaultConnectionGUI {

	public UnknownConnectionTypeGUI(Window parent, ConnectionInformation connection, RepositoryLocation location, boolean editable) {
		super(parent, connection, location, false);
	}

	@Override
	protected void addSourcesTab() {
		// do nothing
	}

	@Override
	public JComponent getComponentForGroup(ConnectionParameterGroupModel groupModel, ConnectionModel connectionModel) {
		return null;
	}
}
