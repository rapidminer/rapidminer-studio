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
import com.rapidminer.gui.RapidMinerGUI;
import com.rapidminer.operator.DebugMode;


/**
 * Start the corresponding action.
 *
 * @author Marco Boeck
 */
public class PropagateRealMetaDataAction extends ToggleAction {

	private static final long serialVersionUID = -1317229512005928906L;


	public PropagateRealMetaDataAction() {
		super(true, "process_debug_mode");
	}

	/**
	 * @deprecated use {@link #PropagateRealMetaDataAction()} instead
	 */
	@Deprecated
	public PropagateRealMetaDataAction(MainFrame mainFrame) {
		this();
	}

	@Override
	public void actionToggled(ActionEvent e) {
		if (!isSelected()) {
			RapidMinerGUI.getMainFrame().getProcess().setDebugMode(DebugMode.COLLECT_METADATA_AFTER_EXECUTION);
		} else {
			RapidMinerGUI.getMainFrame().getProcess().setDebugMode(DebugMode.DEBUG_OFF);
		}
	}

	@Override
	public boolean isSelected() {
		return RapidMinerGUI.getMainFrame() != null && RapidMinerGUI.getMainFrame().getProcess().getDebugMode() == DebugMode.COLLECT_METADATA_AFTER_EXECUTION;
	}
}
