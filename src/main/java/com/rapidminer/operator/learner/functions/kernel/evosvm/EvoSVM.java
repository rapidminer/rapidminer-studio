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
package com.rapidminer.operator.learner.functions.kernel.evosvm;

import com.rapidminer.example.Attribute;
import com.rapidminer.example.Example;
import com.rapidminer.example.ExampleSet;
import com.rapidminer.example.set.SplittedExampleSet;
import com.rapidminer.operator.Model;
import com.rapidminer.operator.OperatorCapability;
import com.rapidminer.operator.OperatorDescription;
import com.rapidminer.operator.OperatorException;
import com.rapidminer.operator.UserError;
import com.rapidminer.operator.annotation.ResourceConsumptionEstimator;
import com.rapidminer.operator.learner.functions.kernel.AbstractKernelBasedLearner;
import com.rapidminer.operator.performance.PerformanceVector;
import com.rapidminer.parameter.ParameterType;
import com.rapidminer.parameter.ParameterTypeBoolean;
import com.rapidminer.parameter.ParameterTypeCategory;
import com.rapidminer.parameter.ParameterTypeDouble;
import com.rapidminer.parameter.ParameterTypeInt;
import com.rapidminer.tools.OperatorResourceConsumptionHandler;
import com.rapidminer.tools.RandomGenerator;
import com.rapidminer.tools.Tools;
import com.rapidminer.tools.math.kernels.Kernel;
import com.rapidminer.tools.math.optimization.ec.es.ESOptimization;

import java.util.List;


/**
 * <p>
 * This is a SVM implementation using an evolutionary algorithm (ES) to solve the dual optimization
 * problem of a SVM. It turns out that on many datasets this simple implementation is as fast and
 * accurate as the usual SVM implementations. In addition, it is also capable of learning with
 * Kernels which are not positive semi-definite and can also be used for multi-objective learning
 * which makes the selection of C unecessary before learning.
 * </p>
 * 
 * <p>
 * Mierswa, Ingo. Evolutionary Learning with Kernels: A Generic Solution for Large Margin Problems.
 * In Proc. of the Genetic and Evolutionary Computation Conference (GECCO 2006), 2006.
 * </p>
 * 
 * @rapidminer.index SVM
 * 
 * @author Ingo Mierswa
 */
public class EvoSVM extends AbstractKernelBasedLearner {

	/**
	 * The parameter name for &quot;The SVM complexity constant (0: calculates probably good
	 * value).&quot;
	 */
	public static final String PARAMETER_C = "C";

	/**
	 * The parameter name for &quot;The width of the regression tube loss function of the regression
	 * SVM&quot;
	 */
	public static final String PARAMETER_EPSILON = "epsilon";

	/** The parameter name for &quot;The type of start population initialization.&quot; */
	public static final String PARAMETER_START_POPULATION_TYPE = "start_population_type";

	/** The parameter name for &quot;Stop after this many evaluations&quot; */
	public static final String PARAMETER_MAX_GENERATIONS = "max_generations";

	/**
	 * The parameter name for &quot;Stop after this number of generations without improvement (-1:
	 * optimize until max_iterations).&quot;
	 */
	public static final String PARAMETER_GENERATIONS_WITHOUT_IMPROVAL = "generations_without_improval";

	/** The parameter name for &quot;The population size (-1: number of examples)&quot; */
	public static final String PARAMETER_POPULATION_SIZE = "population_size";

	/**
	 * The parameter name for &quot;The fraction of the population used for tournament
	 * selection.&quot;
	 */
	public static final String PARAMETER_TOURNAMENT_FRACTION = "tournament_fraction";

	/**
	 * The parameter name for &quot;Indicates if the best individual should survive (elititst
	 * selection).&quot;
	 */
	public static final String PARAMETER_KEEP_BEST = "keep_best";

	/** The parameter name for &quot;The type of the mutation operator.&quot; */
	public static final String PARAMETER_MUTATION_TYPE = "mutation_type";

	/** The parameter name for &quot;The type of the selection operator.&quot; */
	public static final String PARAMETER_SELECTION_TYPE = "selection_type";

	/** The parameter name for &quot;The probability for crossovers.&quot; */
	public static final String PARAMETER_CROSSOVER_PROB = "crossover_prob";

	/**
	 * The parameter name for &quot;Uses this amount as a hold out set to estimate generalization
	 * error after learning (currently only used for multi-objective classification).&quot;
	 */
	public static final String PARAMETER_HOLD_OUT_SET_RATIO = "hold_out_set_ratio";

	/**
	 * The parameter name for &quot;Indicates if a dialog with a convergence plot should be
	 * drawn.&quot;
	 */
	public static final String PARAMETER_SHOW_CONVERGENCE_PLOT = "show_convergence_plot";

	public static final String PARAMETER_SHOW_POPULATION_PLOT = "show_population_plot";

	/**
	 * The parameter name for &quot;Indicates if final optimization fitness should be returned as
	 * performance.&quot;
	 */
	public static final String PARAMETER_RETURN_OPTIMIZATION_PERFORMANCE = "return_optimization_performance";

	/** The optimization procedure. */
	private EvoOptimization optimization;

	/**
	 * Creates a new SVM which uses an Evolutionary Strategy approach for optimization.
	 */
	public EvoSVM(OperatorDescription description) {
		super(description);
	}

	/** Returns the value of the corresponding parameter. */
	@Override
	public boolean shouldDeliverOptimizationPerformance() {
		return getParameterAsBoolean(PARAMETER_RETURN_OPTIMIZATION_PERFORMANCE);
	}

	/**
	 * Returns the optimization performance of the best result. This method must be called after
	 * training, not before.
	 */
	@Override
	public PerformanceVector getOptimizationPerformance() {
		return optimization.getOptimizationPerformance();
	}

	/** Learns and returns a model. */
	@Override
	public Model learn(ExampleSet exampleSet) throws OperatorException {
		// if (exampleSet.getLabel().getNumberOfValues() != 2) {
		// throw new UserError(this, 114, getName(), exampleSet.getLabel().getName());
		// }

		// kernel
		Kernel kernel = Kernel.createKernel(this);

		RandomGenerator random = RandomGenerator.getRandomGenerator(this);

		// optimization
		Attribute label = exampleSet.getAttributes().getLabel();
		if (label.isNominal()) {
			if (label.getMapping().size() == 2) {
				ExampleSet holdOutSet = null;
				ExampleSet trainingSet = exampleSet;
				double holdOutSetRatio = getParameterAsDouble(PARAMETER_HOLD_OUT_SET_RATIO);
				if (!Tools.isZero(holdOutSetRatio)) {
					SplittedExampleSet splittedExampleSet = new SplittedExampleSet(exampleSet, new double[] {
							1.0d - holdOutSetRatio, holdOutSetRatio }, SplittedExampleSet.SHUFFLED_SAMPLING,
							getParameterAsBoolean(RandomGenerator.PARAMETER_USE_LOCAL_RANDOM_SEED),
							getParameterAsInt(RandomGenerator.PARAMETER_LOCAL_RANDOM_SEED));
					splittedExampleSet.selectSingleSubset(0);
					trainingSet = splittedExampleSet.clone();
					splittedExampleSet.selectAllSubsetsBut(0);
					holdOutSet = splittedExampleSet.clone();
				}
				optimization = new ClassificationEvoOptimization(trainingSet, kernel, getParameterAsDouble(PARAMETER_C),
						getParameterAsInt(PARAMETER_START_POPULATION_TYPE), getParameterAsInt(PARAMETER_MAX_GENERATIONS),
						getParameterAsInt(PARAMETER_GENERATIONS_WITHOUT_IMPROVAL),
						getParameterAsInt(PARAMETER_POPULATION_SIZE), getParameterAsInt(PARAMETER_SELECTION_TYPE),
						getParameterAsDouble(PARAMETER_TOURNAMENT_FRACTION), getParameterAsBoolean(PARAMETER_KEEP_BEST),
						getParameterAsInt(PARAMETER_MUTATION_TYPE), getParameterAsDouble(PARAMETER_CROSSOVER_PROB),
						getParameterAsBoolean(PARAMETER_SHOW_CONVERGENCE_PLOT),
						getParameterAsBoolean(PARAMETER_SHOW_POPULATION_PLOT), holdOutSet, random, this, this);
			} else {
				throw new UserError(this, 114, getName(), label.getName());
			}
		} else {
			optimization = new RegressionEvoOptimization(exampleSet, kernel, getParameterAsDouble(PARAMETER_C),
					getParameterAsDouble(PARAMETER_EPSILON), getParameterAsInt(PARAMETER_START_POPULATION_TYPE),
					getParameterAsInt(PARAMETER_MAX_GENERATIONS), getParameterAsInt(PARAMETER_GENERATIONS_WITHOUT_IMPROVAL),
					getParameterAsInt(PARAMETER_POPULATION_SIZE), getParameterAsInt(PARAMETER_SELECTION_TYPE),
					getParameterAsDouble(PARAMETER_TOURNAMENT_FRACTION), getParameterAsBoolean(PARAMETER_KEEP_BEST),
					getParameterAsInt(PARAMETER_MUTATION_TYPE), getParameterAsDouble(PARAMETER_CROSSOVER_PROB),
					getParameterAsBoolean(PARAMETER_SHOW_CONVERGENCE_PLOT),
					getParameterAsBoolean(PARAMETER_SHOW_POPULATION_PLOT), random, this, this);
		}
		return optimization.train();
	}

	/**
	 * Returns true for numerical attributes, binominal classes, and numerical target attributes.
	 */
	@Override
	public boolean supportsCapability(OperatorCapability lc) {
		if (lc == OperatorCapability.NUMERICAL_ATTRIBUTES) {
			return true;
		}
		if (lc == OperatorCapability.BINOMINAL_LABEL) {
			return true;
		}
		if (lc == OperatorCapability.NUMERICAL_LABEL) {
			return true;
		}
		if (lc == OperatorCapability.WEIGHTED_EXAMPLES) {
			return true;
		}
		if (lc == OperatorCapability.FORMULA_PROVIDER) {
			return true;
		}
		return false;
	}

	public static double[] createBoundArray(double bound, int size) {
		double[] result = new double[size];
		for (int i = 0; i < result.length; i++) {
			result[i] = bound;
		}
		return result;
	}

	public static final double[] determineMax(double _c, Kernel kernel, ExampleSet exampleSet, int selectionType,
			int arraySize) {
		double[] max = new double[arraySize];

		// init the kernel !
		kernel.init(exampleSet);

		double globalC = 1000;
		if (selectionType != ESOptimization.NON_DOMINATED_SORTING_SELECTION) {
			if (_c <= 0.0d) {
				double c = 0.0d;
				for (int i = 0; i < exampleSet.size(); i++) {
					c += kernel.getDistance(i, i);
				}
				globalC = exampleSet.size() / c;
				exampleSet.getLog().log("Determine probably good value for C: set to " + c);
			} else {
				globalC = _c;
			}
		}

		for (int i = 0; i < max.length; i++) {
			max[i] = globalC;
		}

		// apply weights
		Attribute weightAttribute = exampleSet.getAttributes().getWeight();
		if (weightAttribute != null) {
			int counter = 0;
			for (Example e : exampleSet) {
				max[counter++] *= e.getValue(weightAttribute);
			}
		}

		return max;
	}

	@Override
	public List<ParameterType> getParameterTypes() {
		List<ParameterType> types = super.getParameterTypes();
		// adding kernel parameters
		types.addAll(Kernel.getParameters(this));
		// adding SVM parameters
		ParameterType type = new ParameterTypeDouble(PARAMETER_C,
				"The SVM complexity constant (0: calculates probably good value).", 0.0d, Double.POSITIVE_INFINITY, 0.0d);
		type.setExpert(false);
		types.add(type);
		type = new ParameterTypeDouble(PARAMETER_EPSILON,
				"The width of the regression tube loss function of the regression SVM", 0.0d, Double.POSITIVE_INFINITY, 0.1d);
		type.setExpert(false);
		types.add(type);
		types.add(new ParameterTypeCategory(PARAMETER_START_POPULATION_TYPE, "The type of start population initialization.",
				ESOptimization.POPULATION_INIT_TYPES, ESOptimization.INIT_TYPE_RANDOM));
		types.add(new ParameterTypeInt(PARAMETER_MAX_GENERATIONS, "Stop after this many evaluations", 1, Integer.MAX_VALUE,
				10000));
		types.add(new ParameterTypeInt(PARAMETER_GENERATIONS_WITHOUT_IMPROVAL,
				"Stop after this number of generations without improvement (-1: optimize until max_iterations).", -1,
				Integer.MAX_VALUE, 30));
		types.add(new ParameterTypeInt(PARAMETER_POPULATION_SIZE, "The population size (-1: number of examples)", -1,
				Integer.MAX_VALUE, 1));
		types.add(new ParameterTypeDouble(PARAMETER_TOURNAMENT_FRACTION,
				"The fraction of the population used for tournament selection.", 0.0d, Double.POSITIVE_INFINITY, 0.75d));
		types.add(new ParameterTypeBoolean(PARAMETER_KEEP_BEST,
				"Indicates if the best individual should survive (elititst selection).", true));
		types.add(new ParameterTypeCategory(PARAMETER_MUTATION_TYPE, "The type of the mutation operator.",
				ESOptimization.MUTATION_TYPES, ESOptimization.GAUSSIAN_MUTATION));
		types.add(new ParameterTypeCategory(PARAMETER_SELECTION_TYPE, "The type of the selection operator.",
				ESOptimization.SELECTION_TYPES, ESOptimization.TOURNAMENT_SELECTION));
		types.add(new ParameterTypeDouble(PARAMETER_CROSSOVER_PROB, "The probability for crossovers.", 0.0d, 1.0d, 1.0d));

		types.addAll(RandomGenerator.getRandomGeneratorParameters(this));

		types.add(new ParameterTypeDouble(
				PARAMETER_HOLD_OUT_SET_RATIO,
				"Uses this amount as a hold out set to estimate generalization error after learning (currently only used for multi-objective classification).",
				0.0d, 1.0d, 0.0d));
		types.add(new ParameterTypeBoolean(PARAMETER_SHOW_CONVERGENCE_PLOT,
				"Indicates if a dialog with a convergence plot should be drawn.", false));
		types.add(new ParameterTypeBoolean(PARAMETER_SHOW_POPULATION_PLOT,
				"Indicates if the population plot in case of the non-dominated sorting should be shown.", false));
		types.add(new ParameterTypeBoolean(PARAMETER_RETURN_OPTIMIZATION_PERFORMANCE,
				"Indicates if final optimization fitness should be returned as performance.", false));
		return types;
	}

	@Override
	public ResourceConsumptionEstimator getResourceConsumptionEstimator() {
		return OperatorResourceConsumptionHandler.getResourceConsumptionEstimator(getExampleSetInputPort(), EvoSVM.class,
				null);
	}
}
