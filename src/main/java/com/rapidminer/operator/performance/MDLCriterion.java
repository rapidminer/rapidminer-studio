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
import com.rapidminer.example.ExampleSet;
import com.rapidminer.example.set.AttributeWeightedExampleSet;
import com.rapidminer.operator.OperatorException;
import com.rapidminer.tools.math.Averagable;


/**
 * Measures the length of an example set (i.e. the number of attributes).
 * 
 * @author Ingo Mierswa
 */
public class MDLCriterion extends MeasuredPerformance {

	private static final long serialVersionUID = -5023462349084083154L;

	/** The possible optimization directions. */
	public static final String[] DIRECTIONS = { "minimization", "maximization" };

	/**
	 * Indicates that the fitness should be higher for smaller numbers of features.
	 */
	public static final int MINIMIZATION = 0;

	/**
	 * Indicates that the fitness should be higher for larger numbers of features.
	 */
	public static final int MAXIMIZATION = 1;

	/** The length of this example set. */
	private int length;

	/** A counter for average building. */
	private double counter = 1;

	/**
	 * Indicates if the fitness should be higher or smaller depending on the number of features.
	 */
	private int direction = MINIMIZATION;

	public MDLCriterion() {}

	public MDLCriterion(int direction) {
		this();
		this.direction = direction;
	}

	public MDLCriterion(MDLCriterion mdl) {
		super(mdl);
		this.length = mdl.length;
		this.counter = mdl.counter;
		this.direction = mdl.direction;
	}

	@Override
	public String getName() {
		return "number_of_attributes";
	}

	@Override
	public String getDescription() {
		return "Measures the length of an example set (i.e. the number of attributes).";
	}

	@Override
	public void startCounting(ExampleSet eSet, boolean useExampleWeights) throws OperatorException {
		super.startCounting(eSet, useExampleWeights);
		if (eSet instanceof AttributeWeightedExampleSet) {
			this.length = ((AttributeWeightedExampleSet) eSet).getNumberOfUsedAttributes();
		} else {
			this.length = eSet.getAttributes().size();
		}
	}

	@Override
	public double getExampleCount() {
		return counter;
	}

	@Override
	public void countExample(Example example) {}

	@Override
	public double getFitness() {
		switch (direction) {
			case MINIMIZATION:
				return (-1) * (double) length / counter;
			case MAXIMIZATION:
				return length / counter;
			default:
				return Double.NaN; // cannot happen
		}
	}

	@Override
	public double getMikroAverage() {
		return (length) / counter;
	}

	@Override
	public double getMikroVariance() {
		return Double.NaN;
	}

	@Override
	public void buildSingleAverage(Averagable averagable) {
		MDLCriterion other = (MDLCriterion) averagable;
		this.length += other.length;
		this.counter++;
	}
}
