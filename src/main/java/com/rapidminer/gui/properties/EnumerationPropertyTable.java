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
package com.rapidminer.gui.properties;

import com.rapidminer.gui.tools.SwingTools;
import com.rapidminer.operator.Operator;
import com.rapidminer.parameter.ParameterType;
import com.rapidminer.parameter.ParameterTypeCategory;
import com.rapidminer.parameter.ParameterTypeEnumeration;
import com.rapidminer.parameter.ParameterTypeStringCategory;

import java.awt.Point;
import java.awt.event.MouseEvent;
import java.util.List;

import javax.swing.table.DefaultTableModel;


/**
 * For {@link com.rapidminer.parameter.ParameterTypeList} the parameter values are parameter lists
 * themselves. Hence, the key must be editable, too (not only the value). That is what this
 * implementation of PropertyTable is good for.
 * 
 * @author Simon Fischer, Ingo Mierswa
 */
public class EnumerationPropertyTable extends PropertyTable {

	private static final long serialVersionUID = -4547732551646588939L;

	private transient ParameterTypeEnumeration type;

	private transient Operator operator;

	private transient String keyToolTip;

	public EnumerationPropertyTable(ParameterTypeEnumeration type, List<String> valueList, Operator operator) {
		super(new String[] { type.getValueType().getKey(), type.getValueType().getKey() });
		this.type = type;
		this.operator = operator;
		updateTableData(valueList.size());
		updateEditorsAndRenderers();
		int j = 0;
		for (String value : valueList) {
			getModel().setValueAt(value, j, 0);
			j++;
		}

		// generating toolTip for keys
		ParameterType valueType = type.getValueType();
		StringBuffer toolTip = new StringBuffer(valueType.getDescription());
		if ((!(valueType instanceof ParameterTypeCategory)) && (!(valueType instanceof ParameterTypeStringCategory))) {
			String range = valueType.getRange();
			if ((range != null) && (range.trim().length() > 0)) {
				toolTip.append(" (");
				toolTip.append(valueType.getRange());
				toolTip.append(")");
			}
		}
		keyToolTip = SwingTools.transformToolTipText(toolTip.toString());
	}

	public void addRow() {
		if (type.getValueType().getDefaultValue() != null) {
			getDefaultModel().addRow(new Object[] { type.getValueType().getDefaultValue() });
		} else {
			getDefaultModel().addRow(new Object[] { "" });
		}
		updateEditorsAndRenderers();

		// necessary to use default values (without changes)
		int lastIndex = getRowCount() - 1;
		// final Object value = getKeyEditor(lastIndex).getCellEditorValue();
		Object value = type.getValueType().getDefaultValue();
		getModel().setValueAt(value, lastIndex, 0);
	}

	public void removeSelected() {
		int[] selectedRow = getSelectedRows();
		for (int i = selectedRow.length - 1; i >= 0; i--) {
			getDefaultModel().removeRow(selectedRow[i]);
		}
		getDefaultModel().fireTableStructureChanged();
	}

	@Override
	protected void updateTableData(int rows) {
		DefaultTableModel model = new DefaultTableModel(new String[] { type.getKey() }, rows);
		setModel(model);
	}

	public void getParameterList(List<String[]> list) {
		list.clear();
		for (int i = 0; i < getModel().getRowCount(); i++) {
			String firstString = null;
			Object firstObject = getModel().getValueAt(i, 0);
			if (firstObject instanceof String) {
				firstString = (String) firstObject;
			} else if (firstObject != null) {
				firstString = firstObject.toString();
			}

			String secondString = null;
			Object secondObject = getModel().getValueAt(i, 1);
			if (secondObject instanceof String) {
				secondString = (String) secondObject;
			} else if (secondObject != null) {
				secondString = secondObject.toString();
			}

			list.add(new String[] { firstString, secondString });
		}
	}

	/** This method ensures that the correct tool tip for the current table cell is delivered. */
	@Override
	public String getToolTipText(MouseEvent e) {
		Point p = e.getPoint();
		int column = columnAtPoint(p);
		if (column == 0) {
			return keyToolTip;
		} else {
			return super.getToolTipText(e);
		}
	}

	@Override
	public Operator getOperator(int row) {
		return operator;
	}

	@Override
	public ParameterType getParameterType(int row) {
		return type.getValueType();
	}

	@Override
	public ParameterType getKeyParameterType(int row) {
		return type;
	}

	@Override
	public boolean isCellEditable(int row, int col) {
		return true;
	}

	public void getParameterEnumeration(List<String> list) {
		list.clear();
		for (int i = 0; i < getModel().getRowCount(); i++) {
			Object firstObject = getModel().getValueAt(i, 0);
			if (firstObject instanceof String) {
				list.add((String) firstObject);
			} else if (firstObject != null) {
				list.add(firstObject.toString());
			}
		}
	}
}
