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

import com.rapidminer.Process;
import com.rapidminer.gui.MainFrame;
import com.rapidminer.gui.RapidMinerGUI;
import com.rapidminer.operator.ports.Port;
import com.rapidminer.tools.ParameterService;

import java.awt.event.ActionEvent;


/**
 * Determines whether the process setup is validated automatically on any update.
 * 
 * @author Simon Fischer
 * 
 */
public class ValidateAutomaticallyAction extends ToggleAction {

	private static final String PROPERTY_VALIDATE_AUTOMATICALLY = "rapidminer.gui.validate_automatically";

	public ValidateAutomaticallyAction() {
		super(false, "validate_automatically");
		setSelected(!"false".equals(ParameterService.getParameterValue(PROPERTY_VALIDATE_AUTOMATICALLY)));
	}

	private static final long serialVersionUID = 1L;

	@Override
	public void actionToggled(ActionEvent e) {
		MainFrame mainFrame = RapidMinerGUI.getMainFrame();
		if (isSelected()) {
			mainFrame.validateProcess(mainFrame.getProcessState() != Process.PROCESS_STATE_RUNNING);
		} else {
			mainFrame.getProcess().getRootOperator().clear(Port.CLEAR_ALL_ERRORS | Port.CLEAR_ALL_METADATA);
			mainFrame.fireProcessUpdated();
		}
		ParameterService.setParameterValue(PROPERTY_VALIDATE_AUTOMATICALLY, Boolean.toString(isSelected()));
		ParameterService.saveParameters();
	}
}
