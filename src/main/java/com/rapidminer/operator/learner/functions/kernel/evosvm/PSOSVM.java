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
import com.rapidminer.example.ExampleSet;
import com.rapidminer.operator.Model;
import com.rapidminer.operator.OperatorCapability;
import com.rapidminer.operator.OperatorDescription;
import com.rapidminer.operator.OperatorException;
import com.rapidminer.operator.UserError;
import com.rapidminer.operator.annotation.ResourceConsumptionEstimator;
import com.rapidminer.operator.learner.functions.kernel.AbstractKernelBasedLearner;
import com.rapidminer.parameter.ParameterType;
import com.rapidminer.parameter.ParameterTypeBoolean;
import com.rapidminer.parameter.ParameterTypeDouble;
import com.rapidminer.parameter.ParameterTypeInt;
import com.rapidminer.tools.OperatorResourceConsumptionHandler;
import com.rapidminer.tools.RandomGenerator;
import com.rapidminer.tools.math.kernels.Kernel;

import java.util.List;


/**
 * This is a SVM implementation using a particle swarm optimization (PSO) approach to solve the dual
 * optimization problem of a SVM. It turns out that on many datasets this simple implementation is
 * as fast and accurate as the usual SVM implementations.
 * 
 * @rapidminer.index SVM
 * 
 * @author Ingo Mierswa
 */
public class PSOSVM extends AbstractKernelBasedLearner {

	/**
	 * The parameter name for &quot;Indicates if a dialog with a convergence plot should be
	 * drawn.&quot;
	 */
	public static final String PARAMETER_SHOW_CONVERGENCE_PLOT = "show_convergence_plot";

	/**
	 * The parameter name for &quot;The SVM complexity constant (0: calculates probably good
	 * value).&quot;
	 */
	public static final String PARAMETER_C = "C";

	/** The parameter name for &quot;Stop after this many evaluations&quot; */
	public static final String PARAMETER_MAX_EVALUATIONS = "max_evaluations";

	/**
	 * The parameter name for &quot;Stop after this number of generations without improvement (-1:
	 * optimize until max_iterations).&quot;
	 */
	public static final String PARAMETER_GENERATIONS_WITHOUT_IMPROVAL = "generations_without_improval";

	/** The parameter name for &quot;The population size (-1: number of examples)&quot; */
	public static final String PARAMETER_POPULATION_SIZE = "population_size";

	/** The parameter name for &quot;The (initial) weight for the old weighting.&quot; */
	public static final String PARAMETER_INERTIA_WEIGHT = "inertia_weight";

	/** The parameter name for &quot;The weight for the individual's best position during run.&quot; */
	public static final String PARAMETER_LOCAL_BEST_WEIGHT = "local_best_weight";

	/** The parameter name for &quot;The weight for the population's best position during run.&quot; */
	public static final String PARAMETER_GLOBAL_BEST_WEIGHT = "global_best_weight";

	/** The parameter name for &quot;If set to true the inertia weight is improved during run.&quot; */
	public static final String PARAMETER_DYNAMIC_INERTIA_WEIGHT = "dynamic_inertia_weight";

	/**
	 * Creates a new SVM which uses a particle swarm optimization approach for optimization.
	 */
	public PSOSVM(OperatorDescription description) {
		super(description);
	}

	/** Learns and returns a model. */
	@Override
	public Model learn(ExampleSet exampleSet) throws OperatorException {
		Attribute label = exampleSet.getAttributes().getLabel();
		if (label.getMapping().size() != 2) {
			throw new UserError(this, 114, getName(), label.getName());
		}
		// kernel
		Kernel kernel = Kernel.createKernel(this);

		// optimization
		PSOSVMOptimization optimization = new PSOSVMOptimization(exampleSet, kernel, getParameterAsDouble(PARAMETER_C),
				getParameterAsInt(PARAMETER_MAX_EVALUATIONS), getParameterAsInt(PARAMETER_GENERATIONS_WITHOUT_IMPROVAL),
				getParameterAsInt(PARAMETER_POPULATION_SIZE), getParameterAsDouble(PARAMETER_INERTIA_WEIGHT),
				getParameterAsDouble(PARAMETER_LOCAL_BEST_WEIGHT), getParameterAsDouble(PARAMETER_GLOBAL_BEST_WEIGHT),
				getParameterAsBoolean(PARAMETER_DYNAMIC_INERTIA_WEIGHT),
				getParameterAsBoolean(PARAMETER_SHOW_CONVERGENCE_PLOT), RandomGenerator.getRandomGenerator(this), this);
		optimization.optimize();

		double[] bestAlphas = optimization.getBestValuesEver();
		return optimization.getModel(bestAlphas);
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
		if (lc == OperatorCapability.FORMULA_PROVIDER) {
			return true;
		}
		return false;
	}

	@Override
	public List<ParameterType> getParameterTypes() {
		List<ParameterType> types = super.getParameterTypes();
		types.add(new ParameterTypeBoolean(PARAMETER_SHOW_CONVERGENCE_PLOT,
				"Indicates if a dialog with a convergence plot should be drawn.", false));
		// adding Kernel parameter
		types.addAll(Kernel.getParameters(this));

		ParameterType type = new ParameterTypeDouble(PARAMETER_C,
				"The SVM complexity constant (0: calculates probably good value).", 0.0d, Double.POSITIVE_INFINITY, 0.0d);
		types.add(type);
		types.add(new ParameterTypeInt(PARAMETER_MAX_EVALUATIONS, "Stop after this many evaluations", 1, Integer.MAX_VALUE,
				500));
		types.add(new ParameterTypeInt(PARAMETER_GENERATIONS_WITHOUT_IMPROVAL,
				"Stop after this number of generations without improvement (-1: optimize until max_iterations).", -1,
				Integer.MAX_VALUE, 10));
		types.add(new ParameterTypeInt(PARAMETER_POPULATION_SIZE, "The population size (-1: number of examples)", -1,
				Integer.MAX_VALUE, 10));
		types.add(new ParameterTypeDouble(PARAMETER_INERTIA_WEIGHT, "The (initial) weight for the old weighting.", 0.0d,
				Double.POSITIVE_INFINITY, 0.1d));
		types.add(new ParameterTypeDouble(PARAMETER_LOCAL_BEST_WEIGHT,
				"The weight for the individual's best position during run.", 0.0d, Double.POSITIVE_INFINITY, 1.0d));
		types.add(new ParameterTypeDouble(PARAMETER_GLOBAL_BEST_WEIGHT,
				"The weight for the population's best position during run.", 0.0d, Double.POSITIVE_INFINITY, 1.0d));
		types.add(new ParameterTypeBoolean(PARAMETER_DYNAMIC_INERTIA_WEIGHT,
				"If set to true the inertia weight is improved during run.", true));
		types.addAll(RandomGenerator.getRandomGeneratorParameters(this));
		return types;
	}

	@Override
	public ResourceConsumptionEstimator getResourceConsumptionEstimator() {
		return OperatorResourceConsumptionHandler.getResourceConsumptionEstimator(getExampleSetInputPort(), PSOSVM.class,
				null);
	}
}
