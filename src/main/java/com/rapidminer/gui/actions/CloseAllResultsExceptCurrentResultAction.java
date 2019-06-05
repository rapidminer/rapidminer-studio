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

import com.rapidminer.gui.MainFrame;
import com.rapidminer.gui.tools.ResourceAction;


/**
 * An action to close all currently open results except for the result that this action was triggered on.
 *
 * @author Marco Boeck
 * @since 9.3
 */
public class CloseAllResultsExceptCurrentResultAction extends ResourceAction {

	private final MainFrame mainframe;
	private final String dockKey;


	public CloseAllResultsExceptCurrentResultAction(MainFrame mainframe, String dockKey) {
		super(true, "close_all_results_except_current");
		this.mainframe = mainframe;
		this.dockKey = dockKey;
	}

	@Override
	public void loggedActionPerformed(ActionEvent e) {
		if (mainframe != null && mainframe.getResultDisplay() != null) {
			mainframe.getResultDisplay().clearAllExcept(dockKey);
		}
	}
}
