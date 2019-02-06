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

import com.rapidminer.gui.ApplicationFrame;
import com.rapidminer.gui.MainFrame;
import com.rapidminer.gui.PerspectiveController;
import com.rapidminer.gui.RapidMinerGUI;
import com.rapidminer.gui.tools.ResourceAction;
import com.rapidminer.gui.tools.SwingTools;
import com.rapidminer.gui.tools.dialogs.InputDialog;


/**
 *
 * @author Simon Fischer
 */
public class NewPerspectiveAction extends ResourceAction {

	private static final long serialVersionUID = 5526646387968616318L;

	private static class NewPerspectiveDialog extends InputDialog {

		private static final long serialVersionUID = -7106546247629834518L;

		private final PerspectiveController perspectiveController;

		private boolean ok = false;

		private NewPerspectiveDialog(PerspectiveController perspectiveController) {
			super(ApplicationFrame.getApplicationFrame(), "new_perspective");
			this.perspectiveController = perspectiveController;
		}

		public boolean isOk() {
			return ok;
		}

		@Override
		protected void ok() {
			if (perspectiveController.getModel().isValidName(getInputText())) {
				ok = true;
				dispose();
			} else {
				SwingTools.showVerySimpleErrorMessage(this, "invalid_perspective_name");
			}
		}
	}


	public NewPerspectiveAction() {
		super("new_perspective");
		setCondition(EDIT_IN_PROGRESS, DONT_CARE);
	}

	/**
	 * @deprecated use {@link #NewPerspectiveAction()} instead
	 */
	@Deprecated
	public NewPerspectiveAction(MainFrame mainFrame) {
		this();
	}

	@Override
	public void loggedActionPerformed(ActionEvent e) {
		NewPerspectiveDialog dialog = new NewPerspectiveDialog(RapidMinerGUI.getMainFrame().getPerspectiveController());
		dialog.setVisible(true);
		if (dialog.isOk()) {
			RapidMinerGUI.getMainFrame().getPerspectiveController().createUserPerspective(dialog.getInputText(), true);
		}
	}
}
