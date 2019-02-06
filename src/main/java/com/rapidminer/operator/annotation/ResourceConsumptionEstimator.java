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
package com.rapidminer.operator.annotation;

import com.rapidminer.operator.Operator;


/**
 * Estimates the resource consumption (CPU time and memory usage) of an {@link Operator} based on
 * its current input. The methods in this interface do not take any arguments. Instead, they are
 * backed by an operator and consider its current input.
 * 
 * @author Simon Fischer
 * 
 */
public interface ResourceConsumptionEstimator {

	/**
	 * Returns the estimated number of CPU-cycles. If, for any reason, computation is impossible, -1
	 * should be returned.
	 */
	public long estimateRuntime();

	/**
	 * Returns the estimated number of bytes required when executing this operator. If, for any
	 * reason, computation is impossible, -1 should be returned.
	 */
	public long estimateMemoryConsumption();

	/** Returns the cpu function. */
	public PolynomialFunction getCpuFunction();

	/** Returns the memory function. */
	public PolynomialFunction getMemoryFunction();

}
