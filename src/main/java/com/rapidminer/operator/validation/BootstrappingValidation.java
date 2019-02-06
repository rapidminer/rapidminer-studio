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
package com.rapidminer.operator.validation;

import java.util.List;

import com.rapidminer.example.ExampleSet;
import com.rapidminer.example.set.MappedExampleSet;
import com.rapidminer.operator.OperatorCapability;
import com.rapidminer.operator.OperatorDescription;
import com.rapidminer.operator.OperatorException;
import com.rapidminer.operator.ValueDouble;
import com.rapidminer.operator.ports.metadata.MDInteger;
import com.rapidminer.operator.visualization.ProcessLogOperator;
import com.rapidminer.parameter.ParameterType;
import com.rapidminer.parameter.ParameterTypeBoolean;
import com.rapidminer.parameter.ParameterTypeDouble;
import com.rapidminer.parameter.ParameterTypeInt;
import com.rapidminer.parameter.UndefinedParameterError;
import com.rapidminer.tools.RandomGenerator;


/**
 * <p>
 * This validation operator performs several bootstrapped samplings (sampling with replacement) on
 * the input set and trains a model on these samples. The remaining samples, i.e. those which were
 * not sampled, build a test set on which the model is evaluated. This process is repeated for the
 * specified number of iterations after which the average performance is calculated.
 * </p>
 *
 * <p>
 * The basic setup is the same as for the usual cross validation operator. The first inner operator
 * must provide a model and the second a performance vector. Please note that this operator does not
 * regard example weights, i.e. weights specified in a weight column.
 * </p>
 *
 * <p>
 * This validation operator provides several values which can be logged by means of a
 * {@link ProcessLogOperator}. All performance estimation operators of RapidMiner provide access to
 * the average values calculated during the estimation. Since the operator cannot ensure the names
 * of the delivered criteria, the ProcessLog operator can access the values via the generic value
 * names:
 * </p>
 * <ul>
 * <li>performance: the value for the main criterion calculated by this validation operator</li>
 * <li>performance1: the value of the first criterion of the performance vector calculated</li>
 * <li>performance2: the value of the second criterion of the performance vector calculated</li>
 * <li>performance3: the value of the third criterion of the performance vector calculated</li>
 * <li>for the main criterion, also the variance and the standard deviation can be accessed where
 * applicable.</li>
 * </ul>
 *
 * @author Ingo Mierswa, Tobias Malbrecht
 */
public class BootstrappingValidation extends ValidationChain {

	public static final String PARAMETER_NUMBER_OF_VALIDATIONS = "number_of_validations";

	public static final String PARAMETER_SAMPLE_RATIO = "sample_ratio";

	public static final String PARAMETER_USE_WEIGHTS = "use_weights";

	public static final String PARAMETER_AVERAGE_PERFORMANCES_ONLY = "average_performances_only";

	private int number;

	private int iteration;

	public BootstrappingValidation(OperatorDescription description) {
		super(description);
		addValue(new ValueDouble("iteration", "The number of the current iteration.") {

			@Override
			public double getDoubleValue() {
				return iteration;
			}
		});
	}

	@Override
	public void estimatePerformance(ExampleSet inputSet) throws OperatorException {
		boolean useWeights = getParameterAsBoolean(PARAMETER_USE_WEIGHTS);
		number = getParameterAsInt(PARAMETER_NUMBER_OF_VALIDATIONS);
		int size = (int) Math.round(inputSet.size() * getParameterAsDouble(PARAMETER_SAMPLE_RATIO));

		// start bootstrapping loop
		RandomGenerator random = RandomGenerator.getRandomGenerator(this);
		if (modelOutput.isConnected()) {
			getProgress().setTotal(number + 1);
		} else {
			getProgress().setTotal(number);
		}
		getProgress().setCheckForStop(false);

		for (iteration = 0; iteration < number; iteration++) {
			int[] mapping = null;
			if (useWeights && inputSet.getAttributes().getWeight() != null) {
				mapping = MappedExampleSet.createWeightedBootstrappingMapping(inputSet, size, random);
			} else {
				mapping = MappedExampleSet.createBootstrappingMapping(inputSet, size, random);
			}
			MappedExampleSet trainingSet = new MappedExampleSet(inputSet, mapping, true);
			learn(trainingSet);

			MappedExampleSet inverseExampleSet = new MappedExampleSet(inputSet, mapping, false);
			evaluate(inverseExampleSet);
			inApplyLoop();
			getProgress().step();
		}
	}

	@Override
	protected MDInteger getTestSetSize(MDInteger originalSize) throws UndefinedParameterError {
		return originalSize.multiply(1d - getParameterAsDouble(PARAMETER_SAMPLE_RATIO));
	}

	@Override
	protected MDInteger getTrainingSetSize(MDInteger originalSize) throws UndefinedParameterError {
		return originalSize.multiply(getParameterAsDouble(PARAMETER_SAMPLE_RATIO));
	}

	@Override
	public List<ParameterType> getParameterTypes() {
		List<ParameterType> types = super.getParameterTypes();
		ParameterType type = new ParameterTypeInt(PARAMETER_NUMBER_OF_VALIDATIONS,
				"The number of validations that should be executed.", 2, Integer.MAX_VALUE, 10);
		type.setExpert(false);
		types.add(type);
		types.add(new ParameterTypeDouble(PARAMETER_SAMPLE_RATIO,
				"This ratio of examples will be sampled (with replacement) in each iteration.", 0.0d,
				Double.POSITIVE_INFINITY, 1.0d));
		types.add(new ParameterTypeBoolean(PARAMETER_USE_WEIGHTS,
				"If checked, example weights will be used for bootstrapping if such weights are available.", true));
		types.add(new ParameterTypeBoolean(PARAMETER_AVERAGE_PERFORMANCES_ONLY,
				"Indicates if only performance vectors should be averaged or all types of averagable result vectors.",
				true));
		types.addAll(RandomGenerator.getRandomGeneratorParameters(this));
		return types;
	}

	@Override
	public boolean supportsCapability(OperatorCapability capability) {
		return true;
	}
}
