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
package com.rapidminer.tools.math.optimization;

import com.rapidminer.operator.OperatorException;
import com.rapidminer.operator.performance.PerformanceVector;


/**
 * General interface for all optimization methods. The method {@link #optimize()} should be invoked
 * to start the optimization process. The optimal result can be queried by the method
 * {@link #getBestValuesEver()}. The other methods of this interface can be used to support logging
 * or plotting.
 * 
 * @author Ingo Mierswa
 */
public interface Optimization {

	/**
	 * Should be invoked to start optimization. Since the optimization can use other (inner)
	 * operators to support fitness evaluation this method is allowed to throw OperatorExceptions.
	 */
	public void optimize() throws OperatorException;

	/** Returns the current generation. */
	public int getGeneration();

	/** Returns the best fitness in the current generation. */
	public double getBestFitnessInGeneration();

	/** Returns the best fitness ever. */
	public double getBestFitnessEver();

	/** Returns the best performance vector ever. */
	public PerformanceVector getBestPerformanceEver();

	/**
	 * Returns the best values ever. Use this method after optimization to get the best result.
	 */
	public double[] getBestValuesEver();
}
