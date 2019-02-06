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

import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.text.SimpleDateFormat;
import java.util.Arrays;
import java.util.Date;
import java.util.LinkedList;
import java.util.List;
import java.util.NoSuchElementException;
import java.util.TimeZone;
import java.util.logging.Level;

import org.apache.poi.openxml4j.exceptions.InvalidFormatException;
import org.apache.poi.ss.usermodel.Cell;
import org.apache.poi.ss.usermodel.DateUtil;
import org.apache.poi.ss.usermodel.Row;
import org.apache.poi.ss.usermodel.Sheet;
import org.apache.poi.ss.usermodel.Workbook;
import org.apache.poi.ss.usermodel.WorkbookFactory;

import com.rapidminer.operator.Operator;
import com.rapidminer.operator.OperatorException;
import com.rapidminer.operator.UserError;
import com.rapidminer.operator.nio.model.xlsx.XlsxResultSet;
import com.rapidminer.parameter.ParameterTypeDateFormat;
import com.rapidminer.tools.I18N;
import com.rapidminer.tools.LogService;
import com.rapidminer.tools.ProgressListener;
import com.rapidminer.tools.Tools;


/**
 * A DataResultSet for an Excel 2007 File.
 *
 * @author Marco Boeck
 *
 * @deprecated Was replaced by {@link XlsxResultSet}.
 * @since 6.3.0
 *
 */
@Deprecated
public class Excel2007ResultSet implements DataResultSet {

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
	private InputStream workbookInputStream;

	private String[] attributeNames;

	private String timeZone;
	private String dateFormat;

	/**
	 * The constructor to build an ExcelResultSet from the given configuration. The calling operator
	 * might be null. It is only needed for error handling.
	 */
	public Excel2007ResultSet(Operator callingOperator, ExcelResultSetConfiguration configuration) throws OperatorException {
		// reading configuration
		columnOffset = configuration.getColumnOffset();
		rowOffset = configuration.getRowOffset();
		currentRow = configuration.getRowOffset() - 1;

		timeZone = configuration.getTimezone();
		dateFormat = configuration.getDatePattern();
		try {
			workbookInputStream = new FileInputStream(configuration.getFile());
			workbook = WorkbookFactory.create(workbookInputStream);
		} catch (IOException | InvalidFormatException e) {
			throw new UserError(callingOperator, "file_consumer.error_loading_file");
		}

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
		try {
			sheet = configuration.selectSheetFrom(workbook);
		} catch (IndexOutOfBoundsException | IllegalArgumentException e) {
			throw new UserError(callingOperator, 953, configuration.getSheet() + 1);
		} catch (ExcelSheetSelection.SheetNotFoundException e) {
			throw new UserError(callingOperator, 321, configuration.getFile(), e.getMessage());
		}
		Row row = sheet.getRow(sheet.getFirstRowNum());
		if (row == null) {
			totalNumberOfColumns = 0;
			totalNumberOfRows = 0;
		} else {
			totalNumberOfColumns = Math.min(configuration.getColumnLast(), sheet.getRow(sheet.getFirstRowNum())
					.getLastCellNum() - 1)
					- columnOffset + 1;
			totalNumberOfRows = Math.min(configuration.getRowLast(), sheet.getLastRowNum()) - rowOffset + 1;
		}

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
					final Row rowItem = sheet.getRow(r + rowOffset);
					if (rowItem == null) {
						continue;
					}
					final Cell cell = rowItem.getCell(c + columnOffset);
					if (cell == null) {
						continue;
					}
					boolean empty;
					try {
						empty = "".equals(cell.getStringCellValue().trim());
					} catch (IllegalStateException e) {
						empty = false;
					}
					if (!empty) {
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
	}

	@Override
	public void reset(ProgressListener listener) {
		currentRow = rowOffset - 1;
		if (listener != null) {
			listener.setTotal(totalNumberOfRows);
			listener.setCompleted(0);
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
				currentRowCells[columnCounter] = sheet.getRow(currentRow).getCell(c + columnOffset);
				columnCounter++;
			}
		}

		// notifying progress listener
		if (listener != null) {
			listener.setCompleted(currentRow);
		}
	}

	@Override
	public void close() throws OperatorException {
		try {
			if (workbookInputStream != null) {
				workbookInputStream.close();
				workbookInputStream = null;
			}
		} catch (IOException e) {
			LogService.getRoot().log(
					Level.WARNING,
					I18N.getMessage(LogService.getRoot().getResourceBundle(),
							"com.rapidminer.operator.nio.model.ExcelResultSetConfiguration.close_workbook_error",
							e.getMessage()), e);
		}
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
		try {
			return cell == null || cell.getCellType() == Cell.CELL_TYPE_BLANK
					|| cell.getCellType() == Cell.CELL_TYPE_ERROR || "".equals(cell.getStringCellValue().trim());
		} catch (IllegalStateException e) {
			return false;
		}
	}

	/**
	 * Gets the cell with the given index in the current row.
	 *
	 * @param index
	 * @return
	 */
	private Cell getCurrentCell(int index) {
		return currentRowCells[index];
	}

	@Override
	public Number getNumber(int columnIndex) throws ParseException {
		final Cell cell = getCurrentCell(columnIndex);
		if (cell == null) {
			return Double.NaN;
		}
		if (cell.getCellType() == Cell.CELL_TYPE_NUMERIC || cell.getCellType() == Cell.CELL_TYPE_FORMULA
				&& cell.getCachedFormulaResultType() == Cell.CELL_TYPE_NUMERIC) {
			final double value = cell.getNumericCellValue();
			return Double.valueOf(value);
		} else {
			String valueString = "";
			try {
				valueString = cell.getStringCellValue();
				return Double.valueOf(valueString);
			} catch (NumberFormatException e) {
				throw new ParseException(new ParsingError(currentRow, columnIndex, ParsingError.ErrorCode.UNPARSEABLE_REAL,
						valueString));
			} catch (IllegalStateException e) {
				throw new ParseException(new ParsingError(currentRow, columnIndex, ParsingError.ErrorCode.UNPARSEABLE_REAL,
						"CELL_NOT_NUMERIC"));
			}
		}
	}

	@Override
	public Date getDate(int columnIndex) throws ParseException {
		final Cell cell = getCurrentCell(columnIndex);
		if (cell == null) {
			return null;
		}
		if (cell.getCellType() == Cell.CELL_TYPE_NUMERIC) {
			return cell.getDateCellValue();
		} else {
			try {
				String valueString = cell.getStringCellValue();
				try {
					SimpleDateFormat simpleDateFormat = ParameterTypeDateFormat.createCheckedDateFormat(dateFormat, null);
					simpleDateFormat.setTimeZone(TimeZone.getTimeZone(this.timeZone));
					return simpleDateFormat.parse(valueString);
				} catch (java.text.ParseException e) {
					throw new ParseException(new ParsingError(currentRow, columnIndex,
							ParsingError.ErrorCode.UNPARSEABLE_DATE, valueString));
				} catch (UserError userError) {
					throw new ParseException(new ParsingError(currentRow, columnIndex,
							ParsingError.ErrorCode.UNPARSEABLE_DATE, userError.getMessage()));
				}
			} catch (IllegalStateException e) {
				throw new ParseException(new ParsingError(currentRow, columnIndex, ParsingError.ErrorCode.UNPARSEABLE_DATE,
						"CELL_NEITHER_NUMERIC_NOR_NOMINAL"));
			}
		}
	}

	@Override
	public String getString(int columnIndex) {
		final Cell cell = getCurrentCell(columnIndex);
		if (cell == null) {
			return "";
		}
		if (cell.getCellType() == Cell.CELL_TYPE_NUMERIC) {
			return String.valueOf(cell.getNumericCellValue());
		} else if (cell.getCellType() == Cell.CELL_TYPE_FORMULA) {
			String value;
			if (cell.getCachedFormulaResultType() == Cell.CELL_TYPE_NUMERIC) {
				value = String.valueOf(cell.getNumericCellValue());
			} else {
				value = cell.getStringCellValue();
			}
			return value;
		} else {
			try {
				return cell.getStringCellValue();
			} catch (IllegalStateException e) {
				return "";
			}
		}
	}

	@Override
	public int[] getValueTypes() {
		return new int[this.attributeNames.length];
	}

	@Override
	public ValueType getNativeValueType(int columnIndex) throws ParseException {
		Cell cell = getCurrentCell(columnIndex);
		final int type = cell.getCellType();
		if (type == Cell.CELL_TYPE_BLANK) {
			return ValueType.EMPTY;
		} else if (type == Cell.CELL_TYPE_STRING) {
			return ValueType.STRING;
		} else if (type == Cell.CELL_TYPE_NUMERIC) {
			if (DateUtil.isCellDateFormatted(cell)) {
				return ValueType.DATE;
			} else {
				return ValueType.NUMBER;
			}
		} else if (type == Cell.CELL_TYPE_FORMULA) {
			if (cell.getCachedFormulaResultType() == Cell.CELL_TYPE_NUMERIC) {
				return ValueType.NUMBER;
			} else {
				return ValueType.STRING;
			}
		} else {
			return ValueType.STRING;
		}
	}

	@Override
	public int getCurrentRow() {
		return currentRow;
	}

}
