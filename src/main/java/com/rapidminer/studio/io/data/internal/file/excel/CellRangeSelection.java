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
package com.rapidminer.studio.io.data.internal.file.excel;

import com.rapidminer.operator.nio.model.ExcelResultSetConfiguration;


/**
 *
 * Simple POJO class to store the sheet and cell selection within the
 * {@link ExcelSheetSelectionPanel}.
 *
 * @author Nils Woehler
 * @since 7.0.0
 */
class CellRangeSelection {

	private int columnIndexStart;
	private int rowIndexStart;
	private int columnIndexEnd;
	private int rowIndexEnd;

	CellRangeSelection(ExcelResultSetConfiguration config) {
		this(config.getColumnOffset(), config.getRowOffset(), config.getColumnLast(), config.getRowLast());
	}

	CellRangeSelection(int columnIndexStart, int rowIndexStart, int columnIndexEnd, int rowIndexEnd) {
		this.columnIndexStart = columnIndexStart;
		this.rowIndexStart = rowIndexStart;
		this.columnIndexEnd = columnIndexEnd;
		this.rowIndexEnd = rowIndexEnd;
	}

	/**
	 * Copy constructor.
	 *
	 * @param other
	 *            the other instance to copy
	 */
	CellRangeSelection(CellRangeSelection other) {
		this(other.columnIndexStart, other.rowIndexStart, other.columnIndexEnd, other.rowIndexEnd);
	}

	public int getColumnIndexEnd() {
		return columnIndexEnd;
	}

	public int getColumnIndexStart() {
		return columnIndexStart;
	}

	public int getRowIndexEnd() {
		return rowIndexEnd;
	}

	public int getRowIndexStart() {
		return rowIndexStart;
	}

	public void setColumnIndexStart(int columnIndexStart) {
		this.columnIndexStart = columnIndexStart;
	}

	public void setRowIndexStart(int rowIndexStart) {
		this.rowIndexStart = rowIndexStart;
	}

	public void setColumnIndexEnd(int columnIndexEnd) {
		this.columnIndexEnd = columnIndexEnd;
	}

	public void setRowIndexEnd(int rowIndexEnd) {
		this.rowIndexEnd = rowIndexEnd;
	}

}
