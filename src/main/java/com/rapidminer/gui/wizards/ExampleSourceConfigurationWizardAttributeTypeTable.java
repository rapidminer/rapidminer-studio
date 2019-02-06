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
package com.rapidminer.gui.wizards;

import com.rapidminer.example.Attributes;
import com.rapidminer.gui.EditorCellRenderer;
import com.rapidminer.gui.tools.ExtendedJTable;
import com.rapidminer.gui.tools.SwingTools;
import com.rapidminer.tools.att.AttributeDataSource;

import java.util.List;

import javax.swing.DefaultCellEditor;
import javax.swing.JComboBox;
import javax.swing.table.AbstractTableModel;
import javax.swing.table.TableCellEditor;
import javax.swing.table.TableCellRenderer;
import javax.swing.table.TableColumn;
import javax.swing.table.TableColumnModel;


/**
 * This table shows only the attribute names and the attribute types (regular or special).
 * 
 * @author Ingo Mierswa
 */
public class ExampleSourceConfigurationWizardAttributeTypeTable extends ExtendedJTable {

	private static final long serialVersionUID = -2517765684242352099L;

	private static class ExampleSourceConfigurationWizardAttributeTypeTableModel extends AbstractTableModel {

		private static final long serialVersionUID = -6609321134250471524L;

		private List<AttributeDataSource> sources;

		public ExampleSourceConfigurationWizardAttributeTypeTableModel(List<AttributeDataSource> sources) {
			this.sources = sources;
		}

		@Override
		public int getColumnCount() {
			return sources.size();
		}

		@Override
		public int getRowCount() {
			return 1;
		}

		@Override
		public void setValueAt(Object value, int rowIndex, int columnIndex) {
			String type = (String) value;
			sources.get(columnIndex).setType(type);
		}

		@Override
		public Object getValueAt(int rowIndex, int columnIndex) {
			return sources.get(columnIndex).getType();
		}

		@Override
		public String getColumnName(int column) {
			return sources.get(column).getAttribute().getName();
		}
	}

	public ExampleSourceConfigurationWizardAttributeTypeTable(List<AttributeDataSource> sources) {
		super(false);
		setAutoResizeMode(AUTO_RESIZE_OFF);
		setModel(new ExampleSourceConfigurationWizardAttributeTypeTableModel(sources));
		setRowHeight(getRowHeight() + SwingTools.TABLE_WITH_COMPONENTS_ROW_EXTRA_HEIGHT);
		update();
	}

	public void update() {
		((AbstractTableModel) getModel()).fireTableStructureChanged();
		TableColumnModel columnModel = getColumnModel();
		for (int i = 0; i < columnModel.getColumnCount(); i++) {
			TableColumn tableColumn = columnModel.getColumn(i);
			tableColumn.setPreferredWidth(120);
		}
	}

	@Override
	public boolean isCellEditable(int row, int col) {
		return true;
	}

	@Override
	public TableCellEditor getCellEditor(int row, int column) {
		JComboBox<String> typeBox = new JComboBox<>(Attributes.KNOWN_ATTRIBUTE_TYPES);
		typeBox.setEditable(true);
		typeBox.setBackground(javax.swing.UIManager.getColor("Table.cellBackground"));
		return new DefaultCellEditor(typeBox);
	}

	@Override
	public TableCellRenderer getCellRenderer(int row, int column) {
		return new EditorCellRenderer(getCellEditor(row, column));
	}
}
