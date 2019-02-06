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
package com.rapidminer.tools.math.kernels;

import com.rapidminer.tools.Tools;


/**
 * Returns the simple inner product of both examples.
 * 
 * @author Ingo Mierswa
 */
public class DotKernel extends Kernel {

	private static final long serialVersionUID = -7737835520088841652L;

	@Override
	public int getType() {
		return KERNEL_DOT;
	}

	/** Subclasses must implement this method. */
	@Override
	public double calculateDistance(double[] x1, double[] x2) {
		return innerProduct(x1, x2);
	}

	@Override
	public String getDistanceFormula(double[] x, String[] attributeConstructions) {
		StringBuffer result = new StringBuffer();
		boolean first = true;
		for (int i = 0; i < x.length; i++) {
			double value = x[i];
			if (!Tools.isZero(value)) {
				if (value < 0.0d) {
					if (first) {
						result.append("-" + Math.abs(value) + " * " + attributeConstructions[i]);
					} else {
						result.append(" - " + Math.abs(value) + " * " + attributeConstructions[i]);
					}
				} else {
					if (first) {
						result.append(value + " * " + attributeConstructions[i]);
					} else {
						result.append(" + " + value + " * " + attributeConstructions[i]);
					}
				}
				first = false;
			}
		}
		return result.toString();
	}

	@Override
	public String toString() {
		return "Dot Kernel";
	}
}
