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
package com.rapidminer.operator.learner.subgroups.utility;

import com.rapidminer.operator.learner.subgroups.hypothesis.Hypothesis;
import com.rapidminer.operator.learner.subgroups.hypothesis.Rule;

import java.io.Serializable;


/**
 * This is the abstract superclass for all utility functions for calculating the utility of rules.
 * 
 * @author Tobias Malbrecht
 */
public abstract class UtilityFunction implements Serializable {

	private static final long serialVersionUID = 1L;

	public static final int COVERAGE = 0;

	public static final int PRECISION = 1;

	public static final int ACCURACY = 2;

	public static final int BIAS = 3;

	public static final int LIFT = 4;

	public static final int BINOMIAL = 5;

	public static final int WRACC = 6;

	public static final int SQUARED = 7;

	public static final int ODDS = 8;

	public static final int ODDS_RATIO = 9;

	public static final String[] FUNCTIONS = { "Coverage", "Precision", "Accuracy", "Bias", "Lift", "Binomial", "WRAcc",
			"Squared", "Odds", "Odds Ratio" };

	protected static final int POSITIVE_CLASS = 1;

	protected static final int NEGATIVE_CLASS = 0;

	protected double totalWeight = 0.0d;

	protected double totalPositiveWeight = 0.0d;

	protected double totalNegativeWeight = 0.0d;

	double[] priors = new double[2];

	public UtilityFunction(double totalWeight, double totalPositiveWeight) {
		this.totalWeight = totalWeight;
		this.totalPositiveWeight = totalPositiveWeight;
		this.totalNegativeWeight = totalWeight - totalPositiveWeight;
		priors[POSITIVE_CLASS] = totalPositiveWeight / totalWeight;
		priors[NEGATIVE_CLASS] = 1.0d - priors[POSITIVE_CLASS];
	}

	public abstract double utility(Rule rule);

	public abstract double optimisticEstimate(Hypothesis hypothesis);

	public abstract String getName();

	public abstract String getAbbreviation();

	public double getTotalWeight() {
		return totalWeight;
	}

	public double getTotalPositiveWeight() {
		return totalPositiveWeight;
	}

	public double getTotalNegativeWeight() {
		return totalNegativeWeight;
	}

	public static UtilityFunction getUtilityFunction(int utilityFunctionIndex, double totalWeight, double totalPositiveWeight) {
		switch (utilityFunctionIndex) {
			case UtilityFunction.COVERAGE:
				return new Coverage(totalWeight, totalPositiveWeight);
			case UtilityFunction.PRECISION:
				return new Precision(totalWeight, totalPositiveWeight);
			case UtilityFunction.ACCURACY:
				return new Accuracy(totalWeight, totalPositiveWeight);
			case UtilityFunction.BIAS:
				return new Bias(totalWeight, totalPositiveWeight);
			case UtilityFunction.LIFT:
				return new Lift(totalWeight, totalPositiveWeight);
			case UtilityFunction.BINOMIAL:
				return new Binomial(totalWeight, totalPositiveWeight);
			case UtilityFunction.WRACC:
				return new WRAcc(totalWeight, totalPositiveWeight);
			case UtilityFunction.SQUARED:
				return new Squared(totalWeight, totalPositiveWeight);
			case UtilityFunction.ODDS:
				return new Odds(totalWeight, totalPositiveWeight);
			case UtilityFunction.ODDS_RATIO:
				return new OddsRatio(totalWeight, totalPositiveWeight);
		}
		return new Coverage(totalWeight, totalPositiveWeight);
	}

	public static Class<? extends UtilityFunction> getUtilityFunctionClass(int utilityFunctionIndex) {
		switch (utilityFunctionIndex) {
			case UtilityFunction.COVERAGE:
				return Coverage.class;
			case UtilityFunction.PRECISION:
				return Precision.class;
			case UtilityFunction.ACCURACY:
				return Accuracy.class;
			case UtilityFunction.BIAS:
				return Bias.class;
			case UtilityFunction.LIFT:
				return Lift.class;
			case UtilityFunction.BINOMIAL:
				return Binomial.class;
			case UtilityFunction.WRACC:
				return WRAcc.class;
			case UtilityFunction.SQUARED:
				return Squared.class;
			case UtilityFunction.ODDS:
				return Odds.class;
			case UtilityFunction.ODDS_RATIO:
				return OddsRatio.class;
		}
		return null;
	}

	public static UtilityFunction[] getUtilityFunctions(double totalWeight, double totalPositiveWeight) {
		UtilityFunction[] utilities = new UtilityFunction[FUNCTIONS.length];
		for (int i = 0; i < FUNCTIONS.length; i++) {
			utilities[i] = getUtilityFunction(i, totalWeight, totalPositiveWeight);
		}
		return utilities;
	}

	@SuppressWarnings("unchecked")
	public static Class<? extends UtilityFunction>[] getUtilityFunctionClasses() {
		Class<? extends UtilityFunction>[] utilityFunctionClasses = new Class[FUNCTIONS.length];
		for (int i = 0; i < FUNCTIONS.length; i++) {
			utilityFunctionClasses[i] = getUtilityFunctionClass(i);
		}
		return utilityFunctionClasses;
	}

	@Override
	public String toString() {
		return getName();
	}
}
