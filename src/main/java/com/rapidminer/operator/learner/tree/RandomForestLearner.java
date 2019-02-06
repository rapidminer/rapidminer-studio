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
package com.rapidminer.operator.learner.tree;

import java.util.LinkedList;
import java.util.List;

import com.rapidminer.example.ExampleSet;
import com.rapidminer.operator.Model;
import com.rapidminer.operator.OperatorCapability;
import com.rapidminer.operator.OperatorCreationException;
import com.rapidminer.operator.OperatorDescription;
import com.rapidminer.operator.OperatorException;
import com.rapidminer.operator.learner.PredictionModel;
import com.rapidminer.operator.learner.tree.ConfigurableRandomForestModel.VotingStrategy;
import com.rapidminer.operator.preprocessing.sampling.BootstrappingOperator;
import com.rapidminer.parameter.ParameterType;
import com.rapidminer.parameter.ParameterTypeInt;
import com.rapidminer.tools.OperatorService;
import com.rapidminer.tools.RandomGenerator;


/**
 * This operators learns a random forest. The resulting forest model contains several single random
 * tree models.
 *
 * @author Ingo Mierswa, Sebastian Land
 *
 * @deprecated use {@link ParallelRandomForestLearner} instead
 */
@Deprecated
public class RandomForestLearner extends RandomTreeLearner {

	/** The parameter name for the number of trees. */
	public static final String PARAMETER_NUMBER_OF_TREES = "number_of_trees";

	public RandomForestLearner(OperatorDescription description) {
		super(description);
	}

	@Override
	public Class<? extends PredictionModel> getModelClass() {
		return ConfigurableRandomForestModel.class;
	}

	@Override
	public Model learn(ExampleSet exampleSet) throws OperatorException {
		BootstrappingOperator bootstrapping = null;
		try {
			bootstrapping = OperatorService.createOperator(BootstrappingOperator.class);
			bootstrapping.setParameter(BootstrappingOperator.PARAMETER_USE_WEIGHTS, "false");
			bootstrapping.setParameter(BootstrappingOperator.PARAMETER_SAMPLE_RATIO, "1.0");
			if (getParameterAsBoolean(RandomGenerator.PARAMETER_USE_LOCAL_RANDOM_SEED)) {
				bootstrapping.setParameter(RandomGenerator.PARAMETER_USE_LOCAL_RANDOM_SEED, Boolean.toString(true));
				bootstrapping.setParameter(RandomGenerator.PARAMETER_LOCAL_RANDOM_SEED,
						getParameter(RandomGenerator.PARAMETER_LOCAL_RANDOM_SEED));
			}
		} catch (OperatorCreationException e) {
			throw new OperatorException(getName() + ": cannot construct random tree learner: " + e.getMessage());
		}

		// learn base models
		List<TreeModel> baseModels = new LinkedList<TreeModel>();
		int numberOfTrees = getParameterAsInt(PARAMETER_NUMBER_OF_TREES);

		for (int i = 0; i < numberOfTrees; i++) {
			TreeModel model = (TreeModel) super.learn(bootstrapping.apply(exampleSet));
			model.setSource(getName());
			baseModels.add(model);
		}

		// create and return model
		return new ConfigurableRandomForestModel(exampleSet, baseModels, VotingStrategy.MAJORITY_VOTE);
	}

	@Override
	public boolean supportsCapability(OperatorCapability capability) {
		if (capability == com.rapidminer.operator.OperatorCapability.BINOMINAL_ATTRIBUTES) {
			return true;
		}
		if (capability == com.rapidminer.operator.OperatorCapability.POLYNOMINAL_ATTRIBUTES) {
			return true;
		}
		if (capability == com.rapidminer.operator.OperatorCapability.NUMERICAL_ATTRIBUTES) {
			return true;
		}
		if (capability == com.rapidminer.operator.OperatorCapability.POLYNOMINAL_LABEL) {
			return true;
		}
		if (capability == com.rapidminer.operator.OperatorCapability.BINOMINAL_LABEL) {
			return true;
		}
		if (capability == com.rapidminer.operator.OperatorCapability.WEIGHTED_EXAMPLES) {
			return false;
		}
		return false;
	}

	@Override
	public List<ParameterType> getParameterTypes() {

		List<ParameterType> types = new LinkedList<ParameterType>();

		ParameterType type = new ParameterTypeInt(PARAMETER_NUMBER_OF_TREES, "The number of learned random trees.", 1,
				Integer.MAX_VALUE, 10);
		type.setExpert(false);
		types.add(type);

		types.addAll(super.getParameterTypes());

		return types;
	}
}
