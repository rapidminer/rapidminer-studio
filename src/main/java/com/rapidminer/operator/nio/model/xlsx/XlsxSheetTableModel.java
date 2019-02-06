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
package com.rapidminer.operator.nio.model.xlsx;

import java.util.ArrayList;
import java.util.List;

import javax.swing.table.AbstractTableModel;

import com.rapidminer.operator.OperatorException;
import com.rapidminer.operator.nio.ImportWizardUtils;
import com.rapidminer.operator.nio.model.DefaultPreview;
import com.rapidminer.operator.nio.model.ExcelResultSetConfiguration;
import com.rapidminer.operator.nio.model.ParseException;
import com.rapidminer.operator.nio.model.ExcelSheetSelection;
import com.rapidminer.operator.nio.model.xlsx.XlsxResultSet.XlsxReadMode;
import com.rapidminer.tools.ProgressListener;
import com.rapidminer.tools.Tools;


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
	private final List<String[]> sheetContentCache;

	private final int rowCount;
	private final int columnCount;

	/** defines whether only a preview or the full data was loaded */
	private boolean isPreview = false;

	/** the number of sheet rows available in the XLSX result set */
	private final int sheetRowCount;

	/**
	 * @param configuration
	 * @param sheetIndex
	 *            the selected sheet
	 * @param readMode
	 * @param absolutePath
	 *            the absolute path of the Excel file
	 * @param progressListener
	 *            the listener to report the progress to
	 */
	public XlsxSheetTableModel(ExcelResultSetConfiguration configuration, int sheetIndex, XlsxReadMode readMode,
							   String absolutePath, ProgressListener progressListener) throws OperatorException, ParseException {
		this(configuration, ExcelSheetSelection.byIndex(sheetIndex), readMode, absolutePath, progressListener);
	}



	/**
	 * @param configuration
	 * @param sheetSelection
	 *            the selected sheet
	 * @param readMode
	 * @param absolutePath
	 *            the absolute path of the Excel file
	 * @param progressListener
	 *            the listener to report the progress to
	 */
	public XlsxSheetTableModel(ExcelResultSetConfiguration configuration, ExcelSheetSelection sheetSelection, XlsxReadMode readMode,
							   String absolutePath, ProgressListener progressListener) throws OperatorException, ParseException {

		isPreview = readMode == XlsxReadMode.WIZARD_PREVIEW;

		int previewSize;
		switch (readMode) {
			case WIZARD_SHEET_SELECTION:
				previewSize = XlsxUtilities.getSheetSelectionLength();
				break;
			case WIZARD_PREVIEW:
				previewSize = ImportWizardUtils.getPreviewLength();
				break;
			default:
				previewSize = Integer.MAX_VALUE;
		}

		if (progressListener != null) {
			progressListener.setCompleted(20);
		}

		/*
		 * Keep track of the row count ourselves so we can omit empty rows at the end of the Excel
		 * file
		 */
		try (XlsxResultSet xlsxResultSet = new XlsxResultSet(null, configuration, sheetSelection, readMode)) {
			int numberOfRows = xlsxResultSet.getNumberOfRows();
			int initialCapacity = numberOfRows != -1 ? numberOfRows : XlsxSheetMetaDataParser.MAXIMUM_XLSX_ROW_INDEX + 1;
			this.sheetContentCache = new ArrayList<>(initialCapacity);
			this.columnCount = xlsxResultSet.getNumberOfColumns();
			this.sheetRowCount = initialCapacity;

			if (progressListener != null) {
				progressListener.setCompleted(40);
			}

			if (columnCount > 0) {

				// load all data
				int rowCount = 0;
				while (xlsxResultSet.getCurrentRow() + 1 < previewSize && xlsxResultSet.hasNext()) {
					xlsxResultSet.next(null);
					if (progressListener != null) {
						int completed = 60 * rowCount / previewSize;
						progressListener.setCompleted(completed + 40);
					}

					// Add next row to cache
					String[] rowContent = new String[columnCount];
					for (int i = 0; i < columnCount; ++i) {
						if (!xlsxResultSet.isMissing(i)) {
							try {
								switch (xlsxResultSet.getNativeValueType(i)) {
									case DATE:
										rowContent[i] = Tools.formatDateTime(xlsxResultSet.getDate(i));
										break;
									case NUMBER:
										rowContent[i] = Tools.formatNumber(xlsxResultSet.getNumber(i).doubleValue());
										break;
									case STRING:
										rowContent[i] = DefaultPreview.shortenDisplayValue(xlsxResultSet.getString(i));
										break;
									default:
										rowContent[i] = EMPTY_STRING;
										break;
								}
							} catch (ParseException e) {
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

			// in case the preview size limit has been reached and more rows are available
			// a preview is displayed
			if (rowCount == previewSize && xlsxResultSet.hasNext()) {
				isPreview = true;
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

	/**
	 * @return {@code true} in case the model contains only a preview of the sheet data and not the
	 *         whole content
	 */
	public boolean isPreview() {
		return isPreview;
	}

	/**
	 * @return the number of rows within the sheet
	 */
	public int getNumberOfRows() {
		return sheetRowCount;
	}

}
