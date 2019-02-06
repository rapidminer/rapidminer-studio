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

import com.rapidminer.example.ExampleSet;
import com.rapidminer.example.table.NumericalAttribute;
import com.rapidminer.example.table.SparseFormatDataRowReader;
import com.rapidminer.operator.OperatorDescription;
import com.rapidminer.operator.OperatorException;
import com.rapidminer.operator.UserError;
import com.rapidminer.parameter.ParameterType;
import com.rapidminer.parameter.ParameterTypeBoolean;
import com.rapidminer.parameter.ParameterTypeCategory;
import com.rapidminer.parameter.ParameterTypeFile;
import com.rapidminer.tools.io.Encoding;

import java.io.File;
import java.io.IOException;
import java.nio.charset.Charset;
import java.util.LinkedList;
import java.util.List;


/**
 * Writes values of all examples in an {@link ExampleSet} to a file. Dense, sparse, and user defined
 * formats (specified by the parameter 'format') can be used. Attribute description files may be
 * generated for dense and sparse format as well. These formats can be read using the
 * {@link ExampleSource} and {@link SparseFormatExampleSource} operators.
 * 
 * <dl>
 * <dt>dense:</dt>
 * <dd>Each line of the generated data file is of the form<br/>
 * <center>
 * 
 * <pre>
 * regular attributes &lt;special attributes&gt;
 * </pre>
 * 
 * </center> For example, each line could have the form <center>
 * 
 * <pre>
 * value1 value2 ... valueN &lt;id&gt; &lt;label&gt; &lt;prediction&gt; ... &lt;confidences&gt;
 * </pre>
 * 
 * </center> Values in parenthesis are optional and are only printed if they are available. The
 * confidences are only given for nominal predictions. Other special attributes might be the example
 * weight or the cluster number.</dd>
 * <dt>sparse:</dt>
 * <dd>Only non 0 values are written to the file, prefixed by a column index. See the description of
 * {@link SparseFormatExampleSource} for details.</dd>
 * </dl>
 * 
 * @see com.rapidminer.example.ExampleSet
 * 
 * @author Simon Fischer, Ingo Mierswa
 */
public class ExampleSetWriter extends AppendingExampleSetWriter {

	/** The parameter name for &quot;File to save the example set to.&quot; */
	public static final String PARAMETER_EXAMPLE_SET_FILE = "example_set_file";

	/** The parameter name for &quot;File to save the attribute descriptions to.&quot; */
	public static final String PARAMETER_ATTRIBUTE_DESCRIPTION_FILE = "attribute_description_file";

	/** The parameter name for &quot;Format to use for output.&quot; */
	public static final String PARAMETER_FORMAT = "format";

	/** The parameter name for &quot;Indicates if the data file content should be zipped.&quot; */
	public static final String PARAMETER_ZIPPED = "zipped";

	private static final String[] FORMAT_NAMES = new String[SparseFormatDataRowReader.FORMAT_NAMES.length + 1];

	private static final int DENSE_FORMAT = 0;

	static {
		FORMAT_NAMES[0] = "dense";
		for (int i = 0; i < SparseFormatDataRowReader.FORMAT_NAMES.length; i++) {
			FORMAT_NAMES[i + 1] = "sparse_" + SparseFormatDataRowReader.FORMAT_NAMES[i];
		}
	}

	public ExampleSetWriter(OperatorDescription description) {
		super(description);
	}

	@Override
	public ExampleSet write(ExampleSet eSet) throws OperatorException {
		boolean zipped = getParameterAsBoolean(PARAMETER_ZIPPED);
		File dataFile = getParameterAsFile(PARAMETER_EXAMPLE_SET_FILE, true);
		if (zipped) {
			dataFile = new File(dataFile.getAbsolutePath() + ".gz");
		}
		File attFile = getParameterAsFile(PARAMETER_ATTRIBUTE_DESCRIPTION_FILE, true);

		boolean append = shouldAppend(dataFile);
		Charset encoding = Encoding.getEncoding(this);

		try {
			// write example set
			int format = getParameterAsInt(PARAMETER_FORMAT);
			getLogger().info("Writing example set in format '" + FORMAT_NAMES[format] + "'.");
			if (format == DENSE_FORMAT) { // dense
				eSet.writeDataFile(dataFile, NumericalAttribute.UNLIMITED_NUMBER_OF_DIGITS, true, zipped, append, encoding);
				if (attFile != null) {
					eSet.writeAttributeFile(attFile, dataFile, encoding);
				}
			} else { // sparse
				eSet.writeSparseDataFile(dataFile, format - 1, NumericalAttribute.UNLIMITED_NUMBER_OF_DIGITS, true, zipped,
						append, encoding);
				if (attFile != null) {
					eSet.writeSparseAttributeFile(attFile, dataFile, format - 1, encoding);
				}
			}
		} catch (IOException e) {
			throw new UserError(this, e, 303, new Object[] { dataFile + " / " + attFile, e.getMessage() });
		}

		return eSet;
	}

	@Override
	protected boolean supportsEncoding() {
		return true;
	}

	@Override
	public List<ParameterType> getParameterTypes() {
		List<ParameterType> types = new LinkedList<ParameterType>();
		types.add(new ParameterTypeFile(PARAMETER_EXAMPLE_SET_FILE, "File to save the example set to.", "dat", false));
		types.add(new ParameterTypeFile(PARAMETER_ATTRIBUTE_DESCRIPTION_FILE, "File to save the attribute descriptions to.",
				"aml", true));
		types.add(new ParameterTypeCategory(PARAMETER_FORMAT, "Format to use for output.", FORMAT_NAMES, 0));
		types.add(new ParameterTypeBoolean(PARAMETER_ZIPPED, "Indicates if the data file content should be zipped.", false));
		types.addAll(super.getParameterTypes());
		return types;
	}
}
