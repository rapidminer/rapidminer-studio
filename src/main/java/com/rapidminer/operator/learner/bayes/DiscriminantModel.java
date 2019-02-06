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
package com.rapidminer.operator.learner.bayes;

import com.rapidminer.example.Attribute;
import com.rapidminer.example.Example;
import com.rapidminer.example.ExampleSet;
import com.rapidminer.example.set.ExampleSetUtilities;
import com.rapidminer.operator.OperatorException;
import com.rapidminer.operator.learner.SimplePredictionModel;
import com.rapidminer.tools.Tools;

import Jama.Matrix;


/**
 * This is the model for discriminant analysis based learning schemes.
 *
 * @author Sebastian Land
 */
public class DiscriminantModel extends SimplePredictionModel {

	private static final long serialVersionUID = 3793343069512113817L;

	private double alpha;
	private String[] labels;

	private Matrix[] meanVectors;
	private Matrix[] inverseCovariances;
	private double[] aprioriProbabilities;

	private double[] constClassValues;

	public DiscriminantModel(ExampleSet exampleSet, String[] labels, Matrix[] meanVectors, Matrix[] inverseCovariances,
			double[] aprioriProbabilities, double alpha) {
		super(exampleSet, ExampleSetUtilities.SetsCompareOption.EQUAL,
				ExampleSetUtilities.TypesCompareOption.ALLOW_SAME_PARENTS);
		this.alpha = alpha;
		this.labels = labels;
		this.meanVectors = meanVectors;
		this.inverseCovariances = inverseCovariances;
		this.aprioriProbabilities = aprioriProbabilities;

		this.constClassValues = new double[labels.length];
		for (int i = 0; i < labels.length; i++) {
			constClassValues[i] = -0.5d
					* meanVectors[i].times(inverseCovariances[i]).times(meanVectors[i].transpose()).get(0, 0)
					+ Math.log(aprioriProbabilities[i]);
		}

	}

	@Override
	public double predict(Example example) throws OperatorException {
		int numberOfAttributes = meanVectors[0].getColumnDimension();
		double[] vector = new double[numberOfAttributes];
		int i = 0;
		for (Attribute attribute : example.getAttributes()) {
			if (attribute.isNumerical()) {
				vector[i] = example.getValue(attribute);
				i++;
			}
		}
		Matrix xVector = new Matrix(vector, 1);

		double[] labelFunction = new double[labels.length];
		for (int labelIndex = 0; labelIndex < labels.length; labelIndex++) {
			labelFunction[labelIndex] = xVector.times(inverseCovariances[labelIndex])
					.times(meanVectors[labelIndex].transpose()).get(0, 0) + constClassValues[labelIndex];

		}
		double maximalValue = Double.NEGATIVE_INFINITY;
		int bestValue = 0;
		for (int labelIndex = 0; labelIndex < labels.length; labelIndex++) {
			if (labelFunction[labelIndex] >= maximalValue) {
				bestValue = labelIndex;
				maximalValue = labelFunction[labelIndex];
			}
		}
		return bestValue;
	}

	@Override
	protected boolean supportsConfidences(Attribute label) {
		return false;
	}

	@Override
	public String getName() {
		if (alpha == QuadraticDiscriminantAnalysis.QDA_ALPHA) {
			return "Quadratic Discriminant Model";
		} else if (alpha == LinearDiscriminantAnalysis.LDA_ALPHA) {
			return "Linear Discriminant Model";
		} else {
			return "Regularized Discriminant Model";
		}
	}

	@Override
	public String toString() {
		StringBuffer buffer = new StringBuffer();
		buffer.append("Apriori probabilities:\n");
		for (int i = 0; i < labels.length; i++) {
			buffer.append(labels[i] + "\t");
			buffer.append(Tools.formatNumber(aprioriProbabilities[i], 4) + "\n");
		}
		return buffer.toString();
	}

}
