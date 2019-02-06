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

/**
 * Helper class containing all information about a group, i.e. the number of elements, the mean and
 * variance value.
 * 
 * @author Ingo Mierswa
 */
public class TestGroup {

	private double number;

	private double mean;

	private double variance;

	public TestGroup(double number, double mean, double variance) {
		this.number = number;
		this.mean = mean;
		this.variance = variance;
	}

	public double getNumber() {
		return this.number;
	}

	public double getMean() {
		return this.mean;
	}

	public double getVariance() {
		return this.variance;
	}
}
