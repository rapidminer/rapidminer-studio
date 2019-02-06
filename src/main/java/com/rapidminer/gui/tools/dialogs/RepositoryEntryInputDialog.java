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

import javax.swing.JButton;

import com.rapidminer.gui.ApplicationFrame;
import com.rapidminer.gui.tools.RepositoryEntryTextField;
import com.rapidminer.tools.Observable;
import com.rapidminer.tools.Observer;


/**
 * Shows an input dialog which uses the {@link RepositoryEntryTextField}.
 *
 * @author Marco Boeck
 */
public class RepositoryEntryInputDialog extends ButtonDialog implements Observer<Boolean> {

	private static final long serialVersionUID = -5825873580778775409L;

	private final JButton okButton;
	private final JButton cancelButton;

	private final RepositoryEntryTextField textField = new RepositoryEntryTextField();

	/**
	 * @deprecated use {@link #RepositoryEntryInputDialog(Window, String, String, Object...)}
	 *             instead
	 * @param key
	 */
	@Deprecated
	public RepositoryEntryInputDialog(String key) {
		this(ApplicationFrame.getApplicationFrame(), key, null);
	}

	/**
	 * @deprecated use {@link #RepositoryEntryInputDialog(Window, String, String, Object...)}
	 *             instead
	 * @param key
	 */
	@Deprecated
	public RepositoryEntryInputDialog(String key, String text, Object... arguments) {
		this(ApplicationFrame.getApplicationFrame(), key, text, arguments);
	}

	/**
	 * Display an input dialog for a repository entry.
	 *
	 * @param owner
	 *            the owner of the input dialog
	 * @param key
	 *            the i18n key
	 * @param text
	 *            the text to display
	 * @param arguments
	 *            optional i18n arguments
	 */
	public RepositoryEntryInputDialog(Window owner, String key, String text, Object... arguments) {
		super(owner, "input." + key, ModalityType.APPLICATION_MODAL, arguments);
		this.okButton = makeOkButton();
		this.cancelButton = makeCancelButton();
		if (text != null) {
			textField.setText(text);
		}
		textField.addObserver(this, true);
		layoutDefault(textField, okButton, cancelButton);
		textField.requestFocusInWindow();
		textField.triggerCheck();
	}

	public String getInputText() {
		return textField.getText();
	}

	@Override
	public void update(Observable<Boolean> observable, Boolean arg) {
		okButton.setEnabled(arg);
	}
}
