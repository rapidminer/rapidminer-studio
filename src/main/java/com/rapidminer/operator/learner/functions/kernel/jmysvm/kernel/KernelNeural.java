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

import com.rapidminer.tools.Tools;


/**
 * Neural Kernel
 * 
 * @author Stefan Rueping, Ingo Mierswa
 */
public class KernelNeural extends Kernel {

	private static final long serialVersionUID = 3862702323530107467L;

	double a = 1.0;

	double b = 0.0;

	/**
	 * Class constructor
	 */
	public KernelNeural() {};

	/**
	 * Output as String
	 */
	@Override
	public String toString() {
		return ("neural(" + a + "," + b + ")");
	};

	public void setParameters(double a, double b) {
		this.a = a;
		this.b = b;
	}

	/**
	 * Calculates kernel value of vectors x and y
	 */
	@Override
	public double calculate_K(int[] x_index, double[] x_att, int[] y_index, double[] y_att) {
		// K = tanh(a(x*y)+b)
		double prod = a * innerproduct(x_index, x_att, y_index, y_att) + b;
		double e1 = Math.exp(prod);
		double e2 = Math.exp(-prod);
		return ((e1 - e2) / (e1 + e2));
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

		String e1 = "exp(" + result.toString() + ")";
		String e2 = "exp(-1 * (" + result.toString() + "))";

		return "((" + e1 + " - " + e2 + ") / (" + e1 + " + " + e2 + "))";
	}
};
