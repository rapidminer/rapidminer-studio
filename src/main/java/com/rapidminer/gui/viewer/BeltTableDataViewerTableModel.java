/**
 * Copyright (C) 2001-2020 by RapidMiner and the contributors
 *
 * Complete list of developers available at our web site:
 *
 * http://rapidminer.com
 *
 * This program is free software: you can redistribute it and/or modify it under the terms of the GNU Affero General
 * Public License as published by the Free Software Foundation, either version 3 of the License, or (at your option) any
 * later version.
 *
 * This program is distributed in the hope that it will be useful, but WITHOUT ANY WARRANTY; without even the implied
 * warranty of MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the GNU Affero General Public License for more
 * details.
 *
 * You should have received a copy of the GNU Affero General Public License along with this program. If not, see
 * http://www.gnu.org/licenses/.
 */
package com.rapidminer.gui.viewer;

import static com.rapidminer.gui.viewer.metadata.BeltMetaDataStatisticsViewer.BELT_COLUMN_ROLE_COMPARATOR;

import java.time.Instant;
import java.util.Comparator;
import java.util.Date;
import java.util.List;
import java.util.Objects;
import javax.swing.table.AbstractTableModel;

import com.rapidminer.belt.column.Column;
import com.rapidminer.belt.reader.MixedRowReader;
import com.rapidminer.belt.reader.Readers;
import com.rapidminer.belt.table.Table;
import com.rapidminer.belt.util.ColumnRole;
import com.rapidminer.example.Attribute;


/**
 * The model for the {@link BeltTableDataViewerTable}.
 *
 * @author Ingo Mierswa, Gisa Meier
 * @since 9.7.0
 */
class BeltTableDataViewerTableModel extends AbstractTableModel {

	private static final long serialVersionUID = -3057323874942971672L;

	private final int[] newOrdering;
	private final int numberOfSpecials;
	private int[] rowSelection;

	private final transient Table table;
	private final transient MixedRowReader reader;

	BeltTableDataViewerTableModel(Table table) {
		this.table = table;
		reader = Readers.mixedRowReader(table);
		newOrdering = new int[table.width()];
		List<String> labels = table.select().withMetaData(ColumnRole.class).labels();
		numberOfSpecials = labels.size();
		labels.sort(Comparator.comparing(s -> table.getFirstMetaData(s, ColumnRole.class),
				BELT_COLUMN_ROLE_COMPARATOR));

		for (int i = 0; i < labels.size(); i++) {
			int oldIndex = table.index(labels.get(i));
			newOrdering[i] = oldIndex;
		}

		int offset = labels.size();
		List<String> regular = table.select().withoutMetaData(ColumnRole.class).labels();
		for (int i = 0; i < regular.size(); i++) {
			int oldIndex = table.index(regular.get(i));
			int newIndex = i + offset;
			newOrdering[newIndex] = oldIndex;
		}

	}

	/**
	 * Sets the selected rows.
	 *
	 * @param selection
	 * 		the array describing the selected rows, can be {@code null} for the whole table
	 */
	void setRows(int[] selection) {
		this.rowSelection = selection;
	}

	@Override
	public Class<?> getColumnClass(int column) {
		Class<?> type;
		if (column == 0) {
			type = Integer.class;
		} else {
			Column attribute = getColumn(column);
			if (attribute.type().id() == Column.TypeId.DATE_TIME) {
				type = Date.class;
			} else if (attribute.type().category() == Column.Category.NUMERIC) {
				type = Double.class;
			} else {
				type = String.class;
			}
		}
		return type;
	}

	@Override
	public int getRowCount() {
		return rowSelection == null ? table.height() : rowSelection.length;
	}

	/**
	 * Returns the sum of the number of attributes and 1 for the row no.
	 * column.
	 */
	@Override
	public int getColumnCount() {
		return table.width() + 1;
	}

	@Override
	public Object getValueAt(int row, int column) {
		if (rowSelection != null) {
			row = rowSelection[row];
		}
		if (column == 0) {
			return row + 1;
		} else {
			if (reader.position() != row) {
				reader.setPosition(row - 1);
				reader.move();
			}
			Column attribute = getColumn(column);
			column = newOrdering[column - 1];
			if (attribute.type().id() == Column.TypeId.DATE_TIME) {
				Instant read = reader.getObject(column, Instant.class);
				if (read == null) {
					return Attribute.MISSING_NOMINAL_VALUE;
				}
				return Date.from(read);
			} else if (attribute.type().category() == Column.Category.NUMERIC) {
				return reader.getNumeric(column);
			} else {
				return Objects.toString(reader.getObject(column), Attribute.MISSING_NOMINAL_VALUE);
			}

		}
	}

	@Override
	public String getColumnName(int column) {
		if (column < 0) {
			return "";
		}
		if (column == 0) {
			return "Row No.";
		} else {
			int col = column - 1;
			return table.label(newOrdering[col]);
		}
	}

	/**
	 * Returns the {@link Column} for the given columnIndex.
	 *
	 * @param columnIndex
	 * 		the index of the column
	 * @return the column, {@code null} for 0
	 */
	Column getColumn(int columnIndex) {
		if (columnIndex == 0) {
			return null;
		}
		int col = columnIndex - 1;
		return table.column(newOrdering[col]);
	}

	/**
	 * @return the number of columns with a role
	 */
	int getNumberOfSpecials() {
		return numberOfSpecials;
	}


}
