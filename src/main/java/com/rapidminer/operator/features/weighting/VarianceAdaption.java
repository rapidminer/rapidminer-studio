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
package com.rapidminer.operator.features.weighting;

import com.rapidminer.operator.features.Population;
import com.rapidminer.operator.features.PopulationOperator;

import java.util.Iterator;
import java.util.LinkedList;


/**
 * Implements the 1/5-Rule for dynamic parameter adaption of the variance of a
 * {@link WeightingMutation}.
 * 
 * @author Ingo Mierswa Exp $
 */
public class VarianceAdaption implements PopulationOperator {

	/** The weighting mutation. */
	private WeightingMutation weightingMutation = null;

	/** The interval size in which the new variance is calculated. */
	private int intervalSize = 400;

	/** Remember for all positions if an improval was found. */
	private LinkedList<Boolean> successList = new LinkedList<Boolean>();

	/**
	 * The interval size should be as big as the changeable components, i.e. the number of
	 * attributes.
	 */
	public VarianceAdaption(WeightingMutation weightingMutation, int intervalSize) {
		this.weightingMutation = weightingMutation;
		this.intervalSize = intervalSize;
	}

	/** The default implementation returns true for every generation. */
	@Override
	public boolean performOperation(int generation) {
		return true;
	}

	@Override
	public void operate(Population population) {
		if (population.getGenerationsWithoutImproval() < 2) {
			successList.add(true);
		} else {
			successList.add(false);
		}

		if (population.getGeneration() >= 10 * intervalSize) {
			successList.removeFirst();
			if ((population.getGeneration() % intervalSize) == 0) {
				int successCount = 0;
				Iterator<Boolean> i = successList.iterator();
				while (i.hasNext()) {
					if (i.next()) {
						successCount++;
					}
				}

				if ((successCount / (10.0d * intervalSize)) < 0.2) {
					weightingMutation.setVariance(weightingMutation.getVariance() * 0.85d);
				} else {
					weightingMutation.setVariance(weightingMutation.getVariance() / 0.85d);
				}
			}
		}
	}
}
