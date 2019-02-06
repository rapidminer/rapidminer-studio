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
package com.rapidminer.studio.io.gui.internal.steps.configuration;

import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.Set;
import javax.swing.ImageIcon;

import com.rapidminer.core.io.data.ColumnMetaData;
import com.rapidminer.gui.tools.SwingTools;
import com.rapidminer.tools.I18N;
import com.rapidminer.tools.Observer;


/**
 * Model for a table showing errors and warnings.
 *
 * @author Gisa Schaefer
 * @since 7.0.0
 */
class ErrorWarningTableModel extends AbstractErrorWarningTableModel {

	private static final long serialVersionUID = 1L;

	/** image indicating errors */
	private static final ImageIcon ICON_ERROR = SwingTools.createIcon("16/error.png");
	private static final ImageIcon ICON_WARNING = SwingTools.createIcon("16/sign_warning.png");

	private static final String COLUMN = I18N.getGUILabel("io.dataimport.step.data_column_configuration.error_table.column");
	private static final String ROW = I18N.getGUILabel("io.dataimport.step.data_column_configuration.error_table.row");
	private static final String NO_ENTRY = "-";

	private static final String HEADER_TYPE = I18N
			.getGUILabel("io.dataimport.step.data_column_configuration.error_table.header_type");
	private static final String HEADER_VALUE = I18N
			.getGUILabel("io.dataimport.step.data_column_configuration.error_table.header_value");
	private static final String HEADER_MESSAGE = I18N
			.getGUILabel("io.dataimport.step.data_column_configuration.error_table.header_message");

	private static final String PARSING_ERROR_TYPE = I18N
			.getGUILabel("io.dataimport.step.data_column_configuration.error_table.parsing_error");
	private static final String COLUMN_ERROR_TYPE = I18N
			.getGUILabel("io.dataimport.step.data_column_configuration.error_table.column_error");

	private final List<ParsingError> parsingErrors = new ArrayList<>();
	private final List<ColumnError> columnErrors = new ArrayList<>();
	private List<ColumnMetaData> columnMetaData;
	private boolean faultTolerant;

	ErrorWarningTableModel(final ConfigureDataValidator validator) {
		final Observer<Set<Integer>> observer = (observable, arg) -> {
			setParsingErrors(validator.getParsingErrors());
			setColumnErrors(validator.getColumnErrors());
			fireTableDataChanged();
		};
		validator.addObserver(observer, false);
	}

	@Override
	public String getColumnName(int column) {
		switch (column) {
			case 0:
				return "";
			case 1:
				return ROW;
			case 2:
				return COLUMN;
			case 3:
				return HEADER_TYPE;
			case 4:
				return HEADER_VALUE;
			case 5:
				return HEADER_MESSAGE;
			default:
				return super.getColumnName(column);
		}
	}

	@Override
	public int getRowCount() {
		return parsingErrors.size() + columnErrors.size();
	}

	@Override
	public int getColumnCount() {
		return 6;
	}

	@Override
	public Object getValueAt(int rowIndex, int columnIndex) {
		final int numberOfColumnErrors = columnErrors.size();
		if (rowIndex < numberOfColumnErrors) {
			ColumnError error = columnErrors.get(rowIndex);
			switch (columnIndex) {
				case 0:
					return ICON_ERROR;
				case 1:
				case 2:
					return NO_ENTRY;
				case 3:
					return COLUMN_ERROR_TYPE;
				case 4:
					return error.getProblematicValue();
				case 5:
					return error.getMessage();
				default:
					return null;
			}
		} else {
			ParsingError error = parsingErrors.get(rowIndex - numberOfColumnErrors);
			switch (columnIndex) {
				case 0:
					return faultTolerant ? ICON_WARNING : ICON_ERROR;
				case 1:
					return error.getRow();
				case 2:
					return columnMetaData.get(error.getColumn()).getName();
				case 3:
					return PARSING_ERROR_TYPE;
				case 4:
					return error.getOriginalValue();
				case 5:
					return error.getMessage();
				default:
					return null;
			}
		}
	}

	/**
	 * Sets the parsing errors and sorts them by rows.
	 *
	 * @param errors
	 *            the parsing errors to set
	 */
	private synchronized void setParsingErrors(Collection<ParsingError> errors) {
		this.parsingErrors.clear();
		this.parsingErrors.addAll(errors);
		this.parsingErrors.sort((o1, o2) -> {
			int rowDiff = o1.getRow() - o2.getRow();
			if (rowDiff != 0) {
				return rowDiff;
			} else {
				return o1.getColumn() - o2.getColumn();
			}
		});
	}

	/**
	 * Sets the column errors.
	 *
	 * @param errors
	 *            the errors to set
	 */
	private void setColumnErrors(Collection<ColumnError> errors) {
		this.columnErrors.clear();
		this.columnErrors.addAll(errors);
	}

	/**
	 * Sets the column meta data
	 *
	 * @param columnMetaData
	 *            the column meta data
	 */
	void setColumnMetaData(List<ColumnMetaData> columnMetaData) {
		this.columnMetaData = columnMetaData;
	}

	/**
	 * Sets whether the model is fault tolerant with respect to parsing errors. Notifies listeners
	 * that the table content may have changed.
	 *
	 * @param faultTolerant
	 *            the value to set
	 */
	void setFaultTolerant(boolean faultTolerant) {
		this.faultTolerant = faultTolerant;
		fireTableDataChanged();
	}

	@Override
	public int getErrorCount() {
		int numberOfparsingErrors = faultTolerant ? 0 : parsingErrors.size();
		return numberOfparsingErrors + columnErrors.size();
	}

	@Override
	public int getWarningCount() {
		return faultTolerant ? parsingErrors.size() : 0;
	}

}
