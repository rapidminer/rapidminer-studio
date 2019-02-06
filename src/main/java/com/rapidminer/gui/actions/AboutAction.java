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

import com.rapidminer.RapidMiner;
import com.rapidminer.core.license.ProductConstraintManager;
import com.rapidminer.gui.MainFrame;
import com.rapidminer.gui.RapidMinerGUI;
import com.rapidminer.gui.tools.ResourceAction;
import com.rapidminer.gui.tools.dialogs.AboutBox;


/**
 *
 * @author Simon Fischer
 */
public class AboutAction extends ResourceAction {

	private static final long serialVersionUID = 1L;


	public AboutAction() {
		super("about");
		setCondition(EDIT_IN_PROGRESS, DONT_CARE);
	}

	/**
	 * @deprecated use {@link #AboutAction()} instead
	 */
	@Deprecated
	public AboutAction(MainFrame mainFrame) {
		this();
	}

	@Override
	public void loggedActionPerformed(ActionEvent e) {
		new AboutBox(RapidMinerGUI.getMainFrame(), RapidMiner.getLongVersion(), ProductConstraintManager.INSTANCE.getActiveLicense())
				.setVisible(true);
	}

}
