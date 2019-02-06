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
 * The margin of a classifier, defined as the minimal confidence for the correct label.
 * 
 * @author Martin Scholz, Ingo Mierswa
 */
public class Margin extends MeasuredPerformance {

	private static final long serialVersionUID = -2987795640706342168L;

	/** The value of the criterion. */
	private double margin = Double.NaN;

	private double counter = 1.0d;

	/** Clone constructor. */
	public Margin() {}

	public Margin(Margin m) {
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
		this.margin = 1.0d;
		Attribute labelAttr = exampleSet.getAttributes().getLabel();
		while (reader.hasNext()) {
			Example example = reader.next();
			String trueLabel = example.getNominalValue(labelAttr);
			double confidence = example.getConfidence(trueLabel);
			this.margin = Math.min(margin, confidence);
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

	/** Returns the fitness. */
	@Override
	public double getFitness() {
		return getAverage();
	}

	@Override
	public String getName() {
		return "margin";
	}

	@Override
	public String getDescription() {
		return "The margin of a classifier, defined as the minimal confidence for the correct label.";
	}

	@Override
	public void buildSingleAverage(Averagable performance) {
		Margin other = (Margin) performance;
		this.margin += other.margin;
		this.counter += other.counter;
	}

	/** Returns the super class implementation of toString(). */
	@Override
	public String toString() {
		return super.toString();
	}
}
