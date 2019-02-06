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

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.OutputStream;
import java.io.OutputStreamWriter;
import java.io.PrintWriter;
import java.nio.charset.Charset;
import java.util.Iterator;
import java.util.LinkedList;
import java.util.List;
import java.util.logging.Level;
import java.util.zip.GZIPOutputStream;

import com.rapidminer.example.Example;
import com.rapidminer.example.ExampleFormatter;
import com.rapidminer.example.ExampleSet;
import com.rapidminer.example.FormatterException;
import com.rapidminer.example.table.NumericalAttribute;
import com.rapidminer.operator.OperatorDescription;
import com.rapidminer.operator.OperatorException;
import com.rapidminer.operator.UserError;
import com.rapidminer.parameter.ParameterType;
import com.rapidminer.parameter.ParameterTypeBoolean;
import com.rapidminer.parameter.ParameterTypeCategory;
import com.rapidminer.parameter.ParameterTypeFile;
import com.rapidminer.parameter.ParameterTypeInt;
import com.rapidminer.parameter.ParameterTypeString;
import com.rapidminer.tools.io.Encoding;


/**
 * Writes an example set to a file using a special, user defined format.
 * 
 * Using the parameter 'special_format', the user can specify the exact format. The $ sign has a
 * special meaning and introduces a command (the following character) Additional arguments to this
 * command may be supplied enclosing it in square brackets.
 * <dl>
 * <dt>$a:</dt>
 * <dd>All attributes separated by the default separator</dd>
 * <dt>$a[separator]:</dt>
 * <dd>All attributes separated by separator</dd>
 * <dt>$s[separator][indexSeparator]:</dt>
 * <dd>Sparse format. For all non zero attributes the following strings are concatenated: the column
 * index, the value of indexSeparator, the attribute value. Attributes are separated by separator.</dd>
 * <dt>$v[name]:</dt>
 * <dd>The value of the attribute with the given name (both regular and special attributes)</dd>
 * <dt>$k[index]:</dt>
 * <dd>The value of the attribute with the given index</dd>
 * <dt>$l:</dt>
 * <dd>The label</dd>
 * <dt>$p:</dt>
 * <dd>The predicted label</dd>
 * <dt>$d:</dt>
 * <dd>All prediction confidences for all classes in the form conf(class)=value</dd>
 * <dt>$d[class]:</dt>
 * <dd>The prediction confidence for the defined class as a simple number</dd>
 * <dt>$i:</dt>
 * <dd>The id</dd>
 * <dt>$w:</dt>
 * <dd>The weight</dd>
 * <dt>$b:</dt>
 * <dd>The batch number</dd>
 * <dt>$n:</dt>
 * <dd>The newline character</dd>
 * <dt>$t:</dt>
 * <dd>The tabulator character</dd>
 * <dt>$$:</dt>
 * <dd>The dollar sign</dd>
 * <dt>$[:</dt>
 * <dd>The '[' character</dd>
 * <dt>$]:</dt>
 * <dd>The ']' character</dd>
 * </dl>
 * Make sure the format string ends with $n if you want examples to be separated by newlines!
 * 
 * Up to Version 5.0, the functionality of this operator was covered by the regular
 * {@link ExampleSetWriter}.
 * 
 * @author Simon Fischer
 */
public class SpecialFormatExampleSetWriter extends AppendingExampleSetWriter {

	public static final String PARAMETER_ADD_NEW_LINE = "add_line_separator";

	/** The parameter name for &quot;Format string to use for output.&quot; */
	public static final String PARAMETER_SPECIAL_FORMAT = "special_format";

	/** The parameter name for &quot;File to save the example set to.&quot; */
	public static final String PARAMETER_EXAMPLE_SET_FILE = "example_set_file";

	/**
	 * The parameter name for &quot;The number of fraction digits in the output file (-1: all
	 * possible digits).&quot;
	 */
	public static final String PARAMETER_FRACTION_DIGITS = "fraction_digits";

	/**
	 * Indicates if nominal values should be quoted with double quotes. Quotes inside of nominal
	 * values will be escaped by a backslash.
	 */
	public static final String PARAMETER_QUOTE_NOMINAL_VALUES = "quote_nominal_values";

	/** The parameter name for &quot;Indicates if the data file content should be zipped.&quot; */
	public static final String PARAMETER_ZIPPED = "zipped";

	/** The parameter name for &quot;Indicates if an existing table should be overwritten.&quot; */
	public static final String PARAMETER_OVERWRITE_MODE = "overwrite_mode";

	public SpecialFormatExampleSetWriter(OperatorDescription description) {
		super(description);
	}

	@Override
	public ExampleSet write(ExampleSet ioobject) throws OperatorException {
		boolean zipped = getParameterAsBoolean(PARAMETER_ZIPPED);
		File dataFile = getParameterAsFile(PARAMETER_EXAMPLE_SET_FILE, true);
		if (zipped) {
			dataFile = new File(dataFile.getAbsolutePath() + ".gz");
		}
		boolean quoteNominal = getParameterAsBoolean(PARAMETER_QUOTE_NOMINAL_VALUES);
		int fractionDigits = getParameterAsInt(PARAMETER_FRACTION_DIGITS);
		if (fractionDigits < 0) {
			fractionDigits = NumericalAttribute.UNLIMITED_NUMBER_OF_DIGITS;
		}

		Charset encoding = Encoding.getEncoding(this);

		writeSpecialFormat(ioobject, dataFile, fractionDigits, getParameterAsBoolean(PARAMETER_ADD_NEW_LINE), quoteNominal,
				zipped, shouldAppend(dataFile), encoding);
		return ioobject;
	}

	@Override
	protected boolean supportsEncoding() {
		return true;
	}

	private void writeSpecialFormat(ExampleSet exampleSet, File dataFile, int fractionDigits, boolean automaticLineBreak,
			boolean quoteNominal, boolean zipped, boolean append, Charset encoding) throws OperatorException {
		String format = getParameterAsString(PARAMETER_SPECIAL_FORMAT);
		if (format == null) {
			throw new UserError(this, 201, new Object[] { "special_format", "format", "special_format" });
		}
		ExampleFormatter formatter;
		try {
			formatter = ExampleFormatter.compile(format, exampleSet, fractionDigits, quoteNominal);
		} catch (FormatterException e) {
			throw new UserError(this, 901, format, e.getMessage());
		}
		OutputStream out = null;
		try (OutputStream outStream = new FileOutputStream(dataFile, append);
				OutputStream zippedStream = zipped ? new GZIPOutputStream(outStream) : null;
				OutputStreamWriter osw = new OutputStreamWriter(zipped ? zippedStream : outStream, encoding);
				PrintWriter writer = new PrintWriter(osw)) {
			out = outStream;
			Iterator<Example> reader = exampleSet.iterator();
			while (reader.hasNext()) {
				if (automaticLineBreak) {
					writer.println(formatter.format(reader.next()));
				} else {
					writer.print(formatter.format(reader.next()));
				}
			}
		} catch (IOException e) {
			throw new UserError(this, 303, dataFile, e.getMessage());
		} finally {
			if (out != null) {
				try {
					out.close();
				} catch (IOException e) {
					getLogger().log(Level.WARNING, "Cannot close stream to file " + dataFile, e);
				}
			}
		}
	}

	@Override
	public List<ParameterType> getParameterTypes() {
		List<ParameterType> types = new LinkedList<>();
		ParameterType type = new ParameterTypeFile(PARAMETER_EXAMPLE_SET_FILE, "File to save the example set to.", "dat", false);
		type.setPrimary(true);
		types.add(type);
		types.add(new ParameterTypeString(PARAMETER_SPECIAL_FORMAT, "Format string to use for output.", false));
		types.add(new ParameterTypeInt(PARAMETER_FRACTION_DIGITS,
				"The number of fraction digits in the output file (-1: all possible digits).", -1, Integer.MAX_VALUE, -1));
		types.add(new ParameterTypeBoolean(PARAMETER_ADD_NEW_LINE,
				"If checked, each example is followed by a line break automatically", true));
		types.add(new ParameterTypeBoolean(PARAMETER_QUOTE_NOMINAL_VALUES,
				"Indicates if nominal values should be quoted with double quotes.", true));
		types.add(new ParameterTypeBoolean(PARAMETER_ZIPPED, "Indicates if the data file content should be zipped.", false));
		types.add(new ParameterTypeCategory(PARAMETER_OVERWRITE_MODE,
				"Indicates if an existing table should be overwritten or if data should be appended.", OVERWRITE_MODES,
				OVERWRITE_MODE_OVERWRITE_FIRST));
		types.addAll(super.getParameterTypes());
		return types;
	}
}
