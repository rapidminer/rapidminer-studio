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
package com.rapidminer.operator.validation;

import java.util.List;

import com.rapidminer.operator.IOObject;
import com.rapidminer.operator.OperatorChain;
import com.rapidminer.operator.OperatorDescription;
import com.rapidminer.operator.OperatorException;
import com.rapidminer.operator.ValueDouble;
import com.rapidminer.operator.performance.PerformanceCriterion;
import com.rapidminer.operator.performance.PerformanceVector;
import com.rapidminer.operator.ports.PortPairExtender;
import com.rapidminer.operator.ports.metadata.MetaData;
import com.rapidminer.operator.ports.metadata.SubprocessTransformRule;
import com.rapidminer.parameter.ParameterType;
import com.rapidminer.parameter.ParameterTypeBoolean;
import com.rapidminer.parameter.ParameterTypeInt;
import com.rapidminer.tools.math.AverageVector;


/**
 * This operator chain performs the inner operators the given number of times. The inner operators
 * must provide a PerformanceVector. These are averaged and returned as result.
 *
 * @author Ingo Mierswa
 */
public class IteratingPerformanceAverage extends OperatorChain {

	public static final String PARAMETER_ITERATIONS = "iterations";

	public static final String PARAMETER_AVERAGE_PERFORMANCES_ONLY = "average_performances_only";

	private PerformanceCriterion lastPerformance;

	private PortPairExtender inExtender = new PortPairExtender("in", getInputPorts(), getSubprocess(0).getInnerSources());
	private final PortPairExtender performancePortExtender = new PortPairExtender("averagable", getSubprocess(0)
			.getInnerSinks(), getOutputPorts(), new MetaData(AverageVector.class));

	public IteratingPerformanceAverage(OperatorDescription description) {
		super(description, "Subprocess");

		inExtender.start();
		performancePortExtender.start();
		getTransformer().addRule(inExtender.makePassThroughRule());
		getTransformer().addRule(new SubprocessTransformRule(getSubprocess(0)));
		getTransformer().addRule(performancePortExtender.makePassThroughRule());

		addValue(new ValueDouble("performance", "The last performance average (main criterion).") {

			@Override
			public double getDoubleValue() {
				if (lastPerformance != null) {
					return lastPerformance.getAverage();
				} else {
					return Double.NaN;
				}
			}
		});
	}

	@Override
	public void doWork() throws OperatorException {
		int numberOfIterations = getParameterAsInt(PARAMETER_ITERATIONS);

		// init OperatorProgressBar
		getProgress().setTotal(numberOfIterations);
		getProgress().setCheckForStop(false);

		for (int i = 0; i < numberOfIterations; i++) {
			evaluate();
			inApplyLoop();
			getProgress().step();
		}

		// set last result for plotting purposes. This is an average value and
		// actually not the last performance value!
		boolean success = false;
		for (IOObject result : performancePortExtender.getData(IOObject.class)) {
			if (result instanceof PerformanceVector) {
				lastPerformance = ((PerformanceVector) result).getMainCriterion();
				success = true;
			}
		}
		if (!success) {
			lastPerformance = null;
			getLogger().warning("No performance vector found among averagable results. Performance will not be loggable.");
		}
		getProgress().complete();
	}

	/** Applies the inner operator on a clone of the input. */
	private void evaluate() throws OperatorException {
		inExtender.passCloneThrough();
		getSubprocess(0).execute();
		Tools.buildAverages(performancePortExtender);
	}

	@Override
	public List<ParameterType> getParameterTypes() {
		List<ParameterType> types = super.getParameterTypes();
		ParameterType type = new ParameterTypeInt(PARAMETER_ITERATIONS, "The number of iterations.", 1, Integer.MAX_VALUE,
				10);
		type.setExpert(false);
		types.add(type);
		types.add(new ParameterTypeBoolean(PARAMETER_AVERAGE_PERFORMANCES_ONLY,
				"Indicates if only performance vectors should be averaged or all types of averagable result vectors.", true));
		return types;
	}
}
