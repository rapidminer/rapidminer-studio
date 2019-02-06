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

import java.awt.Component;
import java.awt.Point;
import java.awt.event.KeyEvent;
import java.awt.event.MouseEvent;
import java.util.EventObject;
import java.util.LinkedList;
import java.util.List;
import javax.swing.JComponent;
import javax.swing.JTable;
import javax.swing.KeyStroke;
import javax.swing.event.ChangeEvent;
import javax.swing.table.TableCellEditor;
import javax.swing.table.TableCellRenderer;

import com.rapidminer.gui.tools.SwingTools;
import com.rapidminer.operator.Operator;
import com.rapidminer.parameter.ParameterType;
import com.rapidminer.parameter.ParameterTypeCategory;
import com.rapidminer.parameter.ParameterTypeEnumeration;
import com.rapidminer.parameter.ParameterTypeList;
import com.rapidminer.parameter.ParameterTypeStringCategory;


/**
 * Parameter table for list and enumeration types (GUI class).
 *
 * Note: This class uses a dedicated renderer/editor for each row. Actually, this is unnecessary.
 * The only reason is that if we re-use the editors/renderers, one has to click into the cell once
 * for starting editing and then again for opening a combo box, e.g. This may be related to
 * editingStopped-events which are fired by the cells that loose focus.
 *
 * @author Simon Fischer, Marius Helf
 *
 */
public class ListPropertyTable2 extends JTable {

	private static final long serialVersionUID = 1L;
	private List<TableCellRenderer[]> renderers = new LinkedList<>();
	private List<TableCellEditor[]> editors = new LinkedList<>();
	private ParameterType[] types;
	private Operator operator;

	public ListPropertyTable2(ParameterTypeList type, List<String[]> parameterList, Operator operator) {
		this(new ParameterType[] { type.getKeyType(), type.getValueType() }, parameterList, operator);
	}

	public ListPropertyTable2(ParameterTypeEnumeration type, List<String> parameterList, Operator operator) {
		this(new ParameterType[] { type.getValueType() }, to2DimList(parameterList), operator);
	}

	private List<String[]> createParameterListCopy(List<String[]> parameterList) {
		List<String[]> copiedParameterList = new LinkedList<>();
		for (String[] paramArray : parameterList) {
			String[] copiedParamArray = new String[paramArray.length];
			int i = 0;
			for (String string : paramArray) {
				copiedParamArray[i] = string;
				i++;
			}
			copiedParameterList.add(copiedParamArray);
		}
		return copiedParameterList;
	}

	private ListPropertyTable2(ParameterType[] types, List<String[]> parameterList, Operator operator) {
		this.types = types;
		this.operator = operator;
		setSelectionMode(javax.swing.ListSelectionModel.SINGLE_SELECTION);
		getTableHeader().setReorderingAllowed(false);
		setRowSelectionAllowed(true);
		setColumnSelectionAllowed(false);
		setRowHeight(PropertyPanel.VALUE_CELL_EDITOR_HEIGHT);
		setSurrendersFocusOnKeystroke(true);

		setModel(new ListTableModel(types, createParameterListCopy(parameterList)));

		fillEditors();

		requestFocusForLastEditableCell();
	}

	@Override
	protected boolean processKeyBinding(KeyStroke ks, KeyEvent e, int condition, boolean pressed) {
		if (e.getKeyCode() == KeyEvent.VK_TAB) {
			stopEditing();
		}
		return super.processKeyBinding(ks, e, condition, pressed);
	}

	private static List<String[]> to2DimList(List<String> parameterList) {
		List<String[]> result = new LinkedList<>();
		for (String v : parameterList) {
			result.add(new String[] { v });
		}
		return result;
	}

	public void addRow() {
		stopEditing();
		((ListTableModel) getModel()).addRow();
		fillEditors();

		// start editing the new row
		requestFocusForLastEditableCell();
	}

	public boolean isEmpty() {
		return renderers.isEmpty();
	}

	@Override
	public boolean editCellAt(int row, int column, EventObject e){
		stopEditing();
		return super.editCellAt(row,column,e);
	}

	protected void fillEditors() {
		while (editors.size() < getModel().getRowCount()) {
			TableCellRenderer rowRenderers[] = new TableCellRenderer[types.length];
			TableCellEditor rowEditors[] = new TableCellEditor[types.length];
			for (int i = 0; i < types.length; i++) {
				rowRenderers[i] = PropertyPanel.instantiateValueCellEditor(types[i], operator);
				rowEditors[i] = PropertyPanel.instantiateValueCellEditor(types[i], operator);
			}
			renderers.add(rowRenderers);
			editors.add(rowEditors);
		}
	}

	@Override
	public Component prepareEditor(TableCellEditor editor, int row, int column) {
		changeSelection(row, column, false, false);
		return super.prepareEditor(editor, row, column);
	}

	public boolean requestFocusForLastEditableCell() {
		boolean foundCell = false;
		for (int row = getRowCount() - 1; row >= 0; --row) {
			for (int column = 0; column < getColumnCount(); ++column) {
				if (!isCellEditable(row, column)) {
					continue;
				} else {
					changeSelection(row, column, false, false);
					foundCell = startCellEditingAndRequestFocus(row, column);
					break;
				}
			}
			if (foundCell) {
				break;
			}
		}
		return foundCell;
	}

	private boolean startCellEditingAndRequestFocus(int row, int column) {
		if (isCellEditable(row, column)) {
			editCellAt(row, column);
			Component editorComponent = getEditorComponent();
			if (editorComponent != null) {
				if (editorComponent instanceof JComponent) {
					JComponent jComponent = (JComponent) editorComponent;
					if (!jComponent.hasFocus()) {
						jComponent.requestFocusInWindow();
					}
					return true;
				}
			}
		}
		return false;
	}

	public void removeSelected() {
		if (getSelectedRow() != -1) {
			stopEditing();
			((ListTableModel) getModel()).removeRow(getSelectedRow());
			requestFocusForLastEditableCell();
		}
	}

	public void storeParameterList(List<String[]> parameterList2) {
		parameterList2.clear();
		parameterList2.addAll(((ListTableModel) getModel()).getParameterList());
	}

	public void stopEditing() {
		TableCellEditor editor = getCellEditor();
		if (editor != null) {
			boolean stoppedCellEditing = editor.stopCellEditing();
			//remove the editor if stopping was successful
			if (stoppedCellEditing) {
				removeEditor();
			}
		}
	}

	public void storeParameterEnumeration(List<String> parameterList2) {

		parameterList2.clear();
		for (String[] values : ((ListTableModel) getModel()).getParameterList()) {
			parameterList2.add(values[0]);
		}
	}

	@Override
	public TableCellRenderer getCellRenderer(int row, int column) {
		return renderers.get(row)[column];
	}

	@Override
	public TableCellEditor getCellEditor(int row, int column) {
		return editors.get(row)[column];
	}

	/** This method ensures that the correct tool tip for the current table cell is delivered. */
	@Override
	public String getToolTipText(MouseEvent e) {
		Point p = e.getPoint();
		int column = columnAtPoint(p);
		ParameterType type = types[column];
		StringBuffer toolTip = new StringBuffer(type.getDescription());
		if (!(type instanceof ParameterTypeCategory) && !(type instanceof ParameterTypeStringCategory)) {
			String range = type.getRange();
			if (range != null && range.trim().length() > 0) {
				toolTip.append(" (");
				toolTip.append(type.getRange());
				toolTip.append(")");
			}
		}
		String toolTipText = SwingTools.transformToolTipText(toolTip.toString());
		return toolTipText;
	}

	/**
	 * This is needed in order to allow auto completion: Otherwise the editor will be immediately
	 * removed after setting the first selected value and loosing its focus. This way it is ensured
	 * that the editor won't be removed.
	 */
	@Override
	public void editingStopped(ChangeEvent e) {

		TableCellEditor editor = getCellEditor();
		if (editor != null) {
			Object value = editor.getCellEditorValue();
			setValueAt(value, editingRow, editingColumn);
		}
	}

	/*
	 * DO NOT ENABLE THE LIST SELECTION CHANGE METHODS! If you do so and request the focus when
	 * changing selection this leads to strange side effects (like losing focus directly after
	 * clicking an a table cell).
	 */
	// /**
	// * Row selection change listener
	// */
	// @Override
	// public void valueChanged(ListSelectionEvent e) {
	// super.valueChanged(e);
	// int col = getSelectedColumn();
	// int row = getSelectedRow();
	// if (col >= 0 && row >= 0) {
	// startCellEditingAndRequestFocus(row, col);
	// }
	// }
	//
	// /**
	// * Column selection change listener
	// */
	// @Override
	// public void columnSelectionChanged(ListSelectionEvent e) {
	// super.columnSelectionChanged(e);
	// int col = getSelectedColumn();
	// int row = getSelectedRow();
	// if (col >= 0 && row >= 0) {
	// startCellEditingAndRequestFocus(row, col);
	// }
	// }

}
