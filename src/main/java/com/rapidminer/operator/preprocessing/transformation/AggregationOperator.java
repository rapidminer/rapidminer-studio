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
package com.rapidminer.operator.preprocessing.transformation;

import java.lang.reflect.InvocationTargetException;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Iterator;
import java.util.LinkedList;
import java.util.List;
import java.util.Set;
import java.util.TreeSet;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.regex.PatternSyntaxException;

import com.rapidminer.example.Attribute;
import com.rapidminer.example.Attributes;
import com.rapidminer.example.Example;
import com.rapidminer.example.ExampleSet;
import com.rapidminer.example.table.AttributeFactory;
import com.rapidminer.example.table.NominalMapping;
import com.rapidminer.example.utils.ExampleSetBuilder;
import com.rapidminer.example.utils.ExampleSets;
import com.rapidminer.operator.OperatorCreationException;
import com.rapidminer.operator.OperatorDescription;
import com.rapidminer.operator.OperatorException;
import com.rapidminer.operator.UserError;
import com.rapidminer.operator.annotation.ResourceConsumptionEstimator;
import com.rapidminer.operator.error.AttributeNotFoundError;
import com.rapidminer.operator.ports.metadata.AttributeMetaData;
import com.rapidminer.operator.ports.metadata.ExampleSetMetaData;
import com.rapidminer.operator.ports.metadata.MDInteger;
import com.rapidminer.operator.ports.metadata.MetaData;
import com.rapidminer.operator.ports.metadata.SetRelation;
import com.rapidminer.operator.preprocessing.AbstractDataProcessing;
import com.rapidminer.operator.preprocessing.filter.ExampleFilter;
import com.rapidminer.operator.preprocessing.filter.NumericToNominal;
import com.rapidminer.operator.preprocessing.filter.NumericToPolynominal;
import com.rapidminer.operator.preprocessing.filter.attributes.RegexpAttributeFilter;
import com.rapidminer.operator.tools.AttributeSubsetSelector;
import com.rapidminer.parameter.ParameterType;
import com.rapidminer.parameter.ParameterTypeAttribute;
import com.rapidminer.parameter.ParameterTypeAttributes;
import com.rapidminer.parameter.ParameterTypeBoolean;
import com.rapidminer.parameter.ParameterTypeList;
import com.rapidminer.parameter.ParameterTypeStringCategory;
import com.rapidminer.parameter.UndefinedParameterError;
import com.rapidminer.parameter.conditions.BooleanParameterCondition;
import com.rapidminer.tools.Ontology;
import com.rapidminer.tools.OperatorResourceConsumptionHandler;
import com.rapidminer.tools.OperatorService;
import com.rapidminer.tools.container.MultidimensionalArraySet;
import com.rapidminer.tools.container.ValueSet;
import com.rapidminer.tools.math.function.aggregation.AbstractAggregationFunction;
import com.rapidminer.tools.math.function.aggregation.AggregationFunction;


/**
 *
 * <p>
 * This operator creates a new example set from the input example set showing the results of
 * arbitrary aggregation functions (as SUM, COUNT etc. known from SQL). Before the values of
 * different rows are aggregated into a new row the rows might be grouped by the values of a
 * multiple attributes (similar to the group-by clause known from SQL). In this case a new line will
 * be created for each group.
 * </p>
 *
 * <p>
 * Please note that the known HAVING clause from SQL can be simulated by an additional
 * {@link ExampleFilter} operator following this one.
 * </p>
 * This class has been replaced by the
 * {@link com.rapidminer.operator.preprocessing.transformation.aggregation.AggregationOperator}
 * Operator.
 *
 * @author Tobias Malbrecht, Ingo Mierswa, Sebastian Land
 */
@Deprecated
public class AggregationOperator extends AbstractDataProcessing {

	private static class AggregationAttribute {

		Attribute attribute;
		String functionName;
		int resultType;
	}

	public static final String PARAMETER_USE_DEFAULT_AGGREGATION = "use_default_aggregation";
	public static final String PARAMETER_DEFAULT_AGGREGATION_FUNCTION = "default_aggregation_function";
	public static final String PARAMETER_AGGREGATION_ATTRIBUTES = "aggregation_attributes";

	public static final String PARAMETER_AGGREGATION_FUNCTIONS = "aggregation_functions";

	public static final String PARAMETER_GROUP_BY_ATTRIBUTES = "group_by_attributes";

	public static final String PARAMETER_ONLY_DISTINCT = "only_distinct";

	public static final String PARAMETER_IGNORE_MISSINGS = "ignore_missings";

	public static final String GENERIC_GROUP_NAME = "group";

	public static final String GENERIC_ALL_NAME = "all";

	public static final String PARAMETER_ALL_COMBINATIONS = "count_all_combinations";

	private final AttributeSubsetSelector attributeSelector = new AttributeSubsetSelector(this, getExampleSetInputPort());

	public AggregationOperator(OperatorDescription desc) {
		super(desc);
	}

	@Override
	protected MetaData modifyMetaData(ExampleSetMetaData metaData) throws UndefinedParameterError {
		ExampleSetMetaData resultMD = metaData.clone();
		resultMD.clear();

		// add group by attributes
		if (isParameterSet(PARAMETER_GROUP_BY_ATTRIBUTES) && !getParameterAsString(PARAMETER_GROUP_BY_ATTRIBUTES).isEmpty()) {
			String attributeRegex = getParameterAsString(PARAMETER_GROUP_BY_ATTRIBUTES);
			Pattern pattern = Pattern.compile(attributeRegex);

			for (AttributeMetaData amd : metaData.getAllAttributes()) {
				if (pattern.matcher(amd.getName()).matches()) {
					if (amd.isNumerical()) { // converting type to mimic NumericalToPolynomial used
						// below
						amd.setType(Ontology.NOMINAL);
						amd.setValueSet(Collections.<String> emptySet(), SetRelation.SUPERSET);
					}
					resultMD.addAttribute(amd);
				}
			}
			resultMD.getNumberOfExamples().reduceByUnknownAmount();
		} else {
			AttributeMetaData allGroup = new AttributeMetaData(GENERIC_GROUP_NAME, Ontology.NOMINAL);
			Set<String> values = new TreeSet<>();
			values.add(GENERIC_ALL_NAME);
			allGroup.setValueSet(values, SetRelation.EQUAL);
			resultMD.addAttribute(allGroup);
			resultMD.setNumberOfExamples(new MDInteger(1));
		}

		// add aggregated attributes of default aggregation
		if (getParameterAsBoolean(PARAMETER_USE_DEFAULT_AGGREGATION)) {
			String defaultFunction = getParameterAsString(PARAMETER_DEFAULT_AGGREGATION_FUNCTION);
			ExampleSetMetaData metaDataSubset = attributeSelector.getMetaDataSubset(metaData, false);
			for (AttributeMetaData amd : metaDataSubset.getAllAttributes()) {
				resultMD.addAttribute(new AttributeMetaData(defaultFunction + "(" + amd.getName() + ")", getResultType(
						defaultFunction, amd)));
			}
		}

		// add aggregated attributes of list
		List<String[]> parameterList = this.getParameterList(PARAMETER_AGGREGATION_ATTRIBUTES);
		for (String[] function : parameterList) {
			AttributeMetaData amd = metaData.getAttributeByName(function[0]);
			if (amd != null) {
				resultMD.addAttribute(new AttributeMetaData(function[1] + "(" + function[0] + ")", getResultType(
						function[1], amd)));
			}
		}
		return resultMD;
	}

	/**
	 * Returns the result type of an aggregation of a given attribute with given function name
	 */
	private int getResultType(String functionName, AttributeMetaData attribute) {
		if (functionName
				.equals(AbstractAggregationFunction.KNOWN_AGGREGATION_FUNCTION_NAMES[AbstractAggregationFunction.COUNT])) {
			return Ontology.NUMERICAL;
		} else {
			if (attribute.isNumerical()) {
				return Ontology.NUMERICAL;
			} else if (attribute.isNominal()) {
				return Ontology.NOMINAL;
			} else {
				return attribute.getValueType();
			}
		}
	}

	@Override
	public ExampleSet apply(ExampleSet exampleSet) throws OperatorException {
		exampleSet = (ExampleSet) exampleSet.clone();
		boolean onlyDistinctValues = getParameterAsBoolean(PARAMETER_ONLY_DISTINCT);
		boolean ignoreMissings = getParameterAsBoolean(PARAMETER_IGNORE_MISSINGS);

		ArrayList<AggregationAttribute> aggregationAttributes = new ArrayList<AggregationOperator.AggregationAttribute>();

		// first store all default attributes if defined
		if (getParameterAsBoolean(PARAMETER_USE_DEFAULT_AGGREGATION)) {
			Set<Attribute> attributeSubset = attributeSelector.getAttributeSubset(exampleSet, false);
			String defaultFunctionName = getParameterAsString(PARAMETER_DEFAULT_AGGREGATION_FUNCTION);
			for (Attribute attribute : attributeSubset) {
				AggregationAttribute currentAggregationAttribute = new AggregationAttribute();
				currentAggregationAttribute.attribute = attribute;
				currentAggregationAttribute.functionName = defaultFunctionName;
				currentAggregationAttribute.resultType = getResultType(defaultFunctionName, attribute);

				aggregationAttributes.add(currentAggregationAttribute);
			}
		}

		// second read specific attributes and override if already part of default attributes
		List<String[]> parameterList = this.getParameterList(PARAMETER_AGGREGATION_ATTRIBUTES);
		for (String[] valuePair : parameterList) {
			AggregationAttribute currentAggregationAttribute = new AggregationAttribute();

			// attribute
			String attributeName = valuePair[0];
			Attribute attribute = exampleSet.getAttributes().get(attributeName);
			if (attribute == null) {
				throw new AttributeNotFoundError(this, PARAMETER_AGGREGATION_ATTRIBUTES, attributeName);
			}
			currentAggregationAttribute.attribute = attribute;

			// functionname and resulttype
			currentAggregationAttribute.functionName = valuePair[1];
			currentAggregationAttribute.resultType = getResultType(currentAggregationAttribute.functionName, attribute);

			aggregationAttributes.add(currentAggregationAttribute);
		}
		AggregationAttribute[] aggregations = aggregationAttributes
				.toArray(new AggregationAttribute[aggregationAttributes.size()]);
		int numberOfAggregations = aggregationAttributes.size();

		Attribute weightAttribute = exampleSet.getAttributes().getWeight();
		ExampleSetBuilder builder = null;
		boolean allCombinations = getParameterAsBoolean(PARAMETER_ALL_COMBINATIONS);

		/*
		 * We have to check whether parameter is set and not empty, because RegexpAttributeFilter
		 * needs parameter set and not empty. Otherwise a UserError is thrown.
		 */
		if (isParameterSet(PARAMETER_GROUP_BY_ATTRIBUTES)
				&& !getParameterAsString(PARAMETER_GROUP_BY_ATTRIBUTES).isEmpty()) {
			String groupByAttributesRegex = getParameterAsString(PARAMETER_GROUP_BY_ATTRIBUTES);

			// make attributes nominal
			try {
				NumericToNominal toNominalOperator = OperatorService.createOperator(NumericToPolynominal.class);
				toNominalOperator.setParameter(AttributeSubsetSelector.PARAMETER_FILTER_TYPE,
						AttributeSubsetSelector.CONDITION_REGULAR_EXPRESSION + "");
				toNominalOperator.setParameter(RegexpAttributeFilter.PARAMETER_REGULAR_EXPRESSION, groupByAttributesRegex);
				toNominalOperator.setParameter(AttributeSubsetSelector.PARAMETER_INCLUDE_SPECIAL_ATTRIBUTES, "true");
				exampleSet = toNominalOperator.apply(exampleSet);
			} catch (OperatorCreationException e) {
				// might work if attributes already nominal. Otherwise UserError will be thrown
				// below.
				// TODO (Simon): Huh?
			}

			Attribute[] groupByAttributes = getAttributesArrayFromRegex(exampleSet.getAttributes(), groupByAttributesRegex);

			if (groupByAttributes.length == 0) {
				throw new AttributeNotFoundError(this, PARAMETER_GROUP_BY_ATTRIBUTES, groupByAttributesRegex);
			}

			int[] mappingSizes = new int[groupByAttributes.length];
			for (int i = 0; i < groupByAttributes.length; i++) {
				if (groupByAttributes[i].isNumerical()) {
					throw new UserError(this, 103, new Object[] { groupByAttributesRegex, "grouping by attribute." });
				}
				mappingSizes[i] = groupByAttributes[i].getMapping().size();
			}

			// create aggregation functions
			MultidimensionalArraySet<AggregationFunction[]> functionSet = new MultidimensionalArraySet<>(mappingSizes);

			if (onlyDistinctValues && !allCombinations) {

				// initialize distinct value sets
				MultidimensionalArraySet<ValueSet[]> distinctValueSet = new MultidimensionalArraySet<>(mappingSizes);

				// extract distinct values
				for (Example example : exampleSet) {
					int[] indices = new int[groupByAttributes.length];
					for (int i = 0; i < groupByAttributes.length; i++) {
						indices[i] = (int) example.getValue(groupByAttributes[i]);
					}

					ValueSet[] distinctValues = distinctValueSet.get(indices);
					if (distinctValues == null) {
						distinctValues = new ValueSet[numberOfAggregations];
						for (int j = 0; j < numberOfAggregations; j++) {
							distinctValues[j] = new ValueSet();
						}
						distinctValueSet.set(indices, distinctValues);
					}

					double weight = weightAttribute != null ? example.getWeight() : 1.0d;
					for (int i = 0; i < numberOfAggregations; i++) {
						distinctValues[i].add(example.getValue(aggregations[i].attribute), weight);
					}
				}

				// TODO (Simon): Isn't this loop rather pointless? Why do we iterate over
				// functionSet
				// compute aggregation function values
				for (int i = 0; i < functionSet.size(); i++) {
					ValueSet[] distinctValues = distinctValueSet.get(i);
					if (distinctValues != null) {
						AggregationFunction[] functions = new AggregationFunction[numberOfAggregations];
						for (int j = 0; j < numberOfAggregations; j++) {
							functions[j] = getAggregationFunction(aggregations[j].functionName, ignoreMissings,
									aggregations[j].attribute);
						}
						functionSet.set(i, functions);
						for (int j = 0; j < numberOfAggregations; j++) {
							for (Double value : distinctValues[j]) {
								functions[j].update(value);
							}
						}
					}
				}
			} else {
				if (allCombinations) {
					registerAllCombinations(groupByAttributes, functionSet, ignoreMissings, aggregations);
				}

				// compute aggregation function values
				for (Example example : exampleSet) {
					int[] indices = new int[groupByAttributes.length];
					for (int i = 0; i < groupByAttributes.length; i++) {
						indices[i] = (int) example.getValue(groupByAttributes[i]);
					}
					double weight = weightAttribute != null ? example.getWeight() : 1.0d;
					AggregationFunction[] functions = functionSet.get(indices);
					if (functions == null) {
						functions = new AggregationFunction[numberOfAggregations];
						for (int j = 0; j < numberOfAggregations; j++) {
							functions[j] = getAggregationFunction(aggregations[j].functionName, ignoreMissings,
									aggregations[j].attribute);
						}
						functionSet.set(indices, functions);
					}
					for (int i = 0; i < numberOfAggregations; i++) {
						functions[i].update(example.getValue(aggregations[i].attribute), weight);
					}
				}
			}

			// create grouped data table
			List<Attribute> resultAttributes = new ArrayList<>();
			Attribute[] resultGroupAttributes = new Attribute[groupByAttributes.length];
			for (int i = 0; i < groupByAttributes.length; i++) {
				Attribute resultGroupAttribute = AttributeFactory.createAttribute(groupByAttributes[i].getName(),
						Ontology.NOMINAL);
				for (int j = 0; j < groupByAttributes[i].getMapping().size(); j++) {
					resultGroupAttribute.getMapping().mapString(groupByAttributes[i].getMapping().mapIndex(j));
				}
				resultAttributes.add(resultGroupAttribute);
				resultGroupAttributes[i] = resultGroupAttribute;
			}
			for (int i = 0; i < numberOfAggregations; i++) {
				// if (nominalResults[i]) {
				// resultAttributes.add(AttributeFactory.createAttribute(aggregationFunctionNames[i]
				// + "(" +
				// aggregationAttributes[i].getName() + ")", Ontology.NOMINAL));
				// } else {
				// resultAttributes.add(AttributeFactory.createAttribute(aggregationFunctionNames[i]
				// + "(" +
				// aggregationAttributes[i].getName() + ")", Ontology.REAL));
				// }
				resultAttributes.add(AttributeFactory.createAttribute(
						aggregations[i].functionName + "(" + aggregations[i].attribute.getName() + ")",
						aggregations[i].resultType));
			}
			builder = ExampleSets.from(resultAttributes);

			// fill data table
			// TODO (Simon): Again pointless loop. We should iterate only over non-null entries
			for (int i = 0; i < functionSet.size(); i++) {
				double data[] = new double[groupByAttributes.length + numberOfAggregations];
				int[] indices = functionSet.getIndices(i);
				for (int j = 0; j < groupByAttributes.length; j++) {
					data[j] = indices[j];
				}
				AggregationFunction[] functions = functionSet.get(i);
				if (functions != null) {
					for (int j = 0; j < numberOfAggregations; j++) {
						// data[groupByAttributes.length + j] = nominalResults[j] ?
						// resultTable.getAttribute(groupByAttributes.length +
						// j).getMapping().mapString(aggregationAttributes[j].getMapping().mapIndex((int)
						// functions[j].getValue())) :
						// functions[j].getValue();
						if (Ontology.ATTRIBUTE_VALUE_TYPE.isA(aggregations[j].resultType, Ontology.NOMINAL)) {
							data[groupByAttributes.length + j] = resultAttributes.get(groupByAttributes.length + j)
									.getMapping().mapString(
											aggregations[j].attribute.getMapping().mapIndex((int) functions[j].getValue()));
						} else {
							data[groupByAttributes.length + j] = functions[j].getValue();
						}
					}
					builder.addRow(data);
				}
			}
		} else {
			AggregationFunction[] functions = new AggregationFunction[numberOfAggregations];
			for (int i = 0; i < numberOfAggregations; i++) {
				functions[i] = getAggregationFunction(aggregations[i].functionName, ignoreMissings,
						aggregations[i].attribute);
			}

			if (onlyDistinctValues) {

				// initialize distinct value sets
				ValueSet[] distinctValues = new ValueSet[numberOfAggregations];
				for (int i = 0; i < numberOfAggregations; i++) {
					distinctValues[i] = new ValueSet();
				}
				for (Example example : exampleSet) {
					double weight = weightAttribute != null ? example.getWeight() : 1.0d;
					for (int i = 0; i < distinctValues.length; i++) {
						distinctValues[i].add(example.getValue(aggregations[i].attribute), weight);
					}
				}

				// compute aggregation function values
				for (int i = 0; i < distinctValues.length; i++) {
					for (Double value : distinctValues[i]) {
						functions[i].update(value);
					}
				}
			} else {

				// compute aggregation function values
				for (Example example : exampleSet) {
					double weight = weightAttribute != null ? example.getWeight() : 1.0d;
					for (int i = 0; i < functions.length; i++) {
						functions[i].update(example.getValue(aggregations[i].attribute), weight);
					}
				}
			}

			// create data table
			List<Attribute> resultAttributes = new ArrayList<>();
			Attribute resultGroupAttribute = AttributeFactory.createAttribute(GENERIC_GROUP_NAME, Ontology.NOMINAL);
			resultAttributes.add(resultGroupAttribute);
			for (int i = 0; i < numberOfAggregations; i++) {
				// if (nominalResults[i]) {
				// resultAttributes.add(AttributeFactory.createAttribute(aggregationFunctionNames[i]
				// + "(" +
				// aggregationAttributes[i].getName() + ")", Ontology.NOMINAL));
				// } else {
				// resultAttributes.add(AttributeFactory.createAttribute(aggregationFunctionNames[i]
				// + "(" +
				// aggregationAttributes[i].getName() + ")", Ontology.REAL));
				// }
				resultAttributes.add(AttributeFactory.createAttribute(
						aggregations[i].functionName + "(" + aggregations[i].attribute.getName() + ")",
						aggregations[i].resultType));

			}
			for (Attribute attribute : resultAttributes) {
				attribute.setConstruction(attribute.getName());
			}
			builder = ExampleSets.from(resultAttributes);

			// fill data table
			double[] data = new double[numberOfAggregations + 1];
			data[0] = resultGroupAttribute.getMapping().mapString(GENERIC_ALL_NAME);
			for (int i = 0; i < numberOfAggregations; i++) {
				// data[i + 1] = nominalResults[i] ? resultTable.getAttribute(i +
				// 1).getMapping().mapString(aggregationAttributes[i].getMapping().mapIndex((int)
				// functions[i].getValue())) :
				// functions[i].getValue();
				if (Ontology.ATTRIBUTE_VALUE_TYPE.isA(aggregations[i].resultType, Ontology.NOMINAL)) {
					data[i + 1] = resultAttributes.get(i + 1).getMapping()
							.mapString(aggregations[i].attribute.getMapping().mapIndex((int) functions[i].getValue()));
				} else {
					data[i + 1] = functions[i].getValue();
				}

			}
			builder.addRow(data);
		}

		ExampleSet resultSet = builder.build();
		return resultSet;
	}

	/**
	 * Returns the result type of an aggregation of a given attribute with given functio nname
	 */
	private int getResultType(String functionName, Attribute attribute) {
		if (functionName
				.equals(AbstractAggregationFunction.KNOWN_AGGREGATION_FUNCTION_NAMES[AbstractAggregationFunction.COUNT])) {
			return Ontology.NUMERICAL;
		} else {
			if (attribute.isNumerical()) {
				return Ontology.NUMERICAL;
			} else if (attribute.isNominal()) {
				return Ontology.NOMINAL;
			} else {
				return attribute.getValueType();
			}
		}
	}

	/**
	 * This method will register for each index of the group by attributes' mapping the
	 * corresponding aggregation functions
	 *
	 * @throws UserError
	 */
	private void registerAllCombinations(Attribute[] groupByAttributes,
			MultidimensionalArraySet<AggregationFunction[]> functionSet, boolean ignoreMissings,
			AggregationAttribute[] aggregationAttributes) throws UserError {
		registerAllCombinationsRecursion(groupByAttributes, functionSet, ignoreMissings, aggregationAttributes,
				new int[groupByAttributes.length], 0);
	}

	/**
	 * The recursively called method.
	 *
	 * @throws UserError
	 */
	private void registerAllCombinationsRecursion(Attribute[] groupByAttributes,
			MultidimensionalArraySet<AggregationFunction[]> functionSet, boolean ignoreMissings,
			AggregationAttribute[] aggregationAttributes, int[] indices, int depth) throws UserError {
		if (depth == indices.length) {
			AggregationFunction[] functions = new AggregationFunction[aggregationAttributes.length];
			for (int j = 0; j < aggregationAttributes.length; j++) {
				functions[j] = getAggregationFunction(aggregationAttributes[j].functionName, ignoreMissings,
						aggregationAttributes[j].attribute);
			}
			functionSet.set(indices, functions);
		} else {
			NominalMapping mapping = groupByAttributes[depth].getMapping();
			for (String value : mapping.getValues()) {
				indices[depth] = mapping.getIndex(value);
				registerAllCombinationsRecursion(groupByAttributes, functionSet, ignoreMissings, aggregationAttributes,
						indices, depth + 1);
			}
		}
	}

	private AggregationFunction getAggregationFunction(String functionName, boolean ignoreMissings, Attribute attribute)
			throws UserError {
		AggregationFunction function;
		try {
			function = AbstractAggregationFunction.createAggregationFunction(functionName, ignoreMissings);
		} catch (InstantiationException e) {
			throw new UserError(this, 904, functionName, e.getMessage());
		} catch (IllegalAccessException e) {
			throw new UserError(this, 904, functionName, e.getMessage());
		} catch (ClassNotFoundException e) {
			throw new UserError(this, 904, functionName, e.getMessage());
		} catch (NoSuchMethodException e) {
			throw new UserError(this, 904, functionName, e.getMessage());
		} catch (InvocationTargetException e) {
			throw new UserError(this, 904, functionName, e.getMessage());
		}
		if (!function.supportsAttribute(attribute)) {
			throw new UserError(this, 136, attribute.getName());
		}
		return function;
	}

	private Attribute[] getAttributesArrayFromRegex(Attributes attributes, String regex) throws OperatorException {
		Pattern pattern = null;
		try {
			pattern = Pattern.compile(regex);
		} catch (PatternSyntaxException e) {
			throw new UserError(this, 206, regex, e.getMessage());
		}
		List<Attribute> attributeList = new LinkedList<>();
		Iterator<Attribute> i = attributes.allAttributes();
		while (i.hasNext()) {
			Attribute attribute = i.next();
			Matcher matcher = pattern.matcher(attribute.getName());
			if (matcher.matches()) {
				attributeList.add(attribute);
			}
		}

		Attribute[] attributesArray = new Attribute[attributeList.size()];
		attributesArray = attributeList.toArray(attributesArray);
		return attributesArray;
	}

	@Override
	public List<ParameterType> getParameterTypes() {
		List<ParameterType> types = super.getParameterTypes();

		types.add(new ParameterTypeBoolean(PARAMETER_USE_DEFAULT_AGGREGATION,
				"If checked you can select a default aggregation function for a subset of the attributes.", false, false));
		List<ParameterType> parameterTypes = attributeSelector.getParameterTypes();
		for (ParameterType type : parameterTypes) {
			type.registerDependencyCondition(new BooleanParameterCondition(this, PARAMETER_USE_DEFAULT_AGGREGATION, false,
					true));
			types.add(type);
		}
		ParameterType type = new ParameterTypeStringCategory(PARAMETER_DEFAULT_AGGREGATION_FUNCTION,
				"The type of the used aggregation function for all default attributes.",
				AbstractAggregationFunction.KNOWN_AGGREGATION_FUNCTION_NAMES,
				AbstractAggregationFunction.KNOWN_AGGREGATION_FUNCTION_NAMES[0]);
		type.registerDependencyCondition(new BooleanParameterCondition(this, PARAMETER_USE_DEFAULT_AGGREGATION, false, true));
		type.setExpert(false);
		types.add(type);

		types.add(new ParameterTypeList(PARAMETER_AGGREGATION_ATTRIBUTES, "The attributes which should be aggregated.",
				new ParameterTypeAttribute("aggregation_attribute", "Specifies the attribute which is aggregated.",
						getExampleSetInputPort()), new ParameterTypeStringCategory(PARAMETER_AGGREGATION_FUNCTIONS,
						"The type of the used aggregation function.",
						AbstractAggregationFunction.KNOWN_AGGREGATION_FUNCTION_NAMES,
						AbstractAggregationFunction.KNOWN_AGGREGATION_FUNCTION_NAMES[0]), false));
		types.add(new ParameterTypeAttributes(PARAMETER_GROUP_BY_ATTRIBUTES,
				"Performs a grouping by the values of the attributes whose names match the given regular expression.",
				getExampleSetInputPort(), true, false));
		types.add(new ParameterTypeBoolean(
				PARAMETER_ALL_COMBINATIONS,
				"Indicates that all possible combinations of the values of the group by attributes are counted, even if they don't occur. Please handle with care, since the number might be enormous.",
				false));
		type = new ParameterTypeBoolean(
				PARAMETER_ONLY_DISTINCT,
				"Indicates if only rows with distinct values for the aggregation attribute should be used for the calculation of the aggregation function.",
				false);
		type.registerDependencyCondition(new BooleanParameterCondition(this, PARAMETER_ALL_COMBINATIONS, false, false));
		types.add(type);
		types.add(new ParameterTypeBoolean(
				PARAMETER_IGNORE_MISSINGS,
				"Indicates if missings should be ignored and aggregation should be based only on existing values or not. In the latter case the aggregated value will be missing in the presence of missing values.",
				true));
		return types;
	}

	@Override
	public boolean writesIntoExistingData() {
		return false;
	}

	@Override
	public ResourceConsumptionEstimator getResourceConsumptionEstimator() {
		return OperatorResourceConsumptionHandler.getResourceConsumptionEstimator(getInputPort(), AggregationOperator.class,
				null);
	}
}
