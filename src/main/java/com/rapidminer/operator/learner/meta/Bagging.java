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
package com.rapidminer.operator.learner.meta;

import java.util.List;
import java.util.Vector;

import com.rapidminer.example.ExampleSet;
import com.rapidminer.example.set.SplittedExampleSet;
import com.rapidminer.operator.Model;
import com.rapidminer.operator.OperatorCapability;
import com.rapidminer.operator.OperatorDescription;
import com.rapidminer.operator.OperatorException;
import com.rapidminer.operator.ValueDouble;
import com.rapidminer.parameter.ParameterType;
import com.rapidminer.parameter.ParameterTypeBoolean;
import com.rapidminer.parameter.ParameterTypeDouble;
import com.rapidminer.parameter.ParameterTypeInt;
import com.rapidminer.tools.RandomGenerator;


/**
 * This Bagging implementation can be used with all learners available in RapidMiner, not only the
 * ones which originally are part of the Weka package.
 *
 * @author Martin Scholz, Ingo Mierswa
 */
public class Bagging extends AbstractMetaLearner {

	/**
	 * Name of the variable specifying the maximal number of iterations of the learner.
	 */
	public static final String PARAMETER_ITERATIONS = "iterations";

	/** Name of the flag indicating internal bootstrapping. */
	public static final String PARAMETER_SAMPLE_RATIO = "sample_ratio";

	/** Name of the flag indicating internal bootstrapping. */
	public static final String PARAMETER_AVERAGE_CONFIDENCES = "average_confidences";

	// field for visualizing performance
	protected int currentIteration;

	/** Constructor. */
	public Bagging(OperatorDescription description) {
		super(description);
		addValue(new ValueDouble("iteration", "The current iteration.") {

			@Override
			public double getDoubleValue() {
				return currentIteration;
			}
		});
	}

	/**
	 * Constructs a {@link Model} by repeatedly running a base learner on subsamples.
	 */
	@Override
	public Model learn(ExampleSet exampleSet) throws OperatorException {
		final double splitRatio = this.getParameterAsDouble(PARAMETER_SAMPLE_RATIO);
		final int numInterations = this.getParameterAsInt(PARAMETER_ITERATIONS);
		boolean useLocalRandomSeed = getParameterAsBoolean(RandomGenerator.PARAMETER_USE_LOCAL_RANDOM_SEED);
		int localRandomSeed = getParameterAsInt(RandomGenerator.PARAMETER_LOCAL_RANDOM_SEED);

		Vector<Model> modelList = new Vector<Model>();
		for (this.currentIteration = 0; this.currentIteration < numInterations; this.currentIteration++) {
			SplittedExampleSet splitted = new SplittedExampleSet(exampleSet, splitRatio,
					SplittedExampleSet.SHUFFLED_SAMPLING, useLocalRandomSeed, localRandomSeed);
			splitted.selectSingleSubset(0);
			modelList.add(applyInnerLearner(splitted));
			inApplyLoop();
		}

		boolean numerical = exampleSet.getAttributes().getLabel().isNumerical();
		if (this.getParameterAsBoolean(PARAMETER_AVERAGE_CONFIDENCES) || numerical) {
			return new BaggingModel(exampleSet, modelList);
		} else {
			List<Double> weights = new Vector<Double>();
			for (int i = 0; i < modelList.size(); i++) {
				weights.add(1.0d);
			}
			return new AdaBoostModel(exampleSet, modelList, weights);
		}
	}

	@Override
	public List<ParameterType> getParameterTypes() {
		List<ParameterType> types = super.getParameterTypes();
		ParameterType type = new ParameterTypeDouble(PARAMETER_SAMPLE_RATIO,
				"Fraction of examples used for training. Must be greater than 0 and should be lower than 1.", 0, 1, 0.9);
		type.setExpert(false);
		types.add(type);

		type = new ParameterTypeInt(PARAMETER_ITERATIONS, "The number of iterations (base models).", 1, Integer.MAX_VALUE,
				10);
		type.setExpert(false);
		types.add(type);
		types.add(new ParameterTypeBoolean(PARAMETER_AVERAGE_CONFIDENCES,
				"Specifies whether to average available prediction confidences or not.", true));

		types.addAll(RandomGenerator.getRandomGeneratorParameters(this));

		return types;
	}

	@Override
	public boolean supportsCapability(OperatorCapability capability) {
		switch (capability) {
			case NO_LABEL:
			case UPDATABLE:
			case FORMULA_PROVIDER:
				return false;
			default:
				return true;
		}
	}
}
