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
package com.rapidminer.tools.math.function.window;

/**
 * A gaussian window function.
 * 
 * @author Tobias Malbrecht
 */
public class GaussianWindowFunction extends WindowFunction {

	private double sigma;

	public GaussianWindowFunction(Integer width, double sigma) {
		super(width);
		this.sigma = sigma;
	}

	public GaussianWindowFunction(Integer width, Integer justification) {
		this(width, justification, 0.5d);
	}

	public GaussianWindowFunction(Integer width, Integer justification, double sigma) {
		super(width, justification);
		this.sigma = sigma;
	}

	@Override
	protected double getValue(int width, int n) {
		double term = (2 * n / (sigma * (width - 1))) - 1 / sigma;
		return Math.exp(-0.5 * term * term);
	}
}
