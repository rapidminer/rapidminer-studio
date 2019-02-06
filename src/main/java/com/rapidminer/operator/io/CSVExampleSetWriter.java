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
package com.rapidminer.operator.io;

import java.io.OutputStreamWriter;
import java.io.PrintWriter;
import java.sql.Date;
import java.text.DateFormat;
import java.util.Arrays;
import java.util.LinkedList;
import java.util.List;
import java.util.logging.Level;

import com.rapidminer.example.Attribute;
import com.rapidminer.example.Example;
import com.rapidminer.example.ExampleSet;
import com.rapidminer.operator.OperatorCreationException;
import com.rapidminer.operator.OperatorDescription;
import com.rapidminer.operator.OperatorException;
import com.rapidminer.operator.OperatorProgress;
import com.rapidminer.operator.OperatorVersion;
import com.rapidminer.operator.ProcessStoppedException;
import com.rapidminer.parameter.ParameterType;
import com.rapidminer.parameter.ParameterTypeBoolean;
import com.rapidminer.parameter.ParameterTypeString;
import com.rapidminer.parameter.conditions.PortConnectedCondition;
import com.rapidminer.tools.LogService;
import com.rapidminer.tools.Ontology;
import com.rapidminer.tools.OperatorService;
import com.rapidminer.tools.io.Encoding;


/**
 * <p>
 * This operator can be used to write data into CSV files (Comma Separated Values). The values and
 * columns are separated by &quot;;&quot;. Missing data values are indicated by empty cells.
 * </p>
 *
 * @author Ingo Mierswa
 */
public class CSVExampleSetWriter extends AbstractStreamWriter {

	/** The parameter name for &quot;The CSV file which should be written.&quot; */
	public static final String PARAMETER_CSV_FILE = "csv_file";

	/** The parameter name for the column separator parameter. */
	public static final String PARAMETER_COLUMN_SEPARATOR = "column_separator";

	/** Indicates if the attribute names should be written as first row. */
	public static final String PARAMETER_WRITE_ATTRIBUTE_NAMES = "write_attribute_names";

	/**
	 * Indicates if nominal values should be quoted with double quotes. Quotes inside of nominal
	 * values will be escaped by a backslash.
	 */
	public static final String PARAMETER_QUOTE_NOMINAL_VALUES = "quote_nominal_values";

	public static final String PARAMETER_APPEND_FILE = "append_to_file";

	/**
	 * Indicates if date attributes are written as a formated string or as milliseconds past since
	 * January 1, 1970, 00:00:00 GMT
	 */
	// TODO introduce parameter which allows to determine the written format see
	// Nominal2Date operator
	public static final String PARAMETER_FORMAT_DATE = "format_date_attributes";

	/** The last version which treated integer as real. */
	public static final OperatorVersion INTEGER_AS_REAL = new OperatorVersion(8, 2, 0);

	public CSVExampleSetWriter(OperatorDescription description) {
		super(description);
	}

	/**
	 * Writes the exampleSet with the {@link PrintWriter} out, using colSeparator as column
	 * separator.
	 *
	 * @param exampleSet
	 *            the example set to write
	 * @param out
	 *            the {@link PrintWriter}
	 * @param colSeparator
	 *            the column separator
	 * @param quoteNomValues
	 *            if {@code true} nominal values are quoted
	 * @param writeAttribNames
	 *            if {@code true} the attribute names are written into the first row
	 * @param formatDate
	 *            if {@code true} dates are formatted to "M/d/yy h:mm a", otherwise milliseconds
	 *            since the epoch are used
	 *
	 * @deprecated please use
	 *             {@link CSVExampleSetWriter#writeCSV(ExampleSet, PrintWriter, String, boolean, boolean, boolean, OperatorProgress)}
	 *             instead to support operator progress.
	 */
	@Deprecated
	public static void writeCSV(ExampleSet exampleSet, PrintWriter out, String colSeparator, boolean quoteNomValues,
			boolean writeAttribNames, boolean formatDate) {
		try {
			writeCSV(exampleSet, out, colSeparator, quoteNomValues, writeAttribNames, formatDate, null, null);
		} catch (ProcessStoppedException e) {
			// can not happen because we provide no OperatorProgressListener
		}
	}

	/**
	 * Writes the exampleSet with the {@link PrintWriter} out, using colSeparator as column
	 * separator.
	 *
	 * @param exampleSet
	 *            the example set to write
	 * @param out
	 *            the {@link PrintWriter}
	 * @param colSeparator
	 *            the column separator
	 * @param quoteNomValues
	 *            if {@code true} nominal values are quoted
	 * @param writeAttribNames
	 *            if {@code true} the attribute names are written into the first row
	 * @param formatDate
	 *            if {@code true} dates are formatted to "M/d/yy h:mm a", otherwise milliseconds
	 *            since the epoch are used
	 * @param operatorProgress
	 *            the {@link OperatorProgress} is used to provide a more detailed progress. Within
	 *            this method the progress will be increased by number of examples times the number
	 *            of attributes. If you do not want the operator progress, just provide <code> null
	 *            <code>.
	 */
	public static void writeCSV(ExampleSet exampleSet, PrintWriter out, String colSeparator, boolean quoteNomValues,
			boolean writeAttribNames, boolean formatDate, OperatorProgress operatorProgress) throws ProcessStoppedException {
		writeCSV(exampleSet, out, colSeparator, quoteNomValues, writeAttribNames, formatDate, null, operatorProgress);
	}

	/**
	 * Writes the exampleSet with the {@link PrintWriter} out, using colSeparator as column
	 * separator and infinitySybol to denote infinite values.
	 *
	 * @param exampleSet
	 *            the example set to write
	 * @param out
	 *            the {@link PrintWriter}
	 * @param colSeparator
	 *            the column separator
	 * @param quoteNomValues
	 *            if {@code true} nominal values are quoted
	 * @param writeAttribNames
	 *            if {@code true} the attribute names are written into the first row
	 * @param formatDate
	 *            if {@code true} dates are formatted to "M/d/yy h:mm a", otherwise milliseconds
	 *            since the epoch are used
	 * @param infinitySymbol
	 *            the symbol to use for infinite values; if {@code null} the default symbol
	 *            "Infinity" is used
	 *
	 * @deprecated please use
	 *             {@link CSVExampleSetWriter#writeCSV(ExampleSet, PrintWriter, String, boolean, boolean, boolean, String, OperatorProgress)}
	 *             to support operator progress.
	 */
	@Deprecated
	public static void writeCSV(ExampleSet exampleSet, PrintWriter out, String colSeparator, boolean quoteNomValues,
			boolean writeAttribNames, boolean formatDate, String infinitySymbol) {
		try {
			writeCSV(exampleSet, out, colSeparator, quoteNomValues, writeAttribNames, formatDate, infinitySymbol, null);
		} catch (ProcessStoppedException e) {
			// can not happen because we provide no OperatorProcessListener
		}
	}

	/**
	 * Writes the exampleSet with the {@link PrintWriter} out, using colSeparator as column
	 * separator and infinitySybol to denote infinite values.
	 *
	 * @param exampleSet
	 *            the example set to write
	 * @param out
	 *            the {@link PrintWriter}
	 * @param colSeparator
	 *            the column separator
	 * @param quoteNomValues
	 *            if {@code true} nominal values are quoted
	 * @param writeAttribNames
	 *            if {@code true} the attribute names are written into the first row
	 * @param formatDate
	 *            if {@code true} dates are formatted to "M/d/yy h:mm a", otherwise milliseconds
	 *            since the epoch are used
	 * @param infinitySymbol
	 *            the symbol to use for infinite values; if {@code null} the default symbol
	 *            "Infinity" is used
	 * @param opProg
	 *            the {@link OperatorProgress} is used to provide a more detailed progress. Within
	 *            this method the progress will be increased by number of examples times the number
	 *            of attributes. If you do not want the operator progress, just provide <code> null
	 *            <code>.
	 */
	public static void writeCSV(ExampleSet exampleSet, PrintWriter out, String colSeparator, boolean quoteNomValues,
								boolean writeAttribNames, boolean formatDate, String infinitySymbol, OperatorProgress opProg)
			throws ProcessStoppedException {
		try {
			CSVExampleSetWriter writer = OperatorService.createOperator(CSVExampleSetWriter.class);
			writer.writeCSV(exampleSet, out, colSeparator, quoteNomValues, writeAttribNames, formatDate ? DateFormat.getInstance() : null, infinitySymbol, opProg);
		} catch (OperatorCreationException e) {
			LogService.getRoot().log(Level.SEVERE, "com.rapidminer.operator.io.CSVExampleSetWriter.creation_failed", e);
			return;
		}
	}

	/**
	 * Writes the exampleSet with the {@link PrintWriter} out, using colSeparator as column
	 * separator and infinitySybol to denote infinite values.
	 *
	 * @param exampleSet
	 *            the example set to write
	 * @param out
	 *            the {@link PrintWriter}
	 * @param colSeparator
	 *            the column separator
	 * @param quoteNomValues
	 *            if {@code true} nominal values are quoted
	 * @param writeAttribNames
	 *            if {@code true} the attribute names are written into the first row
	 * @param dateFormatter
	 *            the {@link DateFormat} used, if null milliseconds
	 *            since the epoch are used
	 * @param infinitySymbol
	 *            the symbol to use for infinite values; if {@code null} the default symbol
	 *            "Infinity" is used
	 * @param opProg
	 *            the {@link OperatorProgress} is used to provide a more detailed progress. Within
	 *            this method the progress will be increased by number of examples times the number
	 *            of attributes. If you do not want the operator progress, just provide <code> null
	 *            <code>.
	 * @since 8.2.1
	 */
	protected void writeCSV(final ExampleSet exampleSet,final PrintWriter out, final String colSeparator,final boolean quoteNomValues,
						 final boolean writeAttribNames, DateFormat dateFormatter, String infinitySymbol, OperatorProgress opProg)
			throws ProcessStoppedException {
		infinitySymbol = infinitySymbol == null ? String.valueOf(Double.POSITIVE_INFINITY) : infinitySymbol;
		final String negativeInfinitySymbol = "-" + infinitySymbol;
		final boolean writeInt = getCompatibilityLevel().isAbove(INTEGER_AS_REAL);
		final boolean formatDate = dateFormatter != null;

		// write column names
		if (writeAttribNames) {
			boolean first = true;
			for (Attribute attribute : (Iterable<Attribute>) () -> exampleSet.getAttributes().allAttributes()) {
				if (!first) {
					out.print(colSeparator);
				}
				String name = attribute.getName();
				if (quoteNomValues) {
					out.print('"');
					out.print(name.replace('"', '\''));
					out.print('"');
				} else {
					out.print(name);
				}
				first = false;
			}
			out.println();
		}

		// write data
		int progressCounter = 0;
		final int progressStep = Math.max(exampleSet.size() / 100, 1);
		for (Example example : exampleSet) {
			boolean first = true;
			for (Attribute attribute : (Iterable<Attribute>) () -> exampleSet.getAttributes().allAttributes()) {
				if (!first) {
					out.print(colSeparator);
				}
				if (!Double.isNaN(example.getValue(attribute))) {
					if (attribute.isNominal()) {
						String stringValue = example.getValueAsString(attribute);
						if (quoteNomValues) {
							out.print('"');
							out.print(stringValue.replace('"', '\''));
							out.print('"');
						} else {
							out.print(stringValue);
						}
					} else {
						Double value = example.getValue(attribute);
						if (Ontology.ATTRIBUTE_VALUE_TYPE.isA(attribute.getValueType(), Ontology.DATE_TIME)) {
							if (formatDate) {
								Date date = new Date(value.longValue());
								out.print(dateFormatter.format(date));
							} else if (writeInt) {
								out.print(value.longValue());
							} else {
								out.print(value);
							}
						} else if (value == Double.POSITIVE_INFINITY) {
							out.print(infinitySymbol);
						} else if (value == Double.NEGATIVE_INFINITY) {
							out.print(negativeInfinitySymbol);
						} else if (writeInt && attribute.getValueType() == Ontology.INTEGER) {
							out.print(value.longValue());
						} else {
							out.print(value);
						}
					}
				}
				first = false;
			}

			out.println();

			// trigger operator progress at every percent of the total data
			if (opProg != null) {
				progressCounter = (progressCounter + 1) % progressStep;
				if (progressCounter == 0) {
					opProg.step(progressStep);
				}
			}
		}
	}

	@Override
	public void writeStream(ExampleSet exampleSet, java.io.OutputStream outputStream) throws OperatorException {

		String columnSeparator = getParameterAsString(PARAMETER_COLUMN_SEPARATOR);
		boolean quoteNominalValues = getParameterAsBoolean(PARAMETER_QUOTE_NOMINAL_VALUES);
		boolean writeAttribNames = getParameterAsBoolean(PARAMETER_WRITE_ATTRIBUTE_NAMES);
		boolean formatDate = getParameterAsBoolean(PARAMETER_FORMAT_DATE);
		try (PrintWriter out = new PrintWriter(new OutputStreamWriter(outputStream, Encoding.getEncoding(this)))) {
			// init operator progress
			getProgress().setTotal(exampleSet.size());
			writeCSV(exampleSet, out, columnSeparator, quoteNominalValues, writeAttribNames, formatDate ? DateFormat.getInstance() : null, String.valueOf(Double.POSITIVE_INFINITY), getProgress());
			getProgress().complete();
		}
	}

	@Override
	protected boolean supportsEncoding() {
		return true;
	}

	@Override
	protected boolean shouldAppend() {
		return getParameterAsBoolean(PARAMETER_APPEND_FILE);
	}

	@Override
	public List<ParameterType> getParameterTypes() {
		List<ParameterType> types = new LinkedList<>();
		ParameterType type = makeFileParameterType();
		type.setPrimary(true);
		types.add(type);
		// "The CSV file which should be written.", "csv", false));
		types.add(new ParameterTypeString(PARAMETER_COLUMN_SEPARATOR, "The column separator.", ";", false));
		types.add(new ParameterTypeBoolean(PARAMETER_WRITE_ATTRIBUTE_NAMES,
				"Indicates if the attribute names should be written as first row.", true, false));
		types.add(new ParameterTypeBoolean(PARAMETER_QUOTE_NOMINAL_VALUES,
				"Indicates if nominal values should be quoted with double quotes.", true, false));
		types.add(new ParameterTypeBoolean(PARAMETER_FORMAT_DATE,
				"Indicates if date attributes are written as a formated string or as milliseconds past since January 1, 1970, 00:00:00 GMT",
				true, true));
		type = new ParameterTypeBoolean(PARAMETER_APPEND_FILE,
				"Indicates if new content should be appended to the file or if the pre-existing file content should be overwritten.",
				false, false);
		type.registerDependencyCondition(new PortConnectedCondition(this, () -> fileOutputPort, true, false));
		types.add(type);
		types.addAll(super.getParameterTypes());
		return types;
	}

	@Override
	protected String getFileParameterName() {
		return PARAMETER_CSV_FILE;
	}

	@Override
	protected String[] getFileExtensions() {
		return new String[] { "csv" };
	}
	
	@Override
	public OperatorVersion[] getIncompatibleVersionChanges() {
		OperatorVersion[] incompatibleVersions = super.getIncompatibleVersionChanges();
		OperatorVersion[] extendedIncompatibleVersions = Arrays.copyOf(incompatibleVersions,
				incompatibleVersions.length + 1);
		extendedIncompatibleVersions[incompatibleVersions.length] = INTEGER_AS_REAL;
		return extendedIncompatibleVersions;
	}
}
