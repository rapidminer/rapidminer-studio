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
package com.rapidminer.operator.features.selection;

import com.rapidminer.example.Attribute;
import com.rapidminer.example.AttributeWeights;
import com.rapidminer.example.ExampleSet;
import com.rapidminer.operator.OperatorDescription;
import com.rapidminer.operator.OperatorException;
import com.rapidminer.operator.ports.InputPort;
import com.rapidminer.operator.ports.OutputPort;
import com.rapidminer.operator.ports.metadata.ExampleSetMetaData;
import com.rapidminer.operator.ports.metadata.MetaData;
import com.rapidminer.operator.preprocessing.AbstractDataProcessing;
import com.rapidminer.parameter.ParameterType;
import com.rapidminer.parameter.ParameterTypeBoolean;
import com.rapidminer.parameter.ParameterTypeCategory;
import com.rapidminer.parameter.ParameterTypeDouble;
import com.rapidminer.parameter.ParameterTypeInt;
import com.rapidminer.parameter.conditions.EqualTypeCondition;

import java.util.Iterator;
import java.util.List;
import java.util.Vector;


/**
 * This operator selects all attributes which have a weight satisfying a given condition. For
 * example, only attributes with a weight greater than <code>min_weight</code> should be selected.
 * This operator is also able to select the k attributes with the highest weight.
 * 
 * @author Ingo Mierswa, Stefan Rueping
 */
public class AttributeWeightSelection extends AbstractDataProcessing {

	/** The parameter name for &quot;Use this weight for the selection relation.&quot; */
	public static final String PARAMETER_WEIGHT = "weight";

	/** The parameter name for &quot;Selects only weights which fulfill this relation.&quot; */
	public static final String PARAMETER_WEIGHT_RELATION = "weight_relation";

	/**
	 * The parameter name for &quot;Number k of attributes to be selected for weight-relations 'top
	 * k' or 'bottom k'.&quot;
	 */
	public static final String PARAMETER_K = "k";

	/**
	 * The parameter name for &quot;Percentage of attributes to be selected for weight-relations
	 * 'top p%' or 'bottom p%'.&quot;
	 */
	public static final String PARAMETER_P = "p";

	/**
	 * The parameter name for &quot;Indicates if attributes which weight is unknown should be
	 * deselected.&quot;
	 */
	public static final String PARAMETER_DESELECT_UNKNOWN = "deselect_unknown";

	/**
	 * The parameter name for &quot;Indicates if the absolute values of the weights should be used
	 * for comparison.&quot;
	 */
	public static final String PARAMETER_USE_ABSOLUTE_WEIGHTS = "use_absolute_weights";

	private static final String[] WEIGHT_RELATIONS = { "greater", "greater equals", "equals", "less equals", "less",
			"top k", "bottom k", "all but top k", "all but bottom k", "top p%", "bottom p%" };

	private static final int GREATER = 0;

	private static final int GREATER_EQUALS = 1;

	private static final int EQUALS = 2;

	private static final int LESS_EQUALS = 3;

	private static final int LESS = 4;

	private static final int TOPK = 5;

	private static final int BOTTOMK = 6;

	private static final int ALLBUTTOPK = 7;

	private static final int ALLBUTBOTTOMK = 8;

	private static final int TOPPPERCENT = 9;

	private static final int BOTTOMPPERCENT = 10;

	private InputPort weightsInput = getInputPorts().createPort("weights", AttributeWeights.class);
	private OutputPort weightsOutput = getOutputPorts().createPort("weights");

	public AttributeWeightSelection(OperatorDescription description) {
		super(description);
		getTransformer().addPassThroughRule(weightsInput, weightsOutput);
	}

	@Override
	protected MetaData modifyMetaData(ExampleSetMetaData metaData) {
		metaData.attributesAreSubset();
		return metaData;
	}

	@Override
	public ExampleSet apply(ExampleSet exampleSet) throws OperatorException {
		AttributeWeights weights = weightsInput.getData(AttributeWeights.class);
		boolean deselectUnknown = getParameterAsBoolean(PARAMETER_DESELECT_UNKNOWN);
		double relationWeight = getParameterAsDouble(PARAMETER_WEIGHT);
		int relation = getParameterAsInt(PARAMETER_WEIGHT_RELATION);
		boolean useAbsoluteWeights = getParameterAsBoolean(PARAMETER_USE_ABSOLUTE_WEIGHTS);

		// determine which attributes have a known weight value
		boolean[] weightKnown = new boolean[exampleSet.getAttributes().size()];
		Vector<Attribute> knownAttributes = new Vector<Attribute>();
		int index = 0;
		for (Attribute attribute : exampleSet.getAttributes()) {
			double weight = weights.getWeight(attribute.getName());
			if (!Double.isNaN(weight)) {
				knownAttributes.add(attribute);
				weightKnown[index++] = true;
			} else {
				weightKnown[index++] = false;
			}
		}

		// determine number of attributes that should be selected
		int nrAtts = knownAttributes.size();
		int k = getParameterAsInt(PARAMETER_K);

		if (relation == ALLBUTTOPK) {
			relation = BOTTOMK;
			k = nrAtts - k;
		}
		if (relation == ALLBUTBOTTOMK) {
			relation = TOPK;
			k = nrAtts - k;
		}
		if (relation == TOPPPERCENT) {
			relation = TOPK;
			k = (int) Math.round(nrAtts * getParameterAsDouble(PARAMETER_P));
		}
		if (relation == BOTTOMPPERCENT) {
			relation = BOTTOMK;
			k = (int) Math.round(nrAtts * getParameterAsDouble(PARAMETER_P));
		}

		if (k < 1) {
			k = 1;
		}

		if (k > nrAtts) {
			k = nrAtts;
		}

		// top k or bottom k
		if ((relation == TOPK) || (relation == BOTTOMK)) {
			int direction = AttributeWeights.DECREASING;
			if (relation == BOTTOMK) {
				direction = AttributeWeights.INCREASING;
			}
			int comparatorType = AttributeWeights.ORIGINAL_WEIGHTS;
			if (useAbsoluteWeights) {
				comparatorType = AttributeWeights.ABSOLUTE_WEIGHTS;
			}

			String[] attributeNames = new String[knownAttributes.size()];
			index = 0;
			for (Attribute attribute : knownAttributes) {
				attributeNames[index++] = attribute.getName();
			}
			weights.sortByWeight(attributeNames, direction, comparatorType);

			Iterator<Attribute> iterator = exampleSet.getAttributes().iterator();
			index = 0;
			while (iterator.hasNext()) {
				Attribute attribute = iterator.next();
				if (!weightKnown[index]) {
					if (deselectUnknown) {
						iterator.remove();
					}
				} else {
					boolean remove = true;
					for (int i = 0; i < k; i++) {
						if (attribute.getName().equals(attributeNames[i])) {
							remove = false;
							break;
						}
					}
					if (remove) {
						iterator.remove();
					}
				}
				index++;
			}
		} else { // simple relations
			Iterator<Attribute> iterator = exampleSet.getAttributes().iterator();
			while (iterator.hasNext()) {
				Attribute attribute = iterator.next();
				double weight = weights.getWeight(attribute.getName());
				if (useAbsoluteWeights) {
					weight = Math.abs(weight);
				}
				if (Double.isNaN(weight) && (deselectUnknown)) {
					iterator.remove();
				} else {
					switch (relation) {
						case GREATER:
							if (weight <= relationWeight) {
								iterator.remove();
							}
							break;
						case GREATER_EQUALS:
							if (weight < relationWeight) {
								iterator.remove();
							}
							break;
						case EQUALS:
							if (weight != relationWeight) {
								iterator.remove();
							}
							break;
						case LESS_EQUALS:
							if (weight > relationWeight) {
								iterator.remove();
							}
							break;
						case LESS:
							if (weight >= relationWeight) {
								iterator.remove();
							}
							break;
					}
				}
			}
		}

		weightsOutput.deliver(weights);
		return exampleSet;
	}

	@Override
	public boolean shouldAutoConnect(OutputPort port) {
		if (port == weightsOutput) {
			return getParameterAsBoolean("keep_attribute_weights");
		} else {
			return super.shouldAutoConnect(port);
		}
	}

	@Override
	public List<ParameterType> getParameterTypes() {
		List<ParameterType> types = super.getParameterTypes();
		ParameterType type = new ParameterTypeCategory(PARAMETER_WEIGHT_RELATION,
				"Selects only weights which fulfill this relation.", WEIGHT_RELATIONS, GREATER_EQUALS);
		type.setExpert(false);
		types.add(type);

		type = new ParameterTypeDouble(PARAMETER_WEIGHT, "The selected relation will be evaluated against this value.",
				Double.NEGATIVE_INFINITY, Double.POSITIVE_INFINITY, 1.0d);
		type.registerDependencyCondition(new EqualTypeCondition(this, PARAMETER_WEIGHT_RELATION, WEIGHT_RELATIONS, true,
				GREATER, GREATER_EQUALS, LESS, LESS_EQUALS, EQUALS));
		type.setExpert(false);
		types.add(type);
		type = new ParameterTypeInt(
				PARAMETER_K,
				"Number k of attributes to be selected. For example 'top k' with k = 5 will return an exampleset containing only the 5 highest weighted attributes.",
				1, Integer.MAX_VALUE, 10);
		type.registerDependencyCondition(new EqualTypeCondition(this, PARAMETER_WEIGHT_RELATION, WEIGHT_RELATIONS, true,
				TOPK, BOTTOMK, ALLBUTBOTTOMK, ALLBUTTOPK));
		type.setExpert(false);
		types.add(type);
		type = new ParameterTypeDouble(
				PARAMETER_P,
				"Percentage of attributes to be selected. For example 'top p%' with p = 15 will return an exampleset containing only attributes which are part of the 15% of the highest weighted attributes.",
				0.0d, 1.0d, 0.5d);
		type.registerDependencyCondition(new EqualTypeCondition(this, PARAMETER_WEIGHT_RELATION, WEIGHT_RELATIONS, true,
				TOPPPERCENT, BOTTOMPPERCENT));
		type.setExpert(false);
		types.add(type);
		types.add(new ParameterTypeBoolean(PARAMETER_DESELECT_UNKNOWN,
				"Indicates if attributes which weight is unknown should be removed from example set.", true));
		types.add(new ParameterTypeBoolean(PARAMETER_USE_ABSOLUTE_WEIGHTS,
				"Indicates if the absolute values of the weights should be used for comparison.", true));
		return types;
	}

	@Override
	public boolean writesIntoExistingData() {
		return false;
	}
}
