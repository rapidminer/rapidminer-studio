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
 * This class provides a relative sampling sequence. The given fraction will be part of the sample.
 * This is achieved by utilizing the {@link AbsoluteSamplingSequenceGenerator} with the absolute
 * number derived from fraction and source size.
 * 
 * @author Sebastian Land
 */
public class RelativeSamplingSequenceGenerator extends AbsoluteSamplingSequenceGenerator {

	public RelativeSamplingSequenceGenerator(int source, double fraction, RandomGenerator random) {
		super(source, (int) (source * fraction), random);
	}
}
