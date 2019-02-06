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
package com.rapidminer.gui.properties.celleditors.value;

import java.awt.Component;
import java.awt.GridBagConstraints;
import java.awt.GridBagLayout;
import java.awt.Insets;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.awt.event.FocusEvent;
import java.awt.event.FocusListener;
import java.beans.PropertyVetoException;
import java.text.DateFormat;
import java.text.ParseException;
import java.util.Date;
import java.util.concurrent.atomic.AtomicBoolean;

import javax.swing.AbstractCellEditor;
import javax.swing.JPanel;
import javax.swing.JTable;
import javax.swing.JTextField;

import com.michaelbaranov.microba.calendar.DatePicker;
import com.rapidminer.operator.Operator;
import com.rapidminer.parameter.ParameterTypeDate;


/**
 * A cell editor for date parameters. It renders a {@TextField} and {@link DatePicker}
 * to choose dates comfortable.
 *
 * @author Nils Woehler
 *
 */
public class DateValueCellEditor extends AbstractCellEditor implements PropertyValueCellEditor {

	private static final long serialVersionUID = 1L;

	private final JPanel panel = new JPanel();

	private final JTextField textField = new JTextField();
	private final DatePicker datePicker = new DatePicker(new Date(), DateFormat.getDateTimeInstance());
	private AtomicBoolean updatingComponents = new AtomicBoolean(false);

	public DateValueCellEditor(final ParameterTypeDate type) {
		panel.setLayout(new GridBagLayout());
		panel.setToolTipText(type.getDescription());

		textField.setToolTipText(type.getDescription());
		textField.addActionListener(new ActionListener() {

			@Override
			public void actionPerformed(ActionEvent e) {
				fireEditingStopped();
			}
		});
		textField.addFocusListener(new FocusListener() {

			@Override
			public void focusLost(FocusEvent e) {
				// The event is only fired if the focus loss is permanently,
				// i.e. it is not fired if the user e.g. just switched to another window.
				if (!e.isTemporary()) {
					fireEditingStopped();
				}
			}

			@Override
			public void focusGained(FocusEvent e) {}
		});

		GridBagConstraints c = new GridBagConstraints();
		c.fill = GridBagConstraints.BOTH;
		c.weighty = 1;
		c.weightx = 1;
		panel.add(textField, c);

		datePicker.setPickerStyle(DatePicker.PICKER_STYLE_BUTTON);
		datePicker.setToolTipText(type.getDescription());
		datePicker.setStripTime(false);
		datePicker.setKeepTime(true);
		datePicker.setDropdownFocusable(false);
		datePicker.setShowNoneButton(false);
		datePicker.addActionListener(new ActionListener() {

			@Override
			public void actionPerformed(ActionEvent e) {
				// only apply change if we aren't currently updating to avoid loosing macros
				// this has to be checked because updating will trigger an action event too
				if (!updatingComponents.get()) {
					Date date = datePicker.getDate();
					if (date != null) {
						textField.setText(ParameterTypeDate.DATE_FORMAT.get().format(date));
						fireEditingStopped();
					}
				}
			}
		});

		c.weightx = 0;
		c.insets = new Insets(0, 5, 0, 0);
		panel.add(datePicker, c);
	}

	@Override
	public Object getCellEditorValue() {
		return textField.getText();
	}

	@Override
	public boolean rendersLabel() {
		return false;
	}

	@Override
	public void setOperator(Operator operator) {}

	@Override
	public boolean useEditorAsRenderer() {
		return false;
	}

	@Override
	public Component getTableCellEditorComponent(JTable table, Object value, boolean isSelected, int row, int column) {
		updateComponents(value);
		return panel;
	}

	@Override
	public Component getTableCellRendererComponent(JTable table, Object value, boolean isSelected, boolean hasFocus,
			int row, int column) {
		updateComponents(value);
		return panel;
	}

	private void updateComponents(Object value) {
		updatingComponents.set(true);
		textField.setText(value == null ? "" : value.toString());
		Date parsedDate;
		try {
			parsedDate = ParameterTypeDate.DATE_FORMAT.get().parse(textField.getText());
		} catch (ParseException e) {
			parsedDate = new Date();
		}
		try {
			datePicker.setDate(parsedDate);
		} catch (PropertyVetoException e) {
		}
		updatingComponents.set(false);
	}

}
