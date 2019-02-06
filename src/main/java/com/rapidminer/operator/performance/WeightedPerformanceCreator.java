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

import com.rapidminer.operator.Operator;
import com.rapidminer.operator.OperatorDescription;
import com.rapidminer.operator.OperatorException;
import com.rapidminer.operator.ports.InputPort;
import com.rapidminer.operator.ports.OutputPort;
import com.rapidminer.parameter.ParameterType;
import com.rapidminer.parameter.ParameterTypeDouble;
import com.rapidminer.parameter.ParameterTypeList;
import com.rapidminer.parameter.ParameterTypeString;

import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;


/**
 * Returns a performance vector containing the weighted fitness value of the input criteria.
 * 
 * @author Ingo Mierswa ingomierswa Exp $
 */
public class WeightedPerformanceCreator extends Operator {

	private InputPort performanceInput = getInputPorts().createPort("performance");
	private OutputPort performanceOutput = getOutputPorts().createPort("performance");

	/**
	 * The parameter name for &quot;The default weight for all criteria not specified in the list
	 * 'criteria_weights'.&quot;
	 */
	public static final String PARAMETER_DEFAULT_WEIGHT = "default_weight";

	/**
	 * The parameter name for &quot;The weights for several performance criteria. Criteria weights
	 * not defined in this list are set to 'default_weight'.&quot;
	 */
	public static final String PARAMETER_CRITERIA_WEIGHTS = "criteria_weights";

	public WeightedPerformanceCreator(OperatorDescription description) {
		super(description);
		getTransformer().addPassThroughRule(performanceInput, performanceOutput);
	}

	@Override
	public void doWork() throws OperatorException {
		PerformanceVector inputPerformance = performanceInput.getData(PerformanceVector.class);

		Map<String, Double> weightMap = new HashMap<String, Double>();
		List<String[]> weightList = getParameterList(PARAMETER_CRITERIA_WEIGHTS);
		Iterator<String[]> i = weightList.iterator();
		while (i.hasNext()) {
			String[] entry = i.next();
			String criterionName = entry[0];
			Double criterionWeight = Double.valueOf(entry[1]);
			weightMap.put(criterionName, criterionWeight);
		}

		double defaultWeight = getParameterAsDouble(PARAMETER_DEFAULT_WEIGHT);
		double sum = 0.0d;
		double weightSum = 0.0d;
		for (int j = 0; j < inputPerformance.getSize(); j++) {
			PerformanceCriterion pc = inputPerformance.getCriterion(j);
			Double weightObject = weightMap.get(pc.getName());
			double weight = weightObject != null ? weightObject.doubleValue() : defaultWeight;
			sum += weight * pc.getFitness();
			weightSum += weight;
		}
		sum /= weightSum;

		PerformanceVector performance = new PerformanceVector();
		performance.addCriterion(new EstimatedPerformance("weighted_performance", sum, 1, false));
		performanceOutput.deliver(performance);
	}

	@Override
	public List<ParameterType> getParameterTypes() {
		List<ParameterType> types = super.getParameterTypes();
		types.add(new ParameterTypeDouble(PARAMETER_DEFAULT_WEIGHT,
				"The default weight for all criteria not specified in the list 'criteria_weights'.", 0.0d,
				Double.POSITIVE_INFINITY, 1.0d));
		types.add(new ParameterTypeList(
				PARAMETER_CRITERIA_WEIGHTS,
				"The weights for several performance criteria. Criteria weights not defined in this list are set to 'default_weight'.",
				new ParameterTypeString("criteria_name", "The name of the criteria."), new ParameterTypeDouble(
						"criteria_weight", "The weight for this criteria.", 0.0d, Double.POSITIVE_INFINITY, 1.0d)));
		return types;
	}
}
