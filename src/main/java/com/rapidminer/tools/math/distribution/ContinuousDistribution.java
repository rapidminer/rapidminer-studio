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
package com.rapidminer.tools.math.distribution;

import com.rapidminer.tools.Tools;


/**
 * The distribution for a continous variable.
 * 
 * @author Tobias Malbrecht
 */
public abstract class ContinuousDistribution implements Distribution {

	/**
	 * 
	 */
	private static final long serialVersionUID = 6775492269986383673L;

	@Override
	public final boolean isDiscrete() {
		return false;
	}

	@Override
	public final boolean isContinuous() {
		return true;
	}

	@Override
	public abstract double getProbability(double x);

	/**
	 * This method returns a lower bound of values. This bound should be given by the distributions
	 * tail, for example bounds should contain 95% interval. Nominal distributions should return
	 * NaN.
	 */
	public abstract double getLowerBound();

	/**
	 * This method returns an upper bound of possible values. This bound should be given by the
	 * distributions tail, for example bounds should contain 95% interval. Nominal distributions
	 * should return NaN.
	 */
	public abstract double getUpperBound();

	@Override
	public String mapValue(double value) {
		return Tools.formatNumber(value);
	}
}
