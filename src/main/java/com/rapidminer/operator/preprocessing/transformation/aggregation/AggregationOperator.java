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
package com.rapidminer.operator.preprocessing.transformation.aggregation;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.HashSet;
import java.util.Iterator;
import java.util.LinkedList;
import java.util.List;
import java.util.Map.Entry;
import java.util.Set;
import java.util.TreeMap;
import java.util.TreeSet;
import java.util.regex.Pattern;
import java.util.regex.PatternSyntaxException;

import org.apache.commons.lang.ArrayUtils;
import org.apache.commons.lang.StringUtils;

import com.rapidminer.example.Attribute;
import com.rapidminer.example.AttributeRole;
import com.rapidminer.example.Attributes;
import com.rapidminer.example.Example;
import com.rapidminer.example.ExampleSet;
import com.rapidminer.example.table.AttributeFactory;
import com.rapidminer.example.table.DataRowFactory;
import com.rapidminer.example.table.DoubleArrayDataRow;
import com.rapidminer.example.utils.ExampleSetBuilder;
import com.rapidminer.example.utils.ExampleSets;
import com.rapidminer.operator.OperatorCreationException;
import com.rapidminer.operator.OperatorDescription;
import com.rapidminer.operator.OperatorException;
import com.rapidminer.operator.OperatorVersion;
import com.rapidminer.operator.ProcessSetupError.Severity;
import com.rapidminer.operator.UserError;
import com.rapidminer.operator.annotation.ResourceConsumptionEstimator;
import com.rapidminer.operator.ports.metadata.AttributeMetaData;
import com.rapidminer.operator.ports.metadata.ExampleSetMetaData;
import com.rapidminer.operator.ports.metadata.MDInteger;
import com.rapidminer.operator.ports.metadata.MetaData;
import com.rapidminer.operator.ports.metadata.SetRelation;
import com.rapidminer.operator.ports.metadata.SimpleMetaDataError;
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


/**
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
 *
 * @author Tobias Malbrecht, Ingo Mierswa, Sebastian Land, Marius Helf
 */
public class AggregationOperator extends AbstractDataProcessing {

	public static class AggregationTreeNode {

		private TreeMap<Object, AggregationTreeNode> childrenMap = null;
		private TreeMap<Object, LeafAggregationTreeNode> leafMap = null;

		public AggregationTreeNode getOrCreateChild(Object value) {
			// creating map dynamically to save allocated objects in case this won't be used
			if (childrenMap == null) {
				childrenMap = new TreeMap<>();
			}

			// searching entry and creating it if necessary
			AggregationTreeNode childNode = childrenMap.get(value);
			if (childNode == null) {
				childNode = new AggregationTreeNode();
				childrenMap.put(value, childNode);
			}
			return childNode;
		}

		public AggregationTreeNode getChild(Object value) {
			if (childrenMap != null) {
				return childrenMap.get(value);
			}
			return null;
		}

		public Set<Entry<Object, AggregationTreeNode>> getChilds() {
			return childrenMap.entrySet();
		}

		public LeafAggregationTreeNode getOrCreateLeaf(Object value, List<AggregationFunction> aggregationFunctions) {
			// creating map dynamically to save allocated objects in case this won't be used
			if (leafMap == null) {
				leafMap = new TreeMap<>();
			}

			// searching entry and creating it if necessary
			LeafAggregationTreeNode leafNode = leafMap.get(value);
			if (leafNode == null) {
				leafNode = new LeafAggregationTreeNode(aggregationFunctions);
				leafMap.put(value, leafNode);
			}
			return leafNode;
		}

		public LeafAggregationTreeNode getLeaf(Object value) {
			if (leafMap != null) {
				return leafMap.get(value);
			}
			return null;
		}

		public Set<Entry<Object, LeafAggregationTreeNode>> getLeaves() {
			return leafMap.entrySet();
		}

		public Collection<? extends Object> getValues() {
			if (childrenMap != null) {
				return childrenMap.keySet();
			}
			if (leafMap != null) {
				return leafMap.keySet();
			}
			return Collections.emptyList();
		}
	}

	public static class LeafAggregationTreeNode {

		private List<Aggregator> aggregators;

		/**
		 * Creates a new {@link LeafAggregationTreeNode} for all the given
		 * {@link AggregationFunction}s. For each function, one {@link Aggregator} will be created,
		 * that will keep track of the current counted values.
		 */
		public LeafAggregationTreeNode(List<AggregationFunction> aggregationFunctions) {
			aggregators = new ArrayList<>(aggregationFunctions.size());
			for (AggregationFunction function : aggregationFunctions) {
				aggregators.add(function.createAggregator());
			}
		}

		/**
		 * This will count the given examples for all registered {@link Aggregator}s.
		 */
		public void count(Example example) {
			for (Aggregator aggregator : aggregators) {
				aggregator.count(example);
			}
		}

		/**
		 * This will count the given examples for all registered {@link Aggregator}s with the given
		 * weight. If there's no weight attribute available, it is preferable to use the
		 * {@link #count(Example)} method, as it might be more efficiently implemented.
		 */
		public void count(Example example, double weight) {
			for (Aggregator aggregator : aggregators) {
				aggregator.count(example, weight);
			}
		}

		/**
		 * This simply returns the list of all aggregators. They may be used for setting values
		 * within the respective data row of the created example set.
		 */
		public List<Aggregator> getAggregators() {
			return aggregators;
		}
	}

	public static final String PARAMETER_USE_DEFAULT_AGGREGATION = "use_default_aggregation";
	public static final String PARAMETER_DEFAULT_AGGREGATION_FUNCTION = "default_aggregation_function";
	public static final String PARAMETER_AGGREGATION_ATTRIBUTES = "aggregation_attributes";
	public static final String PARAMETER_AGGREGATION_FUNCTIONS = "aggregation_functions";
	public static final String PARAMETER_GROUP_BY_ATTRIBUTES = "group_by_attributes";
	public static final String PARAMETER_ONLY_DISTINCT = "only_distinct";
	public static final String PARAMETER_IGNORE_MISSINGS = "ignore_missings";
	public static final String PARAMETER_ALL_COMBINATIONS = "count_all_combinations";

	/* These two only remain for compatibility */
	public static final String GENERIC_GROUP_NAME = "group";
	public static final String GENERIC_ALL_NAME = "all";

	/*
	 * Later from this version, no group attribute will be created if no attributes for groups were
	 * selected. Also after this numerical attributes used for grouping will not be transformed into
	 * nominal attributes anymore.
	 */
	private static final OperatorVersion VERSION_5_1_6 = new OperatorVersion(5, 1, 6);

	private static final OperatorVersion VERSION_5_2_8 = new OperatorVersion(5, 2, 8);

	/*
	 * From version 6.0.7 on the operator will throw an user error if group-by-attributes argument
	 * refers to an attribute not present in the example set or is empty
	 */
	private static final OperatorVersion VERSION_6_0_6 = new OperatorVersion(6, 0, 6);

	/*
	 * From version 7.5.0 on the operator will use a new MedianAggregator
	 */
	static final OperatorVersion VERSION_7_4_0 = new OperatorVersion(7, 4, 0);

	/**
	 * After version 8.2.0, special grouping attributes keep their role and
	 * {@link AggregationFunction#FUNCTION_NAME_CONCATENATION} will support {@link #PARAMETER_ONLY_DISTINCT}
	 */
	static final OperatorVersion VERSION_8_2_0 = new OperatorVersion(8, 2, 0);

	private final AttributeSubsetSelector attributeSelector = new AttributeSubsetSelector(this, getExampleSetInputPort());

	public AggregationOperator(OperatorDescription desc) {
		super(desc);
	}

	@Override
	protected MetaData modifyMetaData(ExampleSetMetaData metaData) throws UndefinedParameterError {
		ExampleSetMetaData resultMD = metaData.clone();
		resultMD.clear();

		// add group by attributes
		if (isParameterSet(PARAMETER_GROUP_BY_ATTRIBUTES)
				&& !getParameterAsString(PARAMETER_GROUP_BY_ATTRIBUTES).isEmpty()) {
			String attributeRegex = getParameterAsString(PARAMETER_GROUP_BY_ATTRIBUTES);
			Pattern pattern = Pattern.compile(attributeRegex);

			for (AttributeMetaData amd : metaData.getAllAttributes()) {
				if (pattern.matcher(amd.getName()).matches()) {
					if (amd.isNumerical() && getCompatibilityLevel().isAtMost(VERSION_5_1_6)) {
						// converting type to mimic NumericalToPolynomial used below
						amd.setType(Ontology.NOMINAL);
						amd.setValueSet(Collections.<String>emptySet(), SetRelation.SUPERSET);
					}
					resultMD.addAttribute(amd);
				}
			}
			resultMD.getNumberOfExamples().reduceByUnknownAmount();
		}
		if (resultMD.getAllAttributes().isEmpty() && getCompatibilityLevel().isAtMost(VERSION_5_1_6)) {
			AttributeMetaData allGroup = new AttributeMetaData(GENERIC_GROUP_NAME, Ontology.NOMINAL);
			Set<String> values = new TreeSet<>();
			values.add(GENERIC_ALL_NAME);
			allGroup.setValueSet(values, SetRelation.EQUAL);
			resultMD.addAttribute(allGroup);
			resultMD.setNumberOfExamples(new MDInteger(1));
		}

		// add aggregated attributes of default aggregation: They will apply only to those attribute
		// not mentioned explicitly
		List<String[]> parameterList = this.getParameterList(PARAMETER_AGGREGATION_ATTRIBUTES);
		HashSet<String> explicitDefinedAttributes = new HashSet<>();
		for (String[] function : parameterList) {
			explicitDefinedAttributes.add(function[0]);
		}
		if (getParameterAsBoolean(PARAMETER_USE_DEFAULT_AGGREGATION)) {
			String defaultFunction = getParameterAsString(PARAMETER_DEFAULT_AGGREGATION_FUNCTION);
			ExampleSetMetaData metaDataSubset = attributeSelector.getMetaDataSubset(metaData, false);
			for (AttributeMetaData amd : metaDataSubset.getAllAttributes()) {
				if (!explicitDefinedAttributes.contains(amd.getName())) {
					AttributeMetaData newAMD = AggregationFunction.getAttributeMetaData(defaultFunction, amd,
							getExampleSetInputPort());
					if (newAMD != null) {
						resultMD.addAttribute(newAMD);
					}
				}
			}
		}

		// add explicitly defined attributes of list
		for (String[] function : parameterList) {
			AttributeMetaData amd = metaData.getAttributeByName(function[0]);
			if (amd != null) {
				AttributeMetaData newMD = AggregationFunction.getAttributeMetaData(function[1], amd,
						getExampleSetInputPort());
				if (newMD != null) {
					resultMD.addAttribute(newMD);
				}
			} else {
				// in this case we should register a warning, but continue anyway in cases we don't
				// have the correct set available
				getExampleSetInputPort().addError(new SimpleMetaDataError(Severity.WARNING, getExampleSetInputPort(),
						"aggregation.attribute_unknown", function[0]));
				AttributeMetaData newAMD = AggregationFunction.getAttributeMetaData(function[1],
						new AttributeMetaData(function[0], Ontology.ATTRIBUTE_VALUE), getExampleSetInputPort());
				if (newAMD != null) {
					resultMD.addAttribute(newAMD);
				}
			}
		}

		if (getCompatibilityLevel().isAbove(VERSION_6_0_6)) {
			String[] groupByAttributes = getParameterAsString(PARAMETER_GROUP_BY_ATTRIBUTES)
					.split(ParameterTypeAttributes.ATTRIBUTE_SEPARATOR_REGEX);
			for (String attribute : groupByAttributes) {
				// if the list is empty, there is already a warning:
				if (!attribute.isEmpty() && metaData.getAttributeByName(attribute) == null) {
					getExampleSetInputPort().addError(new SimpleMetaDataError(Severity.WARNING, getExampleSetInputPort(),
							"aggregation.group_by_attribute_unknown", attribute));
				}
			}
		}

		return resultMD;
	}

	@Override
	public ExampleSet apply(ExampleSet exampleSet) throws OperatorException {
		// creating data structures for building aggregates
		List<AggregationFunction> aggregationFunctions = createAggreationFunctions(exampleSet);
		Attribute[] groupAttributes;

		/*
		 * From version 6.0.7 on, the group-by-attribute parameter must contain valid attribute
		 * names (e.g. the names must be presented in the example set and do not contain invalid
		 * characters like "|").
		 */
		if (getCompatibilityLevel().isAbove(VERSION_6_0_6)) {
			String[] groupByAttributesNames = getParameterAsString(PARAMETER_GROUP_BY_ATTRIBUTES)
					.split(ParameterTypeAttributes.ATTRIBUTE_SEPARATOR_REGEX);

			// if the group-by-attributes parameter is empty, we can skip these checks
			boolean emptySelection = groupByAttributesNames.length == 1 && groupByAttributesNames[0].trim().isEmpty();
			if (emptySelection) {
				groupAttributes = new Attribute[0];
			} else {
				List<String> unknownAttributes = new ArrayList<>();
				Set<Attribute> groupByAttributes = new HashSet<>(groupByAttributesNames.length);

				for (String attributeName : groupByAttributesNames) {
					// the user can add empty attribute names by accident
					if (attributeName.trim().length() == 0) {
						continue;
					}
					Attribute attribute = exampleSet.getAttributes().get(attributeName);
					if (attribute == null) {
						unknownAttributes.add(attributeName);
					} else {
						groupByAttributes.add(attribute);
					}
				}

				if (unknownAttributes.size() != 0) {
					if (unknownAttributes.size() > 1) {
						throw new UserError(this, "aggregate_group_by_not_found", StringUtils.join(unknownAttributes, ", "));
					} else {
						throw new UserError(this, "aggregate_group_by_not_found_single", unknownAttributes.get(0));
					}
				} else {
					// keep the attribute ordering of the input example set (to conform with the
					// legacy code)
					groupAttributes = new Attribute[groupByAttributes.size()];
					Iterator<Attribute> it = exampleSet.getAttributes().allAttributes();
					int i = 0;
					while (it.hasNext()) {
						Attribute attribute = it.next();
						if (groupByAttributes.contains(attribute)) {
							groupAttributes[i] = attribute;
							i++;
						}
					}
				}
			}
		} else {
			groupAttributes = getMatchingAttributes(exampleSet.getAttributes(),
					getParameterAsString(PARAMETER_GROUP_BY_ATTRIBUTES));
		}

		Attribute weightAttribute = exampleSet.getAttributes().getWeight();
		boolean useWeights = weightAttribute != null;

		// running over exampleSet and aggregate data of each example
		AggregationTreeNode rootNode = new AggregationTreeNode();
		LeafAggregationTreeNode leafNode = null;
		if (groupAttributes.length == 0) {
			// if no grouping, we will directly insert into leaf node
			leafNode = new LeafAggregationTreeNode(aggregationFunctions);
		}
		getProgress().setTotal(exampleSet.size());
		int progressCounter = 0;
		for (Example example : exampleSet) {
			if (groupAttributes.length > 0) {
				AggregationTreeNode currentNode = rootNode;
				// now traversing aggregation tree for m-1 group attributes
				for (int i = 0; i < groupAttributes.length - 1; i++) {
					Attribute currentAttribute = groupAttributes[i];
					if (currentAttribute.isNominal()) {
						currentNode = currentNode.getOrCreateChild(example.getValueAsString(currentAttribute));
					} else {
						currentNode = currentNode.getOrCreateChild(example.getValue(currentAttribute));
					}
				}

				// now we have to get the leaf node containing the aggregators
				Attribute currentAttribute = groupAttributes[groupAttributes.length - 1];
				if (currentAttribute.isNominal()) {
					leafNode = currentNode.getOrCreateLeaf(example.getValueAsString(currentAttribute), aggregationFunctions);
				} else {
					leafNode = currentNode.getOrCreateLeaf(example.getValue(currentAttribute), aggregationFunctions);
				}
			}
			// now count current example
			if (!useWeights) {
				leafNode.count(example);
			} else {
				leafNode.count(example, example.getValue(weightAttribute));
			}

			// Trigger operator progress
			if (++progressCounter % 100 == 0) {
				getProgress().setCompleted(progressCounter);
			}
		}

		// now derive new example set from aggregated values
		boolean isCountingAllCombinations = getParameterAsBoolean(PARAMETER_ALL_COMBINATIONS);

		// building new attributes from grouping attributes and aggregation functions
		Attribute[] newAttributes = new Attribute[groupAttributes.length + aggregationFunctions.size()];
		for (int i = 0; i < groupAttributes.length; i++) {
			newAttributes[i] = AttributeFactory.createAttribute(groupAttributes[i]);
		}
		int i = groupAttributes.length;
		for (AggregationFunction function : aggregationFunctions) {
			newAttributes[i] = function.getTargetAttribute();
			i++;
		}

		// creating example set
		ExampleSetBuilder builder = ExampleSets.from(newAttributes);
		// preserve roles of grouping attributes
		if (getCompatibilityLevel().isAbove(VERSION_8_2_0)) {
			int attributeArrayIndex = 0;
			for (Attribute groupAttribute : groupAttributes) {
				AttributeRole role = exampleSet.getAttributes().getRole(groupAttribute);
				if (role != null && role.getSpecialName() != null) {
					builder.withRole(newAttributes[attributeArrayIndex], role.getSpecialName());
				}
				attributeArrayIndex++;
			}
		}
		DataRowFactory factory = new DataRowFactory(DataRowFactory.TYPE_DOUBLE_ARRAY, '.');
		double[] dataOfUpperLevels = new double[groupAttributes.length];

		// prepare empty lists
		ArrayList<List<Aggregator>> allAggregators = new ArrayList<>();
		for (int aggregatorIdx = 0; aggregatorIdx < aggregationFunctions.size(); ++aggregatorIdx) {
			allAggregators.add(new ArrayList<>());
		}

		ArrayList<double[]> allGroupCombinations = new ArrayList<>();

		if (groupAttributes.length > 0) {
			// going through all possible groups recursively
			parseTree(rootNode, groupAttributes, dataOfUpperLevels, 0, allGroupCombinations, allAggregators, factory,
					newAttributes, isCountingAllCombinations, aggregationFunctions);
		} else {
			// just enter values from single leaf node
			parseLeaf(leafNode, dataOfUpperLevels, allGroupCombinations, allAggregators, factory, newAttributes,
					aggregationFunctions);
		}

		// apply post-processing
		int currentFunctionIdx = 0;
		for (AggregationFunction aggregationFunction : aggregationFunctions) {
			aggregationFunction.postProcessing(allAggregators.get(currentFunctionIdx));
			++currentFunctionIdx;
		}

		// write data into table
		builder.withExpectedSize(allGroupCombinations.size());
		int currentRow = 0;
		for (double[] groupValues : allGroupCombinations) {
			double[] rowData = new double[newAttributes.length];

			// copy group values into row
			System.arraycopy(groupValues, 0, rowData, 0, groupValues.length);
			DoubleArrayDataRow dataRow = new DoubleArrayDataRow(rowData);

			// copy aggregated values into row
			int currentColumn = groupValues.length;
			for (List<Aggregator> aggregatorsForColumn : allAggregators) {
				Aggregator aggregatorForCurrentCell = aggregatorsForColumn.get(currentRow);
				Attribute currentAttribute = newAttributes[currentColumn];
				if (aggregatorForCurrentCell != null) {
					aggregatorForCurrentCell.set(currentAttribute, dataRow);
				} else {
					aggregationFunctions.get(currentColumn - groupAttributes.length).setDefault(currentAttribute, dataRow);
				}
				++currentColumn;
			}
			builder.addDataRow(dataRow);
			++currentRow;
		}

		// postprocessing for remaining compatibility: Old versions automatically added group "all".
		// Must remain this way for old operator
		// version
		if (getCompatibilityLevel().isAtMost(VERSION_5_1_6)) {
			if (groupAttributes.length == 0) {
				ExampleSet resultSet = builder.build();
				Attribute resultGroupAttribute = AttributeFactory.createAttribute(GENERIC_GROUP_NAME, Ontology.NOMINAL);
				resultSet.getExampleTable().addAttribute(resultGroupAttribute);
				resultSet.getAttributes().addRegular(resultGroupAttribute);
				resultSet.getExample(0).setValue(resultGroupAttribute,
						resultGroupAttribute.getMapping().mapString(GENERIC_ALL_NAME));

				resultSet.getAnnotations().addAll(exampleSet.getAnnotations());
				for (Attribute attribute : newAttributes) {
					resultSet.getAttributes().remove(attribute);
					resultSet.getAttributes().addRegular(attribute);
				}
				return resultSet;
			} else {
				// make attributes nominal
				ExampleSet resultSet = builder.build();
				resultSet.getAnnotations().addAll(exampleSet.getAnnotations());
				try {
					NumericToNominal toNominalOperator = OperatorService.createOperator(NumericToPolynominal.class);
					toNominalOperator.setParameter(AttributeSubsetSelector.PARAMETER_FILTER_TYPE,
							AttributeSubsetSelector.CONDITION_REGULAR_EXPRESSION + "");
					toNominalOperator.setParameter(RegexpAttributeFilter.PARAMETER_REGULAR_EXPRESSION,
							getParameterAsString(PARAMETER_GROUP_BY_ATTRIBUTES));
					toNominalOperator.setParameter(AttributeSubsetSelector.PARAMETER_INCLUDE_SPECIAL_ATTRIBUTES, "true");
					return toNominalOperator.apply(resultSet);
				} catch (OperatorCreationException e) {
					// otherwise compatibility could not be ensured
					return resultSet;
				}
			}
		}

		// for recent version table is correct: Deliver example set
		ExampleSet resultSet = builder.build();
		resultSet.getAnnotations().addAll(exampleSet.getAnnotations());
		return resultSet;
	}

	private void parseLeaf(LeafAggregationTreeNode node, double[] dataOfUpperLevels, List<double[]> allGroupCombinations,
						   List<List<Aggregator>> allAggregators, DataRowFactory factory, Attribute[] newAttributes,
						   List<AggregationFunction> aggregationFunctions) {
		// first copying data from groups
		double[] newGroupCombination = new double[dataOfUpperLevels.length];
		System.arraycopy(dataOfUpperLevels, 0, newGroupCombination, 0, dataOfUpperLevels.length);
		allGroupCombinations.add(newGroupCombination);

		// DoubleArrayDataRow row = new DoubleArrayDataRow(newData);

		// check whether leaf exists
		if (node != null) {
			// int i = dataOfUpperLevels.length; // number of group attributes
			int i = 0;
			for (Aggregator aggregator : node.getAggregators()) {
				allAggregators.get(i).add(aggregator);
				// aggregator.set(newAttributes[i], row);
				i++;
			}
		} else {
			// fill in defaults for all aggregation functions
			// int i = dataOfUpperLevels.length; // number of group attributes
			// for (AggregationFunction function : aggregationFunctions) {
			// function.setDefault(newAttributes[i], row);
			// i++;
			for (List<Aggregator> current : allAggregators) {
				current.add(null);
			}
		}

		// table.addDataRow(row);
	}

	private void parseTree(AggregationTreeNode node, Attribute[] groupAttributes, double[] dataOfUpperLevels, int groupLevel,
						   List<double[]> allGroupCombinations, List<List<Aggregator>> allAggregators, DataRowFactory factory,
						   Attribute[] newAttributes, boolean isCountingAllCombinations, List<AggregationFunction> aggregationFunctions)
			throws UserError {
		Attribute currentAttribute = groupAttributes[groupLevel];
		if (currentAttribute.isNominal()) {
			Collection<? extends Object> nominalValues = null;
			if (isCountingAllCombinations) {
				nominalValues = currentAttribute.getMapping().getValues();
			} else {
				nominalValues = node.getValues();
			}
			for (Object nominalValue : nominalValues) {
				dataOfUpperLevels[groupLevel] = newAttributes[groupLevel].getMapping().mapString(nominalValue.toString());
				// check if we have more group defining attributes
				if (groupLevel + 1 < groupAttributes.length) {
					parseTree(node.getOrCreateChild(nominalValue), groupAttributes, dataOfUpperLevels, groupLevel + 1,
							allGroupCombinations, allAggregators, factory, newAttributes, isCountingAllCombinations,
							aggregationFunctions);
				} else {
					// if not, insert values from aggregation functions
					parseLeaf(node.getLeaf(nominalValue), dataOfUpperLevels, allGroupCombinations, allAggregators, factory,
							newAttributes, aggregationFunctions);
				}

			}
		} else if (currentAttribute.isNumerical()
				|| Ontology.ATTRIBUTE_VALUE_TYPE.isA(currentAttribute.getValueType(), Ontology.DATE_TIME)) {
			for (Object numericalValue : node.getValues()) {
				dataOfUpperLevels[groupLevel] = (Double) numericalValue;
				if (groupLevel + 1 < groupAttributes.length) {
					parseTree(node.getOrCreateChild(numericalValue), groupAttributes, dataOfUpperLevels, groupLevel + 1,
							allGroupCombinations, allAggregators, factory, newAttributes, isCountingAllCombinations,
							aggregationFunctions);
				} else {
					// if not, insert values from aggregation functions
					parseLeaf(node.getLeaf(numericalValue), dataOfUpperLevels, allGroupCombinations, allAggregators, factory,
							newAttributes, aggregationFunctions);
				}
			}
		} else {
			throw new UserError(this, "aggregation_operator.unsupported_value_type", currentAttribute.getName(),
					Ontology.ATTRIBUTE_VALUE_TYPE.getNames()[currentAttribute.getValueType()]);
		}
	}

	private Attribute[] getMatchingAttributes(Attributes attributes, String regex) throws OperatorException {
		Pattern pattern = null;
		try {
			pattern = Pattern.compile(regex);
		} catch (PatternSyntaxException e) {
			throw new UserError(this, 206, regex, e.getMessage());
		}
		List<Attribute> attributeList = new LinkedList<>();
		Iterator<Attribute> iterator = attributes.allAttributes();
		while (iterator.hasNext()) {
			Attribute attribute = iterator.next();
			if (pattern.matcher(attribute.getName()).matches()) {
				attributeList.add(attribute);
			}
		}

		// building array of attributes for faster access.
		Attribute[] attributesArray = new Attribute[attributeList.size()];
		attributesArray = attributeList.toArray(attributesArray);
		return attributesArray;
	}

	private List<AggregationFunction> createAggreationFunctions(ExampleSet exampleSet) throws OperatorException {
		// load global switches
		boolean ignoreMissings = getParameterAsBoolean(PARAMETER_IGNORE_MISSINGS);
		boolean countOnlyDistinct = getParameterAsBoolean(PARAMETER_ONLY_DISTINCT);

		// creating data structures for building aggregates
		List<AggregationFunction> aggregationFunctions = new LinkedList<>();

		// building functions for all explicitly defined aggregation attributes
		Set<Attribute> explicitlyAggregatedAttributes = new HashSet<>();
		List<String[]> aggregationFunctionPairs = getParameterList(PARAMETER_AGGREGATION_ATTRIBUTES);
		for (String[] aggregationFunctionPair : aggregationFunctionPairs) {
			Attribute attribute = exampleSet.getAttributes().get(aggregationFunctionPair[0]);
			if (attribute == null) {
				throw new UserError(this, "aggregation.aggregation_attribute_not_present", aggregationFunctionPair[0]);
			}
			AggregationFunction function = AggregationFunction.createAggregationFunction(aggregationFunctionPair[1],
					attribute, ignoreMissings, countOnlyDistinct, getCompatibilityLevel());
			if (!function.isCompatible()) {
				throw new UserError(this, "aggregation.incompatible_attribute_type", attribute.getName(),
						aggregationFunctionPair[1]);
			}
			// adding objects for this attribute to structure
			explicitlyAggregatedAttributes.add(attribute);
			aggregationFunctions.add(function);
		}

		// building the default aggregations
		if (getParameterAsBoolean(PARAMETER_USE_DEFAULT_AGGREGATION)) {
			String defaultAggregationFunctionName = getParameterAsString(PARAMETER_DEFAULT_AGGREGATION_FUNCTION);

			Iterator<Attribute> iterator = attributeSelector.getAttributeSubset(exampleSet, false).iterator();
			if (getCompatibilityLevel().isAtMost(VERSION_5_2_8)) {
				iterator = exampleSet.getAttributes().iterator();
			}

			while (iterator.hasNext()) {
				Attribute attribute = iterator.next();
				if (!explicitlyAggregatedAttributes.contains(attribute)) {
					AggregationFunction function = AggregationFunction.createAggregationFunction(
							defaultAggregationFunctionName, attribute, ignoreMissings, countOnlyDistinct, getCompatibilityLevel());
					if (function.isCompatible()) {
						aggregationFunctions.add(function);
					}
				}
			}
		}

		return aggregationFunctions;
	}

	@Override
	public List<ParameterType> getParameterTypes() {
		List<ParameterType> types = super.getParameterTypes();

		types.add(new ParameterTypeBoolean(PARAMETER_USE_DEFAULT_AGGREGATION,
				"If checked you can select a default aggregation function for a subset of the attributes.", false, false));
		List<ParameterType> parameterTypes = attributeSelector.getParameterTypes();
		for (ParameterType type : parameterTypes) {
			type.registerDependencyCondition(
					new BooleanParameterCondition(this, PARAMETER_USE_DEFAULT_AGGREGATION, false, true));
			types.add(type);
		}
		String[] functions = AggregationFunction.getAvailableAggregationFunctionNames();
		ParameterType type = new ParameterTypeStringCategory(PARAMETER_DEFAULT_AGGREGATION_FUNCTION,
				"The type of the used aggregation function for all default attributes.", functions, functions[0]);
		type.registerDependencyCondition(
				new BooleanParameterCondition(this, PARAMETER_USE_DEFAULT_AGGREGATION, false, true));
		type.setExpert(false);
		types.add(type);

		ParameterTypeList aggregation_attribute = new ParameterTypeList(PARAMETER_AGGREGATION_ATTRIBUTES, "The attributes which should be aggregated.",
				new ParameterTypeAttribute("aggregation_attribute", "Specifies the attribute which is aggregated.",
						getExampleSetInputPort(), false),
				new ParameterTypeStringCategory(PARAMETER_AGGREGATION_FUNCTIONS,
						"The type of the used aggregation function.", functions, functions[0]),
				false);
		aggregation_attribute.setPrimary(true);
		types.add(aggregation_attribute);
		types.add(new ParameterTypeAttributes(PARAMETER_GROUP_BY_ATTRIBUTES,
				"Performs a grouping by the values of the attributes by the selected attributes.", getExampleSetInputPort(),
				true, false));
		types.add(new ParameterTypeBoolean(PARAMETER_ALL_COMBINATIONS,
				"Indicates that all possible combinations of the values of the group by attributes are counted, even if they don't occur. Please handle with care, since the number might be enormous.",
				false));
		type = new ParameterTypeBoolean(PARAMETER_ONLY_DISTINCT,
				"Indicates if only rows with distinct values for the aggregation attribute should be used for the calculation of the aggregation function.",
				false);
		type.registerDependencyCondition(new BooleanParameterCondition(this, PARAMETER_ALL_COMBINATIONS, false, false));
		types.add(type);
		types.add(new ParameterTypeBoolean(PARAMETER_IGNORE_MISSINGS,
				"Indicates if missings should be ignored and aggregation should be based only on existing values or not. In the latter case the aggregated value will be missing in the presence of missing values.",
				true));
		return types;
	}

	@Override
	public OperatorVersion[] getIncompatibleVersionChanges() {
		return (OperatorVersion[]) ArrayUtils.addAll(super.getIncompatibleVersionChanges(),
				new OperatorVersion[]{VERSION_5_1_6, VERSION_5_2_8, VERSION_6_0_6, VERSION_7_4_0, VERSION_8_2_0});
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
