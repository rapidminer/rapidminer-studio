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

import com.rapidminer.datatable.DataTable;
import com.rapidminer.datatable.SimpleDataTable;
import com.rapidminer.datatable.SimpleDataTableRow;
import com.rapidminer.example.Attribute;
import com.rapidminer.example.Example;
import com.rapidminer.example.ExampleSet;
import com.rapidminer.operator.Model;
import com.rapidminer.operator.Operator;
import com.rapidminer.operator.OperatorCreationException;
import com.rapidminer.operator.OperatorException;
import com.rapidminer.operator.learner.functions.kernel.SupportVector;
import com.rapidminer.operator.performance.EstimatedPerformance;
import com.rapidminer.operator.performance.PerformanceEvaluator;
import com.rapidminer.operator.performance.PerformanceVector;
import com.rapidminer.tools.LoggingHandler;
import com.rapidminer.tools.OperatorService;
import com.rapidminer.tools.RandomGenerator;
import com.rapidminer.tools.math.kernels.Kernel;
import com.rapidminer.tools.math.optimization.ec.es.ESOptimization;
import com.rapidminer.tools.math.optimization.ec.es.Individual;
import com.rapidminer.tools.math.optimization.ec.es.NonDominatedSortingSelection;
import com.rapidminer.tools.math.optimization.ec.es.Population;

import java.util.ArrayList;
import java.util.Collections;
import java.util.Iterator;
import java.util.LinkedList;
import java.util.List;


/**
 * Evolutionary Strategy approach for SVM optimization. This class can be used for classification
 * problems.
 * 
 * @author Ingo Mierswa
 */
public class ClassificationEvoOptimization extends ESOptimization implements EvoOptimization {

	/** Number smaller than this number are regarded as zero. */
	private final static double IS_ZERO = 1e-8;

	/** The training example set. */
	private ExampleSet exampleSet;

	/** The used kernel function. */
	private Kernel kernel;

	/** The parameter C indicating the weight of errors. */
	private double c;

	/** The label values. */
	private double[] ys;

	/** This function is to maximize. */
	private OptimizationFunction optimizationFunction;

	/**
	 * The hold-out set which can be used to display the generalization performance for the final
	 * population.
	 */
	private ExampleSet holdOutSet = null;

	/** Final population size for plotting. */
	private int populationSize = 10;

	/** Creates a new evolutionary SVM optimization. */
	@Deprecated
	public ClassificationEvoOptimization(
			ExampleSet exampleSet, // training data
			Kernel kernel,
			double c, // SVM paras
			int initType, // start population creation type para
			int maxIterations, int generationsWithoutImprovement,
			int popSize, // GA paras
			int selectionType, double tournamentFraction,
			boolean keepBest, // selection paras
			int mutationType, // type of mutation
			double crossoverProb, boolean showConvergencePlot, boolean showPopulationPlot, ExampleSet holdOutSet,
			RandomGenerator random, LoggingHandler logging) {
		this(exampleSet, kernel, c, initType, maxIterations, generationsWithoutImprovement, popSize, selectionType,
				tournamentFraction, keepBest, mutationType, crossoverProb, showConvergencePlot, showPopulationPlot,
				holdOutSet, random, logging, null);
	}

	/** Creates a new evolutionary SVM optimization. */
	public ClassificationEvoOptimization(
			ExampleSet exampleSet, // training data
			Kernel kernel,
			double c, // SVM paras
			int initType, // start population creation type para
			int maxIterations, int generationsWithoutImprovement,
			int popSize, // GA paras
			int selectionType, double tournamentFraction,
			boolean keepBest, // selection paras
			int mutationType, // type of mutation
			double crossoverProb, boolean showConvergencePlot, boolean showPopulationPlot, ExampleSet holdOutSet,
			RandomGenerator random, LoggingHandler logging, Operator executingOperator) {
		super(EvoSVM.createBoundArray(0.0d, exampleSet.size()), EvoSVM.determineMax(c, kernel, exampleSet, selectionType,
				exampleSet.size()), popSize, exampleSet.size(), initType, maxIterations, generationsWithoutImprovement,
				selectionType, tournamentFraction, keepBest, mutationType, Double.NaN, crossoverProb, showConvergencePlot,
				showPopulationPlot, random, logging, executingOperator);
		this.exampleSet = exampleSet;
		this.holdOutSet = holdOutSet;
		this.populationSize = popSize;

		this.kernel = kernel;
		this.c = getMax(0);

		// label values
		this.ys = new double[exampleSet.size()];
		Iterator<Example> reader = exampleSet.iterator();
		int index = 0;
		Attribute label = exampleSet.getAttributes().getLabel();
		while (reader.hasNext()) {
			Example example = reader.next();
			ys[index++] = example.getLabel() == label.getMapping().getPositiveIndex() ? 1.0d : -1.0d;
		}

		// optimization function
		this.optimizationFunction = new ClassificationOptimizationFunction(selectionType == NON_DOMINATED_SORTING_SELECTION);
	}

	@Override
	public PerformanceVector evaluateIndividual(Individual individual) {
		double[] fitness = optimizationFunction.getFitness(individual.getValues(), ys, kernel);
		PerformanceVector performanceVector = new PerformanceVector();
		if (fitness.length == 1) {
			performanceVector.addCriterion(new EstimatedPerformance("SVM_fitness", fitness[0], 1, false));
		} else {
			performanceVector.addCriterion(new EstimatedPerformance("alpha_sum", fitness[0], 1, false));
			performanceVector.addCriterion(new EstimatedPerformance("svm_objective_function", fitness[1], 1, false));
			if (fitness.length == 3) {
				performanceVector.addCriterion(new EstimatedPerformance("alpha_label_sum", fitness[2], 1, false));
			}
		}
		return performanceVector;
	}

	// ================================================================================
	// T R A I N
	// ================================================================================

	/**
	 * Trains the SVM. In this case an evolutionary strategy approach is applied to determine the
	 * best alpha values.
	 */
	@Override
	public EvoSVMModel train() throws OperatorException {
		optimize();

		if (holdOutSet != null) {
			// List<Double> maxAlphas = new LinkedList<Double>();
			DataTable holdOutSetPerfomance = new SimpleDataTable("Generalization Performance", new String[] { "individual",
					"training error", "test error" });
			Population population = getPopulation();
			NonDominatedSortingSelection selection = new NonDominatedSortingSelection(this.populationSize);
			selection.operate(population);

			// population.sort();

			class TrainingTestError implements Comparable<TrainingTestError> {

				private double trainingError;
				private double testError;
				private double[] alphas;

				TrainingTestError(double trainingError, double testError, double[] alphas) {
					this.trainingError = trainingError;
					this.testError = testError;
					this.alphas = alphas;
				}

				@Override
				public int compareTo(TrainingTestError o) {
					return -1 * Double.compare(this.trainingError, o.trainingError);
				}

				@Override
				public boolean equals(Object o) {
					if (!(o instanceof TrainingTestError)) {
						return false;
					}
					return this.trainingError == ((TrainingTestError) o).trainingError;
				}

				@Override
				public int hashCode() {
					return Double.valueOf(this.trainingError).hashCode();
				}
			}

			List<TrainingTestError> errorList = new LinkedList<>();
			for (int i = 0; i < population.getNumberOfIndividuals(); i++) {
				double[] currentValues = population.get(i).getValues();

				// calc max alpha (corresponds to C)
				/*
				 * double max = Double.NEGATIVE_INFINITY; for (double alpha : currentValues) if
				 * (alpha > max) max = alpha; maxAlphas.add(max);
				 */

				// calc generalization error on hold-out set
				Model model = null;
				try {
					model = getModel(currentValues);
				} catch (IllegalArgumentException e) {
					// skip model
				}

				if (model != null) {
					double trainingError = getError(exampleSet, model);
					double testError = getError(holdOutSet, model);
					errorList.add(new TrainingTestError(trainingError, testError, currentValues));
				}

			}

			Collections.sort(errorList);
			Iterator<TrainingTestError> i = errorList.iterator();
			int counter = 0;
			int bestIndex = -1;
			double bestValue = Double.POSITIVE_INFINITY;
			while (i.hasNext()) {
				TrainingTestError error = i.next();
				holdOutSetPerfomance.add(new SimpleDataTableRow(
						new double[] { counter, error.trainingError, error.testError }));
				if (error.testError < bestValue) {
					bestIndex = counter;
					bestValue = error.testError;
				}
				counter++;
			}

			// create plotter
			/*
			 * SimplePlotterDialog plotter = new SimplePlotterDialog(holdOutSetPerfomance, false);
			 * plotter.setXAxis(0); plotter.plotColumn(1, true); plotter.plotColumn(2, true);
			 * plotter.setPointType(ScatterPlotter.POINTS); plotter.setVisible(true);
			 */

			return getModel(errorList.get(bestIndex).alphas);
		} else {
			return getModel(getBestValuesEver());
		}
	}

	private double getError(ExampleSet exampleSet, Model model) throws OperatorException {
		exampleSet = model.apply(exampleSet);
		try {
			PerformanceEvaluator evaluator = OperatorService.createOperator(PerformanceEvaluator.class);
			evaluator.setParameter("classification_error", "true");
			PerformanceVector performance = evaluator.doWork(exampleSet);
			return performance.getMainCriterion().getAverage();
		} catch (OperatorCreationException e) {
			e.printStackTrace();
			return Double.NaN;
		}
	}

	/** Delivers the fitness of the best individual as performance vector. */
	@Override
	public PerformanceVector getOptimizationPerformance() {
		double[] bestValuesEver = getBestValuesEver();
		double[] finalFitness = optimizationFunction.getFitness(bestValuesEver, ys, kernel);
		PerformanceVector result = new PerformanceVector();
		if (finalFitness.length == 1) {
			result.addCriterion(new EstimatedPerformance("svm_objective_function", finalFitness[0], 1, false));
		} else {
			result.addCriterion(new EstimatedPerformance("alpha_sum", finalFitness[0], 1, false));
			result.addCriterion(new EstimatedPerformance("svm_objective_function", finalFitness[1], 1, false));
			if (finalFitness.length == 3) {
				result.addCriterion(new EstimatedPerformance("alpha_label_sum", finalFitness[2], 1, false));
			}
		}
		return result;
	}

	// ================================================================================
	// C R E A T E M O D E L
	// ================================================================================

	/**
	 * Returns a model containing all support vectors, i.e. the examples with non-zero alphas.
	 */
	private EvoSVMModel getModel(double[] alphas) {
		// calculate support vectors
		Attribute[] regularAttributes = exampleSet.getAttributes().createRegularAttributeArray();
		Iterator<Example> reader = exampleSet.iterator();
		List<SupportVector> supportVectors = new ArrayList<>();
		int index = 0;
		while (reader.hasNext()) {
			double currentAlpha = alphas[index];
			Example currentExample = reader.next();
			if (currentAlpha != 0.0d) {
				double[] x = new double[exampleSet.getAttributes().size()];
				int a = 0;
				for (Attribute attribute : regularAttributes) {
					x[a++] = currentExample.getValue(attribute);
				}
				supportVectors.add(new SupportVector(x, ys[index], currentAlpha));
			}
			index++;
		}

		// calculate all sum values
		double[] sum = new double[exampleSet.size()];
		reader = exampleSet.iterator();
		index = 0;
		while (reader.hasNext()) {
			Example current = reader.next();
			double[] x = new double[exampleSet.getAttributes().size()];
			int a = 0;
			for (Attribute attribute : regularAttributes) {
				x[a++] = current.getValue(attribute);
			}
			sum[index] = kernel.getSum(supportVectors, x);
			index++;
		}

		// calculate b (from Stefan's mySVM code)
		double bSum = 0.0d;
		int bCounter = 0;
		for (int i = 0; i < alphas.length; i++) {
			if ((ys[i] * alphas[i] - c < -IS_ZERO) && (ys[i] * alphas[i] > IS_ZERO)) {
				bSum += ys[i] - sum[i];
				bCounter++;
			} else if ((ys[i] * alphas[i] + c > IS_ZERO) && (ys[i] * alphas[i] < -IS_ZERO)) {
				bSum += ys[i] - sum[i];
				bCounter++;
			}
		}

		if (bCounter == 0) {
			// unlikely
			bSum = 0.0d;
			for (int i = 0; i < alphas.length; i++) {
				if ((ys[i] * alphas[i] < IS_ZERO) && (ys[i] * alphas[i] > -IS_ZERO)) {
					bSum += ys[i] - sum[i];
					bCounter++;
				}
			}
			if (bCounter == 0) {
				// even unlikelier
				bSum = 0.0d;
				for (int i = 0; i < alphas.length; i++) {
					bSum += ys[i] - sum[i];
					bCounter++;
				}
			}
		}
		return new EvoSVMModel(exampleSet, supportVectors, kernel, bSum / bCounter);
	}
}
