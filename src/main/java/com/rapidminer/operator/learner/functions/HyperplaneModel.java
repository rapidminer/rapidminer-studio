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

import com.rapidminer.datatable.DataTable;
import com.rapidminer.datatable.SimpleDataTable;
import com.rapidminer.datatable.SimpleDataTableRow;
import com.rapidminer.example.Attribute;
import com.rapidminer.example.Example;
import com.rapidminer.example.ExampleSet;
import com.rapidminer.example.set.ExampleSetUtilities;
import com.rapidminer.operator.OperatorException;
import com.rapidminer.operator.learner.SimplePredictionModel;
import com.rapidminer.tools.Tools;
import com.rapidminer.tools.math.kernels.DotKernel;
import com.rapidminer.tools.math.kernels.Kernel;


/**
 * This model is a separating hyperplane for two classes.
 *
 * @author Sebastian Land
 */
public class HyperplaneModel extends SimplePredictionModel {

	private static final long serialVersionUID = -4990692589416639697L;

	private String[] coefficientNames;

	private double[] coefficients;

	private double intercept;

	private String classNegative;

	private String classPositive;

	private Kernel kernel;

	public HyperplaneModel(ExampleSet exampleSet) {
		this(exampleSet, null, null);
	}

	public HyperplaneModel(ExampleSet exampleSet, String classNegative, String classPositive) {
		this(exampleSet, classNegative, classPositive, new DotKernel());
	}

	public HyperplaneModel(ExampleSet exampleSet, String classNegative, String classPositive, Kernel kernel) {
		super(exampleSet, ExampleSetUtilities.SetsCompareOption.EQUAL,
				ExampleSetUtilities.TypesCompareOption.ALLOW_SAME_PARENTS);
		this.coefficientNames = com.rapidminer.example.Tools.getRegularAttributeNames(exampleSet);
		this.classNegative = classNegative;
		this.classPositive = classPositive;
		this.kernel = kernel;
	}

	@Override
	public double predict(Example example) throws OperatorException {
		int i = 0;
		double distance = intercept;
		// using kernel for distance calculation
		double[] values = new double[example.getAttributes().size()];
		for (Attribute currentAttribute : example.getAttributes()) {
			values[i] = example.getValue(currentAttribute);
			i++;
		}
		distance += kernel.calculateDistance(values, coefficients);
		if (getLabel().isNominal()) {
			int positiveMapping = getLabel().getMapping().mapString(classPositive);
			int negativeMapping = getLabel().getMapping().mapString(classNegative);
			boolean isApplying = example.getAttributes().getPredictedLabel() != null;
			if (isApplying) {
				example.setConfidence(classPositive, 1.0d / (1.0d + java.lang.Math.exp(-distance)));
				example.setConfidence(classNegative, 1.0d / (1.0d + java.lang.Math.exp(distance)));
			}
			if (distance < 0) {
				return negativeMapping;
			} else {
				return positiveMapping;
			}
		} else {
			return distance;
		}
	}

	public void init(double[] coefficients, double intercept) {
		this.coefficients = coefficients;
		this.intercept = intercept;
	}

	public double[] getCoefficients() {
		return coefficients;
	}

	public double getIntercept() {
		return intercept;
	}

	public void setCoefficients(double[] coefficients) {
		this.coefficients = coefficients;
	}

	public void setIntercept(double intercept) {
		this.intercept = intercept;
	}

	@Override
	public String toString() {
		StringBuffer buffer = new StringBuffer();
		if (classPositive != null && classNegative != null) {
			buffer.append(
					"Hyperplane seperating " + classPositive + " and " + classNegative + "." + Tools.getLineSeparator());
		} else {
			buffer.append("Hyperplane for linear regression." + Tools.getLineSeparator());
		}
		buffer.append("Intercept: ");
		buffer.append(Double.toString(intercept));
		buffer.append(Tools.getLineSeparator());
		buffer.append("Coefficients: " + Tools.getLineSeparator());
		int counter = 0;
		for (double value : coefficients) {
			buffer.append("w(" + coefficientNames[counter] + ") = " + Tools.formatIntegerIfPossible(value)
					+ Tools.getLineSeparator());
			counter++;
		}
		buffer.append(Tools.getLineSeparator());
		return buffer.toString();
	}

	public DataTable createWeightsTable() {
		SimpleDataTable weightTable = new SimpleDataTable("Hyperplane Model Weights",
				new String[] { "Attribute", "Weight" });
		for (int j = 0; j < this.coefficientNames.length; j++) {
			int nameIndex = weightTable.mapString(0, this.coefficientNames[j]);
			weightTable.add(new SimpleDataTableRow(new double[] { nameIndex, this.coefficients[j] }));
		}
		return weightTable;
	}
}
