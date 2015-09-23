/**
 * Copyright (C) 2001-2015 by RapidMiner and the contributors
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
package com.rapidminer.gui.autosave;

import java.awt.event.ActionEvent;
import java.awt.event.KeyEvent;

import javax.swing.JButton;
import javax.swing.JComponent;
import javax.swing.KeyStroke;

import com.rapidminer.gui.ApplicationFrame;
import com.rapidminer.gui.tools.ResourceAction;
import com.rapidminer.gui.tools.dialogs.ConfirmDialog;


/**
 * @author Venkatesh Umaashankar
 *
 */
public class RecoverDialog extends ConfirmDialog {

	private static final long serialVersionUID = -5352785145613909431L;

	/**
	 * @param key
	 * @param mode
	 * @param showAskAgainCheckbox
	 * @param arguments
	 */
	public RecoverDialog(String processPath) {
		super(ApplicationFrame.getApplicationFrame(), "recover.process", ConfirmDialog.YES_NO_OPTION, false, processPath);
	}

	@Override
	protected JButton makeYesButton() {
		JButton yesButton = new JButton(new ResourceAction("recover.process.yes") {

			private static final long serialVersionUID = -8887199234055845095L;

			@Override
			public void actionPerformed(ActionEvent e) {
				returnOption = YES_OPTION;
				yes();
			}
		});
		getRootPane().setDefaultButton(yesButton);
		return yesButton;
	}

	@Override
	protected JButton makeNoButton() {
		ResourceAction noAction = new ResourceAction("recover.process.no") {

			private static final long serialVersionUID = -8887199234055845095L;

			@Override
			public void actionPerformed(ActionEvent e) {
				returnOption = NO_OPTION;
				no();
			}
		};
		getRootPane().getInputMap(JComponent.WHEN_IN_FOCUSED_WINDOW).put(
				KeyStroke.getKeyStroke(KeyEvent.VK_ESCAPE, 0, false), "NO");
		getRootPane().getActionMap().put("NO", noAction);
		return new JButton(noAction);
	}

}
