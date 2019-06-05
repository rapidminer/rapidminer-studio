/**
 * Copyright (C) 2001-2019 by RapidMiner and the contributors
 *
 * Complete list of developers available at our web site:
 *
 * http://rapidminer.com
 *
 * This program is free software: you can redistribute it and/or modify it under the terms of the GNU Affero General
 * Public License as published by the Free Software Foundation, either version 3 of the License, or (at your option) any
 * later version.
 *
 * This program is distributed in the hope that it will be useful, but WITHOUT ANY WARRANTY; without even the implied
 * warranty of MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the GNU Affero General Public License for more
 * details.
 *
 * You should have received a copy of the GNU Affero General Public License along with this program. If not, see
 * http://www.gnu.org/licenses/.
 */
package com.rapidminer.operator.io;

import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.OutputStream;
import java.nio.charset.Charset;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.Collections;
import java.util.Date;
import java.util.HashSet;
import java.util.Iterator;
import java.util.LinkedList;
import java.util.List;
import java.util.Locale;
import java.util.Set;

import org.apache.commons.lang.ArrayUtils;
import org.apache.poi.ss.usermodel.Cell;
import org.apache.poi.ss.usermodel.CellStyle;
import org.apache.poi.ss.usermodel.CreationHelper;
import org.apache.poi.ss.usermodel.Font;
import org.apache.poi.ss.usermodel.Row;
import org.apache.poi.ss.usermodel.Sheet;
import org.apache.poi.ss.util.WorkbookUtil;
import org.apache.poi.xssf.streaming.SXSSFWorkbook;

import com.rapidminer.example.Attribute;
import com.rapidminer.example.Example;
import com.rapidminer.example.ExampleSet;
import com.rapidminer.operator.MissingIOObjectException;
import com.rapidminer.operator.Operator;
import com.rapidminer.operator.OperatorDescription;
import com.rapidminer.operator.OperatorException;
import com.rapidminer.operator.OperatorVersion;
import com.rapidminer.operator.ProcessSetupError;
import com.rapidminer.operator.ProcessStoppedException;
import com.rapidminer.operator.SimpleProcessSetupError;
import com.rapidminer.operator.UserError;
import com.rapidminer.operator.nio.file.FileObject;
import com.rapidminer.operator.nio.file.FileOutputPortHandler;
import com.rapidminer.operator.ports.InputPort;
import com.rapidminer.operator.ports.OutputPort;
import com.rapidminer.operator.ports.PortPairExtender;
import com.rapidminer.operator.ports.metadata.ExampleSetMetaData;
import com.rapidminer.operator.ports.metadata.InputMissingMetaDataError;
import com.rapidminer.parameter.ParameterType;
import com.rapidminer.parameter.ParameterTypeCategory;
import com.rapidminer.parameter.ParameterTypeDateFormat;
import com.rapidminer.parameter.ParameterTypeEnumeration;
import com.rapidminer.parameter.ParameterTypeString;
import com.rapidminer.parameter.UndefinedParameterError;
import com.rapidminer.parameter.conditions.BelowOrEqualOperatorVersionCondition;
import com.rapidminer.parameter.conditions.EqualTypeCondition;
import com.rapidminer.tools.I18N;
import com.rapidminer.tools.Ontology;
import com.rapidminer.tools.ValidationUtil;
import com.rapidminer.tools.io.Encoding;

import jxl.Workbook;
import jxl.WorkbookSettings;
import jxl.write.DateFormat;
import jxl.write.DateTime;
import jxl.write.Label;
import jxl.write.Number;
import jxl.write.NumberFormat;
import jxl.write.WritableCellFormat;
import jxl.write.WritableFont;
import jxl.write.WritableSheet;
import jxl.write.WritableWorkbook;
import jxl.write.WriteException;


/**
 * <p>
 * This operator can be used to write data into Microsoft Excel spreadsheets. This operator creates Excel files readable
 * by Excel 95, 97, 2000, XP, 2003 and newer. Missing data values are indicated by empty cells.
 * </p>
 *
 * @author Ingo Mierswa, Nils Woehler
 */
public class ExcelExampleSetWriter extends Operator {

	/**
	 * Versions > this version do not need the sheet name parameter anymore. It has been replaced by the sheet names
	 * parameter which can handle multiple sheet names.
	 */
	private static final OperatorVersion VERSION_WITH_SHEET_NAME_PARAMETER = new OperatorVersion(9, 2, 1);

	private static final String RAPID_MINER_DATA = "RapidMiner Data";

	/**
	 * The parameter name for &quot;The Excel spreadsheet file which should be written.&quot;
	 */
	public static final String PARAMETER_EXCEL_FILE = "excel_file";

	public static final String FILE_FORMAT_XLS = "xls";
	public static final String FILE_FORMAT_XLSX = "xlsx";
	public static final String[] FILE_FORMAT_CATEGORIES = new String[]{FILE_FORMAT_XLS, FILE_FORMAT_XLSX};
	public static final int FILE_FORMAT_XLS_INDEX = 0;
	public static final int FILE_FORMAT_XLSX_INDEX = 1;

	public static final String PARAMETER_FILE_FORMAT = "file_format";
	public static final String PARAMETER_NUMBER_FORMAT = "number_format";
	/**
	 * Only there for compatibility to older versions. Has been replaced by {@link ExcelExampleSetWriter#PARAMETER_SHEET_NAMES}.
	 */
	public static final String PARAMETER_SHEET_NAME = "sheet_name";
	public static final String PARAMETER_SHEET_NAME_DESCRIPTION = "The name of the first sheet. Note that sheet names " +
			"in Excel must be unique and must not exceed 31 characters.";
	public static final String PARAMETER_SHEET_NAMES = "sheet_names";
	public static final String PARAMETER_SHEET_NAME_ELEMENT = "sheet_name_element";
	public static final String PARAMETER_OTHER_SHEET_NAMES_DESCRIPTION = "Sheet names can be specified here. " +
			"Note that sheet names in Excel must be unique and must not exceed 31 characters.";

	public static final String DEFAULT_DATE_FORMAT = ParameterTypeDateFormat.DATE_TIME_FORMAT_YYYY_MM_DD_HH_MM_SS;
	public static final String DEFAULT_NUMBER_FORMAT = "#.0";
	public static final int XLSX_MAX_COLUMNS = 16384;
	public static final int XLS_MAX_COLUMNS = 256;

	/**
	 * the limit of an excel cell, see the <a href= "https://support.office.com/en-gb/article/Excel-specifications-and-limits-16c69c74-3d6a-4aaf-ba35-e6eb276e8eaa">
	 * Microsoft limit documentation</a> for more information.
	 */
	public static final int CHARACTER_CELL_LIMIT = 32_767;
	public static final String CROP_INDICATOR = "[...]";

	public static final String INPUT_PORT_PREFIX = "input";
	public static final String OUTPUT_PORT_PREFIX = "through";

	/**
	 * Excel sheet names must not contain more than 31 characters.
	 */
	public static final int MAX_SHEET_NAME_LENGTH = 31;

	protected OutputPort fileOutputPort = getOutputPorts().createPort("file");
	protected FileOutputPortHandler filePortHandler;

	protected final PortPairExtender portExtender = new PortPairExtender(INPUT_PORT_PREFIX, OUTPUT_PORT_PREFIX, getInputPorts(), getOutputPorts(),
			new ExampleSetMetaData(), true) {
		/**
		 * Does the same as the original fixNames but replaces the first input/output ports' original names for
		 * compatibility to older versions.
		 */
		@Override
		protected void fixNames() {
			super.fixNames();
			// rename ports for compatibility
			getManagedPairs().stream().findFirst().ifPresent(pair -> {
				getInputPorts().renamePort(pair.getInputPort(), INPUT_PORT_PREFIX);
				getOutputPorts().renamePort(pair.getOutputPort(), OUTPUT_PORT_PREFIX);
					}
			);
		}
	};

	/**
	 * Creates a new ExcelExampleSetWriter with the given description.
	 *
	 * @param description
	 */
	public ExcelExampleSetWriter(OperatorDescription description) {
		super(description);
		filePortHandler = new FileOutputPortHandler(this, fileOutputPort, getFileParameterName());
		getTransformer().addGenerationRule(fileOutputPort, FileObject.class);
		portExtender.start();
		portExtender.ensureMinimumNumberOfPorts(1);
		getTransformer().addRule(portExtender.makePassThroughRule());
	}

	@Override
	public void doWork() throws OperatorException {
		OutputStream outputStream = null;
		try (OutputStream os = filePortHandler.openSelectedFile()) {
			outputStream = os;
			writeStream(outputStream);
		} catch (IOException e) {
				if (outputStream instanceof FileOutputStream) {
					throw new UserError(this, e, 322, getParameterAsFile(getFileParameterName()), "");
				} else if (outputStream instanceof ByteArrayOutputStream) {
					throw new UserError(this, e, 322, "output stream", "");
				} else {
					throw new UserError(this, e, 322, "unknown file or stream", "");
				}
		}
	}

	@Override
	public OperatorVersion[] getIncompatibleVersionChanges() {
		return (OperatorVersion[]) ArrayUtils.add(super.getIncompatibleVersionChanges(),
				VERSION_WITH_SHEET_NAME_PARAMETER);
	}

	@Override
	public List<ParameterType> getParameterTypes() {
		List<ParameterType> types = super.getParameterTypes();
		ParameterType type = makeFileParameterType();
		type.setPrimary(true);
		types.add(type);

		types.add(new ParameterTypeCategory(PARAMETER_FILE_FORMAT,
				"Defines the file format the excel file should be saved as.", FILE_FORMAT_CATEGORIES, FILE_FORMAT_XLSX_INDEX,
				false));

		types.add(new ParameterTypeEnumeration(PARAMETER_SHEET_NAMES, PARAMETER_OTHER_SHEET_NAMES_DESCRIPTION,
				new ParameterTypeString(PARAMETER_SHEET_NAME_ELEMENT, PARAMETER_SHEET_NAME_DESCRIPTION), false));

		List<ParameterType> xlsxTypes = new LinkedList<>();

		ParameterTypeString sheetNameType = new ParameterTypeString(PARAMETER_SHEET_NAME,
				PARAMETER_SHEET_NAME_DESCRIPTION, RAPID_MINER_DATA);
		sheetNameType.setExpert(false);
		// the sheet name parameter has been replaced by the sheet names parameter in newer versions
		sheetNameType.registerDependencyCondition(new BelowOrEqualOperatorVersionCondition(this,
				VERSION_WITH_SHEET_NAME_PARAMETER));
		xlsxTypes.add(sheetNameType);

		ParameterType dateType = new ParameterTypeDateFormat();
		dateType.setDefaultValue(DEFAULT_DATE_FORMAT);
		dateType.setExpert(true);
		xlsxTypes.add(dateType);
		xlsxTypes.add(new ParameterTypeString(PARAMETER_NUMBER_FORMAT,
				"Specifies the number format for date entries. Default: \"#.0\"", DEFAULT_NUMBER_FORMAT, true));
		for (ParameterType xlsxType : xlsxTypes) {
			xlsxType.registerDependencyCondition(new EqualTypeCondition(this, PARAMETER_FILE_FORMAT, FILE_FORMAT_CATEGORIES,
					false, FILE_FORMAT_XLSX_INDEX));
		}
		types.addAll(xlsxTypes);

		List<ParameterType> encodingTypes = Encoding.getParameterTypes(this);
		for (ParameterType encodingType : encodingTypes) {
			encodingType.registerDependencyCondition(new EqualTypeCondition(this, PARAMETER_FILE_FORMAT, FILE_FORMAT_CATEGORIES,
					false, FILE_FORMAT_XLS_INDEX));
		}
		types.addAll(encodingTypes);

		return types;
	}

	@Override
	protected void performAdditionalChecks() {
		super.performAdditionalChecks();
		// check if any input port is connected
		if (portExtender.getManagedPairs().stream().map(PortPairExtender.PortPair::getInputPort).
				noneMatch(InputPort::isConnected)) {
			// we expect at least one connected input port
			portExtender.getManagedPairs().stream().findFirst().ifPresent(portPair -> addError(
					new InputMissingMetaDataError(portPair.getInputPort(), ExampleSet.class)));
		}
		// check if the specified sheet names are valid
		try {
			validateSheetNames(getSheetNames());
		} catch (UserError userError) {
			addError(new SimpleProcessSetupError(ProcessSetupError.Severity.ERROR, getPortOwner(), Collections.emptyList(), true, userError.getMessage()));
		}
	}

	protected String[] getFileExtensions() {
		return new String[]{FILE_FORMAT_XLSX, FILE_FORMAT_XLS};
	}

	protected String getFileParameterName() {
		return PARAMETER_EXCEL_FILE;
	}

	/**
	 * Creates (but does not add) the file parameter named by {@link #getFileParameterName()} that depends on whether or
	 * not {@link #fileOutputPort} is connected.
	 */
	protected ParameterType makeFileParameterType() {
		return FileOutputPortHandler.makeFileParameterType(this, getFileParameterName(), () -> fileOutputPort, getFileExtensions());
	}

	protected void writeStream(OutputStream outputStream) throws OperatorException {
		portExtender.passDataThrough();
		File file = getParameterAsFile(PARAMETER_EXCEL_FILE, true);
		List<ExampleSet> exampleSets = new ArrayList<>(portExtender.getData(ExampleSet.class, true));
		if (exampleSets.isEmpty()) {
			throw new MissingIOObjectException(ExampleSet.class);
		}
		String[] sheetNames = getSheetNames();
		validateSheetNames(sheetNames);

		if (getParameterAsString(PARAMETER_FILE_FORMAT).equals(FILE_FORMAT_XLSX)) {
			// check if date format is valid
			ParameterTypeDateFormat.createCheckedDateFormat(this, null);
			String dateFormat = isParameterSet(ParameterTypeDateFormat.PARAMETER_DATE_FORMAT) ? getParameterAsString(ParameterTypeDateFormat.PARAMETER_DATE_FORMAT) : null;
			String numberFormat = isParameterSet(PARAMETER_NUMBER_FORMAT) ? getParameterAsString(PARAMETER_NUMBER_FORMAT)
					: null;
			try {
				writeXLSX(exampleSets, Arrays.asList(sheetNames), dateFormat, numberFormat, outputStream, this);
			} catch (IOException e) {
				throw new UserError(this, 303, file.getName(), e.getMessage());
			}

		} else {
			Charset encoding = Encoding.getEncoding(this);
			try {
				write(exampleSets, Arrays.asList(sheetNames), encoding, outputStream, this);
			} catch (WriteException | IOException e) {
				throw new UserError(this, 303, file.getName(), e.getMessage());
			}
		}
	}

	/**
	 * Helper method that collects the specified sheet names considering the chosen file format and compatibility
	 * level.
	 *
	 * @return array containing the user specified sheet names for the current settings
	 * @throws UndefinedParameterError
	 */
	private String[] getSheetNames() throws UndefinedParameterError {
		String[] sheetNames = ParameterTypeEnumeration
				.transformString2Enumeration(getParameterAsString(PARAMETER_SHEET_NAMES));
		if (getCompatibilityLevel().isAtMost(VERSION_WITH_SHEET_NAME_PARAMETER)) {
			String sheetName = null;
			if (getParameterAsString(PARAMETER_FILE_FORMAT).equals(FILE_FORMAT_XLSX)) {
				// for older versions and xlsx file type we need to take the sheet name parameter into account
				sheetName = getParameterAsString(PARAMETER_SHEET_NAME);
			}
			if (sheetName == null) {
				// for file type xls or if no sheet name has been specified fall back to default sheet name
				sheetName = RAPID_MINER_DATA;
			}
			sheetNames = (String[]) ArrayUtils.add(sheetNames, 0, sheetName);
		}
		return sheetNames;
	}

	/**
	 * Throws a UserError if any of the given sheet names are too long or if there are duplicate sheet names.
	 *
	 * @param sheetNames
	 * 		the sheet names to validate
	 * @throws UserError
	 */
	private void validateSheetNames(String[] sheetNames) throws UserError {
		Set<String> uniqueNames = new HashSet<>();
		for (String name : sheetNames) {
			if (name.length() > MAX_SHEET_NAME_LENGTH) {
				throw new UserError(this, "excel_sheet_name_too_long", name, name.length());
			}
			if (!uniqueNames.add(name)) {
				throw new UserError(this, "excel_sheet_name_duplicate", name);
			}
		}
	}

	/**
	 * Writes the example set into an excel file with XLS format. If you want to write it in XLSX format use {@link
	 * #writeXLSX(ExampleSet, String, String, String, OutputStream, Operator)}
	 *
	 * @param exampleSet
	 * 		the exampleSet to write.
	 * @param encoding
	 * 		the Charset to use for the file.
	 * @param out
	 * 		the stream to use.
	 * @deprecated please use {@link ExcelExampleSetWriter#write(ExampleSet, Charset, OutputStream, Operator)} to
	 * support checkForStop.
	 */
	@Deprecated
	public static void write(ExampleSet exampleSet, Charset encoding, OutputStream out) throws IOException, WriteException {
		try {
			write(exampleSet, encoding, out, null);
		} catch (ProcessStoppedException e) {
			// can not happen because we do not deliver an Operator
		}
	}

	/**
	 * Writes the example set into an excel file with XLS format. If you want to write it in XLSX format use {@link
	 * #writeXLSX(ExampleSet, String, String, String, OutputStream)}
	 *
	 * @param exampleSet
	 * 		the exampleSet to write.
	 * @param encoding
	 * 		the Charset to use for the file.
	 * @param out
	 * 		the stream to use.
	 * @param op
	 * 		will be used to provide checkForStop.
	 */
	public static void write(ExampleSet exampleSet, Charset encoding, OutputStream out, Operator op)
			throws IOException, WriteException, ProcessStoppedException {
		write(Collections.singletonList(exampleSet), Collections.singletonList(RAPID_MINER_DATA), encoding, out, op);
	}

	/**
	 * Writes the list of example sets into an excel file with XLS format. Every example set will occupy a sheet in the
	 * resulting excel file. Sheets will be named after their corresponding example sets. Optionally sheet names can
	 * also be specified via the sheetNames string array. If you want to write it in XLSX format use {@link
	 * #writeXLSX(List, List, String, String, OutputStream, Operator)}
	 *
	 * @param exampleSets
	 * 		every example set in this list will be written into one sheet of the excel file
	 * @param sheetNames
	 * 		optional sheet names (the order defines which name corresponds to which example set)
	 * @param encoding
	 * 		the Charset to use for the file.
	 * @param out
	 * 		the stream to use.
	 * @param op
	 * 		will be used to provide checkForStop.
	 * @throws IOException
	 * @throws WriteException
	 * @throws ProcessStoppedException
	 */
	public static void write(List<ExampleSet> exampleSets, List<String> sheetNames, Charset encoding, OutputStream out, Operator op)
			throws IOException, WriteException, ProcessStoppedException {
		try {
			// .xls files can only store up to XLS_MAX_COLUMNS columns, so throw error in case of more
			if (exampleSets.stream().anyMatch(e -> e.getAttributes().allSize() > XLS_MAX_COLUMNS)) {
				throw new IllegalArgumentException(
						I18N.getMessage(I18N.getErrorBundle(), "export.excel.excel_xls_file_exceeds_column_limit"));
			}

			WorkbookSettings ws = new WorkbookSettings();
			ws.setEncoding(encoding.name());
			ws.setLocale(Locale.US);
			WritableWorkbook workbook = Workbook.createWorkbook(out, ws);

			// write one sheet per example set
			int index = 0;
			Iterator<String> nameIt = sheetNames.iterator();
			Set<String> usedNames = new HashSet<>();
			for (ExampleSet e : exampleSets) {
				String sheetName = createSheetName(nameIt, usedNames, e);
				WritableSheet sheet = workbook.createSheet(sheetName, index);
				writeDataSheet(sheet, e, op);
				index++;
			}
			workbook.write();
			workbook.close();
		} finally {
			try {
				out.close();
			} catch (Exception e) {
				// silent. exception will trigger warning anyway
			}
		}
	}

	/**
	 * Writes the example set into a excel file with XLSX format. If you want to write it in XLS format use {@link
	 * #write(ExampleSet, Charset, OutputStream)}.
	 *
	 * @param exampleSet
	 * 		the exampleSet to write
	 * @param sheetName
	 * 		name of the excel sheet which will be created.
	 * @param dateFormat
	 * 		a string which describes the format used for dates.
	 * @param numberFormat
	 * 		a string which describes the format used for numbers.
	 * @param outputStream
	 * 		the stream to write the file to
	 * @deprecated please use {@link ExcelExampleSetWriter#writeXLSX(ExampleSet, String, String, String, OutputStream,
	 * Operator)} to support checkForStop.
	 */
	@Deprecated
	public static void writeXLSX(ExampleSet exampleSet, String sheetName, String dateFormat, String numberFormat,
								 OutputStream outputStream) throws IOException {
		try {
			writeXLSX(exampleSet, sheetName, dateFormat, numberFormat, outputStream, null);
		} catch (ProcessStoppedException e) {
			// can not happen because we provide no Operator
		}
	}

	/**
	 * Writes the example set into a excel file with XLSX format. If you want to write it in XLS format use {@link
	 * #write(ExampleSet, Charset, OutputStream)}.
	 *
	 * @param exampleSet
	 * 		the exampleSet to write
	 * @param sheetName
	 * 		name of the excel sheet which will be created.
	 * @param dateFormat
	 * 		a string which describes the format used for dates.
	 * @param numberFormat
	 * 		a string which describes the format used for numbers.
	 * @param outputStream
	 * 		the stream to write the file to
	 * @param op
	 * 		needed for checkForStop
	 */
	public static void writeXLSX(ExampleSet exampleSet, String sheetName, String dateFormat, String numberFormat,
								 OutputStream outputStream, Operator op) throws IOException, ProcessStoppedException {
		writeXLSX(Collections.singletonList(exampleSet), Collections.singletonList(sheetName), dateFormat, numberFormat, outputStream, op);
	}

	/**
	 * Writes the list of example sets into an excel file with XLSX format. Every example set will occupy a sheet in the
	 * resulting excel file. Sheets will be named after their corresponding example sets. Optionally sheet names can
	 * also be specified via the sheetNames string array. If you want to write it in XLS format use {@link #write(List,
	 * List, Charset, OutputStream, Operator)}
	 *
	 * @param exampleSets
	 * 		every example set in this list will be written into one sheet of the excel file.
	 * @param sheetNames
	 * 		optional sheet names (the order defines which name corresponds to which example set)
	 * @param dateFormat
	 * 		a string which describes the format used for dates.
	 * @param numberFormat
	 * 		a string which describes the format used for numbers.
	 * @param outputStream
	 * 		the stream to use.
	 * @param op
	 * 		will be used to provide checkForStop.
	 * @throws IOException
	 * @throws ProcessStoppedException
	 */
	public static void writeXLSX(List<ExampleSet> exampleSets, List<String> sheetNames, String dateFormat, String numberFormat,
								 OutputStream outputStream, Operator op) throws IOException, ProcessStoppedException {
		// .xlsx files can only store up to XLSX_MAX_COLUMNS columns, so throw error in case of more
		if (exampleSets.stream().anyMatch(e -> e.getAttributes().allSize() > XLSX_MAX_COLUMNS)) {
			throw new IllegalArgumentException(
					I18N.getMessage(I18N.getErrorBundle(), "export.excel.excel_xlsx_file_exceeds_column_limit"));
		}

		try (SXSSFWorkbook workbook = new SXSSFWorkbook(null, SXSSFWorkbook.DEFAULT_WINDOW_SIZE, false, true)) {
			dateFormat = dateFormat == null ? DEFAULT_DATE_FORMAT : dateFormat;
			numberFormat = numberFormat == null ? "#.0" : numberFormat;

			// header font
			Font headerFont = workbook.createFont();
			headerFont.setBold(true);
			CellStyle headerStyle = workbook.createCellStyle();
			headerStyle.setFont(headerFont);

			// body font
			Font bodyFont = workbook.createFont();
			bodyFont.setBold(false);
			CreationHelper createHelper = workbook.getCreationHelper();

			// number format
			CellStyle numericalStyle = workbook.createCellStyle();
			numericalStyle.setDataFormat(createHelper.createDataFormat().getFormat(numberFormat));
			numericalStyle.setFont(bodyFont);

			// date format
			CellStyle dateStyle = workbook.createCellStyle();
			dateStyle.setDataFormat(createHelper.createDataFormat().getFormat(dateFormat));
			dateStyle.setFont(bodyFont);

			// create nominal cell style
			CellStyle nominalStyle = workbook.createCellStyle();
			nominalStyle.setFont(bodyFont);

			// write one sheet per example set
			Iterator<String> nameIt = sheetNames.iterator();
			Set<String> usedNames = new HashSet<>();
			for (ExampleSet e : exampleSets) {
				String sheetName = createSheetName(nameIt, usedNames, e);
				// create a new sheet for the current example set
				Sheet sheet = workbook.createSheet(sheetName);
				writeXLSXDataSheet(sheet, e, op, headerStyle, dateStyle, numericalStyle, nominalStyle);
			}
			workbook.write(outputStream);
		} finally {
			outputStream.flush();
			outputStream.close();
		}
	}

	/**
	 * Writes the provided {@link ExampleSet} to a XLSX formatted data sheet.
	 *
	 * @param sheet
	 * 		the excel sheet to write to.
	 * @param exampleSet
	 * 		the exampleSet to write
	 * @param op
	 * 		needed for checkForStop
	 * @param headerStyle
	 * 		the style used for headers
	 * @param dateStyle
	 * 		the style used for dates
	 * @param numericalStyle
	 * 		the style used for numericals
	 * @param nominalStyle
	 * 		the style used for nominals
	 * @throws ProcessStoppedException
	 * 		if the process was stopped by the user.
	 * @throws WriteException
	 */
	private static void writeXLSXDataSheet(Sheet sheet, ExampleSet exampleSet, Operator op,
										   CellStyle headerStyle, CellStyle dateStyle, CellStyle numericalStyle,
										   CellStyle nominalStyle) throws ProcessStoppedException {
		// create the header
		Iterator<Attribute> a = exampleSet.getAttributes().allAttributes();
		int columnCounter = 0;
		int rowCounter = 0;
		Row headerRow = sheet.createRow(rowCounter);
		while (a.hasNext()) {
			Attribute attribute = a.next();
			Cell headerCell = headerRow.createCell(columnCounter);
			headerCell.setCellValue(attribute.getName());
			headerCell.setCellStyle(headerStyle);
			columnCounter++;
		}
		rowCounter++;

		// fill body
		for (Example example : exampleSet) {

			// create new row
			Row bodyRow = sheet.createRow(rowCounter);

			// iterate over attributes and save examples
			a = exampleSet.getAttributes().allAttributes();
			columnCounter = 0;
			while (a.hasNext()) {
				Attribute attribute = a.next();
				Cell currentCell = bodyRow.createCell(columnCounter);
				if (!Double.isNaN(example.getValue(attribute))) {
					if (Ontology.ATTRIBUTE_VALUE_TYPE.isA(attribute.getValueType(), Ontology.DATE_TIME)) {
						Date dateValue = example.getDateValue(attribute);
						currentCell.setCellValue(dateValue);
						currentCell.setCellStyle(dateStyle);
					} else if (Ontology.ATTRIBUTE_VALUE_TYPE.isA(attribute.getValueType(), Ontology.NUMERICAL)) {
						double numericalValue = example.getNumericalValue(attribute);
						currentCell.setCellValue(numericalValue);
						currentCell.setCellStyle(numericalStyle);
					} else {
						currentCell
								.setCellValue(stripIfNecessary(replaceForbiddenChars(example.getValueAsString(attribute))));
						currentCell.setCellStyle(nominalStyle);
					}
				}
				columnCounter++;
			}
			rowCounter++;

			// checkForStop every 100 examples
			if (op != null && rowCounter % 100 == 0) {
				op.checkForStop();
			}
		}
	}

	/**
	 * Checks if the given value length is greater than the allowed Excel cell limit ( {@value #CHARACTER_CELL_LIMIT}).
	 * If it exceeds the limit the string will be stripped.
	 *
	 * @param value
	 * 		the string value which should be checked
	 * @return the original string if the character limit is not exceeded, otherwise a stripped one
	 */
	private static String stripIfNecessary(String value) {
		if (value.length() > CHARACTER_CELL_LIMIT) {
			return value.substring(0, CHARACTER_CELL_LIMIT - CROP_INDICATOR.length()) + CROP_INDICATOR;
		} else {
			return value;
		}
	}

	/**
	 * Helper method that is used to shorten the sheet name in case it has become to long after calling {@link
	 * ValidationUtil#getNewName(Collection, String)} because of the index that this method might add to the name.
	 */
	private static String shortenSheetName(String originalName) {
		int length = originalName.length();
		if (length > MAX_SHEET_NAME_LENGTH) {
			// originalName is of format "name (index)". We want to shorten the name
			// but preserve the index
			int diff = length - MAX_SHEET_NAME_LENGTH;
			int indexStart = originalName.lastIndexOf(" (");
			String shortenedName = originalName.substring(0, indexStart - diff);
			String index = originalName.substring(indexStart, length);
			return shortenedName + index;
		} else {
			return originalName;
		}
	}

	/**
	 * Creates a sheet name and makes sure that the name is valid and that there are not duplicates.
	 */
	private static String createSheetName(Iterator<String> nameIt, Set<String> usedNames, ExampleSet e) {
		// the default is to use the source's name
		String sheetName = e.getSource();
		if (nameIt.hasNext()) {
			// if the user has specified a name we will use that instead
			sheetName = nameIt.next();
		}
		// the following code makes sure sheet name is valid and there are no duplicates
		sheetName = WorkbookUtil.createSafeSheetName(sheetName);
		sheetName = ValidationUtil.getNewName(usedNames, sheetName, false);
		String shortenedName = shortenSheetName(sheetName);
		// If shortened name == sheetName we know that the name is still unique.
		// Otherwise we need to repeat the procedure to make sure.
		while (!shortenedName.equals(sheetName)) {
			sheetName = shortenedName;
			sheetName = ValidationUtil.getNewName(usedNames, sheetName, false);
			shortenedName = shortenSheetName(sheetName);
		}
		usedNames.add(sheetName);
		return sheetName;
	}

	/**
	 * Writes the provided {@link ExampleSet} to the {@link WritableSheet}.
	 *
	 * @param s
	 * 		the DataSheet to be filled
	 * @param exampleSet
	 * 		the data to write
	 * @param op
	 * 		an {@link Operator} of the executing operator to checkForStop
	 * @throws WriteException
	 * @throws ProcessStoppedException
	 */
	private static void writeDataSheet(WritableSheet s, ExampleSet exampleSet, Operator op)
			throws WriteException, ProcessStoppedException {

		// Format the Font
		WritableFont wf = new WritableFont(WritableFont.ARIAL, 10, WritableFont.BOLD);
		WritableCellFormat cf = new WritableCellFormat(wf);

		Iterator<Attribute> a = exampleSet.getAttributes().allAttributes();
		int counter = 0;
		while (a.hasNext()) {
			Attribute attribute = a.next();
			s.addCell(new Label(counter++, 0, attribute.getName(), cf));
		}

		NumberFormat nf = new NumberFormat(DEFAULT_NUMBER_FORMAT);
		WritableCellFormat nfCell = new WritableCellFormat(nf);
		WritableFont wf2 = new WritableFont(WritableFont.ARIAL, 10, WritableFont.NO_BOLD);
		WritableCellFormat cf2 = new WritableCellFormat(wf2);

		DateFormat df = new DateFormat(ParameterTypeDateFormat.DATE_TIME_FORMAT_YYYY_MM_DD_HH_MM_SS);

		WritableCellFormat dfCell = new WritableCellFormat(df);
		int rowCounter = 1;
		for (Example example : exampleSet) {
			a = exampleSet.getAttributes().allAttributes();
			int columnCounter = 0;
			while (a.hasNext()) {
				Attribute attribute = a.next();
				if (!Double.isNaN(example.getValue(attribute))) {
					if (Ontology.ATTRIBUTE_VALUE_TYPE.isA(attribute.getValueType(), Ontology.NOMINAL)) {
						s.addCell(new Label(columnCounter, rowCounter,
								stripIfNecessary(replaceForbiddenChars(example.getValueAsString(attribute))), cf2));
					} else if (Ontology.ATTRIBUTE_VALUE_TYPE.isA(attribute.getValueType(), Ontology.DATE_TIME)) {
						DateTime dateTime = new DateTime(columnCounter, rowCounter,
								new Date((long) example.getValue(attribute)), dfCell);
						s.addCell(dateTime);
					} else if (Ontology.ATTRIBUTE_VALUE_TYPE.isA(attribute.getValueType(), Ontology.NUMERICAL)) {
						Number number = new Number(columnCounter, rowCounter, example.getValue(attribute), nfCell);
						s.addCell(number);
					} else {
						// default: write as a String
						s.addCell(new Label(columnCounter, rowCounter,
								stripIfNecessary(replaceForbiddenChars(example.getValueAsString(attribute))), cf2));
					}
				}
				columnCounter++;
			}
			rowCounter++;

			// checkForStop every 100 examples
			if (op != null && rowCounter % 100 == 0) {
				op.checkForStop();
			}
		}
	}

	private static String replaceForbiddenChars(String originalValue) {
		return originalValue.replace((char) 0, ' ');
	}

}
