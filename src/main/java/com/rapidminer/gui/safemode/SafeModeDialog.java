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
package com.rapidminer.gui.safemode;

import java.awt.event.ActionEvent;
import java.awt.event.KeyEvent;

import javax.swing.JButton;
import javax.swing.JComponent;
import javax.swing.KeyStroke;

import com.rapidminer.gui.tools.ResourceAction;
import com.rapidminer.gui.tools.SwingTools;
import com.rapidminer.gui.tools.SwingTools.ResultRunnable;
import com.rapidminer.gui.tools.dialogs.ConfirmDialog;


/**
 * @author Nils Woehler
 *
 */
public class SafeModeDialog extends ConfirmDialog {

	private static final long serialVersionUID = 1L;

	public SafeModeDialog(String key, int mode, boolean showAskAgainCheckbox, Object... arguments) {
		super(null, key, mode, showAskAgainCheckbox, arguments);
	}

	@Override
	protected JButton makeYesButton() {
		JButton yesButton = new JButton(new ResourceAction("start.without.extensions") {

			private static final long serialVersionUID = -8887199234055845095L;

			@Override
			public void loggedActionPerformed(ActionEvent e) {
				setReturnOption(YES_OPTION);
				yes();
			}
		});
		getRootPane().setDefaultButton(yesButton);
		return yesButton;
	}

	@Override
	protected JButton makeNoButton() {
		ResourceAction noAction = new ResourceAction("start.normally") {

			private static final long serialVersionUID = -8887199234055845095L;

			@Override
			public void loggedActionPerformed(ActionEvent e) {
				setReturnOption(NO_OPTION);
				no();
			}
		};
		getRootPane().getInputMap(JComponent.WHEN_IN_FOCUSED_WINDOW)
		        .put(KeyStroke.getKeyStroke(KeyEvent.VK_ESCAPE, 0, false), "NO");
		getRootPane().getActionMap().put("NO", noAction);
		JButton noButton = new JButton(noAction);
		return noButton;
	}

	public static int showSafeModeDialog(final String key, final int mode, final Object... i18nArgs) {
		return SwingTools.invokeAndWaitWithResult(new ResultRunnable<Integer>() {

			@Override
			public Integer run() {
				SafeModeDialog dialog = new SafeModeDialog(key, mode, false, i18nArgs);
				dialog.setAlwaysOnTop(true);
				dialog.setVisible(true);
				return dialog.getReturnOption();
			}
		});
	}

}
