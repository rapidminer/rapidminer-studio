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

import com.rapidminer.gui.tools.ExtendedJTable;
import com.rapidminer.tools.att.AttributeDataSource;

import java.util.List;

import javax.swing.table.AbstractTableModel;
import javax.swing.table.TableColumn;
import javax.swing.table.TableColumnModel;


/**
 * This class display a small data view corresponding on the current wizard settings.
 * 
 * @author Ingo Mierswa
 */
public class ExampleSourceConfigurationWizardDataTable extends ExtendedJTable {

	private static final long serialVersionUID = -6334023466810899931L;

	private static class ExampleSourceConfigurationWizardDataTableModel extends AbstractTableModel {

		private static final long serialVersionUID = 8548500131446968338L;

		private List<AttributeDataSource> sources;

		private List<String[]> data;

		public ExampleSourceConfigurationWizardDataTableModel(List<AttributeDataSource> sources, List<String[]> data) {
			this.sources = sources;
			this.data = data;
		}

		@Override
		public int getColumnCount() {
			return this.sources.size();
		}

		@Override
		public int getRowCount() {
			return data.size();
		}

		@Override
		public Object getValueAt(int rowIndex, int columnIndex) {
			String[] row = data.get(rowIndex);
			if (columnIndex >= row.length) {
				return "?";
			} else {
				return row[columnIndex];
			}
		}

		@Override
		public String getColumnName(int column) {
			return sources.get(column).getAttribute().getName();
		}
	}

	public ExampleSourceConfigurationWizardDataTable(List<AttributeDataSource> sources, List<String[]> data) {
		super();
		setAutoResizeMode(AUTO_RESIZE_OFF);
		setModel(new ExampleSourceConfigurationWizardDataTableModel(sources, data));
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
}
