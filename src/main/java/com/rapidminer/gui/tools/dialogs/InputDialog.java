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
package com.rapidminer.gui.tools.dialogs;

import java.awt.Window;

import javax.swing.JTextField;

import com.rapidminer.gui.ApplicationFrame;


/**
 *
 * @author Tobias Malbrecht
 */
public class InputDialog extends ButtonDialog {

	private static final long serialVersionUID = -5825873580778775409L;

	private final JTextField textField = new JTextField();

	/**
	 * Creates an input dialog for the user to enter something.
	 *
	 * @param key
	 *            i18n key
	 * @deprecated use {@link #InputDialog(Window, String)} instead
	 */
	@Deprecated
	public InputDialog(String key) {
		this(ApplicationFrame.getApplicationFrame(), key);
	}

	/**
	 * Creates an input dialog for the user to enter something.
	 *
	 * @param key
	 *            i18n key
	 * @param text
	 *            the text to display
	 * @param arguments
	 *            additional i18n arguments
	 * @deprecated use {@link #InputDialog(Window, String, String, Object...)} instead
	 */
	@Deprecated
	public InputDialog(String key, String text, Object... arguments) {
		this(ApplicationFrame.getApplicationFrame(), key, text, arguments);
	}

	/**
	 * Creates an input dialog for the user to enter something.
	 *
	 * @param owner
	 *            the owner window where this dialog is displayed in
	 * @param key
	 *            i18n key
	 * @since 6.5.0
	 */
	public InputDialog(Window owner, String key) {
		this(owner, key, null);
	}

	/**
	 *
	 * @param owner
	 *            the owner window where this dialog is displayed in
	 * @param key
	 *            i18n key
	 * @param text
	 *            the text to display
	 * @param arguments
	 *            additional i18n arguments
	 * @since 6.5.0
	 */
	public InputDialog(Window owner, String key, String text, Object... arguments) {
		super(owner, "input." + key, ModalityType.APPLICATION_MODAL,
				arguments);
		if (text != null) {
			textField.setText(text);
		}
		layoutDefault(textField, makeOkButton(), makeCancelButton());
	}

	public String getInputText() {
		return textField.getText();
	}
}
