/**
 * Copyright (C) 2001-2015 by RapidMiner and the contributors
 *
 * Complete list of developers available at our web site:
 *
 *      http://rapidminer.com
 *
 * This program is free software: you can redistribute it and/or modify
 * it under the terms of the GNU Affero General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU Affero General Public License for more details.
 *
 * You should have received a copy of the GNU Affero General Public License
 * along with this program.  If not, see http://www.gnu.org/licenses/.
 */
package com.rapidminer.template.gui;

import com.rapidminer.example.ExampleSet;

import java.awt.Color;
import java.awt.Component;
import java.util.Enumeration;

import javax.swing.JTable;
import javax.swing.table.DefaultTableModel;
import javax.swing.table.TableCellRenderer;
import javax.swing.table.TableColumn;
import javax.swing.table.TableModel;


/**
 * Fancy table for example sets. Uses a {@link ExampleSetTableModel} as well as a
 * {@link DataCellRenderer}.
 * 
 * @author Simon Fischer
 * 
 */
public class ExampleSetTable extends JTable {

	private static final long serialVersionUID = 1L;

	private boolean limited;

	private DataCellRenderer cellRenderer = new DataCellRenderer(null, false);
	private DataCellRenderer headerCellRenderer = new DataCellRenderer(null, true);

	public ExampleSetTable(boolean limited) {
		this.limited = limited;
		setDefaultRenderer(Object.class, cellRenderer);
		setDefaultRenderer(Double.class, cellRenderer);
		setDefaultRenderer(double.class, cellRenderer);
		setShowGrid(false);
		setShowHorizontalLines(false);
		setShowVerticalLines(false);
		setRowMargin(0);
		setRowHeight(30);
		getColumnModel().setColumnMargin(0);
		getTableHeader().setDefaultRenderer(headerCellRenderer);
		setAutoResizeMode(JTable.AUTO_RESIZE_OFF);
		getTableHeader().setBackground(Color.WHITE);
		setBorder(null);
		setBackground(Color.WHITE);
	}

	public ExampleSetTable(ExampleSet exampleSet, boolean limited) {
		this(limited);
		setExampleSet(exampleSet);
	}

	public void setExampleSet(ExampleSet exampleSet) {
		if (exampleSet != null) {
			ExampleSetTableModel dataModel;
			TableModel oldModel = getModel();
			if ((oldModel == null) || !(oldModel instanceof ExampleSetTableModel)) {
				dataModel = new ExampleSetTableModel(exampleSet, limited);
			} else {
				dataModel = (ExampleSetTableModel) oldModel;
				dataModel.setExampleSet(exampleSet);
			}
			setModel(dataModel);
			cellRenderer.setModel(dataModel);
			headerCellRenderer.setModel(dataModel);
			Enumeration<TableColumn> columns = getColumnModel().getColumns();
			TableCellRenderer rend = getTableHeader().getDefaultRenderer();
			int col = 0;
			while (columns.hasMoreElements()) {
				TableColumn tc = columns.nextElement();
				TableCellRenderer rendCol = tc.getHeaderRenderer(); // likely null
				if (rendCol == null) {
					rendCol = rend;
				}
				int max = 20;
				Component c = rendCol.getTableCellRendererComponent(this, tc.getHeaderValue(), false, false, 0, col);
				max = Math.max(max, c.getPreferredSize().width);
				for (int row = 0; row < Math.min(10, dataModel.getRowCount()); row++) {
					c = rend.getTableCellRendererComponent(this, dataModel.getValueAt(row, col), false, false, row, col);
					max = Math.max(max, c.getPreferredSize().width);
				}
				tc.setPreferredWidth(max);
				col++;
			}
		} else {
			setModel(new DefaultTableModel());
		}
	}
}
