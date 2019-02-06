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
 * This class provides a sampling sequence, where each element will have the given probability to be
 * part of the sample.
 * 
 * @author Sebastian Land
 */
public class ProbabilitySamplingSequenceGenerator extends SamplingSequenceGenerator {

	private double fraction;

	public ProbabilitySamplingSequenceGenerator(double fraction, RandomGenerator random) {
		super(random);
		this.fraction = fraction;
	}

	@Override
	public boolean useNext() {
		return random.nextDouble() < fraction;
	}
}
