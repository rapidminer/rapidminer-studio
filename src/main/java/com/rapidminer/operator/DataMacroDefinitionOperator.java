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
package com.rapidminer.operator;

import java.util.Iterator;
import java.util.LinkedList;
import java.util.List;

import com.rapidminer.example.Attribute;
import com.rapidminer.example.Example;
import com.rapidminer.example.ExampleSet;
import com.rapidminer.example.Statistics;
import com.rapidminer.operator.error.AttributeNotFoundError;
import com.rapidminer.operator.meta.FeatureIterator;
import com.rapidminer.operator.ports.InputPort;
import com.rapidminer.operator.ports.OutputPort;
import com.rapidminer.operator.ports.metadata.AttributeSetPrecondition;
import com.rapidminer.operator.ports.metadata.ParameterConditionedPrecondition;
import com.rapidminer.operator.ports.metadata.PassThroughRule;
import com.rapidminer.parameter.ParameterType;
import com.rapidminer.parameter.ParameterTypeAttribute;
import com.rapidminer.parameter.ParameterTypeCategory;
import com.rapidminer.parameter.ParameterTypeInt;
import com.rapidminer.parameter.ParameterTypeList;
import com.rapidminer.parameter.ParameterTypeString;
import com.rapidminer.parameter.UndefinedParameterError;
import com.rapidminer.parameter.conditions.EqualTypeCondition;
import com.rapidminer.tools.Ontology;
import com.rapidminer.tools.Tools;


/**
 * <p>
 * (Re-)Define macros for the current process. Macros will be replaced in the value strings of
 * parameters by the macro values defined as a parameter of this operator. In contrast to the usual
 * {@link MacroDefinitionOperator}, this operator supports the definition of a single macro from
 * properties of a given input example set, e.g. from properties like the number of examples or
 * attributes or from a specific data value.
 * </p>
 *
 * <p>
 * You have to define the macro name (without the enclosing brackets) and the macro value. The
 * defined macro can then be used in all succeeding operators as parameter value for string type
 * parameters. A macro must then be enclosed by &quot;MACRO_START&quot; and &quot;MACRO_END&quot;.
 * </p>
 *
 * <p>
 * There are several predefined macros:
 * </p>
 * <ul>
 * <li>MACRO_STARTprocess_nameMACRO_END: will be replaced by the name of the process (without path
 * and extension)</li>
 * <li>MACRO_STARTprocess_fileMACRO_END: will be replaced by the file name of the process (with
 * extension)</li>
 * <li>MACRO_STARTprocess_pathMACRO_END: will be replaced by the complete absolute path of the
 * process file</li>
 * </ul>
 *
 * <p>
 * In addition to those the user might define arbitrary other macros which will be replaced by
 * arbitrary string during the process run. Please note also that several other short macros exist,
 * e.g. MACRO_STARTaMACRO_END for the number of times the current operator was applied. Please refer
 * to the section about macros in the RapidMiner tutorial. Please note also that other operators
 * like the {@link FeatureIterator} also add specific macros.
 * </p>
 *
 * @author Ingo Mierswa
 */
public class DataMacroDefinitionOperator extends Operator {

	private InputPort exampleSetInput = getInputPorts().createPort("example set", ExampleSet.class);
	private OutputPort exampleSetOutput = getOutputPorts().createPort("example set");

	/** The parameter name for &quot;The values of the user defined macros.&quot; */
	public static final String PARAMETER_MACRO = "macro";

	public static final String PARAMETER_MACRO_TYPE = "macro_type";

	public static final String PARAMETER_EXAMPLE_INDEX = "example_index";

	public static final String PARAMETER_ATTRIBUTE_NAME = "attribute_name";

	public static final String PARAMETER_ATTRIBUTE_VALUE = "attribute_value";

	public static final String PARAMETER_STATISTICS = "statistics";

	public static final String PARAMETER_LIST_MACROS = "additional_macros";

	public static final String[] MACRO_TYPES = new String[] { "number_of_examples", "number_of_attributes", "data_value",
	"statistics" };

	public static final int MACRO_TYPE_EXAMPLES = 0;
	public static final int MACRO_TYPE_ATTRIBUTES = 1;
	public static final int MACRO_TYPE_DATA = 2;
	public static final int MACRO_TYPE_STATISTICS = 3;

	public static final String[] STATISTICS_TYPES = new String[] { "average", "deviation", "variance", "min", "max",
		"count", "unknown" };

	public static final int STATISTICS_TYPE_AVERAGE = 0;
	public static final int STATISTICS_TYPE_DEVIATION = 1;
	public static final int STATISTICS_TYPE_VARIANCE = 2;
	public static final int STATISTICS_TYPE_MIN = 3;
	public static final int STATISTICS_TYPE_MAX = 4;
	public static final int STATISTICS_TYPE_COUNT = 5;
	public static final int STATISTICS_TYPE_UNKNOWN = 6;

	/** The last defined macro. */
	private String macroValue = null;

	public DataMacroDefinitionOperator(OperatorDescription description) {
		super(description);

		exampleSetInput.addPrecondition(new ParameterConditionedPrecondition(exampleSetInput, new AttributeSetPrecondition(
				exampleSetInput, AttributeSetPrecondition.getAttributesByParameter(this, PARAMETER_ATTRIBUTE_NAME)), this,
				PARAMETER_MACRO_TYPE, MACRO_TYPES[MACRO_TYPE_DATA]));
		exampleSetInput.addPrecondition(new ParameterConditionedPrecondition(exampleSetInput, new AttributeSetPrecondition(
				exampleSetInput, AttributeSetPrecondition.getAttributesByParameter(this, PARAMETER_ATTRIBUTE_NAME)), this,
				PARAMETER_MACRO_TYPE, MACRO_TYPES[MACRO_TYPE_STATISTICS]));

		getTransformer().addRule(new PassThroughRule(exampleSetInput, exampleSetOutput, false));

		addValue(new ValueString("macro_name", "The name of the macro.") {

			@Override
			public String getStringValue() {
				try {
					return getParameterAsString(PARAMETER_MACRO);
				} catch (UndefinedParameterError e) {
					return null;
				}
			}
		});

		addValue(new ValueString("macro_value", "The value of the macro.") {

			@Override
			public String getStringValue() {
				return macroValue;
			}
		});
	}

	@Override
	public void doWork() throws OperatorException {
		ExampleSet exampleSet = exampleSetInput.getData(ExampleSet.class);

		List<String> listOfMacroNames = new LinkedList<>();
		List<String> listOfMacroValues = new LinkedList<>();
		String macroName = getParameterAsString(PARAMETER_MACRO);
		this.macroValue = null;

		int macroType = getParameterAsInt(PARAMETER_MACRO_TYPE);
		switch (macroType) {
			case MACRO_TYPE_ATTRIBUTES:
				macroValue = exampleSet.getAttributes().size() + "";
				break;
			case MACRO_TYPE_EXAMPLES:
				macroValue = exampleSet.size() + "";
				break;
			case MACRO_TYPE_DATA:
				int exampleIndex = getParameterAsInt(PARAMETER_EXAMPLE_INDEX);
				if (exampleIndex == 0) {
					throw new UserError(this, 207, new Object[] { "0", PARAMETER_EXAMPLE_INDEX,
					"only positive or negative indices are allowed" });
				}

				if (exampleIndex < 0) {
					exampleIndex = exampleSet.size() + exampleIndex;
				} else {
					exampleIndex--;
				}

				if (exampleIndex >= exampleSet.size()) {
					throw new UserError(this, 110, exampleIndex + 1);
				}

				// iterate over all defined macro pairs and add them to the lists
				Iterator<String[]> j = getParameterList(PARAMETER_LIST_MACROS).iterator();
				while (j.hasNext()) {
					String[] macronameAttributePair = j.next();
					String nameOfMacro = macronameAttributePair[0];
					String nameOfAttr = macronameAttributePair[1];

					if (nameOfAttr == null) {
						throw new AttributeNotFoundError(this, PARAMETER_LIST_MACROS, "");
					}
					Attribute attribute = exampleSet.getAttributes().get(nameOfAttr);
					if (attribute == null) {
						throw new AttributeNotFoundError(this, PARAMETER_LIST_MACROS, nameOfAttr);
					}

					Example example = exampleSet.getExample(exampleIndex);
					if (attribute.isNumerical()) {
						macroValue = Tools.formatIntegerIfPossible(example.getValue(attribute));
					} else {
						macroValue = example.getValueAsString(attribute);
					}
					// add name and value to the respective lists
					listOfMacroNames.add(nameOfMacro);
					listOfMacroValues.add(macroValue);

					checkForStop();
				}

				// to ensure compatibility with the old version with no macro list, we need to add
				// the original single macro parameter value if it
				// exists
				if (!"".equals(macroName)) {
					String parameterString = getParameterAsString(PARAMETER_ATTRIBUTE_NAME);
					if (parameterString == null) {
						throw new AttributeNotFoundError(this, PARAMETER_ATTRIBUTE_NAME, "");
					}
					Attribute attribute = exampleSet.getAttributes().get(parameterString);
					if (attribute == null) {
						throw new AttributeNotFoundError(this, PARAMETER_ATTRIBUTE_NAME,
								getParameterAsString(PARAMETER_ATTRIBUTE_NAME));
					}

					Example example = exampleSet.getExample(exampleIndex);
					if (attribute.isNumerical()) {
						macroValue = Tools.formatIntegerIfPossible(example.getValue(attribute));
					} else {
						macroValue = example.getValueAsString(attribute);
					}

					listOfMacroNames.add(macroName);
					listOfMacroValues.add(macroValue);
				}

				break;
			case MACRO_TYPE_STATISTICS:
				String parameterString = getParameterAsString(PARAMETER_ATTRIBUTE_NAME);
				if (parameterString == null) {
					throw new AttributeNotFoundError(this, PARAMETER_ATTRIBUTE_NAME, "");
				}
				Attribute attribute = exampleSet.getAttributes().get(parameterString);
				if (attribute == null) {
					throw new AttributeNotFoundError(this, PARAMETER_ATTRIBUTE_NAME,
							getParameterAsString(PARAMETER_ATTRIBUTE_NAME));
				}

				exampleSet.recalculateAttributeStatistics(attribute);

				int statisticsType = getParameterAsInt(PARAMETER_STATISTICS);
				switch (statisticsType) {
					case STATISTICS_TYPE_AVERAGE:
						if (attribute.isNominal()) {
							macroValue = attribute.getMapping().mapIndex(
									(int) exampleSet.getStatistics(attribute, Statistics.MODE));
						} else {
							macroValue = exampleSet.getStatistics(attribute, Statistics.AVERAGE) + "";
						}
						break;
					case STATISTICS_TYPE_DEVIATION:
						if (!attribute.isNominal()) {
							macroValue = Math.sqrt(exampleSet.getStatistics(attribute, Statistics.VARIANCE)) + "";
						} else {
							throw new UserError(this, 120, new Object[] { attribute.getName(),
									Ontology.VALUE_TYPE_NAMES[attribute.getValueType()],
									Ontology.VALUE_TYPE_NAMES[Ontology.NUMERICAL] });
						}
						break;
					case STATISTICS_TYPE_VARIANCE:
						if (!attribute.isNominal()) {
							macroValue = exampleSet.getStatistics(attribute, Statistics.VARIANCE) + "";
						} else {
							throw new UserError(this, 120, new Object[] { attribute.getName(),
									Ontology.VALUE_TYPE_NAMES[attribute.getValueType()],
									Ontology.VALUE_TYPE_NAMES[Ontology.NUMERICAL] });
						}
						break;
					case STATISTICS_TYPE_MAX:
						if (attribute.isNominal()) {
							macroValue = attribute.getMapping().mapIndex(
									(int) exampleSet.getStatistics(attribute, Statistics.MAXIMUM));
						} else {
							macroValue = exampleSet.getStatistics(attribute, Statistics.MAXIMUM) + "";
						}
						break;
					case STATISTICS_TYPE_MIN:
						if (attribute.isNominal()) {
							macroValue = attribute.getMapping().mapIndex(
									(int) exampleSet.getStatistics(attribute, Statistics.MINIMUM));
						} else {
							macroValue = exampleSet.getStatistics(attribute, Statistics.MINIMUM) + "";
						}
						break;
					case STATISTICS_TYPE_COUNT:
						if (attribute.isNominal()) {
							String attributeValue = getParameterAsString(PARAMETER_ATTRIBUTE_VALUE);
							int index = attribute.getMapping().getIndex(attributeValue);
							if (index < 0) {
								throw new UserError(this, 143, attributeValue, attribute.getName());
							}
							macroValue = (int) exampleSet.getStatistics(attribute, Statistics.COUNT, attributeValue) + "";
						} else {
							throw new UserError(this, 119, attribute.getName(), getName());
						}
						break;
					case STATISTICS_TYPE_UNKNOWN:
						macroValue = exampleSet.getStatistics(attribute, Statistics.UNKNOWN) + "";
				}
				break;
		}

		// add the single macro name if we don't have a list of macro names (happens when the macro
		// type is not MACRO_TYPE_DATA)
		if (listOfMacroNames.size() <= 0) {
			listOfMacroNames.add(macroName);
		}
		// add the single macro value if we don't have a list of macro values (happens when the
		// macro type is not MACRO_TYPE_DATA)
		if (listOfMacroValues.size() <= 0) {
			listOfMacroValues.add(macroValue);
		}
		// iterate over the lists and add macro name/value pairs to the macro handler
		// both lists should always be of the same size, so just for the case they are not
		// Math.min(x,y) is used
		for (int i = 0; i < Math.min(listOfMacroNames.size(), listOfMacroValues.size()); i++) {
			String nameOfMacro = listOfMacroNames.get(i);
			String valueOfMacro = listOfMacroValues.get(i);
			// define macro
			getProcess().getMacroHandler().addMacro(nameOfMacro, valueOfMacro);
		}

		exampleSetOutput.deliver(exampleSet);
	}

	@Override
	public List<ParameterType> getParameterTypes() {
		List<ParameterType> types = super.getParameterTypes();
		types.add(new ParameterTypeString(PARAMETER_MACRO, "The macro name defined by the user.", false, false));

		ParameterType type = new ParameterTypeCategory(PARAMETER_MACRO_TYPE,
				"Indicates the way how the macro should be defined.", MACRO_TYPES, MACRO_TYPE_EXAMPLES);
		type.setExpert(false);
		types.add(type);

		type = new ParameterTypeCategory(PARAMETER_STATISTICS,
				"The statistics of the specified attribute which should be used as macro value.", STATISTICS_TYPES,
				STATISTICS_TYPE_AVERAGE);
		type.setExpert(false);
		type.registerDependencyCondition(new EqualTypeCondition(this, PARAMETER_MACRO_TYPE, MACRO_TYPES, true,
				MACRO_TYPE_STATISTICS));
		types.add(type);

		type = new ParameterTypeAttribute(PARAMETER_ATTRIBUTE_NAME,
				"The name of the attribute from which the data should be derived.", exampleSetInput, true);
		type.setExpert(false);
		type.registerDependencyCondition(new EqualTypeCondition(this, PARAMETER_MACRO_TYPE, MACRO_TYPES, true,
				MACRO_TYPE_DATA, MACRO_TYPE_STATISTICS));
		types.add(type);

		type = new ParameterTypeString(PARAMETER_ATTRIBUTE_VALUE, "The value of the attribute which should be counted.",
				true);
		type.setExpert(false);
		type.registerDependencyCondition(new EqualTypeCondition(this, PARAMETER_MACRO_TYPE, MACRO_TYPES, true,
				MACRO_TYPE_STATISTICS));
		type.registerDependencyCondition(new EqualTypeCondition(this, PARAMETER_STATISTICS, STATISTICS_TYPES, true,
				STATISTICS_TYPE_COUNT));
		types.add(type);

		type = new ParameterTypeInt(
				PARAMETER_EXAMPLE_INDEX,
				"The index of the example from which the data should be derived. This index will also be used for all attributes in the optional list of additional macros. Negative indices are counted from the end of the data set. Positive counting starts with 1, negative counting with -1.",
				-Integer.MAX_VALUE, Integer.MAX_VALUE, true);
		type.setExpert(false);
		type.registerDependencyCondition(new EqualTypeCondition(this, PARAMETER_MACRO_TYPE, MACRO_TYPES, true,
				MACRO_TYPE_DATA));
		types.add(type);

		type = new ParameterTypeList(PARAMETER_LIST_MACROS, "A list with optional additional macros.",
				new ParameterTypeString(PARAMETER_MACRO, "The macro name defined by the user."), new ParameterTypeAttribute(
						PARAMETER_ATTRIBUTE_NAME, "The name of the attribute from which the data should be derived.",
						exampleSetInput));
		type.setExpert(false);
		type.registerDependencyCondition(new EqualTypeCondition(this, PARAMETER_MACRO_TYPE, MACRO_TYPES, false,
				MACRO_TYPE_DATA));
		types.add(type);

		return types;
	}
}
