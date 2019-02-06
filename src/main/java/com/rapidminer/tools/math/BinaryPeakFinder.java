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
 * <p>
 * Generates the amplitude and index point of the highest peaks in the series. Find peaks by a
 * divide and conquer algorithm. In each series it tries to find the maximum and then find other
 * peaks left and right from it. Makes sure that values of the current peak are excluded.
 * </p>
 * 
 * @author Ingo Mierswa
 */
public class BinaryPeakFinder implements PeakFinder {

	/** An area of the series which still should be investigated. */
	private static class Area {

		private int start;

		private int end;

		private Area(int start, int end) {
			this.start = start;
			this.end = end;
		}

		@Override
		public String toString() {
			return "start: " + start + ", end: " + end;
		}
	}

	/**
	 * Defines the sloppyness of the algorithm, i.e. how many wrong values are possible until
	 * detecting a new peak.
	 */
	// private int sloppyValues;
	/**
	 * Defines how big the variance of a value can be until he counts as wrong value.
	 */
	// private double toleranceOfVariance;
	/** The min width of a peak. */
	// private int minWidth = 5;
	/** The average of the current series. */
	private double average;

	public BinaryPeakFinder() {}

	// int sloppyValues, int minWidth, double toleranceOfVariance) {
	// this.sloppyValues = sloppyValues;
	// this.toleranceOfVariance = toleranceOfVariance;
	// this.minWidth = minWidth;
	// }

	@Override
	public List<Peak> getPeaks(Peak[] series) {
		// calculate average
		this.average = 0.0;
		for (int i = 0; i < series.length; i++) {
			this.average += series[i].getMagnitude();
		}
		this.average /= series.length;

		LinkedList<Area> areaStack = new LinkedList<Area>();
		Area startArea = new Area(0, series.length);
		areaStack.add(startArea);

		List<Peak> result = new LinkedList<Peak>();
		while (areaStack.size() != 0) {
			Area current = areaStack.removeLast();
			// adds the maximum value of the current area to the peaks
			// collection
			int maxIndex = findMaximum(series, current.start, current.end);

			// adds up to two new areas to the area stack
			int newRightEnd = getLeftEndOfPeak(series, maxIndex, current.start);
			if (newRightEnd > current.start) {
				areaStack.add(new Area(current.start, newRightEnd));
			}

			int newLeftEnd = getRightEndOfPeak(series, maxIndex, current.end);
			if (newLeftEnd < current.end) {
				areaStack.add(new Area(newLeftEnd, current.end));
			}

			// if ((newLeftEnd - newRightEnd) > minWidth) {
			// Peak peak = new Peak(maxIndex,
			// series[maxIndex].getMagnitude(series.length));
			result.add(series[maxIndex]);
			// }
		}

		return result;
	}

	/**
	 * Traverses the series from max to left until startIndex is reached or while the current value
	 * is below average or while the values are still decreasing.
	 */
	private int getLeftEndOfPeak(Peak[] series, int max, int startIndex) {
		int left = max;
		long sloppyCount = 2 + Math.round(Math.sqrt((double) max / (double) series.length) * 5.0d);
		double lastValue;
		boolean ok = false;
		do {
			ok = false;
			lastValue = series[left].getMagnitude();
			for (int i = 0; i < sloppyCount + 1; i++) {
				left--;
				if (left <= startIndex) {
					break;
				} else {
					double tolerance = Math.sqrt((double) left / (double) series.length) + 1.2;
					if ((series[left].getMagnitude() < 2 * average) || (series[left].getMagnitude() < tolerance * lastValue)) {
						ok = true;
						break;
					}
				}
			}
		} while (ok);
		if (left <= startIndex) {
			return startIndex;
		} else {
			return left;
		}
	}

	/**
	 * Traverses the series from max to right until endIndex is reached or while the current value
	 * is below average or while the values are still decreasing.
	 */
	private int getRightEndOfPeak(Peak[] series, int max, int endIndex) {
		int right = max;
		long sloppyCount = 2 + Math.round(Math.sqrt((double) max / (double) series.length) * 5.0d);
		double lastValue;
		boolean ok = false;
		do {
			ok = false;
			lastValue = series[right].getMagnitude();
			for (int i = 0; i < sloppyCount + 1; i++) {
				right++;
				if (right >= endIndex) {
					break;
				} else {
					double tolerance = Math.sqrt((double) right / (double) series.length) + 1.2;
					if ((series[right].getMagnitude() < 2 * average)
							|| (series[right].getMagnitude() < tolerance * lastValue)) {
						ok = true;
						break;
					}
				}
			}
		} while (ok);
		if (right >= endIndex) {
			return endIndex;
		} else {
			return right;
		}
	}

	/**
	 * Finds the index point of the maximum of the series between startIndex and endIndex.
	 */
	private int findMaximum(Peak[] series, int startIndex, int endIndex) {
		int maxIndex = startIndex;
		double maxValue = Double.NEGATIVE_INFINITY;
		for (int i = startIndex; i < endIndex; i++) {
			if (series[i].getMagnitude() > maxValue) {
				maxValue = series[i].getMagnitude();
				maxIndex = i;
			}
		}
		return maxIndex;
	}
}
