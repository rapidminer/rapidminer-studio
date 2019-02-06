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

package com.rapidminer.gui.operatortree.actions;

import java.awt.event.ActionEvent;

import com.rapidminer.gui.RapidMinerGUI;
import com.rapidminer.gui.tools.ResourceAction;
import com.rapidminer.tools.I18N;


/**
 * Removes all breakpoints from the current process.
 *
 * @author JoaoPedroPinheiro
 * @since 8.2.0
 */
public class RemoveAllBreakpointsAction extends ResourceAction {


	private static final String I18N_KEY = "remove_all_breakpoints";

	private static final String TOOLTIP_MESSAGE_ENABLED_I18N_KEY ="gui.action." + I18N_KEY + ".tip";
	private static final String TOOLTIP_MESSAGE_DISABLED_I18N_KEY="gui.action." + I18N_KEY + ".disabled.tip";


	{
		setCondition(PROCESS_HAS_BREAKPOINTS, MANDATORY);
	}

	public RemoveAllBreakpointsAction() {
		super(true, I18N_KEY);
	}

	@Override
	public void loggedActionPerformed(ActionEvent e) {
		RapidMinerGUI.getMainFrame().getProcess().removeAllBreakpoints();
	}

	@Override
	public void setEnabled(boolean enabled) {
		super.setEnabled(enabled);

		// change tooltip so if the action is disabled the user can see what needs to be done to
		// enable the action
		String tip;
		if (enabled) {
			tip = I18N.getMessageOrNull(I18N.getGUIBundle(), TOOLTIP_MESSAGE_ENABLED_I18N_KEY);
		} else {
			tip = I18N.getMessageOrNull(I18N.getGUIBundle(), TOOLTIP_MESSAGE_DISABLED_I18N_KEY);
		}
		if (tip != null) {
			putValue(SHORT_DESCRIPTION, tip);
		}
	}
}
