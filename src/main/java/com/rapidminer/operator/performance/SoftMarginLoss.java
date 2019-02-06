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

import java.util.Iterator;


/**
 * The soft margin loss of a classifier, defined as the average over all 1 - y * f(x).
 * 
 * @author Ingo Mierswa
 */
public class SoftMarginLoss extends MeasuredPerformance {

	private static final long serialVersionUID = -2987795640706342168L;

	/** The value of the margin. */
	private double margin = Double.NaN;

	/** A counter for average building. */
	private double counter = 1;

	/** Clone constructor. */
	public SoftMarginLoss() {}

	public SoftMarginLoss(SoftMarginLoss m) {
		super(m);
		this.margin = m.margin;
		this.counter = m.counter;
	}

	/** Calculates the margin. */
	@Override
	public void startCounting(ExampleSet exampleSet, boolean useExampleWeights) throws OperatorException {
		super.startCounting(exampleSet, useExampleWeights);
		// compute margin
		Iterator<Example> reader = exampleSet.iterator();
		this.margin = 0.0d;
		this.counter = 0.0d;
		Attribute labelAttr = exampleSet.getAttributes().getLabel();
		Attribute weightAttribute = null;
		if (useExampleWeights) {
			weightAttribute = exampleSet.getAttributes().getWeight();
		}
		while (reader.hasNext()) {
			Example example = reader.next();
			String trueLabel = example.getNominalValue(labelAttr);
			double confidence = example.getConfidence(trueLabel);
			double currentMargin = Math.max(0, 1.0d - confidence);

			double weight = 1.0d;
			if (weightAttribute != null) {
				weight = example.getValue(weightAttribute);
			}
			this.margin += currentMargin * weight;

			this.counter += weight;
		}
	}

	/** Does nothing. Everything is done in {@link #startCounting(ExampleSet, boolean)}. */
	@Override
	public void countExample(Example example) {}

	@Override
	public double getExampleCount() {
		return counter;
	}

	@Override
	public double getMikroVariance() {
		return Double.NaN;
	}

	@Override
	public double getMikroAverage() {
		return margin / counter;
	}

	/**
	 * Returns 0.
	 */
	@Override
	public double getMaxFitness() {
		return 0.0d;
	}

	/** Returns the fitness. */
	@Override
	public double getFitness() {
		return -1 * getAverage();
	}

	@Override
	public String getName() {
		return "soft_margin_loss";
	}

	@Override
	public String getDescription() {
		return "The average soft margin loss of a classifier, defined as the average of all 1 - confidences for the correct label.";
	}

	@Override
	public void buildSingleAverage(Averagable performance) {
		SoftMarginLoss other = (SoftMarginLoss) performance;
		this.counter += other.counter;
		this.margin += other.margin;
	}

	/** Returns the super class implementation of toString(). */
	@Override
	public String toString() {
		return super.toString();
	}
}
