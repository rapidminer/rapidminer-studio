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

import java.util.Iterator;
import java.util.List;

import com.rapidminer.example.Attribute;
import com.rapidminer.example.AttributeWeights;
import com.rapidminer.example.ExampleSet;
import com.rapidminer.example.set.AttributeWeightedExampleSet;
import com.rapidminer.operator.OperatorChain;
import com.rapidminer.operator.OperatorDescription;
import com.rapidminer.operator.OperatorException;
import com.rapidminer.operator.ProcessStoppedException;
import com.rapidminer.operator.UserError;
import com.rapidminer.operator.ValueDouble;
import com.rapidminer.operator.performance.PerformanceVector;
import com.rapidminer.operator.ports.InputPort;
import com.rapidminer.operator.ports.OutputPort;
import com.rapidminer.operator.ports.metadata.ExampleSetPassThroughRule;
import com.rapidminer.operator.ports.metadata.SetRelation;
import com.rapidminer.operator.ports.metadata.SubprocessTransformRule;
import com.rapidminer.parameter.ParameterType;
import com.rapidminer.parameter.ParameterTypeDouble;
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
 * @author Ingo Mierswa <br>
 */
public abstract class ExampleSetBasedFeatureOperator extends OperatorChain {

	public static final String PARAMETER_MAXIMAL_FITNESS = "maximal_fitness";

	private ExampleSetBasedPopulation population;

	/** The optimization stops if this maximal fitness was reached. */
	private double maximalFitness = Double.POSITIVE_INFINITY;

	private boolean checkForMaximalFitness = true;

	private int evaluationCounter = 0;

	private int totalEvaluations = 0;

	private RandomGenerator random;

	private final InputPort exampleSetInput = getInputPorts().createPort("example set in", ExampleSet.class);

	private final OutputPort innerExampleSetSource = getSubprocess(0).getInnerSources().createPort("example set source");
	private final InputPort innerPerformanceSink = getSubprocess(0).getInnerSinks().createPort("performance sink",
			PerformanceVector.class);

	private final OutputPort exampleSetOutput = getOutputPorts().createPort("example set out");
	private final OutputPort attributeWeightsOutput = getOutputPorts().createPort("attribute weights out");
	private final OutputPort performanceOutput = getOutputPorts().createPort("performance out");

	public ExampleSetBasedFeatureOperator(OperatorDescription description) {
		super(description, "Evaluation Process");

		getTransformer().addRule(new ExampleSetPassThroughRule(exampleSetInput, innerExampleSetSource, SetRelation.SUBSET));
		getTransformer().addRule(new SubprocessTransformRule(getSubprocess(0)));
		getTransformer().addPassThroughRule(innerPerformanceSink, performanceOutput);
		getTransformer().addRule(new ExampleSetPassThroughRule(exampleSetInput, exampleSetOutput, SetRelation.SUBSET));
		getTransformer().addGenerationRule(attributeWeightsOutput, AttributeWeights.class);

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
						lengthSum += population.get(i).getExampleSet().getNumberOfUsedAttributes();
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
				ExampleSetBasedIndividual individual = population.getBestIndividualEver();
				if (individual != null) {
					AttributeWeightedExampleSet eSet = individual.getExampleSet();
					if (eSet != null) {
						return eSet.getNumberOfUsedAttributes();
					} else {
						return Double.NaN;
					}
				} else {
					return Double.NaN;
				}
			}
		});
	}

	/**
	 * Create an initial population. The example set will be cloned before the method is invoked.
	 * This method is invoked after the pre- and post-evaluation population operators were
	 * collected.
	 */
	public abstract ExampleSetBasedPopulation createInitialPopulation(ExampleSet es) throws OperatorException;

	/**
	 * Must return a list of <tt>PopulationOperator</tt>s. All operators are applied to the
	 * population in their order within the list before the population is evaluated. Since this
	 * method is invoked only once the list cannot by dynamically changed during runtime.
	 */
	public abstract List<ExampleSetBasedPopulationOperator> getPreEvaluationPopulationOperators(ExampleSet input)
			throws OperatorException;

	/**
	 * Must return a list of <tt>PopulationOperator</tt>s. All operators are applied to the
	 * population in their order within the list after the population is evaluated. Since this
	 * method is invoked only once the list cannot by dynamically changed during runtime.
	 */
	public abstract List<ExampleSetBasedPopulationOperator> getPostEvaluationPopulationOperators(ExampleSet input)
			throws OperatorException;

	/**
	 * Has to return true if the main loop can be stopped because a solution is considered to be
	 * good enough according to some criterion.
	 */
	public abstract boolean solutionGoodEnough(ExampleSetBasedPopulation pop) throws OperatorException;

	protected RandomGenerator getRandom() {
		return random;
	}

	protected ExampleSetBasedPopulation getPopulation() {
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
		this.evaluationCounter = 0;
		this.totalEvaluations = 0;
		this.maximalFitness = getParameterAsDouble(PARAMETER_MAXIMAL_FITNESS);

		ExampleSet es = exampleSetInput.getData(ExampleSet.class);

		if (es.getAttributes().size() == 0) {
			throw new UserError(this, 125, 0, 1);
		}

		List<ExampleSetBasedPopulationOperator> preOps = getPreEvaluationPopulationOperators(es);
		List<ExampleSetBasedPopulationOperator> postOps = getPostEvaluationPopulationOperators(es);

		// create initial population
		population = createInitialPopulation(es);
		log("Initial population has " + population.getNumberOfIndividuals() + " individuals.");
		evaluate(population);

		getProgress().setTotal(getMaxGenerations());
		getProgress().setCheckForStop(false);

		// optimization loop
		while (!solutionGoodEnough(population) && !isMaximumReached()) {
			population.nextGeneration();

			applyOpList(preOps, population);

			log(Tools.ordinalNumber(population.getGeneration()) + " generation has " + population.getNumberOfIndividuals()
					+ " individuals.");
			log("Evaluating " + Tools.ordinalNumber(population.getGeneration()) + " population.");

			evaluate(population);
			population.updateEvaluation();
			applyOpList(postOps, population);

			applyLoopOperations();
		}

		// optimization finished
		applyOpList(postOps, population);
		log("Optimization finished. " + evaluationCounter + " / " + totalEvaluations + " evaluations performed.");

		// create result example set
		ExampleSetBasedIndividual bestEver = population.getBestIndividualEver();

		// create resulting weights
		AttributeWeightedExampleSet weightedResultSet = bestEver.getExampleSet();
		for (Attribute attribute : weightedResultSet.getAttributes()) {
			if (Double.isNaN(weightedResultSet.getWeight(attribute))) {
				weightedResultSet.setWeight(attribute, 1.0d);
			}
		}
		AttributeWeights weights = weightedResultSet.getAttributeWeights();
		Iterator<String> n = weights.getAttributeNames().iterator();
		while (n.hasNext()) {
			String name = n.next();
			if (weightedResultSet.getAttributes().get(name) == null) {
				weights.setWeight(name, 0.0d);
			}
		}

		// normalize weights
		weights.normalize();

		exampleSetOutput.deliver(weightedResultSet.createCleanClone());
		attributeWeightsOutput.deliver(weights);
		performanceOutput.deliver(bestEver.getPerformance());
	}

	/** Applies all PopulationOperators in opList to the population. */
	void applyOpList(List<ExampleSetBasedPopulationOperator> opList, ExampleSetBasedPopulation population)
			throws OperatorException {
		Iterator<ExampleSetBasedPopulationOperator> i = opList.listIterator();
		while (i.hasNext()) {
			ExampleSetBasedPopulationOperator op = i.next();
			if (op.performOperation(population.getGeneration())) {
				try {
					op.operate(population);
					for (int k = 0; k < population.getNumberOfIndividuals(); k++) {
						if (population.get(k).getExampleSet().getNumberOfUsedAttributes() <= 0) {
							getLogger().warning(
									"Population operator " + op + " has produced an example set without attributes!");
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
	protected void evaluate(ExampleSetBasedPopulation population) throws OperatorException {
		for (int i = 0; i < population.getNumberOfIndividuals(); i++) {
			evaluate(population.get(i));
		}
	}

	/**
	 * Evaluates the given individual. The performance is set as user data of the individual and
	 * also returned by this method.
	 */
	protected PerformanceVector evaluate(ExampleSetBasedIndividual individual) throws OperatorException {
		totalEvaluations++;
		if (individual.getPerformance() != null) {
			return individual.getPerformance();
		} else {
			evaluationCounter++;
			AttributeWeightedExampleSet clone = individual.getExampleSet().createCleanClone();
			innerExampleSetSource.deliver(clone);

			getSubprocess(0).execute();

			PerformanceVector performanceVector = innerPerformanceSink.getData(PerformanceVector.class);
			individual.setPerformance(performanceVector);
			return performanceVector;
		}
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

	@Override
	public List<ParameterType> getParameterTypes() {
		List<ParameterType> types = super.getParameterTypes();

		types.addAll(RandomGenerator.getRandomGeneratorParameters(this));

		types.add(new ParameterTypeDouble(PARAMETER_MAXIMAL_FITNESS,
				"The optimization will stop if the fitness reaches the defined maximum.", 0.0d, Double.POSITIVE_INFINITY,
				Double.POSITIVE_INFINITY));
		return types;
	}

	/**
	 * This method should call {@link #inApplyLoop()} and perform operations which should be done
	 * after each iteration of the inner Process.
	 *
	 * @throws ProcessStoppedException
	 */
	protected void applyLoopOperations() throws ProcessStoppedException {
		inApplyLoop();
	}

	/**
	 * This method returns the number of the maximum generations. This is used to determine the
	 * total progress of the operators progress bar. The default value -1 leads to an alternating
	 * progress bar. This should be overwritten by a subclass, if the number of max generations can
	 * be determined.
	 *
	 * @return Number of maximum generations or -1 (if the max generations cannot be determined)
	 */
	protected int getMaxGenerations() {
		return -1;
	}
}
