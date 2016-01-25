/**
 * Copyright (C) 2001-2016 by RapidMiner and the contributors
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
package com.rapidminer.operator.preprocessing.weighting;

import com.rapidminer.example.Attribute;
import com.rapidminer.example.Attributes;
import com.rapidminer.example.Example;
import com.rapidminer.example.ExampleSet;
import com.rapidminer.example.Statistics;
import com.rapidminer.example.table.AttributeFactory;
import com.rapidminer.example.table.NominalMapping;
import com.rapidminer.operator.OperatorDescription;
import com.rapidminer.operator.OperatorException;
import com.rapidminer.operator.annotation.ResourceConsumptionEstimator;
import com.rapidminer.operator.ports.metadata.SetRelation;
import com.rapidminer.parameter.ParameterType;
import com.rapidminer.parameter.ParameterTypeDouble;
import com.rapidminer.parameter.UndefinedParameterError;
import com.rapidminer.tools.Ontology;
import com.rapidminer.tools.OperatorResourceConsumptionHandler;
import com.rapidminer.tools.math.container.Range;

import java.util.HashMap;
import java.util.List;
import java.util.Map;


/**
 * This operator distributes example weights so that all example weights of labels sum up equally.
 * 
 * @author Sebastian Land
 */
public class EqualLabelWeighting extends AbstractExampleWeighting {

	private static final String PARAMETER_TOTAL_WEIGHT = "total_weight";

	public EqualLabelWeighting(OperatorDescription description) {
		super(description);
	}

	@Override
	protected Range getWeightAttributeRange() {
		try {
			return new Range(0, getParameterAsDouble(PARAMETER_TOTAL_WEIGHT));
		} catch (UndefinedParameterError e) {
			return new Range(0, Double.POSITIVE_INFINITY);
		}
	}

	@Override
	protected SetRelation getWeightAttributeValueRelation() {
		return SetRelation.SUPERSET;
	}

	@Override
	public ExampleSet apply(ExampleSet exampleSet) throws OperatorException {
		if (exampleSet.getAttributes().getWeight() == null) {
			Attribute weight = AttributeFactory.createAttribute(Attributes.WEIGHT_NAME, Ontology.NUMERICAL);
			exampleSet.getExampleTable().addAttribute(weight);
			exampleSet.getAttributes().addRegular(weight);
			exampleSet.getAttributes().setWeight(weight);

			Attribute label = exampleSet.getAttributes().getLabel();
			exampleSet.recalculateAttributeStatistics(label);
			NominalMapping labelMapping = label.getMapping();
			Map<String, Double> labelFrequencies = new HashMap<String, Double>();
			for (String labelName : labelMapping.getValues()) {
				labelFrequencies.put(labelName, exampleSet.getStatistics(label, Statistics.COUNT, labelName));
			}
			double numberOfLabels = labelFrequencies.size();
			double perLabelWeight = getParameterAsDouble(PARAMETER_TOTAL_WEIGHT) / numberOfLabels;
			for (Example example : exampleSet) {
				double exampleWeight = perLabelWeight
						/ labelFrequencies.get(labelMapping.mapIndex((int) example.getValue(label)));
				example.setValue(weight, exampleWeight);
			}
		}
		return exampleSet;
	}

	@Override
	public List<ParameterType> getParameterTypes() {
		List<ParameterType> types = super.getParameterTypes();
		ParameterType type = new ParameterTypeDouble(PARAMETER_TOTAL_WEIGHT,
				"The total weight distributed over all examples.", Double.NEGATIVE_INFINITY, Double.POSITIVE_INFINITY, 1);
		type.setExpert(false);
		types.add(type);
		return types;
	}

	@Override
	public boolean writesIntoExistingData() {
		return true;
	}

	@Override
	public ResourceConsumptionEstimator getResourceConsumptionEstimator() {
		return OperatorResourceConsumptionHandler.getResourceConsumptionEstimator(getInputPort(), EqualLabelWeighting.class,
				null);
	}

}
