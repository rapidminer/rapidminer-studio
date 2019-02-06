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
package com.rapidminer.gui.properties.celleditors.key;

import com.rapidminer.gui.properties.PropertyPanel;
import com.rapidminer.gui.properties.PropertyTable;
import com.rapidminer.gui.properties.celleditors.value.PropertyValueCellEditor;
import com.rapidminer.operator.Operator;
import com.rapidminer.parameter.ParameterTypeEnumeration;
import com.rapidminer.parameter.ParameterTypeList;

import java.awt.Component;
import java.util.EventObject;

import javax.swing.JTable;
import javax.swing.event.CellEditorListener;


/**
 * This is the key renderer for the ParameterTypeList to enable special parameter types as
 * attributes. Or regular expressions. Therefore it uses a delegation concept and tries to create
 * the appropriate editor for the given type.
 * 
 * @author Sebastian Land
 */
public class DelegationKeyCellEditor implements PropertyKeyCellEditor {

	private PropertyValueCellEditor delegationEditor;

	public DelegationKeyCellEditor(ParameterTypeList type) {
		delegationEditor = PropertyPanel.instantiateValueCellEditor(type.getKeyType(), null);
	}

	public DelegationKeyCellEditor(ParameterTypeEnumeration type) {
		delegationEditor = PropertyPanel.instantiateValueCellEditor(type.getValueType(), null);
	}

	@Override
	public void setOperator(Operator operator, PropertyTable propertyTable) {
		delegationEditor.setOperator(operator);
	}

	@Override
	public Component getTableCellEditorComponent(JTable table, Object value, boolean isSelected, int row, int column) {
		return delegationEditor.getTableCellEditorComponent(table, value, isSelected, row, column);
	}

	@Override
	public void addCellEditorListener(CellEditorListener l) {
		delegationEditor.addCellEditorListener(l);
	}

	@Override
	public void cancelCellEditing() {
		delegationEditor.cancelCellEditing();
	}

	@Override
	public Object getCellEditorValue() {
		return delegationEditor.getCellEditorValue();
	}

	@Override
	public boolean isCellEditable(EventObject anEvent) {
		return delegationEditor.isCellEditable(anEvent);
	}

	@Override
	public void removeCellEditorListener(CellEditorListener l) {
		delegationEditor.removeCellEditorListener(l);
	}

	@Override
	public boolean shouldSelectCell(EventObject anEvent) {
		return delegationEditor.shouldSelectCell(anEvent);
	}

	@Override
	public boolean stopCellEditing() {
		return delegationEditor.stopCellEditing();
	}

	@Override
	public Component getTableCellRendererComponent(JTable table, Object value, boolean isSelected, boolean hasFocus,
			int row, int column) {
		return delegationEditor.getTableCellRendererComponent(table, value, isSelected, hasFocus, row, column);
	}

}
