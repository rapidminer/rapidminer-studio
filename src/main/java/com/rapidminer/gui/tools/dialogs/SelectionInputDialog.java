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
import java.awt.event.ItemEvent;
import java.awt.event.ItemListener;
import java.util.Collection;

import javax.swing.JButton;
import javax.swing.JComboBox;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.SwingConstants;

import com.rapidminer.gui.ApplicationFrame;


/**
 * Dialog with a selection input field and an input validation.
 *
 * @param <T>
 *            the class for the combobox model
 *
 * @author Tobias Malbrecht, Marcel Michel
 */
public class SelectionInputDialog<T> extends ButtonDialog {

	private static final long serialVersionUID = -5825873580778775409L;

	private final JComboBox<T> comboBox = new JComboBox<>();

	private final InputValidator<T> inputValidator;

	private final JButton okButton;

	private JLabel errorLabel;

	/**
	 * Create a SelectionInputDIalog.
	 *
	 * @param key
	 *            i18n key
	 * @param selectionValues
	 *            the available selection values
	 * @param initialSelectionValue
	 *            the initially selected value
	 * @deprecated use {@link #SelectionInputDialog(Window, String, Object[], Object)}
	 */
	@Deprecated
	public SelectionInputDialog(String key, T[] selectionValues, T initialSelectionValue) {
		this(ApplicationFrame.getApplicationFrame(), key, selectionValues, initialSelectionValue);
	}

	/**
	 * Create a SelectionInputDIalog.
	 *
	 * @param key
	 *            the i18n key
	 * @param selectionValues
	 *            the available selection values
	 * @param initialSelectionValue
	 *            the initially selected value
	 * @param keyArguments
	 *            additional i18n arguments
	 * @deprecated use {@link #SelectionInputDialog(Window, String, Object[], Object, Object...)}
	 *             instead
	 */
	@Deprecated
	public SelectionInputDialog(String key, T[] selectionValues, T initialSelectionValue, Object... keyArguments) {
		this(ApplicationFrame.getApplicationFrame(), key, selectionValues, initialSelectionValue, keyArguments);
	}

	/**
	 * Create a SelectionInputDIalog whose Combobox can be editable.
	 *
	 * @param key
	 *            the i18n key
	 * @param editable
	 *            if the selection should be editable
	 * @param selectionValues
	 *            the available selection values
	 * @param initialSelectionValue
	 *            the initially selected value
	 * @param keyArguments
	 *            additional i18n arguments
	 * @deprecated use
	 *             {@link #SelectionInputDialog(Window, String, boolean, Collection, Object, Object...)}
	 *             instead
	 */
	@Deprecated
	public SelectionInputDialog(String key, boolean editable, Collection<T> selectionValues, T initialSelectionValue,
			Object... keyArguments) {
		this(ApplicationFrame.getApplicationFrame(), key, editable, selectionValues, initialSelectionValue, keyArguments);
	}

	/**
	 * Create a SelectionInputDIalog whose Combobox can be editable.
	 *
	 * @param key
	 *            the i18n key
	 * @param editable
	 *            if the selection should be editable
	 * @param selectionValues
	 *            the available selection values
	 * @param initialSelectionValue
	 *            the initially selected value
	 * @param keyArguments
	 *            additional i18n arguments
	 * @deprecated use
	 *             {@link #SelectionInputDialog(Window, String, boolean, Object[], Object, Object...)}
	 *             instead
	 */
	@Deprecated
	public SelectionInputDialog(String key, boolean editable, T[] selectionValues, T initialSelectionValue,
			Object... keyArguments) {
		this(ApplicationFrame.getApplicationFrame(), key, editable, selectionValues, initialSelectionValue, keyArguments);
	}

	/**
	 * Create a SelectionInputDIalog.
	 *
	 * @param key
	 *            the i18n key
	 * @param selectionValues
	 *            the available selection values
	 * @param initialSelectionValue
	 *            the initially selected value
	 * @param keyArguments
	 *            additional i18n arguments
	 * @deprecated use {@link #SelectionInputDialog(Window, String, Collection, Object, Object...)}
	 *             instead
	 */
	@Deprecated
	public SelectionInputDialog(String key, Collection<T> selectionValues, T initialSelectionValue, Object... keyArguments) {
		this(ApplicationFrame.getApplicationFrame(), key, selectionValues, initialSelectionValue, keyArguments);
	}

	/**
	 * Create a SelectionInputDIalog.
	 *
	 * @param key
	 *            i18n key
	 * @param selectionValues
	 *            the available selection values
	 * @param initialSelectionValue
	 *            the initially selected value
	 * @since 6.5.0
	 */
	public SelectionInputDialog(Window owner, String key, T[] selectionValues, T initialSelectionValue) {
		this(owner, key, selectionValues, initialSelectionValue, new Object[] {});
	}

	/**
	 * Create a SelectionInputDIalog.
	 *
	 * @param key
	 *            the i18n key
	 * @param selectionValues
	 *            the available selection values
	 * @param initialSelectionValue
	 *            the initially selected value
	 * @param keyArguments
	 *            additional i18n arguments
	 * @since 6.5.0
	 */
	public SelectionInputDialog(Window owner, String key, T[] selectionValues, T initialSelectionValue,
			Object... keyArguments) {
		this(owner, key, selectionValues, initialSelectionValue, null, keyArguments);
	}

	/**
	 * Create a SelectionInputDIalog.
	 *
	 * @param key
	 *            the i18n key
	 * @param selectionValues
	 *            the available selection values
	 * @param initialSelectionValue
	 *            the initially selected value
	 * @param inputValidator
	 *            used to validate the input and to show an error message
	 * @param keyArguments
	 *            additional i18n arguments
	 * @since 7.0.0
	 */
	public SelectionInputDialog(Window owner, String key, T[] selectionValues, T initialSelectionValue,
			InputValidator<T> inputValidator, Object... keyArguments) {
		super(owner, "input." + key, ModalityType.APPLICATION_MODAL, keyArguments);
		this.inputValidator = inputValidator;
		this.okButton = makeOkButton();
		for (T selectionValue : selectionValues) {
			comboBox.addItem(selectionValue);
		}
		comboBox.setSelectedItem(initialSelectionValue);
		initGui();
	}

	/**
	 * Create a SelectionInputDIalog whose Combobox can be editable.
	 *
	 * @param key
	 *            the i18n key
	 * @param editable
	 *            if the selection should be editable
	 * @param selectionValues
	 *            the available selection values
	 * @param initialSelectionValue
	 *            the initially selected value
	 * @param inputValidator
	 *            used to validate the input and to show an error message
	 * @param keyArguments
	 *            additional i18n arguments
	 * @since 7.0.0
	 */
	public SelectionInputDialog(Window owner, String key, boolean editable, Collection<T> selectionValues,
			T initialSelectionValue, InputValidator<T> inputValidator, Object... keyArguments) {
		this(owner, key, selectionValues, initialSelectionValue, inputValidator, keyArguments);
		comboBox.setEditable(editable);
	}

	/**
	 * Create a SelectionInputDIalog whose Combobox can be editable.
	 *
	 * @param key
	 *            the i18n key
	 * @param editable
	 *            if the selection should be editable
	 * @param selectionValues
	 *            the available selection values
	 * @param initialSelectionValue
	 *            the initially selected value
	 * @param keyArguments
	 *            additional i18n arguments
	 * @since 6.5.0
	 */
	public SelectionInputDialog(Window owner, String key, boolean editable, Collection<T> selectionValues,
			T initialSelectionValue, Object... keyArguments) {
		this(owner, key, selectionValues, initialSelectionValue, keyArguments);
		comboBox.setEditable(editable);
	}

	/**
	 * Create a SelectionInputDialog whose Combobox can be editable.
	 *
	 * @param key
	 *            the i18n key
	 * @param editable
	 *            if the selection should be editable
	 * @param selectionValues
	 *            the available selection values
	 * @param initialSelectionValue
	 *            the initially selected value
	 * @param keyArguments
	 *            additional i18n arguments
	 * @since 6.5.0
	 */
	public SelectionInputDialog(Window owner, String key, boolean editable, T[] selectionValues, T initialSelectionValue,
			Object... keyArguments) {
		this(owner, key, selectionValues, initialSelectionValue, keyArguments);
		comboBox.setEditable(editable);
	}

	/**
	 * Create a SelectionInputDialog whose Combobox can be editable.
	 *
	 * @param key
	 *            the i18n key
	 * @param editable
	 *            if the selection should be editable
	 * @param selectionValues
	 *            the available selection values
	 * @param initialSelectionValue
	 *            the initially selected value
	 * @param keyArguments
	 *            additional i18n arguments
	 * @since 7.0.0
	 */
	public SelectionInputDialog(Window owner, String key, boolean editable, T[] selectionValues, T initialSelectionValue,
			InputValidator<T> inputValidator, Object... keyArguments) {
		this(owner, key, selectionValues, initialSelectionValue, inputValidator, keyArguments);
		comboBox.setEditable(editable);
	}

	/**
	 * Create a SelectionInputDialog.
	 *
	 * @param key
	 *            the i18n key
	 * @param selectionValues
	 *            the available selection values
	 * @param initialSelectionValue
	 *            the initially selected value
	 * @param keyArguments
	 *            additional i18n arguments
	 * @since 6.5.0
	 */
	public SelectionInputDialog(Window owner, String key, Collection<T> selectionValues, T initialSelectionValue,
			Object... keyArguments) {
		this(owner, key, selectionValues, initialSelectionValue, null, keyArguments);
	}

	/**
	 * Create a SelectionInputiIalog.
	 *
	 * @param key
	 *            the i18n key
	 * @param selectionValues
	 *            the available selection values
	 * @param initialSelectionValue
	 *            the initially selected value
	 * @param inputValidator
	 *            used to validate the input and to show an error message
	 * @param keyArguments
	 *            additional i18n arguments
	 * @since 6.5.0
	 */
	public SelectionInputDialog(Window owner, String key, Collection<T> selectionValues, T initialSelectionValue,
			InputValidator<T> inputValidator, Object... keyArguments) {
		super(owner, "input." + key, ModalityType.APPLICATION_MODAL, keyArguments);
		this.inputValidator = inputValidator;
		this.okButton = makeOkButton();
		for (T selectionValue : selectionValues) {
			comboBox.addItem(selectionValue);
		}
		comboBox.setSelectedItem(initialSelectionValue);
		initGui();
	}

	private void initGui() {
		if (inputValidator == null) {
			errorLabel = null;
			layoutDefault(comboBox, okButton, makeCancelButton());
		} else {
			JPanel panel = new JPanel(new GridBagLayout());

			GridBagConstraints gbc = new GridBagConstraints();
			gbc.weightx = 1;
			gbc.gridy = 0;
			gbc.fill = GridBagConstraints.HORIZONTAL;
			panel.add(comboBox, gbc);

			gbc.gridy += 1;
			gbc.insets = new Insets(5, 5, 5, 5);
			errorLabel = new JLabel(" ", SwingConstants.RIGHT);
			errorLabel.setForeground(Color.RED);
			panel.add(errorLabel, gbc);

			comboBox.addItemListener(new ItemListener() {

				@Override
				public void itemStateChanged(ItemEvent e) {
					checkText();
				}
			});

			layoutDefault(panel, okButton, makeCancelButton());
		}
	}

	@Override
	protected void ok() {
		if (inputValidator != null) {
			String error = inputValidator.validateInput(comboBox.getItemAt(comboBox.getSelectedIndex()));
			updateError(error);
			if (error == null) {
				super.ok();
			}
		} else {
			super.ok();
		}
	}

	private void checkText() {
		updateError(inputValidator.validateInput(comboBox.getItemAt(comboBox.getSelectedIndex())));
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

	/**
	 * @return the selected input in case the dialog was confirmed or {@code null} in case the user
	 *         aborted the dialog.
	 *         <p>
	 *         In case the {@link #comboBox} model also contains {@code null} as a possible value
	 *         check via {@link #wasConfirmed()} whether the dialog was confirmed.
	 */
	@SuppressWarnings("unchecked")
	public T getInputSelection() {
		return wasConfirmed() ? (T) comboBox.getSelectedItem() : null;
	}
}
