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
package com.rapidminer.operator.preprocessing.sampling.sequences;

import com.rapidminer.tools.RandomGenerator;


/**
 * This sampling sequence guarantees that the resulting sequence will contain either all elements or
 * exactly as much as given as target.
 * 
 * @author Sebastian Land
 */
public class AbsoluteSamplingSequenceGenerator extends SamplingSequenceGenerator {

	private int toCome;

	private int toAccept;

	public AbsoluteSamplingSequenceGenerator(int source, int target, RandomGenerator random) {
		super(random);
		this.toCome = source;
		this.toAccept = target;
	}

	@Override
	public boolean useNext() {
		boolean accept = (random.nextInt(toCome) + 1) <= toAccept;
		if (accept) {
			toAccept--;
		}
		toCome--;
		return accept;
	}
}
