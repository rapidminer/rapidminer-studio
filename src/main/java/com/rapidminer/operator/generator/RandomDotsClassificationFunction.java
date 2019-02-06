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
package com.rapidminer.operator.generator;

import java.util.LinkedList;
import java.util.List;

import com.rapidminer.tools.RandomGenerator;


/**
 * Returns a classification function with one major class and a set of dots of the second class.
 *
 * @author Ingo Mierswa
 */
public class RandomDotsClassificationFunction extends ClassificationFunction {

	private static final int NUMBER_OF_POSITIVE_DOTS = 10;

	private static final int NUMBER_OF_NEGATIVE_DOTS = 5;

	private List<Dot> positiveDots = new LinkedList<Dot>();

	private List<Dot> negativeDots = new LinkedList<Dot>();

	@Override
	public void init(RandomGenerator random) {
		positiveDots.clear();
		negativeDots.clear();
		double maxRadius = (upper - lower) / NUMBER_OF_POSITIVE_DOTS;
		for (int i = 0; i < NUMBER_OF_POSITIVE_DOTS; i++) {
			positiveDots.add(new Dot(random.nextDoubleInRange(lower, upper), random.nextDoubleInRange(lower, upper), random
					.nextDoubleInRange(0, maxRadius)));
		}
		for (int i = 0; i < NUMBER_OF_NEGATIVE_DOTS; i++) {
			negativeDots.add(new Dot(random.nextDoubleInRange(lower, upper), random.nextDoubleInRange(lower, upper), random
					.nextDoubleInRange(0, maxRadius)));
		}
	}

	@Override
	public double calculate(double[] att) throws FunctionException {
		if (att.length != 2) {
			throw new FunctionException("Random Dot classification function", "needs 2 attributes!");
		}
		for (Dot pDot : positiveDots) {
			if (pDot.contains(att[0], att[1])) {
				for (Dot nDot : negativeDots) {
					if (nDot.contains(att[0], att[1])) {
						return getLabel().getMapping().mapString("negative");
					}
				}
				return getLabel().getMapping().mapString("positive");
			}
		}
		return getLabel().getMapping().mapString("negative");
	}

	@Override
	public int getMinNumberOfAttributes() {
		return 2;
	}

	@Override
	public int getMaxNumberOfAttributes() {
		return 2;
	}
}
