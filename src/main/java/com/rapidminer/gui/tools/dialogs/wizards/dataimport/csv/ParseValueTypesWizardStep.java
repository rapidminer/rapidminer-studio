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
package com.rapidminer.gui.tools.dialogs.wizards.dataimport.csv;

import com.rapidminer.gui.tools.CharTextField;
import com.rapidminer.gui.tools.dialogs.ButtonDialog;
import com.rapidminer.gui.tools.dialogs.wizards.dataimport.MetaDataDeclerationWizardStep;
import com.rapidminer.operator.io.AbstractDataReader;
import com.rapidminer.parameter.UndefinedParameterError;
import com.rapidminer.tools.DateParser;
import com.rapidminer.tools.StrictDecimalFormat;

import java.awt.BorderLayout;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.awt.event.KeyAdapter;
import java.awt.event.KeyEvent;

import javax.swing.JCheckBox;
import javax.swing.JComponent;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.JTextField;


/**
 * 
 * @author Tobias Malbrecht, Sebastian Loh
 */

public abstract class ParseValueTypesWizardStep extends MetaDataDeclerationWizardStep {

	private final KeyAdapter textFieldKeyListener = new KeyAdapter() {

		@Override
		public void keyReleased(KeyEvent e) {
			settingsChanged();
		}
	};

	protected void settingsChanged() {
		reader.setParameter(DateParser.PARAMETER_DATE_FORMAT, getDateFormat());
		reader.setParameter(StrictDecimalFormat.PARAMETER_DECIMAL_CHARACTER, Character.toString(getDecimalPointCharacter()));
		reader.setParameter(StrictDecimalFormat.PARAMETER_GROUPING_CHARACTER, Character.toString(getGroupingSeparator()));
	}

	private final CharTextField decimalPointCharacterTextField = new CharTextField(
			StrictDecimalFormat.DEFAULT_DECIMAL_CHARACTER);
	{
		decimalPointCharacterTextField.addKeyListener(textFieldKeyListener);
	}

	private final CharTextField groupingCharacterTextField = new CharTextField(
			StrictDecimalFormat.DEFAULT_GROUPING_CHARACTER);
	{
		groupingCharacterTextField.addKeyListener(textFieldKeyListener);
		groupingCharacterTextField.setEnabled(false);
	}

	private final JCheckBox groupNumbersBox = new JCheckBox("Digit Grouping", false);
	{
		groupNumbersBox.addActionListener(new ActionListener() {

			@Override
			public void actionPerformed(ActionEvent e) {
				groupingCharacterTextField.setEnabled(groupNumbersBox.isSelected());
				settingsChanged();
			}
		});
	}

	// TODO add cell editor of custom date format parameter type (still to implement also)
	private final JTextField dateFormatTextField = new JTextField();
	{
		String format = DateParser.DEFAULT_DATE_TIME_FORMAT;
		try {
			format = reader.getParameter(DateParser.PARAMETER_DATE_FORMAT);
		} catch (UndefinedParameterError e) {
			// nothing to do
		}
		dateFormatTextField.setText(format);
		dateFormatTextField.addKeyListener(textFieldKeyListener);
	}

	public ParseValueTypesWizardStep(String key, AbstractDataReader reader) {
		super(key, reader);
		// a different text for the CSV version
		super.tolerateErrorCheckBox.setText("Read non matching values as missings and tolerate too short rows.");
	}

	protected char getDecimalPointCharacter() {
		return decimalPointCharacterTextField.getCharacter();
	}

	protected boolean groupDigits() {
		return groupNumbersBox.isSelected();
	}

	protected char getGroupingSeparator() {
		return groupingCharacterTextField.getCharacter();
	}

	protected String getDateFormat() {
		return dateFormatTextField.getText();
	}

	@Override
	protected void doAfterEnteringAction() {}

	@Override
	protected JComponent getComponent() {
		JPanel detectionPanel = new JPanel(ButtonDialog.createGridLayout(4, 2));
		detectionPanel.add(new JLabel("Guess the value types of all attributes"));
		detectionPanel.add(super.guessingButtonsPanel);
		detectionPanel.add(new JLabel("Decimal Character"));
		detectionPanel.add(decimalPointCharacterTextField);
		detectionPanel.add(groupNumbersBox);
		detectionPanel.add(groupingCharacterTextField);
		detectionPanel.add(new JLabel("Date Format"));
		detectionPanel.add(dateFormatTextField);
		detectionPanel.setBorder(ButtonDialog.createTitledBorder("Type Detection"));

		JPanel parsingPanel = new JPanel(ButtonDialog.createGridLayout(1, 2));
		parsingPanel.add(detectionPanel);

		JComponent editorPanel = super.getComponent();
		editorPanel.setBorder(null);

		JPanel panel = new JPanel(new BorderLayout(0, ButtonDialog.GAP));
		panel.add(parsingPanel, BorderLayout.NORTH);
		panel.add(editorPanel, BorderLayout.CENTER);
		return panel;
	}
}
