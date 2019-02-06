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
package com.rapidminer.operator.performance;

import com.rapidminer.example.Attribute;
import com.rapidminer.example.Example;
import com.rapidminer.example.ExampleSet;
import com.rapidminer.operator.OperatorException;
import com.rapidminer.operator.UserError;
import com.rapidminer.tools.math.Averagable;

import java.util.Iterator;


/**
 * Relative squared error is the total squared error made relative to what the error would have been
 * if the prediction had been the average of the absolute value. As done with the root mean-squared
 * error, the square root of the relative squared error is taken to give it the same dimensions as
 * the predicted values themselves. Also, just like root mean-squared error, this exaggerates the
 * cases in which the prediction error was significantly greater than the mean error.
 * 
 * @author Ingo Mierswa ingomierswa Exp $
 */
public class RootRelativeSquaredError extends MeasuredPerformance {

	private static final long serialVersionUID = 7781104825149866444L;

	private Attribute predictedAttribute;

	private Attribute labelAttribute;

	private Attribute weightAttribute;

	private double deviationSum = 0.0d;

	private double relativeSum = 0.0d;

	private double trueLabelSum = 0.0d;

	private double exampleCounter = 0;

	public RootRelativeSquaredError() {}

	public RootRelativeSquaredError(RootRelativeSquaredError rse) {
		super(rse);
		this.deviationSum = rse.deviationSum;
		this.relativeSum = rse.relativeSum;
		this.trueLabelSum = rse.trueLabelSum;
		this.exampleCounter = rse.exampleCounter;
		this.labelAttribute = (Attribute) rse.labelAttribute.clone();
		this.predictedAttribute = (Attribute) rse.predictedAttribute.clone();
		if (rse.weightAttribute != null) {
			this.weightAttribute = (Attribute) rse.weightAttribute.clone();
		}
	}

	@Override
	public String getName() {
		return "root_relative_squared_error";
	}

	@Override
	public String getDescription() {
		return "Averaged root-relative-squared error";
	}

	@Override
	public double getExampleCount() {
		return exampleCounter;
	}

	@Override
	public void startCounting(ExampleSet exampleSet, boolean useExampleWeights) throws OperatorException {
		super.startCounting(exampleSet, useExampleWeights);
		if (exampleSet.size() <= 1) {
			throw new UserError(null, 919, getName(),
					"root relative squared error can only be calculated for test sets with more than 2 examples.");
		}
		this.predictedAttribute = exampleSet.getAttributes().getPredictedLabel();
		this.labelAttribute = exampleSet.getAttributes().getLabel();
		if (useExampleWeights) {
			this.weightAttribute = exampleSet.getAttributes().getWeight();
		}

		this.trueLabelSum = 0.0d;
		this.deviationSum = 0.0d;
		this.relativeSum = 0.0d;
		this.exampleCounter = 0.0d;
		Iterator<Example> reader = exampleSet.iterator();
		while (reader.hasNext()) {
			Example example = reader.next();
			double label = example.getValue(labelAttribute);
			if (!Double.isNaN(label)) {
				exampleCounter += 1;
				trueLabelSum += label;
			}
		}
	}

	/** Calculates the error for the current example. */
	@Override
	public void countExample(Example example) {
		double plabel;
		double label = example.getValue(labelAttribute);

		if (!predictedAttribute.isNominal()) {
			plabel = example.getValue(predictedAttribute);
		} else {
			String labelS = example.getNominalValue(labelAttribute);
			plabel = example.getConfidence(labelS);
			label = 1.0d;
		}

		double weight = 1.0d;
		if (weightAttribute != null) {
			weight = example.getValue(weightAttribute);
		}

		double diff = Math.abs(label - plabel);
		deviationSum += diff * diff * weight * weight;
		double relDiff = Math.abs(label - (trueLabelSum / exampleCounter));
		relativeSum += relDiff * relDiff * weight * weight;
	}

	@Override
	public double getMikroAverage() {
		return Math.sqrt(deviationSum / relativeSum);
	}

	@Override
	public double getMikroVariance() {
		return Double.NaN;
	}

	@Override
	public double getFitness() {
		return (-1) * getAverage();
	}

	@Override
	public void buildSingleAverage(Averagable performance) {
		RootRelativeSquaredError other = (RootRelativeSquaredError) performance;
		this.deviationSum += other.deviationSum;
		this.relativeSum += other.relativeSum;
	}
}
