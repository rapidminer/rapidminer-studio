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
import java.io.FileNotFoundException;
import java.io.IOException;
import java.nio.charset.Charset;
import java.text.NumberFormat;
import java.util.Iterator;
import java.util.List;

import com.rapidminer.example.Attributes;
import com.rapidminer.example.ExampleSet;
import com.rapidminer.example.table.DataRowFactory;
import com.rapidminer.example.table.FileDataRowReader;
import com.rapidminer.example.utils.ExampleSetBuilder;
import com.rapidminer.example.utils.ExampleSets;
import com.rapidminer.gui.tools.dialogs.wizards.dataimport.csv.CSVFileReader;
import com.rapidminer.operator.OperatorDescription;
import com.rapidminer.operator.OperatorException;
import com.rapidminer.operator.UserError;
import com.rapidminer.operator.ports.metadata.MetaData;
import com.rapidminer.parameter.ParameterType;
import com.rapidminer.parameter.ParameterTypeBoolean;
import com.rapidminer.parameter.ParameterTypeChar;
import com.rapidminer.parameter.ParameterTypeDouble;
import com.rapidminer.parameter.ParameterTypeFile;
import com.rapidminer.parameter.ParameterTypeInt;
import com.rapidminer.parameter.ParameterTypeString;
import com.rapidminer.parameter.UndefinedParameterError;
import com.rapidminer.parameter.conditions.BooleanParameterCondition;
import com.rapidminer.tools.LineParser;
import com.rapidminer.tools.RandomGenerator;
import com.rapidminer.tools.StrictDecimalFormat;
import com.rapidminer.tools.att.AttributeDataSource;
import com.rapidminer.tools.att.AttributeDataSourceCreator;
import com.rapidminer.tools.att.AttributeDataSources;
import com.rapidminer.tools.att.AttributeSet;
import com.rapidminer.tools.io.Encoding;


/**
 * <p>
 * This operator reads an example set from (a) file(s). Probably you can use the default parameter
 * values for the most file formats (including the format produced by the ExampleSetWriter, CSV,
 * ...). In fact, in many cases this operator is more appropriate for CSV based file formats than
 * the {@link CSVExampleSource} operator itself since you can better control some of the necessary
 * settings like column separators etc.
 * </p>
 *
 * <p>
 * In contrast to the usual ExampleSource operator this operator is able to read the attribute names
 * from the first line of the data file. However, there is one restriction: the data can only be
 * read from one file instead of multiple files. If you need a fully flexible operator for data
 * loading you should use the more powerful ExampleSource operator which also provides more
 * parameters tuning for example the quoting mechanism and other specialized settings.
 * </p>
 *
 * <p>
 * The column split points can be defined with regular expressions (please refer to the annex of the
 * RapidMiner tutorial). The default split parameter &quot;,\s*|;\s*|\s+&quot; should work for most
 * file formats. This regular expression describes the following column separators
 * <ul>
 * <li>the character &quot;,&quot; followed by a whitespace of arbitrary length (also no white
 * space)</li>
 * <li>the character &quot;;&quot; followed by a whitespace of arbitrary length (also no white
 * space)</li>
 * <li>a whitespace of arbitrary length (min. 1)</li>
 * </ul>
 * A logical XOR is defined by &quot;|&quot;. Other useful separators might be &quot;\t&quot; for
 * tabulars, &quot; &quot; for a single whitespace, and &quot;\s&quot; for any whitespace.
 * </p>
 *
 * <p>
 * Quoting is also possible with &quot;. Escaping a quote is done with \&quot;. Additionally you can
 * specify comment characters which can be used at arbitrary locations of the data lines and will
 * skip the remaining part of the lines. Unknown attribute values can be marked with empty strings
 * or a question mark.
 * </p>
 *
 * @rapidminer.index csv
 * @author Ingo Mierswa
 */

@Deprecated
public class SimpleExampleSource extends AbstractExampleSource {

	/**
	 * The parameter name for &quot;Name of the label attribute (if empty, the column defined by
	 * label_column will be used)&quot;
	 */
	public static final String PARAMETER_LABEL_NAME = "label_name";

	/**
	 * The parameter name for &quot;Column number of the label attribute (only used if label_name is
	 * empty; 0 = none; negative values are counted from the last column)&quot;
	 */
	public static final String PARAMETER_LABEL_COLUMN = "label_column";

	/**
	 * The parameter name for &quot;Name of the id attribute (if empty, the column defined by
	 * id_column will be used)&quot;
	 */
	public static final String PARAMETER_ID_NAME = "id_name";

	/**
	 * The parameter name for &quot;Column number of the id attribute (only used if id_name is
	 * empty; 0 = none; negative values are counted from the last column)&quot;
	 */
	public static final String PARAMETER_ID_COLUMN = "id_column";

	/**
	 * The parameter name for &quot;Name of the weight attribute (if empty, the column defined by
	 * weight_column will be used)&quot;
	 */
	public static final String PARAMETER_WEIGHT_NAME = "weight_name";

	/**
	 * The parameter name for &quot;Column number of the weight attribute (only used if weight_name
	 * is empty; 0 = none, negative values are counted from the last column)&quot;
	 */
	public static final String PARAMETER_WEIGHT_COLUMN = "weight_column";

	/**
	 * The parameter name for &quot;The fraction of the data set which should be read (1 = all; only
	 * used if sample_size = -1)&quot;
	 */
	public static final String PARAMETER_SAMPLE_RATIO = "sample_ratio";

	/**
	 * The parameter name for &quot;The exact number of samples which should be read (-1 = use
	 * sample ratio; if not -1, sample_ratio will not have any effect)&quot;
	 */
	public static final String PARAMETER_SAMPLE_SIZE = "sample_size";

	public static final String PARAMETER_FILENAME = "file_name";

	public static final String PARAMETER_USE_FIRST_ROW_AS_ATTRIBUTE_NAMES = "use_first_row_as_attribute_names";

	public static final String PARAMETER_TRIM_LINES = "trim_lines";

	public static final String PARAMETER_SKIP_COMMENTS = "skip_comments";

	public static final String PARAMETER_COMMENT_CHARS = "comment_characters";

	public static final String PARAMETER_USE_QUOTES = "use_quotes";

	public static final String PARAMETER_QUOTES_CHARACTER = "quotes_character";

	public static final String PARAMETER_COLUMN_SEPARATORS = "column_separators";

	public SimpleExampleSource(OperatorDescription description) {
		super(description);
	}

	public CSVFileReader createReader(File file) throws UndefinedParameterError {
		final LineParser parser = new LineParser();
		parser.setTrimLine(getParameterAsBoolean(PARAMETER_TRIM_LINES));
		parser.setSkipComments(getParameterAsBoolean(PARAMETER_SKIP_COMMENTS));
		try {
			parser.setSplitExpression(getParameterAsString(PARAMETER_COLUMN_SEPARATORS));
		} catch (OperatorException e) {
			throw new UndefinedParameterError(e.getMessage());
		}
		parser.setUseQuotes(getParameterAsBoolean(PARAMETER_USE_QUOTES));
		parser.setQuoteCharacter(getParameterAsChar(PARAMETER_QUOTES_CHARACTER));
		parser.setCommentCharacters(getParameterAsString(PARAMETER_COMMENT_CHARS));
		final NumberFormat numberFormat = StrictDecimalFormat.getInstance(this);
		final CSVFileReader reader = new CSVFileReader(file,
				getParameterAsBoolean(PARAMETER_USE_FIRST_ROW_AS_ATTRIBUTE_NAMES), parser, numberFormat);
		return reader;
	}

	@Override
	public MetaData getGeneratedMetaData() throws OperatorException {
		File file = getParameterAsFile(PARAMETER_FILENAME);
		CSVFileReader reader = createReader(getParameterAsFile(PARAMETER_FILENAME));
		try {
			return reader.getMetaData();
		} catch (FileNotFoundException e) {
			throw new UserError(this, 302, file, e.getMessage());
		} catch (IOException e) {
		}
		return getDefaultMetaData();
	}

	/** @return {@code true} */
	@Override
	protected boolean isMetaDataCacheable() {
		return true;
	}

	@Override
	public ExampleSet createExampleSet() throws OperatorException {
		File file = getParameterAsFile(PARAMETER_FILENAME);
		final CSVFileReader reader = createReader(file);
		ExampleSet result = null;
		try {
			result = reader.createExampleSet();
		} catch (IOException e) {

		}
		return result;
	}

	public static ExampleSet createExampleSet(File file, boolean firstRowAsColumnNames, double sampleRatio, int maxLines,
			String separatorRegExpr, char[] comments, int dataRowType, boolean useQuotes, boolean trimLines,
			boolean skipErrorLines, char decimalPointCharacter, Charset encoding, String labelName, int labelColumn,
			String idName, int idColumn, String weightName, int weightColumn) throws IOException, UserError,
			IndexOutOfBoundsException {
		// create attribute data sources and guess value types (performs a data scan)
		AttributeDataSourceCreator adsCreator = new AttributeDataSourceCreator();

		adsCreator.loadData(file, comments, separatorRegExpr, decimalPointCharacter, useQuotes, '"', '\\', trimLines,
				firstRowAsColumnNames, -1, skipErrorLines, encoding, null);

		List<AttributeDataSource> attributeDataSources = adsCreator.getAttributeDataSources();

		// set special attributes
		resetAttributeType(attributeDataSources, labelName, labelColumn, Attributes.LABEL_NAME);
		resetAttributeType(attributeDataSources, idName, idColumn, Attributes.ID_NAME);
		resetAttributeType(attributeDataSources, weightName, weightColumn, Attributes.WEIGHT_NAME);

		// read data
		FileDataRowReader reader = new FileDataRowReader(new DataRowFactory(dataRowType, decimalPointCharacter),
				attributeDataSources, sampleRatio, maxLines, separatorRegExpr, comments, useQuotes, '"', '\\', trimLines,
				skipErrorLines, encoding, RandomGenerator.getGlobalRandomGenerator());
		if (firstRowAsColumnNames) {
			reader.skipLine();
		}

		AttributeSet attributeSet = new AttributeSet(new AttributeDataSources(attributeDataSources, file, encoding));

		// create table and example set
		ExampleSetBuilder builder = ExampleSets.from(attributeSet.getAllAttributes()).withDataRowReader(reader);
		attributeSet.getSpecialAttributes().entrySet().stream()
				.forEach(entry -> builder.withRole(entry.getValue(), entry.getKey()));
		return builder.build();
	}

	private static void resetAttributeType(List<AttributeDataSource> attributeDataSources, String attribute, int column,
			String typeName) throws IndexOutOfBoundsException {
		if (attribute == null || attribute.length() == 0) {
			if (column != 0) {
				if (column < 0) {
					column = attributeDataSources.size() + column + 1;
				}
				if (column < 1 || column >= attributeDataSources.size() + 1) {
					throw new IndexOutOfBoundsException("column = " + column + " as label");
				}
				column--;
				attributeDataSources.get(column).setType(typeName);
			}
		} else {
			Iterator<AttributeDataSource> i = attributeDataSources.iterator();
			while (i.hasNext()) {
				AttributeDataSource ads = i.next();
				if (ads.getAttribute().getName().equals(attribute)) {
					ads.setType(typeName);
					break;
				}
			}
		}
	}

	@Override
	public List<ParameterType> getParameterTypes() {
		List<ParameterType> types = super.getParameterTypes();
		types.add(new ParameterTypeFile(PARAMETER_FILENAME, "Name of the file to read the data from.", "dat", false));
		types.addAll(Encoding.getParameterTypes(this));
		types.add(new ParameterTypeBoolean(
				PARAMETER_TRIM_LINES,
				"Indicates if lines should be trimmed (empty spaces are removed at the beginning and the end) before the column split is performed.",
				false));
		types.add(new ParameterTypeBoolean(PARAMETER_SKIP_COMMENTS, "Indicates if qa comment character should be used.",
				true));
		ParameterType type = new ParameterTypeString(PARAMETER_COMMENT_CHARS,
				"Lines beginning with these characters are ignored.", "#", true);
		type.registerDependencyCondition(new BooleanParameterCondition(this, PARAMETER_SKIP_COMMENTS, false, true));
		types.add(type);
		type = new ParameterTypeBoolean(PARAMETER_USE_FIRST_ROW_AS_ATTRIBUTE_NAMES,
				"Read attribute names from file (assumes the attribute names are in the first line of the file).", false);
		type.setExpert(false);
		types.add(type);
		types.add(new ParameterTypeBoolean(PARAMETER_USE_QUOTES, "Indicates if quotes should be regarded (slower!).", false));
		type = new ParameterTypeChar(PARAMETER_QUOTES_CHARACTER, "The quotes character.", '"', true);
		type.registerDependencyCondition(new BooleanParameterCondition(this, PARAMETER_USE_QUOTES, false, true));
		types.add(type);
		types.add(new ParameterTypeString(PARAMETER_COLUMN_SEPARATORS,
				"Column separators for data files (regular expression)", ",\\s*|;\\s*|\\s+"));
		types.addAll(StrictDecimalFormat.getParameterTypes(this));

		type = new ParameterTypeString(PARAMETER_LABEL_NAME,
				"Name of the label attribute (if empty, the column defined by label_column will be used)", true);
		type.setExpert(false);
		types.add(type);
		type = new ParameterTypeInt(
				PARAMETER_LABEL_COLUMN,
				"Column number of the label attribute (only used if label_name is empty; 0 = none; negative values are counted from the last column)",
				Integer.MIN_VALUE, Integer.MAX_VALUE, 0);
		type.setExpert(false);
		types.add(type);
		types.add(new ParameterTypeString(PARAMETER_ID_NAME,
				"Name of the id attribute (if empty, the column defined by id_column will be used)", true));
		types.add(new ParameterTypeInt(
				PARAMETER_ID_COLUMN,
				"Column number of the id attribute (only used if id_name is empty; 0 = none; negative values are counted from the last column)",
				Integer.MIN_VALUE, Integer.MAX_VALUE, 0));
		types.add(new ParameterTypeString(PARAMETER_WEIGHT_NAME,
				"Name of the weight attribute (if empty, the column defined by weight_column will be used)", true));
		types.add(new ParameterTypeInt(
				PARAMETER_WEIGHT_COLUMN,
				"Column number of the weight attribute (only used if weight_name is empty; 0 = none, negative values are counted from the last column)",
				Integer.MIN_VALUE, Integer.MAX_VALUE, 0));
		types.add(new ParameterTypeDouble(PARAMETER_SAMPLE_RATIO,
				"The fraction of the data set which should be read (1 = all; only used if sample_size = -1)", 0.0d, 1.0d,
				1.0d));
		types.add(new ParameterTypeInt(
				PARAMETER_SAMPLE_SIZE,
				"The exact number of samples which should be read (-1 = use sample ratio; if not -1, sample_ratio will not have any effect)",
				-1, Integer.MAX_VALUE, -1));
		return types;
	}
}
