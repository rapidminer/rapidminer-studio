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

import com.rapidminer.connection.ConnectionInformation;
import com.rapidminer.repository.RepositoryLocation;


/**
 * A {@link ConnectionGUIProvider} that contains a method for editing a {@link ConnectionInformation}.
 *
 * @author Jonas Wilms-Pfau
 * @since 9.3
 */
public interface ConnectionGUIProvider {

	/**
	 * Creates an edit view for the given configuration.
	 *
	 * @param parent
	 * 		the parent window
	 * @param connection
	 * 		the connection to edit
	 * @param location
	 * 		the repository location
	 * @param editable
	 * 		if the connection is editable
	 * @return the gui for the connection configuration
	 */
	AbstractConnectionGUI edit(Window parent, ConnectionInformation connection, RepositoryLocation location, boolean editable);

}
