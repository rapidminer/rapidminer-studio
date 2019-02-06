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
package com.rapidminer.tools.math.optimization.ec.es;

import java.util.Collection;
import java.util.Iterator;
import java.util.LinkedList;
import java.util.List;
import java.util.concurrent.atomic.AtomicInteger;

import com.rapidminer.datatable.DataTable;
import com.rapidminer.datatable.SimpleDataTable;
import com.rapidminer.datatable.SimpleDataTableRow;
import com.rapidminer.gui.plotter.SimplePlotterDialog;
import com.rapidminer.operator.Operator;
import com.rapidminer.operator.OperatorException;
import com.rapidminer.operator.meta.ParameterOptimizationOperator;
import com.rapidminer.operator.performance.PerformanceVector;
import com.rapidminer.parameter.ParameterType;
import com.rapidminer.parameter.ParameterTypeBoolean;
import com.rapidminer.parameter.ParameterTypeCategory;
import com.rapidminer.parameter.ParameterTypeDouble;
import com.rapidminer.parameter.ParameterTypeInt;
import com.rapidminer.parameter.conditions.BooleanParameterCondition;
import com.rapidminer.parameter.conditions.EqualTypeCondition;
import com.rapidminer.tools.LoggingHandler;
import com.rapidminer.tools.RandomGenerator;
import com.rapidminer.tools.math.optimization.Optimization;


/**
 * Evolutionary Strategy approach for all real-valued optimization tasks.
 *
 * @author Ingo Mierswa
 */
public abstract class ESOptimization implements Optimization {

	public static final String PARAMETER_MAX_GENERATIONS = "max_generations";
	public static final String PARAMETER_USE_EARLY_STOPPING = "use_early_stopping";
	public static final String PARAMETER_GENERATIONS_WITHOUT_IMPROVAL = "generations_without_improval";
	public static final String PARAMETER_POPULATION_SIZE = "population_size";
	public static final String PARAMETER_TOURNAMENT_FRACTION = "tournament_fraction";
	public static final String PARAMETER_KEEP_BEST = "keep_best";
	public static final String PARAMETER_MUTATION_TYPE = "mutation_type";
	public static final String PARAMETER_SELECTION_TYPE = "selection_type";
	public static final String PARAMETER_CROSSOVER_PROB = "crossover_prob";
	public static final String PARAMETER_SHOW_CONVERGENCE_PLOT = "show_convergence_plot";
	public static final String PARAMETER_SPECIFIY_POPULATION_SIZE = "specify_population_size";

	/** The names of all available selection schemes. */
	public static final String[] SELECTION_TYPES = { "uniform", "cut", "roulette wheel", "stochastic universal sampling",
			"Boltzmann", "rank", "tournament", "non dominated sorting" };

	/** Indicates a uniform sampling selection scheme. */
	public static final int UNIFORM_SELECTION = 0;

	/** Indicates a cut selection scheme. */
	public static final int CUT_SELECTION = 1;

	/** Indicates a roulette wheel selection scheme. */
	public static final int ROULETTE_WHEEL = 2;

	/** Indicates a stochastic universal sampling selection scheme. */
	public static final int STOCHASTIC_UNIVERSAL = 3;

	/** Indicates a Boltzmann selection scheme. */
	public static final int BOLTZMANN_SELECTION = 4;

	/** Indicates a rank based selection scheme. */
	public static final int RANK_SELECTION = 5;

	/** Indicates a tournament selection scheme. */
	public static final int TOURNAMENT_SELECTION = 6;

	/** Indicates a multi-objective selection scheme (NSGA II). */
	public static final int NON_DOMINATED_SORTING_SELECTION = 7;

	/** The names of the mutation types. */
	public static final String[] MUTATION_TYPES = { "none", "gaussian_mutation", "switching_mutation", "sparsity_mutation" };

	/** Indicates no mutation. */
	public static final int NO_MUTATION = 0;

	/** Indicates a gaussian mutation. */
	public static final int GAUSSIAN_MUTATION = 1;

	/** Indicates a switching mutation. */
	public static final int SWITCHING_MUTATION = 2;

	/** Indicates a hybrid between switching mutation and Gaussian mutation. */
	public static final int SPARSITY_MUTATION = 3;

	/** The names of the initialization types. */
	public static final String[] POPULATION_INIT_TYPES = { "random", "min", "max" };

	/** Indicates that the start population should be randomly initialized. */
	public static final int INIT_TYPE_RANDOM = 0;

	/**
	 * Indicates that the start population should be initialized with the minimum value.
	 */
	public static final int INIT_TYPE_MIN = 1;

	/**
	 * Indicates that the start population should be initialized with the maximum value.
	 */
	public static final int INIT_TYPE_MAX = 2;

	/** Indicates that the start population should be initialized with one. */
	public static final int INIT_TYPE_ONE = 3;

	/** Indicates that the start population should be initialized with zero. */
	public static final int INIT_TYPE_ZERO = 4;

	/**
	 * For extending this class and providing a default start population. Override {@link #createDefaultStartPopulation()}.
	 *
	 * @since 9.0.0
	 */
	public static final int INIT_TYPE_DEFAULT = 42;

	/** This parameter indicates the minimum value for all genes. */
	private double[] min;

	/** This parameter indicates the maximum value for all genes. */
	private double[] max;

	/** The value types, either DOUBLE (default) or INT. */
	private OptimizationValueType[] valueTypes;

	/** The number of individuals. */
	private int populationSize;

	/** The dimension of each individual. */
	private int individualSize;

	/** The maximum number of generations. */
	private int maxGenerations;

	/** The maximum numbers of generations without improvement. */
	private int generationsWithoutImprovement;

	/** The type of start population initialization. */
	private int initType = INIT_TYPE_RANDOM;

	/** The type of the mutation. */
	// private int mutationType = GAUSSIAN_MUTATION;

	/** The population plotter (if enabled). */
	private PopulationPlotter populationPlotter = null;

	/** The mutation operator. */
	private Mutation mutation;

	/** The current population. */
	private Population population;

	/** Population operators. */
	private Collection<PopulationOperator> popOps;

	/** Indicates if a convergence plot should be drawn. */
	private boolean showConvergencePlot = false;

	/**
	 * This field counts the total number of evaluations during optimization.
	 */
	private AtomicInteger totalEvalCounter = new AtomicInteger();

	/**
	 * This field counts the number of actually calculated evaluations (unchanged individuals do not
	 * have to be re-evaluated).
	 */
	private AtomicInteger currentEvalCounter = new AtomicInteger();

	/** The random number generator. */
	private RandomGenerator random;

	private LoggingHandler logging;
	private Individual currentBest;
	private Operator executingOperator = null;

	/**
	 * Creates a new evolutionary SVM optimization which also checks for Stop if the
	 * executingOperator is set.
	 */
	@Deprecated
	public ESOptimization(double[] minValues, double[] maxValues, int populationSize, int individualSize, int initType,        // population
			// paras
			int maxGenerations, int generationsWithoutImprovement,        // GA paras
			int selectionType, double tournamentFraction, boolean keepBest,        // selection
			// paras
			int mutationType,        // type of mutation
			double defaultSigma, double crossoverProb, boolean showConvergencePlot, boolean showPopulationPlot,
			RandomGenerator random, LoggingHandler logging) {

		this(minValues, maxValues, populationSize, individualSize, initType, maxGenerations, generationsWithoutImprovement,
				selectionType, tournamentFraction, keepBest, mutationType, defaultSigma, crossoverProb, showConvergencePlot,
				showPopulationPlot, random, logging, null);
	}

	/** Creates a new evolutionary SVM optimization. */
	@Deprecated
	public ESOptimization(double minValue, double maxValue, int populationSize, int individualSize, int initType,        // population
			// paras
			int maxGenerations, int generationsWithoutImprovement,        // GA paras
			int selectionType, double tournamentFraction, boolean keepBest,        // selection
			// paras
			int mutationType,        // type of mutation
			double crossoverProb, boolean showConvergencePlot, boolean showPopulationPlot, RandomGenerator random,
			LoggingHandler logging) {
		this(createBoundArray(minValue, individualSize), createBoundArray(maxValue, individualSize), populationSize,
				individualSize, initType, maxGenerations, generationsWithoutImprovement, selectionType, tournamentFraction,
				keepBest, mutationType, Double.NaN, crossoverProb, showConvergencePlot, showPopulationPlot, random, logging,
				null);
	}

	/**
	 * Creates a new evolutionary SVM optimization which also checks for Stop if the
	 * executingOperator is set.
	 *
	 * @param executingOperator
	 *            If this parameter is null, no exception will be thrown.
	 */
	public ESOptimization(double minValue, double maxValue, int populationSize, int individualSize, int initType,        // population
			// paras
			int maxGenerations, int generationsWithoutImprovement,        // GA paras
			int selectionType, double tournamentFraction, boolean keepBest,        // selection
			// paras
			int mutationType,        // type of mutation
			double crossoverProb, boolean showConvergencePlot, boolean showPopulationPlot, RandomGenerator random,
			LoggingHandler logging, Operator executingOperator) {
		this(createBoundArray(minValue, individualSize), createBoundArray(maxValue, individualSize), populationSize,
				individualSize, initType, maxGenerations, generationsWithoutImprovement, selectionType, tournamentFraction,
				keepBest, mutationType, Double.NaN, crossoverProb, showConvergencePlot, showPopulationPlot, random, logging,
				executingOperator);
	}

	/**
	 * Creates a new evolutionary SVM optimization.
	 *
	 * @param executingOperator
	 *            If this parameter is null, no exception will be thrown.
	 *
	 */
	public ESOptimization(double[] minValues, double[] maxValues, int populationSize, int individualSize, int initType,        // population
			// paras
			int maxGenerations, int generationsWithoutImprovement,        // GA paras
			int selectionType, double tournamentFraction, boolean keepBest,        // selection
			// paras
			int mutationType,        // type of mutation
			double defaultSigma, double crossoverProb, boolean showConvergencePlot, boolean showPopulationPlot,
			RandomGenerator random, LoggingHandler logging, Operator executingOperator) {
		this.logging = logging;
		this.random = random;
		this.showConvergencePlot = showConvergencePlot;
		this.populationSize = populationSize;
		this.individualSize = individualSize;
		this.min = minValues;
		this.max = maxValues;
		this.valueTypes = new OptimizationValueType[individualSize];
		this.executingOperator = executingOperator;
		for (int i = 0; i < this.valueTypes.length; i++) {
			this.valueTypes[i] = OptimizationValueType.VALUE_TYPE_DOUBLE;
		}
		this.initType = initType;
		this.maxGenerations = maxGenerations;
		this.generationsWithoutImprovement = generationsWithoutImprovement < 1 ? this.maxGenerations
				: generationsWithoutImprovement;
				// this.mutationType = mutationType;

		// population operators
		popOps = new LinkedList<>();
		switch (selectionType) {
			case UNIFORM_SELECTION:
				popOps.add(new UniformSelection(populationSize, keepBest, random));
				break;
			case CUT_SELECTION:
				popOps.add(new CutSelection(populationSize));
				break;
			case ROULETTE_WHEEL:
				popOps.add(new RouletteWheel(populationSize, keepBest, random));
				break;
			case STOCHASTIC_UNIVERSAL:
				popOps.add(new StochasticUniversalSampling(populationSize, keepBest, random));
				break;
			case BOLTZMANN_SELECTION:
				popOps.add(new BoltzmannSelection(populationSize, 1.0d, this.maxGenerations, true, keepBest, random));
				break;
			case RANK_SELECTION:
				popOps.add(new RankSelection(populationSize, keepBest, random));
				break;
			case TOURNAMENT_SELECTION:
				popOps.add(new TournamentSelection(populationSize, tournamentFraction, keepBest, random));
				break;
			case NON_DOMINATED_SORTING_SELECTION:
				popOps.add(new NonDominatedSortingSelection(populationSize));
				if (showPopulationPlot) {
					this.populationPlotter = new PopulationPlotter();
					popOps.add(this.populationPlotter);
				}
				break;
		}
		popOps.add(new Crossover(crossoverProb, random));
		switch (mutationType) {
			case GAUSSIAN_MUTATION:
				double[] sigma = new double[this.min.length];
				if (!Double.isNaN(defaultSigma)) {
					for (int s = 0; s < sigma.length; s++) {
						sigma[s] = defaultSigma;
					}
				} else {
					for (int s = 0; s < sigma.length; s++) {
						sigma[s] = (this.max[s] - this.min[s]) / 100.0d;
					}
				}
				GaussianMutation gm = new GaussianMutation(sigma, this.min, this.max, this.valueTypes, random);
				popOps.add(gm);
				popOps.add(new VarianceAdaption(gm, individualSize, this.logging));
				this.mutation = gm;
				break;
			case SWITCHING_MUTATION:
				this.mutation = new SwitchingMutation(1.0d / individualSize, this.min, this.max, this.valueTypes, random);
				popOps.add(this.mutation);
				break;
			case SPARSITY_MUTATION:
				this.mutation = new SparsityMutation(1.0d / individualSize, this.min, this.max, this.valueTypes, random);
				popOps.add(this.mutation);
				break;
			default:
				break; // no mutation at all
		}
	}

	private static double[] createBoundArray(double bound, int size) {
		double[] result = new double[size];
		for (int i = 0; i < result.length; i++) {
			result[i] = bound;
		}
		return result;
	}

	/**
	 * Subclasses must implement this method to calculate the fitness of the given individual.
	 * Please note that null might be returned for non-valid individuals. The fitness will be
	 * maximized.
	 */
	public abstract PerformanceVector evaluateIndividual(Individual individual) throws OperatorException;

	/**
	 * This method is invoked after each evaluation. The default implementation does nothing but
	 * subclasses might implement this method to support online plotting or logging.
	 */
	public void nextIteration() throws OperatorException {}

	public double getMin(int index) {
		return min[index];
	}

	public double getMax(int index) {
		return max[index];
	}

	public void setMin(int index, double v) {
		this.min[index] = v;
		/*
		 * if (mutationType == GAUSSIAN_MUTATION) recalculateSigma((GaussianMutation)this.mutation,
		 * this.individualSize);
		 */
	}

	public void setMax(int index, double v) {
		this.max[index] = v;
		/*
		 * if (mutationType == GAUSSIAN_MUTATION) recalculateSigma((GaussianMutation)this.mutation,
		 * this.individualSize);
		 */
	}

	/*
	 * protected void recalculateSigma(GaussianMutation mutation, int individualSize) { double[]
	 * sigma = new double[individualSize]; for (int s = 0; s < sigma.length; s++) sigma[s] =
	 * Math.abs(this.max[s] - this.min[s]) / 100.0d; mutation.setSigma(sigma); }
	 */

	public OptimizationValueType getValueType(int index) {
		return this.valueTypes[index];
	}

	public void setValueType(int index, OptimizationValueType type) {
		this.valueTypes[index] = type;
		mutation.setValueType(index, type);
	}

	// ================================================================================
	// O P T I M I Z A T I O N
	// ================================================================================

	/**
	 * Starts the optimization.
	 */
	@Override
	public void optimize() throws OperatorException {
		this.totalEvalCounter = new AtomicInteger();
		this.currentEvalCounter = new AtomicInteger();
		boolean executingOperatorExists = executingOperator != null;

		switch (initType) {
			case INIT_TYPE_RANDOM:
				this.population = createRandomStartPopulation();
				break;
			case INIT_TYPE_MIN:
				this.population = createMinStartPopulation();
				break;
			case INIT_TYPE_MAX:
				this.population = createMaxStartPopulation();
				break;
			case INIT_TYPE_ONE:
				this.population = createFixedStartPopulation(1.0d);
				break;
			case INIT_TYPE_ZERO:
				this.population = createFixedStartPopulation(0.0d);
				break;
			case INIT_TYPE_DEFAULT:
				this.population = createDefaultStartPopulation();
				break;
			default:
				break; // this cannot happen
		}

		evaluate(population);
		DataTable dataTable = null;
		SimplePlotterDialog plotter = null;
		if (showConvergencePlot) {
			dataTable = new SimpleDataTable("Fitness vs. Generations",
					new String[] { "Generations", "Best Fitness", "Current Fitness" });
			plotter = new SimplePlotterDialog(dataTable, false);
			plotter.setXAxis(0);
			plotter.plotColumn(1, true);
			plotter.plotColumn(2, true);
			plotter.setVisible(true);
			dataTable.add(new SimpleDataTableRow(
					new double[] { 0.0d, population.getBestEver().getFitness().getMainCriterion().getFitness(),
							population.getCurrentBest().getFitness().getMainCriterion().getFitness() }));
		}

		if (executingOperatorExists) {
			executingOperator.getProgress().setTotal(maxGenerations);
		}

		while (true) {

			if (population.getGeneration() >= maxGenerations) {
				logging.log("ES finished: maximum number of iterations reached.");
				break;
			}
			if (population.getGenerationsWithoutImprovement() > generationsWithoutImprovement) {
				logging.log("ES converged in generation " + population.getGeneration() + ": No improvement in last "
						+ generationsWithoutImprovement + " generations.");
				break;
			}
			Iterator<PopulationOperator> i = popOps.iterator();

			while (i.hasNext()) {

				i.next().operate(population);
			}
			evaluate(population);
			if (showConvergencePlot) {
				dataTable.add(new SimpleDataTableRow(new double[] { population.getGeneration(),
						population.getBestEver().getFitness().getMainCriterion().getFitness(),
						population.getCurrentBest().getFitness().getMainCriterion().getFitness() }));
			}

			if (executingOperatorExists) {
				executingOperator.getProgress().setCompleted(population.getGeneration());
			}

			population.nextGeneration();
			nextIteration();
		}

		if (showConvergencePlot) {
			plotter.dispose();
		}

		if (populationPlotter != null) {
			this.populationPlotter.setCreateOtherPlottersEnabled(true);
		}

		logging.log("ES Evaluations: " + currentEvalCounter + " / " + totalEvalCounter);
	}

	/**
	 * Create the start population when init type is set to {@link #INIT_TYPE_DEFAULT}. If not overriden, creates a random
	 * start population via {@link #createRandomStartPopulation()}.
	 *
	 * @return the population, must not return {@code null}
	 * @since 9.0.0
	 */
	protected Population createDefaultStartPopulation() {
		return createRandomStartPopulation();
	}

	/** Evaluates the individuals of the given population. */
	protected void evaluate(Population population) throws OperatorException {
		currentBest = null;
		evaluateAll(population);
		if (currentBest != null) {
			population.setCurrentBest(currentBest);
			Individual bestEver = population.getBestEver();
			if (bestEver == null || currentBest.getFitness().getMainCriterion().getFitness() > bestEver.getFitness()
					.getMainCriterion().getFitness()) {
				Individual bestEverClone = (Individual) currentBest.clone();
				bestEverClone.setFitness(currentBest.getFitness());
				population.setBestEver(bestEverClone);
			}
		}
	}

	protected void evaluateAll(Population population) throws OperatorException {
		for (int i = population.getNumberOfIndividuals() - 1; i >= 0; i--) {
			Individual current = population.get(i);
			if (current.getFitness() == null) {
				evaluate(current, population);
			}
			totalEvalCounter.incrementAndGet();
		}
	}

	protected void evaluate(Individual current, Population population) throws OperatorException {
		PerformanceVector fitness = evaluateIndividual(current);
		if (fitness != null) {
			current.setFitness(fitness);
			if (currentBest == null
					|| fitness.getMainCriterion().getFitness() > currentBest.getFitness().getMainCriterion().getFitness()) {
				currentBest = (Individual) current.clone();
				currentBest.setFitness(current.getFitness());
				// check if current best is the pest individual ever. If so, the individual is the
				// result of the ESOptimization.
				Individual bestEver = population.getBestEver();
				if (executingOperator != null && executingOperator instanceof ParameterOptimizationOperator
						&& (bestEver == null || fitness.getMainCriterion().getFitness() > bestEver.getFitness()
								.getMainCriterion().getFitness())) {
					/*
					 * pass results through each time the fitness improved (only the last call of
					 * passResultsThrough() matters, so it will be the best run.)
					 */
					ParameterOptimizationOperator op = (ParameterOptimizationOperator) executingOperator;
					op.passResultsThrough();
				}
			}
		} else {
			population.remove(current);
		}
		currentEvalCounter.incrementAndGet();
	}

	/** Returns the current generation. */
	@Override
	public int getGeneration() {
		return population.getGeneration();
	}

	/** Returns the best fitness in the current generation. */
	@Override
	public double getBestFitnessInGeneration() {
		Individual individual = population.getCurrentBest();
		if (individual != null) {
			return individual.getFitnessValues()[0];
		} else {
			return Double.NaN;
		}
	}

	/** Returns the best fitness ever. */
	@Override
	public double getBestFitnessEver() {
		Individual individual = population.getBestEver();
		if (individual != null) {
			return individual.getFitnessValues()[0];
		} else {
			return Double.NaN;
		}
	}

	/** Returns the best performance vector ever. */
	@Override
	public PerformanceVector getBestPerformanceEver() {
		Individual individual = population.getBestEver();
		if (individual != null) {
			return individual.getFitness();
		} else {
			return null;
		}
	}

	public Population getPopulation() {
		return this.population;
	}

	/**
	 * Returns the best values ever. Use this method after optimization to get the best result.
	 * Might returns null if the optimization did not work.
	 */
	@Override
	public double[] getBestValuesEver() {
		Individual individual = population.getBestEver();
		if (individual != null) {
			return individual.getValues();
		} else {
			return null;
		}
	}

	// ================================================================================
	// S T A R T P O P U L A T I O N S
	// ================================================================================

	/** Randomly creates the initial population. */
	private Population createRandomStartPopulation() {
		Population population = new Population();
		for (int p = 0; p < this.populationSize; p++) {
			double[] alphas = new double[this.individualSize];
			for (int j = 0; j < alphas.length; j++) {
				if (getValueType(j).equals(OptimizationValueType.VALUE_TYPE_INT)) {
					alphas[j] = (int) Math.round(random.nextDoubleInRange(this.min[j], this.max[j]));
				} else if (getValueType(j).equals(OptimizationValueType.VALUE_TYPE_BOUNDS)) {
					boolean upper = random.nextBoolean();
					if (upper) {
						alphas[j] = this.max[j];
					} else {
						alphas[j] = this.min[j];
					}
				} else {
					alphas[j] = random.nextDoubleInRange(this.min[j], this.max[j]);
				}
			}
			population.add(new Individual(alphas));
		}
		return population;
	}

	/** Randomly creates the initial population. */
	private Population createMinStartPopulation() {
		Population population = new Population();
		for (int p = 0; p < this.populationSize; p++) {
			double[] alphas = new double[this.individualSize];
			for (int j = 0; j < alphas.length; j++) {
				alphas[j] = this.min[j];
			}
			population.add(new Individual(alphas));
		}
		return population;
	}

	/** Randomly creates the initial population. */
	private Population createMaxStartPopulation() {
		Population population = new Population();
		for (int p = 0; p < this.populationSize; p++) {
			double[] alphas = new double[this.individualSize];
			for (int j = 0; j < alphas.length; j++) {
				alphas[j] = this.max[j];
			}
			population.add(new Individual(alphas));
		}
		return population;
	}

	/** Randomly creates the initial population. */
	private Population createFixedStartPopulation(double fixedValue) {
		Population population = new Population();
		for (int p = 0; p < this.populationSize; p++) {
			double[] alphas = new double[this.individualSize];
			for (int j = 0; j < alphas.length; j++) {
				alphas[j] = fixedValue;
			}
			population.add(new Individual(alphas));
		}
		return population;
	}

	/**
	 * Getter and setter for subclasses
	 **/

	public void increaseCurrentEvaluationCounter() {
		this.currentEvalCounter.incrementAndGet();
	}

	public void increaseTotalEvaluationCounter() {
		this.totalEvalCounter.incrementAndGet();
	}

	public static final List<ParameterType> getParameterTypes(Operator parameterHandler) {
		LinkedList<ParameterType> types = new LinkedList<>();
		ParameterType type;
		types.add(new ParameterTypeInt(PARAMETER_MAX_GENERATIONS, "Stop after this many evaluations.", 1, Integer.MAX_VALUE,
				50, false));
		types.add(new ParameterTypeBoolean(PARAMETER_USE_EARLY_STOPPING,
				"Enables early stopping. If unchecked, always the maximum number of generations is performed.", false,
				false));
		type = new ParameterTypeInt(PARAMETER_GENERATIONS_WITHOUT_IMPROVAL,
				"Stop criterion: Stop after n generations without improval of the performance.", 1, Integer.MAX_VALUE, 2,
				false);
		type.registerDependencyCondition(
				new BooleanParameterCondition(parameterHandler, PARAMETER_USE_EARLY_STOPPING, true, true));
		type.setExpert(false);
		types.add(type);

		types.add(new ParameterTypeBoolean(PARAMETER_SPECIFIY_POPULATION_SIZE,
				"If unchecked, one individuum per example of the delivered example set is used.", true, false));
		type = new ParameterTypeInt(PARAMETER_POPULATION_SIZE, "The population size.", 1, Integer.MAX_VALUE, 5, false);
		type.registerDependencyCondition(
				new BooleanParameterCondition(parameterHandler, PARAMETER_SPECIFIY_POPULATION_SIZE, true, true));
		types.add(type);

		types.add(new ParameterTypeBoolean(PARAMETER_KEEP_BEST,
				"Indicates if the best individual should survive (elititst selection).", true));
		types.add(new ParameterTypeCategory(PARAMETER_MUTATION_TYPE, "The type of the mutation operator.",
				ESOptimization.MUTATION_TYPES, ESOptimization.GAUSSIAN_MUTATION));

		types.add(new ParameterTypeCategory(PARAMETER_SELECTION_TYPE, "The type of the selection operator.",
				ESOptimization.SELECTION_TYPES, ESOptimization.TOURNAMENT_SELECTION));
		type = new ParameterTypeDouble(PARAMETER_TOURNAMENT_FRACTION,
				"The fraction of the population used for tournament selection.", 0.0d, Double.POSITIVE_INFINITY, 0.25d);
		type.registerDependencyCondition(new EqualTypeCondition(parameterHandler, PARAMETER_SELECTION_TYPE,
				ESOptimization.SELECTION_TYPES, true, ESOptimization.TOURNAMENT_SELECTION));
		types.add(type);
		types.add(new ParameterTypeDouble(PARAMETER_CROSSOVER_PROB, "The probability for crossover.", 0.0d, 1.0d, 0.9d));

		types.addAll(RandomGenerator.getRandomGeneratorParameters(parameterHandler));

		types.add(new ParameterTypeBoolean(PARAMETER_SHOW_CONVERGENCE_PLOT,
				"Indicates if a dialog with a convergence plot should be drawn.", false));

		return types;
	}

	/**
	 * The given Operator will be used to perform checkForStop-Operations. If you do not want this
	 * class to perform these checks, set the executingOperator to null
	 */
	public void setExecutingOperator(Operator executingOperator) {
		this.executingOperator = executingOperator;
	}

	/**
	 * this method delivers the Operator which is internally used to perform checkForStopOperations.
	 *
	 * @return executing Operator which checks for Stop
	 */
	public Operator getExecutingOperator() {
		return executingOperator;
	}
}
