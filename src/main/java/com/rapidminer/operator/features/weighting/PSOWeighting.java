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
package com.rapidminer.operator.features.weighting;

import java.util.List;

import com.rapidminer.example.Attribute;
import com.rapidminer.example.AttributeWeights;
import com.rapidminer.example.ExampleSet;
import com.rapidminer.example.set.AttributeWeightedExampleSet;
import com.rapidminer.operator.OperatorChain;
import com.rapidminer.operator.OperatorDescription;
import com.rapidminer.operator.OperatorException;
import com.rapidminer.operator.ValueDouble;
import com.rapidminer.operator.performance.PerformanceVector;
import com.rapidminer.operator.ports.InputPort;
import com.rapidminer.operator.ports.OutputPort;
import com.rapidminer.operator.ports.PortPairExtender;
import com.rapidminer.operator.ports.metadata.GenerateNewMDRule;
import com.rapidminer.operator.ports.metadata.SubprocessTransformRule;
import com.rapidminer.parameter.ParameterType;
import com.rapidminer.parameter.ParameterTypeBoolean;
import com.rapidminer.parameter.ParameterTypeDouble;
import com.rapidminer.parameter.ParameterTypeInt;
import com.rapidminer.parameter.UndefinedParameterError;
import com.rapidminer.parameter.conditions.BooleanParameterCondition;
import com.rapidminer.tools.RandomGenerator;
import com.rapidminer.tools.math.optimization.Optimization;
import com.rapidminer.tools.math.optimization.ec.pso.PSOOptimization;


/**
 * This operator performs the weighting of features with a particle swarm approach.
 *
 * @author Ingo Mierswa
 */
public class PSOWeighting extends OperatorChain {

	/** The parameter name for &quot;Activates the normalization of all weights.&quot; */
	public static final String PARAMETER_NORMALIZE_WEIGHTS = "normalize_weights";

	/** The parameter name for &quot;Number of individuals per generation.&quot; */
	public static final String PARAMETER_POPULATION_SIZE = "population_size";

	/**
	 * The parameter name for &quot;Number of generations after which to terminate the
	 * algorithm.&quot;
	 */
	public static final String PARAMETER_MAXIMUM_NUMBER_OF_GENERATIONS = "maximum_number_of_generations";

	public static final String PARAMETER_USE_EARLY_STOPPING = "use_early_stopping";

	/**
	 * The parameter name for &quot;Stop criterion: Stop after n generations without improval of the
	 * performance (-1: perform all generations).&quot;
	 */
	public static final String PARAMETER_GENERATIONS_WITHOUT_IMPROVAL = "generations_without_improval";

	/** The parameter name for &quot;The (initial) weight for the old weighting.&quot; */
	public static final String PARAMETER_INERTIA_WEIGHT = "inertia_weight";

	/** The parameter name for &quot;The weight for the individual's best position during run.&quot; */
	public static final String PARAMETER_LOCAL_BEST_WEIGHT = "local_best_weight";

	/** The parameter name for &quot;The weight for the population's best position during run.&quot; */
	public static final String PARAMETER_GLOBAL_BEST_WEIGHT = "global_best_weight";

	/** The parameter name for &quot;If set to true the inertia weight is improved during run.&quot; */
	public static final String PARAMETER_DYNAMIC_INERTIA_WEIGHT = "dynamic_inertia_weight";

	/** The parameter name for &quot;The lower bound for the weights.&quot; */
	public static final String PARAMETER_MIN_WEIGHT = "min_weight";

	/** The parameter name for &quot;The upper bound for the weights.&quot; */
	public static final String PARAMETER_MAX_WEIGHT = "max_weight";

	private final InputPort exampleSetInput = getInputPorts().createPort("example set", ExampleSet.class);
	private final OutputPort weightsOutput = getOutputPorts().createPort("weights");
	private final OutputPort exampleSetOutput = getOutputPorts().createPort("example set");
	private final OutputPort performanceOutput = getOutputPorts().createPort("performance");
	private final InputPort performanceInnerSink = getSubprocess(0).getInnerSinks().createPort("performance",
			PerformanceVector.class);
	private final OutputPort exampleSetInnerSource = getSubprocess(0).getInnerSources().createPort("example set");
	private final PortPairExtender inputExtender = new PortPairExtender("input", getInputPorts(), getSubprocess(0)
			.getInnerSources());

	/** The optimization class. */
	private static class PSOWeightingOptimization extends PSOOptimization {

		private final PSOWeighting op;

		public PSOWeightingOptimization(PSOWeighting op, int individualSize, RandomGenerator random)
				throws UndefinedParameterError {
			super(op.getParameterAsInt(PARAMETER_POPULATION_SIZE), individualSize, op
					.getParameterAsInt(PARAMETER_MAXIMUM_NUMBER_OF_GENERATIONS), op
					.getParameterAsBoolean(PARAMETER_USE_EARLY_STOPPING) ? op
							.getParameterAsInt(PARAMETER_GENERATIONS_WITHOUT_IMPROVAL) : -1, op
							.getParameterAsDouble(PARAMETER_INERTIA_WEIGHT), op.getParameterAsDouble(PARAMETER_LOCAL_BEST_WEIGHT),
							op.getParameterAsDouble(PARAMETER_GLOBAL_BEST_WEIGHT), op.getParameterAsDouble(PARAMETER_MIN_WEIGHT), op
							.getParameterAsDouble(PARAMETER_MAX_WEIGHT), op
							.getParameterAsBoolean(PARAMETER_DYNAMIC_INERTIA_WEIGHT), random, op);
			this.op = op;
		}

		/**
		 * Uses the inner operators of the weighting operator to determine the best weights.
		 */
		@Override
		public PerformanceVector evaluateIndividual(double[] individual) throws OperatorException {
			return op.evaluateIndividual(individual);
		}

		@Override
		public void nextIteration() throws OperatorException {
			super.nextIteration();
			op.inApplyLoop();
		}
	}

	private Optimization optimization;

	private ExampleSet exampleSet;

	public PSOWeighting(OperatorDescription description) {
		super(description, "Performance Evaluation");

		inputExtender.start();
		getTransformer().addPassThroughRule(exampleSetInput, exampleSetInnerSource);
		getTransformer().addRule(inputExtender.makePassThroughRule());
		getTransformer().addRule(new SubprocessTransformRule(getSubprocess(0)));
		getTransformer().addPassThroughRule(performanceInnerSink, performanceOutput);
		getTransformer().addPassThroughRule(exampleSetInput, exampleSetOutput);
		getTransformer().addRule(new GenerateNewMDRule(weightsOutput, AttributeWeights.class));

		addValue(new ValueDouble("generation", "The number of the current generation.") {

			@Override
			public double getDoubleValue() {
				return optimization.getGeneration();
			}
		});
		addValue(new ValueDouble("performance", "The performance of the current generation (main criterion).") {

			@Override
			public double getDoubleValue() {
				return optimization.getBestFitnessInGeneration();
			}
		});
		addValue(new ValueDouble("best", "The performance of the best individual ever (main criterion).") {

			@Override
			public double getDoubleValue() {
				return optimization.getBestFitnessEver();
			}
		});
	}

	@Override
	public void doWork() throws OperatorException {
		// optimization
		this.exampleSet = exampleSetInput.getData(ExampleSet.class);
		this.optimization = new PSOWeightingOptimization(this, this.exampleSet.getAttributes().size(),
				RandomGenerator.getRandomGenerator(this));
		this.optimization.optimize();

		// create and return result
		double[] globalBestWeights = optimization.getBestValuesEver();
		AttributeWeightedExampleSet result = createWeightedExampleSet(globalBestWeights);
		AttributeWeights weights = new AttributeWeights();
		int index = 0;
		for (Attribute attribute : result.getAttributes()) {
			weights.setWeight(attribute.getName(), globalBestWeights[index++]);
		}

		// normalize
		if (getParameterAsBoolean(PARAMETER_NORMALIZE_WEIGHTS)) {
			weights.normalize();
		}

		exampleSetOutput.deliver(exampleSet);
		weightsOutput.deliver(weights);
		performanceOutput.deliver(optimization.getBestPerformanceEver());
	}

	private PerformanceVector evaluateIndividual(double[] individual) throws OperatorException {
		// check if all weights are zero
		boolean onlyZeros = true;
		for (int i = 0; i < individual.length; i++) {
			if (individual[i] != 0.0d) {
				onlyZeros = false;
				break;
			}
		}
		if (onlyZeros) {
			return null;
		}

		// use inner validation for performance estimation
		AttributeWeightedExampleSet evaluationSet = createWeightedExampleSet(individual).createCleanClone();

		exampleSetInnerSource.deliver(evaluationSet);
		inputExtender.passDataThrough();
		getSubprocess(0).execute();
		return performanceInnerSink.getData(PerformanceVector.class);
	}

	private AttributeWeightedExampleSet createWeightedExampleSet(double[] weights) {
		AttributeWeightedExampleSet result = new AttributeWeightedExampleSet(exampleSet, null);
		int index = 0;
		for (Attribute attribute : exampleSet.getAttributes()) {
			result.setWeight(attribute, weights[index++]);
		}
		return result;
	}

	@Override
	public List<ParameterType> getParameterTypes() {
		List<ParameterType> types = super.getParameterTypes();
		types.add(new ParameterTypeBoolean(PARAMETER_NORMALIZE_WEIGHTS, "Activates the normalization of all weights.", false));
		ParameterType type = new ParameterTypeInt(PARAMETER_POPULATION_SIZE, "Number of individuals per generation.", 1,
				Integer.MAX_VALUE, 5);
		type.setExpert(false);
		types.add(type);

		type = new ParameterTypeInt(PARAMETER_MAXIMUM_NUMBER_OF_GENERATIONS,
				"Number of generations after which to terminate the algorithm.", 1, Integer.MAX_VALUE, 30);
		type.setExpert(false);
		types.add(type);

		types.add(new ParameterTypeBoolean(PARAMETER_USE_EARLY_STOPPING,
				"Enables early stopping. If unchecked, always the maximum number of generations is performed.", false));
		type = new ParameterTypeInt(PARAMETER_GENERATIONS_WITHOUT_IMPROVAL,
				"Stop criterion: Stop after n generations without improval of the performance.", 1, Integer.MAX_VALUE, 2);
		type.registerDependencyCondition(new BooleanParameterCondition(this, PARAMETER_USE_EARLY_STOPPING, true, true));
		type.setExpert(false);
		types.add(type);

		types.add(new ParameterTypeDouble(PARAMETER_INERTIA_WEIGHT, "The (initial) weight for the old weighting.", 0.0d,
				Double.POSITIVE_INFINITY, 1.0d));
		types.add(new ParameterTypeDouble(PARAMETER_LOCAL_BEST_WEIGHT,
				"The weight for the individual's best position during run.", 0.0d, Double.POSITIVE_INFINITY, 1.0d));
		types.add(new ParameterTypeDouble(PARAMETER_GLOBAL_BEST_WEIGHT,
				"The weight for the population's best position during run.", 0.0d, Double.POSITIVE_INFINITY, 1.0d));
		types.add(new ParameterTypeBoolean(PARAMETER_DYNAMIC_INERTIA_WEIGHT,
				"If set to true the inertia weight is improved during run.", true));
		types.add(new ParameterTypeDouble(PARAMETER_MIN_WEIGHT, "The lower bound for the weights.",
				Double.NEGATIVE_INFINITY, Double.POSITIVE_INFINITY, 0.0d));
		types.add(new ParameterTypeDouble(PARAMETER_MAX_WEIGHT, "The upper bound for the weights.",
				Double.NEGATIVE_INFINITY, Double.POSITIVE_INFINITY, 1.0d));

		types.addAll(RandomGenerator.getRandomGeneratorParameters(this));
		return types;
	}
}
