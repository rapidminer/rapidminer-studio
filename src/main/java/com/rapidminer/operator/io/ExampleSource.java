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
import java.io.IOException;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;

import org.xml.sax.SAXException;

import com.rapidminer.example.Attribute;
import com.rapidminer.example.ExampleSet;
import com.rapidminer.example.table.DataRowFactory;
import com.rapidminer.example.table.ExampleTable;
import com.rapidminer.example.table.FileDataRowReader;
import com.rapidminer.example.table.MemoryExampleTable;
import com.rapidminer.gui.wizards.ExampleSourceConfigurationWizardCreator;
import com.rapidminer.operator.OperatorDescription;
import com.rapidminer.operator.OperatorException;
import com.rapidminer.operator.UserError;
import com.rapidminer.operator.generator.ExampleSetGenerator;
import com.rapidminer.operator.ports.metadata.AttributeMetaData;
import com.rapidminer.operator.ports.metadata.ExampleSetMetaData;
import com.rapidminer.operator.ports.metadata.MetaData;
import com.rapidminer.parameter.ParameterType;
import com.rapidminer.parameter.ParameterTypeAttributeFile;
import com.rapidminer.parameter.ParameterTypeBoolean;
import com.rapidminer.parameter.ParameterTypeCategory;
import com.rapidminer.parameter.ParameterTypeConfiguration;
import com.rapidminer.parameter.ParameterTypeDouble;
import com.rapidminer.parameter.ParameterTypeInt;
import com.rapidminer.parameter.ParameterTypeString;
import com.rapidminer.parameter.conditions.BooleanParameterCondition;
import com.rapidminer.tools.RandomGenerator;
import com.rapidminer.tools.XMLException;
import com.rapidminer.tools.XMLParserException;
import com.rapidminer.tools.att.AttributeDataSource;
import com.rapidminer.tools.att.AttributeDataSources;
import com.rapidminer.tools.att.AttributeSet;
import com.rapidminer.tools.io.Encoding;


/**
 * <p>
 * This operator reads an example set from (a) file(s). Probably you can use the default parameter
 * values for the most file formats (including the format produced by the ExampleSetWriter, CSV,
 * ...). Please refer to section {@rapidminer.ref sec:inputfiles|First steps/File formats} for
 * details on the attribute description file set by the parameter <var>attributes</var> used to
 * specify attribute types. You can use the wizard of this operator or the tool Attribute Editor in
 * order to create those meta data .aml files for your datasets.
 * </p>
 *
 * <p>
 * This operator supports the reading of data from multiple source files. Each attribute (including
 * special attributes like labels, weights, ...) might be read from another file. Please note that
 * only the minimum number of lines of all files will be read, i.e. if one of the data source files
 * has less lines than the others, only this number of examples will be read.
 * </p>
 *
 * <p>
 * The split points can be defined with regular expressions (please refer to the annex of the
 * RapidMiner tutorial for an overview). The default split parameter &quot;,\s*|;\s*|\s+&quot;
 * should work for most file formats. This regular expression describes the following column
 * separators
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
 * Quoting is also possible with &quot;. You can escape quotes with a backslash, i.e. \&quot;.
 * Please note that you can change these characters by adjusting the corresponding settings.
 * </p>
 *
 * <p>
 * Additionally you can specify comment characters which can be used at arbitrary locations of the
 * data lines. Any content after the comment character will be ignored. Unknown attribute values can
 * be marked with empty strings (if this is possible for your column separators) or by a question
 * mark (recommended).
 * </p>
 *
 * @author Simon Fischer, Ingo Mierswa
 * @deprecated since 7.3, only used by deprecated operator
 */
@Deprecated
public class ExampleSource extends AbstractExampleSource {

	/**
	 * The parameter name for &quot;Filename for the XML attribute description file. This file also
	 * contains the names of the files to read the data from.&quot;
	 */
	public static final String PARAMETER_ATTRIBUTES = "attributes";

	static {
		AbstractReader.registerReaderDescription(new ReaderDescription("aml", ExampleSource.class, PARAMETER_ATTRIBUTES));
	}

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

	/** The parameter name for &quot;Indicates if the loaded data should be permuted.&quot; */
	public static final String PARAMETER_PERMUTATE = "permute";

	/** The parameter name for &quot;Column separators for data files (regular expression)&quot; */
	public static final String PARAMETER_COLUMN_SEPARATORS = "column_separators";

	/** The parameter name for &quot;Indicates if a comment character should be used&quot; */
	public static final String PARAMETER_USE_COMMENT_CHARACTERS = "use_comment_characters";

	/** The parameter name for &quot;Lines beginning with these characters are ignored.&quot; */
	public static final String PARAMETER_COMMENT_CHARS = "comment_chars";

	/** The parameter name for &quot;Character that is used as decimal point.&quot; */
	public static final String PARAMETER_DECIMAL_POINT_CHARACTER = "decimal_point_character";

	/** The parameter name for &quot;Indicates if quotes should be regarded (slower!).&quot; */
	public static final String PARAMETER_USE_QUOTES = "use_quotes";

	/** Specifies the used quoting character. */
	public static final String PARAMETER_QUOTE_CHARACTER = "quote_character";

	/** Specifies the used character for escaping quoting. */
	public static final String PARAMETER_QUOTING_ESCAPE_CHARACTER = "quoting_escape_character";

	/** Indicates if the lines should be trimmed during reading. */
	public static final String PARAMETER_TRIM_LINES = "trim_lines";

	/** Indicates if lines leading to errors should be skipped. */
	public static final String PARAMETER_SKIP_ERROR_LINES = "skip_error_lines";

	/** The parameter name for &quot;Determines, how the data is represented internally.&quot; */
	public static final String PARAMETER_DATAMANAGEMENT = ExampleSetGenerator.PARAMETER_DATAMANAGEMENT;

	public ExampleSource(final OperatorDescription description) {
		super(description);
	}

	@Override
	public MetaData getGeneratedMetaData() throws OperatorException {
		getLogger().fine("Generating meta data for " + this.getName());
		File attributeFile = getParameterAsFile(PARAMETER_ATTRIBUTES);
		if (attributeFile == null) {
			return getDefaultMetaData();
		}
		AttributeDataSources attributeDataSources;
		try {
			attributeDataSources = AttributeDataSource.createAttributeDataSources(attributeFile, true, this);
		} catch (XMLParserException | XMLException e) {
			throw new UserError(this, e, 401, e.getMessage());
		} catch (IOException e) {
			throw new UserError(this, e, 302, attributeFile, e.getMessage());
		} catch (SAXException e) {
			throw new UserError(this, e, 401, e.toString());
		}
		AttributeSet attributeSet = new AttributeSet(attributeDataSources);
		ExampleSetMetaData emd = new ExampleSetMetaData();
		for (Map.Entry<String, Attribute> entry : attributeSet.getSpecialAttributes().entrySet()) {
			AttributeMetaData a = new AttributeMetaData(entry.getValue());
			a.setRole(entry.getKey());
			emd.addAttribute(a);
		}
		for (Attribute attribute : attributeSet.getRegularAttributes()) {
			emd.addAttribute(new AttributeMetaData(attribute));
		}

		return emd;
	}

	/** @return {@code true} iff an attributes file is specified */
	@Override
	protected boolean isMetaDataCacheable() {
		try {
			return getParameterAsFile(PARAMETER_ATTRIBUTES) != null;
		} catch (UserError userError) {
			return true;
		}
	}

	@Override
	public ExampleSet createExampleSet() throws OperatorException {

		AttributeDataSources attributeDataSources = null;
		FileDataRowReader reader = null;

		File attributeFile = getParameterAsFile(PARAMETER_ATTRIBUTES);
		try {
			attributeDataSources = AttributeDataSource.createAttributeDataSources(attributeFile, true, this);
			char[] commentCharacters = null;
			if (getParameterAsBoolean(PARAMETER_USE_COMMENT_CHARACTERS)) {
				commentCharacters = getParameterAsString(PARAMETER_COMMENT_CHARS).toCharArray();
			}
			reader = new FileDataRowReader(
					new DataRowFactory(getParameterAsInt(PARAMETER_DATAMANAGEMENT),
							getParameterAsString(PARAMETER_DECIMAL_POINT_CHARACTER).charAt(0)),
					attributeDataSources.getDataSources(), getParameterAsDouble(PARAMETER_SAMPLE_RATIO),
					getParameterAsInt(PARAMETER_SAMPLE_SIZE), getParameterAsString(PARAMETER_COLUMN_SEPARATORS),
					commentCharacters, getParameterAsBoolean(PARAMETER_USE_QUOTES),
					getParameterAsString(PARAMETER_QUOTE_CHARACTER).charAt(0),
					getParameterAsString(PARAMETER_QUOTING_ESCAPE_CHARACTER).charAt(0),
					getParameterAsBoolean(PARAMETER_TRIM_LINES), getParameterAsBoolean(PARAMETER_SKIP_ERROR_LINES),
					// only null if old version of description format: Then emulate old behavior
					// using root operator
					attributeDataSources.getEncoding() == null ? Encoding.getEncoding(this)
							: attributeDataSources.getEncoding(),
					RandomGenerator.getRandomGenerator(
							getParameterAsBoolean(RandomGenerator.PARAMETER_USE_LOCAL_RANDOM_SEED),
							getParameterAsInt(RandomGenerator.PARAMETER_LOCAL_RANDOM_SEED)));
		} catch (XMLParserException | XMLException e) {
			throw new UserError(this, e, 401, e.getMessage());
		} catch (IOException e) {
			throw new UserError(this, e, 302, attributeFile, e.getMessage());
		} catch (SAXException e) {
			throw new UserError(this, e, 401, e.toString());
		}

		AttributeSet attributeSet = new AttributeSet(attributeDataSources);

		ExampleTable table = new MemoryExampleTable(attributeSet.getAllAttributes(), reader,
				getParameterAsBoolean(PARAMETER_PERMUTATE));
		return table.createExampleSet(attributeSet);
	}

	@Override
	protected boolean supportsEncoding() {
		return true;
	}

	@Override
	public List<ParameterType> getParameterTypes() {
		List<ParameterType> types = new LinkedList<>();
		ParameterType type = new ParameterTypeConfiguration(ExampleSourceConfigurationWizardCreator.class, this);
		type.setExpert(false);
		types.add(type);
		types.add(new ParameterTypeAttributeFile(PARAMETER_ATTRIBUTES,
				"Filename for the xml attribute description file. This file also contains the names of the files to read the data from.",
				false));
		type = new ParameterTypeDouble(PARAMETER_SAMPLE_RATIO,
				"The fraction of the data set which should be read (1 = all; only used if sample_size = -1)", 0.0d, 1.0d,
				1.0d);
		type.setExpert(false);
		types.add(type);
		types.add(new ParameterTypeInt(PARAMETER_SAMPLE_SIZE,
				"The exact number of samples which should be read (-1 = use sample ratio; if not -1, sample_ratio will not have any effect)",
				-1, Integer.MAX_VALUE, -1));
		types.add(
				new ParameterTypeBoolean(PARAMETER_PERMUTATE, "Indicates if the loaded data should be permutated.", false));

		types.add(
				new ParameterTypeString(PARAMETER_DECIMAL_POINT_CHARACTER, "Character that is used as decimal point.", "."));

		types.add(new ParameterTypeString(PARAMETER_COLUMN_SEPARATORS,
				"Column separators for data files (regular expression)", ",\\s*|;\\s*|\\s+"));

		types.add(new ParameterTypeBoolean(PARAMETER_USE_COMMENT_CHARACTERS,
				"Indicates if a comment character should be used.", true));
		type = new ParameterTypeString(PARAMETER_COMMENT_CHARS,
				"Any content in a line after one of these characters will be ignored.", "#");
		type.registerDependencyCondition(new BooleanParameterCondition(this, PARAMETER_USE_COMMENT_CHARACTERS, false, true));
		types.add(type);

		types.add(new ParameterTypeBoolean(PARAMETER_USE_QUOTES, "Indicates if quotes should be regarded.", true));
		type = new ParameterTypeString(PARAMETER_QUOTE_CHARACTER,
				"Specifies the character which should be used for quoting.", "\"");
		type.registerDependencyCondition(new BooleanParameterCondition(this, PARAMETER_USE_QUOTES, false, true));
		types.add(type);

		type = new ParameterTypeString(PARAMETER_QUOTING_ESCAPE_CHARACTER,
				"Specifies the character which should be used for escape the quoting.", "\\");
		type.registerDependencyCondition(new BooleanParameterCondition(this, PARAMETER_USE_QUOTES, false, true));
		types.add(type);

		types.add(new ParameterTypeBoolean(PARAMETER_TRIM_LINES,
				"Indicates if lines should be trimmed (empty spaces are removed at the beginning and the end) before the column split is performed.",
				false));

		types.add(new ParameterTypeBoolean(PARAMETER_SKIP_ERROR_LINES,
				"Indicates if lines which can not be read should be skipped instead of letting this operator fail its execution.",
				false));

		types.add(new ParameterTypeCategory(PARAMETER_DATAMANAGEMENT, "Determines, how the data is represented internally.",
				DataRowFactory.TYPE_NAMES, DataRowFactory.TYPE_DOUBLE_ARRAY));

		types.addAll(super.getParameterTypes());
		types.addAll(RandomGenerator.getRandomGeneratorParameters(this));

		return types;
	}
}
