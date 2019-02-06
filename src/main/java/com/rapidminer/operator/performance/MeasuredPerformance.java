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
package com.rapidminer.operator.performance;

import com.rapidminer.example.Example;
import com.rapidminer.example.ExampleSet;
import com.rapidminer.operator.OperatorException;
import com.rapidminer.tools.math.Averagable;


/**
 * <p>
 * Superclass for performance citeria that are actually measured (not estimated). These criteria can
 * be calculated by the operator {@link PerformanceEvaluator}.
 * </p>
 * 
 * <p>
 * Beside the methods from {@link Averagable} and {@link PerformanceCriterion} this class must
 * implement some additonal methods. Please note that the actual measurement could be done in either
 * {@link #countExample(Example)} or in {@link #startCounting(ExampleSet)} but is does not need to
 * be performed in both. In all cases where your measure needs to see the entire example set at one
 * time, the actual computation of performance is done in startCounting and the countExample method
 * returns without doing anything. For a performance measure that will be computed incrementally,
 * you would do initialization in startCounting and updating in countExample. In all cases where an
 * incremental calculation is possible the calculation and update in {@link #countExample(Example)}
 * is preferred since this would allow the easy integration of this criterion for incremental
 * scenarios (although this is currently not supported).
 * </p>
 * 
 * <p>
 * IMPORTANT: Please note that your criterion need a public constructor without arguments in order
 * to create this object via reflection. It is also possible to have a one-argument string
 * constructor taking a user defined parameter string. This is of course not necessary if your
 * criterion does not handle any parameters. Another public one-argument constructor taking an
 * object of the same class must also be provided (clone constructor). This constructor is used to
 * copy the internal settings of a different instance of your measure into the current instance. If
 * you were computing mean and the argument to cloneAverageable was an instance with mean nu and
 * sample count n, you would set the mean of the current instance to nu and the sample count of the
 * current instance to n. Don't forget to invoke the super constructor first in your clone
 * constructor.
 * </p>
 * 
 * <p>
 * The method {@link #getMikroAverage()} should return the value computed in startCounting or the
 * current value calculated by countExample or Double.NaN if the value was not yet known, or was not
 * well defined). If you can calculate a value for micro variance you should return this value in
 * {@link #getMikroVariance()}. If this is not possible simply return Double.NaN.
 * </p>
 * 
 * <p>
 * The method {@link #getExampleCount()} returns the number of observations used to calculate the
 * measure. This value is usually used for building micro averages (in the method
 * {@link #buildSingleAverage(Averagable)} and might be important for calculating significance
 * tests.
 * </p>
 * 
 * <p>
 * If your measure is averageable, you use {@link #buildSingleAverage(Averagable)} to fold another
 * copy of your measure into the current copy. Suppose you were implementing sample mean. The
 * current instance would have a mean mu and sample count m. If buildSingleAverage were invoked on
 * another instance with mean nu and sample count n, you would update the current instance to mean
 * (m*mu+n*nu)/(m+n) and count m+n. This is actually the computation of the micro average.
 * <p>
 * 
 * <p>
 * In some cases the criterion is better if the value returned by {@link #getAverage()} is smaller.
 * In order to support optimizations, the method {@link #getFitness()} should return the result of
 * {@link #getAverage()} (not from {@link #getMikroAverage()} since the macro average is often more
 * stable for this purpose and will be returned by getAverage() if possible) in cases where higher
 * values are better. If smaller values are better, the method should return something like -1 *
 * {@link #getAverage()}.
 * </p>
 * 
 * <p>
 * The methods {@link #getName()} and {@link #getDescription()} are user by the user interface.
 * Please note that the name should only contain lowercase letters and underscore (RapidMiner
 * parameter format). You might also want to override {@link #toResultString()} or
 * {@link #getVisualizationComponent(com.rapidminer.operator.IOContainer)} in order to provide a
 * nice visualization for the performance criterion but you usually don't have to.
 * </p>
 * 
 * @author Ingo Mierswa, Simon Fischer
 */
public abstract class MeasuredPerformance extends PerformanceCriterion {

	/**
	 * 
	 */
	private static final long serialVersionUID = 4465054472762456363L;

	public MeasuredPerformance() {}

	public MeasuredPerformance(MeasuredPerformance o) {
		super(o);
	}

	/** Counts a single example, e.g. by summing up errors. */
	public abstract void countExample(Example example);

	/**
	 * Initialized the criterion. The default implementation invokes the initialization method with
	 * useExampleWeights set to true.
	 * 
	 * @deprecated Please use the other start counting method directly
	 */
	@Deprecated
	public final void startCounting(ExampleSet set) throws OperatorException {
		startCounting(set, true);
	}

	/** Initializes the criterion. The default implementation does nothing. */
	public void startCounting(ExampleSet set, boolean useExampleWeights) throws OperatorException {}

}
