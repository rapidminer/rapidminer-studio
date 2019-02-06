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
package com.rapidminer.operator.learner.igss;

import com.rapidminer.example.Example;
import com.rapidminer.example.ExampleSet;
import com.rapidminer.operator.ResultObjectAdapter;
import com.rapidminer.operator.learner.igss.hypothesis.Hypothesis;
import com.rapidminer.tools.Tools;

import java.util.Iterator;
import java.util.LinkedList;


/**
 * This class stores all results found by the IGSS algorithm. It contains method to calculate the
 * prior probabilities and the diversity in the results of the predictions.
 * 
 * @author Dirk Dach
 */
public class IGSSResult extends ResultObjectAdapter {

	private static final long serialVersionUID = -3021620651938759878L;

	/** The list of results. */
	private LinkedList<Result> results;

	/** The default probability of the positive class */
	private double[] priors;

	public IGSSResult(ExampleSet eSet) {
		this.priors = getPriors(eSet);
		results = new LinkedList<Result>();
	}

	/** Adds a result. */
	public void addResult(Result r) {
		this.results.addLast(r);
	}

	/** Returns a list of all stored results. */
	public LinkedList<Result> getResults() {
		return this.results;
	}

	/** Returns the default probability of the example set the object was constructed with. */
	public double[] getPriors() {
		return this.priors;
	}

	/** Returns the default probability of the given example set. */
	public static double[] getPriors(ExampleSet exampleSet) {
		Iterator<Example> reader = exampleSet.iterator();
		double totalWeight = 0.0d;
		double totalPositiveWeight = 0.0d;
		while (reader.hasNext()) {
			Example e = reader.next();
			totalWeight += e.getWeight();
			if ((int) e.getLabel() == Hypothesis.POSITIVE_CLASS) {
				totalPositiveWeight += e.getWeight();
			}
		}
		double[] result = new double[2];
		result[Hypothesis.POSITIVE_CLASS] = totalPositiveWeight / totalWeight;
		result[Hypothesis.NEGATIVE_CLASS] = 1.0d - result[Hypothesis.POSITIVE_CLASS];
		return result;
	}

	/** Calculates the diversity in the predictions of the results for the given example set. */
	public static double calculateDiversity(ExampleSet exampleSet, LinkedList<Result> theResults) {
		Iterator<Example> reader = exampleSet.iterator();
		int[][] predictionMatrix = new int[exampleSet.size()][2];
		for (int i = 0; reader.hasNext(); i++) { // all examples
			Example e = reader.next();
			for (Result res : theResults) {// all results
				Hypothesis hypo = res.getHypothesis(); // get hypothesis
				if (hypo.applicable(e)) {
					predictionMatrix[i][hypo.getPrediction()]++;
				} else {
					predictionMatrix[i][1 - hypo.getPrediction()]++;
				}
			}
		}
		double sum1 = 0.0d;
		for (int i = 0; i < predictionMatrix.length; i++) {
			if (predictionMatrix[i][0] != 0 && predictionMatrix[i][1] != 0) { // avoid Double.NaN
																				// for p0=0 or p1=0
				double p0 = (double) predictionMatrix[i][0] / (double) theResults.size();
				double p1 = (double) predictionMatrix[i][1] / (double) theResults.size();
				sum1 = sum1 + ((-1) * p0 * log2(p0)) + ((-1) * p1 * log2(p1));
			}
		}

		double result = sum1 / predictionMatrix.length;
		return result;
	}

	/** Returns a String-representation of the results in this object. */
	@Override
	public String toString() {
		LinkedList<Result> includedResultsForDiversityCalculation = new LinkedList<Result>();
		StringBuffer result = new StringBuffer("(Rule, Utility)" + Tools.getLineSeparator());
		Iterator<Result> it = this.results.iterator();
		double cumulativeWeight = 0.0d;
		for (int i = 1; it.hasNext(); i++) {
			result.append(i + ") ");
			Result r = it.next();
			includedResultsForDiversityCalculation.addLast(r);
			cumulativeWeight = cumulativeWeight + r.getTotalWeight();
			result.append(r.getHypothesis().toString() + ", " + r.getUtility() + Tools.getLineSeparator());
		}
		result.append("total necessary example weight: " + cumulativeWeight + Tools.getLineSeparator());
		result.append("a priori probability: " + this.priors[Hypothesis.POSITIVE_CLASS] + Tools.getLineSeparator());
		return result.toString();
	}

	/** Returns the logarithm to base 2. */
	public static double log2(double arg) {
		return Math.log(arg) / Math.log(2);
	}

	public String getExtension() {
		return "gss";
	}

	public String getFileDescription() {
		return "IGSS results";
	}

}
