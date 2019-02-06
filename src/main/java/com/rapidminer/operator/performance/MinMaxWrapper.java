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

import java.util.List;

import com.rapidminer.operator.Operator;
import com.rapidminer.operator.OperatorDescription;
import com.rapidminer.operator.OperatorException;
import com.rapidminer.operator.ports.InputPort;
import com.rapidminer.operator.ports.OutputPort;
import com.rapidminer.parameter.ParameterType;
import com.rapidminer.parameter.ParameterTypeDouble;


/**
 * Wraps a {@link MinMaxCriterion} around each performance criterion of type MeasuredPerformance.
 * This criterion uses the minimum fitness achieved instead of the average fitness or arbitrary
 * weightings of both. Please note that the average values stay the same and only the fitness values
 * change.
 *
 * @author Ingo Mierswa
 */
public class MinMaxWrapper extends Operator {

	/**
	 * The parameter name for &quot;Defines the weight for the minimum fitness agains the average
	 * fitness&quot;
	 */
	public static final String PARAMETER_MINIMUM_WEIGHT = "minimum_weight";

	private InputPort performanceInput = getInputPorts().createPort("performance vector", PerformanceVector.class);
	private OutputPort performanceOutput = getOutputPorts().createPort("performance vector");

	public MinMaxWrapper(OperatorDescription description) {
		super(description);

		getTransformer().addPassThroughRule(performanceInput, performanceOutput);
	}

	@Override
	public void doWork() throws OperatorException {
		PerformanceVector performanceVector = performanceInput.getData(PerformanceVector.class);
		PerformanceVector result = new PerformanceVector();
		double minimumWeight = getParameterAsDouble(PARAMETER_MINIMUM_WEIGHT);
		for (int i = 0; i < performanceVector.size(); i++) {
			PerformanceCriterion crit = performanceVector.getCriterion(i);
			if (crit instanceof MeasuredPerformance) {
				result.addCriterion(new MinMaxCriterion((MeasuredPerformance) crit, minimumWeight));
			}
		}
		result.setMainCriterionName(performanceVector.getMainCriterion().getName());
		performanceOutput.deliver(result);
	}

	@Override
	public List<ParameterType> getParameterTypes() {
		List<ParameterType> types = super.getParameterTypes();
		ParameterType type = new ParameterTypeDouble(PARAMETER_MINIMUM_WEIGHT,
				"Defines the weight for the minimum fitness agains the average fitness", 0.0d, 1.0d, 1.0d);
		type.setExpert(false);
		types.add(type);
		return types;
	}
}
