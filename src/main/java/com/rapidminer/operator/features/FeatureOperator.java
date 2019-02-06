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
package com.rapidminer.operator.features;

import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.io.PrintWriter;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;

import com.rapidminer.datatable.SimpleDataTable;
import com.rapidminer.example.Attribute;
import com.rapidminer.example.AttributeWeights;
import com.rapidminer.example.ExampleSet;
import com.rapidminer.example.set.AttributeWeightedExampleSet;
import com.rapidminer.gui.dialog.IndividualSelector;
import com.rapidminer.operator.OperatorChain;
import com.rapidminer.operator.OperatorDescription;
import com.rapidminer.operator.OperatorException;
import com.rapidminer.operator.UserError;
import com.rapidminer.operator.ValueDouble;
import com.rapidminer.operator.ValueString;
import com.rapidminer.operator.performance.PerformanceVector;
import com.rapidminer.operator.ports.InputPort;
import com.rapidminer.operator.ports.OutputPort;
import com.rapidminer.operator.ports.PortPairExtender;
import com.rapidminer.operator.ports.metadata.ExampleSetMetaData;
import com.rapidminer.operator.ports.metadata.ExampleSetPassThroughRule;
import com.rapidminer.operator.ports.metadata.GenerateNewMDRule;
import com.rapidminer.operator.ports.metadata.MetaData;
import com.rapidminer.operator.ports.metadata.PassThroughRule;
import com.rapidminer.operator.ports.metadata.SetRelation;
import com.rapidminer.operator.ports.metadata.SimplePrecondition;
import com.rapidminer.operator.ports.metadata.SubprocessTransformRule;
import com.rapidminer.parameter.ParameterType;
import com.rapidminer.parameter.ParameterTypeBoolean;
import com.rapidminer.parameter.ParameterTypeDouble;
import com.rapidminer.parameter.ParameterTypeFile;
import com.rapidminer.parameter.ParameterTypeInt;
import com.rapidminer.parameter.UndefinedParameterError;
import com.rapidminer.parameter.conditions.BooleanParameterCondition;
import com.rapidminer.tools.RandomGenerator;
import com.rapidminer.tools.Tools;


/**
 * This class is the superclass of all feature selection and generation operators. It provides an
 * easy to use plug-in interface for operators that modify populations. Subclasses just have to
 * supply lists of <tt>PopulationOperators</tt> by overriding
 * <tt>getPreEvalutaionPopulationOperators()</tt> and
 * <tt>getPostEvalutaionPopulationOperators()</tt> during a loop which will terminate if
 * <tt>solutionGoodEnough()</tt> returns true.
 *
 * @author Simon Fischer, Ingo Mierswa <br>
 */
public abstract class FeatureOperator extends OperatorChain {

	public static final String PARAMETER_NORMALIZE_WEIGHTS = "normalize_weights";

	public static final String PARAMETER_USER_RESULT_INDIVIDUAL_SELECTION = "user_result_individual_selection";

	public static final String PARAMETER_SHOW_POPULATION_PLOTTER = "show_population_plotter";

	public static final String PARAMETER_PLOT_GENERATIONS = "plot_generations";

	public static final String PARAMETER_CONSTRAINT_DRAW_RANGE = "constraint_draw_range";

	public static final String PARAMETER_DRAW_DOMINATED_POINTS = "draw_dominated_points";

	public static final String PARAMETER_POPULATION_CRITERIA_DATA_FILE = "population_criteria_data_file";

	public static final String PARAMETER_MAXIMAL_FITNESS = "maximal_fitness";

	private final InputPort exampleSetInput = getInputPorts().createPort("example set in");
	private final OutputPort exampleSetOutput = getOutputPorts().createPort("example set out");
	private final OutputPort attributeWeightsOutput = getOutputPorts().createPort("weights");
	private final OutputPort performanceOutput = getOutputPorts().createPort("performance");
	private final OutputPort subprocessExampleOutput = getSubprocess(0).getInnerSources().createPort("example set");
	private final InputPort subprocessPerformanceInput = getSubprocess(0).getInnerSinks().createPort("performance");
	private final PortPairExtender throughExtender = new PortPairExtender("through", getInputPorts(),
			getSubprocess(0).getInnerSources());

	private ExampleSet exampleSet;

	private Population population;

	private PopulationEvaluator populationEvaluator;

	/** The optimization stops if this maximal fitness was reached. */
	private double maximalFitness = Double.POSITIVE_INFINITY;

	private boolean checkForMaximalFitness = true;

	private RandomGenerator random;

	public FeatureOperator(OperatorDescription description) {
		super(description, "Evaluation Process");

		throughExtender.start();
		exampleSetInput.addPrecondition(new SimplePrecondition(exampleSetInput, new ExampleSetMetaData()));
		subprocessPerformanceInput
				.addPrecondition(new SimplePrecondition(subprocessPerformanceInput, new MetaData(PerformanceVector.class)));
		getTransformer().addRule(new ExampleSetPassThroughRule(exampleSetInput, subprocessExampleOutput, SetRelation.EQUAL) {

			@Override
			public ExampleSetMetaData modifyExampleSet(ExampleSetMetaData metaData) {
				return modifyInnerOutputExampleSet(metaData);
			}
		});
		getTransformer().addRule(new ExampleSetPassThroughRule(exampleSetInput, exampleSetOutput, SetRelation.EQUAL) {

			@Override
			public ExampleSetMetaData modifyExampleSet(ExampleSetMetaData metaData) {
				return modifyOutputExampleSet(metaData);
			}
		});
		getTransformer().addRule(throughExtender.makePassThroughRule());
		getTransformer().addRule(new SubprocessTransformRule(getSubprocess(0)));
		getTransformer().addRule(new GenerateNewMDRule(attributeWeightsOutput, new MetaData(AttributeWeights.class)));
		getTransformer().addRule(new PassThroughRule(subprocessPerformanceInput, performanceOutput, false));

		addValue(new ValueDouble("generation", "The number of the current generation.") {

			@Override
			public double getDoubleValue() {
				if (population == null) {
					return 0;
				}
				return population.getGeneration();
			}
		});
		addValue(new ValueDouble("performance", "The performance of the current generation (main criterion).") {

			@Override
			public double getDoubleValue() {
				if (population == null) {
					return Double.NaN;
				}
				if (population.getCurrentBestPerformance() == null) {
					return Double.NaN;
				}
				PerformanceVector pv = population.getCurrentBestPerformance();
				if (pv == null) {
					return Double.NaN;
				}
				return pv.getMainCriterion().getAverage();
			}
		});
		addValue(new ValueDouble("best", "The performance of the best individual ever (main criterion).") {

			@Override
			public double getDoubleValue() {
				if (population == null) {
					return Double.NaN;
				}
				PerformanceVector pv = population.getBestPerformanceEver();
				if (pv == null) {
					return Double.NaN;
				}
				return pv.getMainCriterion().getAverage();
			}
		});
		addValue(new ValueDouble("average_length", "The average number of attributes.") {

			@Override
			public double getDoubleValue() {
				if (population == null) {
					return Double.NaN;
				} else {
					double lengthSum = 0.0d;
					for (int i = 0; i < population.getNumberOfIndividuals(); i++) {
						lengthSum += population.get(i).getNumberOfUsedAttributes();
					}
					return lengthSum / population.getNumberOfIndividuals();
				}
			}
		});
		addValue(new ValueDouble("best_length", "The number of attributes of the best example set.") {

			@Override
			public double getDoubleValue() {
				if (population == null) {
					return Double.NaN;
				}
				Individual individual = population.getBestIndividualEver();
				if (individual != null) {
					return individual.getNumberOfUsedAttributes();
				} else {
					return Double.NaN;
				}
			}
		});
		addValue(new ValueString("feature_names", "The names of the features of the best individual so far.") {

			@Override
			public String getStringValue() {
				if (population == null) {
					return "?";
				}
				Individual individual = population.getBestIndividualEver();
				if (individual != null) {
					double[] weights = individual.getWeights();
					String[] names = com.rapidminer.example.Tools.getRegularAttributeNames(exampleSet);
					StringBuffer result = new StringBuffer();
					boolean first = true;
					for (int i = 0; i < weights.length; i++) {
						if (weights[i] > 0) {
							if (!first) {
								result.append(", ");
							}
							result.append(names[i]);
							first = false;
						}
					}
					return result.toString();
				} else {
					return "?";
				}
			}
		});
	}

	/**
	 * Subclasses might override this method in order to change the meta data delivered to the inner
	 * operators.
	 */
	protected ExampleSetMetaData modifyInnerOutputExampleSet(ExampleSetMetaData metaData) {
		return metaData;
	}

	/**
	 * Subclasses might override this method in order to change the final outputed meta data
	 */
	protected ExampleSetMetaData modifyOutputExampleSet(ExampleSetMetaData metaData) {
		return metaData;
	}

	/**
	 * Create an initial population. The example set will be cloned before the method is invoked.
	 * This method is invoked after the pre- and postevaluation population operators were collected.
	 */
	public abstract Population createInitialPopulation(ExampleSet es) throws OperatorException;

	/**
	 * Must return a list of <tt>PopulationOperator</tt>s. All operators are applied to the
	 * population in their order within the list before the population is evaluated. Since this
	 * methode is invoked only once the list cannot by dynamically changed during runtime.
	 */
	public abstract List<PopulationOperator> getPreEvaluationPopulationOperators(ExampleSet input) throws OperatorException;

	/**
	 * Must return a list of <tt>PopulationOperator</tt>s. All operators are applied to the
	 * population in their order within the list after the population is evaluated. Since this
	 * methode is invoked only once the list cannot by dynamically changed during runtime.
	 */
	public abstract List<PopulationOperator> getPostEvaluationPopulationOperators(ExampleSet input) throws OperatorException;

	/**
	 * Has to return true if the main loop can be stopped because a solution is considered to be
	 * good enough according to some criterion.
	 */
	public abstract boolean solutionGoodEnough(Population pop) throws OperatorException;

	protected RandomGenerator getRandom() {
		return random;
	}

	protected Population getPopulation() {
		return population;
	}

	/**
	 * Applies the feature operator:
	 * <ol>
	 * <li>collects the pre- and postevaluation operators
	 * <li>create an initial population
	 * <li>evaluate the initial population
	 * <li>loop as long as solution is not good enough
	 * <ol>
	 * <li>apply all pre evaluation operators
	 * <li>evaluate the population
	 * <li>update the population's best individual
	 * <li>apply all post evaluation operators
	 * </ol>
	 * <li>return all generation's best individual
	 * </ol>
	 */
	@Override
	public void doWork() throws OperatorException {
		// init
		this.random = RandomGenerator.getRandomGenerator(this);
		this.maximalFitness = getParameterAsDouble(PARAMETER_MAXIMAL_FITNESS);

		ExampleSet es = exampleSetInput.getData(ExampleSet.class);
		// creating population evaluator
		this.populationEvaluator = getPopulationEvaluator(es);

		if (es.getAttributes().size() == 0) {
			throw new UserError(this, 125, 0, 1);
		}

		this.exampleSet = es;

		List<PopulationOperator> preOps = getPreEvaluationPopulationOperators(es);
		List<PopulationOperator> postOps = getPostEvaluationPopulationOperators(es);

		// create initial population
		population = createInitialPopulation(es);
		log("Initial population has " + population.getNumberOfIndividuals() + " individuals.");

		// initial evaluation
		if (getMaximumGenerations() >= 0) {
			getProgress().setTotal(getMaximumGenerations() * population.getNumberOfIndividuals());
		} else {
			getProgress().setTotal(-1);
		}
		evaluate(population, exampleSet);

		// population plotter
		PopulationPlotter popPlotter = null;
		// Check must be made to ensure inner operators did not exite prematurely leaving null
		// performance
		checkForStop();
		population.updateEvaluation();
		if (getParameterAsBoolean(PARAMETER_SHOW_POPULATION_PLOTTER)) {
			popPlotter = new PopulationPlotter(exampleSet, getParameterAsInt(PARAMETER_PLOT_GENERATIONS),
					getParameterAsBoolean(PARAMETER_CONSTRAINT_DRAW_RANGE),
					getParameterAsBoolean(PARAMETER_DRAW_DOMINATED_POINTS));
			popPlotter.operate(population);
		}
		inApplyLoop();

		// optimization loop
		while (!solutionGoodEnough(population) && !isMaximumReached()) {
			population.nextGeneration();

			applyOpList(preOps, population);

			log(Tools.ordinalNumber(population.getGeneration()) + " generation has " + population.getNumberOfIndividuals()
					+ " individuals.");
			log("Evaluating " + Tools.ordinalNumber(population.getGeneration()) + " population.");

			evaluate(population, exampleSet);
			population.updateEvaluation();
			applyOpList(postOps, population);
			if (popPlotter != null) {
				popPlotter.operate(population);
			}

			inApplyLoop();
		}

		// optimization finished: Check must be made to ensure inner operators did not exite
		// prematurely leaving null performance
		checkForStop();
		applyOpList(postOps, population);

		// write criteria data of the final population into a file
		if (isParameterSet(PARAMETER_POPULATION_CRITERIA_DATA_FILE)) {
			SimpleDataTable finalStatistics = PopulationPlotter.createDataTable(population);
			PopulationPlotter.fillDataTable(finalStatistics, new HashMap<String, double[]>(), population,
					getParameterAsBoolean(PARAMETER_DRAW_DOMINATED_POINTS));
			File outFile = getParameterAsFile(PARAMETER_POPULATION_CRITERIA_DATA_FILE, true);
			try (FileWriter fw = new FileWriter(outFile); PrintWriter out = new PrintWriter(fw)) {
				finalStatistics.write(out);
			} catch (IOException e) {
				throw new UserError(this, e, 303, new Object[] { outFile, e.getMessage() });
			}
		}

		// create result example set
		Individual bestEver = null;
		if (getParameterAsBoolean(PARAMETER_USER_RESULT_INDIVIDUAL_SELECTION)) {
			IndividualSelector selector = new IndividualSelector(exampleSet, population);
			selector.setVisible(true);
			bestEver = selector.getSelectedIndividual();
			if (bestEver == null) {
				logWarning("No individual selected. Using individual with highest fitness for main criterion...");
			}
		}
		if (bestEver == null) {
			bestEver = population.getBestIndividualEver();
		}

		// create resulting weights
		double[] weights = bestEver.getWeights();
		int a = 0;
		AttributeWeights attributeWeights = new AttributeWeights();
		for (Attribute attribute : exampleSet.getAttributes()) {
			double weight = weights[a];
			if (Double.isNaN(weight)) {
				weight = 1.0d;
			}
			attributeWeights.setWeight(attribute.getName(), weight);
			a++;
		}

		// normalize weights
		if (getParameterAsBoolean(PARAMETER_NORMALIZE_WEIGHTS)) {
			attributeWeights.normalize();
		}

		getProgress().complete();

		// clean up
		exampleSetOutput.deliver(createCleanClone(exampleSet, weights));
		attributeWeightsOutput.deliver(attributeWeights);
		performanceOutput.deliver(bestEver.getPerformance());

		this.population.clear();
		this.population = null;
		this.exampleSet = null;
	}

	public static ExampleSet createCleanClone(ExampleSet exampleSet, double[] weights) {
		AttributeWeightedExampleSet clone = new AttributeWeightedExampleSet(exampleSet, null);
		int a = 0;
		for (Attribute attribute : clone.getAttributes()) {
			clone.setWeight(attribute, weights[a++]);
		}
		return clone.createCleanClone();
	}

	/** Applies all PopulationOperators in opList to the population. */
	void applyOpList(List<PopulationOperator> opList, Population population) throws OperatorException {
		Iterator<PopulationOperator> i = opList.listIterator();
		while (i.hasNext()) {
			PopulationOperator op = i.next();
			if (op.performOperation(population.getGeneration())) {
				try {
					op.operate(population);
					for (int k = 0; k < population.getNumberOfIndividuals(); k++) {
						if (population.get(k).getNumberOfUsedAttributes() <= 0) {
							logError("Population operator " + op + " has produced an example set without attributes!");
						}
					}
				} catch (Exception e) {
					throw new UserError(this, e, 108, e.toString());
				}
			}
		}
	}

	/**
	 * Evaluates all individuals in the population by applying the inner operators.
	 */
	private void evaluate(Population population, ExampleSet originalExampleSet) throws OperatorException {
		populationEvaluator.evaluate(population);
	}

	/**
	 * This method gives access to the subprocess for evaluating an example set
	 *
	 * @param exampleSet
	 *            a weighted exampleSet
	 * @return
	 * @throws OperatorException
	 */
	public final PerformanceVector executeEvaluationProcess(ExampleSet exampleSet) throws OperatorException {
		subprocessExampleOutput.deliver(exampleSet);
		throughExtender.passDataThrough();

		runEvaluationProcess();

		return subprocessPerformanceInput.getData(PerformanceVector.class);
	}

	protected void runEvaluationProcess() throws OperatorException {
		getSubprocess(0).execute();
	}

	/** This method checks if the maximum was reached for the main criterion. */
	private boolean isMaximumReached() {
		if (checkForMaximalFitness) {
			PerformanceVector pv = population.getBestPerformanceEver();
			if (pv == null) {
				return false;
			} else {
				if (pv.getMainCriterion().getFitness() == Double.POSITIVE_INFINITY) {
					return true;
				} else if (pv.getMainCriterion().getMaxFitness() == pv.getMainCriterion().getFitness()) {
					return true;
				} else {
					return pv.getMainCriterion().getFitness() >= maximalFitness;
				}
			}
		} else {
			return false;
		}
	}

	/**
	 * Sets if the operator should check if the maximum was reached for the main criterion.
	 * Subclasses may want to set this to false, e.g. for multiobjective optimization.
	 */
	protected void setCheckForMaximum(boolean checkForMaximalFitness) {
		this.checkForMaximalFitness = checkForMaximalFitness;
	}

	/**
	 * Returns if the operator should check if the maximum was reached for the main criterion.
	 * Subclasses may want to set this to false, e.g. for multiobjective optimization.
	 */
	protected boolean getCheckForMaximum() {
		return this.checkForMaximalFitness;
	}

	public InputPort getExampleSetInput() {
		return exampleSetInput;
	}

	@Override
	public List<ParameterType> getParameterTypes() {
		List<ParameterType> types = super.getParameterTypes();
		types.add(new ParameterTypeBoolean(PARAMETER_NORMALIZE_WEIGHTS,
				"Indicates if the final weights should be normalized.", true));

		types.addAll(RandomGenerator.getRandomGeneratorParameters(this));

		types.add(new ParameterTypeBoolean(PARAMETER_USER_RESULT_INDIVIDUAL_SELECTION,
				"Determines if the user wants to select the final result individual from the last population.", false));

		// generation plot parameters
		types.add(new ParameterTypeBoolean(PARAMETER_SHOW_POPULATION_PLOTTER,
				"Determines if the current population should be displayed in performance space.", false));

		ParameterType type = new ParameterTypeInt(PARAMETER_PLOT_GENERATIONS,
				"Update the population plotter in these generations.", 1, Integer.MAX_VALUE, 10);
		type.registerDependencyCondition(
				new BooleanParameterCondition(this, PARAMETER_SHOW_POPULATION_PLOTTER, false, true));
		types.add(type);
		type = new ParameterTypeBoolean(PARAMETER_CONSTRAINT_DRAW_RANGE,
				"Determines if the draw range of the population plotter should be constrained between 0 and 1.", false);
		type.registerDependencyCondition(
				new BooleanParameterCondition(this, PARAMETER_SHOW_POPULATION_PLOTTER, false, true));
		types.add(type);
		type = new ParameterTypeBoolean(PARAMETER_DRAW_DOMINATED_POINTS,
				"Determines if only points which are not Pareto dominated should be painted.", true);
		type.registerDependencyCondition(
				new BooleanParameterCondition(this, PARAMETER_SHOW_POPULATION_PLOTTER, false, true));
		types.add(type);

		types.add(new ParameterTypeFile(PARAMETER_POPULATION_CRITERIA_DATA_FILE,
				"The path to the file in which the criteria data of the final population should be saved.", "cri", true));
		types.add(new ParameterTypeDouble(PARAMETER_MAXIMAL_FITNESS,
				"The optimization will stop if the fitness reaches the defined maximum.", 0.0d, Double.POSITIVE_INFINITY,
				Double.POSITIVE_INFINITY));
		return types;
	}

	protected PopulationEvaluator getPopulationEvaluator(ExampleSet exampleSet) throws OperatorException {
		return new SimplePopulationEvaluator(this, exampleSet);
	}

	/**
	 * Returns the maximum number of generations which can be used as the total operator progress.
	 * Returns -1, if the operator is not able to determine a total operator progress.
	 *
	 * @throws UndefinedParameterError
	 */
	protected int getMaximumGenerations() throws UndefinedParameterError {
		return -1;
	}
}
