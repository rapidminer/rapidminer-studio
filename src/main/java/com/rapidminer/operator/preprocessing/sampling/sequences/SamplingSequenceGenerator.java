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
 * This is an abstract super class of all sampling sequence generators. Subclasses of this class
 * will return a sequence of true/false values to indicate that the next example to come should be
 * part of the sample or not.
 * 
 * @author Sebastian Land
 */
public abstract class SamplingSequenceGenerator {

	protected RandomGenerator random;

	protected SamplingSequenceGenerator(RandomGenerator random) {
		this.random = random;
	}

	/**
	 * This method has to be overridden. Subclasses must implement this method, so that it returns
	 * true if the next example should be part of the sample or no otherwise.
	 */
	public abstract boolean useNext();
}
