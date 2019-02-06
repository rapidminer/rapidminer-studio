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
package com.rapidminer.operator.performance.cost;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.apache.commons.lang.ArrayUtils;

import com.rapidminer.example.Attribute;
import com.rapidminer.example.Attributes;
import com.rapidminer.example.Example;
import com.rapidminer.example.ExampleSet;
import com.rapidminer.example.Tools;
import com.rapidminer.operator.Operator;
import com.rapidminer.operator.OperatorDescription;
import com.rapidminer.operator.OperatorException;
import com.rapidminer.operator.OperatorVersion;
import com.rapidminer.operator.UserError;
import com.rapidminer.operator.ValueDouble;
import com.rapidminer.operator.performance.PerformanceVector;
import com.rapidminer.operator.ports.InputPort;
import com.rapidminer.operator.ports.OutputPort;
import com.rapidminer.operator.ports.metadata.ExampleSetPrecondition;
import com.rapidminer.parameter.ParameterType;
import com.rapidminer.parameter.ParameterTypeBoolean;
import com.rapidminer.parameter.ParameterTypeEnumeration;
import com.rapidminer.parameter.ParameterTypeMatrix;
import com.rapidminer.parameter.ParameterTypeString;
import com.rapidminer.tools.Ontology;


/**
 * This operator provides the ability to evaluate classification costs. Therefore a cost matrix
 * might be specified, denoting the costs for every possible classification outcome: predicted label
 * x real label. Costs will be minimized during optimization.
 * 
 * @author Sebastian Land
 */
public class CostEvaluator extends Operator {

	private static final String PARAMETER_COST_MATRIX = "cost_matrix";
	private static final String PARAMETER_KEEP_EXAMPLE_SET = "keep_exampleSet";
	private static final String PARAMETER_CLASS_NAME = "class_name";
	private static final String PARAMETER_CLASS_DEFINITION = "class_order_definition";

	/** Up to and including version 9.0.2 the wrong fitness was returned by the ClassificationCostCriterion */
	static final OperatorVersion WRONG_FITNESS = new OperatorVersion(9, 0, 2);

	private InputPort exampleSetInput = getInputPorts().createPort("example set");

	private OutputPort exampleSetOutput = getOutputPorts().createPort("example set");
	private OutputPort performanceOutput = getOutputPorts().createPort("performance");

	private double lastCosts = Double.NaN;

	public CostEvaluator(OperatorDescription description) {
		super(description);

		exampleSetInput.addPrecondition(new ExampleSetPrecondition(exampleSetInput, Ontology.ATTRIBUTE_VALUE,
				Attributes.LABEL_NAME));
		exampleSetInput.addPrecondition(new ExampleSetPrecondition(exampleSetInput, Ontology.ATTRIBUTE_VALUE,
				Attributes.PREDICTION_NAME));
		getTransformer().addGenerationRule(performanceOutput, PerformanceVector.class);
		getTransformer().addPassThroughRule(exampleSetInput, exampleSetOutput);

		addValue(new ValueDouble("costs", "The last costs.") {

			@Override
			public double getDoubleValue() {
				return lastCosts;
			}
		});
	}

	@Override
	public void doWork() throws OperatorException {
		ExampleSet exampleSet = exampleSetInput.getData(ExampleSet.class);
		Tools.hasNominalLabels(exampleSet, getOperatorClassName());
		Attribute predictedLabel = exampleSet.getAttributes().getPredictedLabel();
		if (predictedLabel == null) {
			throw new UserError(this, 107);
		}
		Attribute label = exampleSet.getAttributes().getLabel();
		double[][] costMatrix = getParameterAsMatrix(PARAMETER_COST_MATRIX);

		// build label ordering map
		Map<String, Integer> classOrderMap = null;
		if (isParameterSet(PARAMETER_CLASS_DEFINITION)) {
			String[] enumeratedValues = ParameterTypeEnumeration
					.transformString2Enumeration(getParameterAsString(PARAMETER_CLASS_DEFINITION));

			if (enumeratedValues.length > 0) {
				classOrderMap = new HashMap<>();
				int i = 0;

				for (String className : enumeratedValues) {
					classOrderMap.put(className, i);
					i++;
				}
				// check whether each possible label occurred once
				for (String value : label.getMapping().getValues()) {
					if (!classOrderMap.containsKey(value)) {
						throw new UserError(this, "performance_costs.class_order_definition_misses_value", value);
					}
				}

				// check whether map is of same size than costMatrix
				if (costMatrix.length != classOrderMap.size()) {
					throw new UserError(this, "performance_costs.cost_matrix_with_wrong_dimension", costMatrix.length,
							classOrderMap.size());
				}

			}
		}

		ClassificationCostCriterion criterion = new ClassificationCostCriterion(costMatrix, classOrderMap, label, predictedLabel);
		criterion.setVersion(getCompatibilityLevel());
		PerformanceVector performance = new PerformanceVector();
		performance.addCriterion(criterion);
		// now measuring costs
		criterion.startCounting(exampleSet, false);
		for (Example example : exampleSet) {
			criterion.countExample(example);
		}

		// setting logging value
		lastCosts = criterion.getAverage();

		exampleSetOutput.deliver(exampleSet);
		performanceOutput.deliver(performance);
	}

	@Override
	public List<ParameterType> getParameterTypes() {
		List<ParameterType> types = super.getParameterTypes();
		ParameterType type = new ParameterTypeBoolean(PARAMETER_KEEP_EXAMPLE_SET,
				"Indicates if the example set should be kept.", false);
		type.setHidden(true);
		types.add(type);
		type = new ParameterTypeMatrix(PARAMETER_COST_MATRIX,
				"The matrix of missclassification costs. Columns and Rows in order of internal mapping.", "Cost Matrix",
				"Predicted Class", "True Class", true, false);
		type.setPrimary(true);
		types.add(type);
		types.add(new ParameterTypeEnumeration(
				PARAMETER_CLASS_DEFINITION,
				"With this parameter it is possible to define the order of classes used in the cost matrix. First class in this list is First class in the matrix.",
				new ParameterTypeString(PARAMETER_CLASS_NAME, "The name of the class."), false));
		return types;
	}

	@Override
	public OperatorVersion[] getIncompatibleVersionChanges() {
		return (OperatorVersion[]) ArrayUtils.addAll(super.getIncompatibleVersionChanges(),
				new OperatorVersion[]{WRONG_FITNESS});
	}
}
