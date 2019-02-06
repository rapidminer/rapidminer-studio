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

import com.rapidminer.tools.math.distribution.EmpiricalNormalDistribution;


/**
 * A normal kernel.
 *
 * @author Tobias Malbrecht
 */
public class NormalKernel extends EmpiricalNormalDistribution {

	public static final long serialVersionUID = -320543538793918L;

	private double minimumBandwidth;

	public NormalKernel(double minimumBandwidth) {
		super();
		this.minimumBandwidth = minimumBandwidth;
		standardDeviation = minimumBandwidth;
	}

	@Override
	protected void updateDistributionProperties() {
		if (recentlyUpdated) {
			mean = sum / totalWeightSum;
			double stdDev = totalWeightSum > 1 ? Math.sqrt((squaredSum - sum * sum / totalWeightSum) / (totalWeightSum - 1))
					: Double.MIN_VALUE;
			standardDeviation = stdDev > minimumBandwidth ? stdDev : minimumBandwidth;
			recentlyUpdated = false;
		}
	}
}
