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

import com.rapidminer.operator.IOContainer;
import com.rapidminer.operator.OperatorDescription;
import com.rapidminer.operator.OperatorException;
import com.rapidminer.parameter.ParameterType;
import com.rapidminer.parameter.ParameterTypeBoolean;
import com.rapidminer.parameter.ParameterTypeInt;
import com.rapidminer.parameter.conditions.BooleanParameterCondition;


/**
 * Performs its inner operators for the defined number of times. The input of this operator will be
 * the input of the first operator in the first iteration. The output of each nested operator is the
 * input for the following one, the output of the last inner operator will be the input for the
 * first child in the next iteration. The output of the last operator in the last iteration will be
 * the output of this operator.
 *
 * @author Ingo Mierswa
 * @deprecated since 7.4, replaced by the LoopOperator in the Concurrency extension
 */
@Deprecated
public class IteratingOperatorChain extends AbstractIteratingOperatorChain {

	/** The parameter name for &quot;Number of iterations&quot; */
	public static final String PARAMETER_ITERATIONS = "iterations";

	public static final String PARAMETER_LIMIT_TIME = "limit_time";
	/** The parameter name for &quot;Timeout in minutes (-1: no timeout)&quot; */
	public static final String PARAMETER_TIMEOUT = "timeout";

	private long stoptime;

	private boolean limitTime;

	private int iterations;

	public IteratingOperatorChain(OperatorDescription description) {
		super(description);
	}

	@Override
	public void doWork() throws OperatorException {
		stoptime = Long.MAX_VALUE;
		limitTime = getParameterAsBoolean(PARAMETER_LIMIT_TIME);
		if (limitTime) {
			stoptime = System.currentTimeMillis() + 60L * 1000 * getParameterAsInt(PARAMETER_TIMEOUT);
		}
		iterations = getParameterAsInt(PARAMETER_ITERATIONS);
		getProgress().setTotal(getParameterAsInt(PARAMETER_ITERATIONS));
		super.doWork();
	}

	@Override
	boolean shouldStop(IOContainer unused) throws OperatorException {
		if (limitTime) {
			if (System.currentTimeMillis() > stoptime) {
				getLogger().info("Timeout reached");
				return true;
			}
		}
		return getIteration() >= iterations;
	}

	@Override
	public List<ParameterType> getParameterTypes() {
		List<ParameterType> types = super.getParameterTypes();
		ParameterType type = new ParameterTypeInt(PARAMETER_ITERATIONS, "Number of iterations", 0, Integer.MAX_VALUE, 1);
		type.setExpert(false);
		types.add(type);
		type = new ParameterTypeBoolean(PARAMETER_LIMIT_TIME,
				"If checked, the loop will be aborted at last after a specified time.", false);
		types.add(type);
		type = new ParameterTypeInt(PARAMETER_TIMEOUT, "Timeout in minutes", 1, Integer.MAX_VALUE, 1);
		type.registerDependencyCondition(new BooleanParameterCondition(this, PARAMETER_LIMIT_TIME, true, true));
		type.setExpert(true);
		types.add(type);

		return types;
	}
}
