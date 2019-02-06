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
package com.rapidminer.operator.learner.functions.kernel.jmysvm.kernel;

import com.rapidminer.operator.learner.functions.kernel.jmysvm.examples.SVMExamples;
import com.rapidminer.tools.Tools;


/**
 * Linear Kernel
 * 
 * @author Stefan Rueping, Ingo Mierswa
 */
public class KernelDot extends Kernel {

	private static final long serialVersionUID = -6384697098131949237L;

	/** Class constructor */
	public KernelDot() {}

	/** Output as String */
	@Override
	public String toString() {
		return ("linear");
	}

	/**
	 * Class constructor
	 * 
	 * @param examples
	 *            Container for the examples.
	 */
	public KernelDot(SVMExamples examples, int cacheSize) {
		init(examples, cacheSize);
	}

	/**
	 * Calculates kernel value of vectors x and y
	 */
	@Override
	public double calculate_K(int[] x_index, double[] x_att, int[] y_index, double[] y_att) {
		return innerproduct(x_index, x_att, y_index, y_att);
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
};
