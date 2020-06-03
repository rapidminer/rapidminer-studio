/**
 * Copyright (C) 2001-2020 by RapidMiner and the contributors
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
package com.rapidminer.gui.viewer;

import java.awt.Color;
import java.awt.event.MouseEvent;
import java.util.HashMap;
import java.util.Map;
import javax.swing.table.JTableHeader;

import com.rapidminer.adaption.belt.IOTable;
import com.rapidminer.belt.column.Column;
import com.rapidminer.belt.table.LegacyType;
import com.rapidminer.belt.table.Table;
import com.rapidminer.belt.util.ColumnRole;
import com.rapidminer.example.Attributes;
import com.rapidminer.gui.look.Colors;
import com.rapidminer.gui.look.RapidLookTools;
import com.rapidminer.gui.tools.AttributeGuiTools;
import com.rapidminer.gui.tools.ExtendedJTable;
import com.rapidminer.tools.I18N;


/**
 * Can be used to display (parts of) the data by means of a JTable. Used to display
 * {@link IOTable} results in the {@link BeltTableDataViewer}.
 *
 * @author Ingo Mierswa, Gisa Meier
 * @since 9.7.0
 */
class BeltTableDataViewerTable extends ExtendedJTable {

	private static final int MAXIMAL_CONTENT_LENGTH = 200;

	protected static final int MAX_ROW_HEIGHT = 30;
	protected static final int MIN_ROW_HEIGHT = 25;

	private static final long serialVersionUID = 5435239693801265693L;

	private int[] dateColumns;

	private Map<String, Color> mappingAttributeNamesToColor;

	private BeltTableDataViewerTableModel dvTableModel;

	BeltTableDataViewerTable(Table table) {
		this.mappingAttributeNamesToColor = new HashMap<>();
		setAutoResizeMode(AUTO_RESIZE_OFF);
		setFixFirstColumnForRearranging(true);
		installToolTip();

		// handles the highlighting of the currently hovered row
		setRowHighlighting(true);
		setTable(table);
	}

	private void setTable(Table table) {
		this.dvTableModel = new BeltTableDataViewerTableModel(table);
		setModel(dvTableModel);

		dateColumns = new int[table.width() + 1];
		dateColumns[0] = NO_DATE_FORMAT;
		int index = 1;
		for (int i = 0; i < dvTableModel.getNumberOfSpecials(); i++) {
			String name = dvTableModel.getColumnName(index);
			ColumnRole role = table.getFirstMetaData(name, ColumnRole.class);
			Color specialColor = getColorForRole(role);
			mappingAttributeNamesToColor.put(name, specialColor);
			setCellFormat(table, index, name);
			index++;
		}

		for (int i = 0; i < table.width() - dvTableModel.getNumberOfSpecials(); i++) {
			String name = dvTableModel.getColumnName(index);
			mappingAttributeNamesToColor.put(name, Colors.WHITE);
			setCellFormat(table, index, name);
			index++;
		}

		setCellColorProvider((row, column) -> {
			int col = convertColumnIndexToModel(column);
			Color returnCol;
			if (dvTableModel != null && dvTableModel.getColumn(col) != null) {
				returnCol = mappingAttributeNamesToColor.get(dvTableModel.getColumnName(col));
			} else {
				returnCol = Colors.WHITE;
			}

			return returnCol;
		});

		setGridColor(Colors.TABLE_CELL_BORDER);

		setCutOnLineBreak(true);
		setMaximalTextLength(MAXIMAL_CONTENT_LENGTH);

		final int size = table.height();
		setRowHeight(calcRowHeight(MAX_ROW_HEIGHT, MIN_ROW_HEIGHT, size));

	}

	/**
	 * Sets different date-time formats or no date format.
	 */
	private void setCellFormat(Table table, int index, String name) {
		Column column = dvTableModel.getColumn(index);
		if (column.type().id() == Column.TypeId.DATE_TIME) {
			if (table.getFirstMetaData(name, LegacyType.class) == LegacyType.DATE) {
				dateColumns[index] = DATE_FORMAT;
			} else if (table.getFirstMetaData(name, LegacyType.class) == LegacyType.TIME) {
				dateColumns[index] = TIME_FORMAT;
			} else {
				dateColumns[index] = DATE_TIME_FORMAT;
			}
		} else {
			dateColumns[index] = NO_DATE_FORMAT;
		}
	}

	/**
	 * Sets the selected rows and updates the row height.
	 *
	 * @param selection
	 * 		the array describing the selected rows, can be {@code null} for the whole table
	 */
	void setRows(int[] selection) {
		dvTableModel.setRows(selection);
		int size = dvTableModel.getRowCount();
		setRowHeight(calcRowHeight(MAX_ROW_HEIGHT, MIN_ROW_HEIGHT, size));
	}

	/**
	 * Converts the role to a color.
	 */
	private Color getColorForRole(ColumnRole role) {
		String roleName = role == ColumnRole.SCORE ? Attributes.CONFIDENCE_NAME : role.toString().toLowerCase();
		return AttributeGuiTools.getColorForAttributeRole(roleName);
	}

	/**
	 * Calculates the row height depending on the count and between the given min and max.
	 */
	private static int calcRowHeight(int maxRowHeight, int minRowHeight, int count) {
		if (count == 0) {
			return maxRowHeight;
		}
		final int f = Integer.MAX_VALUE / count;
		final int m = Math.max(minRowHeight, f);
		return Math.min(maxRowHeight, m);
	}

	/**
	 * This method ensures that the correct tool tip for the current column is delivered.
	 */
	@Override
	protected JTableHeader createDefaultTableHeader() {
		JTableHeader header = new JTableHeader(columnModel) {

			private static final long serialVersionUID = 1L;

			@Override
			public String getToolTipText(MouseEvent e) {
				java.awt.Point p = e.getPoint();
				int index = columnModel.getColumnIndexAtX(p.x);
				int realColumnIndex = convertColumnIndexToModel(index);
				return BeltTableDataViewerTable.this.getHeaderToolTipText(realColumnIndex);
			}
		};
		header.putClientProperty(RapidLookTools.PROPERTY_TABLE_HEADER_BACKGROUND, Colors.WHITE);
		return header;
	}

	@Override
	public int getDateFormat(int row, int column) {
		return dateColumns[column];
	}

	/**
	 * Get the tooltip for the header row.
	 */
	private String getHeaderToolTipText(int realColumnIndex) {
		if (realColumnIndex == 0) {
			// tooltip text for the column containing the row index
			return I18N.getMessage(I18N.getGUIBundle(), "gui.label.data_view.example_index.tooltip");
		} else {
			return I18N.getMessage(I18N.getGUIBundle(), "gui.label.data_view.click_to_sort.tooltip");
		}
	}

}
