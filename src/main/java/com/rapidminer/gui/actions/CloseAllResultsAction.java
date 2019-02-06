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

import com.rapidminer.gui.MainFrame;
import com.rapidminer.gui.RapidMinerGUI;
import com.rapidminer.gui.tools.ResourceAction;
import com.rapidminer.gui.tools.dialogs.DecisionRememberingConfirmDialog;

import java.awt.event.ActionEvent;


/**
 * An action to close all currently open results.
 * 
 * @author Marco Boeck
 * 
 */
public class CloseAllResultsAction extends ResourceAction {

	private final MainFrame mainframe;

	public CloseAllResultsAction(MainFrame mainframe) {
		super(true, "close_all_results");
		this.mainframe = mainframe;
	}

	private static final long serialVersionUID = 1L;

	@Override
	public void loggedActionPerformed(ActionEvent e) {
		if (mainframe != null) {
			if (mainframe.getResultDisplay() != null) {
				if (DecisionRememberingConfirmDialog.confirmAction("close_all_results",
						RapidMinerGUI.PROPERTY_CLOSE_ALL_RESULTS_NOW)) {
					mainframe.getResultDisplay().clearAll();
				}
			}
		}
	}
}
