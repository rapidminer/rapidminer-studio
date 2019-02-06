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
package com.rapidminer.example.set;

import com.rapidminer.tools.RandomGenerator;

import java.util.Random;


/**
 * Creates a shuffled partition for an example set. This implementation traverses the partition
 * backwards, from the last element up to the second, repeatedly swapping a randomly selected
 * element into the "current position". Elements are randomly selected from the portion of the list
 * that runs from the first element to the current position, inclusive.
 * 
 * @author Ingo Mierswa
 */
public class ShuffledPartitionBuilder extends SimplePartitionBuilder {

	private Random random;

	public ShuffledPartitionBuilder(boolean useLocalRandomSeed, int seed) {
		this.random = RandomGenerator.getRandomGenerator(useLocalRandomSeed, seed);
	}

	/**
	 * Returns a shuffled partition for an example set. Uses the partition delivered by the
	 * superclass and shuffles the elements.
	 */
	@Override
	public int[] createPartition(double[] ratio, int size) {
		int[] part = super.createPartition(ratio, size);

		// Create a random permutation of the generated array by swapping
		// elements
		for (int i = part.length - 1; i >= 1; i--) {
			int swap = random.nextInt(i);
			int dummy = part[i];
			part[i] = part[swap];
			part[swap] = dummy;
		}

		return part;
	}
}
