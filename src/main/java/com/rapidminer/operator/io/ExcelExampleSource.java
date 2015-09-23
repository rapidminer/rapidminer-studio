/**
 * Copyright (C) 2001-2015 by RapidMiner and the contributors
 *
 * Complete list of developers available at our web site:
 *
 *      http://rapidminer.com
 *
 * This program is free software: you can redistribute it and/or modify
 * it under the terms of the GNU Affero General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU Affero General Public License for more details.
 *
 * You should have received a copy of the GNU Affero General Public License
 * along with this program.  If not, see http://www.gnu.org/licenses/.
 */
package com.rapidminer.operator.io;

import java.io.IOException;
import java.util.Arrays;
import java.util.Date;
import java.util.HashMap;
import java.util.HashSet;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.SortedSet;
import java.util.TimeZone;
import java.util.TreeSet;

import jxl.Cell;
import jxl.CellType;
import jxl.DateCell;
import jxl.NumberCell;
import jxl.Sheet;
import jxl.Workbook;
import jxl.read.biff.BiffException;

import com.rapidminer.gui.tools.dialogs.wizards.dataimport.excel.ExcelImportWizard;
import com.rapidminer.operator.Annotations;
import com.rapidminer.operator.OperatorDescription;
import com.rapidminer.operator.OperatorException;
import com.rapidminer.operator.UserError;
import com.rapidminer.parameter.ParameterType;
import com.rapidminer.parameter.ParameterTypeBoolean;
import com.rapidminer.parameter.ParameterTypeCategory;
import com.rapidminer.parameter.ParameterTypeConfiguration;
import com.rapidminer.parameter.ParameterTypeFile;
import com.rapidminer.parameter.ParameterTypeInt;
import com.rapidminer.parameter.ParameterTypeList;
import com.rapidminer.parameter.UndefinedParameterError;


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
 * @author Ingo Mierswa, Tobias Malbrecht, Sebastian Loh
 *
 * @deprecated Replaced by {@link com.rapidminer.operator.nio.ExcelExampleSource}.
 */
@Deprecated
public class ExcelExampleSource extends AbstractDataReader {

	/** Pseudo-annotation to be used for attribute names. */
	public static final String ANNOTATION_NAME = Annotations.ANNOTATION_NAME;

	/**
	 * Pseudo-annotation to be used for original attribute names which are read from the
	 * ANNOTATION_NAME line and later were changed by the user
	 */
	public static final String ANNOTATION_NAMES_FROM_DOCUMENT = "Name from document";

	/**
	 * The parameter name for &quot;The Excel spreadsheet file which should be loaded.&quot;
	 */
	public static final String PARAMETER_EXCEL_FILE = "excel_file";

	// static {
	// AbstractReader.registerReaderDescription(new ReaderDescription("xls",
	// ExcelExampleSource.class, PARAMETER_EXCEL_FILE));
	// }

	/**
	 * The parameter name for &quot;The number of the sheet which should be imported.&quot;
	 */
	public static final String PARAMETER_SHEET_NUMBER = "sheet_number";

	/**
	 * The parameter name for &quot;Indicates if the first row should be used for the attribute
	 * names.&quot;
	 */
	public static final String PARAMETER_FIRST_ROW_AS_NAMES = "first_row_as_names";

	/**
	 * The parameter name for &quot;Indicates which column should be used for the label attribute
	 * (0: no label)&quot;
	 */
	public static final String PARAMETER_LABEL_COLUMN = "label_column";

	/**
	 * The parameter name for &quot;Indicates which column should be used for the Id attribute (0:
	 * no id)&quot;
	 */
	public static final String PARAMETER_ID_COLUMN = "id_column";

	/**
	 * The parameter name for &quot;Determines, how the data is represented internally.&quot;
	 */
	public static final String PARAMETER_DATAMANAGEMENT = "datamanagement";

	public static final String PARAMETER_COLUMN_OFFSET = "column_offset";

	public static final String PARAMETER_ROW_OFFSET = "row_offset";

	public static final String PARAMETER_CREATE_LABEL = "create_label";

	public static final String PARAMETER_CREATE_ID = "create_id";

	public static final String PARAMETER_ANNOTATIONS = "annotations";

	private Workbook workbook = null;

	private boolean keepWorkbookOpen = false;

	private ExcelDataSet cacheDataSet = null;

	private boolean skipAnnotationRows = false;

	private int nameRowF = -1;

	private Map<Integer, String> annotationsMap;

	private Set<Integer> annotationRows;

	public ExcelExampleSource(final OperatorDescription description) {
		super(description);
		getParameters().addObserver(new CacheResetParameterObserver(PARAMETER_EXCEL_FILE), false);
	}

	// @Override
	// public ExampleSet createExampleSet() throws OperatorException {
	// // if
	// (getParameterAsFile(PARAMETER_CACHED_EXCEL_FILE).equals(getParameterAsFile(PARAMETER_EXCEL_FILE))
	// && attributeNamesDefinedByUser()) {
	// if (attributeNamesDefinedByUser()){
	// loadMetaDataFromParameters();
	// }
	// return super.createExampleSet();
	// }

	@Override
	public void writeMetaDataInParameter() {
		try {
			// clean up annotations in case attribute names are defined manually
			List<String[]> annotations = getParameterList(PARAMETER_ANNOTATIONS);
			List<String[]> cleanedAnnotations = new LinkedList<>();
			// annotation name needs to be changed into
			// ANNOTATION_NAMES_FROM_DOCUMENT since the reader shall
			// not overwrite the user defined attribute name by names which
			// might occur in the name row.
			for (String[] pair : annotations) {
				if (!pair[1].equals(Annotations.ANNOTATION_NAME)) {
					cleanedAnnotations.add(pair);
				} else {
					pair[1] = ExcelExampleSource.ANNOTATION_NAMES_FROM_DOCUMENT;
					cleanedAnnotations.add(pair);
				}
			}
			setParameter(PARAMETER_ANNOTATIONS, ParameterTypeList.transformList2String(cleanedAnnotations));
			// setParameter(ExcelExampleSource.PARAMETER_CACHED_EXCEL_FILE,
			// getParameter(PARAMETER_EXCEL_FILE));
		} catch (UndefinedParameterError e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
		super.writeMetaDataInParameter();
	}

	/**
	 * inform the excel reader to keep the workbook open due to performance reasons. The workbook
	 * has to be closed then manually.
	 *
	 * @see ExcelExampleSource#closeWorkbook()
	 */
	public void keepWorkbookOpen() {
		keepWorkbookOpen = true;
	}

	/**
	 * closes the the excel workbook
	 */
	public void closeWorkbook() {
		if (workbook != null) {
			workbook.close();
		}
	}

	public void resetWorkbook() {
		closeWorkbook();
		workbook = null;
		cacheDataSet = null;
	}

	public void skipNameAnnotationRow(final boolean flag) {
		skipAnnotationRows = flag;
	}

	@Override
	protected ExcelDataSet getDataSet() throws OperatorException {

		if (cacheDataSet != null) {
			cacheDataSet.setCurrentRow(0);
			return cacheDataSet;
		}

		List<String[]> allAnnotations = getParameterList(PARAMETER_ANNOTATIONS);
		annotationRows = new HashSet<>();
		annotationsMap = new HashMap<>();
		boolean nameFound = false;
		int lastAnnotatedRow = -1;
		int nameRow = -1;
		for (String[] pair : allAnnotations) {
			try {
				final int row = Integer.parseInt(pair[0]);
				if (row > lastAnnotatedRow) {
					lastAnnotatedRow = row;
				}
				annotationsMap.put(row, pair[1]);
				if (Annotations.ANNOTATION_NAME.equals(pair[1])) {
					nameFound = true;
					nameRow = row;
				}
				annotationRows.add(row);
			} catch (NumberFormatException e) {
				throw new OperatorException("row_number entries in parameter list " + PARAMETER_ANNOTATIONS
						+ " must be integers.", e);
			}
		}
		if (nameFound && getParameterAsBoolean(PARAMETER_FIRST_ROW_AS_NAMES)) {
			throw new OperatorException("If " + PARAMETER_FIRST_ROW_AS_NAMES + " is set to true, you cannot use "
					+ Annotations.ANNOTATION_NAME + " entries in parameter list " + PARAMETER_ANNOTATIONS + ".");
		}
		if (getParameterAsBoolean(PARAMETER_FIRST_ROW_AS_NAMES)) {
			annotationsMap.put(0, Annotations.ANNOTATION_NAME);
			annotationRows.add(0);
			nameRow = 0;
		}
		// final int lastAnnotatedRowF = lastAnnotatedRow + 1; //+1 since the
		// last annotated row itself must be ignored
		nameRowF = nameRow;

		return new ExcelDataSet();
	}

	@Override
	public List<ParameterType> getParameterTypes() {
		List<ParameterType> types = new LinkedList<>();
		ParameterType type = new ParameterTypeConfiguration(
				ExcelImportWizard.ExcelExampleSourceConfigurationWizardCreator.class, this);
		type.setExpert(false);
		types.add(type);
		types.add(new ParameterTypeFile(PARAMETER_EXCEL_FILE, "Name of the excel file to read the data from.", "xls", false));
		types.add(new ParameterTypeInt(PARAMETER_SHEET_NUMBER, "The number of the sheet which should be imported.", 1,
				Integer.MAX_VALUE, 1, false));
		types.add(new ParameterTypeInt(PARAMETER_ROW_OFFSET,
				"The number of rows to skip at top of sheet as they contain no usable data.", 0, 65535, 0, true));
		types.add(new ParameterTypeInt(PARAMETER_COLUMN_OFFSET,
				"The number of columns to skip at left side of sheet as they contain no usable data.", 0, 255, 0, true));
		types.add(new ParameterTypeBoolean(PARAMETER_FIRST_ROW_AS_NAMES,
				"Indicates if the first row should be used for the attribute names.", true, true));
		List<String> annotations = new LinkedList<>();
		annotations.add(Annotations.ANNOTATION_NAME);
		annotations.addAll(Arrays.asList(Annotations.ALL_KEYS_ATTRIBUTE));
		types.add(new ParameterTypeList(PARAMETER_ANNOTATIONS, "Maps row numbers to annotation names.",
				new ParameterTypeInt("row_number", "Row number which contains an annotation", 0, Integer.MAX_VALUE),
				new ParameterTypeCategory("annotation", "Name of the annotation to assign this row.", annotations
						.toArray(new String[annotations.size()]), 0)));
		types.addAll(super.getParameterTypes());
		return types;
	}

	/**
	 *
	 * Dedicated DataSet class for ExcelDataSets. It is need too cache the the DataSet in the
	 * {@link ExcelExampleSource#getDataSet()} method.
	 *
	 * @author Sebastian Loh (22.06.2010)
	 *
	 */
	private class ExcelDataSet extends DataSet {

		private Sheet sheet = null;

		private Cell[] cells = null;

		private final SortedSet<Integer> emptyRows = new TreeSet<>();

		private final SortedSet<Integer> emptyColumns = new TreeSet<>();

		private int rowOffset = 0;
		private int columnOffset = 0;
		private int numberOfRows = 0;
		private int numberOfColumns = 0;
		private int currentRow;

		public ExcelDataSet() throws OperatorException {

			rowOffset = getParameterAsInt(PARAMETER_ROW_OFFSET);
			columnOffset = getParameterAsInt(PARAMETER_COLUMN_OFFSET);
			currentRow = rowOffset;

			// load the excelWorkbook if it is not set
			if (workbook == null || true) {
				try {
					workbook = Workbook.getWorkbook(getParameterAsInputStream(PARAMETER_EXCEL_FILE));
				} catch (IOException e) {
					throw new UserError(ExcelExampleSource.this, 302, getParameter(PARAMETER_EXCEL_FILE), e.getMessage());
				} catch (BiffException e) {
					throw new UserError(ExcelExampleSource.this, 302, getParameter(PARAMETER_EXCEL_FILE), e.getMessage());
				}
			}
			try {
				sheet = workbook.getSheet(getParameterAsInt(PARAMETER_SHEET_NUMBER) - 1);
			} catch (IndexOutOfBoundsException | IllegalArgumentException e) {
				throw new UserError(ExcelExampleSource.this, 953, getParameter(PARAMETER_SHEET_NUMBER));
			}
			numberOfColumns = sheet.getColumns();
			numberOfRows = sheet.getRows();

			// TODO unifiy offset and emptiness checks in one loop
			// determine offsets
			boolean contentFound = false;
			for (int r = rowOffset; r < numberOfRows; r++) {
				for (int c = columnOffset; c < numberOfColumns; c++) {
					if (sheet.getCell(c, r).getType() != CellType.EMPTY
							&& !"".equals(sheet.getCell(c, r).getContents().trim())) {
						columnOffset = c;
						contentFound = true;
						break;
					}
				}
				if (contentFound) {
					rowOffset = r;
					break;
				}
			}
			if (!contentFound) {
				throw new UserError(ExcelExampleSource.this, 302, getParameter(PARAMETER_EXCEL_FILE),
						"spreadsheet seems to be empty");
			}

			// determine empty rows
			for (int r = rowOffset; r < numberOfRows; r++) {
				boolean rowEmpty = true;
				for (int c = columnOffset; c < numberOfColumns; c++) {
					if (sheet.getCell(c, r).getType() != CellType.EMPTY
							&& !"".equals(sheet.getCell(c, r).getContents().trim())) {
						rowEmpty = false;
						break;
					}
				}
				if (rowEmpty) {
					emptyRows.add(r);
				}
			}

			// determine empty columns
			for (int c = columnOffset; c < numberOfColumns; c++) {
				boolean columnEmpty = true;
				for (int r = rowOffset; r < numberOfRows; r++) {
					if (sheet.getCell(c, r).getType() != CellType.EMPTY
							&& !"".equals(sheet.getCell(c, r).getContents().trim())) {
						columnEmpty = false;
						break;
					}
				}
				if (columnEmpty) {
					emptyColumns.add(c);
				}
			}

			// set attribute names
			if (nameRowF != -1) {
				String[] attributeNames = new String[numberOfColumns - columnOffset - emptyColumns.size()];
				int columnCounter = 0;
				for (int c = columnOffset; c < numberOfColumns; c++) {
					// skip empty columns
					if (!emptyColumns.contains(c)) {
						Cell cell = sheet.getCell(c, rowOffset + nameRowF);
						attributeNames[columnCounter++] = cell.getContents();
					}
				}
				setAttributeNames(attributeNames);
				currentRow++;
			}

			// Annotations
			int columnCounter = 0;
			Annotations[] annotations = new Annotations[numberOfColumns - columnOffset - emptyColumns.size()];
			boolean foundAnnotations = false;
			for (int c = columnOffset; c < numberOfColumns; c++) {
				// skip empty columns
				if (emptyColumns.contains(c)) {
					continue;
				}

				annotations[columnCounter] = new Annotations();
				for (Map.Entry<Integer, String> entry : annotationsMap.entrySet()) {
					if (Annotations.ANNOTATION_NAME.equals(entry.getValue())) {
						continue;
					} else {
						Cell cell = sheet.getCell(c, rowOffset + entry.getKey());
						annotations[columnCounter].put(entry.getValue(), cell.getContents());
						foundAnnotations = true;
					}
				}
				columnCounter++;
			}
			if (foundAnnotations) {
				setAnnotations(annotations);
			}
		}

		@Override
		public int getNumberOfColumnsInCurrentRow() {
			return numberOfColumns - columnOffset - emptyColumns.size();
		}

		@Override
		public boolean isMissing(final int columnIndex) {
			return cells[columnIndex].getType() == CellType.EMPTY || cells[columnIndex].getType() == CellType.ERROR
					|| cells[columnIndex].getType() == CellType.FORMULA_ERROR || cells[columnIndex].getContents() == null
					|| "".equals(cells[columnIndex].getContents().trim());
		}

		@Override
		public Number getNumber(final int columnIndex) {
			try {
				if (cells[columnIndex].getType() == CellType.NUMBER) {
					return Double.valueOf(((NumberCell) cells[columnIndex]).getValue());
				} else {
					return Double.valueOf(cells[columnIndex].getContents());
				}
			} catch (ClassCastException e) {
			} catch (NumberFormatException e) {
			}
			return null;
		}

		@Override
		public Date getDate(final int columnIndex) {
			try {
				Date date = ((DateCell) cells[columnIndex]).getDate();
				if (date == null) {
					return null;
				}
				int offset = TimeZone.getDefault().getOffset(date.getTime());
				return new Date(date.getTime() - offset);
			} catch (ClassCastException e) {
			}
			return null;
		}

		@Override
		public String getString(final int columnIndex) {
			return cells[columnIndex].getContents();
		}

		@Override
		public boolean next() {
			while ((emptyRows.contains(currentRow) || skipAnnotationRows && annotationRows.contains(currentRow))
					&& currentRow < numberOfRows) {
				currentRow++;
			}
			if (currentRow >= numberOfRows) {
				return false;
			}
			cells = new Cell[numberOfColumns - columnOffset - emptyColumns.size()];
			int columnCounter = 0;
			for (int c = columnOffset; c < numberOfColumns; c++) {
				if (emptyColumns.contains(c)) {
					continue;
				}
				// sheet.getRow(currentRow);
				cells[columnCounter] = sheet.getCell(c, currentRow);
				columnCounter++;
			}
			currentRow++;
			return true;
		}

		@Override
		public void close() throws OperatorException {
			if (!keepWorkbookOpen) {
				workbook.close();
			}
		}

		public void setCurrentRow(final int currentRow) {
			this.currentRow = currentRow;
		}
	}
}
