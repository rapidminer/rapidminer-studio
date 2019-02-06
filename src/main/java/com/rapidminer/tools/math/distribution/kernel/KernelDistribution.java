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
package com.rapidminer.tools.math.distribution.kernel;

import com.rapidminer.tools.math.distribution.ContinuousDistribution;
import com.rapidminer.tools.math.distribution.EmpiricalDistribution;


/**
 * A kernel based empirical distribution.
 * 
 * @author Tobias Malbrecht
 */
public abstract class KernelDistribution extends ContinuousDistribution implements EmpiricalDistribution {

	public static final long serialVersionUID = -3298190542815818L;

	protected static final double DEFAULT_BANDWIDTH = 0.2;

	@Override
	public String getAttributeName() {
		return null;
	}

	@Override
	public int getNumberOfParameters() {
		return 0;
	}

	@Override
	public String getParameterName(int index) {
		return null;
	}

	@Override
	public double getParameterValue(int index) {
		return Double.NaN;
	}
}
