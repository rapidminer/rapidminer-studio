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

import java.util.Collections;
import java.util.List;
import java.util.Vector;

import com.rapidminer.RapidMiner;
import com.rapidminer.example.Attribute;
import com.rapidminer.example.Example;
import com.rapidminer.example.ExampleSet;
import com.rapidminer.example.set.SortedExampleSet;
import com.rapidminer.example.table.AttributeFactory;
import com.rapidminer.example.table.DataRow;
import com.rapidminer.example.table.DataRowFactory;
import com.rapidminer.example.utils.ExampleSetBuilder;
import com.rapidminer.example.utils.ExampleSets;
import com.rapidminer.operator.OperatorDescription;
import com.rapidminer.operator.OperatorException;
import com.rapidminer.operator.UserError;
import com.rapidminer.operator.annotation.ResourceConsumptionEstimator;
import com.rapidminer.operator.error.AttributeNotFoundError;
import com.rapidminer.operator.generator.ExampleSetGenerator;
import com.rapidminer.operator.ports.metadata.AttributeMetaData;
import com.rapidminer.operator.ports.metadata.AttributeSetPrecondition;
import com.rapidminer.operator.ports.metadata.ExampleSetMetaData;
import com.rapidminer.operator.ports.metadata.MDInteger;
import com.rapidminer.operator.ports.metadata.MetaData;
import com.rapidminer.operator.ports.metadata.SetRelation;
import com.rapidminer.parameter.ParameterType;
import com.rapidminer.parameter.ParameterTypeAttribute;
import com.rapidminer.parameter.ParameterTypeBoolean;
import com.rapidminer.parameter.ParameterTypeCategory;
import com.rapidminer.parameter.UndefinedParameterError;
import com.rapidminer.parameter.conditions.BooleanParameterCondition;
import com.rapidminer.tools.Ontology;
import com.rapidminer.tools.OperatorResourceConsumptionHandler;
import com.rapidminer.tools.ParameterService;
import com.rapidminer.tools.math.function.aggregation.AbstractAggregationFunction;
import com.rapidminer.tools.math.function.aggregation.AggregationFunction;
import com.rapidminer.tools.parameter.internal.DataManagementParameterHelper;


/**
 * <p>
 * Transforms an example set by grouping multiple examples of single groups into single examples.
 * The parameter <em>group_attribute</em> specifies an attribute which identifies examples belonging
 * to the groups. The parameter <em>index_attribute</em> specifies an attribute whose values are
 * used to identify the examples inside the groups. The values of this attributes are used to name
 * the group attributes which are created during the pivoting. Typically the values of such an
 * attribute capture subgroups or dates. If the source example set contains example weights, these
 * weights may be aggregated in each group to maintain the weightings among groups.
 * </p>
 *
 * @author Tobias Malbrecht
 * @deprecated since 9.1.0, use {@link com.rapidminer.operator.preprocessing.transformation.pivot.PivotOperator} instead
 */
public class Example2AttributePivoting extends ExampleSetTransformationOperator {

	public static final String PARAMETER_GROUP_ATTRIBUTE = "group_attribute";

	public static final String PARAMETER_INDEX_ATTRIBUTE = "index_attribute";

	public static final String PARAMETER_CONSIDER_WEIGHTS = "consider_weights";

	public static final String PARAMETER_WEIGHT_AGGREGATION = "weight_aggregation";

	public static final String PARAMETER_SKIP_CONSTANT_ATTRIBUTES = "skip_constant_attributes";

	/** The parameter name for &quot;Determines, how the data is represented internally.&quot; */
	public static final String PARAMETER_DATAMANAGEMENT = ExampleSetGenerator.PARAMETER_DATAMANAGEMENT;

	public Example2AttributePivoting(OperatorDescription description) {
		super(description);

		getExampleSetInputPort()
				.addPrecondition(new AttributeSetPrecondition(getExampleSetInputPort(), AttributeSetPrecondition
						.getAttributesByParameter(this, PARAMETER_GROUP_ATTRIBUTE, PARAMETER_INDEX_ATTRIBUTE)));
	}

	@Override
	protected MetaData modifyMetaData(ExampleSetMetaData metaData) throws UndefinedParameterError {
		AttributeMetaData groupAttribute = metaData.getAttributeByName(getParameterAsString(PARAMETER_GROUP_ATTRIBUTE));
		AttributeMetaData indexAttribute = metaData.getAttributeByName(getParameterAsString(PARAMETER_INDEX_ATTRIBUTE));

		if (groupAttribute != null && indexAttribute != null) {
			ExampleSetMetaData emd = new ExampleSetMetaData();
			// number of examples
			if (groupAttribute.isNominal()) {
				emd.setNumberOfExamples(groupAttribute.getValueSet().size());
				if (groupAttribute.getValueSetRelation() == SetRelation.SUBSET) {
					emd.getNumberOfExamples().reduceByUnknownAmount();
				} else if (groupAttribute.getValueSetRelation() == SetRelation.SUPERSET) {
					emd.getNumberOfExamples().increaseByUnknownAmount();
				}
			} else {
				emd.setNumberOfExamples(new MDInteger());
			}
			// attributes
			if (indexAttribute.isNominal()) {
				// nominal index attribute
				for (AttributeMetaData originalAMD : metaData.getAllAttributes()) {
					if (!originalAMD.isSpecial() && originalAMD != indexAttribute && originalAMD != groupAttribute) {
						if (indexAttribute.getValueSet().size() > 1) {
							for (String value : indexAttribute.getValueSet()) {
								AttributeMetaData newIndexedAttribute = originalAMD.clone();
								newIndexedAttribute.setName(originalAMD.getName() + "_" + value);
								newIndexedAttribute.getNumberOfMissingValues().increaseByUnknownAmount();
								newIndexedAttribute.setValueSetRelation(SetRelation.SUBSET);
								emd.addAttribute(newIndexedAttribute);
								emd.mergeSetRelation(newIndexedAttribute.getValueSetRelation());
							}
						} else {
							AttributeMetaData newIndexedAttribute = originalAMD.clone();
							emd.addAttribute(newIndexedAttribute);
						}
					}
					if (originalAMD == groupAttribute) {
						emd.addAttribute(originalAMD.clone());
					}
				}
			} else {
				// numerical index attribute
				// add range borders in order to have an example how it could look like
				for (AttributeMetaData originalAMD : metaData.getAllAttributes()) {
					if (!originalAMD.isSpecial() && originalAMD != indexAttribute && originalAMD != groupAttribute) {
						AttributeMetaData newIndexedAttribute = originalAMD.clone();
						newIndexedAttribute
								.setName(originalAMD.getName() + "_" + newIndexedAttribute.getValueRange().getLower());
						newIndexedAttribute.getNumberOfMissingValues().increaseByUnknownAmount();
						newIndexedAttribute.setValueSetRelation(SetRelation.SUBSET);
						emd.addAttribute(newIndexedAttribute);
						newIndexedAttribute = originalAMD.clone();
						newIndexedAttribute
								.setName(originalAMD.getName() + "_" + newIndexedAttribute.getValueRange().getUpper());
						newIndexedAttribute.getNumberOfMissingValues().increaseByUnknownAmount();
						newIndexedAttribute.setValueSetRelation(SetRelation.SUBSET);
						emd.addAttribute(newIndexedAttribute);
					}
					if (originalAMD == groupAttribute) {
						emd.addAttribute(originalAMD.clone());
					}
				}
				emd.mergeSetRelation(SetRelation.SUPERSET);
			}

			return emd;
		} else {
			return new ExampleSetMetaData();
		}
	}

	@Override
	public ExampleSet apply(ExampleSet sourceExampleSet) throws OperatorException {
		boolean skipConstantAttributes = getParameterAsBoolean(PARAMETER_SKIP_CONSTANT_ATTRIBUTES);
		String groupAttributeName = getParameterAsString(PARAMETER_GROUP_ATTRIBUTE);
		String indexAttributeName = getParameterAsString(PARAMETER_INDEX_ATTRIBUTE);
		boolean considerWeights = getParameterAsBoolean(PARAMETER_CONSIDER_WEIGHTS);
		int weightAggregationFunctionIndex = getParameterAsInt(PARAMETER_WEIGHT_AGGREGATION);

		Attribute groupAttribute = sourceExampleSet.getAttributes().get(groupAttributeName);
		if (groupAttribute == null) {
			throw new AttributeNotFoundError(this, PARAMETER_GROUP_ATTRIBUTE, groupAttributeName);
		}

		Attribute indexAttribute = sourceExampleSet.getAttributes().get(indexAttributeName);
		if (indexAttribute == null) {
			throw new AttributeNotFoundError(this, PARAMETER_INDEX_ATTRIBUTE, indexAttributeName);
		}

		Attribute weightAttribute = sourceExampleSet.getAttributes().getWeight();

		SortedExampleSet exampleSet = new SortedExampleSet(
				new SortedExampleSet(sourceExampleSet, indexAttribute, SortedExampleSet.INCREASING), groupAttribute,
				SortedExampleSet.INCREASING);
		// identify static or dynamic attributes and record index values
		List<String> indexValues = new Vector<>();
		Attribute[] attributes = exampleSet.getAttributes().createRegularAttributeArray();
		boolean[] constantAttributeValues = new boolean[attributes.length];
		for (int i = 0; i < constantAttributeValues.length; i++) {
			constantAttributeValues[i] = true;
		}

		// init operator progress
		getProgress().setTotal(100);

		Example lastExample = null;
		int counter = 0;
		for (Example example : exampleSet) {
			if (lastExample != null) {
				if (lastExample.getValue(groupAttribute) == example.getValue(groupAttribute)) {
					for (int i = 0; i < attributes.length; i++) {
						Attribute attribute = attributes[i];
						if (Double.isNaN(lastExample.getValue(attribute)) && Double.isNaN(example.getValue(attribute))) {
							continue;
						}
						if (lastExample.getValue(attribute) != example.getValue(attribute)) {
							constantAttributeValues[i] = false;
							continue;
						}
					}
				}
			}
			String indexValue = example.getValueAsString(indexAttribute);
			if (!indexValues.contains(indexValue)) {
				indexValues.add(indexValue);
			}
			lastExample = example;

			if (++counter % 100 == 0) {
				getProgress().setCompleted((int) ((float) counter / exampleSet.size() * 30));
			}
		}
		getProgress().setCompleted(30);
		if (!indexAttribute.isNominal()) {
			Collections.sort(indexValues);
		}
		List<String> attributeNames = new Vector<>();
		List<Attribute> newAttributes = new Vector<>();
		Attribute newWeightAttribute = null;
		if (weightAttribute != null && considerWeights) {
			newWeightAttribute = AttributeFactory.createAttribute(weightAttribute.getName(), Ontology.REAL);
			newAttributes.add(newWeightAttribute);
			attributeNames.add(newWeightAttribute.getName());
		}
		for (int i = 0; i < attributes.length; i++) {
			Attribute attribute = attributes[i];
			if (!attribute.equals(indexAttribute)) {
				if (skipConstantAttributes && constantAttributeValues[i] || attribute.equals(groupAttribute)) {
					newAttributes.add(AttributeFactory.createAttribute(attribute.getName(), attribute.getValueType()));
					attributeNames.add(attribute.getName());
				} else {
					for (String indexValue : indexValues) {
						String newAttributeName = attribute.getName() + "_" + indexValue;
						Attribute newAttribute = AttributeFactory.createAttribute(newAttributeName,
								attribute.getValueType());
						newAttribute.setDefault(Double.NaN);
						newAttributes.add(newAttribute);
						attributeNames.add(newAttributeName);
					}
				}
			}
			if (i % 50 == 0) {
				getProgress().setCompleted((int) ((float) i / attributes.length * 10 + 30));
			}
		}
		getProgress().setCompleted(40);

		ExampleSetBuilder builder = ExampleSets.from(newAttributes);

		int datamanagement = getParameterAsInt(PARAMETER_DATAMANAGEMENT);
		if (!Boolean.parseBoolean(ParameterService.getParameterValue(RapidMiner.PROPERTY_RAPIDMINER_SYSTEM_LEGACY_DATA_MGMT))) {
			datamanagement = DataRowFactory.TYPE_DOUBLE_ARRAY;
			builder.withOptimizationHint(DataManagementParameterHelper.getSelectedDataManagement(this));
		}

		AggregationFunction aggregationFunction = null;
		if (newWeightAttribute != null && considerWeights) {
			try {
				aggregationFunction = AbstractAggregationFunction.createAggregationFunction(weightAggregationFunctionIndex);
			} catch (Exception e) {
				throw new UserError(this, 904,
						AbstractAggregationFunction.KNOWN_AGGREGATION_FUNCTION_NAMES[weightAggregationFunctionIndex],
						e.getMessage());
			}
		}
		double lastGroupValue = Double.NaN;

		DataRowFactory dataRowFactory = new DataRowFactory(datamanagement, '.');
		DataRow dataRow = dataRowFactory.create(newAttributes.size());
		if (exampleSet.size() > 0) {
			// if the original example set has a size = 0 we would create a size = 1 new example set
			// without the size check here
			for (Attribute newAttribute : newAttributes) {
				dataRow.set(newAttribute, Double.NaN);
			}
		}

		counter = 0;
		for (Example example : exampleSet) {
			double currentGroupValue = example.getValue(groupAttribute);
			if (!Double.isNaN(lastGroupValue) && lastGroupValue != currentGroupValue) {
				if (aggregationFunction != null) {
					dataRow.set(newWeightAttribute, aggregationFunction.getValue());
					try {
						aggregationFunction = AbstractAggregationFunction
								.createAggregationFunction(weightAggregationFunctionIndex);
					} catch (Exception e) {
						throw new UserError(this, 904,
								AbstractAggregationFunction.KNOWN_AGGREGATION_FUNCTION_NAMES[weightAggregationFunctionIndex],
								e.getMessage());
					}
				}
				builder.addDataRow(dataRow);
				dataRow = dataRowFactory.create(newAttributes.size());
				for (Attribute newAttribute : newAttributes) {
					dataRow.set(newAttribute, Double.NaN);
				}
			}
			if (aggregationFunction != null) {
				aggregationFunction.update(example.getWeight());
			}
			for (int i = 0; i < attributes.length; i++) {
				Attribute attribute = attributes[i];
				int newIndex = -1;
				if (skipConstantAttributes && constantAttributeValues[i] || attribute.equals(groupAttribute)) {
					newIndex = attributeNames.indexOf(attribute.getName());
				} else {
					String newAttributeName = attribute.getName() + "_" + example.getValueAsString(indexAttribute);
					newIndex = attributeNames.indexOf(newAttributeName);
				}
				if (newIndex != -1) {
					Attribute newAttribute = newAttributes.get(newIndex);
					double value = example.getValue(attribute);
					if (Double.isNaN(value)) {
						dataRow.set(newAttribute, Double.NaN);
					} else {
						if (attribute.isNominal()) {
							dataRow.set(newAttribute,
									newAttribute.getMapping().mapString(attribute.getMapping().mapIndex((int) value)));
						} else {
							dataRow.set(newAttribute, value);
						}
					}
				}
			}
			lastGroupValue = currentGroupValue;

			// report progress every 100 examples
			if (++counter % 100 == 0) {
				getProgress().setCompleted((int) ((float) counter / exampleSet.size() * 60 + 40));
			}
		}
		if (aggregationFunction != null) {
			dataRow.set(newWeightAttribute, aggregationFunction.getValue());
		}

		if (exampleSet.size() > 0) {
			// if the original example set has size = 0 we would create a size = 1 new example set
			// without the size check here
			builder.addDataRow(dataRow);
		}

		// create and deliver example set
		ExampleSet result = builder.build();
		if (newWeightAttribute != null) {
			result.getAttributes().setWeight(newWeightAttribute);
		}
		result.recalculateAllAttributeStatistics();
		result.getAnnotations().addAll(exampleSet.getAnnotations());

		getProgress().complete();
		return result;
	}

	@Override
	public List<ParameterType> getParameterTypes() {
		List<ParameterType> types = super.getParameterTypes();
		types.add(new ParameterTypeAttribute(PARAMETER_GROUP_ATTRIBUTE,
				"Attribute that groups the examples which form one example after pivoting.", getExampleSetInputPort(),
				false));
		types.add(new ParameterTypeAttribute(PARAMETER_INDEX_ATTRIBUTE,
				"Attribute which differentiates examples inside a group.", getExampleSetInputPort(), false));
		types.add(new ParameterTypeBoolean(PARAMETER_CONSIDER_WEIGHTS,
				"Determines whether weights will be kept and aggregated or ignored.", true, false));
		ParameterType type = new ParameterTypeCategory(PARAMETER_WEIGHT_AGGREGATION,
				"Specifies how example weights are aggregated in the groups.",
				AbstractAggregationFunction.KNOWN_AGGREGATION_FUNCTION_NAMES, AbstractAggregationFunction.SUM, false);
		type.registerDependencyCondition(new BooleanParameterCondition(this, PARAMETER_CONSIDER_WEIGHTS, true, true));
		types.add(type);

		types.add(new ParameterTypeBoolean(PARAMETER_SKIP_CONSTANT_ATTRIBUTES,
				"Skips attributes if their value never changes within a group.", true));
		DataManagementParameterHelper.addParameterTypes(types, this);
		return types;
	}

	@Override
	public boolean writesIntoExistingData() {
		return false;
	}

	@Override
	public ResourceConsumptionEstimator getResourceConsumptionEstimator() {
		return OperatorResourceConsumptionHandler.getResourceConsumptionEstimator(getInputPort(),
				Example2AttributePivoting.class, null);
	}
}
