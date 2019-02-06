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
package com.rapidminer.operator.learner.lazy;

import com.rapidminer.example.Attribute;
import com.rapidminer.example.Attributes;
import com.rapidminer.example.Example;
import com.rapidminer.example.ExampleSet;
import com.rapidminer.operator.Model;
import com.rapidminer.operator.OperatorCapability;
import com.rapidminer.operator.OperatorDescription;
import com.rapidminer.operator.OperatorException;
import com.rapidminer.operator.annotation.ResourceConsumptionEstimator;
import com.rapidminer.operator.learner.AbstractLearner;
import com.rapidminer.operator.learner.PredictionModel;
import com.rapidminer.operator.ports.metadata.DistanceMeasurePrecondition;
import com.rapidminer.parameter.ParameterType;
import com.rapidminer.parameter.ParameterTypeBoolean;
import com.rapidminer.parameter.ParameterTypeInt;
import com.rapidminer.tools.OperatorResourceConsumptionHandler;
import com.rapidminer.tools.math.container.GeometricDataCollection;
import com.rapidminer.tools.math.container.LinearList;
import com.rapidminer.tools.math.similarity.DistanceMeasure;
import com.rapidminer.tools.math.similarity.DistanceMeasureHelper;
import com.rapidminer.tools.math.similarity.DistanceMeasures;

import java.util.List;


/**
 * A k nearest neighbor implementation.
 * 
 * @author Sebastian Land
 * 
 */
public class KNNLearner extends AbstractLearner {

	/** The parameter name for &quot;The used number of nearest neighbors.&quot; */
	public static final String PARAMETER_K = "k";

	/** The parameter name for &quot;Indicates if the votes should be weighted by similarity.&quot; */
	public static final String PARAMETER_WEIGHTED_VOTE = "weighted_vote";

	private DistanceMeasureHelper measureHelper = new DistanceMeasureHelper(this);

	public KNNLearner(OperatorDescription description) {
		super(description);

		getExampleSetInputPort().addPrecondition(new DistanceMeasurePrecondition(getExampleSetInputPort(), this));
	}

	@Override
	public Model learn(ExampleSet exampleSet) throws OperatorException {
		DistanceMeasure measure = measureHelper.getInitializedMeasure(exampleSet);
		Attribute label = exampleSet.getAttributes().getLabel();
		if (label.isNominal()) {
			// classification
			GeometricDataCollection<Integer> samples = new LinearList<Integer>(measure);

			Attributes attributes = exampleSet.getAttributes();

			int valuesSize = attributes.size();
			for (Example example : exampleSet) {
				double[] values = new double[valuesSize];
				int i = 0;
				for (Attribute attribute : attributes) {
					values[i] = example.getValue(attribute);
					i++;
				}
				int labelValue = (int) example.getValue(label);
				samples.add(values, labelValue);
				checkForStop();
			}
			return new KNNClassificationModel(exampleSet, samples, getParameterAsInt(PARAMETER_K),
					getParameterAsBoolean(PARAMETER_WEIGHTED_VOTE));
		} else {
			// regression
			GeometricDataCollection<Double> samples = new LinearList<Double>(measure);
			Attributes attributes = exampleSet.getAttributes();

			int valuesSize = attributes.size();
			for (Example example : exampleSet) {
				double[] values = new double[valuesSize];
				int i = 0;
				for (Attribute attribute : attributes) {
					values[i] = example.getValue(attribute);
					i++;
				}
				double labelValue = example.getValue(label);
				samples.add(values, labelValue);
				checkForStop();
			}
			return new KNNRegressionModel(exampleSet, samples, getParameterAsInt(PARAMETER_K),
					getParameterAsBoolean(PARAMETER_WEIGHTED_VOTE));
		}
	}

	@Override
	public Class<? extends PredictionModel> getModelClass() {
		// TODO: Needs to unify models in order to return common class
		return super.getModelClass();
	}

	@Override
	public boolean supportsCapability(OperatorCapability capability) {
		int measureType = DistanceMeasures.MIXED_MEASURES_TYPE;
		try {
			measureType = measureHelper.getSelectedMeasureType();
		} catch (Exception e) {

		}
		switch (capability) {
			case BINOMINAL_ATTRIBUTES:
			case POLYNOMINAL_ATTRIBUTES:
				return (measureType == DistanceMeasures.MIXED_MEASURES_TYPE)
						|| (measureType == DistanceMeasures.NOMINAL_MEASURES_TYPE);
			case NUMERICAL_ATTRIBUTES:
				return (measureType == DistanceMeasures.MIXED_MEASURES_TYPE)
						|| (measureType == DistanceMeasures.DIVERGENCES_TYPE)
						|| (measureType == DistanceMeasures.NUMERICAL_MEASURES_TYPE);
			case POLYNOMINAL_LABEL:
			case BINOMINAL_LABEL:
			case NUMERICAL_LABEL:
			case MISSING_VALUES:
				return true;
			default:
				return false;
		}
	}

	@Override
	public List<ParameterType> getParameterTypes() {
		List<ParameterType> types = super.getParameterTypes();

		ParameterType type = new ParameterTypeInt(PARAMETER_K, "The used number of nearest neighbors.", 1,
				Integer.MAX_VALUE, 5);
		type.setExpert(false);
		types.add(type);

		types.add(new ParameterTypeBoolean(PARAMETER_WEIGHTED_VOTE,
				"Indicates if the votes should be weighted by similarity.", true, false));

		types.addAll(DistanceMeasures.getParameterTypes(this));
		return types;
	}

	@Override
	public ResourceConsumptionEstimator getResourceConsumptionEstimator() {
		return OperatorResourceConsumptionHandler.getResourceConsumptionEstimator(getExampleSetInputPort(),
				KNNLearner.class, null);
	}
}
