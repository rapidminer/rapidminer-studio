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
package com.rapidminer.gui.tools;

import javax.swing.JScrollPane;
import javax.swing.JViewport;
import javax.swing.ListSelectionModel;
import javax.swing.ScrollPaneConstants;
import javax.swing.table.DefaultTableColumnModel;
import javax.swing.table.JTableHeader;
import javax.swing.table.TableColumn;
import javax.swing.table.TableColumnModel;
import javax.swing.table.TableModel;


/**
 * This table can be used to display a fixed column as the first column which will never be moved or
 * scrolled away. This can be nice if the first column contains names / ids for the rows which
 * should be displayed independently of the scrolling of the rest of the table.
 * 
 * @author Ingo Mierswa
 */
public class ExtendedFixedColumnJTable extends JScrollPane {

	private static final long serialVersionUID = -5141975629639275878L;

	private ExtendedJTable mainDataTable;

	private ExtendedFixedColumnJTable(ExtendedJTable mainDataTable, JViewport viewport, JTableHeader header) {
		// we have to manually attach the row headers, but after that, the scroll pane keeps them in
		// sync
		super(mainDataTable);
		this.mainDataTable = mainDataTable;
		setRowHeader(viewport);
		setCorner(ScrollPaneConstants.UPPER_LEFT_CORNER, header);
		mainDataTable.setShowPopupMenu(false);
	}

	public static ExtendedFixedColumnJTable createFixedColumnTable(TableModel model) {
		// determine and set preferred size from data, use maximum size anyway
		int max = 0;
		for (int r = 0; r < model.getRowCount(); r++) {
			String valueString = model.getValueAt(r, 0).toString();
			max = Math.max(valueString.length() * 8, max);
		}

		final int preferredWidth = Math.min(max, 250);

		// create a column model for the main table. This model ignores the first column added
		TableColumnModel cm = new DefaultTableColumnModel() {

			private static final long serialVersionUID = -4882307040329823339L;

			private boolean first = true;

			@Override
			public void addColumn(TableColumn tc) {
				if (first) {
					first = false;
				} else {
					super.addColumn(tc);
				}
			}
		};

		// create a column model that will serve as our row header table. This model only stores the
		// first column.
		TableColumnModel rowHeaderModel = new DefaultTableColumnModel() {

			private static final long serialVersionUID = -4852540063984136543L;

			private boolean first = true;

			@Override
			public void addColumn(TableColumn tc) {
				if (first) {
					super.addColumn(tc);
					tc.setPreferredWidth(preferredWidth);
					first = false;
				}
			}
		};

		// shut off autoResizeMode, our tables won't scroll correctly (horizontally, anyway)!
		ExtendedJTable mainDataTable = new ExtendedJTable(model, false, true, false, true, false);
		mainDataTable.setColumnModel(cm);

		// header column
		ExtendedJTable headerColumn = new ExtendedJTable(model, false, true, false, true, false);
		headerColumn.setCellColorProvider(new CellColorProviderYellow());
		headerColumn.setColumnModel(rowHeaderModel);
		mainDataTable.createDefaultColumnsFromModel();
		headerColumn.createDefaultColumnsFromModel();

		headerColumn.getTableHeader().setReorderingAllowed(false);
		headerColumn.getTableHeader().setResizingAllowed(false);

		// make sure that selections between the main table and the header stay in sync (by sharing
		// the same model)
		mainDataTable.getSelectionModel().setSelectionMode(ListSelectionModel.SINGLE_SELECTION);
		mainDataTable.setSelectionModel(headerColumn.getSelectionModel());

		// adjust preferred size
		headerColumn.setPreferredScrollableViewportSize(headerColumn.getPreferredSize());

		// put it in a viewport that we can control
		JViewport viewport = new JViewport();
		viewport.setView(headerColumn);
		viewport.setPreferredSize(headerColumn.getPreferredSize());

		return new ExtendedFixedColumnJTable(mainDataTable, viewport, headerColumn.getTableHeader());
	}

	public ExtendedJTable getMainDataTable() {
		return this.mainDataTable;
	}
}
