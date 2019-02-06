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
package com.rapidminer.operator.learner.functions;

import java.util.List;

import com.rapidminer.example.ExampleSet;
import com.rapidminer.operator.Model;
import com.rapidminer.operator.OperatorCapability;
import com.rapidminer.operator.OperatorDescription;
import com.rapidminer.operator.OperatorException;
import com.rapidminer.operator.UserError;
import com.rapidminer.operator.learner.AbstractLearner;
import com.rapidminer.operator.learner.PredictionModel;
import com.rapidminer.operator.performance.PerformanceVector;
import com.rapidminer.parameter.ParameterType;
import com.rapidminer.parameter.ParameterTypeBoolean;
import com.rapidminer.parameter.ParameterTypeCategory;
import com.rapidminer.parameter.ParameterTypeDouble;
import com.rapidminer.parameter.ParameterTypeInt;
import com.rapidminer.tools.RandomGenerator;
import com.rapidminer.tools.math.optimization.ec.es.ESOptimization;


/**
 * This operator determines a logistic regression model.
 *
 * @author Ingo Mierswa, Tobias Malbrecht
 *
 * @deprecated This learner is not used anymore.
 */
@Deprecated
public class LogisticRegression extends AbstractLearner {

	/** The parameter name for &quot;Determines whether to include an intercept.&quot; */
	public static final String PARAMETER_ADD_INTERCEPT = "add_intercept";

	/** The parameter name for &quot;Determines whether to return the performance.&quot; */
	public static final String PARAMETER_RETURN_PERFORMANCE = "return_model_performance";

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
	 * The parameter name for &quot;Indicates if a dialog with a convergence plot should be
	 * drawn.&quot;
	 */
	public static final String PARAMETER_SHOW_CONVERGENCE_PLOT = "show_convergence_plot";

	private PerformanceVector estimatedPerformance;

	public LogisticRegression(OperatorDescription description) {
		super(description);
	}

	@Override
	public Model learn(ExampleSet exampleSet) throws OperatorException {
		RandomGenerator random = RandomGenerator.getRandomGenerator(this);
		LogisticRegressionOptimization optimization = new LogisticRegressionOptimization(exampleSet,
				getParameterAsBoolean(PARAMETER_ADD_INTERCEPT), getParameterAsInt(PARAMETER_START_POPULATION_TYPE),
				getParameterAsInt(PARAMETER_MAX_GENERATIONS), getParameterAsInt(PARAMETER_GENERATIONS_WITHOUT_IMPROVAL),
				getParameterAsInt(PARAMETER_POPULATION_SIZE), getParameterAsInt(PARAMETER_SELECTION_TYPE),
				getParameterAsDouble(PARAMETER_TOURNAMENT_FRACTION), getParameterAsBoolean(PARAMETER_KEEP_BEST),
				getParameterAsInt(PARAMETER_MUTATION_TYPE), getParameterAsDouble(PARAMETER_CROSSOVER_PROB),
				getParameterAsBoolean(PARAMETER_SHOW_CONVERGENCE_PLOT), random, this, this);
		LogisticRegressionModel model = optimization.train();
		estimatedPerformance = optimization.getPerformance();
		return model;
	}

	@Override
	public boolean canEstimatePerformance() {
		return true;
	}

	@Override
	public PerformanceVector getEstimatedPerformance() throws OperatorException {
		if (getParameterAsBoolean(PARAMETER_RETURN_PERFORMANCE)) {
			if (estimatedPerformance != null) {
				return estimatedPerformance;
			}
		}
		throw new UserError(this, 912, getName(), "could not deliver optimization performance.");
	}

	@Override
	public Class<? extends PredictionModel> getModelClass() {
		return LogisticRegressionModel.class;
	}

	@Override
	public boolean supportsCapability(OperatorCapability lc) {
		if (lc == OperatorCapability.NUMERICAL_ATTRIBUTES) {
			return true;
		}
		if (lc == OperatorCapability.BINOMINAL_LABEL) {
			return true;
		}
		if (lc == OperatorCapability.WEIGHTED_EXAMPLES) {
			return true;
		}
		return false;
	}

	@Override
	public List<ParameterType> getParameterTypes() {
		List<ParameterType> types = super.getParameterTypes();
		types.add(new ParameterTypeBoolean(PARAMETER_ADD_INTERCEPT, "Determines whether to include an intercept.", true));

		types.add(new ParameterTypeCategory(PARAMETER_START_POPULATION_TYPE, "The type of start population initialization.",
				ESOptimization.POPULATION_INIT_TYPES, ESOptimization.INIT_TYPE_RANDOM));
		types.add(new ParameterTypeInt(PARAMETER_MAX_GENERATIONS, "Stop after this many evaluations", 1, Integer.MAX_VALUE,
				10000));
		types.add(new ParameterTypeInt(PARAMETER_GENERATIONS_WITHOUT_IMPROVAL,
				"Stop after this number of generations without improvement (-1: optimize until max_iterations).", -1,
				Integer.MAX_VALUE, 300));
		types.add(new ParameterTypeInt(PARAMETER_POPULATION_SIZE, "The population size (-1: number of examples)", -1,
				Integer.MAX_VALUE, 3));
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

		types.add(new ParameterTypeBoolean(PARAMETER_SHOW_CONVERGENCE_PLOT,
				"Indicates if a dialog with a convergence plot should be drawn.", false));

		// deprecated parameters
		ParameterType type = new ParameterTypeBoolean(PARAMETER_RETURN_PERFORMANCE,
				"Determines whether to return the performance.", true);
		type.setDeprecated();
		types.add(type);

		return types;
	}
}
