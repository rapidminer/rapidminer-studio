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
import java.awt.event.InputEvent;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;

import javax.swing.JTable;
import javax.swing.JViewport;
import javax.swing.table.JTableHeader;
import javax.swing.table.TableColumn;


/**
 * This is a the listener for double clicks on column separators in order to pack the left column.
 * 
 * @author Santhosh Kumar, Ingo Mierswa
 */
public class ExtendedJTableColumnFitMouseListener extends MouseAdapter {

	@Override
	public void mouseClicked(MouseEvent e) {
		if (e.getClickCount() == 2) {
			JTableHeader header = (JTableHeader) e.getSource();
			TableColumn tableColumn = getResizingColumn(header, e.getPoint());

			if (tableColumn == null) {
				return;
			}

			JTable table = header.getTable();

			if ((e.getModifiers() & InputEvent.CTRL_MASK) == InputEvent.CTRL_MASK) {
				if (table instanceof ExtendedJTable) {
					((ExtendedJTable) table).pack();
					e.consume();
				}
			} else {
				int col = header.getColumnModel().getColumnIndex(tableColumn.getIdentifier());
				int width = (int) header.getDefaultRenderer()
						.getTableCellRendererComponent(table, tableColumn.getIdentifier(), false, false, -1, col)
						.getPreferredSize().getWidth();

				int firstRow = 0;
				int lastRow = table.getRowCount();

				if (table instanceof ExtendedJTable) {
					ExtendedJScrollPane scrollPane = ((ExtendedJTable) table).getExtendedScrollPane();
					if (scrollPane != null) {
						JViewport viewport = scrollPane.getViewport();
						Rectangle viewRect = viewport.getViewRect();
						if (viewport.getHeight() < table.getHeight()) {
							firstRow = table.rowAtPoint(new Point(0, viewRect.y));
							firstRow = Math.max(0, firstRow);
							lastRow = table.rowAtPoint(new Point(0, viewRect.y + viewRect.height - 1));
							lastRow = Math.min(lastRow, table.getRowCount());
						}
					}
				}

				for (int row = firstRow; row < lastRow; row++) {
					int preferedWidth = (int) table.getCellRenderer(row, col)
							.getTableCellRendererComponent(table, table.getValueAt(row, col), false, false, row, col)
							.getPreferredSize().getWidth();
					width = Math.max(width, preferedWidth);
				}
				header.setResizingColumn(tableColumn); // this line is very important
				tableColumn.setWidth(width + table.getIntercellSpacing().width);

				e.consume();
			}
		}
	}

	// copied from BasicTableHeader.MouseInputHandler.getResizingColumn
	private TableColumn getResizingColumn(JTableHeader header, Point p) {
		return getResizingColumn(header, p, header.columnAtPoint(p));
	}

	// copied from BasicTableHeader.MouseInputHandler.getResizingColumn
	private TableColumn getResizingColumn(JTableHeader header, Point p, int column) {
		if (column == -1) {
			return null;
		}

		Rectangle r = header.getHeaderRect(column);
		r.grow(-3, 0);

		if (r.contains(p)) {
			return null;
		}

		int midPoint = r.x + r.width / 2;
		int columnIndex = 0;
		if (header.getComponentOrientation().isLeftToRight()) {
			columnIndex = (p.x < midPoint) ? column - 1 : column;
		} else {
			columnIndex = (p.x < midPoint) ? column : column - 1;
		}

		if (columnIndex == -1) {
			return null;
		}

		return header.getColumnModel().getColumn(columnIndex);
	}
}
