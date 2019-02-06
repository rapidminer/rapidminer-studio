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
package com.rapidminer.operator.nio;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.InputStream;
import java.nio.charset.Charset;
import java.util.Arrays;
import java.util.Date;
import java.util.List;

import com.rapidminer.example.Attribute;
import com.rapidminer.example.ExampleSet;
import com.rapidminer.example.table.AttributeFactory;
import com.rapidminer.example.utils.ExampleSetBuilder;
import com.rapidminer.example.utils.ExampleSets;
import com.rapidminer.operator.Annotations;
import com.rapidminer.operator.IOObject;
import com.rapidminer.operator.OperatorDescription;
import com.rapidminer.operator.OperatorException;
import com.rapidminer.operator.UserError;
import com.rapidminer.operator.io.AbstractExampleSource;
import com.rapidminer.operator.nio.file.FileInputPortHandler;
import com.rapidminer.operator.nio.file.FileObject;
import com.rapidminer.operator.ports.InputPort;
import com.rapidminer.operator.ports.metadata.MetaData;
import com.rapidminer.operator.ports.metadata.SimplePrecondition;
import com.rapidminer.parameter.ParameterType;
import com.rapidminer.parameter.ParameterTypeInt;
import com.rapidminer.tools.Ontology;
import com.rapidminer.tools.Tools;
import com.rapidminer.tools.io.Encoding;

import jxl.Cell;
import jxl.CellType;
import jxl.DateCell;
import jxl.NumberCell;
import jxl.Sheet;
import jxl.Workbook;
import jxl.WorkbookSettings;
import jxl.biff.EmptyCell;
import jxl.format.CellFormat;
import jxl.read.biff.BiffException;


/**
 *
 * <p>
 * This operator can be used to load data from Microsoft Excel spreadsheets. This operator is able
 * to reads data from Excel 95, 97, 2000, XP, and 2003. The user has to define which of the
 * spreadsheets in the workbook should be used as data table. The table must have a format so that
 * each line is an example and each column represents an attribute. Please note that the first line
 * might be used for attribute names which can be indicated by a parameter.
 * </p>
 *
 * <p>
 * The data table can be placed anywhere on the sheet and is allowed to contain arbitrary formatting
 * instructions, empty rows, and empty columns. Missing data values are indicated by empty cells or
 * by cells containing only &quot;?&quot;.
 * </p>
 *
 * @author Ingo Mierswa
 */
public class ExcelFormatExampleSource extends AbstractExampleSource {

	/**
	 * The parameter name for &quot;The Excel spreadsheet file which should be loaded.&quot;
	 */
	public static final String PARAMETER_EXCEL_FILE = "excel_file";

	/**
	 * The parameter name for &quot;The number of the sheet which should be imported.&quot;
	 */
	public static final String PARAMETER_SHEET_NUMBER = "sheet_number";

	private final InputPort fileInputPort = getInputPorts().createPort("file");

	public ExcelFormatExampleSource(final OperatorDescription description) {
		super(description);

		fileInputPort.addPrecondition(new SimplePrecondition(fileInputPort, new MetaData(FileObject.class)) {

			@Override
			protected boolean isMandatory() {
				return false;
			}
		});
	}

	@Override
	public ExampleSet createExampleSet() throws OperatorException {
		// reading configuration

		String sourceAnnotation = null;
		InputStream inputStream = null;
		if (!fileInputPort.isConnected()) {
			File inputFile = getParameterAsFile(PARAMETER_EXCEL_FILE);
			try {
				inputStream = new FileInputStream(inputFile);
			} catch (FileNotFoundException e) {
				throw new UserError(this, 302, inputFile.getPath(), e.getMessage());
			}
			sourceAnnotation = inputFile.getPath();
		} else {
			IOObject fileObject = fileInputPort.getDataOrNull(IOObject.class);
			if (fileObject != null) {
				inputStream = fileInputPort.getData(FileObject.class).openStream();
				sourceAnnotation = fileObject.getAnnotations().getAnnotation(Annotations.KEY_SOURCE);
			} else {
				throw new UserError(this, 302, "no data specified at input port");
			}
		}

		// load the excelWorkbook if it is not set
		Workbook workbook = null;
		try {
			Charset encoding = Encoding.getEncoding(this);
			WorkbookSettings workbookSettings = new WorkbookSettings();
			workbookSettings.setEncoding(encoding.name());
			workbook = Workbook.getWorkbook(inputStream, workbookSettings);
		} catch (IOException e) {
			throw new UserError(this, 302, sourceAnnotation, e.getMessage());
		} catch (BiffException e) {
			throw new UserError(this, 302, sourceAnnotation, e.getMessage());
		}

		int sheetNumber = getParameterAsInt(PARAMETER_SHEET_NUMBER) - 1;
		Sheet sheet = null;
		try {
			sheet = workbook.getSheet(sheetNumber);
		} catch (IndexOutOfBoundsException | IllegalArgumentException e) {
			throw new UserError(this, 953, sheetNumber + 1);
		}

		int totalNumberOfColumns = sheet.getColumns();
		int totalNumberOfRows = sheet.getRows();

		if (totalNumberOfColumns < 0 || totalNumberOfRows < 0) {
			throw new UserError(this, 404);
		}

		boolean[] emptyColumns = new boolean[totalNumberOfColumns];
		boolean[] emptyRows = new boolean[totalNumberOfRows];

		// filling offsets
		Arrays.fill(emptyColumns, true);
		Arrays.fill(emptyRows, true);

		// determine offsets and emptiness
		boolean foundAny = false;
		for (int r = 0; r < totalNumberOfRows; r++) {
			for (int c = 0; c < totalNumberOfColumns; c++) {
				if (emptyRows[r] || emptyColumns[c]) {
					final Cell cell = sheet.getCell(c, r);
					if (!(cell instanceof EmptyCell) && cell.getType() != CellType.EMPTY
							&& !"".equals(cell.getContents().trim())) {
						foundAny = true;
						emptyRows[r] = false;
						emptyColumns[c] = false;
					}
				}
			}
		}
		if (!foundAny) {
			throw new UserError(this, 302, sourceAnnotation, "spreadsheet seems to be empty");
		}

		// retrieve attribute names: first count columns
		int numberOfAttributes = 0;
		for (int i = 0; i < totalNumberOfColumns; i++) {
			if (!emptyColumns[i]) {
				numberOfAttributes++;
			}
		}

		// retrieve or generate attribute names
		String[] attributeNames = new String[numberOfAttributes * 2];
		int counter = 0;
		for (int i = 0; i < totalNumberOfColumns; i++) {
			if (!emptyColumns[i]) {
				String currentName = Tools.getExcelColumnName(i);
				attributeNames[counter] = currentName;
				attributeNames[counter + numberOfAttributes] = currentName + "_format";
				counter++;
			}
		}

		// create example table
		Attribute[] attributes = new Attribute[attributeNames.length];
		for (int i = 0; i < attributeNames.length; i++) {
			attributes[i] = AttributeFactory.createAttribute(attributeNames[i], Ontology.NOMINAL);
		}
		ExampleSetBuilder builder = ExampleSets.from(attributes);

		for (int r = 0; r < totalNumberOfRows; r++) {
			if (emptyRows[r]) {
				continue;
			}
			int currentC = 0;
			double[] data = new double[attributes.length];
			for (int c = 0; c < totalNumberOfColumns; c++) {
				if (emptyColumns[c]) {
					continue;
				}

				final Cell cell = sheet.getCell(c, r);
				if (cell instanceof EmptyCell || cell.getType() == CellType.EMPTY || cell.getType() == CellType.ERROR
						|| cell.getType() == CellType.FORMULA_ERROR || cell.getContents() == null
						|| "".equals(cell.getContents().trim())) {
					data[currentC] = Double.NaN;
					data[currentC + numberOfAttributes] = Double.NaN;
				} else {
					final CellType type = cell.getType();
					if (type == CellType.NUMBER || type == CellType.NUMBER_FORMULA) {
						final double value = ((NumberCell) cell).getValue();
						data[currentC] = attributes[currentC].getMapping().mapString(value + "");
					} else if (type == CellType.DATE || type == CellType.DATE_FORMULA) {
						Date date = ((DateCell) cell).getDate();
						data[currentC] = attributes[currentC].getMapping().mapString(Tools.formatDateTime(date));
					} else {
						data[currentC] = attributes[currentC].getMapping().mapString(cell.getContents());
					}

					final CellFormat cellFormat = cell.getCellFormat();
					StringBuffer formatInfo = new StringBuffer();
					formatInfo.append("background: " + cellFormat.getBackgroundColour().getDescription() + "; ");
					formatInfo.append("pattern: " + cellFormat.getPattern().getDescription() + "; ");
					formatInfo.append("foreground: " + cellFormat.getFont().getColour().getDescription() + "; ");
					formatInfo.append("font_name: " + cellFormat.getFont().getName() + "; ");
					formatInfo.append("font_bold_weight: " + cellFormat.getFont().getBoldWeight() + "; ");
					formatInfo.append("font_size: " + cellFormat.getFont().getPointSize() + "; ");
					formatInfo.append("font_italic: " + cellFormat.getFont().isItalic() + "; ");
					formatInfo.append("font_struckout: " + cellFormat.getFont().isStruckout() + ";");
					data[currentC + numberOfAttributes] = attributes[currentC + numberOfAttributes].getMapping()
							.mapString(formatInfo.toString());
				}
				currentC++;
			}
			builder.addRow(data);
		}

		ExampleSet exampleSet = builder.build();
		if (sourceAnnotation != null) {
			exampleSet.getAnnotations().setAnnotation(Annotations.KEY_SOURCE, sourceAnnotation);
		}
		return exampleSet;
	}

	@Override
	public List<ParameterType> getParameterTypes() {
		List<ParameterType> types = super.getParameterTypes();

		ParameterType type = FileInputPortHandler.makeFileParameterType(this, PARAMETER_EXCEL_FILE,
				"Name of the file to read the data from.", "xls", () -> fileInputPort);
		type.setPrimary(true);
		types.add(type);

		types.add(new ParameterTypeInt(PARAMETER_SHEET_NUMBER, "The number of the sheet which should be imported.", 1,
				Integer.MAX_VALUE, 1, false));

		types.addAll(Encoding.getParameterTypes(this));

		return types;
	}
}
