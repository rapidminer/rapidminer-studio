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
import java.util.Random;

import com.rapidminer.example.ExampleSet;
import com.rapidminer.example.set.MappedExampleSet;
import com.rapidminer.operator.OperatorDescription;
import com.rapidminer.operator.OperatorException;
import com.rapidminer.operator.ValueDouble;
import com.rapidminer.operator.ports.metadata.MDInteger;
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
 * @author Ingo Mierswa
 */
public abstract class AbstractBootstrappingValidation extends ValidationChain {

	public static final String PARAMETER_NUMBER_OF_VALIDATIONS = "number_of_validations";

	public static final String PARAMETER_SAMPLE_RATIO = "sample_ratio";

	public static final String PARAMETER_AVERAGE_PERFORMANCES_ONLY = "average_performances_only";

	private int number;

	private int iteration;

	public AbstractBootstrappingValidation(OperatorDescription description) {
		super(description);
		addValue(new ValueDouble("iteration", "The number of the current iteration.") {

			@Override
			public double getDoubleValue() {
				return iteration;
			}
		});
	}

	protected abstract int[] createMapping(ExampleSet exampleSet, int size, Random random) throws OperatorException;

	@Override
	public void estimatePerformance(ExampleSet inputSet) throws OperatorException {
		number = getParameterAsInt(PARAMETER_NUMBER_OF_VALIDATIONS);
		double sampleRatio = getParameterAsDouble(PARAMETER_SAMPLE_RATIO);

		// start bootstrapping loop
		RandomGenerator random = RandomGenerator.getRandomGenerator(this);
		for (iteration = 0; iteration < number; iteration++) {
			int[] mapping = createMapping(inputSet, (int) Math.round(inputSet.size() * sampleRatio), random);
			MappedExampleSet trainingSet = new MappedExampleSet(inputSet, mapping, true);
			learn(trainingSet);

			MappedExampleSet inverseExampleSet = new MappedExampleSet(inputSet, mapping, false);
			evaluate(inverseExampleSet);
			inApplyLoop();
		}
		// end loop
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
				"Number of subsets for the crossvalidation.", 2, Integer.MAX_VALUE, 10);
		type.setExpert(false);
		types.add(type);
		types.add(new ParameterTypeDouble(PARAMETER_SAMPLE_RATIO,
				"This ratio of examples will be sampled (with replacement) in each iteration.", 0.0d,
				Double.POSITIVE_INFINITY, 1.0d));
		types.add(new ParameterTypeBoolean(PARAMETER_AVERAGE_PERFORMANCES_ONLY,
				"Indicates if only performance vectors should be averaged or all types of averagable result vectors.",
				true));

		types.addAll(RandomGenerator.getRandomGeneratorParameters(this));

		return types;
	}
}
