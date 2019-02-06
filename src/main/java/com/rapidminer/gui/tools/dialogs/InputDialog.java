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

import java.awt.Color;
import java.awt.GridBagConstraints;
import java.awt.GridBagLayout;
import java.awt.Insets;
import java.awt.Window;

import javax.swing.JButton;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.JTextField;
import javax.swing.SwingConstants;
import javax.swing.event.DocumentEvent;
import javax.swing.event.DocumentListener;

import com.rapidminer.gui.ApplicationFrame;


/**
 * Dialog with an input field and an input validation.
 *
 * @author Tobias Malbrecht, Marcel Michel
 */
public class InputDialog extends ButtonDialog {

	private static final long serialVersionUID = -5825873580778775409L;

	private final JTextField textField = new JTextField();

	private final InputValidator<String> inputValidator;

	private final JLabel errorLabel;

	private final JButton okButton;

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
	 * Creates an input dialog for the user to enter something.
	 *
	 * @param owner
	 *            the owner window where this dialog is displayed in
	 * @param key
	 *            i18n key
	 * @param inputValidator
	 *            used to validate the input and to show an error message
	 * @since 7.0.0
	 */
	public InputDialog(Window owner, String key, InputValidator<String> inputValidator) {
		this(owner, key, null, inputValidator);
	}

	/**
	 * Creates an input dialog for the user to enter something.
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
		this(owner, key, text, null, arguments);
	}

	/**
	 * Creates an input dialog for the user to enter something.
	 *
	 * @param owner
	 *            the owner window where this dialog is displayed in
	 * @param key
	 *            i18n key
	 * @param text
	 *            the text to display
	 * @param inputValidator
	 *            used to validate the input and to show an error message
	 * @param arguments
	 *            additional i18n arguments
	 * @since 7.0.0
	 */
	public InputDialog(Window owner, String key, String text, final InputValidator<String> inputValidator,
			Object... arguments) {
		super(owner, "input." + key, ModalityType.APPLICATION_MODAL, arguments);
		this.inputValidator = inputValidator;
		this.okButton = makeOkButton();
		if (text != null) {
			textField.setText(text);
		}
		if (inputValidator == null) {
			errorLabel = null;
			layoutDefault(textField, okButton, makeCancelButton());
		} else {
			JPanel panel = new JPanel(new GridBagLayout());

			GridBagConstraints gbc = new GridBagConstraints();
			gbc.weightx = 1;
			gbc.gridy = 0;
			gbc.fill = GridBagConstraints.HORIZONTAL;
			panel.add(textField, gbc);

			gbc.gridy += 1;
			gbc.insets = new Insets(5, 5, 5, 5);
			errorLabel = new JLabel(" ", SwingConstants.RIGHT);
			errorLabel.setForeground(Color.RED);
			panel.add(errorLabel, gbc);

			textField.getDocument().addDocumentListener(new DocumentListener() {

				@Override
				public void removeUpdate(DocumentEvent e) {
					checkText();
				}

				@Override
				public void insertUpdate(DocumentEvent e) {
					checkText();
				}

				@Override
				public void changedUpdate(DocumentEvent e) {
					checkText();
				}
			});
			layoutDefault(panel, okButton, makeCancelButton());
		}
	}

	@Override
	protected void ok() {
		if (inputValidator != null) {
			String error = inputValidator.validateInput(textField.getText());
			updateError(error);
			if (error == null) {
				super.ok();
			}
		} else {
			super.ok();
		}
	}

	private void checkText() {
		updateError(inputValidator.validateInput(textField.getText()));
	}

	private void updateError(String error) {
		if (error != null) {
			errorLabel.setText(error);
			okButton.setEnabled(false);
		} else {
			errorLabel.setText(" ");
			okButton.setEnabled(true);
		}
	}

	public String getInputText() {
		return textField.getText();
	}
}
