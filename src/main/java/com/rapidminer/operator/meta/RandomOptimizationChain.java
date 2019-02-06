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
package com.rapidminer.operator.meta;

import java.util.List;

import com.rapidminer.operator.IOObject;
import com.rapidminer.operator.OperatorDescription;
import com.rapidminer.operator.OperatorException;
import com.rapidminer.operator.SimpleOperatorChain;
import com.rapidminer.operator.ValueDouble;
import com.rapidminer.operator.performance.PerformanceVector;
import com.rapidminer.operator.ports.InputPort;
import com.rapidminer.operator.ports.OutputPort;
import com.rapidminer.parameter.ParameterType;
import com.rapidminer.parameter.ParameterTypeBoolean;
import com.rapidminer.parameter.ParameterTypeInt;
import com.rapidminer.parameter.conditions.BooleanParameterCondition;


/**
 * This operator iterates several times through the inner operators and in each cycle evaluates a
 * performance measure. The IOObjects that are produced as output of the inner operators in the best
 * cycle are then returned. The target of this operator are methods that involve some
 * non-deterministic elements such that the performance in each cycle may vary. An example is
 * k-means with random initialization.
 *
 * @author Michael Wurst, Ingo Mierswa, Sebastian Land
 */
public class RandomOptimizationChain extends SimpleOperatorChain {

	/** The parameter name for &quot;The number of iterations to perform&quot; */
	public static final String PARAMETER_ITERATIONS = "iterations";

	/** The parameter name for &quot;Timeout in minutes (-1 = no timeout)&quot; */
	public static final String PARAMETER_TIMEOUT = "timeout";

	public static final String PARAMETER_ENABLE_TIMEOUT = "enable_timeout";

	private int iterationValue;
	private double bestPerformanceValue = 0.0;
	private double avgPerformanceValue = 0.0;

	private final InputPort innerPerformanceSink = getSubprocess(0).getInnerSinks().createPort("performance vector",
			PerformanceVector.class);
	private final OutputPort performanceOutput = getOutputPorts().createPort("performance");

	public RandomOptimizationChain(OperatorDescription description) {
		super(description, "Optimizing");

		getTransformer().addGenerationRule(performanceOutput, PerformanceVector.class);

		addValue(new ValueDouble("iteration", "The number of the current iteration.") {

			@Override
			public double getDoubleValue() {
				return iterationValue;
			}
		});

		addValue(new ValueDouble("performance", "The current best performance") {

			@Override
			public double getDoubleValue() {
				return bestPerformanceValue;
			}
		});

		addValue(new ValueDouble("avg_performance", "The average performance") {

			@Override
			public double getDoubleValue() {
				return avgPerformanceValue;
			}
		});

	}

	@Override
	public void doWork() throws OperatorException {
		int maxIterations = getParameterAsInt(PARAMETER_ITERATIONS);
		long stoptime;
		int timeout = getParameterAsInt(PARAMETER_TIMEOUT);
		if (!getParameterAsBoolean(PARAMETER_ENABLE_TIMEOUT)) {
			stoptime = Long.MAX_VALUE;
		} else {
			stoptime = System.currentTimeMillis() + 60L * 1000 * timeout;
		}

		// init Operator progress
		getProgress().setTotal(maxIterations);
		getProgress().setCheckForStop(false);

		double perfSum = 0.0;
		List<IOObject> bestResult = null;
		PerformanceVector bestPerformance = null;
		for (iterationValue = 0; iterationValue < maxIterations; iterationValue++) {

			// executing sub process
			super.doWork();

			PerformanceVector performanceVector = innerPerformanceSink.getData(PerformanceVector.class);
			if (bestPerformance == null) {
				bestPerformance = performanceVector;
				bestResult = outputExtender.getData(IOObject.class);
			} else {
				if (performanceVector.getMainCriterion().compareTo(bestPerformance.getMainCriterion()) == 1) {
					bestPerformance = performanceVector;
					bestResult = outputExtender.getData(IOObject.class);
				}
			}
			this.bestPerformanceValue = bestPerformance.getMainCriterion().getFitness();

			perfSum = perfSum + performanceVector.getMainCriterion().getAverage();
			avgPerformanceValue = perfSum / iterationValue;

			if (java.lang.System.currentTimeMillis() > stoptime) {
				log("Runtime exceeded in iteration " + iterationValue + ".");
				break;
			}

			inApplyLoop();
			getProgress().step();
		}

		outputExtender.deliver(bestResult);
		performanceOutput.deliver(bestPerformance);
		getProgress().complete();
	}

	@Override
	public List<ParameterType> getParameterTypes() {
		List<ParameterType> types = super.getParameterTypes();

		types.add(new ParameterTypeInt(PARAMETER_ITERATIONS, "The number of iterations to perform", 1, Integer.MAX_VALUE,
				10, false));
		types.add(new ParameterTypeBoolean(PARAMETER_ENABLE_TIMEOUT,
				"If used the processing will be aborted after the next completed execution of child operators.", false, true));
		ParameterType type = new ParameterTypeInt(PARAMETER_TIMEOUT, "Timeout in minutes", 1, Integer.MAX_VALUE, 1);
		type.registerDependencyCondition(new BooleanParameterCondition(this, PARAMETER_ENABLE_TIMEOUT, true, true));
		types.add(type);

		return types;
	}

}
