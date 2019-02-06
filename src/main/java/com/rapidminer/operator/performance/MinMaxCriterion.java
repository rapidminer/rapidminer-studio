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

import com.rapidminer.example.Example;
import com.rapidminer.tools.math.Averagable;


/**
 * This criterion should be used as wrapper around other performance criteria (see
 * {@link MinMaxWrapper}). Instead of averages this criterion builds the minimum of the fitness of
 * the criteria which should be averaged. Maximizing the minimum instead of the average could lead
 * to a more robust behaviour and generalization capacity. Furthermore, this criterion can be build
 * arbitrarily weighted linear combinations of both the minimum and the normal average.
 * 
 * @author Ingo Mierswa
 */
public class MinMaxCriterion extends MeasuredPerformance {

	private static final long serialVersionUID = 1739979152946602515L;

	private MeasuredPerformance delegate;

	private double fitness = Double.POSITIVE_INFINITY;

	private double fitnessSum = 0.0d;

	private double value = Double.NaN;

	private double weight = 1.0d;

	private int counter = 0;

	/** Necessary for newInstance() during loading. */
	public MinMaxCriterion() {}

	public MinMaxCriterion(MeasuredPerformance delegate, double weight) {
		this.delegate = delegate;
		this.weight = weight;
		if (!Double.isNaN(delegate.getFitness())) {
			this.fitness = delegate.getFitness();
			this.value = delegate.getMikroAverage();
			counter++;
		}
	}

	public MinMaxCriterion(MinMaxCriterion mmc) {
		super(mmc);
		this.delegate = mmc.delegate;
		this.fitness = mmc.fitness;
		this.fitnessSum = mmc.fitnessSum;
		this.counter = mmc.counter;
		this.weight = mmc.weight;
	}

	@Override
	public double getExampleCount() {
		return delegate.getExampleCount();
	}

	/** Counts a single example by invoking the delegates method. */
	@Override
	public void countExample(Example example) {
		delegate.countExample(example);
	}

	/** Returns a description of the performance criterion. */
	@Override
	public String getDescription() {
		return delegate.getDescription();
	}

	@Override
	public String getName() {
		return delegate.getName();
	}

	@Override
	public double getMikroAverage() {
		return weight * value + (1.0d - weight) * delegate.getMikroAverage();
	}

	@Override
	public double getFitness() {
		return weight * fitness + (1.0d - weight) * (fitnessSum / counter);
	}

	@Override
	public double getMikroVariance() {
		return Double.NaN;
	}

	@Override
	public void buildSingleAverage(Averagable avg) {
		MinMaxCriterion mmc = (MinMaxCriterion) avg;
		double currentFitness = mmc.delegate.getFitness();
		if (currentFitness < this.fitness) {
			this.fitness = currentFitness;
			this.value = mmc.delegate.getMikroAverage();
		}
		this.fitnessSum += currentFitness;
		counter++;
		delegate.buildAverage(mmc.delegate);
	}
}
