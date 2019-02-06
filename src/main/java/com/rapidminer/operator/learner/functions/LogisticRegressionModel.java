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
import com.rapidminer.operator.learner.SimpleBinaryPredictionModel;
import com.rapidminer.tools.Tools;


/**
 * The model determined by the {@link LogisticRegression} operator.
 *
 * @author Ingo Mierswa, Tobias Malbrecht
 */
public class LogisticRegressionModel extends SimpleBinaryPredictionModel {

	private static final long serialVersionUID = -966943348790852574L;

	private double[] beta = null;

	private double[] standardError = null;

	private double[] waldStatistic = null;

	private String[] attributeNames;

	private boolean interceptAdded;

	public LogisticRegressionModel(ExampleSet exampleSet, double[] beta, double[] variance, boolean interceptAdded) {
		super(exampleSet, 0.5d);
		this.attributeNames = com.rapidminer.example.Tools.getRegularAttributeNames(exampleSet);
		this.beta = beta;
		this.interceptAdded = interceptAdded;

		standardError = new double[variance.length];
		waldStatistic = new double[variance.length];
		for (int j = 0; j < beta.length; j++) {
			standardError[j] = Math.sqrt(variance[j]);
			waldStatistic[j] = beta[j] * beta[j] / variance[j];
		}
	}

	@Override
	public double predict(Example example) {
		double eta = 0.0d;
		int i = 0;
		for (Attribute attribute : example.getAttributes()) {
			double value = example.getValue(attribute);
			eta += beta[i] * value;
			i++;
		}
		if (interceptAdded) {
			eta += beta[beta.length - 1];
		}
		return Math.exp(eta) / (1 + Math.exp(eta));
	}

	@Override
	public String toString() {
		StringBuffer result = new StringBuffer();
		if (interceptAdded) {
			result.append("Bias (offset): " + Tools.formatNumber(beta[beta.length - 1]));
			result.append("  \t(SE: " + Tools.formatNumber(standardError[standardError.length - 1]));
			result.append(", Wald: " + Tools.formatNumber(waldStatistic[waldStatistic.length - 1]) + ")"
					+ Tools.getLineSeparators(2));
		}
		result.append("Coefficients:" + Tools.getLineSeparator());
		for (int j = 0; j < beta.length - 1; j++) {
			result.append("beta(" + attributeNames[j] + ") = " + Tools.formatNumber(beta[j]));
			result.append(" \t\t(SE: " + Tools.formatNumber(standardError[j]));
			result.append(", Wald: " + Tools.formatNumber(waldStatistic[j]) + ")" + Tools.getLineSeparator());
		}
		result.append(Tools.getLineSeparator() + "Odds Ratios:" + Tools.getLineSeparator());
		for (int j = 0; j < beta.length - 1; j++) {
			result.append("odds_ratio(" + attributeNames[j] + ") = " + Tools.formatNumber(Math.exp(beta[j]))
					+ Tools.getLineSeparator());
		}
		return result.toString();
	}

	public String[] getAttributeNames() {
		return attributeNames;
	}

	public double[] getCoefficients() {
		return beta;
	}

	public String getFirstLabel() {
		return getTrainingHeader().getAttributes().getLabel().getMapping().getNegativeString();
	}

	public String getSecondLabel() {
		return getTrainingHeader().getAttributes().getLabel().getMapping().getPositiveString();
	}
}
