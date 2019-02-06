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
package com.rapidminer.operator.validation.clustering.exampledistribution;

/**
 * Calculates an item distribution measure by summing up the squares of the fraction of items in
 * each cluster. The result is inverted, thus the higher the value, the better the items are
 * distributed.
 * 
 * @author Michael Wurst
 * 
 */
public class SumOfSquares implements ExampleDistributionMeasure {

	@Override
	public double evaluate(int[] x, int n) {

		double result = 0;
		for (int i = 0; i < x.length; i++) {
			result = result + (((double) x[i]) / n) * (((double) x[i]) / n);
		}

		return result;
	}

}
