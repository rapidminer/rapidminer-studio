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
package com.rapidminer.operator.features.construction;

import java.util.ArrayList;
import java.util.LinkedList;
import java.util.List;

import com.rapidminer.example.Attribute;
import com.rapidminer.example.ExampleSet;
import com.rapidminer.example.set.AttributeWeightedExampleSet;
import com.rapidminer.generator.BasicArithmeticOperationGenerator;
import com.rapidminer.generator.FeatureGenerator;
import com.rapidminer.generator.ReciprocalValueGenerator;
import com.rapidminer.operator.OperatorDescription;
import com.rapidminer.operator.OperatorException;
import com.rapidminer.operator.OperatorProgress;
import com.rapidminer.operator.ProcessStoppedException;
import com.rapidminer.operator.features.selection.GeneticAlgorithm;
import com.rapidminer.operator.features.selection.SelectionCrossover;
import com.rapidminer.parameter.ParameterType;
import com.rapidminer.parameter.ParameterTypeBoolean;
import com.rapidminer.parameter.ParameterTypeCategory;
import com.rapidminer.parameter.ParameterTypeDouble;
import com.rapidminer.parameter.ParameterTypeInt;
import com.rapidminer.parameter.UndefinedParameterError;
import com.rapidminer.parameter.conditions.BooleanParameterCondition;


/**
 * In contrast to its superclass {@link GeneticAlgorithm}, the {@link GeneratingGeneticAlgorithm}
 * generates new attributes and thus can change the length of an individual. Therfore specialized
 * mutation and crossover operators are being applied. Generators are chosen at random from a list
 * of generators specified by boolean parameters. <br/>
 *
 * Since this operator does not contain algorithms to extract features from value series, it is
 * restricted to example sets with only single attributes. For automatic feature extraction from
 * values series the value series plugin for RapidMiner written by Ingo Mierswa should be used. It
 * is available at <a href="http://rapidminer.com">http://rapidminer.com</a>
 *
 * @rapidminer.reference Ritthoff/etal/2001a
 *
 * @author Ingo Mierswa
 */
public abstract class AbstractGeneratingGeneticAlgorithm extends ExampleSetBasedFeatureOperator {

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

	public static final String PARAMETER_P_INITIALIZE = "p_initialize";

	public static final String PARAMETER_P_CROSSOVER = "p_crossover";

	public static final String PARAMETER_CROSSOVER_TYPE = "crossover_type";

	public static final String PARAMETER_USE_PLUS = "use_plus";

	public static final String PARAMETER_USE_DIFF = "use_diff";

	public static final String PARAMETER_USE_MULT = "use_mult";

	public static final String PARAMETER_USE_DIV = "use_div";

	public static final String PARAMETER_RECIPROCAL_VALUE = "reciprocal_value";

	/** The size of the population. */
	private int numberOfIndividuals;

	/** Maximum number of generations. */
	private int maxGen;

	/**
	 * Stop criterion: Stop after generationsWithoutImproval generations without an improval of the
	 * fitness.
	 */
	private int generationsWithoutImproval;

	public AbstractGeneratingGeneticAlgorithm(OperatorDescription description) {
		super(description);
	}

	/**
	 * Returns a specialized generating mutation, e.g. a <code>AttributeGenerator</code>.
	 */
	protected abstract ExampleSetBasedPopulationOperator getGeneratingPopulationOperator(ExampleSet exampleSet)
			throws OperatorException;

	/**
	 * Returns an operator that performs the mutation. Can be overridden by subclasses.
	 */
	protected abstract ExampleSetBasedPopulationOperator getMutationPopulationOperator(ExampleSet example)
			throws OperatorException;

	/** Returns an empty list. */
	protected List<ExampleSetBasedPopulationOperator> getPostProcessingPopulationOperators(ExampleSet input)
			throws OperatorException {
		return new LinkedList<ExampleSetBasedPopulationOperator>();
	}

	/** Returns the list with pre eval pop ops. */
	@Override
	public final List<ExampleSetBasedPopulationOperator> getPreEvaluationPopulationOperators(ExampleSet input)
			throws OperatorException {
		// pre eval ops
		List<ExampleSetBasedPopulationOperator> preOp = new LinkedList<ExampleSetBasedPopulationOperator>();

		// crossover
		ExampleSetBasedPopulationOperator crossover = getCrossoverPopulationOperator(input);
		if (crossover != null) {
			preOp.add(crossover);
		}

		// mutation
		ExampleSetBasedPopulationOperator mutation = getMutationPopulationOperator(input);
		if (mutation != null) {
			preOp.add(mutation);
		}

		// other preevaluation ops
		preOp.addAll(getPreProcessingPopulationOperators(input));
		return preOp;
	}

	/** Returns the list with post eval pop ops. */
	@Override
	public final List<ExampleSetBasedPopulationOperator> getPostEvaluationPopulationOperators(ExampleSet input)
			throws OperatorException {
		// other post eval ops
		List<ExampleSetBasedPopulationOperator> postOp = new LinkedList<ExampleSetBasedPopulationOperator>();

		// selection
		this.numberOfIndividuals = getParameterAsInt(PARAMETER_POPULATION_SIZE);
		this.maxGen = getParameterAsInt(PARAMETER_MAXIMUM_NUMBER_OF_GENERATIONS);
		this.generationsWithoutImproval = getParameterAsBoolean(PARAMETER_USE_EARLY_STOPPING)
				? getParameterAsInt(PARAMETER_GENERATIONS_WITHOUT_IMPROVAL) : maxGen;
		boolean keepBest = getParameterAsBoolean(PARAMETER_KEEP_BEST_INDIVIDUAL);
		boolean dynamicSelection = getParameterAsBoolean(PARAMETER_DYNAMIC_SELECTION_PRESSURE);
		postOp.add(new ExampleSetBasedTournamentSelection(numberOfIndividuals,
				getParameterAsDouble(PARAMETER_TOURNAMENT_SIZE), this.maxGen, dynamicSelection, keepBest, getRandom()));

		postOp.addAll(getPostProcessingPopulationOperators(input));
		return postOp;
	}

	/**
	 * Returns true if generation is >= maximum_number_of_generations or after
	 * generations_without_improval generations without improval.
	 */
	@Override
	public boolean solutionGoodEnough(ExampleSetBasedPopulation pop) {
		return pop.getGeneration() >= maxGen || pop.getGenerationsWithoutImproval() >= generationsWithoutImproval;
	}

	/**
	 * Sets up a population of given size and creates ExampleSets with randomly selected attributes
	 * (the probability to be switched on is controlled by pInitialize).
	 */
	@Override
	public ExampleSetBasedPopulation createInitialPopulation(ExampleSet es) throws UndefinedParameterError {
		ExampleSetBasedPopulation initP = new ExampleSetBasedPopulation();
		int populationSize = getParameterAsInt(PARAMETER_POPULATION_SIZE);
		double p_initialize = getParameterAsDouble(PARAMETER_P_INITIALIZE);
		while (initP.getNumberOfIndividuals() < populationSize) {
			AttributeWeightedExampleSet nes = new AttributeWeightedExampleSet(es, null);
			for (Attribute attribute : nes.getAttributes()) {
				if (getRandom().nextDouble() > p_initialize) {
					nes.flipAttributeUsed(attribute);
				}
			}
			if (nes.getNumberOfUsedAttributes() > 0) {
				initP.add(new ExampleSetBasedIndividual(nes));
			}
		}
		return initP;
	}

	protected List<ExampleSetBasedPopulationOperator> getPreProcessingPopulationOperators(ExampleSet exampleSet)
			throws OperatorException {
		List<ExampleSetBasedPopulationOperator> popOps = new LinkedList<ExampleSetBasedPopulationOperator>();
		ExampleSetBasedPopulationOperator generator = getGeneratingPopulationOperator(exampleSet);
		if (generator != null) {
			popOps.add(generator);
		}
		return popOps;
	}

	/** Returns an <code>UnbalancedCrossover</code>. */
	protected ExampleSetBasedPopulationOperator getCrossoverPopulationOperator(ExampleSet exampleSet)
			throws UndefinedParameterError {
		double pCrossover = getParameterAsDouble(PARAMETER_P_CROSSOVER);
		int crossoverType = getParameterAsInt(PARAMETER_CROSSOVER_TYPE);
		return new UnbalancedCrossover(crossoverType, pCrossover, getRandom());
	}

	/** Returns a list with all generator which should be used. */
	public List<FeatureGenerator> getGenerators() {
		List<FeatureGenerator> generators = new ArrayList<FeatureGenerator>();
		if (getParameterAsBoolean(PARAMETER_USE_PLUS)) {
			generators.add(new BasicArithmeticOperationGenerator(BasicArithmeticOperationGenerator.SUM));
		}
		if (getParameterAsBoolean(PARAMETER_USE_DIFF)) {
			generators.add(new BasicArithmeticOperationGenerator(BasicArithmeticOperationGenerator.DIFFERENCE));
		}
		if (getParameterAsBoolean(PARAMETER_USE_MULT)) {
			generators.add(new BasicArithmeticOperationGenerator(BasicArithmeticOperationGenerator.PRODUCT));
		}
		if (getParameterAsBoolean(PARAMETER_USE_DIV)) {
			generators.add(new BasicArithmeticOperationGenerator(BasicArithmeticOperationGenerator.QUOTIENT));
		}
		if (getParameterAsBoolean(PARAMETER_RECIPROCAL_VALUE)) {
			generators.add(new ReciprocalValueGenerator());
		}
		return generators;
	}

	@Override
	public List<ParameterType> getParameterTypes() {
		List<ParameterType> types = super.getParameterTypes();
		ParameterType type = new ParameterTypeInt(PARAMETER_POPULATION_SIZE, "Number of individuals per generation.", 1,
				Integer.MAX_VALUE, 5);
		type.setExpert(false);
		types.add(type);
		type = new ParameterTypeInt(PARAMETER_MAXIMUM_NUMBER_OF_GENERATIONS,
				"Number of generations after which to terminate the algorithm.", 1, Integer.MAX_VALUE, 30);
		type.setExpert(false);
		types.add(type);

		type = new ParameterTypeBoolean(PARAMETER_USE_PLUS, "Generate sums.", true);
		type.setExpert(false);
		types.add(type);
		type = new ParameterTypeBoolean(PARAMETER_USE_DIFF, "Generate differences.", false);
		type.setExpert(false);
		types.add(type);
		type = new ParameterTypeBoolean(PARAMETER_USE_MULT, "Generate products.", true);
		type.setExpert(false);
		types.add(type);
		type = new ParameterTypeBoolean(PARAMETER_USE_DIV, "Generate quotients.", false);
		type.setExpert(false);
		types.add(type);
		type = new ParameterTypeBoolean(PARAMETER_RECIPROCAL_VALUE, "Generate reciprocal values.", true);
		type.setExpert(false);
		types.add(type);

		types.add(new ParameterTypeBoolean(PARAMETER_USE_EARLY_STOPPING,
				"Enables early stopping. If unchecked, always the maximum number of generations is performed.", false));
		type = new ParameterTypeInt(PARAMETER_GENERATIONS_WITHOUT_IMPROVAL,
				"Stop criterion: Stop after n generations without improval of the performance.", 1, Integer.MAX_VALUE, 2);
		type.registerDependencyCondition(new BooleanParameterCondition(this, PARAMETER_USE_EARLY_STOPPING, true, true));
		types.add(type);

		types.add(new ParameterTypeDouble(PARAMETER_TOURNAMENT_SIZE,
				"The fraction of the current population which should be used as tournament members (only tournament selection).",
				0.0d, 1.0d, 0.25d));
		types.add(new ParameterTypeDouble(PARAMETER_START_TEMPERATURE, "The scaling temperature (only Boltzmann selection).",
				0.0d, Double.POSITIVE_INFINITY, 1.0d));
		types.add(new ParameterTypeBoolean(PARAMETER_DYNAMIC_SELECTION_PRESSURE,
				"If set to true the selection pressure is increased to maximum during the complete optimization run (only Boltzmann and tournament selection).",
				true));
		types.add(new ParameterTypeBoolean(PARAMETER_KEEP_BEST_INDIVIDUAL,
				"If set to true, the best individual of each generations is guaranteed to be selected for the next generation (elitist selection).",
				false));

		types.add(new ParameterTypeDouble(PARAMETER_P_INITIALIZE, "Initial probability for an attribute to be switched on.",
				0, 1, 0.5));
		type = new ParameterTypeDouble(PARAMETER_P_CROSSOVER, "Probability for an individual to be selected for crossover.",
				0, 1, 0.5);
		types.add(type);
		types.add(new ParameterTypeCategory(PARAMETER_CROSSOVER_TYPE, "Type of the crossover.",
				SelectionCrossover.CROSSOVER_TYPES, SelectionCrossover.UNIFORM));
		return types;
	}

	@Override
	protected int getMaxGenerations() {
		try {
			return getParameterAsInt(AbstractGeneratingGeneticAlgorithm.PARAMETER_MAXIMUM_NUMBER_OF_GENERATIONS);
		} catch (UndefinedParameterError e) {
			return OperatorProgress.NO_PROGRESS;
		}
	}

	@Override
	protected void applyLoopOperations() throws ProcessStoppedException {
		super.applyLoopOperations();
		getProgress().step();
	}

}
