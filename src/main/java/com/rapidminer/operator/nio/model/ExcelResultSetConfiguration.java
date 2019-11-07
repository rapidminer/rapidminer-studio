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
import java.nio.charset.Charset;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;
import java.util.function.Function;
import java.util.zip.ZipFile;
import javax.swing.table.AbstractTableModel;
import javax.swing.table.TableModel;
import javax.xml.parsers.ParserConfigurationException;

import org.apache.poi.openxml4j.exceptions.InvalidFormatException;
import org.xml.sax.SAXException;

import com.rapidminer.operator.Operator;
import com.rapidminer.operator.OperatorException;
import com.rapidminer.operator.UserError;
import com.rapidminer.operator.nio.ExcelExampleSource;
import com.rapidminer.operator.nio.ExcelSheetTableModel;
import com.rapidminer.operator.nio.model.xlsx.XlsxResultSet;
import com.rapidminer.operator.nio.model.xlsx.XlsxResultSet.XlsxReadMode;
import com.rapidminer.operator.nio.model.xlsx.XlsxSheetTableModel;
import com.rapidminer.operator.nio.model.xlsx.XlsxWorkbookParser;
import com.rapidminer.operator.nio.model.xlsx.XlsxWorkbookParser.XlsxWorkbook;
import com.rapidminer.operator.ports.metadata.ExampleSetMetaData;
import com.rapidminer.parameter.ParameterTypeDateFormat;
import com.rapidminer.parameter.UndefinedParameterError;
import com.rapidminer.tools.ProgressListener;
import com.rapidminer.tools.Tools;
import com.rapidminer.tools.io.Encoding;

import jxl.Workbook;
import jxl.WorkbookSettings;
import jxl.read.biff.BiffException;


/**
 * A class holding information about configuration of the Excel Result Set
 *
 * @author Sebastian Land, Marco Boeck, Nils Woehler
 */
public class ExcelResultSetConfiguration implements DataResultSetFactory, ExcelSheetSelection {

	public static final String EXCEL_FILE_LOCATION = "excel.fileLocation";
	public static final String EXCEL_SHEET_SELECTION_MODE = "excel.sheetSelectionMode";
	public static final String EXCEL_SHEET_NAME = "excel.sheetName";
	public static final String EXCEL_SHEET = "excel.sheet";
	public static final String EXCEL_ROW_OFFSET = "excel.rowOffset";
	public static final String EXCEL_ROW_LAST = "excel.rowLast";
	public static final String EXCEL_COLUMN_OFFSET = "excel.columnOffset";
	public static final String EXCEL_COLUMN_LAST = "excel.columnLast";
	public static final String EXCEL_HEADER_ROW_INDEX = "excel.headerRowIndex";

	private static final String XLS_FILE_ENDING = ".xls";
	private static final String XLSX_FILE_ENDING = ".xlsx";

	private int rowOffset = -1;
	private int columnOffset = -1;
	private int rowLast = Integer.MAX_VALUE;
	private int columnLast = Integer.MAX_VALUE;

	/** Numbering starts at 0. */
	private int sheet = -1;
	private String sheetName;
	private SheetSelectionMode sheetSelectionMode = SheetSelectionMode.BY_INDEX;

	private Charset encoding;
	private jxl.Workbook workbookJXL;
	private File workbookFile;

	private boolean isEmulatingOldNames;

	private String timezone;
	private String datePattern;

	/**
	 * This constructor must read in all settings from the parameters of the given operator.
	 *
	 * @throws OperatorException
	 */
	public ExcelResultSetConfiguration(ExcelExampleSource excelExampleSource) throws OperatorException {
		if (excelExampleSource.isParameterSet(ExcelExampleSource.PARAMETER_IMPORTED_CELL_RANGE)) {
			parseExcelRange(excelExampleSource.getParameterAsString(ExcelExampleSource.PARAMETER_IMPORTED_CELL_RANGE));
		}
		if (excelExampleSource.isParameterSet(ExcelExampleSource.PARAMETER_SHEET_SELECTION)) {
			sheetSelectionMode = SheetSelectionMode.get(excelExampleSource.getParameterAsInt(ExcelExampleSource.PARAMETER_SHEET_SELECTION));
		}
		if (excelExampleSource.isParameterSet(ExcelExampleSource.PARAMETER_SHEET_NAME)) {
			sheetName = excelExampleSource.getParameterAsString(ExcelExampleSource.PARAMETER_SHEET_NAME);
		}
		if (excelExampleSource.isParameterSet(ExcelExampleSource.PARAMETER_SHEET_NUMBER)) {
			sheet = excelExampleSource.getParameterAsInt(ExcelExampleSource.PARAMETER_SHEET_NUMBER) - 1;
		}
		if (excelExampleSource.isFileSpecified()) {
			this.workbookFile = excelExampleSource.getSelectedFile();
		} else {

			String excelParamter;
			try {
				excelParamter = excelExampleSource.getParameter(ExcelExampleSource.PARAMETER_EXCEL_FILE);
			} catch (UndefinedParameterError e) {
				excelParamter = null;
			}
			if (excelParamter != null && !excelParamter.isEmpty()) {
				File excelFile = new File(excelParamter);
				if (excelFile.exists()) {
					this.workbookFile = excelFile;
				}
			}
		}

		if (excelExampleSource.isParameterSet(ParameterTypeDateFormat.PARAMETER_DATE_FORMAT)) {
			datePattern = excelExampleSource.getParameterAsString(ParameterTypeDateFormat.PARAMETER_DATE_FORMAT);
		}

		if (excelExampleSource.isParameterSet(AbstractDataResultSetReader.PARAMETER_TIME_ZONE)) {
			timezone = excelExampleSource.getParameterAsString(AbstractDataResultSetReader.PARAMETER_TIME_ZONE);
		}

		encoding = Encoding.getEncoding(excelExampleSource);

		isEmulatingOldNames = excelExampleSource.getCompatibilityLevel()
				.isAtMost(ExcelExampleSource.CHANGE_5_0_11_NAME_SCHEMA);
	}

	/**
	 * This will create a completely empty result set configuration
	 */
	public ExcelResultSetConfiguration() {}

	/**
	 * @return the 0-based row offset
	 */
	public int getRowOffset() {
		return rowOffset;
	}

	/**
	 * @return the 0-based column offset
	 */
	public int getColumnOffset() {
		return columnOffset;
	}

	/**
	 * Returns if there is an active workbook.
	 */
	public boolean hasWorkbook() {
		return workbookJXL != null;
	}

	/**
	 * Creates an excel table model (either {@link ExcelSheetTableModel} or
	 * {@link XlsxSheetTableModel}, depending on file).
	 *
	 * @param sheetIndex
	 *            the index of the sheet (0-based)
	 * @param readMode
	 *            the read mode for {@link XlsxSheetTableModel} creation. It defines whether only a
	 *            preview or the whole sheet content will be loaded
	 * @param progressListener
	 *            the progress listener to report progress to
	 * @return
	 * @throws BiffException
	 * @throws IOException
	 * @throws InvalidFormatException
	 */
	public AbstractTableModel createExcelTableModel(int sheetIndex, XlsxReadMode readMode, ProgressListener progressListener)
			throws BiffException, IOException, InvalidFormatException, OperatorException, ParseException {
		return createExcelTableModel(ExcelSheetSelection.byIndex(sheetIndex), readMode, progressListener);
	}

	/**
	 * Creates an excel table model (either {@link ExcelSheetTableModel} or
	 * {@link XlsxSheetTableModel}, depending on file).
	 *
	 * @param sheetSelection
	 *            the Sheet Selection method
	 * @param readMode
	 *            the read mode for {@link XlsxSheetTableModel} creation. It defines whether only a
	 *            preview or the whole sheet content will be loaded
	 * @param progressListener
	 *            the progress listener to report progress to
	 * @return
	 * @throws BiffException
	 * @throws IOException
	 * @throws InvalidFormatException
	 */
	public AbstractTableModel createExcelTableModel(ExcelSheetSelection sheetSelection, XlsxReadMode readMode, ProgressListener progressListener)
			throws BiffException, IOException, InvalidFormatException, OperatorException, ParseException {
		if (getFile().getAbsolutePath().toLowerCase(Locale.ENGLISH).endsWith(XLSX_FILE_ENDING)) {
			// excel 2007 file
			return new XlsxSheetTableModel(this, sheetSelection, readMode, getFile().getAbsolutePath(), progressListener);
		} else {
			// excel pre 2007 file
			progressListener.setCompleted(50);

			try {
				return new ExcelSheetTableModel(sheetSelection.selectSheetFrom(getOrCreateWorkbookJXL()));
			} catch (ExcelSheetSelection.SheetNotFoundException e) {
				throw new IOException(e);
			}
		}
	}

	/**
	 * Returns the number of sheets in the excel file
	 *
	 * @return
	 * @throws IOException
	 * @throws BiffException
	 * @throws InvalidFormatException
	 */
	public int getNumberOfSheets() throws BiffException, IOException, InvalidFormatException, UserError {
		if (getFile().getAbsolutePath().toLowerCase(Locale.ENGLISH).endsWith(XLSX_FILE_ENDING)) {
			// excel 2007 file
			try (ZipFile zipFile = new ZipFile(getFile().getAbsolutePath())) {
				return parseWorkbook(zipFile).xlsxWorkbookSheets.size();
			} catch (ParserConfigurationException | SAXException e) {
				throw new UserError(null, e, "xlsx_content_malformed");
			}
		} else {
			// excel pre 2007 file
			return getOrCreateWorkbookJXL().getNumberOfSheets();
		}
	}

	private XlsxWorkbook parseWorkbook(ZipFile zipFile)
			throws UserError, IOException, ParserConfigurationException, SAXException {
		return new XlsxWorkbookParser().parseZipEntry(zipFile);
	}

	/**
	 * Returns the names of all sheets in the excel file
	 *
	 * @return
	 * @throws IOException
	 * @throws BiffException
	 * @throws InvalidFormatException
	 */
	public String[] getSheetNames() throws BiffException, IOException, InvalidFormatException, UserError {
		if (getFile().getAbsolutePath().toLowerCase(Locale.ENGLISH).endsWith(XLSX_FILE_ENDING)) {
			// excel 2007 file
			try (ZipFile zipFile = new ZipFile(getFile().getAbsolutePath())) {
				return parseWorkbook(zipFile).xlsxWorkbookSheets.stream().map(s -> s.name).toArray(String[]::new);
			} catch (ParserConfigurationException | SAXException e) {
				throw new UserError(null, e, "xlsx_content_malformed");
			}
		} else {
			// excel pre 2007 file
			return getOrCreateWorkbookJXL().getSheetNames();
		}
	}

	/**
	 * Returns the encoding for this configuration.
	 *
	 * @return
	 */
	public Charset getEncoding() {
		return encoding;
	}

	/**
	 * @param encoding
	 *            the new encoding
	 */
	public void setEncoding(Charset encoding) {
		this.encoding = encoding;
	}

	/**
	 * This returns the file of the referenced excel file
	 */
	public File getFile() {
		return workbookFile;
	}

	/**
	 * This will set the workbook file. It will assure that an existing preopened workbook will be
	 * closed if files differ.
	 */
	public void setWorkbookFile(File selectedFile) {
		if (Objects.equals(selectedFile, workbookFile)) {
			return;
		}
		closeWorkbook();
		workbookFile = selectedFile;
		rowOffset = 0;
		columnOffset = 0;
		rowLast = Integer.MAX_VALUE;
		columnLast = Integer.MAX_VALUE;
		sheet = 0;
		sheetName = null;
	}

	/**
	 * @return the index of the last row to import. The index is 0 based. In case it is
	 *         {@link Integer#MAX_VALUE} all rows with content should be imported.
	 */
	public int getRowLast() {
		return rowLast;
	}

	public void setRowLast(int rowLast) {
		this.rowLast = rowLast;
	}

	/**
	 * @return the index of the last column to import. The index is 0 based. In case it is
	 *         {@link Integer#MAX_VALUE} all columns with content should be imported.
	 */
	public int getColumnLast() {
		return columnLast;
	}

	public void setColumnLast(int columnLast) {
		this.columnLast = columnLast;
	}

	public int getSheet() {
		return sheet;
	}

	public void setSheet(int sheet) {
		this.sheet = sheet;
	}

	/**
	 * Requires {@link #setSheetSelectionMode(SheetSelectionMode)} to be set to {@code SheetSelectionMode.BY_NAME}
	 * @param sheetName The sheetName to select
	 */
	public void setSheetByName(String sheetName) {
		this.sheetName = sheetName;
	}

	/**
	 * @return the current value of the sheetByName selection
	 */
	public String getSheetByName() {
		return sheetName;
	}

	/**
	 * Returns the currently selected sheet selection method
	 *
	 * @return
	 */
	public ExcelSheetSelection getSheetSelectionMethod() {
		return sheetSelectionMode.getMethodFor(this);
	}

	public SheetSelectionMode getSheetSelectionMode() {
		return sheetSelectionMode;
	}

	public void setSheetSelectionMode(SheetSelectionMode sheetSelectionMode) {
		this.sheetSelectionMode = sheetSelectionMode;
	}

	public void setRowOffset(int rowOffset) {
		this.rowOffset = rowOffset;
	}

	public void setColumnOffset(int columnOffset) {
		this.columnOffset = columnOffset;
	}

	@Override
	public DataResultSet makeDataResultSet(Operator operator) throws OperatorException {
		return makeDataResultSet(operator, XlsxReadMode.OPERATOR);
	}

	/**
	 * Creates a {@link DataResultSet} based on the current configuration and the provided
	 * {@link XlsxReadMode}.
	 *
	 * @param operator
	 *            the operator to create the {@link DataResultSet} for. Might be {@code null} in
	 *            case no operator is available
	 * @param readMode
	 *            the read mode
	 * @param provider
	 *            a {@link DateFormatProvider}, can be {@code null} in which case the date format is
	 *            fixed by the current value of {@link #getDatePattern()}
	 * @return the created {@link DataResultSet}
	 * @throws OperatorException
	 *             in case the creation fails because of an invalid configuration
	 */
	public DataResultSet makeDataResultSet(Operator operator, XlsxReadMode readMode, DateFormatProvider provider)
			throws OperatorException {
		File file = getFile();
		if (file == null) {
			throw new UndefinedParameterError(ExcelExampleSource.PARAMETER_EXCEL_FILE, operator);
		}
		String absolutePath = file.getAbsolutePath();
		DataResultSet resultSet;
		if (absolutePath.toLowerCase(Locale.ENGLISH).endsWith(XLSX_FILE_ENDING)) {
			resultSet = createExcel2007ResultSet(operator, readMode, provider);
		} else if (absolutePath.toLowerCase(Locale.ENGLISH).endsWith(XLS_FILE_ENDING)) {
			// excel pre 2007 file
			resultSet = new ExcelResultSet(operator, this, provider);
		} else {
			// we might also get a file object that has neither .xlsx nor .xls as file ending,
			// so we have no choice but to try and open the file with the pre 2007 JXL lib to
			// see if it works. If it does not work, it's an excel 2007 file.
			try {
				Workbook.getWorkbook(file);
				resultSet = new ExcelResultSet(operator, this, provider);
			} catch (Exception e) {
				resultSet = createExcel2007ResultSet(operator, readMode, provider);
			}
		}
		return resultSet;
	}

	/**
	 * Creates a {@link DataResultSet} based on the current configuration and the provided
	 * {@link XlsxReadMode}.
	 *
	 * @param operator
	 *            the operator to create the {@link DataResultSet} for. Might be {@code null} in
	 *            case no operator is available
	 * @param readMode
	 *            the read mode
	 * @return the created {@link DataResultSet}
	 * @throws OperatorException
	 *             in case the creation fails because of an invalid configuration
	 */
	public DataResultSet makeDataResultSet(Operator operator, XlsxReadMode readMode) throws OperatorException {
		return makeDataResultSet(operator, readMode, null);
	}

	/**
	 * Creates a new XLSX DataResultSet for the specified operator.
	 *
	 * @param operator
	 *            the operator which is used as error source in case something goes wrong
	 * @param readMode
	 *            the read mode which should be used to read the file. The read mode defines how
	 *            many lines should actually be read.
	 * @return the new XLSX DataResultSet
	 */
	@SuppressWarnings("deprecation")
	private DataResultSet createExcel2007ResultSet(Operator operator, XlsxReadMode readMode, DateFormatProvider provider)
			throws OperatorException {
		if (operator == null || operator.getCompatibilityLevel().isAbove(ExcelExampleSource.CHANGE_6_2_0_OLD_XLSX_IMPORT)) {
			return createXLSXResultSet(operator, readMode, provider);
		} else {
			return new Excel2007ResultSet(operator, this);
		}
	}

	private XlsxResultSet createXLSXResultSet(Operator operator, XlsxReadMode readMode, DateFormatProvider provider)
			throws UserError {
		return new XlsxResultSet(operator, this, getSheetSelectionMethod(), readMode, provider);
	}

	/**
	 * See class comment on {@link ExcelSheetTableModel} for a comment why that class is not used
	 * here. In fact we are using a {@link DefaultPreview} here as well.
	 */
	@Override
	public TableModel makePreviewTableModel(ProgressListener listener) throws OperatorException {
		File file = getFile();
		if (file == null) {
			throw new UserError(null, 205, ExcelExampleSource.PARAMETER_EXCEL_FILE, "");
		}

		if (file.getAbsolutePath().toLowerCase(Locale.ENGLISH).endsWith(XLSX_FILE_ENDING)) {
			try {
				return createExcelTableModel(getSheetSelectionMethod(), XlsxReadMode.WIZARD_PREVIEW, listener);
			} catch (BiffException | InvalidFormatException | IOException | ParseException e) {
				throw new UserError(null, e, "xlsx_content_malformed");
			}
		} else {
			try (final DataResultSet resultSet = makeDataResultSet(null)) {
				return new DefaultPreview(resultSet, listener);
			} catch (ParseException e) {
				throw new UserError(null, 302, file.getPath(), e.getMessage());
			}
		}
	}

	public void closeWorkbook() {
		close();
		workbookJXL = null;
	}

	@Override
	public void setParameters(AbstractDataResultSetReader source) {
		String range = Tools.getExcelColumnName(columnOffset) + (rowOffset + 1);

		// only add end range to cell range parameter if user has specified it explicitly
		if (Integer.MAX_VALUE != columnLast && Integer.MAX_VALUE != rowOffset) {
			range += ":" + Tools.getExcelColumnName(columnLast) + (rowLast + 1);
		}

		source.setParameter(ExcelExampleSource.PARAMETER_IMPORTED_CELL_RANGE, range);
		source.setParameter(ExcelExampleSource.PARAMETER_SHEET_SELECTION, String.valueOf(sheetSelectionMode.getIndex()));
		source.setParameter(ExcelExampleSource.PARAMETER_SHEET_NUMBER, String.valueOf(sheet + 1));
		source.setParameter(ExcelExampleSource.PARAMETER_SHEET_NAME, sheetName);
		source.setParameter(ExcelExampleSource.PARAMETER_EXCEL_FILE, workbookFile.getAbsolutePath());
	}

	public void parseExcelRange(String range) throws OperatorException {
		String[] split = range.split(":", 2);
		try {
			int[] topLeft = parseExcelCell(split[0]);
			columnOffset = topLeft[0];
			rowOffset = topLeft[1];
			if (split.length < 2) {
				rowLast = Integer.MAX_VALUE;
				columnLast = Integer.MAX_VALUE;
			} else {
				int[] bottomRight = parseExcelCell(split[1]);
				columnLast = bottomRight[0];
				rowLast = bottomRight[1];
			}
		} catch (OperatorException e) {
			throw new UserError(null, e, 223, range);
		}
	}

	private static int[] parseExcelCell(String string) throws OperatorException {
		int i = 0;
		int column = 0;
		int row = 0;
		while (i < string.length() && Character.isLetter(string.charAt(i))) {
			char c = string.charAt(i);
			c = Character.toUpperCase(c);
			if (c < 'A' || c > 'Z') {
				throw new UserError(null, 224, string);
			}
			column *= 26;
			column += c - 'A' + 1;
			i++;
		}
		if (i < string.length()) { // at least one digit left
			String columnStr = string.substring(i);
			try {
				row = Integer.parseInt(columnStr);
			} catch (NumberFormatException e) {
				throw new UserError(null, 224, string);
			}
		}
		return new int[] { column - 1, row - 1 };
	}

	@Override
	public String getResourceName() {
		return workbookFile.getAbsolutePath();
	}

	@Override
	public ExampleSetMetaData makeMetaData() {
		final ExampleSetMetaData result = new ExampleSetMetaData();
		if (rowLast != Integer.MAX_VALUE) {
			result.setNumberOfExamples(rowLast - rowOffset + 1);
		}
		return result;
	}

	/**
	 * This returns whether the old naming style should be kept from prior to 5.1.000 versions.
	 */
	public boolean isEmulatingOldNames() {
		return isEmulatingOldNames;
	}

	@Override
	public void close() {
		if (hasWorkbook()) {
			workbookJXL.close();
		}
	}

	/**
	 * @return the timezone
	 */
	public String getTimezone() {
		return timezone;
	}

	/**
	 * @return the datePattern
	 */
	public String getDatePattern() {
		return datePattern;
	}

	/**
	 * @param timezone
	 *            the timezone to set
	 */
	public void setTimezone(String timezone) {
		this.timezone = timezone;
	}

	/**
	 * @param datePattern
	 *            the datePattern to set
	 */
	public void setDatePattern(String datePattern) {
		this.datePattern = datePattern;
	}

	/**
	 * Write the parameters of the {@link ExcelResultSetConfiguration} into a map. Each parameter
	 * value is written as a string value.
	 *
	 * @param parameters
	 *            the map to store the parameter to
	 */
	public void storeConfiguration(Map<String, String> parameters) {
		parameters.put(EXCEL_FILE_LOCATION, Objects.toString(getFile(), ""));
		parameters.put(EXCEL_SHEET_SELECTION_MODE, String.valueOf(sheetSelectionMode));
		parameters.put(EXCEL_SHEET, String.valueOf(getSheet()));
		parameters.put(EXCEL_SHEET_NAME, String.valueOf(getSheetByName()));
		parameters.put(EXCEL_ROW_OFFSET, String.valueOf(getRowOffset()));
		parameters.put(EXCEL_ROW_LAST, String.valueOf(getRowLast()));
		parameters.put(EXCEL_COLUMN_OFFSET, String.valueOf(getColumnOffset()));
		parameters.put(EXCEL_COLUMN_LAST, String.valueOf(getColumnLast()));
	}

	@Override
	public XlsxWorkbookParser.XlsxWorkbookSheet selectSheetFrom(List<XlsxWorkbookParser.XlsxWorkbookSheet> sheets) throws SheetNotFoundException {
		return getSheetSelectionMethod().selectSheetFrom(sheets);
	}

	@Override
	public jxl.Sheet selectSheetFrom(jxl.Workbook workbookJXL) throws SheetNotFoundException {
		return getSheetSelectionMethod().selectSheetFrom(workbookJXL);
	}

	@Override
	public org.apache.poi.ss.usermodel.Sheet selectSheetFrom(org.apache.poi.ss.usermodel.Workbook workbook) throws SheetNotFoundException {
		return getSheetSelectionMethod().selectSheetFrom(workbook);
	}

	/**
	 * Creates the Workbook if needed
	 *
	 * @return the existing or freshly created workbook
	 * @throws IOException
	 * @throws BiffException
	 */
	private jxl.Workbook getOrCreateWorkbookJXL() throws IOException, BiffException {
		if (!hasWorkbook()) {
			WorkbookSettings workbookSettings = new WorkbookSettings();
			Optional.ofNullable(encoding).map(Charset::name).ifPresent(workbookSettings::setEncoding);
			workbookJXL = Workbook.getWorkbook(getFile(), workbookSettings);
		}
		return workbookJXL;
	}

	/**
	 * A sheet can be selected by either it's name or it's index
	 * <p>
	 * After choosing a SelectionMode with {@link #setSheetSelectionMode(SheetSelectionMode)} use the methods {@link #setSheet(int)} or {@link #setSheetByName(String)} to specify the index or name.
	 */
	public enum SheetSelectionMode {
		/** Selects a sheet by {@link #getSheet()} */
		BY_INDEX(ExcelExampleSource.SHEET_SELECT_BY_INDEX, (ExcelResultSetConfiguration c) -> ExcelSheetSelection.byIndex(c.getSheet())),
		/** Select a sheet by {@link #getSheetByName()} */
		BY_NAME(ExcelExampleSource.SHEET_SELECT_BY_NAME, (ExcelResultSetConfiguration c) -> ExcelSheetSelection.byName(c.getSheetByName()));

		private int index;
		private Function<ExcelResultSetConfiguration, ExcelSheetSelection> selectionFunction;

		SheetSelectionMode(int index, Function<ExcelResultSetConfiguration, ExcelSheetSelection> selectionFunction) {
			this.index = index;
			this.selectionFunction = selectionFunction;
		}

		/**
		 * Returns the ExcelExampleSource position of the selected method
		 *
		 * @return
		 */
		public int getIndex() {
			return index;
		}

		/**
		 * Returns the selected selection method for the given configuration
		 *
		 * @param configuration
		 * @return The selected sheet selection method
		 */
		public ExcelSheetSelection getMethodFor(ExcelResultSetConfiguration configuration) {
			return selectionFunction.apply(configuration);
		}

		/**
		 * Returns the SheetSelectionMode for the given position
		 *
		 * @param position
		 * @return
		 */
		public static SheetSelectionMode get(int position) {
			for (SheetSelectionMode mode : SheetSelectionMode.values()) {
				if (mode.index == position) {
					return mode;
				}
			}
			throw new IllegalArgumentException("Could not find SheetSelectionMode at position " + position);
		}
	}
}
