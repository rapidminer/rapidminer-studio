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

import java.awt.Dialog;
import java.awt.Dimension;
import java.util.Collection;
import java.util.LinkedList;
import java.util.List;

import javax.swing.AbstractButton;
import javax.swing.Box;
import javax.swing.BoxLayout;
import javax.swing.ButtonGroup;
import javax.swing.JCheckBox;
import javax.swing.JOptionPane;
import javax.swing.JPanel;
import javax.swing.JRadioButton;

import com.rapidminer.gui.ApplicationFrame;
import com.rapidminer.tools.I18N;


/**
 * The SelectionDialog provides two types selection options: Radio buttons and check boxes. It can
 * be used to provide a combination of user choices: Firstly, a user can choose between a set of
 * options (radio buttons). Secondly, a user can choose between multiple binary options
 * (checkboxes). In addition, the request of the dialog can be confirmed or canceled by using
 * different modes.
 *
 * @author Adrian Wilke
 */
public class SelectionDialog extends ButtonDialog {

	/** Dialog mode: Shows only OK button. */
	public static final int OK_OPTION = JOptionPane.OK_OPTION;

	/** Dialog mode: Shows OK and Cancel button. */
	public static final int OK_CANCEL_OPTION = JOptionPane.OK_CANCEL_OPTION;

	/** Return value from class method if CANCEL is chosen. */
	private static final int CANCEL_OPTION = JOptionPane.CANCEL_OPTION;

	private static final long serialVersionUID = 846327738639063294L;

	// ButtonDialog makeInfoPanel(): hgap/2 + border
	private static final int BUTTON_DIALOG_LEFT_GAP = 10 + 16;
	private static final int GAP_BETWEEN_SELECTIONS = 16;

	private static final String I18N_PREFIX_DIALOG = "gui.dialog.";
	private static final String I18N_PREFIX_SELECTION = "selection.";

	private int mode;

	private List<String> optionsToSelect = new LinkedList<>();
	private List<String> optionsToCheck = new LinkedList<>();

	private JPanel selectionPanel = new JPanel();
	private JPanel checkboxPanel = new JPanel();

	/**
	 * Constructs new dialog. Only sets object variables.
	 *
	 * @param key
	 *            The key used for i18n
	 * @param mode
	 *            Defines available buttons
	 * @param arguments
	 *            The arguments used for i18n
	 */
	public SelectionDialog(String key, int mode, Object[] arguments) {
		super(ApplicationFrame.getApplicationFrame(), I18N_PREFIX_SELECTION + key, ModalityType.APPLICATION_MODAL, arguments);
		this.mode = mode;
	}

	/**
	 * Constructs new dialog. Only sets object variables.
	 *
	 * @param key
	 *            The key used for i18n
	 * @param mode
	 *            Defines available buttons
	 * @param arguments
	 *            The arguments used for i18n
	 * @param optionsToSelect
	 *            A list of i18n keys to display as radio buttons
	 * @param optionsToCheck
	 *            A list of i18n keys to display as check boxes
	 */
	public SelectionDialog(String key, int mode, Object[] arguments, List<String> optionsToSelect,
			List<String> optionsToCheck) {
		this(key, mode, arguments);
		this.optionsToSelect = optionsToSelect;
		this.optionsToCheck = optionsToCheck;
	}

	/**
	 * Constructs new dialog. Only sets object variables.
	 *
	 * @param owner
	 *            {@code Dialog} from which the dialog is displayed or {@code null} if this dialog
	 *            has no owner
	 * @param key
	 *            The key used for i18n
	 * @param mode
	 *            Defines available buttons
	 * @param arguments
	 *            The arguments used for i18n
	 */
	public SelectionDialog(Dialog owner, String key, int mode, Object[] arguments) {
		super(owner, I18N_PREFIX_SELECTION + key, ModalityType.APPLICATION_MODAL, arguments);
		this.mode = mode;
	}

	/**
	 * Constructs new dialog. Only sets object variables.
	 *
	 * @param owner
	 *            {@code Dialog} from which the dialog is displayed or {@code null} if this dialog
	 *            has no owner
	 * @param key
	 *            The key used for i18n
	 * @param mode
	 *            Defines available buttons
	 * @param arguments
	 *            The arguments used for i18n
	 * @param optionsToSelect
	 *            A list of i18n keys to display as radio buttons
	 * @param optionsToCheck
	 *            A list of i18n keys to display as check boxes
	 */
	public SelectionDialog(Dialog owner, String key, int mode, Object[] arguments, List<String> optionsToSelect,
			List<String> optionsToCheck) {
		this(owner, key, mode, arguments);
		this.optionsToSelect = optionsToSelect;
		this.optionsToCheck = optionsToCheck;
	}

	/** Adds an option to select. */
	public void addOptionToSelect(String option) {
		if (optionsToSelect == null) {
			optionsToSelect = new LinkedList<>();
		}
		optionsToSelect.add(option);
	}

	/** Adds an option to check. */
	public void addOptionToCheck(String option) {
		if (optionsToCheck == null) {
			optionsToCheck = new LinkedList<>();
		}
		optionsToCheck.add(option);
	}

	/** Constructs the JComponent and shows the dialog. */
	public SelectionDialog showDialog() {

		Collection<AbstractButton> buttons = new LinkedList<>();
		switch (mode) {
			case OK_OPTION:
				buttons.add(makeOkButton());
				break;
			case OK_CANCEL_OPTION:
				buttons.add(makeOkButton());
				buttons.add(makeCancelButton());
				break;
			default:
				break;
		}

		layoutDefault(constructJComponent(), buttons);
		this.setVisible(true);
		return this;
	}

	/** Constructs panel containing the main GUI components. */
	private JPanel constructJComponent() {

		// Selection panel

		BoxLayout selectionPanelLayout = new BoxLayout(selectionPanel, BoxLayout.Y_AXIS);
		selectionPanel.setLayout(selectionPanelLayout);
		ButtonGroup radioButtons = new ButtonGroup();
		if (optionsToSelect != null) {
			for (int i = 0; i < optionsToSelect.size(); i++) {
				JRadioButton radioButton = new JRadioButton(getI18n(optionsToSelect.get(i)));
				radioButton.setHorizontalAlignment(JRadioButton.LEFT);
				if (i == 0) {
					radioButton.setSelected(true);
				}
				radioButtons.add(radioButton);
				selectionPanel.add(radioButton);
			}
		}

		// Checkbox panel

		BoxLayout checkboxPanelLayout = new BoxLayout(checkboxPanel, BoxLayout.Y_AXIS);
		checkboxPanel.setLayout(checkboxPanelLayout);
		if (optionsToCheck != null) {
			for (int i = 0; i < optionsToCheck.size(); i++) {
				JCheckBox jCheckBox = new JCheckBox(getI18n(optionsToCheck.get(i)));
				jCheckBox.setHorizontalAlignment(JCheckBox.LEFT);
				checkboxPanel.add(jCheckBox);
			}
		}

		// Overall panel

		JPanel panel = new JPanel();
		BoxLayout panelLayout = new BoxLayout(panel, BoxLayout.Y_AXIS);
		panel.setLayout(panelLayout);
		panel.add(selectionPanel);
		if (optionsToSelect != null && !optionsToSelect.isEmpty() && optionsToCheck != null && !optionsToCheck.isEmpty()) {
			panel.add(Box.createRigidArea(new Dimension(0, GAP_BETWEEN_SELECTIONS)));
		}
		panel.add(checkboxPanel);

		JPanel leftMarginPanel = new JPanel();
		BoxLayout leftMarginPanelLayout = new BoxLayout(leftMarginPanel, BoxLayout.X_AXIS);
		leftMarginPanel.setLayout(leftMarginPanelLayout);
		leftMarginPanel.add(Box.createRigidArea(new Dimension(getInfoIcon().getIconWidth() + BUTTON_DIALOG_LEFT_GAP, 0)));
		leftMarginPanel.add(panel);

		return leftMarginPanel;
	}

	/**
	 * Gets code of the pressed button.
	 *
	 * @return Option code of pressed button
	 */
	public int getResult() {
		if (wasConfirmed()) {
			return OK_OPTION;
		} else {
			return CANCEL_OPTION;
		}
	}

	/**
	 * Checks, if the specified radio button is selected.
	 *
	 * @param index
	 *            Index of the radio button to examine
	 * @return true, if the radio button is selected
	 */
	public boolean isOptionSelected(int index) {
		if (index >= selectionPanel.getComponents().length) {
			return false;
		}
		JRadioButton radioButton = (JRadioButton) selectionPanel.getComponent(index);
		if (radioButton.isSelected()) {
			return true;
		} else {
			return false;
		}
	}

	/**
	 * Checks, if the specified radio button containing the i18n String of the given key is
	 * selected.
	 *
	 * @param key
	 *            i18n key
	 * @return true, if a matching radio button is selected
	 */
	public boolean isOptionSelected(String key) {
		for (int i = 0; i < selectionPanel.getComponents().length; i++) {
			JRadioButton radioButton = (JRadioButton) selectionPanel.getComponent(i);
			if (radioButton.getText().equals(getI18n(key))) {
				if (radioButton.isSelected()) {
					return true;
				} else {
					return false;
				}
			}
		}
		return false;
	}

	/**
	 * Checks, if the specified check box is selected.
	 *
	 * @param index
	 *            Index of the check box to examine
	 * @return true, if the check box is selected
	 */
	public boolean isOptionChecked(int index) {
		if (index >= checkboxPanel.getComponents().length) {
			return false;
		}
		JCheckBox checkbox = (JCheckBox) checkboxPanel.getComponent(index);
		if (checkbox.isSelected()) {
			return true;
		} else {
			return false;
		}
	}

	/**
	 * Checks, if the specified check box containing the i18n String of the given key is selected.
	 *
	 * @param key
	 *            i18n key
	 * @return true, if a matching check box is selected
	 */
	public boolean isOptionChecked(String key) {
		for (int i = 0; i < checkboxPanel.getComponents().length; i++) {
			JCheckBox checkbox = (JCheckBox) checkboxPanel.getComponent(i);
			if (checkbox.getText().equals(getI18n(key))) {
				if (checkbox.isSelected()) {
					return true;
				} else {
					return false;
				}
			}
		}
		return false;
	}

	/** Gets Internationalization string for the specified key. */
	private String getI18n(String key) {
		return I18N.getMessage(I18N.getGUIBundle(), I18N_PREFIX_DIALOG + I18N_PREFIX_SELECTION + key);

	}
}
