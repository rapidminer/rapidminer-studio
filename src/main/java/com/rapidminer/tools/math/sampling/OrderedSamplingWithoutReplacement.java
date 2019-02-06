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
package com.rapidminer.tools.math.sampling;

import com.rapidminer.tools.RandomGenerator;


/**
 * Implements iterative, linear sampling without replacement. The size of the population and the
 * size or fraction of the sample have to be given as parameters.
 * 
 * The algorithm is based upon A.F. Bissell (1986): Ordered Random Selection Without Replacement.
 * In: Applied Statistics, 35 (1), pp. 73-75.
 * 
 * @author Tobias Malbrecht
 */
public class OrderedSamplingWithoutReplacement {

	/**
	 * The random generator which delivers random numbers between 0 and 1.
	 */
	private RandomGenerator randomGenerator;

	/**
	 * Number of those population elements which are still under consideration and from which the
	 * elements are drawn.
	 */
	private int populationCounter;

	/**
	 * Number of elements in the elements under consideration which will be not included in the
	 * sample.
	 */
	private int notRequiredElementsCounter;

	/**
	 * Probability that no element of the first values are drawn from the population.
	 */
	private double probability;

	/**
	 * A random value.
	 */
	private double randomValue;

	/**
	 * Constructor for an absolute number of elements.
	 * 
	 * @param randomGenerator
	 *            A RandomGenerator.
	 * @param populationSize
	 *            The size of the population.
	 * @param sampleSize
	 *            The size of the sample.
	 */
	public OrderedSamplingWithoutReplacement(RandomGenerator randomGenerator, int populationSize, int sampleSize) {
		this.randomGenerator = randomGenerator;
		this.populationCounter = populationSize;
		this.notRequiredElementsCounter = populationSize - sampleSize;
		this.probability = 1;
		this.randomValue = 1 - randomGenerator.nextDouble();
	}

	/**
	 * Constructor for a relative fraction of elements.
	 * 
	 * @param randomGenerator
	 *            A RandomGenerator.
	 * @param populationSize
	 *            The size of the sample relative to the population size.
	 * @param sampleRatio
	 *            The ratio of the sample.
	 */
	public OrderedSamplingWithoutReplacement(RandomGenerator randomGenerator, int populationSize, double sampleRatio) {
		this(randomGenerator, populationSize, (int) Math.round(populationSize * sampleRatio));
	}

	public static int[] getSampledIndices(RandomGenerator randomGenerator, int populationSize, int sampleSize) {
		OrderedSamplingWithoutReplacement sampling = new OrderedSamplingWithoutReplacement(randomGenerator, populationSize,
				sampleSize);
		int[] result = new int[sampleSize];
		int rCounter = 0;
		for (int i = 0; i < populationSize; i++) {
			if (sampling.acceptElement()) {
				result[rCounter++] = i;
			}
		}
		return result;
	}

	/**
	 * Include element in the sample.
	 * 
	 * @return flag whether to include an element in the sample
	 */
	public boolean acceptElement() {
		probability *= ((double) notRequiredElementsCounter) / ((double) populationCounter);
		if (probability > randomValue) {
			populationCounter--;
			notRequiredElementsCounter--;
			return false;
		} else {
			populationCounter--;
			probability = 1;
			randomValue = 1 - randomGenerator.nextDouble();
			return true;
		}
	}
}
