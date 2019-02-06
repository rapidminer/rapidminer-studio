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
package com.rapidminer.operator.nio.model;

import java.io.File;
import java.io.IOException;
import java.text.DateFormat;
import java.util.Arrays;
import java.util.Date;
import java.util.LinkedList;
import java.util.List;
import java.util.NoSuchElementException;
import java.util.TimeZone;

import com.rapidminer.operator.Operator;
import com.rapidminer.operator.OperatorException;
import com.rapidminer.operator.ProcessStoppedException;
import com.rapidminer.operator.UserError;
import com.rapidminer.parameter.ParameterTypeDateFormat;
import com.rapidminer.tools.ProgressListener;
import com.rapidminer.tools.Tools;

import jxl.Cell;
import jxl.CellType;
import jxl.DateCell;
import jxl.NumberCell;
import jxl.Sheet;
import jxl.Workbook;
import jxl.WorkbookSettings;
import jxl.read.biff.BiffException;


/**
 * A DataResultSet for an Excel File.
 *
 * @author Sebastian Land, Marco Boeck
 *
 */
public class ExcelResultSet implements DataResultSet {

	private Sheet sheet = null;

	private boolean[] emptyRows;
	private boolean[] emptyColumns;

	private int rowOffset = 0;
	private int columnOffset = 0;

	private int totalNumberOfRows = 0;
	private int totalNumberOfColumns = 0;

	private int currentRow;
	private Cell[] currentRowCells;

	private Workbook workbook;

	// private ExcelResultSetConfiguration configuration;

	private String[] attributeNames;

	private final DateFormatProvider dateFormatProvider;

	private Operator operator = null;

	/**
	 * The constructor to build an ExcelResultSet from the given configuration. The calling operator
	 * might be null. It is only needed for error handling.
	 */
	public ExcelResultSet(Operator callingOperator, final ExcelResultSetConfiguration configuration,
			final DateFormatProvider dateFormatProvider) throws OperatorException {
		// reading configuration
		columnOffset = configuration.getColumnOffset();
		rowOffset = Math.max(configuration.getRowOffset(), 0);
		currentRow = configuration.getRowOffset() - 1;

		// check range
		if (columnOffset > configuration.getColumnLast() || rowOffset > configuration.getRowLast() || columnOffset < 0
				|| rowOffset < 0) {
			throw new UserError(callingOperator, 223, Tools.getExcelColumnName(columnOffset) + rowOffset + ":"
					+ Tools.getExcelColumnName(configuration.getColumnLast()) + configuration.getRowLast());
		}

		// check file presence
		if (configuration.getFile() == null) {
			throw new UserError(callingOperator, "file_consumer.no_file_defined");
		}

		// load the excelWorkbook if it is not set
		try {
			File file = configuration.getFile();
			WorkbookSettings workbookSettings = new WorkbookSettings();
			if (configuration.getEncoding() != null) {
				workbookSettings.setEncoding(configuration.getEncoding().name());
			}
			workbook = Workbook.getWorkbook(file, workbookSettings);
		} catch (IOException e) {
			throw new UserError(callingOperator, 302, configuration.getFile().getPath(), e.getMessage());
		} catch (BiffException e) {
			throw new UserError(callingOperator, 302, configuration.getFile().getPath(), e.getMessage());
		}

		try {
			sheet = configuration.selectSheetFrom(workbook);
		} catch (IndexOutOfBoundsException | IllegalArgumentException e) {
			throw new UserError(callingOperator, 953, configuration.getSheet() + 1);
		} catch (ExcelSheetSelection.SheetNotFoundException e) {
			throw new UserError(callingOperator, 321, configuration.getFile(), e.getMessage());
		}

		totalNumberOfColumns = Math.min(configuration.getColumnLast(), sheet.getColumns() - 1) - columnOffset + 1;
		totalNumberOfRows = Math.min(configuration.getRowLast(), sheet.getRows() - 1) - rowOffset + 1;

		if (totalNumberOfColumns < 0 || totalNumberOfRows < 0) {
			throw new UserError(callingOperator, 404);
		}

		emptyColumns = new boolean[totalNumberOfColumns];
		emptyRows = new boolean[totalNumberOfRows];

		// filling offsets
		Arrays.fill(emptyColumns, true);
		Arrays.fill(emptyRows, true);

		// determine offsets and emptiness
		boolean foundAny = false;
		for (int r = 0; r < totalNumberOfRows; r++) {
			for (int c = 0; c < totalNumberOfColumns; c++) {
				if (emptyRows[r] || emptyColumns[c]) {
					final Cell cell = sheet.getCell(c + columnOffset, r + rowOffset);
					if (cell.getType() != CellType.EMPTY && !"".equals(cell.getContents().trim())) {
						foundAny = true;
						emptyRows[r] = false;
						emptyColumns[c] = false;
					}
				}
			}
		}
		if (!foundAny) {
			throw new UserError(callingOperator, 302, configuration.getFile().getPath(), "spreadsheet seems to be empty");
		}

		// retrieve attribute names: first count columns
		int numberOfAttributes = 0;
		List<Integer> nonEmptyColumnsList = new LinkedList<>();
		for (int i = 0; i < totalNumberOfColumns; i++) {
			if (!emptyColumns[i]) {
				numberOfAttributes++;
				nonEmptyColumnsList.add(i);
			}
		}

		// retrieve or generate attribute names
		attributeNames = new String[nonEmptyColumnsList.size()];

		if (!configuration.isEmulatingOldNames()) {
			for (int i = 0; i < numberOfAttributes; i++) {
				attributeNames[i] = Tools.getExcelColumnName(nonEmptyColumnsList.get(i));
			}
		} else {
			// emulate old 5.0.x style
			for (int i = 0; i < numberOfAttributes; i++) {
				if (!emptyColumns[i]) {
					attributeNames[i] = "attribute_" + i;
				}
			}
		}

		final String timezone = configuration.getTimezone();
		if (dateFormatProvider != null) {
			if (timezone != null) {
				this.dateFormatProvider = () -> {
					DateFormat format = dateFormatProvider.geDateFormat();
					format.setTimeZone(TimeZone.getTimeZone(timezone));
					return format;
				};
			} else {
				this.dateFormatProvider = dateFormatProvider;
			}
		} else {
			String datePattern = configuration.getDatePattern();
			final DateFormat dateFormat = ParameterTypeDateFormat.createCheckedDateFormat(callingOperator, (datePattern == null ? "" : datePattern));
			if (timezone != null) {
				dateFormat.setTimeZone(TimeZone.getTimeZone(timezone));
			}
			this.dateFormatProvider = () -> dateFormat;
		}

		if (callingOperator != null) {
			callingOperator.getProgress().setCheckForStop(false);
			callingOperator.getProgress().setTotal(totalNumberOfRows);
			operator = callingOperator;
		}
	}

	@Override
	public void reset(ProgressListener listener) {
		currentRow = rowOffset - 1;
		if (listener != null) {
			listener.setTotal(totalNumberOfRows);
			listener.setCompleted(0);
		}

		if (operator != null) {
			operator.getProgress().setTotal(totalNumberOfRows);
		}
	}

	@Override
	public boolean hasNext() {
		int nextRow = currentRow + 1;
		while (nextRow < totalNumberOfRows + rowOffset && emptyRows[nextRow - rowOffset]) {
			nextRow++;
		}
		return nextRow < totalNumberOfRows + rowOffset;
	}

	@Override
	public void next(ProgressListener listener) {
		currentRow++;
		while (currentRow < totalNumberOfRows + rowOffset && emptyRows[currentRow - rowOffset]) {
			currentRow++;
		}

		if (currentRow >= totalNumberOfRows + rowOffset) {
			throw new NoSuchElementException("No further row in excel sheet.");
		}

		currentRowCells = new Cell[attributeNames.length];
		int columnCounter = 0;
		for (int c = 0; c < totalNumberOfColumns; c++) {
			if (!emptyColumns[c]) {
				currentRowCells[columnCounter] = sheet.getCell(c + columnOffset, currentRow);
				columnCounter++;
			}
		}

		// notifying progress listener
		if (listener != null) {
			listener.setCompleted(currentRow);
		}

		if (operator != null) {
			try {
				if (currentRow % 100 == 0) {
					operator.getProgress().setCompleted(currentRow);
				}
			} catch (ProcessStoppedException e) {
				// Will not happen, because check for stop is deactivated
			}
		}
	}

	@Override
	public void close() throws OperatorException {
		// configuration.closeWorkbook();
	}

	@Override
	public int getNumberOfColumns() {
		return attributeNames.length;
	}

	@Override
	public String[] getColumnNames() {
		return attributeNames;
	}

	@Override
	public boolean isMissing(int columnIndex) {
		Cell cell = getCurrentCell(columnIndex);
		return cell.getType() == CellType.EMPTY || cell.getType() == CellType.ERROR
				|| cell.getType() == CellType.FORMULA_ERROR || cell.getContents() == null
				|| "".equals(cell.getContents().trim());
	}

	private Cell getCurrentCell(int index) {
		// return currentRowCells[index + columnOffset];
		return currentRowCells[index];
	}

	@Override
	public Number getNumber(int columnIndex) throws ParseException {
		final Cell cell = getCurrentCell(columnIndex);
		if (cell.getType() == CellType.NUMBER || cell.getType() == CellType.NUMBER_FORMULA) {
			final double value = ((NumberCell) cell).getValue();
			return Double.valueOf(value);
		} else {
			String valueString = cell.getContents();
			try {
				return Double.valueOf(valueString);
			} catch (NumberFormatException e) {
				throw new ParseException(
						new ParsingError(currentRow, columnIndex, ParsingError.ErrorCode.UNPARSEABLE_REAL, valueString));
			}
		}
	}

	@Override
	public Date getDate(int columnIndex) throws ParseException {
		final Cell cell = getCurrentCell(columnIndex);
		if (cell.getType() == CellType.DATE || cell.getType() == CellType.DATE_FORMULA) {
			Date date = ((DateCell) cell).getDate();

			// hack to get actual date written in excel sheet. converts date to UTC
		    int offset = TimeZone.getDefault().getOffset(date.getTime());
			return new Date(date.getTime() - offset);
		} else {
			String valueString = cell.getContents();
			DateFormat dateFormat = dateFormatProvider.geDateFormat();
			if (dateFormat == null) {
				throw new ParseException(
						new ParsingError(currentRow, columnIndex, ParsingError.ErrorCode.UNPARSEABLE_DATE, "illegal date format pattern"));
			}
			try {
				return dateFormat.parse(valueString);
			} catch (java.text.ParseException e) {
				throw new ParseException(
						new ParsingError(currentRow, columnIndex, ParsingError.ErrorCode.UNPARSEABLE_DATE, valueString));
			}
		}
	}

	@Override
	public String getString(int columnIndex) {
		return getCurrentCell(columnIndex).getContents();
	}

	@Override
	public int[] getValueTypes() {
		return new int[this.attributeNames.length];
	}

	@Override
	public ValueType getNativeValueType(int columnIndex) throws ParseException {
		final CellType type = getCurrentCell(columnIndex).getType();
		if (type == CellType.EMPTY) {
			return ValueType.EMPTY;
		} else if (type == CellType.NUMBER || type == CellType.NUMBER_FORMULA) {
			return ValueType.NUMBER;
		} else if (type == CellType.DATE || type == CellType.DATE_FORMULA) {
			return ValueType.DATE;
		} else {
			return ValueType.STRING;
		}
	}

	@Override
	public int getCurrentRow() {
		return currentRow;
	}

}
