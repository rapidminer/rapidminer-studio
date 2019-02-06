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
import com.rapidminer.tools.math.Averagable;


/**
 * Returns the average value of the prediction. This criterion can be used to detect whether a
 * learning scheme predicts nonsense, e.g. always make the same error. This criterion is not
 * suitable for evaluating the performance and should never be used as main criterion. The
 * {@link #getFitness()} method always returns 0.
 * 
 * @author Ingo Mierswa Exp $
 */
public class PredictionAverage extends MeasuredPerformance {

	private static final long serialVersionUID = -5316112625406102611L;

	private double sum;

	private double squaredSum;

	private double count;

	private Attribute labelAttribute;

	private Attribute weightAttribute;

	public PredictionAverage() {}

	public PredictionAverage(PredictionAverage pa) {
		super(pa);
		this.sum = pa.sum;
		this.squaredSum = pa.squaredSum;
		this.count = pa.count;
		this.labelAttribute = (Attribute) pa.labelAttribute.clone();
		if (pa.weightAttribute != null) {
			this.weightAttribute = (Attribute) pa.weightAttribute.clone();
		}
	}

	@Override
	public double getExampleCount() {
		return count;
	}

	@Override
	public void countExample(Example example) {
		double weight = 1.0d;
		if (weightAttribute != null) {
			weight = example.getValue(weightAttribute);
		}
		count += weight;
		double v = example.getLabel();
		if (!Double.isNaN(v)) {
			sum += v * weight;
			squaredSum += v * v * weight * weight;
		}
	}

	@Override
	public double getMikroAverage() {
		return sum / count;
	}

	@Override
	public double getMikroVariance() {
		double avg = getMikroAverage();
		return (squaredSum / count) - avg * avg;
	}

	@Override
	public void startCounting(ExampleSet set, boolean useExampleWeights) throws OperatorException {
		super.startCounting(set, useExampleWeights);
		count = 0;
		sum = 0.0;
		this.labelAttribute = set.getAttributes().getLabel();
		if (useExampleWeights) {
			this.weightAttribute = set.getAttributes().getWeight();
		}
	}

	@Override
	public String getName() {
		return "prediction_average";
	}

	/** Returns 0. */
	@Override
	public double getFitness() {
		return 0.0;
	}

	@Override
	public void buildSingleAverage(Averagable performance) {
		PredictionAverage other = (PredictionAverage) performance;
		this.sum += other.sum;
		this.squaredSum += other.squaredSum;
		this.count += other.count;
	}

	@Override
	public String getDescription() {
		return "This is not a real performance measure, but merely the average of the predicted labels.";
	}
}
