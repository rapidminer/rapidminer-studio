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
package com.rapidminer.operator.preprocessing.filter;

import java.util.Collection;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;

import org.apache.commons.lang.ArrayUtils;

import com.rapidminer.example.Attribute;
import com.rapidminer.example.ExampleSet;
import com.rapidminer.operator.OperatorDescription;
import com.rapidminer.operator.OperatorException;
import com.rapidminer.operator.OperatorVersion;
import com.rapidminer.operator.UserError;
import com.rapidminer.operator.annotation.ResourceConsumptionEstimator;
import com.rapidminer.operator.ports.metadata.AttributeMetaData;
import com.rapidminer.operator.ports.metadata.ExampleSetMetaData;
import com.rapidminer.operator.ports.metadata.SetRelation;
import com.rapidminer.operator.preprocessing.PreprocessingModel;
import com.rapidminer.operator.preprocessing.PreprocessingOperator;
import com.rapidminer.parameter.ParameterType;
import com.rapidminer.parameter.ParameterTypeAttribute;
import com.rapidminer.parameter.ParameterTypeBoolean;
import com.rapidminer.parameter.ParameterTypeCategory;
import com.rapidminer.parameter.ParameterTypeList;
import com.rapidminer.parameter.ParameterTypeString;
import com.rapidminer.parameter.UndefinedParameterError;
import com.rapidminer.parameter.conditions.BooleanParameterCondition;
import com.rapidminer.parameter.conditions.EqualTypeCondition;
import com.rapidminer.parameter.conditions.OrParameterCondition;
import com.rapidminer.tools.Ontology;
import com.rapidminer.tools.OperatorResourceConsumptionHandler;
import com.rapidminer.tools.container.Pair;
import com.rapidminer.tools.math.container.Range;


/**
 * This operator maps all non numeric attributes to real valued attributes. Nothing is done for
 * numeric attributes, binary attributes are mapped to 0 and 1.
 *
 * For nominal attributes one of the following calculations will be done:
 * <ul>
 * <li>Dichotomization, i.e. dummy coding or effect coding: one new attribute for each but one value
 * of the nominal attribute. The new attribute which corresponds to the actual nominal value gets
 * value 1 and all other attributes gets value 0.</li> If the nominal value is the one for which no
 * attribute is being created, all other target attributes are set to 0 (dummy coding) or -1 (effect
 * coding).
 * <li>Alternatively the values of nominal attributes can be seen as equally ranked, therefore the
 * nominal attribute will simply be turned into a real valued attribute, the old values results in
 * equidistant real values.</li>
 * </ul>
 *
 * At this moment the same applies for ordinal attributes, in a future release more appropriate
 * values based on the ranking between the ordinal values may be included.
 *
 * @author Ingo Mierswa, Sebastian Land, Marius Helf
 */
public class NominalToNumeric extends PreprocessingOperator {

	/**
	 * This inner class is just a stub which delegates serialization to the full implementation
	 * which now resides in its own file. This stub is necessary to be able to read models which
	 * have been saved with an older version of RapidMiner, where the full class had been
	 * implemented at this location. The class has been extracted in RM 5.1.009.
	 *
	 * @see com.rapidminer.operator.preprocessing.filter.NominalToNumericModel
	 * @author Marius Helf
	 */
	@Deprecated
	public static class NominalToNumericModel extends com.rapidminer.operator.preprocessing.filter.NominalToNumericModel {

		private static final long serialVersionUID = -4203775081616082145L;

		protected NominalToNumericModel(ExampleSet exampleSet, int codingType) {
			super(exampleSet, codingType);
		}

		private Object readResolve() {
			return new com.rapidminer.operator.preprocessing.filter.NominalToNumericModel(getTrainingHeader(),
					INTEGERS_CODING);
		}
	}

	public static final String PARAMETER_CODING_TYPE = "coding_type";
	public static final String PARAMETER_USE_COMPARISON_GROUPS = "use_comparison_groups";
	public static final String PARAMETER_COMPARISON_GROUP = "comparison_group";
	public static final String PARAMETER_USE_UNDERSCORE_IN_NAME = "use_underscore_in_name";
	public static final String PARAMETER_COMPARISON_GROUPS = "comparison_groups";
	public static final String PARAMETER_ATTRIBUTE_FOR_COMPARISON_GROUP = "comparison_group_attribute";
	public static final String PARAMETER_UNEXPECTED_VALUE_HANDLING = "unexpected_value_handling";

	// values for coding type combo box
	public static final int DUMMY_CODING = 0;
	public static final int EFFECT_CODING = 1;
	public static final int INTEGERS_CODING = 2;
	public static final String[] ENCODING_TYPES = new String[] { "dummy coding", "effect coding", "unique integers" };

	// values for combobox which defines how additional values in the apply example set are handled
	// which are not in the training set.
	public static final int ALL_ZEROES_AND_NO_WARNING = 0;
	public static final int ALL_ZEROES_AND_WARNING = 1;
	public static final String[] UNEXPECTED_VALUE_HANDLING = new String[] { "all 0", "all 0 and warning" };

	// values for the naming scheme chooser
	public static final int UNDERSCORE_NAMING_SCHEME = 0;
	public static final int EQUAL_SIGN_NAMING_SCHEME = 1;

	private static final OperatorVersion VERSION_5_2_8 = new OperatorVersion(5, 2, 8);

	/**
	 * Incompatible version, old version writes into the exampleset, if original output port is not
	 * connected.
	 */
	private static final OperatorVersion VERSION_MAY_WRITE_INTO_DATA = new OperatorVersion(7, 1, 1);

	public NominalToNumeric(OperatorDescription description) {
		super(description);
	}

	/**
	 * Returns a Map from attribute name to value string containing the values the user entered for
	 * parameter PARAMETER_COMPARISON_GROUPS
	 */
	private Map<String, String> getUserEnteredComparisonGroups() throws UndefinedParameterError {
		List<String[]> userValues = getParameterList(PARAMETER_COMPARISON_GROUPS);
		Map<String, String> userValueMap = new LinkedHashMap<String, String>();
		for (String[] tuple : userValues) {
			userValueMap.put(tuple[0], tuple[1]);
		}
		return userValueMap;
	}

	@Override
	protected Collection<AttributeMetaData> modifyAttributeMetaData(ExampleSetMetaData emd, AttributeMetaData amd)
			throws UndefinedParameterError {
		int codingType = getParameterAsInt(PARAMETER_CODING_TYPE);
		if (codingType == INTEGERS_CODING) {
			// integer coding
			int mappingSize = amd.getValueSet().size();
			amd.setType(Ontology.NUMERICAL);
			amd.setValueRange(new Range(0, mappingSize - 1), amd.getValueSetRelation());
			return Collections.singleton(amd);
		} else {	// dummy coding, effect coding
			Collection<AttributeMetaData> newAttribs = new LinkedList<AttributeMetaData>();
			Map<String, String> attributeToComparisonGroupMap = getUserEnteredComparisonGroups();
			String comparisonGroup = attributeToComparisonGroupMap.get(amd.getName());
			boolean useComparisonGroups = getParameterAsBoolean(PARAMETER_USE_COMPARISON_GROUPS);
			boolean useUnderscoreInName = getParameterAsBoolean(PARAMETER_USE_UNDERSCORE_IN_NAME);
			for (String value : amd.getValueSet()) {
				if (!((useComparisonGroups || codingType == EFFECT_CODING) && value.equals(comparisonGroup))) {
					AttributeMetaData newAttrib = new AttributeMetaData(
							getTargetAttributeName(amd.getName(), value, useUnderscoreInName), Ontology.INTEGER);
					double lowerBound = 0;
					if (codingType == EFFECT_CODING) {
						lowerBound = -1;
					}
					newAttrib.setValueRange(new Range(lowerBound, 1), SetRelation.EQUAL);
					newAttribs.add(newAttrib);
				}
			}
			return newAttribs;
		}
	}

	/**
	 * Constructs the name of the target attribute for the current naming scheme and the given
	 * source attribute and the value string.
	 */
	protected static String getTargetAttributeName(String sourceAttributeName, String value, boolean useUnderscore) {
		if (useUnderscore) {
			return sourceAttributeName + "_" + value;
		} else {
			return sourceAttributeName + " = " + value;
		}
	}

	/**
	 * Creates a map from target attribute names to the value (internal string mapping), for which
	 * the attribute becomes 1. Use this function (only) for dummy coding.
	 */
	private Map<String, Double> getAttributeTo1ValueMap(ExampleSet exampleSet) throws OperatorException {
		Map<String, Double> attributeTo1ValueMap = new LinkedHashMap<String, Double>();

		// get nominal attributes from exampleSet
		LinkedList<Attribute> nominalAttributes = new LinkedList<Attribute>();
		for (Attribute attribute : exampleSet.getAttributes()) {
			if (!attribute.isNumerical()) {
				nominalAttributes.add(attribute);
			}
		}

		boolean useUnderscore = getParameterAsBoolean(PARAMETER_USE_UNDERSCORE_IN_NAME);
		boolean useComparisonGroups = getParameterAsBoolean(PARAMETER_USE_COMPARISON_GROUPS);
		Map<String, Double> sourceAttributeToComparisonGroupMap = null;
		if (useComparisonGroups) {
			sourceAttributeToComparisonGroupMap = getSourceAttributeToComparisonGroupMap(exampleSet);
		}

		for (Attribute nominalAttribute : nominalAttributes) {
			double comparisonGroupValue = -1;
			if (useComparisonGroups) {
				comparisonGroupValue = sourceAttributeToComparisonGroupMap.get(nominalAttribute.getName());
			}
			// creating new attributes for nominal attributes
			for (int currentValue = 0; currentValue < nominalAttribute.getMapping().size(); ++currentValue) {
				if (!useComparisonGroups || currentValue != comparisonGroupValue) {
					attributeTo1ValueMap.put(
							getTargetAttributeName(nominalAttribute.getName(),
									nominalAttribute.getMapping().mapIndex(currentValue), useUnderscore),
							(double) currentValue);
				}
			}
		}
		return attributeTo1ValueMap;
	}

	/**
	 * Creates the a map from target attribute names to a pair of the value (internal string
	 * mapping), for which the attribute becomes 1 (first value of the pair) and for which it
	 * becomes -1 (second value). Use this function for effect coding.
	 */
	private Map<String, Pair<Double, Double>> getAttributeToValuesMap(ExampleSet exampleSet) throws OperatorException {
		Map<String, Pair<Double, Double>> attributeToComparisonGroupValueMap = new LinkedHashMap<String, Pair<Double, Double>>();

		// get nominal attributes from exampleSet
		LinkedList<Attribute> nominalAttributes = new LinkedList<Attribute>();
		for (Attribute attribute : exampleSet.getAttributes()) {
			if (!attribute.isNumerical()) {
				nominalAttributes.add(attribute);
			}
		}

		boolean useUnderscore = getParameterAsBoolean(PARAMETER_USE_UNDERSCORE_IN_NAME);
		boolean useComparisonGroups = getParameterAsBoolean(PARAMETER_USE_COMPARISON_GROUPS);
		int codingType = getParameterAsInt(PARAMETER_CODING_TYPE);
		Map<String, Double> sourceAttributeToComparisonGroupValueMap = getSourceAttributeToComparisonGroupMap(exampleSet);

		for (Attribute nominalAttribute : nominalAttributes) {
			double comparisonGroup = sourceAttributeToComparisonGroupValueMap.get(nominalAttribute.getName());
			for (int currentValue = 0; currentValue < nominalAttribute.getMapping().size(); ++currentValue) {
				if (codingType == DUMMY_CODING && !useComparisonGroups
						|| currentValue != sourceAttributeToComparisonGroupValueMap.get(nominalAttribute.getName())) {
					attributeToComparisonGroupValueMap.put(
							getTargetAttributeName(nominalAttribute.getName(),
									nominalAttribute.getMapping().mapIndex(currentValue), useUnderscore),
							new Pair<Double, Double>((double) currentValue, comparisonGroup));
				} else {
					attributeToComparisonGroupValueMap.put(
							getTargetAttributeName(nominalAttribute.getName(),
									nominalAttribute.getMapping().mapIndex(currentValue), useUnderscore),
							new Pair<Double, Double>(comparisonGroup, (double) currentValue));
				}
			}
		}
		return attributeToComparisonGroupValueMap;
	}

	/**
	 * Returns a map from source attribute name to the value (internal string mapping) of the
	 * comparison group of this attribute.
	 */
	private Map<String, Double> getSourceAttributeToComparisonGroupMap(ExampleSet exampleSet) throws OperatorException {
		Map<String, Double> sourceAttributeToComparisonGroupMap = new LinkedHashMap<String, Double>();

		// check if the user set a comparison group for all selected attributes
		List<String[]> attributesComparisonGroups = getParameterList(PARAMETER_COMPARISON_GROUPS);
		for (Attribute attribute : exampleSet.getAttributes()) {
			if (!attribute.isNumerical()) {
				String attributeName = attribute.getName();

				// search for this attribute in user input
				boolean found = false;
				for (String[] attributeComparisonGroup : attributesComparisonGroups) {
					if (attributeComparisonGroup[0].equals(attributeName)) {
						if (found) {
							throw new UserError(this, "nominal_to_numerical.duplicate_comparison_group", attributeName); 	// duplicate
																														 	// entry
						}
						found = true;

						String comparisonGroup = attributeComparisonGroup[1];
						double comparisonGroupValue = attribute.getMapping().getIndex(comparisonGroup);
						// now check if the supplied value exists in the mapping
						if (comparisonGroupValue < 0) {
							throw new UserError(this, "nominal_to_numerical.illegal_comparison_group", attributeName,
									attributeComparisonGroup[1]);	// illegal
																		// value
						}

						// store comparison group in map:
						sourceAttributeToComparisonGroupMap.put(attributeName, comparisonGroupValue);
					}
				}

				// check if the attribute has been found at all
				if (!found) {
					throw new UserError(this, "nominal_to_numerical.illegal_comparison_group", attributeName, "<undefined>");	// no
																																	// value
				}
			}
		}

		return sourceAttributeToComparisonGroupMap;
	}

	@Override
	public PreprocessingModel createPreprocessingModel(ExampleSet exampleSet) throws OperatorException {

		int codingType = getParameterAsInt(PARAMETER_CODING_TYPE);
		if (codingType == INTEGERS_CODING) {
			return new com.rapidminer.operator.preprocessing.filter.NominalToNumericModel(exampleSet, codingType);
		} else if (codingType == DUMMY_CODING) {
			Map<String, Double> sourceAttributeToComparisonGroupMap = null;
			if (getParameterAsBoolean(PARAMETER_USE_COMPARISON_GROUPS)) {
				sourceAttributeToComparisonGroupMap = getSourceAttributeToComparisonGroupMap(exampleSet);
			}
			;
			Map<String, Double> attributeTo1ValueMap = getAttributeTo1ValueMap(exampleSet);
			return new com.rapidminer.operator.preprocessing.filter.NominalToNumericModel(exampleSet, codingType,
					getParameterAsBoolean(PARAMETER_USE_UNDERSCORE_IN_NAME), sourceAttributeToComparisonGroupMap,
					attributeTo1ValueMap, null, getParameterAsBoolean(PARAMETER_USE_COMPARISON_GROUPS),
					getParameterAsInt(PARAMETER_UNEXPECTED_VALUE_HANDLING));
		} else if (codingType == EFFECT_CODING) {
			Map<String, Double> sourceAttributeToComparisonGroupMap = getSourceAttributeToComparisonGroupMap(exampleSet);
			Map<String, Pair<Double, Double>> attributeToValuesMap = getAttributeToValuesMap(exampleSet);
			return new com.rapidminer.operator.preprocessing.filter.NominalToNumericModel(exampleSet, codingType,
					getParameterAsBoolean(PARAMETER_USE_UNDERSCORE_IN_NAME), sourceAttributeToComparisonGroupMap, null,
					attributeToValuesMap, true, getParameterAsInt(PARAMETER_UNEXPECTED_VALUE_HANDLING));
		} else {
			assert false; // unsupported coding
			return null;
		}
	}

	@Override
	public Class<? extends PreprocessingModel> getPreprocessingModelClass() {
		return com.rapidminer.operator.preprocessing.filter.NominalToNumericModel.class;
	}

	@Override
	protected int[] getFilterValueTypes() {
		return new int[] { Ontology.NOMINAL };
	}

	@Override
	public ResourceConsumptionEstimator getResourceConsumptionEstimator() {
		return OperatorResourceConsumptionHandler.getResourceConsumptionEstimator(getInputPort(), NominalToNumeric.class,
				attributeSelector);
	}

	@Override
	public List<ParameterType> getParameterTypes() {
		List<ParameterType> types = super.getParameterTypes();

		types.add(new ParameterTypeCategory(PARAMETER_CODING_TYPE, "The coding of the numerical attributes.", ENCODING_TYPES,
				getCompatibilityLevel().isAtMost(VERSION_5_2_8) ? INTEGERS_CODING : DUMMY_CODING, false));

		ParameterType type;

		type = new ParameterTypeBoolean(PARAMETER_USE_COMPARISON_GROUPS,
				"If checked, for each selected attribute in the input set a value has to be specified as comparsion group, which will not appear in the final result set. If not checked, all values of the selected attributes will result in an indicator attribute in the result example set. ",
				false, true);
		type.registerDependencyCondition(
				new EqualTypeCondition(this, PARAMETER_CODING_TYPE, ENCODING_TYPES, true, DUMMY_CODING));
		types.add(type);

		type = new ParameterTypeList(PARAMETER_COMPARISON_GROUPS, "The value which becomes the comparison group.",
				new ParameterTypeAttribute(PARAMETER_ATTRIBUTE_FOR_COMPARISON_GROUP,
						"The attribute for which the comparison group is set.", getExampleSetInputPort(), Ontology.NOMINAL),
				new ParameterTypeString(PARAMETER_COMPARISON_GROUP, "The value which is used as comparison group.", true,
						false));
		type.setExpert(false);
		type.registerDependencyCondition(new OrParameterCondition(this, true,
				new BooleanParameterCondition(this, PARAMETER_USE_COMPARISON_GROUPS, true, true),
				new EqualTypeCondition(this, PARAMETER_CODING_TYPE, ENCODING_TYPES, true, EFFECT_CODING)));
		types.add(type);

		type = new ParameterTypeCategory(PARAMETER_UNEXPECTED_VALUE_HANDLING,
				"Indicates how values are handled, which occur in the example set to which the preprocessing model is applied, but not in the training set. By default all attributes are set to 0 and a warning is logged. However, by the additional checks for the warning some overhead is generated, so you can turn off the logging and just set all attributes to 0.",
				UNEXPECTED_VALUE_HANDLING, ALL_ZEROES_AND_WARNING, true);
		type.registerDependencyCondition(
				new EqualTypeCondition(this, PARAMETER_CODING_TYPE, ENCODING_TYPES, false, EFFECT_CODING, DUMMY_CODING));
		types.add(type);

		type = new ParameterTypeBoolean(PARAMETER_USE_UNDERSCORE_IN_NAME,
				"Indicates if underscores should be used in the new attribute names instead of empty spaces and '='. Although the resulting names are harder to read for humans it might be more appropriate to use these if the data should be written into a database system.",
				false, true);
		type.registerDependencyCondition(
				new EqualTypeCondition(this, PARAMETER_CODING_TYPE, ENCODING_TYPES, true, EFFECT_CODING, DUMMY_CODING));
		types.add(type);

		return types;
	}

	@Override
	public boolean writesIntoExistingData() {
		if (getCompatibilityLevel().isAbove(VERSION_MAY_WRITE_INTO_DATA)) {
			return false;
		} else {
			// old version: true only if original output port is connected
			return isOriginalOutputConnected() && super.writesIntoExistingData();
		}
	}

	@Override
	public OperatorVersion[] getIncompatibleVersionChanges() {
		return (OperatorVersion[]) ArrayUtils.addAll(super.getIncompatibleVersionChanges(),
				new OperatorVersion[] { VERSION_5_2_8, VERSION_MAY_WRITE_INTO_DATA });
	}
}
