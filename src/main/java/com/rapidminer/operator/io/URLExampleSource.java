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

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.util.ArrayList;
import java.util.LinkedList;
import java.util.List;
import java.util.regex.Pattern;

import com.rapidminer.example.Attribute;
import com.rapidminer.example.ExampleSet;
import com.rapidminer.example.table.AttributeFactory;
import com.rapidminer.example.utils.ExampleSetBuilder;
import com.rapidminer.example.utils.ExampleSets;
import com.rapidminer.operator.OperatorCreationException;
import com.rapidminer.operator.OperatorDescription;
import com.rapidminer.operator.OperatorException;
import com.rapidminer.operator.UserError;
import com.rapidminer.operator.preprocessing.GuessValueTypes;
import com.rapidminer.parameter.ParameterType;
import com.rapidminer.parameter.ParameterTypeBoolean;
import com.rapidminer.parameter.ParameterTypeString;
import com.rapidminer.tools.Ontology;
import com.rapidminer.tools.OperatorService;
import com.rapidminer.tools.Tools;
import com.rapidminer.tools.io.Encoding;


/**
 * <p>
 * This operator reads an example set from an URL. The format has to be a CSV format with ';' as
 * column separator and nominal values have to be quoted with a double quote (&quot;). A quote
 * inside of a nominal value has to be escaped by a backslash like in \&quot;. The first row is
 * allowed to contain the column names which has to be indicated by the corresponding parameter.
 * Comments are not allowed, unknown attribute values can be marked with empty strings or a question
 * mark.
 * </p>
 *
 * <p>
 * This operator is not nearly as powerful as the operators ExampleSource or SimpleExampleSource but
 * is on the other hand able to read data from arbitrary places as long as the format fits the
 * specification above. Please note also that the usage of this operator hardly allows for a correct
 * meta data description which might lead to problems if the meta data between training and test set
 * differ in a learning scenario.
 * </p>
 *
 * <p>
 * Attribute roles can not be directly set during loading but the operator ChangeAttributeRole has
 * to be used after loading in order to change the roles.
 * </p>
 *
 * @rapidminer.index url
 * @author Ingo Mierswa
 */
public class URLExampleSource extends AbstractExampleSource {

	public static final String PARAMETER_URL = "url";

	public static final String PARAMETER_COLUMN_SEPARATORS = "column_separators";

	/** The parameter name for &quot;Character that is used as decimal point.&quot; */
	public static final String PARAMETER_DECIMAL_POINT_CHARACTER = "decimal_point_character";

	public static final String PARAMETER_READ_ATTRIBUTE_NAMES = "read_attribute_names";

	public static final String PARAMETER_SKIP_ERROR_LINES = "skip_error_lines";

	public URLExampleSource(final OperatorDescription description) {
		super(description);
	}

	@Override
	public ExampleSet createExampleSet() throws OperatorException {
		boolean readAttributeNames = getParameterAsBoolean(PARAMETER_READ_ATTRIBUTE_NAMES);
		boolean skipErrorLines = getParameterAsBoolean(PARAMETER_SKIP_ERROR_LINES);
		char decimalPointCharacter = getParameterAsString(PARAMETER_DECIMAL_POINT_CHARACTER).charAt(0);

		ExampleSetBuilder builder = null;
		BufferedReader in = null;
		try {
			InputStream inputStream = getParameterAsInputStream(PARAMETER_URL);

			in = new BufferedReader(new InputStreamReader(inputStream, Encoding.getEncoding(this)));
			String line = null;
			Pattern separatorPattern = Pattern.compile(getParameterAsString(PARAMETER_COLUMN_SEPARATORS));
			int rowLength = -1;
			int lineCounter = 1;
			List<Attribute> attributes = new ArrayList<>();
			while ((line = in.readLine()) != null) {
				// skip empty lines
				if (line.trim().length() == 0) {
					continue;
				}

				// split row
				String[] row = Tools.quotedSplit(line, separatorPattern);

				// check for row length
				if (rowLength >= 0 && rowLength != row.length) {
					if (skipErrorLines) {
						logWarning("Wrong number of columns in line " + lineCounter + ": was " + row.length + ", expected "
								+ rowLength);
						continue;
					} else {
						throw new UserError(this, 302, getParameter(PARAMETER_URL), "Wrong number of columns in line "
								+ lineCounter + ": was " + row.length + ", expected " + rowLength);
					}
				}

				// store row length for first row
				if (rowLength < 0) {
					rowLength = row.length;
				}

				// create table for first line
				boolean skipLine = false;
				if (builder == null) {
					int attCounter = 1;
					for (String r : row) {
						if (readAttributeNames) {
							attributes.add(AttributeFactory.createAttribute(r, Ontology.NOMINAL));
						} else {
							attributes.add(AttributeFactory.createAttribute("Att" + attCounter, Ontology.NOMINAL));
						}
						attCounter++;
					}
					builder = ExampleSets.from(attributes);

					if (readAttributeNames) {
						skipLine = true;
					}
				}

				// store row as data row
				if (!skipLine) {
					double[] data = new double[row.length];
					for (int i = 0; i < data.length; i++) {
						data[i] = attributes.get(i).getMapping().mapString(row[i]);
					}
					builder.addRow(data);
				}
				lineCounter++;
			}

		} catch (IOException e) {
			throw new UserError(this, 302, getParameter(PARAMETER_URL), e.getMessage());
		} finally {
			if (in != null) {
				try {
					in.close();
				} catch (IOException e) {
					// do nothing
				}
			}
		}

		if (builder == null) {
			builder = ExampleSets.from(new ArrayList<>());
		}

		ExampleSet exampleSet = builder.build();

		try {
			GuessValueTypes guessValuesTypes = OperatorService.createOperator(GuessValueTypes.class);
			guessValuesTypes.setParameter(GuessValueTypes.PARAMETER_DECIMAL_POINT_CHARACTER,
					Character.toString(decimalPointCharacter));
			exampleSet = guessValuesTypes.apply(exampleSet);
		} catch (OperatorCreationException e) {
			throw new OperatorException("Cannot create GuessValueTypes: " + e, e);
		}

		return exampleSet;
	}

	@Override
	protected boolean supportsEncoding() {
		return true;
	}

	@Override
	public List<ParameterType> getParameterTypes() {
		List<ParameterType> types = new LinkedList<>();

		types.add(new ParameterTypeString(PARAMETER_URL, "The url to read the data from.", false));

		ParameterType type = new ParameterTypeString(PARAMETER_COLUMN_SEPARATORS,
				"Column separators for data files (regular expression)", ",\\s*|;\\s*|\\s+");
		type.setExpert(false);
		types.add(type);

		type = new ParameterTypeBoolean(PARAMETER_READ_ATTRIBUTE_NAMES,
				"Read attribute names from file (assumes the attribute names are in the first line of the file).", false);
		type.setExpert(false);
		types.add(type);

		types.add(new ParameterTypeBoolean(PARAMETER_SKIP_ERROR_LINES,
				"Indicates if lines which can not be read should be skipped instead of letting this operator fail its execution.",
				false));

		types.add(
				new ParameterTypeString(PARAMETER_DECIMAL_POINT_CHARACTER, "Character that is used as decimal point.", "."));
		types.addAll(super.getParameterTypes());
		return types;
	}

}
