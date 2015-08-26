/**
 * Copyright (C) 2001-2015 by RapidMiner and the contributors
 *
 * Complete list of developers available at our web site:
 *
 *      http://rapidminer.com
 *
 * This program is free software: you can redistribute it and/or modify
 * it under the terms of the GNU Affero General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU Affero General Public License for more details.
 *
 * You should have received a copy of the GNU Affero General Public License
 * along with this program.  If not, see http://www.gnu.org/licenses/.
 */
package com.rapidminer.operator.features.construction;

import com.rapidminer.example.Attribute;
import com.rapidminer.example.Example;
import com.rapidminer.example.ExampleSet;
import com.rapidminer.example.table.AttributeFactory;
import com.rapidminer.operator.OperatorDescription;
import com.rapidminer.operator.OperatorException;
import com.rapidminer.operator.ProcessSetupError.Severity;
import com.rapidminer.operator.UserError;
import com.rapidminer.operator.ports.metadata.AttributeMetaData;
import com.rapidminer.operator.ports.metadata.ExampleSetMetaData;
import com.rapidminer.operator.ports.metadata.MDInteger;
import com.rapidminer.operator.ports.metadata.MetaData;
import com.rapidminer.operator.ports.metadata.SimpleMetaDataError;
import com.rapidminer.operator.tools.AttributeSubsetSelector;
import com.rapidminer.parameter.ParameterType;
import com.rapidminer.parameter.ParameterTypeBoolean;
import com.rapidminer.parameter.ParameterTypeCategory;
import com.rapidminer.parameter.ParameterTypeString;
import com.rapidminer.parameter.UndefinedParameterError;
import com.rapidminer.tools.Ontology;
import com.rapidminer.tools.math.function.aggregation.AbstractAggregationFunction;
import com.rapidminer.tools.math.function.aggregation.AggregationFunction;

import java.lang.reflect.InvocationTargetException;
import java.util.List;
import java.util.Set;


/**
 * Allows to generate a new attribute which consists of a function of several other attributes. As
 * functions, several aggregation attributes are available.
 * 
 * @author Tobias Malbrecht
 */
public class AttributeAggregationOperator extends AbstractFeatureConstruction {

	public static final String PARAMETER_ATTRIBUTE_NAME = "attribute_name";

	public static final String PARAMETER_AGGREGATION_FUNCTION = "aggregation_function";

	public static final String PARAMETER_IGNORE_MISSINGS = "ignore_missings";

	/** The parameter name for &quot;Indicates if the all old attributes should be kept.&quot; */
	public static final String PARAMETER_KEEP_ALL = "keep_all";

	public AttributeAggregationOperator(OperatorDescription description) {
		super(description);
	}

	@Override
	protected MetaData modifyMetaData(ExampleSetMetaData metaData) {
		try {
			AttributeMetaData newAMD = new AttributeMetaData(getParameterAsString(PARAMETER_ATTRIBUTE_NAME), Ontology.REAL);
			AttributeSubsetSelector selector = new AttributeSubsetSelector(this, getExampleSetInputPort());
			String functionName = AbstractAggregationFunction.KNOWN_AGGREGATION_FUNCTION_NAMES[getParameterAsInt(PARAMETER_AGGREGATION_FUNCTION)];
			boolean ignoreMissings = getParameterAsBoolean(PARAMETER_IGNORE_MISSINGS);
			AggregationFunction aggregationFunction = null;
			try {
				aggregationFunction = AbstractAggregationFunction.createAggregationFunction(functionName, ignoreMissings);
				int numberOfMissings = 0;
				for (AttributeMetaData amd : selector.getMetaDataSubset(metaData, false).getAllAttributes()) {
					if (!aggregationFunction.supportsAttribute(amd)) {
						getExampleSetInputPort().addError(
								new SimpleMetaDataError(Severity.ERROR, getExampleSetInputPort(),
										"exampleset.parameters.attribute_must_be_numerical", amd.getName(),
										PARAMETER_AGGREGATION_FUNCTION, functionName));
					}
					if (amd.getNumberOfMissingValues().isKnown()) {
						numberOfMissings = Math.max(numberOfMissings, amd.getNumberOfMissingValues().getValue());
					}
				}
				newAMD.setNumberOfMissingValues(new MDInteger(numberOfMissings));
			} catch (InstantiationException e) {
			} catch (IllegalAccessException e) {
			} catch (ClassNotFoundException e) {
			} catch (NoSuchMethodException e) {
			} catch (InvocationTargetException e) {
			}

			metaData.addAttribute(newAMD);
		} catch (UndefinedParameterError e) {
		}
		return metaData;
	}

	@Override
	public ExampleSet apply(ExampleSet exampleSet) throws OperatorException {
		AttributeSubsetSelector selector = new AttributeSubsetSelector(this, getExampleSetInputPort());
		Set<Attribute> attributes = selector.getAttributeSubset(exampleSet, false);
		// cannot do anything with no attributes
		if (attributes.size() > 0) {
			String functionName = AbstractAggregationFunction.KNOWN_AGGREGATION_FUNCTION_NAMES[getParameterAsInt(PARAMETER_AGGREGATION_FUNCTION)];
			boolean ignoreMissings = getParameterAsBoolean(PARAMETER_IGNORE_MISSINGS);
			AggregationFunction aggregationFunction = null;
			try {
				aggregationFunction = AbstractAggregationFunction.createAggregationFunction(functionName, ignoreMissings);
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

			int valueType = Ontology.ATTRIBUTE_VALUE;
			for (Attribute attribute : attributes) {
				if (valueType == Ontology.ATTRIBUTE_VALUE) {
					if (attribute.isNominal() || attribute.isNumerical()) {
						valueType = Ontology.NUMERICAL;
					} else {
						valueType = attribute.getValueType();
					}
				}
				if (!aggregationFunction.supportsAttribute(attribute)) {
					throw new UserError(this, 136, attribute.getName());
				}
			}

			// create aggregation attribute
			// Attribute newAttribute =
			// AttributeFactory.createAttribute(getParameterAsString(PARAMETER_ATTRIBUTE_NAME),
			// Ontology.REAL);
			Attribute newAttribute = AttributeFactory.createAttribute(getParameterAsString(PARAMETER_ATTRIBUTE_NAME),
					valueType);

			exampleSet.getExampleTable().addAttribute(newAttribute);
			exampleSet.getAttributes().addRegular(newAttribute);

			// iterate over examples and aggregate values
			double[] values = new double[attributes.size()];
			for (Example example : exampleSet) {
				int i = 0;
				for (Attribute attribute : attributes) {
					values[i] = example.getValue(attribute);
					i++;
				}
				example.setValue(newAttribute, aggregationFunction.calculate(values));
			}

			// remove old attributes
			if (!getParameterAsBoolean(PARAMETER_KEEP_ALL)) {
				for (Attribute attribute : attributes) {
					exampleSet.getAttributes().remove(attribute);
				}
			}
		}

		return exampleSet;
	}

	@Override
	public List<ParameterType> getParameterTypes() {
		List<ParameterType> types = super.getParameterTypes();
		types.add(new ParameterTypeString(PARAMETER_ATTRIBUTE_NAME, "Name of the resulting attributes.", false));
		types.addAll(new AttributeSubsetSelector(this, getExampleSetInputPort()).getParameterTypes());
		ParameterType type = new ParameterTypeCategory(PARAMETER_AGGREGATION_FUNCTION,
				"Function for aggregating the attribute values.",
				AbstractAggregationFunction.KNOWN_AGGREGATION_FUNCTION_NAMES, AbstractAggregationFunction.SUM);
		type.setExpert(false);
		types.add(type);
		type = new ParameterTypeBoolean(PARAMETER_KEEP_ALL, "Indicates if the all old attributes should be kept.", true);
		type.setExpert(false);
		types.add(type);
		types.add(new ParameterTypeBoolean(
				PARAMETER_IGNORE_MISSINGS,
				"Indicates if missings should be ignored and aggregation should be based only on existing values or not. In the latter case the aggregated value will be missing in the presence of missing values.",
				true));
		return types;
	}
}
