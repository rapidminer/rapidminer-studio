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
import javax.swing.JPanel;

import com.rapidminer.gui.ApplicationFrame;
import com.rapidminer.gui.tools.SwingTools;
import com.rapidminer.tools.I18N;


/**
 *
 * @author Tobias Malbrecht
 */
public class ErrorDialog extends ButtonDialog {

	private static final long serialVersionUID = -5825873580778775409L;

	/**
	 * Displays an error dialog.
	 *
	 * @param key
	 *            the i18n key
	 * @param arguments
	 *            further i18n arguments
	 * @deprecated use {@link #ErrorDialog(Window, String, Object...)} instead
	 */
	@Deprecated
	public ErrorDialog(String key, Object... arguments) {
		this(ApplicationFrame.getApplicationFrame(), key, arguments);
	}

	/**
	 * Displays an error dialog.
	 *
	 * @param owner
	 *            the owner window where this dialog is displayed in
	 * @param key
	 *            the i18n key
	 * @param arguments
	 *            further i18n arguments
	 * @since 6.5.0
	 */
	public ErrorDialog(Window owner, String key, Object... arguments) {
		super(owner, "error." + key, ModalityType.APPLICATION_MODAL,
				arguments);
		layoutDefault(new JPanel(), makeOkButton());

	}

	@Override
	protected Icon getInfoIcon() {
		return SwingTools.createIcon("48/" + I18N.getMessage(I18N.getGUIBundle(), "gui.dialog.error.icon"));
	}
}
