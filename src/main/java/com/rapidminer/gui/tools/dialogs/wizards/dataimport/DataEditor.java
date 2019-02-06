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
package com.rapidminer.gui.tools.dialogs.wizards.dataimport;

import com.rapidminer.gui.tools.ExtendedJTable;
import com.rapidminer.operator.ports.metadata.AttributeMetaData;
import com.rapidminer.operator.ports.metadata.ExampleSetMetaData;
import com.rapidminer.tools.Ontology;

import java.awt.Component;
import java.util.List;

import javax.swing.ComboBoxModel;
import javax.swing.DefaultCellEditor;
import javax.swing.DefaultComboBoxModel;
import javax.swing.JComboBox;
import javax.swing.JTable;
import javax.swing.table.AbstractTableModel;
import javax.swing.table.DefaultTableCellRenderer;
import javax.swing.table.TableCellEditor;


/**
 * @author Tobias Malbrecht
 */
public class DataEditor extends ExtendedJTable {

	private static final long serialVersionUID = -520323914589387512L;

	private static final int[][] VALUE_TYPE_OPTIONS = { {}, { Ontology.NOMINAL }, { Ontology.NUMERICAL },
			{ Ontology.NUMERICAL, Ontology.INTEGER }, { Ontology.NUMERICAL, Ontology.REAL },
			{ Ontology.NOMINAL, Ontology.STRING }, { Ontology.NOMINAL, Ontology.BINOMINAL },
			{ Ontology.NOMINAL, Ontology.POLYNOMINAL }, { Ontology.NOMINAL, Ontology.FILE_PATH }, { Ontology.DATE_TIME },
			{ Ontology.DATE_TIME, Ontology.DATE }, { Ontology.DATE_TIME, Ontology.TIME }, };

	private class ValueTypeCellEditor extends DefaultCellEditor {

		private static final long serialVersionUID = 7954919612214223430L;

		@SuppressWarnings("unchecked")
		public ValueTypeCellEditor(final int valueType) {
			super(new JComboBox<String>());
			ComboBoxModel<String> model = new DefaultComboBoxModel<String>() {

				private static final long serialVersionUID = 914764579359633239L;

				private String[] valueTypes = new String[VALUE_TYPE_OPTIONS[valueType].length];
				{
					for (int i = 0; i < VALUE_TYPE_OPTIONS[valueType].length; i++) {
						valueTypes[i] = Ontology.ATTRIBUTE_VALUE_TYPE.mapIndex(VALUE_TYPE_OPTIONS[valueType][i]);
					}
				}

				@Override
				public String getElementAt(int index) {
					return valueTypes[index];
				}

				@Override
				public int getSize() {
					return valueTypes.length;
				}
			};
			((JComboBox<?>) super.getComponent()).setEnabled(editValueTypes);
			((JComboBox<String>) super.getComponent()).setModel(model);
		}
	}

	private class DataEditorCellRenderer extends DefaultTableCellRenderer {

		private static final long serialVersionUID = -4373454555123741476L;

		@Override
		public Component getTableCellRendererComponent(JTable table, Object value, boolean isSelected, boolean hasFocus,
				int row, int column) {
			if (row == 0 && showValueTypes) {
				return valueTypeCellEditors[column].getTableCellEditorComponent(table, value, isSelected, row, column);
			} else {
				return super.getTableCellRendererComponent(table, value, isSelected, hasFocus, row, column);
			}
		}

	}

	private class DataModel extends AbstractTableModel {

		private static final long serialVersionUID = -8096935282615030186L;

		private AttributeMetaData[] attributes = null;

		private List<Object[]> data = null;

		private void setData(ExampleSetMetaData metaData, List<Object[]> data) {
			attributes = new AttributeMetaData[metaData.getAllAttributes().size()];
			int i = 0;
			for (AttributeMetaData amd : metaData.getAllAttributes()) {
				attributes[i++] = amd;
			}
			this.data = data;
			this.fireTableStructureChanged();
		}

		@Override
		public int getColumnCount() {
			return attributes != null ? attributes.length : 0;
		}

		@Override
		public String getColumnName(int columnIndex) {
			return attributes[columnIndex].getName();
		}

		@Override
		public Class<?> getColumnClass(int columnIndex) {
			return String.class;
		}

		private int getRowOffset() {
			int offset = 0;
			offset += showValueTypes ? 1 : 0;
			return offset;
		}

		@Override
		public int getRowCount() {
			return data != null ? (data.size() + getRowOffset()) : 0;
		}

		@Override
		public Object getValueAt(int row, int column) {
			if (row == 0 && showValueTypes) {
				return Ontology.VALUE_TYPE_NAMES[attributes[column].getValueType()];
			}
			if (row >= getRowOffset()) {
				Object[] values = data.get(row - getRowOffset());
				if (column >= values.length) {
					return "";
				}
				if (values[column] == null) {
					return "";
				}
				// if (showValueTypes) {
				// if (Ontology.ATTRIBUTE_VALUE_TYPE.isA(attributes[column].getValueType(),
				// Ontology.INTEGER)) {
				// return Tools.formatIntegerIfPossible(((Number) values[column]).intValue());
				// }
				// if (Ontology.ATTRIBUTE_VALUE_TYPE.isA(attributes[column].getValueType(),
				// Ontology.REAL)) {
				// return Tools.formatNumber(((Number) values[column]).doubleValue());
				// }
				// if (Ontology.ATTRIBUTE_VALUE_TYPE.isA(attributes[column].getValueType(),
				// Ontology.NUMERICAL)) {
				// return Tools.formatNumber(((Number) values[column]).doubleValue());
				// }
				// if (Ontology.ATTRIBUTE_VALUE_TYPE.isA(attributes[column].getValueType(),
				// Ontology.DATE)) {
				// return Tools.formatDate((Date) values[column]);
				// }
				// if (Ontology.ATTRIBUTE_VALUE_TYPE.isA(attributes[column].getValueType(),
				// Ontology.TIME)) {
				// return Tools.formatTime((Date) values[column]);
				// }
				// if (Ontology.ATTRIBUTE_VALUE_TYPE.isA(attributes[column].getValueType(),
				// Ontology.DATE_TIME)) {
				// return Tools.formatDateTime((Date) values[column]);
				// }
				return values[column].toString();
				// }
				// return values[columnIndex].toString();
			}
			return null;
		}

		@Override
		public void setValueAt(Object value, int row, int column) {
			if (row == 0 && editValueTypes) {
				attributes[column].setType(Ontology.ATTRIBUTE_VALUE_TYPE.mapName(value.toString()));
				repaint();
			}
		}
	}

	private boolean showValueTypes = false;

	private boolean editValueTypes = false;

	private final DataModel model = new DataModel();

	private final DataEditorCellRenderer cellRenderer = new DataEditorCellRenderer();

	private ValueTypeCellEditor[] valueTypeCellEditors = null;

	public DataEditor() {
		super(null, false, false, false, false, false);
		setModel(model);
		setDefaultRenderer(String.class, cellRenderer);
		setColumnSelectionAllowed(false);
		setRowSelectionAllowed(false);
		setCellSelectionEnabled(false);
	}

	public DataEditor(boolean showValueTypes) {
		this();
		this.showValueTypes = showValueTypes;
	}

	public DataEditor(boolean showValueTypes, boolean editValueTypes) {
		this();
		this.showValueTypes = true;
		this.editValueTypes = editValueTypes;
	}

	public void setData(ExampleSetMetaData metaData, List<Object[]> data) {
		valueTypeCellEditors = new ValueTypeCellEditor[metaData.getAllAttributes().size()];
		int i = 0;
		for (AttributeMetaData amd : metaData.getAllAttributes()) {
			valueTypeCellEditors[i++] = new ValueTypeCellEditor(amd.getValueType());
		}
		model.setData(metaData, data);
	}

	@Override
	public TableCellEditor getCellEditor(int row, int column) {
		if (row == 0 && showValueTypes) {
			return valueTypeCellEditors[column];
		} else {
			return super.getCellEditor(row, column);
		}
	}

	@Override
	public boolean isCellEditable(int row, int column) {
		if (row == 0 && editValueTypes) {
			return true;
		}
		return false;
	}
}
