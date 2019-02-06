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

import java.io.IOException;
import java.util.HashMap;
import java.util.Map;

import javax.swing.JTable;
import javax.swing.table.TableModel;

import org.apache.poi.openxml4j.exceptions.InvalidFormatException;

import com.rapidminer.gui.tools.ProgressThread;
import com.rapidminer.gui.tools.ProgressThreadStoppedException;
import com.rapidminer.operator.OperatorException;
import com.rapidminer.operator.nio.model.ParseException;
import com.rapidminer.operator.nio.model.xlsx.XlsxResultSet.XlsxReadMode;
import com.rapidminer.operator.nio.model.xlsx.XlsxSheetMetaDataParser;
import com.rapidminer.operator.nio.model.xlsx.XlsxSheetTableModel;
import com.rapidminer.operator.nio.model.xlsx.XlsxUtilities.XlsxCellCoordinates;
import com.rapidminer.studio.io.data.internal.ResultSetAdapter;

import jxl.read.biff.BiffException;


/**
 * Model for the {@link ExcelSheetSelectionPanel} which stores the selected sheet index, the header
 * row index and the cell range selection.
 *
 * @author Nils Woehler
 * @since 7.0.0
 */
class ExcelSheetSelectionPanelModel {

	private static final String LOAD_WORKBOOK_PG_ID = "load_workbook";

	private final Map<Integer, TableModel> tableModelCache = new HashMap<>();
	private final ExcelSheetSelectionModelListener listener;
	private final ExcelDataSource dataSource;

	private boolean isShowingPreview = false;

	private CellRangeSelection cellRangeSelection = null;
	private int headerRowIndex = 0;
	private int sheetIndex = 0;

	private ProgressThread currentThread;

	/**
	 * Constructs a new {@link ExcelSheetSelectionPanelModel} instance.
	 *
	 * @param ds
	 *            the {@link ExcelDataSource}
	 * @param listener
	 *            the listener which is informed about model changes
	 */
	ExcelSheetSelectionPanelModel(ExcelDataSource ds, ExcelSheetSelectionModelListener listener) {
		this.dataSource = ds;
		this.listener = listener;
	}

	/**
	 * Uses the current table selection to update the cell range selection.
	 */
	void updateCellRangeByTableSelection(JTable contentTable) {
		int columnIndexStart = contentTable.getSelectedColumn();
		int rowIndexStart = contentTable.getSelectedRow();
		int columnIndexEnd = columnIndexStart + contentTable.getSelectedColumnCount() - 1;
		int rowIndexEnd = rowIndexStart + contentTable.getSelectedRowCount() - 1;
		setCellRangeSelection(new CellRangeSelection(columnIndexStart, rowIndexStart, columnIndexEnd, rowIndexEnd));
	}

	/**
	 * Updates the cell range selection
	 *
	 * @param newSelection
	 *            the new selection
	 */
	void setCellRangeSelection(CellRangeSelection newSelection) {
		if (newSelection != null) {
			this.cellRangeSelection = new CellRangeSelection(newSelection);
		} else {
			this.cellRangeSelection = null;
		}
		listener.cellRangeSelectionUpdate(newSelection);
	}

	/**
	 * @return a copy of the current cell range selection or {@code null} in case the selection is
	 *         invalid
	 */
	CellRangeSelection getCellRangeSelection() {
		if (cellRangeSelection == null) {
			return null;
		} else {
			return new CellRangeSelection(cellRangeSelection);
		}
	}

	/**
	 * @param headerRowIndex
	 *            the new header row index
	 */
	void setHeaderRowIndex(int headerRowIndex) {
		this.headerRowIndex = headerRowIndex;
		listener.headerRowIndexUpdated(headerRowIndex);
	}

	/**
	 * @return the 0-based index of the header row or {@link ResultSetAdapter#NO_HEADER_ROW} in case
	 *         no header row is defined
	 */
	int getHeaderRowIndex() {
		return headerRowIndex;
	}

	/**
	 * @return the selected sheet index
	 */
	int getSheetIndex() {
		return sheetIndex;
	}

	/**
	 * Updates the selected sheet index and loads a new table model. As a side-effect both the cell
	 * range selection and the header row index are reseted.
	 *
	 * @param newSheetIndex
	 *            the new sheet index
	 */
	void setSheetIndex(final int newSheetIndex) {
		int oldSheetIndex = this.sheetIndex;
		if (oldSheetIndex != newSheetIndex) {
			this.sheetIndex = newSheetIndex;
			updateModel(newSheetIndex, 0, new CellRangeSelection(0, XlsxCellCoordinates.NO_ROW_NUMBER, Integer.MAX_VALUE,
					XlsxSheetMetaDataParser.MAXIMUM_XLSX_ROW_INDEX));
		}
	}

	/**
	 * Updates the cell selection model with the provided new sheetIndex, headerRowIndex, and
	 * {@link CellRangeSelection}.
	 *
	 * @param sheetIndex
	 *            the new sheetIndex
	 * @param headerRowIndex
	 *            the new headerRowIndex
	 * @param selection
	 *            the new cell range selection
	 */
	void updateModel(final int sheetIndex, final int headerRowIndex, final CellRangeSelection selection) {

		ProgressThread loadWorkbook = new ProgressThread(LOAD_WORKBOOK_PG_ID, false) {

			@Override
			public void run() {

				// load new table model or use cached model
				if (loadTableModel(sheetIndex)) {
					// reset header row and cell range selection
					setHeaderRowIndex(headerRowIndex);
					setCellRangeSelection(new CellRangeSelection(selection));
				}
			}

			/**
			 * Loads the table model for the provided sheet. In case the model has been loaded
			 * before, a cached version is returned.
			 *
			 * @param sheetIndex
			 *            the index of the sheet
			 * @return {@code true} in case the loading was successful, {@code false} otherwise
			 */
			private boolean loadTableModel(int sheetIndex) {
				// initializing progress
				getProgressListener().setTotal(130);
				getProgressListener().setCompleted(0);

				try {
					// loading workbook if necessary
					final int numberOfSheets = dataSource.getResultSetConfiguration().getNumberOfSheets();
					final String[] sheetNames = dataSource.getResultSetConfiguration().getSheetNames();

					if (sheetIndex >= numberOfSheets) {
						sheetIndex = 0;
					}
					final int selectedSheet = sheetIndex;

					// check whether a table model is already cached and use it
					boolean modelLoaded = false;
					TableModel model = tableModelCache.get(Integer.valueOf(selectedSheet));
					if (model == null) {
						listener.loadingNewTableModel();

						model = dataSource.getResultSetConfiguration().createExcelTableModel(selectedSheet,
								XlsxReadMode.WIZARD_SHEET_SELECTION, getProgressListener());
						tableModelCache.put(Integer.valueOf(selectedSheet), model);
						modelLoaded = true;
						getProgressListener().setCompleted(110);
					}

					// check whether only a data preview is shown because the file content is
					// too large (only XLSX files can create a preview)
					if (model instanceof XlsxSheetTableModel) {
						isShowingPreview = ((XlsxSheetTableModel) model).isPreview();
					} else {
						isShowingPreview = false;
					}

					final TableModel selectedModel = model;
					final boolean wasModelLoaded = modelLoaded;

					getProgressListener().setCompleted(130);

					// inform about model change
					fireSheetIndexUpdated(sheetIndex, sheetNames, selectedModel, wasModelLoaded);

					return true;
				} catch (BiffException | IOException | OperatorException | InvalidFormatException | ParseException e) {
					listener.reportErrorLoadingTableModel(e);
					return false;
				} catch (ProgressThreadStoppedException pge) {
					return false;
				} finally {
					getProgressListener().complete();
				}
			}

			private void fireSheetIndexUpdated(int newSheetIndex, String[] sheetNames, TableModel newTableModel,
					boolean wasModelLoaded) {
				listener.sheetIndexUpdated(newSheetIndex, sheetNames, newTableModel, isShowingPreview(), wasModelLoaded);
			}
		};
		loadWorkbook.addDependency(LOAD_WORKBOOK_PG_ID);
		loadWorkbook.start();
		currentThread = loadWorkbook;

	}

	/**
	 * @return whether the current table model is only showing a preview instead of the whole data
	 */
	boolean isShowingPreview() {
		return isShowingPreview;
	}

	/**
	 * Clears the table model cache.
	 */
	void clearTableModelCache() {
		tableModelCache.clear();
	}

	/**
	 * Cancels the loading progress
	 */
	void cancelLoading() {
		if (currentThread != null) {
			currentThread.cancel();
		}
	}

}
