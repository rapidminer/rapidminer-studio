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
package com.rapidminer.operator.nio;

import java.awt.Color;
import java.awt.Component;
import java.awt.GridLayout;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.awt.event.FocusEvent;
import java.awt.event.FocusListener;
import java.util.EventObject;
import java.util.Objects;

import javax.swing.JCheckBox;
import javax.swing.JComboBox;
import javax.swing.JPanel;
import javax.swing.JTable;
import javax.swing.JTextField;
import javax.swing.event.CellEditorListener;
import javax.swing.event.ChangeEvent;
import javax.swing.event.EventListenerList;
import javax.swing.table.TableCellEditor;
import javax.swing.table.TableCellRenderer;

import com.rapidminer.example.Attributes;
import com.rapidminer.operator.nio.model.ColumnMetaData;
import com.rapidminer.tools.Ontology;


/**
 *
 * @author Simon Fischer
 *
 */
public class MetaDataTableHeaderCellEditor extends JPanel implements TableCellEditor, TableCellRenderer {

	private static final long serialVersionUID = 1L;

	private ColumnMetaData value;

	private EventListenerList cellEditorListeners = new EventListenerList();

	private JComboBox<String> valueTypeBox = new JComboBox<>(Ontology.ATTRIBUTE_VALUE_TYPE.getNames());
	private JCheckBox selectCheckBox = new JCheckBox();
	private JTextField nameField = new JTextField();
	private JComboBox<String> roleBox = new JComboBox<>(Attributes.KNOWN_ATTRIBUTE_TYPES);

	private MetaDataValidator validator;

	public MetaDataTableHeaderCellEditor() {
		super(new GridLayout(4, 1));
		roleBox.setEditable(true);

		add(selectCheckBox);
		add(nameField);
		add(valueTypeBox);
		add(roleBox);

		valueTypeBox.addActionListener(new ActionListener() {

			@Override
			public void actionPerformed(ActionEvent e) {
				if (value != null) {
					value.setAttributeValueType(valueTypeBox.getSelectedIndex());
				}
			}
		});
		nameField.addFocusListener(new FocusListener() {

			@Override
			public void focusLost(FocusEvent e) {
				if (value != null) {
					final String text = nameField.getText();
					if ((text != null) && !text.isEmpty()) {
						value.setUserDefinedAttributeName(text);
					} else {
						nameField.setText(value.getOriginalAttributeName());
						value.setUserDefinedAttributeName(value.getOriginalAttributeName());
					}
				}
			}

			@Override
			public void focusGained(FocusEvent e) {
				// not needed
			}
		});
		selectCheckBox.addActionListener(new ActionListener() {

			@Override
			public void actionPerformed(ActionEvent e) {
				if (value != null) {
					value.setSelected(selectCheckBox.isSelected());
				}
			}
		});
		roleBox.addActionListener(new ActionListener() {

			@Override
			public void actionPerformed(ActionEvent e) {
				if (value != null) {
					value.setRole(roleBox.getSelectedItem().toString());
				}
			}
		});
	}

	public MetaDataTableHeaderCellEditor(MetaDataValidator headerValidator) {
		this();
		validator = headerValidator;
	}

	@Override
	public Object getCellEditorValue() {
		return value;
	}

	@Override
	public boolean isCellEditable(EventObject anEvent) {
		return true;
	}

	@Override
	public boolean shouldSelectCell(EventObject anEvent) {
		return true;
	}

	@Override
	public boolean stopCellEditing() {
		if (value != null) {
			value.setUserDefinedAttributeName(nameField.getText());
		}
		fireEditingStopped();
		return true;
	}

	private void fireEditingStopped() {
		CellEditorListener[] listeners = listenerList.getListeners(CellEditorListener.class);
		ChangeEvent changeEvent = null;
		for (CellEditorListener l : listeners) {
			if (changeEvent == null) {
				changeEvent = new ChangeEvent(this);
			}
			l.editingStopped(changeEvent);
		}
	}

	private void fireEditingCancelled() {
		CellEditorListener[] listeners = listenerList.getListeners(CellEditorListener.class);
		ChangeEvent changeEvent = null;
		for (CellEditorListener l : listeners) {
			if (changeEvent == null) {
				changeEvent = new ChangeEvent(this);
			}
			l.editingCanceled(changeEvent);
		}
	}

	@Override
	public void cancelCellEditing() {
		fireEditingCancelled();
	}

	@Override
	public void addCellEditorListener(CellEditorListener l) {
		cellEditorListeners.add(CellEditorListener.class, l);
	}

	@Override
	public void removeCellEditorListener(CellEditorListener l) {
		cellEditorListeners.remove(CellEditorListener.class, l);
	}

	@Override
	public Component getTableCellEditorComponent(JTable table, Object value, boolean isSelected, int row, int column) {
		setMetaData((ColumnMetaData) value, column);
		return this;
	}

	@Override
	public Component getTableCellRendererComponent(JTable table, Object value, boolean isSelected, boolean hasFocus,
			int row, int column) {
		setMetaData((ColumnMetaData) value, column);
		return this;
	}

	private void setMetaData(ColumnMetaData value, int column) {
		this.value = value;
		valueTypeBox.setSelectedIndex(value.getAttributeValueType());
		selectCheckBox.setSelected(value.isSelected());
		nameField.setText(value.getUserDefinedAttributeName());
		if (validator != null && validator.isDuplicateNameColumn(column)) {
			nameField.setBackground(Color.red);
		} else {
			nameField.setBackground(this.getBackground());
		}
		if (!Objects.equals(value.getRole(), roleBox.getSelectedItem())) {
			roleBox.setSelectedItem(value.getRole());
		}
		if (validator != null && validator.isDuplicateRoleColumn(column)) {
			roleBox.getEditor().getEditorComponent().setBackground(Color.red);
		} else {
			roleBox.getEditor().getEditorComponent().setBackground(this.getBackground());
		}
	}

	public void updateColumnMetaData() {
		if (value != null) {
			// value type
			value.setAttributeValueType(valueTypeBox.getSelectedIndex());

			// value name
			final String text = nameField.getText();
			if ((text != null) && !text.isEmpty()) {
				value.setUserDefinedAttributeName(text);
			} else {
				nameField.setText(value.getOriginalAttributeName());
				value.setUserDefinedAttributeName(value.getOriginalAttributeName());
			}

			// value selected
			value.setSelected(selectCheckBox.isSelected());

			// value role
			value.setRole(roleBox.getSelectedItem().toString());
		}
	}

}
