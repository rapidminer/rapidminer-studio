/**
 * Copyright (C) 2001-2016 by RapidMiner and the contributors
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

import static com.rapidminer.operator.nio.ExcelExampleSource.PARAMETER_SHEET_NUMBER;

import java.io.File;
import java.io.IOException;
import java.nio.charset.Charset;
import java.util.Map;
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
public class ExcelResultSetConfiguration implements DataResultSetFactory {

	private static final String XLS_FILE_ENDING = ".xls";
	private static final String XLSX_FILE_ENDING = ".xlsx";

	private int rowOffset = -1;
	private int columnOffset = -1;
	private int rowLast = Integer.MAX_VALUE;
	private int columnLast = Integer.MAX_VALUE;

	/** Numbering starts at 0. */
	private int sheet = -1;

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

		if (excelExampleSource.isParameterSet(PARAMETER_SHEET_NUMBER)) {
			this.sheet = excelExampleSource.getParameterAsInt(PARAMETER_SHEET_NUMBER) - 1;
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
			if (excelParamter != null && !"".equals(excelParamter)) {
				File excelFile = new File(excelParamter);
				if (excelFile.exists()) {
					this.workbookFile = excelFile;
				}
			}
		}

		if (excelExampleSource.isParameterSet(AbstractDataResultSetReader.PARAMETER_DATE_FORMAT)) {
			datePattern = excelExampleSource.getParameterAsString(AbstractDataResultSetReader.PARAMETER_DATE_FORMAT);
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
	 * {@link Excel2007SheetTableModel}, depending on file).
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
		if (getFile().getAbsolutePath().endsWith(XLSX_FILE_ENDING)) {
			// excel 2007 file
			return new XlsxSheetTableModel(this, sheetIndex, readMode, getFile().getAbsolutePath(), progressListener);
		} else {
			// excel pre 2007 file
			if (workbookJXL == null) {
				createWorkbookJXL();
			}
			progressListener.setCompleted(50);
			return new ExcelSheetTableModel(workbookJXL.getSheet(sheetIndex));
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
		if (getFile().getAbsolutePath().endsWith(XLSX_FILE_ENDING)) {
			// excel 2007 file
			try (ZipFile zipFile = new ZipFile(getFile().getAbsolutePath())) {
				try {
					return getNumberOfSheets(parseWorkbook(zipFile));
				} catch (ParserConfigurationException | SAXException e) {
					throw new UserError(null, e, "xlsx_content_malformed");
				}
			}
		} else {
			// excel pre 2007 file
			if (workbookJXL == null) {
				createWorkbookJXL();
			}
			return workbookJXL.getNumberOfSheets();
		}
	}

	private int getNumberOfSheets(XlsxWorkbook workbook) {
		return workbook.xlsxWorkbookSheets.size();
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
		if (getFile().getAbsolutePath().endsWith(XLSX_FILE_ENDING)) {
			// excel 2007 file
			try (ZipFile zipFile = new ZipFile(getFile().getAbsolutePath())) {
				XlsxWorkbook workbook;
				try {
					workbook = parseWorkbook(zipFile);
					String[] sheetNames = new String[getNumberOfSheets(workbook)];
					for (int i = 0; i < getNumberOfSheets(); i++) {
						sheetNames[i] = workbook.xlsxWorkbookSheets.get(i).name;
					}
					return sheetNames;
				} catch (ParserConfigurationException | SAXException e) {
					throw new UserError(null, e, "xlsx_content_malformed");
				}

			}
		} else {
			// excel pre 2007 file
			if (workbookJXL == null) {
				createWorkbookJXL();
			}
			return workbookJXL.getSheetNames();
		}
	}

	/**
	 * Returns the encoding for this configuration.
	 *
	 * @return
	 */
	public Charset getEncoding() {
		return this.encoding;
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
		if (selectedFile.equals(this.workbookFile)) {
			return;
		}
		if (workbookJXL != null) {
			workbookJXL.close();
			workbookJXL = null;
		}
		workbookFile = selectedFile;
		rowOffset = 0;
		columnOffset = 0;
		rowLast = Integer.MAX_VALUE;
		columnLast = Integer.MAX_VALUE;
		sheet = 0;
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
	 *            fixed by the current value of {@link configuration#getDatePattern()}
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
		if (absolutePath.endsWith(XLSX_FILE_ENDING)) {
			resultSet = createExcel2007ResultSet(operator, readMode, provider);
		} else if (absolutePath.endsWith(XLS_FILE_ENDING)) {
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
		return new XlsxResultSet(operator, this, getSheet(), readMode, provider);
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

		String absolutePath = file.getAbsolutePath();
		if (absolutePath.endsWith(XLSX_FILE_ENDING)) {
			try {
				return createExcelTableModel(getSheet(), XlsxReadMode.WIZARD_PREVIEW, listener);
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
		if (workbookJXL != null) {
			workbookJXL.close();
			workbookJXL = null;
		}
	}

	@Override
	public void setParameters(AbstractDataResultSetReader source) {
		String range = Tools.getExcelColumnName(columnOffset) + (rowOffset + 1);

		// only add end range to cell range parameter if user has specified it explicitly
		if (Integer.MAX_VALUE != columnLast && Integer.MAX_VALUE != rowOffset) {
			range += ":" + Tools.getExcelColumnName(columnLast) + (rowLast + 1);
		}

		source.setParameter(ExcelExampleSource.PARAMETER_IMPORTED_CELL_RANGE, range);
		source.setParameter(PARAMETER_SHEET_NUMBER, String.valueOf(sheet + 1));
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
		if (workbookJXL != null) {
			workbookJXL.close();
		}
	}

	/**
	 * Creates the JXL workbook.
	 *
	 * @throws BiffException
	 * @throws IOException
	 */
	private void createWorkbookJXL() throws BiffException, IOException {
		File file = getFile();
		WorkbookSettings workbookSettings = new WorkbookSettings();
		if (encoding != null) {
			workbookSettings.setEncoding(encoding.name());
		}
		workbookJXL = Workbook.getWorkbook(file, workbookSettings);
	}

	/**
	 * @return the timezone
	 */
	public String getTimezone() {
		return this.timezone;
	}

	/**
	 * @return the datePattern
	 */
	public String getDatePattern() {
		return this.datePattern;
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
		File file = getFile();
		parameters.put("excel.fileLocation", file != null ? file.toString() : "");
		parameters.put("excel.sheet", String.valueOf(getSheet()));
		parameters.put("excel.rowOffset", String.valueOf(getRowOffset()));
		parameters.put("excel.rowLast", String.valueOf(getRowLast()));
		parameters.put("excel.columnOffset", String.valueOf(getColumnOffset()));
		parameters.put("excel.columnLast", String.valueOf(getColumnLast()));
	}

}
