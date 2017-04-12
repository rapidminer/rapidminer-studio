/**
 * Copyright (C) 2001-2017 by RapidMiner and the contributors
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

import java.text.NumberFormat;
import java.util.Arrays;
import java.util.LinkedList;
import java.util.List;

import com.rapidminer.example.Attribute;
import com.rapidminer.example.ExampleSet;
import com.rapidminer.operator.OperatorDescription;
import com.rapidminer.operator.OperatorException;
import com.rapidminer.operator.OperatorVersion;
import com.rapidminer.operator.io.AbstractReader;
import com.rapidminer.operator.nio.model.AbstractDataResultSetReader;
import com.rapidminer.operator.nio.model.DataResultSet;
import com.rapidminer.operator.nio.model.DataResultSetFactory;
import com.rapidminer.operator.nio.model.DataResultSetTranslationConfiguration;
import com.rapidminer.operator.nio.model.ExcelResultSetConfiguration;
import com.rapidminer.operator.nio.model.xlsx.XlsxResultSet;
import com.rapidminer.parameter.ParameterType;
import com.rapidminer.parameter.ParameterTypeConfiguration;
import com.rapidminer.parameter.ParameterTypeInt;
import com.rapidminer.parameter.ParameterTypeString;
import com.rapidminer.tools.io.Encoding;


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
 * @author Ingo Mierswa, Tobias Malbrecht, Sebastian Loh, Sebastian Land, Marco Boeck
 */
public class ExcelExampleSource extends AbstractDataResultSetReader {

	private static final String XLSX = "xlsx";
	private static final String XLS = "xls";
	public static final OperatorVersion CHANGE_5_0_4 = new OperatorVersion(5, 0, 4);
	public static final OperatorVersion CHANGE_5_0_11_NAME_SCHEMA = new OperatorVersion(5, 0, 11);

	/** Last version that used the old POI XLSX import */
	public static final OperatorVersion CHANGE_6_2_0_OLD_XLSX_IMPORT = new OperatorVersion(6, 2, 0);

	/**
	 * The parameter name for &quot;The Excel spreadsheet file which should be loaded.&quot;
	 */
	public static final String PARAMETER_EXCEL_FILE = "excel_file";

	/**
	 * The parameter name for &quot;The number of the sheet which should be imported.&quot;
	 */
	public static final String PARAMETER_SHEET_NUMBER = "sheet_number";

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

	public static final String PARAMETER_CREATE_LABEL = "create_label";

	public static final String PARAMETER_CREATE_ID = "create_id";

	public static final String PARAMETER_IMPORTED_CELL_RANGE = "imported_cell_range";

	static {
		AbstractReader.registerReaderDescription(new ReaderDescription(XLS, ExcelExampleSource.class, PARAMETER_EXCEL_FILE));
		AbstractReader
				.registerReaderDescription(new ReaderDescription(XLSX, ExcelExampleSource.class, PARAMETER_EXCEL_FILE));
	}

	public ExcelExampleSource(final OperatorDescription description) {
		super(description);
	}

	@Override
	protected ExampleSet transformDataResultSet(DataResultSet dataResultSet) throws OperatorException {
		ExampleSet exampleSet;
		if (getCompatibilityLevel().isAbove(CHANGE_6_2_0_OLD_XLSX_IMPORT) && dataResultSet instanceof XlsxResultSet) {

			XlsxResultSet xlsxResultSet = (XlsxResultSet) dataResultSet;
			xlsxResultSet.setUseFirstRowAsNames(getParameterAsBoolean(PARAMETER_FIRST_ROW_AS_NAMES));

			exampleSet = super.transformDataResultSet(dataResultSet);

			// Remove attributes if they are empty and no meta data is defined
			for (String attrToRemove : xlsxResultSet.getEmptyColumnNames(DataResultSetTranslationConfiguration
					.readColumnMetaData(this))) {
				Attribute toRemove = exampleSet.getAttributes().get(attrToRemove);
				if (toRemove != null) {
					exampleSet.getAttributes().remove(toRemove);
				}
			}
		} else {
			exampleSet = super.transformDataResultSet(dataResultSet);
		}

		return exampleSet;
	}

	@Override
	protected DataResultSetFactory getDataResultSetFactory() throws OperatorException {
		return new ExcelResultSetConfiguration(this);
	}

	@Override
	protected NumberFormat getNumberFormat() throws OperatorException {
		return null;
	}

	@Override
	protected String getFileParameterName() {
		return PARAMETER_EXCEL_FILE;
	}

	@Override
	protected String getFileExtension() {
		return XLS;
	}

	/** Returns the allowed file extensions. */
	@Override
	protected String[] getFileExtensions() {
		return new String[] { XLSX, XLS };
	}

	@Override
	public List<ParameterType> getParameterTypes() {
		List<ParameterType> types = new LinkedList<>();
		ParameterType type = new ParameterTypeConfiguration(ExcelExampleSourceConfigurationWizardCreator.class, this);
		type.setExpert(false);
		types.add(type);

		types.add(makeFileParameterType());

		types.add(new ParameterTypeInt(PARAMETER_SHEET_NUMBER, "The number of the sheet which should be imported.", 1,
				Integer.MAX_VALUE, 1, false));
		types.add(new ParameterTypeString(PARAMETER_IMPORTED_CELL_RANGE,
				"Cells to import, in Excel notation, e.g. B2:D25 or B2 for an open interval.", "A1"));

		types.addAll(Encoding.getParameterTypes(this));

		types.addAll(super.getParameterTypes());

		return types;
	}

	@Override
	public OperatorVersion[] getIncompatibleVersionChanges() {
		OperatorVersion[] changes = super.getIncompatibleVersionChanges();
		changes = Arrays.copyOf(changes, changes.length + 3);
		changes[changes.length - 3] = CHANGE_5_0_4;
		changes[changes.length - 2] = CHANGE_5_0_11_NAME_SCHEMA;
		changes[changes.length - 1] = CHANGE_6_2_0_OLD_XLSX_IMPORT;
		return changes;
	}

}
