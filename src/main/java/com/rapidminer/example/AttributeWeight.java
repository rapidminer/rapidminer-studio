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
package com.rapidminer.example;

import java.util.Objects;

import com.rapidminer.tools.math.Averagable;


/**
 * Helper class containing the name of an attribute and the corresponding weight.
 * 
 * @author Ingo Mierswa
 */
public class AttributeWeight extends Averagable implements Comparable<AttributeWeight> {

	private static final long serialVersionUID = 4459877599722270416L;

	/** The name of the attribute. */
	private String name;

	/** The weight of the attribute. */
	private double weight;

	/** A counter for building averages. */
	private int counter = 1;

	/** The parent attribute weights. */
	private AttributeWeights weights;

	/** Creates a new attribute weight object. */
	public AttributeWeight(AttributeWeights weights, String name, double weight) {
		this.weights = weights;
		this.name = name;
		this.weight = weight;
	}

	/**
	 * Clone constructor. The name and the weight are deep cloned, the reference to the
	 * AttributeWeights object is only a shallow copy.
	 */
	public AttributeWeight(AttributeWeight attWeight) {
		super(attWeight);
		this.weights = attWeight.weights;
		this.name = attWeight.name;
		this.weight = attWeight.weight;
	}

	/** Returns the name of the attribute. */
	@Override
	public String getName() {
		return name;
	}

	/** Returns the weight of the attribute. */
	public double getWeight() {
		return weight / counter;
	}

	/** Sets the weight of the attribute. */
	public void setWeight(double weight) {
		this.weight = weight;
	}

	/**
	 * Returns the MacroVariance since no other micro variance can be calculated.
	 */
	@Override
	public double getMikroVariance() {
		return getMakroVariance();
	}

	/** Returns the current weight. */
	@Override
	public double getMikroAverage() {
		return getWeight() / counter;
	}

	/**
	 * Compares the weight of this object with the weight of another AttributeWeight object. May
	 * also use the absolute weight.
	 */
	@Override
	public int compareTo(AttributeWeight o) {
		double w1 = weight;
		double w2 = o.weight;

		assert (weights == o.weights);
		if (weights.getWeightType() == AttributeWeights.ABSOLUTE_WEIGHTS) {
			w1 = Math.abs(w1);
			w2 = Math.abs(w2);
		}

		return Double.compare(w1, w2) * weights.getSortingType();
	}

	/** Returns true if both objects have the same name and the same weight. */
	@Override
	public boolean equals(Object o) {
		if (this == o) {
			return true;
		}
		if (o == null || getClass() != o.getClass()) {
			return false;
		}
		AttributeWeight that = (AttributeWeight) o;
		return Objects.equals(name, that.name) &&
				Objects.equals(weight, that.weight);
	}

	@Override
	public int hashCode() {
		return Objects.hash(weight, name);
	}

	/** Builds the sum of weights and counters. */
	@Override
	public void buildSingleAverage(Averagable avg) {
		AttributeWeight other = (AttributeWeight) avg;
		this.weight += other.weight;
		this.counter += other.counter;
	}
}
