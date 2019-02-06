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
package com.rapidminer.operator.learner.local;

import java.io.Serializable;
import java.util.Collection;

import Jama.Matrix;

import com.rapidminer.example.Attribute;
import com.rapidminer.example.Attributes;
import com.rapidminer.example.Example;
import com.rapidminer.example.ExampleSet;
import com.rapidminer.example.set.ExampleSetUtilities;
import com.rapidminer.operator.OperatorException;
import com.rapidminer.operator.OperatorProgress;
import com.rapidminer.operator.learner.PredictionModel;
import com.rapidminer.tools.Tools;
import com.rapidminer.tools.container.Tupel;
import com.rapidminer.tools.math.LinearRegression;
import com.rapidminer.tools.math.VectorMath;
import com.rapidminer.tools.math.container.GeometricDataCollection;
import com.rapidminer.tools.math.smoothing.SmoothingKernel;


/**
 * @author Sebastian Land
 *
 */
public class LocalPolynomialRegressionModel extends PredictionModel {

	public static class RegressionData implements Serializable {
		
		private static final long serialVersionUID = 8540161261369474329L;

		private double[] exampleValues;
		private double exampleLabel;
		private double exampleWeight;

		public RegressionData(double[] exampleValues, double exampleLabel, double exampleWeight) {
			this.exampleValues = exampleValues;
			this.exampleLabel = exampleLabel;
			this.exampleWeight = exampleWeight;
		}

		public double[] getExampleValues() {
			return exampleValues;
		}

		public double getExampleLabel() {
			return exampleLabel;
		}

		public double getExampleWeight() {
			return exampleWeight;
		}
	}

	private GeometricDataCollection<RegressionData> samples;
	private Neighborhood neighborhood;
	private SmoothingKernel kernelSmoother;
	private int degree;
	private double ridge;

	protected LocalPolynomialRegressionModel(ExampleSet trainingExampleSet, GeometricDataCollection<RegressionData> data,
			Neighborhood neighborhood, SmoothingKernel kernelSmoother, int degree, double ridge) {
		super(trainingExampleSet, ExampleSetUtilities.SetsCompareOption.ALLOW_SUPERSET,
				ExampleSetUtilities.TypesCompareOption.ALLOW_SAME_PARENTS);
		this.samples = data;
		this.neighborhood = neighborhood;
		this.kernelSmoother = kernelSmoother;
		this.degree = degree;
		this.ridge = ridge;
	}

	private static final long serialVersionUID = -4874020185611138104L;

	@Override
	public ExampleSet performPrediction(ExampleSet exampleSet, Attribute predictedLabel) throws OperatorException {
		Attributes attributes = exampleSet.getAttributes();

		// initialize progress
		OperatorProgress progress = null;
		if (getShowProgress() && getOperator() != null && getOperator().getProgress() != null) {
			progress = getOperator().getProgress();
			progress.setTotal(exampleSet.size());
		}
				
		double[] probe = new double[attributes.size()];
		for (Example example : exampleSet) {
			// copying example values
			int i = 0;
			for (Attribute attribute : attributes) {
				probe[i] = example.getValue(attribute);
				i++;
			}

			// determining neighborhood
			Collection<Tupel<Double, RegressionData>> localExamples = neighborhood.getNeighbourhood(samples, probe);
			if (localExamples.size() > 1) {
				// building matrixes
				double[][] x = new double[localExamples.size()][];
				double[][] y = new double[localExamples.size()][1];
				double[] distance = new double[localExamples.size()];
				double[] weight = new double[localExamples.size()];
				int j = 0;
				for (Tupel<Double, RegressionData> tupel : localExamples) {
					distance[j] = tupel.getFirst();  // distance
					x[j] = VectorMath.polynomialExpansion(tupel.getSecond().getExampleValues(), degree);  // data
					// itself
					y[j][0] = tupel.getSecond().getExampleLabel();   // the label
					weight[j] = tupel.getSecond().getExampleWeight();
					j++;
				}

				// finding greatest distance
				double maxDistance = Double.NEGATIVE_INFINITY;
				for (j = 0; j < distance.length; j++) {
					maxDistance = maxDistance < distance[j] ? distance[j] : maxDistance;
				}

				// using kernel smoother for locality weight calculation and multiply by example
				// weight
				for (j = 0; j < distance.length; j++) {
					weight[j] = weight[j] * kernelSmoother.getWeight(distance[j], maxDistance);
				}

				double[] coefficients = LinearRegression.performRegression(new Matrix(x), new Matrix(y), weight, ridge);
				double[] probeExpaneded = VectorMath.polynomialExpansion(probe, degree);

				example.setPredictedLabel(VectorMath.vectorMultiplication(probeExpaneded, coefficients));
			} else {
				if (localExamples.size() == 1) {
					example.setPredictedLabel(localExamples.iterator().next().getSecond().getExampleLabel());
				} else {
					example.setPredictedLabel(Double.NaN);
				}
			}
			
			if (progress != null) {
				progress.step();
			}
		}
		return exampleSet;
	}

	@Override
	public String toString() {
		StringBuffer buffer = new StringBuffer();
		buffer.append("This model contains " + samples.size() + " examples for determining the neighborhood."
				+ Tools.getLineSeparator());
		buffer.append("The fitted polynomial is of degree " + degree + " and is fitted with a ridge factor of " + ridge
				+ Tools.getLineSeparator());
		buffer.append("It uses the " + neighborhood.toString() + " for neighborhood determination."
				+ Tools.getLineSeparator());
		buffer.append("Weighting is performed using the " + kernelSmoother.toString());
		return buffer.toString();
	}

	public GeometricDataCollection<RegressionData> getSamples() {
		return samples;
	}

	public Neighborhood getNeighborhood() {
		return neighborhood;
	}

	public SmoothingKernel getKernelSmoother() {
		return kernelSmoother;
	}

	public int getDegree() {
		return degree;
	}

	public double getRidge() {
		return ridge;
	}

	public String[] getAttributeNames() {
		ExampleSet trainSet = getTrainingHeader();
		Attributes attributes = trainSet.getAttributes();
		String[] attributeNames = new String[attributes.size()];
		int i = 0;
		for (Attribute attribute : attributes) {
			attributeNames[i] = attribute.getName();
			i++;
		}
		return attributeNames;
	}

}
