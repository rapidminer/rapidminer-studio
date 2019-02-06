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

import com.rapidminer.operator.IOContainer;
import com.rapidminer.operator.Operator;
import com.rapidminer.operator.OperatorDescription;
import com.rapidminer.operator.UserError;
import com.rapidminer.operator.ValueDouble;
import com.rapidminer.operator.performance.PerformanceVector;
import com.rapidminer.operator.ports.OutputPort;
import com.rapidminer.operator.ports.PortPairExtender;
import com.rapidminer.operator.ports.metadata.GenerateNewMDRule;

import java.util.Collection;


/**
 * This operator provides basic functions for all other parameter optimization operators.
 * 
 * @author Ingo Mierswa, Helge Homburg, Tobias Malbrecht
 */
public abstract class ParameterOptimizationOperator extends ParameterIteratingOperatorChain {

	private final OutputPort performanceOutput = getOutputPorts().createPort("performance");
	private final OutputPort parameterOutput = getOutputPorts().createPort("parameter");

	public ParameterOptimizationOperator(OperatorDescription description) {
		super(description, "Optimization Process");

		getTransformer().addPassThroughRule(getPerformanceInnerSink(), performanceOutput);
		getTransformer().addRule(new GenerateNewMDRule(parameterOutput, ParameterSet.class));

		addValue(new ValueDouble("performance", "currently best performance") {

			@Override
			public double getDoubleValue() {
				return getCurrentBestPerformance();
			}
		});
	}

	@Override
	protected PortPairExtender makeInnerSinkExtender() {
		return new PortPairExtender("result", getSubprocess(0).getInnerSinks(), getOutputPorts());
	}

	public abstract double getCurrentBestPerformance();

	protected void deliver(ParameterSet parameterSet) throws UserError {
		if (parameterSet != null) {
			parameterOutput.deliver(parameterSet);
			performanceOutput.deliver(parameterSet.getPerformance());
		} else {
			throw new UserError(this, 161);
		}
	}

	@Override
	protected boolean isPerformanceRequired() {
		return true;
	}

	/**
	 * @Deprecated Call {@link #getPerformance()} to apply inner operators.
	 */
	@Deprecated
	protected PerformanceVector getPerformance(IOContainer input, Collection<Operator> operators) {
		throw new UnsupportedOperationException(
				"getPerformance(IOContainer,Collection<Operator>) is deprecated. Please call getPerformance().");
	}
}
