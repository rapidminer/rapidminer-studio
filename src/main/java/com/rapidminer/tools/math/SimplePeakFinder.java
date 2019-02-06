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
package com.rapidminer.tools.math;

import java.util.LinkedList;
import java.util.List;


/**
 * This simple implementation returns a peak if the specified number of neighbors is smaller than
 * the current value.
 * 
 * @author Ingo Mierswa
 */
public class SimplePeakFinder implements PeakFinder {

	private int numberOfNeighbours = 5;

	public SimplePeakFinder(int neighbours) {
		this.numberOfNeighbours = neighbours;
	}

	/** Returns a list with peaks. */
	@Override
	public List<Peak> getPeaks(Peak[] series) {
		List<Peak> result = new LinkedList<Peak>();
		for (int i = 0; i < series.length; i++) {
			if (isPeak(series, i)) {
				result.add(series[i]);
			}
		}
		return result;
	}

	/**
	 * Returns true if the value for index is an extremum of the given type between the given
	 * numbers of neighbours.
	 */
	private boolean isPeak(Peak[] series, int index) {
		boolean ok = false;
		int current = index;
		int okValues = 0;

		// values to left
		while (!ok) {
			current--;
			if (current < 0) {
				return false;
			}
			if (!isOk(series, current, index)) {
				return false;
			}
			okValues++;
			if (okValues > numberOfNeighbours) {
				ok = true;
			}
		}
		ok = false;
		current = index;
		okValues = 0;
		// values to right
		while (!ok) {
			current++;
			if (current >= series.length) {
				return false;
			}
			if (!isOk(series, current, index)) {
				return false;
			}
			okValues++;
			if (okValues > numberOfNeighbours) {
				ok = true;
			}
		}
		return true;
	}

	/**
	 * In the minimum case this method returns true, if the current value is bigger than the index
	 * value.
	 */
	private boolean isOk(Peak[] series, int current, int index) {
		if (series[current].getMagnitude() <= series[index].getMagnitude()) {
			return false;
		} else {
			return true;
		}
	}
}
