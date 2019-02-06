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
import com.rapidminer.parameter.ParameterTypeList;
import com.rapidminer.parameter.ParameterTypeStringCategory;

import java.awt.Point;
import java.awt.event.MouseEvent;
import java.util.Iterator;
import java.util.List;

import javax.swing.event.ChangeEvent;


/**
 * For {@link com.rapidminer.parameter.ParameterTypeList} the parameter values are parameter lists
 * themselves. Hence, the key must be editable, too (not only the value). That is what this
 * implementation of PropertyTable is good for.
 * 
 * @author Simon Fischer, Ingo Mierswa
 */
public class ListPropertyTable extends PropertyTable {

	private static final long serialVersionUID = -4547732551646588939L;

	private transient ParameterTypeList type;

	private transient Operator operator;

	private transient String keyToolTip;

	public ListPropertyTable(ParameterTypeList type, List<String[]> parameterList, Operator operator) {
		super(new String[] { type.getKeyType().getKey().replaceAll("_", " "),
				type.getValueType().getKey().replaceAll("_", " ") });
		this.type = type;
		this.operator = operator;
		updateTableData(parameterList.size());
		updateEditorsAndRenderers();
		Iterator<String[]> i = parameterList.iterator();
		int j = 0;
		while (i.hasNext()) {
			String[] keyValue = i.next();
			getModel().setValueAt(keyValue[0], j, 0);
			getModel().setValueAt(keyValue[1], j, 1);
			j++;
		}

		// generating toolTip for keys
		ParameterType keyType = type.getKeyType();
		StringBuffer toolTip = new StringBuffer(keyType.getDescription());
		if ((!(keyType instanceof ParameterTypeCategory)) && (!(keyType instanceof ParameterTypeStringCategory))) {
			String range = keyType.getRange();
			if ((range != null) && (range.trim().length() > 0)) {
				toolTip.append(" (");
				toolTip.append(keyType.getRange());
				toolTip.append(")");
			}
		}
		keyToolTip = SwingTools.transformToolTipText(toolTip.toString());
	}

	public void addRow() {
		getDefaultModel().addRow(new Object[] { "", type.getValueType().getDefaultValue() });
		updateEditorsAndRenderers();

		// necessary to use default values (without changes)
		int lastIndex = getRowCount() - 1;
		// getModel().setValueAt(type.getKeyType().getDefaultValue(), lastIndex, 0);
		getModel().setValueAt(getKeyEditor(lastIndex).getCellEditorValue(), lastIndex, 0);
	}

	public void removeSelected() {
		int[] selectedRow = getSelectedRows();
		for (int i = selectedRow.length - 1; i >= 0; i--) {
			getDefaultModel().removeRow(selectedRow[i]);
		}
		getDefaultModel().fireTableStructureChanged();
	}

	public void storeParameterList(List<String[]> list) {
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
		// returning list for creating a delegation key cell editor which delegates to the
		// appropriate key value cell editor of the keyType.
		return type;
	}

	@Override
	public boolean isCellEditable(int row, int col) {
		return true;
	}

	/**
	 * This is needed in order to allow auto completition: Otherwise the editor will be immediately
	 * removed after setting the first selected value and loosing its focus. This way it is ensured
	 * that the editor won't be removed.
	 */
	@Override
	public void editingCanceled(ChangeEvent e) {
		getModel().setValueAt(getCellEditor().getCellEditorValue(), getEditingRow(), getEditingColumn());
	}

	/**
	 * This is needed in order to allow auto completition: Otherwise the editor will be immediately
	 * removed after setting the first selected value and loosing its focus. This way it is ensured
	 * that the editor won't be removed.
	 */
	@Override
	public void editingStopped(ChangeEvent e) {
		getModel().setValueAt(getCellEditor().getCellEditorValue(), getEditingRow(), getEditingColumn());
	}

}
