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
package com.rapidminer.operator.learner.bayes;

import java.util.List;

import com.rapidminer.example.ExampleSet;
import com.rapidminer.operator.Model;
import com.rapidminer.operator.OperatorCapability;
import com.rapidminer.operator.OperatorDescription;
import com.rapidminer.operator.OperatorException;
import com.rapidminer.operator.annotation.ResourceConsumptionEstimator;
import com.rapidminer.operator.learner.AbstractLearner;
import com.rapidminer.operator.learner.PredictionModel;
import com.rapidminer.parameter.ParameterType;
import com.rapidminer.parameter.ParameterTypeBoolean;
import com.rapidminer.tools.OperatorResourceConsumptionHandler;


/**
 * Naive Bayes learner.
 *
 * @author Tobias Malbrecht
 */
public class NaiveBayes extends AbstractLearner {

	public static final String PARAMETER_LAPLACE_CORRECTION = "laplace_correction";

	public NaiveBayes(OperatorDescription description) {
		super(description);
	}

	@Override
	public Model learn(ExampleSet exampleSet) throws OperatorException {
		return new SimpleDistributionModel(exampleSet, getParameterAsBoolean(PARAMETER_LAPLACE_CORRECTION), getProgress());
	}

	@Override
	public Class<? extends PredictionModel> getModelClass() {
		return DistributionModel.class;
	}

	@Override
	public boolean supportsCapability(OperatorCapability lc) {
		switch (lc) {
			case POLYNOMINAL_ATTRIBUTES:
			case BINOMINAL_ATTRIBUTES:
			case NUMERICAL_ATTRIBUTES:
			case POLYNOMINAL_LABEL:
			case BINOMINAL_LABEL:
			case WEIGHTED_EXAMPLES:
			case UPDATABLE:
			case MISSING_VALUES:
				return true;
			default:
				return false;
		}
	}

	@Override
	public List<ParameterType> getParameterTypes() {
		List<ParameterType> types = super.getParameterTypes();
		ParameterType type = new ParameterTypeBoolean(PARAMETER_LAPLACE_CORRECTION,
				"Use Laplace correction to prevent high influence of zero probabilities.", true, false);
		type.setExpert(true);
		types.add(type);
		return types;
	}

	@Override
	public ResourceConsumptionEstimator getResourceConsumptionEstimator() {
		return OperatorResourceConsumptionHandler.getResourceConsumptionEstimator(getExampleSetInputPort(), NaiveBayes.class,
				null);
	}
}
