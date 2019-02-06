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
package com.rapidminer.operator.learner.associations.gsp;

/**
 * This class holds informations about the start and end time of a set of transactions. Please note,
 * that the set itself isn't saved, and isn't needed.
 * 
 * @author Sebastian Land
 */
public class TransactionSet {

	private double startTime = Double.POSITIVE_INFINITY;
	private double endTime = Double.NEGATIVE_INFINITY;

	public double getEndTime() {
		return endTime;
	}

	public double getStartTime() {
		return startTime;
	}

	public void addTimeOfTransaction(double time) {
		if (time > this.endTime) {
			this.endTime = time;
		}
		if (time < this.startTime) {
			this.startTime = time;
		}
	}

	@Override
	public String toString() {
		return startTime + " - " + endTime;
	}

	public void reset() {
		startTime = Double.POSITIVE_INFINITY;
		endTime = Double.NEGATIVE_INFINITY;
	}
}
