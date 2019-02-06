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

import java.awt.Point;
import java.awt.Rectangle;

import javax.swing.JViewport;
import javax.swing.table.TableColumn;
import javax.swing.table.TableColumnModel;


/**
 * This is a packer class for the extended JTable.
 * 
 * @author Santhosh Kumar, Ingo Mierswa
 */
public class ExtendedJTablePacker {

	private boolean distributeExtraArea = true;

	public ExtendedJTablePacker(boolean distributeExtraArea) {
		this.distributeExtraArea = distributeExtraArea;
	}

	private int preferredWidth(ExtendedJTable table, int col) {
		TableColumn tableColumn = table.getColumnModel().getColumn(col);
		int width = (int) table.getTableHeader().getDefaultRenderer()
				.getTableCellRendererComponent(table, tableColumn.getIdentifier(), false, false, -1, col).getPreferredSize()
				.getWidth();

		if (table.getRowCount() != 0) {
			int from = 0;
			int to = table.getRowCount();
			ExtendedJScrollPane scrollPane = (table).getExtendedScrollPane();
			if (scrollPane != null) {
				JViewport viewport = scrollPane.getViewport();
				Rectangle viewRect = viewport.getViewRect();
				from = table.rowAtPoint(new Point(0, viewRect.y));
				from = Math.max(0, from);
				to = table.rowAtPoint(new Point(0, viewRect.y + viewRect.height - 2));
				to = Math.min(to, table.getRowCount());
			}

			for (int row = from; row < to; row++) {
				int preferedWidth = (int) table.getCellRenderer(row, col)
						.getTableCellRendererComponent(table, table.getValueAt(row, col), false, false, row, col)
						.getPreferredSize().getWidth();
				width = Math.max(width, preferedWidth);
			}
		}

		return width + table.getIntercellSpacing().width;
	}

	public void pack(ExtendedJTable table) {
		if (!table.isShowing()) {
			// throw new IllegalStateException("Table must be showed in order to pack it.");
			// return silently;
			return;
		}

		if (table.getTableHeader() == null) {
			return;
		}

		if (table.getColumnCount() == 0) {
			return;
		}

		int width[] = new int[table.getColumnCount()];
		int total = 0;
		for (int col = 0; col < width.length; col++) {
			width[col] = preferredWidth(table, col);
			total += width[col];
		}

		int extra = table.getVisibleRect().width - total;
		if (extra > 0) {
			if (distributeExtraArea) {
				int bonus = extra / table.getColumnCount();
				for (int i = 0; i < width.length; i++) {
					width[i] += bonus;
				}
				extra -= bonus * table.getColumnCount();
			}
			width[width.length - 1] += extra;
		}

		TableColumnModel columnModel = table.getColumnModel();
		for (int col = width.length - 1; col >= 0; col--) {
			TableColumn tableColumn = columnModel.getColumn(col);
			table.getTableHeader().setResizingColumn(tableColumn);
			tableColumn.setWidth(width[col]);
		}
	}
}
