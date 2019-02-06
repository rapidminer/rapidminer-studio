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
package com.rapidminer.tools.math.optimization.ec.es;

import com.rapidminer.tools.LoggingHandler;

import java.util.Iterator;
import java.util.LinkedList;


/**
 * Implements the 1/5-Rule for dynamic parameter adaption of the variance of a
 * {@link GaussianMutation}. The interval size should have the same size as the changable
 * components, i.e. the number of examples (alphas).
 * 
 * @author Ingo Mierswa
 */
public class VarianceAdaption implements PopulationOperator {

	/**
	 * Waits this number of intervals before variance adaption is applied. Usually 10.
	 */
	private static final int WAIT_INTERVALS = 2;

	/** Used factor for shrinking and enlarging. Usually 0.85. */
	private static final double FACTOR = 0.85;

	/** The mutation. */
	private GaussianMutation mutation = null;

	/** The interval size in which the new variance is calculated. */
	private int intervalSize;

	/** Remember for all positions if an improval was found. */
	private LinkedList<Boolean> successList = new LinkedList<Boolean>();

	/** The logging handler. */
	private LoggingHandler logging;

	/**
	 * The interval size should be as big as the changeable components, i.e. the number of examples
	 * (alphas).
	 */
	public VarianceAdaption(GaussianMutation mutation, int intervalSize, LoggingHandler logging) {
		this.mutation = mutation;
		this.intervalSize = intervalSize;
		this.logging = logging;
	}

	@Override
	public void operate(Population population) {
		if (population.getGenerationsWithoutImprovement() < 2) {
			successList.add(true);
		} else {
			successList.add(false);
		}

		if (population.getGeneration() >= WAIT_INTERVALS * intervalSize) {
			successList.removeFirst();
			if ((population.getGeneration() % intervalSize) == 0) {
				int successCount = 0;
				Iterator<Boolean> i = successList.iterator();
				while (i.hasNext()) {
					if (i.next()) {
						successCount++;
					}
				}

				if (((double) successCount / (double) (WAIT_INTERVALS * intervalSize)) < 0.2) {
					double[] sigma = mutation.getSigma();
					for (int s = 0; s < sigma.length; s++) {
						sigma[s] *= FACTOR;
					}
					mutation.setSigma(sigma);
					logging.log("Applying 1/5-rule: shrink variance!");
				} else {
					double[] sigma = mutation.getSigma();
					for (int s = 0; s < sigma.length; s++) {
						sigma[s] /= FACTOR;
					}
					mutation.setSigma(sigma);
					logging.log("Applying 1/5-rule: enlarge variance!");
				}
			}
		}
	}
}
