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
package com.rapidminer.gui.actions.search;

import java.awt.event.ActionEvent;

import com.rapidminer.gui.tools.DockingTools;
import com.rapidminer.gui.tools.ResourceAction;


/**
 * Opens/Displays the given dockable identified by its dock key.
 *
 * @author Marco Boeck
 * @since 8.1
 */
class DockableAction extends ResourceAction {

	private static final long serialVersionUID = 1L;

	private final String dockKey;


	DockableAction(final String dockKey) {
		super("global_search.action.dockable_" + dockKey);
		setCondition(EDIT_IN_PROGRESS, DONT_CARE);

		this.dockKey = dockKey;
	}

	@Override
	public void loggedActionPerformed(ActionEvent e) {
		DockingTools.openDockable(dockKey);
	}
}
