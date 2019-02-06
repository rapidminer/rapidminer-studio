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
package com.rapidminer.operator.learner.functions.kernel.jmysvm.optimizer;

/**
 * A quadratic optimization problem.
 * 
 * @author Stefan Rueping
 */
public abstract class QuadraticProblem {

	// Public variables that describe the quadratic problem
	protected int n; // number of variables

	static int m = 1; // number of linear constraints, 1 for now

	public double[] c;

	public double[] H; // c' * x + 1/2 x' * H * x -> min

	public double[] A;

	public double[] b; // A * x = b

	public double[] l;

	public double[] u; // l <= x <= u

	public double[] x;

	public double max_allowed_error;

	public QuadraticProblem() {
		n = 0;
		lambda_eq = 0.0d;
	};

	public void set_n(int new_n) {
		n = new_n;
		c = new double[n];
		H = new double[n * n];
		A = new double[n];
		b = new double[n];
		l = new double[n];
		u = new double[n];
		x = new double[n];
	};

	public int get_n() {
		return (n);
	};

	public double lambda_eq;

	protected abstract void calc_lambda_eq();

	public abstract int solve();
};
