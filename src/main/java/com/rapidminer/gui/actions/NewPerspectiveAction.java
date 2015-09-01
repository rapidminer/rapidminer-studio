/**
 * Copyright (C) 2001-2015 by RapidMiner and the contributors
 *
 * Complete list of developers available at our web site:
 *
 *      http://rapidminer.com
 *
 * This program is free software: you can redistribute it and/or modify
 * it under the terms of the GNU Affero General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU Affero General Public License for more details.
 *
 * You should have received a copy of the GNU Affero General Public License
 * along with this program.  If not, see http://www.gnu.org/licenses/.
 */
package com.rapidminer.gui.actions;

import com.rapidminer.gui.ApplicationPerspectives;
import com.rapidminer.gui.MainFrame;
import com.rapidminer.gui.tools.ResourceAction;
import com.rapidminer.gui.tools.SwingTools;
import com.rapidminer.gui.tools.dialogs.InputDialog;

import java.awt.event.ActionEvent;


/**
 * 
 * @author Simon Fischer
 */
public class NewPerspectiveAction extends ResourceAction {

	private static final long serialVersionUID = 5526646387968616318L;

	private static class NewPerspectiveDialog extends InputDialog {

		private static final long serialVersionUID = -7106546247629834518L;

		private final ApplicationPerspectives perspectives;

		private boolean ok = false;

		private NewPerspectiveDialog(ApplicationPerspectives perspectives) {
			super("new_perspective");
			this.perspectives = perspectives;
		}

		public boolean isOk() {
			return ok;
		}

		@Override
		protected void ok() {
			if (perspectives.isValidName(getInputText())) {
				ok = true;
				dispose();
			} else {
				SwingTools.showVerySimpleErrorMessage("invalid_perspective_name");
			}
		}
	}

	private final MainFrame mainFrame;

	public NewPerspectiveAction(MainFrame mainFrame) {
		super("new_perspective");
		this.mainFrame = mainFrame;
		setCondition(EDIT_IN_PROGRESS, DONT_CARE);
	}

	@Override
	public void actionPerformed(ActionEvent e) {
		NewPerspectiveDialog dialog = new NewPerspectiveDialog(mainFrame.getPerspectives());
		dialog.setVisible(true);
		if (dialog.isOk()) {
			mainFrame.getPerspectives().createUserPerspective(dialog.getInputText(), true);
		}
	}
}
