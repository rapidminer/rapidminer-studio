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
package com.rapidminer.gui.actions;

import java.awt.event.ActionEvent;

import com.rapidminer.gui.tools.ResourceAction;


/**
 * Action to create a new {@link com.rapidminer.connection.ConnectionInformation Connection}.
 *
 * @author Marco Boeck
 * @since 9.3.0
 */
public class CreateConnectionAction extends ResourceAction {


	public CreateConnectionAction() {
		super("create_connection");
	}

	@Override
	public void loggedActionPerformed(ActionEvent e) {
		com.rapidminer.repository.gui.actions.CreateConnectionAction.createConnection(null);
	}
}
