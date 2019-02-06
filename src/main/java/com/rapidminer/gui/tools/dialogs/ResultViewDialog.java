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
package com.rapidminer.gui.tools.dialogs;

import java.awt.Window;

import javax.swing.Icon;
import javax.swing.JComponent;

import com.rapidminer.gui.ApplicationFrame;
import com.rapidminer.gui.tools.SwingTools;


/**
 *
 * @author Tobias Malbrecht
 */
public class ResultViewDialog extends ButtonDialog {

	private static final long serialVersionUID = -1111667366524535499L;

	/**
	 * Displays a dialog with the given result component.
	 *
	 * @param i18nKey
	 *            the i18n key
	 * @param results
	 *            the result component to display
	 * @param i18nArgs
	 *            the i18n arguments
	 * @deprecated use {@link #ResultViewDialog(Window, String, JComponent, Object...)} instead
	 */
	@Deprecated
	public ResultViewDialog(String i18nKey, JComponent results, Object... i18nArgs) {
		this(ApplicationFrame.getApplicationFrame(), i18nKey, results, i18nArgs);
	}

	/**
	 * Displays a dialog with the given result component.
	 *
	 * @param owner
	 *            the owner where the dialog is shown in
	 * @param i18nKey
	 *            the i18n key
	 * @param results
	 *            the result component to display
	 * @param i18nArgs
	 *            the i18n arguments
	 */
	public ResultViewDialog(Window owner, String i18nKey, JComponent results, Object... i18nArgs) {
		super(owner, "results." + i18nKey, ModalityType.APPLICATION_MODAL, i18nArgs);
		results.setBorder(createBorder());
		layoutDefault(results, ButtonDialog.LARGE, makeCloseButton());
	}

	@Override
	protected Icon getInfoIcon() {
		return SwingTools.createIcon("48/presentation_chart.png");
	}
}
