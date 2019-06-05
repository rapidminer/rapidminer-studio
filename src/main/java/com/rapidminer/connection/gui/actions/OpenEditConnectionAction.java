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
package com.rapidminer.connection.gui.actions;

import java.awt.Window;
import java.awt.event.ActionEvent;

import com.rapidminer.connection.gui.ConnectionEditDialog;
import com.rapidminer.connection.gui.dto.ConnectionInformationHolder;
import com.rapidminer.gui.tools.ResourceAction;


/**
 * Closes the view window and opens the Dialog with edit rights
 *
 * @author Jonas Wilms-Pfau
 * @since 9.3.0
 */
public class OpenEditConnectionAction extends ResourceAction {

	private final Window parent;
	private final transient ConnectionInformationHolder connection;

	public OpenEditConnectionAction(Window parent, ConnectionInformationHolder holder) {
		super("edit_connection");
		this.parent = parent;
		this.connection = holder;
	}

	@Override
	protected void loggedActionPerformed(ActionEvent e) {
		String currentTabTitle = null;
		if (parent instanceof ConnectionEditDialog) {
			currentTabTitle = ((ConnectionEditDialog) parent).getCurrentTabTitle();
		}
		this.parent.dispose();

		ConnectionEditDialog dialog = new ConnectionEditDialog(connection);
		if (currentTabTitle != null) {
			dialog.showTab(currentTabTitle);
		}
		dialog.setVisible(true);
	}
}
