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

import com.rapidminer.tools.math.Averagable;


/**
 * This class is used to store estimated performance values <em>before</em> or even <em>without</em>
 * the performance test is actually done using a test set. Please note that this type of performance
 * cannot be used to calculate average values, i.e. it will lead to an error if an
 * EstimatedPerformance criterion is used in a validation operator.
 * 
 * @author Simon Fischer, Ingo Mierswa
 */
public class EstimatedPerformance extends PerformanceCriterion {

	private static final long serialVersionUID = 2451922700464241674L;

	private String name;

	private double value;

	private double exampleCount = 1;

	private boolean minimizeForFitness;

	/** Clone constructor. */
	public EstimatedPerformance() {}

	public EstimatedPerformance(EstimatedPerformance ep) {
		super(ep);
		this.name = ep.name;
		this.minimizeForFitness = ep.minimizeForFitness;
		this.exampleCount = ep.exampleCount;
		this.value = ep.value;
	}

	/**
	 * Constructs a new extimated performance criterion.
	 * 
	 * @param name
	 *            Human readable name
	 * @param value
	 *            The Value of the performance criterion
	 * @param numberOfExamples
	 *            The number of examples used to estimate this value.
	 * @param minimizeForFitness
	 *            Indicates whether or not (-1) * value should be used as fitness value.
	 */
	public EstimatedPerformance(String name, double value, int numberOfExamples, boolean minimizeForFitness) {
		this.name = name;
		setMikroAverage(value, numberOfExamples);
		this.minimizeForFitness = minimizeForFitness;
	}

	@Override
	public double getExampleCount() {
		return exampleCount;
	}

	@Override
	public double getMikroVariance() {
		return Double.NaN;
	}

	/**
	 * Sets the value of this estimated performance criterion.
	 * 
	 * @param value
	 *            The Value of the performance criterion
	 * @param numberOfExamples
	 *            The number of examples used to estimate this value. It is used for calculating the
	 *            average.
	 */
	public void setMikroAverage(double value, double numberOfExamples) {
		this.value = value * numberOfExamples;
		this.exampleCount = numberOfExamples;
	}

	@Override
	public double getMikroAverage() {
		return value / exampleCount;
	}

	/** Returns the fitness. */
	@Override
	public double getFitness() {
		if (minimizeForFitness) {
			return (-1) * getAverage();
		} else {
			return getAverage();
		}
	}

	/**
	 * Returns the name of this estimated performance criterion, which can be set using
	 * <tt>setName()</tt>.
	 * 
	 * @return The name.
	 */
	@Override
	public String getName() {
		return name;
	}

	@Override
	public String getDescription() {
		return "The estimated performance '" + name + "'";
	}

	@Override
	public void buildSingleAverage(Averagable performance) {
		EstimatedPerformance other = (EstimatedPerformance) performance;
		this.exampleCount += other.exampleCount;
		this.value += other.value;
	}
}
