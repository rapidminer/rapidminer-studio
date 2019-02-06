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

import javax.swing.table.TableModel;


/**
 * Listener that is informed of any {@link ExcelSheetSelectionPanelModel} changes.
 *
 * @author Nils Woehler
 * @since 7.0.0
 */
public interface ExcelSheetSelectionModelListener {

	/**
	 * Called in case the selected sheet has been changed.
	 * 
	 * @param newSheetIndex
	 *            the new sheet index
	 * @param sheetNames
	 *            all available sheet names
	 * @param newTableModel
	 *            the table model for the selected sheet
	 * @param isShowingPreview
	 *            whether the table model is only showing a preview instead of the whole data
	 * @param wasModelLoaded
	 *            whether the table model was loaded ({@code true}) or retrieved from cache (
	 *            {@code false})
	 */
	void sheetIndexUpdated(int newSheetIndex, String[] sheetNames, TableModel newTableModel, boolean isShowingPreview,
			boolean wasModelLoaded);

	/**
	 * Called in case a new table model is loaded.
	 */
	void loadingNewTableModel();

	/**
	 * Called in case the loading of a new table model has failed.
	 *
	 * @param e
	 *            the error that has occurred during loading
	 */
	void reportErrorLoadingTableModel(Exception e);

	/**
	 * Called on a header row index change.
	 *
	 * @param newHeaderRowIndex
	 *            the new header row index
	 */
	void headerRowIndexUpdated(int newHeaderRowIndex);

	/**
	 * Called on a cell range selection change.
	 *
	 * @param newSelection
	 *            the new {@link CellRangeSelection}
	 */
	void cellRangeSelectionUpdate(CellRangeSelection newSelection);
}
