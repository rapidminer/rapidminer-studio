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

import java.io.File;
import java.io.IOException;
import java.math.BigDecimal;
import java.nio.charset.Charset;
import java.nio.charset.StandardCharsets;
import java.text.DateFormat;
import java.text.DecimalFormat;
import java.util.Collections;
import java.util.Date;
import java.util.LinkedList;
import java.util.List;
import java.util.TimeZone;
import java.util.logging.Level;
import java.util.zip.ZipFile;

import javax.xml.parsers.ParserConfigurationException;
import javax.xml.stream.XMLInputFactory;
import javax.xml.stream.XMLStreamException;

import org.apache.poi.ss.usermodel.DateUtil;
import org.xml.sax.SAXException;

import com.rapidminer.operator.Operator;
import com.rapidminer.operator.OperatorException;
import com.rapidminer.operator.ProcessStoppedException;
import com.rapidminer.operator.UserError;
import com.rapidminer.operator.nio.model.ColumnMetaData;
import com.rapidminer.operator.nio.model.DataResultSet;
import com.rapidminer.operator.nio.model.DateFormatProvider;
import com.rapidminer.operator.nio.model.ExcelResultSetConfiguration;
import com.rapidminer.operator.nio.model.ExcelSheetSelection;
import com.rapidminer.operator.nio.model.ParseException;
import com.rapidminer.operator.nio.model.ParsingError;
import com.rapidminer.operator.nio.model.xlsx.XlsxUtilities.XlsxCell;
import com.rapidminer.operator.nio.model.xlsx.XlsxWorkbookParser.XlsxWorkbook;
import com.rapidminer.operator.nio.model.xlsx.XlsxWorkbookRelationParser.XlsxWorkbookRel;
import com.rapidminer.parameter.ParameterTypeDateFormat;
import com.rapidminer.tools.I18N;
import com.rapidminer.tools.LogService;
import com.rapidminer.tools.ProgressListener;


/**
 * A DataResultSet for an Excel 2007 files (XLSX). It uses StAX parsers based on the file format
 * definitions of ECMA-376, 4th Edition to parse configuration and worksheet content from XML files
 * stored within a XLSX file.
 *
 * @author Nils Woehler
 * @since 6.3.0
 */
public class XlsxResultSet implements DataResultSet {

	/**
	 * Defines whether the Excel file is read by the operator or by the Wizard.
	 */
	public enum XlsxReadMode {
		WIZARD_WORKPANE, WIZARD_PREVIEW, OPERATOR,
		/**
		 * Specifies that the {@link XlsxResultSet} was created to display preview content in the
		 * sheet selection step of the new data import dialog. If used the
		 * {@link XlsxSheetTableModel} will load a data preview instead of the full sheet content.
		 */
		WIZARD_SHEET_SELECTION
	}

	/**
	 * The factory used to create XML StAX streams.
	 */
	private static final XMLInputFactory XML_STREAM_FACTORY = XMLInputFactory.newFactory();

	static {
		XML_STREAM_FACTORY.setProperty(XMLInputFactory.IS_NAMESPACE_AWARE, false);
		XML_STREAM_FACTORY.setProperty(XMLInputFactory.IS_VALIDATING, false);
		XML_STREAM_FACTORY.setProperty(XMLInputFactory.IS_COALESCING, true);

		// Re-enable once we can use Aalto or Woodstox parser
		// XML_STREAM_FACTORY.setProperty(XMLInputFactory2.P_LAZY_PARSING, true);
		// XML_STREAM_FACTORY.setProperty(XMLInputFactory2.P_INTERN_NAMES, true);
		// XML_STREAM_FACTORY.setProperty(XMLInputFactory2.P_INTERN_NS_URIS, true);
		// XML_STREAM_FACTORY.setProperty(XMLInputFactory2.P_PRESERVE_LOCATION, false);
		// XML_STREAM_FACTORY.setProperty(XMLInputFactory2.P_REPORT_PROLOG_WHITESPACE, false);
	}

	/**
	 * Returned value in case boolean is stored as 0 in XLSX file
	 */
	private static final String BOOLEAN_FALSE = "false";

	/**
	 * Returned value in case boolean is stored as 1 in XLSX file
	 */
	private static final String BOOLEAN_TRUE = "true";

	/**
	 * Format used to format numbers that should be used as text (nominal) values. This will prevent
	 * scientific notation like '1.12123E12'. Instead it will return '1121230000000'. Numbers with
	 * fractions will be display with up to 30 fraction digits (specified by the number of # after
	 * the dot).
	 */
	private final DecimalFormat decimalFormat = new DecimalFormat("#.##############################");

	/** Configuration, set by constructor */
	private final ExcelResultSetConfiguration configuration;

	/** The StAX parser which parses the worksheet content. */
	private final XlsxSheetContentParser worksheetParser;

	/**
	 * The {@link DateFormatProvider} used to parse cells that contain date entries
	 */
	private final DateFormatProvider dateFormatProvider;

	/**
	 * The parsed sheet meta data which, e.g., contains information about the cell range to parse.
	 */
	private final XlsxSheetMetaData sheetMetaData;

	/** Defines whether reading is done from the Read Excel operator or from a Wizard */
	private final XlsxReadMode readMode;

	/** The content of the parsed workbook file */
	private final XlsxWorkbook xlsxWorkbook;

	private Operator operator = null;

	private long multiplier;

	private int progressCounter = 0;

	/**
	 * Configures the Excel result set with the provided configuration object. Also parses multiple
	 * XML configuration files included in the XLSX file and creates the worksheet parser.
	 *
	 * @param callingOperator
	 *            the calling operator. <code>null</code> is allowed in case the class isn't created
	 *            from within an operator.
	 * @param configuration
	 *            the result set configuration
	 * @param sheetIndex
	 *            index of the selected sheet
	 * @param readMode
	 *            the current read mode
	 * @param provider
	 *            a {@link DateFormatProvider}, can be {@code null} in which case the date format is
	 *            fixed by the current value of {@link ExcelResultSetConfiguration#getDatePattern() configuration.getDatePattern()}
	 * @throws UserError
	 *             in case something is configured in a wrong way so that the XLSX file cannot be
	 *             parsed
	 */
	public XlsxResultSet(Operator callingOperator, final ExcelResultSetConfiguration configuration, int sheetIndex,
						 XlsxReadMode readMode, final DateFormatProvider provider) throws UserError {
		this(callingOperator, configuration, ExcelSheetSelection.byIndex(sheetIndex), readMode, provider);
	}

	/**
	 * Configures the Excel result set with the provided configuration object. Also parses multiple
	 * XML configuration files included in the XLSX file and creates the worksheet parser.
	 *
	 * @param callingOperator
	 *            the calling operator. <code>null</code> is allowed in case the class isn't created
	 *            from within an operator.
	 * @param configuration
	 *            the result set configuration
	 * @param sheetSelection
	 *            the selected sheet
	 * @param readMode
	 *            current read mode
	 * @param provider
	 *            a {@link DateFormatProvider}, can be {@code null} in which case the date format is
	 *            fixed by the current value of {@link ExcelResultSetConfiguration#getDatePattern() configuration.getDatePattern()}
	 * @throws UserError
	 *             in case something is configured in a wrong way so that the XLSX file cannot be
	 *             parsed
	 */
	public XlsxResultSet(Operator callingOperator, final ExcelResultSetConfiguration configuration, ExcelSheetSelection sheetSelection,
			XlsxReadMode readMode, final DateFormatProvider provider) throws UserError {

		// Check file presence
		if (configuration.getFile() == null) {
			throw new UserError(callingOperator, "file_consumer.no_file_defined");
		}

		try {
			File xlsxFile = configuration.getFile();

			XlsxWorkbookRel workbookRelations;
			try (ZipFile zipFile = new ZipFile(xlsxFile)) {

				// Parse workbook XML which contains a list of sheets with name, rId, sheetId
				xlsxWorkbook = new XlsxWorkbookParser().parseZipEntry(zipFile);

				int sheetIndex = xlsxWorkbook.xlsxWorkbookSheets.indexOf(sheetSelection.selectSheetFrom(xlsxWorkbook.xlsxWorkbookSheets));

				// Parse workbook relations XML which contains the path of shared strings
				// and the mapping of relationship IDs and paths of worksheets
				XlsxWorkbookRelationParser xlsxWorkbookRelHandler = new XlsxWorkbookRelationParser(callingOperator, zipFile,
						xlsxWorkbook.xlsxWorkbookSheets, sheetIndex);
				workbookRelations = xlsxWorkbookRelHandler.parseZipEntry(zipFile);
			}

			this.sheetMetaData = new XlsxSheetMetaDataParser(xlsxFile, workbookRelations.worksheetsPath, XML_STREAM_FACTORY)
					.parseMetaData(callingOperator, configuration, readMode);

			// Check if sheet is empty.
			// Wizards should also be able to show empty sheets so also check if we are running from
			// an operator.
			if (readMode == XlsxReadMode.OPERATOR
					&& (sheetMetaData.getLastColumnIndex() < 0 || sheetMetaData.getLastRowIndex() < 0)) {
				throw new UserError(callingOperator, 404);
			}

			// Do not use encoding from ExcelResultSetConfiguration but always use UTF-8
			// as UTF-8 is default XLSX encoding: https://msdn.microsoft.com/en-us/library/bb507946
			Charset encoding = StandardCharsets.UTF_8;

			// Parse shared strings file (only if it exists)
			String[] sharedStrings = new String[0];
			if (workbookRelations.sharedStringsPath != null) {
				sharedStrings = new XlsxSharedStringsParser(xlsxFile, workbookRelations.sharedStringsPath,
						XML_STREAM_FACTORY).parseSharedStrings(callingOperator, encoding);

			}

			// Parse styles file (only if it exists)
			XlsxNumberFormats numberFormats = null;
			if (workbookRelations.stylesPath != null) {
				numberFormats = new XlsxNumberFormatParser(xlsxFile, workbookRelations.stylesPath, XML_STREAM_FACTORY)
						.parseNumberFormats();
			}

			// initialize worksheet parser
			this.worksheetParser = new XlsxSheetContentParser(xlsxFile, workbookRelations.worksheetsPath, sharedStrings,
					numberFormats, sheetMetaData, XML_STREAM_FACTORY, encoding);
		} catch (IOException | XMLStreamException | ExcelSheetSelection.SheetNotFoundException e) {
			throw new UserError(callingOperator, e, 321, configuration.getFile(), e.getMessage());
		} catch (ParserConfigurationException | SAXException e) {
			throw new UserError(callingOperator, e, 401, e.getMessage());
		}

		this.readMode = readMode;
		this.configuration = configuration;

		final String timezone = configuration.getTimezone();
		if (provider != null) {
			if (timezone != null) {
				this.dateFormatProvider = () -> {
					DateFormat format = provider.geDateFormat();
					format.setTimeZone(TimeZone.getTimeZone(timezone));
					return format;
				};
			} else {
				this.dateFormatProvider = provider;
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
			callingOperator.getProgress().setTotal(100);
			progressCounter = 0;
			multiplier = worksheetParser.getTotalSize() / 100L;
			operator = callingOperator;
		}
	}

	/**
	 * Configures the Excel result set with the provided configuration object. Also parses multiple
	 * XML configuration files included in the XLSX file and creates the worksheet parser.
	 *
	 * @param callingOperator
	 *            the calling operator. <code>null</code> is allowed in case the class isn't created
	 *            from within an operator.
	 * @param configuration
	 *            the result set configuration
	 * @param sheetIndex
	 *            index of the selected sheet
	 * @throws UserError
	 *             in case something is configured in a wrong way so that the XLSX file cannot be
	 *             parsed
	 */
	public XlsxResultSet(Operator callingOperator, ExcelResultSetConfiguration configuration, int sheetIndex,
			XlsxReadMode readMode) throws UserError {
		this(callingOperator, configuration, sheetIndex, readMode, null);
	}

	/**
	 * Configures the Excel result set with the provided configuration object. Also parses multiple
	 * XML configuration files included in the XLSX file and creates the worksheet parser.
	 *
	 * @param callingOperator
	 *            the calling operator. <code>null</code> is allowed in case the class isn't created
	 *            from within an operator.
	 * @param configuration
	 *            the result set configuration
	 * @param sheetSelection
	 *            the selected sheet
	 * @throws UserError
	 *             in case something is configured in a wrong way so that the XLSX file cannot be
	 *             parsed
	 */
	public XlsxResultSet(Operator callingOperator, ExcelResultSetConfiguration configuration, ExcelSheetSelection sheetSelection,
			XlsxReadMode readMode) throws UserError {
		this(callingOperator, configuration, sheetSelection, readMode, null);
	}

	@Override
	public String[] getColumnNames() {
		return sheetMetaData.getColumnNames(configuration.isEmulatingOldNames());
	}

	@Override
	public int getCurrentRow() {
		return worksheetParser.getCurrentRowIndex();
	}

	@Override
	public boolean isMissing(int columnIndex) {
		String value = getValue(columnIndex);
		XlsxCellType cellType = getCellType(columnIndex);
		return value == null || value.trim().isEmpty() || XlsxCellType.ERROR.equals(cellType);
	}

	@Override
	public Date getDate(int columnIndex) throws ParseException {
		String dateValue = getValue(columnIndex);
		if (dateValue == null) {
			return null;
		}
		switch (getCellType(columnIndex)) {
			case NUMBER:
			case DATE:
				// XLSX stores dates as double values
				double dateAsDouble = Double.parseDouble(dateValue);

				// Use POI methods to convert value to Date java object
				if (DateUtil.isValidExcelDate(dateAsDouble)) {
					return DateUtil.getJavaDate(dateAsDouble, xlsxWorkbook.isDate1904);
				} else {
					throw new ParseException(new ParsingError(getCurrentRow() + 1, columnIndex,
							ParsingError.ErrorCode.UNPARSEABLE_DATE, dateValue));
				}
			case INLINE_STRING:
			case SHARED_STRING:
			case STRING:
				// In case a date is stored as String, we try to parse it here
				String dateString = dateValue;
				try {
					return dateFormatProvider.geDateFormat().parse(dateString);
				} catch (java.text.ParseException e) {
					throw new ParseException(new ParsingError(getCurrentRow() + 1, columnIndex,
							ParsingError.ErrorCode.UNPARSEABLE_DATE, dateString));
				}
			default:
				throw new ParseException(new ParsingError(getCurrentRow() + 1, columnIndex,
						ParsingError.ErrorCode.UNPARSEABLE_DATE, dateValue));

		}
	}

	@Override
	public ValueType getNativeValueType(int columnIndex) throws ParseException {
		XlsxCellType cellType = getCellType(columnIndex);
		if (cellType == null) {
			return ValueType.EMPTY;
		}
		switch (cellType) {
			case DATE:
				return ValueType.DATE;
			case ERROR:
				return ValueType.EMPTY;
			case NUMBER:
				return ValueType.NUMBER;
			case BOOLEAN:
			case INLINE_STRING:
			case SHARED_STRING:
			case STRING:
			default:
				return ValueType.STRING;
		}
	}

	@Override
	public Number getNumber(int columnIndex) throws ParseException {
		String numberValue = getValue(columnIndex);
		if (numberValue == null) {
			return null;
		}
		try {
			return Double.valueOf(numberValue);
		} catch (NumberFormatException e) {
			throw new ParseException(new ParsingError(getCurrentRow() + 1, columnIndex,
					ParsingError.ErrorCode.UNPARSEABLE_REAL, numberValue));
		}
	}

	/**
	 * @param columnIndex
	 *            the index of the column
	 * @return the value as String
	 */
	private String getValue(int columnIndex) {
		XlsxCell xlsxCell = getXlsxCell(columnIndex);
		return xlsxCell == null ? null : xlsxCell.value;
	}

	/**
	 * @return the {@link XlsxCell} for the specified column
	 */
	private XlsxCell getXlsxCell(int columnIndex) {
		XlsxCell[] rowContent = worksheetParser.getRowContent();
		if (rowContent != null) {
			return rowContent[columnIndex];
		}
		return null;
	}

	/**
	 * @param columnIndex
	 *            the index of the cell in the current row
	 * @return the {@link XlsxCellType} of the cell or <code>null</code> if the cell is empty
	 */
	private XlsxCellType getCellType(int columnIndex) {
		XlsxCell xlsxCell = getXlsxCell(columnIndex);
		return xlsxCell == null ? null : xlsxCell.cellType;
	}

	@Override
	public int getNumberOfColumns() {
		return sheetMetaData.getNumberOfColumns();
	}

	@Override
	public String getString(int columnIndex) throws ParseException {
		String value = getValue(columnIndex);
		XlsxCellType cellType = getCellType(columnIndex);
		if (cellType == null || value == null) {
			return null;
		}
		switch (cellType) {
			case NUMBER:
				return decimalFormat.format(new BigDecimal(value));
			case BOOLEAN:
				if (Integer.parseInt(value) == 1) {
					return BOOLEAN_TRUE;
				} else {
					return BOOLEAN_FALSE;
				}
			default:
				return value;
		}
	}

	@Override
	public int[] getValueTypes() {
		// return an array full of zeros as we cannot determine the value types of each columns here
		return new int[getNumberOfColumns()];
	}

	/**
	 * @return the number of all rows available by the specified worksheet (also includes empty rows
	 *         at the end of the file) or -1 if number is unknown
	 */
	public int getNumberOfRows() {
		return sheetMetaData.getNumberOfRows();
	}

	/**
	 * @param columnMetaDatas
	 *            meta data defined by the operator
	 * @return all names of columns that have been found empty after parsing the XLSX file
	 */
	public List<String> getEmptyColumnNames(ColumnMetaData[] columnMetaDatas) {
		List<String> toRemove = new LinkedList<>();
		String[] columnNames = getColumnNames();
		boolean[] emptyColumns = worksheetParser.getEmptyColumns();

		// For all columns a meta data was defined so do not remove any column
		if (columnNames.length == columnMetaDatas.length) {
			return Collections.emptyList();
		}

		// We can safely assume that empty columns will always have their Excel column name
		for (int i = 0; i < emptyColumns.length; i++) {
			boolean empty = emptyColumns[i];
			ColumnMetaData cmd = null;
			if (i < columnMetaDatas.length) {
				cmd = columnMetaDatas[i];
			}
			if (empty && cmd == null) {
				toRemove.add(columnNames[i]);
			}
		}
		return toRemove;
	}

	@Override
	public boolean hasNext() {
		return worksheetParser.hasNext();
	}

	@Override
	public void next(ProgressListener listener) throws OperatorException {
		try {
			worksheetParser.next(readMode);
		} catch (XMLStreamException | ParseException e) {
			throw new UserError(null, e, 321, configuration.getFile(), e.getMessage());
		}

		if (listener != null) {
			listener.setCompleted(getCurrentRow());
		}

		if (operator != null && ++progressCounter % 100 == 0) {
			try {
				operator.getProgress().setCompleted((int) (worksheetParser.getCurrentPosition() / multiplier));
			} catch (ProcessStoppedException e) {
				// Will not happen, because check for stop is deactivated
			}
		}
	}

	@Override
	public void reset(ProgressListener listener) throws OperatorException {
		try {
			worksheetParser.reset(XML_STREAM_FACTORY);
		} catch (IOException | XMLStreamException e) {
			throw new UserError(null, e, 321, configuration.getFile(), e.getMessage());
		}

		// Update listener
		if (listener != null) {
			int numberOfRows = getNumberOfRows();
			listener.setTotal(numberOfRows == -1 ? XlsxSheetMetaDataParser.MAXIMUM_XLSX_ROW_INDEX + 1 : numberOfRows);
			listener.setCompleted(0);
		}

		// Update progress
		if (operator != null) {
			operator.getProgress().setTotal(100);
			progressCounter = 0;
			multiplier = worksheetParser.getTotalSize() / 100L;
		}
	}

	@Override
	public void close() {
		try {
			if (worksheetParser != null) {
				worksheetParser.close();
			}
		} catch (XMLStreamException | IOException e) {
			LogService.getRoot().log(Level.WARNING,
					I18N.getMessage(LogService.getRoot().getResourceBundle(),
							"com.rapidminer.operator.nio.model.ExcelResultSetConfiguration.close_workbook_error",
							e.getMessage()),
					e);
		}
	}

	/**
	 * @param isFirstRowAsNames
	 *            defines whether the first row should be used as names. If set to <code>true</code>
	 *            the worksheet parser will skip all beginning empty rows until the first row with
	 *            content was found.
	 */
	public void setUseFirstRowAsNames(boolean isFirstRowAsNames) {
		this.worksheetParser.setUseFirstRowAsNames(isFirstRowAsNames);
	}

}
