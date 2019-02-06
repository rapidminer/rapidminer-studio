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

import com.rapidminer.gui.properties.PropertyPanel;
import com.rapidminer.operator.Operator;
import com.rapidminer.parameter.ParameterType;
import com.rapidminer.parameter.ParameterTypeTupel;

import java.awt.Component;
import java.awt.GridLayout;
import java.awt.event.FocusEvent;
import java.awt.event.FocusListener;

import javax.swing.AbstractCellEditor;
import javax.swing.ComboBoxEditor;
import javax.swing.JComboBox;
import javax.swing.JPanel;
import javax.swing.JTable;
import javax.swing.SwingUtilities;
import javax.swing.plaf.basic.BasicComboBoxEditor;


/**
 * An editor for a tuple of parameters.
 * 
 * @author Simon Fischer, Nils Woehler, Marius Helf
 */
public class ParameterTupelCellEditor extends AbstractCellEditor implements PropertyValueCellEditor {

	private static final long serialVersionUID = -2387465714767785072L;

	private JPanel panel;

	/**
	 * The parameter types of the columns
	 */
	private ParameterType[] types;
	private PropertyValueCellEditor[] editors;

	private Operator operator;

	private final FocusListener focusListener;

	public ParameterTupelCellEditor(ParameterTypeTupel type) {
		types = type.getParameterTypes();
		focusListener = new FocusListener() {

			@Override
			public void focusLost(FocusEvent e) {
				// fire only if the focus didn't move to another descendant of the containing panel.
				// If this check
				// would not be included, fireEditingStopped() would prevent switching between tuple
				// components.
				// Additionally, the event is only fired if the focus loss is permanently,
				// i.e. it is not fired if the user e.g. just switched to another window.
				// Otherwise any changes made after switching back to RapidMiner would
				// not be saved for the same reasons as stated above.
				Component oppositeComponent = e.getOppositeComponent();
				if ((oppositeComponent == null || (oppositeComponent != panel && !SwingUtilities.isDescendingFrom(
						oppositeComponent, panel))) && !e.isTemporary()) {
					fireEditingStopped();
				}
			}

			@Override
			public void focusGained(FocusEvent e) {
				// if focus is passed to the root panel and we have at least one
				// subcomponent (editor), then we pass the focus on to the first
				// editor
				if (e.getComponent() == panel && panel.getComponentCount() > 0) {
					panel.getComponent(0).requestFocusInWindow();
				}
			}
		};

	}

	@Override
	public Object getCellEditorValue() {
		String[] values = new String[editors.length];
		for (int i = 0; i < editors.length; i++) {
			if (editors[i].getCellEditorValue() != null) {
				values[i] = editors[i].getCellEditorValue().toString();
			}
		}
		return ParameterTypeTupel.transformTupel2String(values);
	}

	@Override
	public boolean rendersLabel() {
		return false;
	}

	@Override
	public void setOperator(Operator operator) {
		this.operator = operator;
	}

	@Override
	public boolean useEditorAsRenderer() {
		return true;
	}

	@Override
	public Component getTableCellEditorComponent(JTable table, Object value, boolean isSelected, int row, int column) {
		String[] tupel;
		if (value instanceof String) {
			tupel = ParameterTypeTupel.transformString2Tupel((String) value);
		} else {
			tupel = (String[]) value;
		}
		if (panel == null) {
			constructPanel(tupel);
		}
		for (int i = 0; i < editors.length; i++) {
			editors[i].getTableCellEditorComponent(null, tupel[i], false, 0, 0);
		}
		return panel;
	}

	private void constructPanel(String[] values) {
		// constructing editors
		editors = new PropertyValueCellEditor[types.length];
		for (int i = 0; i < types.length; i++) {
			editors[i] = PropertyPanel.instantiateValueCellEditor(types[i], operator);
		}

		// building panel
		panel = new JPanel();
		panel.setFocusable(true);
		panel.setLayout(new GridLayout(1, editors.length));
		for (int i = 0; i < types.length; i++) {
			Component editorComponent = editors[i].getTableCellEditorComponent(null, values[i], false, 0, 0);

			if (editorComponent instanceof JComboBox) {
				if (((JComboBox<?>) editorComponent).isEditable()) {
					ComboBoxEditor editor = ((JComboBox<?>) editorComponent).getEditor();
					if (editor instanceof BasicComboBoxEditor) {
						editor.getEditorComponent().addFocusListener(focusListener);
					}
				} else {
					editorComponent.addFocusListener(focusListener);
				}
			} else if (editorComponent instanceof JPanel) {
				JPanel editorPanel = (JPanel) editorComponent;
				Component[] components = editorPanel.getComponents();
				for (Component comp : components) {
					comp.addFocusListener(focusListener);
				}
			} else {

				editorComponent.addFocusListener(focusListener);
			}
			panel.add(editorComponent);
			panel.addFocusListener(focusListener);
		}
	}

	@Override
	public Component getTableCellRendererComponent(JTable table, Object value, boolean isSelected, boolean hasFocus,
			int row, int column) {
		String[] tupel;
		if (value instanceof String) {
			tupel = ParameterTypeTupel.transformString2Tupel((String) value);
		} else {
			tupel = (String[]) value;
		}
		if (panel == null) {
			constructPanel(tupel);
		}
		for (int i = 0; i < editors.length; i++) {
			editors[i].getTableCellEditorComponent(null, tupel[i], false, 0, 0);
		}
		return panel;
	}

	/**
	 * Loops the sub-editors and calls stopCellEditing() on them. Returns false as soon as one
	 * editor; i.e. does not call the function on editors appearing further down in the list than
	 * the one returning false.
	 */
	@Override
	public boolean stopCellEditing() {
		for (int i = 0; i < editors.length; ++i) {
			if (!editors[i].stopCellEditing()) {
				return false;
			}
		}

		return super.stopCellEditing();
	}

	/**
	 * Cancels editing of all sub-editors
	 */
	@Override
	public void cancelCellEditing() {
		for (int i = 0; i < editors.length; ++i) {
			editors[i].cancelCellEditing();
		}

		super.cancelCellEditing();
	}
}
