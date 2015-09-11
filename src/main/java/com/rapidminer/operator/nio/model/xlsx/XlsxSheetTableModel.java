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
package com.rapidminer.operator.nio.model.xlsx;

import java.util.ArrayList;
import java.util.List;

import javax.swing.table.AbstractTableModel;

import com.rapidminer.operator.OperatorException;
import com.rapidminer.operator.nio.model.DefaultPreview;
import com.rapidminer.operator.nio.model.ExcelResultSetConfiguration;
import com.rapidminer.operator.nio.model.ParseException;
import com.rapidminer.operator.nio.model.xlsx.XlsxResultSet.XlsxReadMode;


/**
 * Returns values for an XLSX file backed by a {@link XlsxResultSet}.
 *
 * @author Nils Woehler
 * @since 6.3.0
 */
public class XlsxSheetTableModel extends AbstractTableModel {

	private static final long serialVersionUID = 1L;

	private static final String EMPTY_STRING = "";

	/** Cache for sheet content data */
	private final List<Object[]> sheetContentCache;

	private final int rowCount;
	private final int columnCount;

	/**
	 * @param sheetIndex
	 *            the selected sheet
	 * @param readMode
	 * @param absolutePath
	 *            the absolute path of the Excel file
	 */
	public XlsxSheetTableModel(ExcelResultSetConfiguration configuration, int sheetIndex, int previewSize,
			XlsxReadMode readMode, String absolutePath) throws OperatorException, ParseException {
		/*
		 * Keep track of the row count ourselves so we can omit empty rows at the end of the Excel
		 * file
		 */
		try (XlsxResultSet xlsxResultSet = new XlsxResultSet(null, configuration, sheetIndex, readMode)) {
			int initialCapacity = xlsxResultSet.getNumberOfRows() != -1 ? xlsxResultSet.getNumberOfRows()
					: XlsxSheetMetaDataParser.MAXIMUM_XLSX_ROW_INDEX + 1;
			this.sheetContentCache = new ArrayList<>(initialCapacity);

			this.columnCount = xlsxResultSet.getNumberOfColumns();

			if (columnCount > 0) {

				// load all data
				int rowCount = 0;
				while (xlsxResultSet.getCurrentRow() + 1 < previewSize && xlsxResultSet.hasNext()) {
					xlsxResultSet.next(null);

					// Add next row to cache
					Object[] rowContent = new Object[columnCount];
					for (int i = 0; i < columnCount; ++i) {
						if (!xlsxResultSet.isMissing(i)) {
							try {
								switch (xlsxResultSet.getNativeValueType(i)) {
									case DATE:
										rowContent[i] = xlsxResultSet.getDate(i);
										break;
									case NUMBER:
										rowContent[i] = xlsxResultSet.getNumber(i);
										break;
									case STRING:
										rowContent[i] = DefaultPreview.shortenDisplayValue(xlsxResultSet.getString(i));
										break;
									default:
										rowContent[i] = EMPTY_STRING;
										break;
								}
							}catch (ParseException e){
								rowContent[i] = EMPTY_STRING;
							}
						} else {
							rowContent[i] = EMPTY_STRING;
						}
					}
					++rowCount;
					sheetContentCache.add(rowContent);
				}
				this.rowCount = rowCount;
			} else {
				this.rowCount = 0;
			}
		}
	}

	@Override
	public Object getValueAt(int rowIndex, int columnIndex) {
		if (rowIndex < sheetContentCache.size()) {
			return sheetContentCache.get(rowIndex)[columnIndex];
		} else {
			return EMPTY_STRING;
		}
	}

	@Override
	public int getRowCount() {
		return rowCount;
	}

	@Override
	public int getColumnCount() {
		return columnCount;
	}

	@Override
	public String getColumnName(int columnIndex) {
		return XlsxUtilities.convertToColumnName(columnIndex);
	}

	/**
	 * Specifies the class of the Java object for the specified column index.
	 *
	 * @return <code>String.class </code> for all columns
	 */
	@Override
	public Class<?> getColumnClass(int columnIndex) {
		return String.class;
	}

	@Override
	public boolean isCellEditable(int rowIndex, int columnIndex) {
		return false;
	}
}
