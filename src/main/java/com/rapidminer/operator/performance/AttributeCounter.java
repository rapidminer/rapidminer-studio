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

import com.rapidminer.example.ExampleSet;
import com.rapidminer.operator.OperatorDescription;
import com.rapidminer.operator.OperatorException;
import com.rapidminer.operator.ValueDouble;
import com.rapidminer.operator.ports.InputPort;
import com.rapidminer.parameter.ParameterType;
import com.rapidminer.parameter.ParameterTypeCategory;

import java.util.List;


/**
 * Returns a performance vector just counting the number of attributes currently used for the given
 * example set.
 * 
 * @author Ingo Mierswa
 */
public class AttributeCounter extends AbstractExampleSetEvaluator {

	private InputPort performanceInput = getInputPorts().createPort("performance", PerformanceVector.class);

	/**
	 * The parameter name for &quot;Indicates if the fitness should for maximal or minimal number of
	 * features.&quot;
	 */
	public static final String PARAMETER_OPTIMIZATION_DIRECTION = "optimization_direction";
	private double lastCount = Double.NaN;

	public AttributeCounter(OperatorDescription description) {
		super(description);
		addValue(new ValueDouble("attributes", "The currently selected number of attributes.") {

			@Override
			public double getDoubleValue() {
				return lastCount;
			}
		});
	}

	/**
	 * Creates a new performance vector if the given one is null. Adds a MDL criterion. If the
	 * criterion was already part of the performance vector before it will be overwritten.
	 */
	private PerformanceVector count(ExampleSet exampleSet, PerformanceVector performanceCriteria) throws OperatorException {
		if (performanceCriteria == null) {
			performanceCriteria = new PerformanceVector();
		}

		MDLCriterion mdlCriterion = new MDLCriterion(getParameterAsInt(PARAMETER_OPTIMIZATION_DIRECTION));
		mdlCriterion.startCounting(exampleSet, true);
		this.lastCount = mdlCriterion.getAverage();
		performanceCriteria.addCriterion(mdlCriterion);
		return performanceCriteria;
	}

	@Override
	public PerformanceVector evaluate(ExampleSet exampleSet) throws OperatorException {
		PerformanceVector inputPerformance = performanceInput.getDataOrNull(PerformanceVector.class);
		PerformanceVector performance = count(exampleSet, inputPerformance);
		return performance;
	}

	@Override
	public List<ParameterType> getParameterTypes() {
		List<ParameterType> types = super.getParameterTypes();
		types.add(new ParameterTypeCategory(PARAMETER_OPTIMIZATION_DIRECTION,
				"Indicates if the fitness should be maximal for the maximal or for the minimal number of features.",
				MDLCriterion.DIRECTIONS, MDLCriterion.MINIMIZATION));
		return types;
	}
}
