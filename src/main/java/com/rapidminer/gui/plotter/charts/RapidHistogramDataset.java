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
package com.rapidminer.gui.plotter.charts;

import org.jfree.data.statistics.HistogramDataset;


/**
 * This is a helper class for applying a logarithmic transformation on histogram datasets.
 * 
 * @author Ingo Mierswa
 * @deprecated since 9.2.0
 */
@Deprecated
public class RapidHistogramDataset extends HistogramDataset {

	private static final long serialVersionUID = 1144347275132559243L;

	private boolean isLog = false;

	public RapidHistogramDataset(boolean isLog) {
		this.isLog = isLog;
	}

	@Override
	public Number getY(int series, int item) {
		Number superValue = super.getY(series, item);
		if (isLog) {
			double superDoubleValue = superValue.doubleValue();
			if (superDoubleValue > 1.0d) {
				return Math.log10(superValue.doubleValue());
			} else {
				return 0.01;
			}
		} else {
			return superValue;
		}
	}
}
