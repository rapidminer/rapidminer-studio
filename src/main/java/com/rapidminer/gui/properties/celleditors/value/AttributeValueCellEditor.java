/**
 * Copyright (C) 2001-2016 by RapidMiner and the contributors
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
import java.awt.event.ActionEvent;
import java.awt.event.FocusAdapter;
import java.awt.event.FocusEvent;
import java.awt.event.KeyAdapter;
import java.awt.event.KeyEvent;

import javax.swing.JComboBox;
import javax.swing.JTable;
import javax.swing.text.JTextComponent;

import com.rapidminer.gui.properties.DefaultRMCellEditor;
import com.rapidminer.operator.Operator;
import com.rapidminer.parameter.ParameterTypeAttribute;


/**
 * Value cell editor for attribute type.
 *
 * @author Sebastian Land
 */
public class AttributeValueCellEditor extends DefaultRMCellEditor implements PropertyValueCellEditor {

	private static final String COMBO_BOX_EDITED = "comboBoxEdited";
	private static final String COMBO_BOX_CHANGED = "comboBoxChanged";

	private static final long serialVersionUID = -1889899793777695100L;

	public AttributeValueCellEditor(ParameterTypeAttribute type) {
		super(new AttributeComboBox(type));
		final JComboBox comboBox = (JComboBox) editorComponent;
		final JTextComponent textField = (JTextComponent) comboBox.getEditor().getEditorComponent();

		comboBox.removeItemListener(this.delegate);
		comboBox.setEditable(true);
		comboBox.removeActionListener(delegate); // this is important since we are replacing the
		// original delegate

		this.delegate = new EditorDelegate() {

			private static final long serialVersionUID = -5592150438626222295L;

			@Override
			public void setValue(Object x) {
				if (x == null) {
					super.setValue(null);
					comboBox.setSelectedItem(null);
				} else {
					String value = x.toString();
					super.setValue(x);
					comboBox.setSelectedItem(value);
					if (value != null) {
						textField.setText(value.toString());
					} else {
						textField.setText("");
					}
				}

			}

			@Override
			public Object getCellEditorValue() {
				String selected = textField.getText();
				if (selected != null && selected.trim().length() == 0) {
					selected = null;
				}
				return selected;
			}

			@Override
			public void actionPerformed(ActionEvent event) {
				String actionCommand = event.getActionCommand();
				if (COMBO_BOX_EDITED.equals(actionCommand) || COMBO_BOX_CHANGED.equals(actionCommand)) {
					super.actionPerformed(event);
				}
			};
		};
		comboBox.addActionListener(delegate);

		textField.addFocusListener(new FocusAdapter() {

			@Override
			public void focusLost(FocusEvent e) {
				// The event is only fired if the focus loss is permanently,
				// i.e. it is not fired if the user e.g. just switched to another window.
				// Otherwise any changes made after switching back to RapidMiner would
				// not be saved for the same reasons as stated above.
				if (!e.isTemporary()) {
					comboBox.actionPerformed(new ActionEvent(comboBox, 12, COMBO_BOX_EDITED));
				}
				super.focusLost(e);
			}
		});

		textField.addKeyListener(new KeyAdapter() {

			@Override
			public void keyPressed(KeyEvent e) {
				if (e.getKeyCode() == KeyEvent.VK_ENTER) {
					if (!comboBox.isPopupVisible()) {
						comboBox.actionPerformed(new ActionEvent(comboBox, 12, COMBO_BOX_EDITED));
						e.consume();
					}
				}
			}
		});
	}

	@Override
	public boolean rendersLabel() {
		return false;
	}

	@Override
	public void setOperator(Operator operator) {

	}

	@Override
	public boolean useEditorAsRenderer() {
		return true;
	}

	@Override
	public Component getTableCellRendererComponent(JTable table, Object value, boolean isSelected, boolean hasFocus,
			int row, int column) {
		return getTableCellEditorComponent(table, value, hasFocus, row, column);
	}

}
