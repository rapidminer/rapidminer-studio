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
import com.rapidminer.gui.plotter.SimplePlotterDialog;
import com.rapidminer.operator.Operator;
import com.rapidminer.operator.learner.functions.kernel.SupportVector;
import com.rapidminer.operator.performance.EstimatedPerformance;
import com.rapidminer.operator.performance.PerformanceVector;
import com.rapidminer.tools.RandomGenerator;
import com.rapidminer.tools.math.kernels.Kernel;
import com.rapidminer.tools.math.optimization.ec.pso.PSOOptimization;

import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;


/**
 * PSO approach for SVM optimization. Currently only classification problems are supported.
 * 
 * @author Ingo Mierswa Exp $
 */
public class PSOSVMOptimization extends PSOOptimization {

	/** Number smaller than this number are regarded as zero. */
	private final static double IS_ZERO = 1e-10;

	/** The training example set. */
	private ExampleSet exampleSet;

	/** The used kernel function. */
	private Kernel kernel;

	/** This parameter indicates the weight of errors. */
	private double c;

	/** This parameter indicates the weight of errors for regression. */
	private double epsilon = 0.0d;

	/** The label values. */
	private double[] ys;

	/** This function is to maximize. */
	private OptimizationFunction optimizationFunction;

	/** Indicates if a convergence plot should be drawn. */
	private boolean showPlot = false;

	private DataTable dataTable;

	private SimplePlotterDialog plotter;

	/**
	 * @deprecated Since 7.4. Please use
	 *             {@link #PSOSVMOptimization(ExampleSet, Kernel, double, int, int, int, double, double, double, boolean, boolean, RandomGenerator, Operator)
	 *             PSOSVMOptimization} if an operator is known to monitor operator progress and stop
	 *             long running processes if necessary.
	 */
	@Deprecated
	public PSOSVMOptimization(ExampleSet exampleSet, // training data
			Kernel kernel, double c, // double epsilon, // SVM paras
			int maxIterations, int generationsWithoutImprovement, // convergence
																	 // paras
			int popSize, double inertiaWeight, double localWeight, double globalWeight, boolean dynamicInertiaWeight,
			boolean showPlot, RandomGenerator random) {
		this(exampleSet, kernel, c, maxIterations, generationsWithoutImprovement, popSize, inertiaWeight, localWeight,
				globalWeight, dynamicInertiaWeight, showPlot, random, null);
	}

	/** Creates a new evolutionary SVM optimization. */
	public PSOSVMOptimization(ExampleSet exampleSet, // training data
			Kernel kernel, double c, // double epsilon, // SVM paras
			int maxIterations, int generationsWithoutImprovement, // convergence
																	 // paras
			int popSize, double inertiaWeight, double localWeight, double globalWeight, boolean dynamicInertiaWeight,
			boolean showPlot, RandomGenerator random, Operator op) {
		super(popSize < 1 ? exampleSet.size() : popSize, exampleSet.size(), maxIterations, generationsWithoutImprovement,
				inertiaWeight, localWeight, globalWeight, 0.0d, 1.0d, dynamicInertiaWeight, random, op);
		this.exampleSet = exampleSet;
		this.kernel = kernel;
		this.kernel.init(exampleSet);
		this.c = c;
		if (this.c <= 0.0d) {
			this.c = 0.0d;
			for (int i = 0; i < exampleSet.size(); i++) {
				this.c += kernel.getDistance(i, i);
			}
			this.c = exampleSet.size() / this.c;
			exampleSet.getLog().log("Determine probably good value for C: set to " + this.c);
		}
		setMinValue(0.0d);
		setMaxValue(this.c);

		// label values
		this.ys = new double[exampleSet.size()];
		Iterator<Example> reader = exampleSet.iterator();
		int index = 0;
		Attribute label = exampleSet.getAttributes().getLabel();
		boolean regression = !label.isNominal() && label.getMapping().size() != 2;
		while (reader.hasNext()) {
			Example example = reader.next();
			if (!regression) {
				ys[index++] = example.getLabel() == label.getMapping().getPositiveIndex() ? 1.0d : -1.0d;
			} else {
				ys[index++] = example.getLabel();
			}
		}

		// optimization function
		if (!regression) {
			this.optimizationFunction = new ClassificationOptimizationFunction(false);
		} else {
			this.optimizationFunction = new RegressionOptimizationFunction(epsilon);
		}

		// plotter
		this.showPlot = showPlot;
		if (showPlot) {
			dataTable = new SimpleDataTable("Fitness vs. Generations", new String[] { "Generations", "Best Fitness",
					"Current Fitness" });
			plotter = new SimplePlotterDialog(dataTable, false);
			plotter.setXAxis(0);
			plotter.plotColumn(1, true);
			plotter.plotColumn(2, true);
			plotter.setVisible(true);
		}
	}

	@Override
	public void nextIteration() {
		if (showPlot) {
			dataTable.add(new SimpleDataTableRow(new double[] { getGeneration(), getBestFitnessEver(),
					getBestFitnessInGeneration() }));
		}
	}

	/** Evaluates the individuals of the given population. */
	@Override
	public PerformanceVector evaluateIndividual(double[] individual) {
		double fitness = optimizationFunction.getFitness(individual, ys, kernel)[0];
		PerformanceVector result = new PerformanceVector();
		result.addCriterion(new EstimatedPerformance("SVMOptValue", fitness, 1, false));
		return result;
	}

	/**
	 * Returns a model containing all support vectors, i.e. the examples with non-zerp alphas.
	 */
	public EvoSVMModel getModel(double[] alphas) {
		if (showPlot) {
			plotter.dispose();
		}

		// calculate support vectors
		Attribute[] regularAttributes = exampleSet.getAttributes().createRegularAttributeArray();
		Iterator<Example> reader = exampleSet.iterator();
		List<SupportVector> supportVectors = new ArrayList<SupportVector>();
		int index = 0;
		while (reader.hasNext()) {
			double currentAlpha = alphas[index];
			Example currentExample = reader.next();
			if (currentAlpha != 0.0d) {
				double[] x = new double[regularAttributes.length];
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
		// double maxNeg = Double.NEGATIVE_INFINITY;
		// double minPos = Double.POSITIVE_INFINITY;
		while (reader.hasNext()) {
			Example current = reader.next();
			double[] x = new double[regularAttributes.length];
			int a = 0;
			for (Attribute attribute : regularAttributes) {
				x[a++] = current.getValue(attribute);
			}
			sum[index] = kernel.getSum(supportVectors, x);
			// if ((ys[index] < 0) && (sum[index] > maxNeg))
			// maxNeg = sum[index];
			// if ((ys[index] > 0) && (sum[index] < minPos))
			// minPos = sum[index];
			index++;
		}
		// return new EvoSVMModel(exampleSet.getLabel(), supportVectors, kernel,
		// (double)(-maxNeg - minPos) / 2.0d);

		// calculate b (from Stefan's mySVM code)
		double bSum = 0.0d;
		int bCounter = 0;
		for (int i = 0; i < alphas.length; i++) {
			if ((ys[i] * alphas[i] - c < -IS_ZERO) && (ys[i] * alphas[i] > IS_ZERO)) {
				bSum += ys[i] - sum[i] - epsilon;
				bCounter++;
			} else if ((ys[i] * alphas[i] + c > IS_ZERO) && (ys[i] * alphas[i] < -IS_ZERO)) {
				bSum += ys[i] - sum[i] - epsilon;
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
		return new EvoSVMModel(exampleSet, supportVectors, kernel, (bSum / bCounter));
	}
}
