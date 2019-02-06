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
package com.rapidminer.studio.io.gui.internal;

/**
 * Event types that can be logged via
 * {@link DataImportWizardUtils#logStats(DataWizardEventType, String)}
 *
 * @since 7.0.0
 * @author Nils Woehler, Marcel Michel
 */
public enum DataWizardEventType {

	/** main data source selected */
	DATASOURCE_SELECTED,
	/** new local file data source selected */
	FILE_DATASOURCE_SELECTED,
	/** when changing the file type in the file location step */
	FILE_TYPE_CHANGED,
	/** entering first step */
	STARTING,
	/** switching to next step */
	NEXT_STEP,
	/** switching to previous step */
	PREVIOUS_STEP,
	/** closing the wizard */
	CLOSED,
	/** searching for data sources in the overview step */
	SEARCH_TYPE,
	/** when clicking on the handle errors as missing values check box */
	ERROR_HANDLING_CHANGED,
	/** when changing the date format */
	DATE_FORMAT_CHANGED,
	/** when changing a column type */
	COLUMN_TYPE_CHANGED,
	/** when changing the header row state in the excel sheet selection view */
	EXCEL_HEADER_ROW_STATE,
	/** when changing the header row state in the CSV sheet selection view */
	CSV_HEADER_ROW_STATE,
	/** when changing the column separator */
	CSV_SEPARATOR_CHANGED,
}
