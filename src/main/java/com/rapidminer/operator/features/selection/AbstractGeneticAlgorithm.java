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
package com.rapidminer.operator.features.selection;

import java.io.File;
import java.util.LinkedList;
import java.util.List;

import com.rapidminer.example.ExampleSet;
import com.rapidminer.example.Tools;
import com.rapidminer.operator.OperatorDescription;
import com.rapidminer.operator.OperatorException;
import com.rapidminer.operator.features.FeatureOperator;
import com.rapidminer.operator.features.Population;
import com.rapidminer.operator.features.PopulationOperator;
import com.rapidminer.parameter.ParameterType;
import com.rapidminer.parameter.ParameterTypeBoolean;
import com.rapidminer.parameter.ParameterTypeCategory;
import com.rapidminer.parameter.ParameterTypeDouble;
import com.rapidminer.parameter.ParameterTypeFile;
import com.rapidminer.parameter.ParameterTypeInt;
import com.rapidminer.parameter.UndefinedParameterError;
import com.rapidminer.parameter.conditions.BooleanParameterCondition;
import com.rapidminer.parameter.conditions.EqualTypeCondition;


/**
 * Genetic algorithms are general purpose optimization / search algorithms that are suitable in case
 * of no or little problem knowledge. <br/>
 *
 * A genetic algorithm works as follows
 * <ol>
 * <li>Generate an initial population consisting of <code>population_size</code> individuals. Each
 * attribute is switched on with probability <code>p_initialize</code></li>
 * <li>For all individuals in the population
 * <ul>
 * <li>Choose two individuals from the population and perform crossover with probability
 * <code>p_crossover</code>. The type of crossover can be selected by <code>crossover_type</code>.</li>
 * <li>Perform mutation, i.e. set used attributes to unused with probability <code>p_mutation</code>
 * and vice versa. Other mutations may for example create new attributes etc.</li>
 * </ul>
 * </li>
 * <li>Perform selection with a defined selection scheme.</li>
 * <li>As long as the fitness improves, go to 2</li>
 * </ol>
 *
 * If the example set contains value series attributes with blocknumbers, the whole block should be
 * switched on and off.
 *
 * @author Ingo Mierswa
 */
public abstract class AbstractGeneticAlgorithm extends FeatureOperator {

	/** The parameter name for &quot;Number of individuals per generation.&quot; */
	public static final String PARAMETER_POPULATION_SIZE = "population_size";

	/**
	 * The parameter name for &quot;Number of generations after which to terminate the
	 * algorithm.&quot;
	 */
	public static final String PARAMETER_MAXIMUM_NUMBER_OF_GENERATIONS = "maximum_number_of_generations";

	/**
	 * The parameter name for &quot;Stop criterion: Stop after n generations without improval of the
	 * performance (-1: perform all generations).&quot;
	 */
	public static final String PARAMETER_GENERATIONS_WITHOUT_IMPROVAL = "generations_without_improval";

	/** The parameter name for &quot;The selection scheme of this EA.&quot; */
	public static final String PARAMETER_SELECTION_SCHEME = "selection_scheme";

	/**
	 * The parameter name for &quot;The fraction of the current population which should be used as
	 * tournament members (only tournament selection).&quot;
	 */
	public static final String PARAMETER_TOURNAMENT_SIZE = "tournament_size";

	/** The parameter name for &quot;The scaling temperature (only Boltzmann selection).&quot; */
	public static final String PARAMETER_START_TEMPERATURE = "start_temperature";

	/**
	 * The parameter name for &quot;If set to true the selection pressure is increased to maximum
	 * during the complete optimization run (only Boltzmann and tournament selection).&quot;
	 */
	public static final String PARAMETER_DYNAMIC_SELECTION_PRESSURE = "dynamic_selection_pressure";

	/**
	 * The parameter name for &quot;If set to true, the best individual of each generations is
	 * guaranteed to be selected for the next generation (elitist selection).&quot;
	 */
	public static final String PARAMETER_KEEP_BEST_INDIVIDUAL = "keep_best_individual";

	public static final String PARAMETER_SAVE_INTERMEDIATE_WEIGHTS = "save_intermediate_weights";

	public static final String PARAMETER_INTERMEDIATE_WEIGHTS_GENERATIONS = "intermediate_weights_generations";

	public static final String PARAMETER_INTERMEDIATE_WEIGHTS_FILE = "intermediate_weights_file";

	public static final String PARAMETER_USE_EARLY_STOPPING = "use_early_stopping";

	public static final String[] SELECTION_SCHEMES = { "uniform", "cut", "roulette wheel", "stochastic universal sampling",
		"Boltzmann", "rank", "tournament", "non dominated sorting" };

	public static final int UNIFORM_SELECTION = 0;

	public static final int CUT_SELECTION = 1;

	public static final int ROULETTE_WHEEL = 2;

	public static final int STOCHASTIC_UNIVERSAL = 3;

	public static final int BOLTZMANN_SELECTION = 4;

	public static final int RANK_SELECTION = 5;

	public static final int TOURNAMENT_SELECTION = 6;

	public static final int NON_DOMINATED_SORTING_SELECTION = 7;

	/** The size of the population. */
	private int numberOfIndividuals;

	/** Maximum number of generations. */
	private int maxGen;

	/**
	 * Stop criterion: Stop after generationsWithoutImproval generations without an improval of the
	 * fitness.
	 */
	private int generationsWithoutImproval;

	public AbstractGeneticAlgorithm(OperatorDescription description) {
		super(description);
	}

	/**
	 * Returns an operator that performs the mutation. Can be overridden by subclasses.
	 */
	protected abstract PopulationOperator getMutationPopulationOperator(ExampleSet example) throws OperatorException;

	/**
	 * Returns an operator that performs crossover. Can be overridden by subclasses.
	 */
	protected abstract PopulationOperator getCrossoverPopulationOperator(ExampleSet example) throws OperatorException;

	/** Returns an empty list. */
	protected List<PopulationOperator> getPreProcessingPopulationOperators(ExampleSet input) throws OperatorException {
		return new LinkedList<PopulationOperator>();
	}

	/** Returns an empty list. */
	protected List<PopulationOperator> getPostProcessingPopulationOperators(ExampleSet input) throws OperatorException {
		return new LinkedList<PopulationOperator>();
	}

	/** Returns the list with pre eval pop ops. */
	@Override
	public final List<PopulationOperator> getPreEvaluationPopulationOperators(ExampleSet input) throws OperatorException {
		// pre eval ops
		List<PopulationOperator> preOp = new LinkedList<PopulationOperator>();

		// crossover
		PopulationOperator crossover = getCrossoverPopulationOperator(input);
		if (crossover != null) {
			preOp.add(crossover);
		}

		// mutation
		PopulationOperator mutation = getMutationPopulationOperator(input);
		if (mutation != null) {
			preOp.add(mutation);
		}

		// other preevaluation ops
		preOp.addAll(getPreProcessingPopulationOperators(input));
		return preOp;
	}

	/** Returns the list with post eval pop ops. */
	@Override
	public final List<PopulationOperator> getPostEvaluationPopulationOperators(ExampleSet input) throws OperatorException {
		// other post eval ops
		List<PopulationOperator> postOp = new LinkedList<PopulationOperator>();

		// selection
		this.numberOfIndividuals = getParameterAsInt(PARAMETER_POPULATION_SIZE);
		this.maxGen = getParameterAsInt(PARAMETER_MAXIMUM_NUMBER_OF_GENERATIONS);
		this.generationsWithoutImproval = getParameterAsBoolean(PARAMETER_USE_EARLY_STOPPING) ? getParameterAsInt(PARAMETER_GENERATIONS_WITHOUT_IMPROVAL)
				: this.maxGen;
		boolean keepBest = getParameterAsBoolean(PARAMETER_KEEP_BEST_INDIVIDUAL);
		boolean dynamicSelection = getParameterAsBoolean(PARAMETER_DYNAMIC_SELECTION_PRESSURE);
		int selectionScheme = getParameterAsInt(PARAMETER_SELECTION_SCHEME);

		switch (selectionScheme) {
			case UNIFORM_SELECTION:
				postOp.add(new UniformSelection(numberOfIndividuals, keepBest, getRandom()));
				break;
			case CUT_SELECTION:
				postOp.add(new CutSelection(numberOfIndividuals));
				break;
			case ROULETTE_WHEEL:
				postOp.add(new RouletteWheel(numberOfIndividuals, keepBest, getRandom()));
				break;
			case STOCHASTIC_UNIVERSAL:
				postOp.add(new StochasticUniversalSampling(numberOfIndividuals, keepBest, getRandom()));
				break;
			case BOLTZMANN_SELECTION:
				postOp.add(new BoltzmannSelection(numberOfIndividuals, getParameterAsDouble(PARAMETER_START_TEMPERATURE),
						this.maxGen, dynamicSelection, keepBest, getRandom()));
				break;
			case RANK_SELECTION:
				postOp.add(new RankSelection(numberOfIndividuals, keepBest, getRandom()));
				break;
			case TOURNAMENT_SELECTION:
				postOp.add(new TournamentSelection(numberOfIndividuals, getParameterAsDouble(PARAMETER_TOURNAMENT_SIZE),
						this.maxGen, dynamicSelection, keepBest, getRandom()));
				break;
			case NON_DOMINATED_SORTING_SELECTION:
				postOp.add(new NonDominatedSortingSelection(numberOfIndividuals));
				setCheckForMaximum(false); // disables the check for the
				// maximal fitness
				break;
			default:
				break;
		}

		if (getParameterAsBoolean(PARAMETER_SAVE_INTERMEDIATE_WEIGHTS)) {
			File file = getParameterAsFile(PARAMETER_INTERMEDIATE_WEIGHTS_FILE, true);
			if (file != null) {
				postOp.add(new SaveIntermediateWeights(this, getParameterAsInt(PARAMETER_INTERMEDIATE_WEIGHTS_GENERATIONS),
						Tools.getRegularAttributeNames(input)));
			} else {
				logWarning("The parameter '" + PARAMETER_SAVE_INTERMEDIATE_WEIGHTS
						+ "' was set but no file was specified: ignoring.");
			}
		}

		postOp.addAll(getPostProcessingPopulationOperators(input));
		return postOp;
	}

	/**
	 * Returns true if generation is >= maximum_number_of_generations or after
	 * generations_without_improval generations without improval.
	 */
	@Override
	public boolean solutionGoodEnough(Population pop) {
		return pop.getGeneration() >= maxGen || pop.getGenerationsWithoutImproval() >= generationsWithoutImproval;
	}

	@Override
	public List<ParameterType> getParameterTypes() {
		List<ParameterType> types = new LinkedList<ParameterType>();

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
		types.add(type);

		types.addAll(super.getParameterTypes());

		types.add(new ParameterTypeCategory(PARAMETER_SELECTION_SCHEME, "The selection scheme of this EA.",
				SELECTION_SCHEMES, TOURNAMENT_SELECTION));
		type = new ParameterTypeDouble(PARAMETER_TOURNAMENT_SIZE,
				"The fraction of the current population which should be used as tournament members.", 0.0d, 1.0d, 0.25d);
		type.registerDependencyCondition(new EqualTypeCondition(this, PARAMETER_SELECTION_SCHEME, SELECTION_SCHEMES, false,
				TOURNAMENT_SELECTION));
		types.add(type);
		type = new ParameterTypeDouble(PARAMETER_START_TEMPERATURE, "The scaling temperature .", 0.0d,
				Double.POSITIVE_INFINITY, 1.0d);
		type.registerDependencyCondition(new EqualTypeCondition(this, PARAMETER_SELECTION_SCHEME, SELECTION_SCHEMES, false,
				BOLTZMANN_SELECTION));
		types.add(type);
		type = new ParameterTypeBoolean(PARAMETER_DYNAMIC_SELECTION_PRESSURE,
				"If set to true the selection pressure is increased to maximum during the complete optimization run.", true);
		type.registerDependencyCondition(new EqualTypeCondition(this, PARAMETER_SELECTION_SCHEME, SELECTION_SCHEMES, false,
				TOURNAMENT_SELECTION, BOLTZMANN_SELECTION));
		types.add(type);
		types.add(new ParameterTypeBoolean(
				PARAMETER_KEEP_BEST_INDIVIDUAL,
				"If set to true, the best individual of each generations is guaranteed to be selected for the next generation (elitist selection).",
				false));

		types.add(new ParameterTypeBoolean(PARAMETER_SAVE_INTERMEDIATE_WEIGHTS,
				"Determines if the intermediate best results should be saved.", false));

		type = new ParameterTypeInt(
				PARAMETER_INTERMEDIATE_WEIGHTS_GENERATIONS,
				"Determines if the intermediate best results should be saved. Will be performed every k generations for a specified value of k.",
				1, Integer.MAX_VALUE, 10);
		type.registerDependencyCondition(new BooleanParameterCondition(this, PARAMETER_SAVE_INTERMEDIATE_WEIGHTS, false,
				true));
		types.add(type);

		type = new ParameterTypeFile(PARAMETER_INTERMEDIATE_WEIGHTS_FILE,
				"The file into which the intermediate weights will be saved.", "wgt", true);
		type.registerDependencyCondition(new BooleanParameterCondition(this, PARAMETER_SAVE_INTERMEDIATE_WEIGHTS, true, true));
		types.add(type);

		return types;
	}

	@Override
	protected int getMaximumGenerations() throws UndefinedParameterError {
		return maxGen;
	}
}
