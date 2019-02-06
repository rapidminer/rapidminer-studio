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
import com.rapidminer.gui.tools.ResourceAction;
import com.vlsolutions.swing.docking.DockingSelectorDialog;

import java.awt.event.ActionEvent;


/**
 * 
 * @author Simon Fischer
 */
public class EditViewAction extends ResourceAction {

	private static final long serialVersionUID = -4418293515072657330L;

	private MainFrame mainFrame;

	public EditViewAction(MainFrame mainFrame) {
		super("edit_view");
		this.mainFrame = mainFrame;
	}

	@Override
	public void loggedActionPerformed(ActionEvent e) {
		DockingSelectorDialog d = new DockingSelectorDialog(mainFrame);
		d.setDockingDesktop(mainFrame.getDockingDesktop());
		d.pack();
		d.setLocationRelativeTo(mainFrame);
		d.setVisible(true);
	}

}
