/**
 * Copyright (C) 2001-2017 by RapidMiner and the contributors
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
package com.rapidminer.operator.generator;

/**
 * The label is positive if the sum of all arguments is positive.
 * 
 * @author Ingo Mierswa
 */
public class SumClassificationFunction extends ClassificationFunction {

	@Override
	public double calculate(double[] args) {
		double sum = 0.0d;
		for (int i = 0; i < args.length; i++) {
			sum += args[i];
		}
		return (sum > 0 ? getLabel().getMapping().mapString("positive") : getLabel().getMapping().mapString("negative"));
	}
}
