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

import com.rapidminer.example.Attribute;
import com.rapidminer.example.Example;
import com.rapidminer.example.ExampleSet;
import com.rapidminer.example.set.ExampleSetUtilities;
import com.rapidminer.operator.OperatorException;
import com.rapidminer.operator.learner.SimplePredictionModel;
import com.rapidminer.tools.Tools;


/**
 * The model for the polynomial regression.
 *
 * @author Ingo Mierswa
 */
public class PolynomialRegressionModel extends SimplePredictionModel {

	private static final long serialVersionUID = 5503523600824976254L;

	private String[] attributeConstructions;

	private double[][] coefficients;

	private double[][] degrees;

	private double offset;

	public PolynomialRegressionModel(ExampleSet exampleSet, double[][] coefficients, double[][] degrees, double offset) {
		super(exampleSet, ExampleSetUtilities.SetsCompareOption.EQUAL,
				ExampleSetUtilities.TypesCompareOption.ALLOW_SAME_PARENTS);
		this.attributeConstructions = com.rapidminer.example.Tools.getRegularAttributeConstructions(exampleSet);
		this.coefficients = coefficients;
		this.degrees = degrees;
		this.offset = offset;
	}

	@Override
	public double predict(Example example) throws OperatorException {
		return calculatePrediction(example, coefficients, degrees, offset);
	}

	/**
	 * Calculates the prediction using the values of the example at its regular attributes.
	 */
	public static double calculatePrediction(Example example, double[][] coefficients, double[][] degrees, double offset) {
		double prediction = 0;
		int index = 0;
		for (Attribute attribute : example.getAttributes()) {
			double value = example.getValue(attribute);
			for (int f = 0; f < coefficients.length; f++) {
				prediction += coefficients[f][index] * Math.pow(value, degrees[f][index]);
			}
			index++;
		}
		prediction += offset;
		return prediction;
	}

	/**
	 * Calculates the prediction using the values of the example for the given attributes.
	 */
	public static double calculatePrediction(Example example, Attribute[] attributes, double[][] coefficients,
			double[][] degrees, double offset) {
		double prediction = 0;
		int index = 0;
		for (Attribute attribute : attributes) {
			double value = example.getValue(attribute);
			for (int f = 0; f < coefficients.length; f++) {
				prediction += coefficients[f][index] * Math.pow(value, degrees[f][index]);
			}
			index++;
		}
		prediction += offset;
		return prediction;
	}

	@Override
	protected boolean supportsConfidences(Attribute label) {
		return false;
	}

	@Override
	public String toString() {
		StringBuffer result = new StringBuffer();
		boolean first = true;
		int index = 0;
		for (int i = 0; i < attributeConstructions.length; i++) {
			for (int f = 0; f < coefficients.length; f++) {
				result.append(getCoefficientString(coefficients[f][index], first) + " * " + attributeConstructions[i]
						+ " ^ " + Tools.formatNumber(degrees[f][i]) + Tools.getLineSeparator());
				first = false;
			}
			index++;
		}
		result.append(getCoefficientString(offset, first));
		return result.toString();
	}

	private String getCoefficientString(double coefficient, boolean first) {
		if (!first) {
			if (coefficient >= 0) {
				return "+ " + Tools.formatNumber(Math.abs(coefficient));
			} else {
				return "- " + Tools.formatNumber(Math.abs(coefficient));
			}
		} else {
			if (coefficient >= 0) {
				return "  " + Tools.formatNumber(Math.abs(coefficient));
			} else {
				return "- " + Tools.formatNumber(Math.abs(coefficient));
			}
		}
	}
}
