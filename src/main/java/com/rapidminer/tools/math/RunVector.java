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

import com.rapidminer.operator.ResultObjectAdapter;
import com.rapidminer.tools.Tools;

import java.util.ArrayList;
import java.util.Iterator;


/**
 * Collects the average vectors of a run. The averagables can be averaged by using
 * {@link #average()}.
 * 
 * @author Ralf Klinkenberg, Ingo Mierswa
 */
public class RunVector extends ResultObjectAdapter {

	private static final long serialVersionUID = -5481280692066966385L;

	/** List of average vectors. */
	private ArrayList<AverageVector> vectorList = new ArrayList<AverageVector>();

	/** Adds a new average vector. */
	public void addVector(AverageVector av) {
		vectorList.add(av);
	}

	/** Returns the average vector with index i. */
	public AverageVector getVector(int index) {
		return vectorList.get(index);
	}

	/** Returns all average vectors as list. */
	public ArrayList<AverageVector> getVectorList() {
		return vectorList;
	}

	/** Returns the number of average vectors. */
	public int size() {
		return vectorList.size();
	}

	/**
	 * Calculates the mean value of the averagables of the average vectors and returns an average
	 * vector which contains for each averagable the mean value.
	 */
	public AverageVector average() {
		try {
			AverageVector output = (AverageVector) getVector(0).clone();
			for (int i = 1; i < size(); i++) {
				AverageVector av = getVector(i);
				for (int j = 0; j < av.size(); j++) {
					output.getAveragable(j).buildAverage(av.getAveragable(j));
				}
			}
			return output;
		} catch (CloneNotSupportedException e) {
			throw new RuntimeException("Clone of average vector is not supported: " + e.getMessage(), e);
		}
	}

	@Override
	public String toString() {
		StringBuffer result = new StringBuffer("RunVector:" + Tools.getLineSeparator());
		Iterator<AverageVector> i = vectorList.iterator();
		while (i.hasNext()) {
			result.append(i.next() + Tools.getLineSeparator());
		}
		return result.toString();
	}

	/**
	 * returns a <tt>String</tt> containing a time series of the values of each averagable and the
	 * average of all averagables over all runs.
	 */
	@Override
	public String toResultString() {
		StringBuffer result = new StringBuffer("");
		AverageVector averageVector = average(); // compute the averages

		// ---- print the series of all performance criteria ----
		for (int averagableIndex = 0; averagableIndex < averageVector.size(); averagableIndex++) {
			result.append("Time series of averagable '" + ((this.getVector(0)).getAveragable(averagableIndex)).getName()
					+ "':");
			for (int timeIndex = 0; timeIndex < this.size(); timeIndex++) {
				AverageVector currentAveragables = getVector(timeIndex);
				Averagable averagable = currentAveragables.getAveragable(averagableIndex);
				result.append("  " + averagable.getAverage());
			}
			result.append(Tools.getLineSeparator());
		}

		// ---- print the overall average (and variance) ----
		for (int i = 0; i < averageVector.size(); i++) {
			Averagable averagable = averageVector.getAveragable(i);
			result.append("  Average of averagable '" + averagable.getName() + "':  " + averagable.getAverage());
			if (averagable.getVariance() >= 0) {
				result.append("  (" + averagable.getVariance() + ")");
			}
			result.append(Tools.getLineSeparator());
		}
		result.append(Tools.getLineSeparator());
		return result.toString();
	}

	public String getExtension() {
		return "rvc";
	}

	public String getFileDescription() {
		return "run vector";
	}

}
