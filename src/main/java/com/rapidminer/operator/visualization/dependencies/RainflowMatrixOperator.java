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
package com.rapidminer.operator.visualization.dependencies;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;

import Jama.Matrix;

import com.rapidminer.example.Attribute;
import com.rapidminer.example.Example;
import com.rapidminer.example.ExampleSet;
import com.rapidminer.operator.Operator;
import com.rapidminer.operator.OperatorDescription;
import com.rapidminer.operator.OperatorException;
import com.rapidminer.operator.UserError;
import com.rapidminer.operator.error.AttributeNotFoundError;
import com.rapidminer.operator.ports.InputPort;
import com.rapidminer.operator.ports.OutputPort;
import com.rapidminer.operator.ports.metadata.AttributeSetPrecondition;
import com.rapidminer.parameter.ParameterType;
import com.rapidminer.parameter.ParameterTypeAttribute;
import com.rapidminer.parameter.ParameterTypeBoolean;
import com.rapidminer.tools.Ontology;


/**
 * <p>
 * This operator calculates the rainflow matrix for a series attribute. Please note that the
 * attribute does have to be discretized before. Since this operator relies on the fact that the
 * names of the bins follow the same natural order than the values they represent, we recommend to
 * use the option &quot;interval names&quot; for the discretized bins.
 * </p>
 *
 * @author Ingo Mierswa
 */
public class RainflowMatrixOperator extends Operator {

	public static final String PARAMETER_ATTRIBUTE = "attribute";

	public static final String PARAMETER_SYMMETRICAL_MATRIX = "symmetrical_matrix";

	private InputPort exampleSetInput = getInputPorts().createPort("example set", ExampleSet.class);
	private OutputPort exampleSetOutput = getOutputPorts().createPort("example set");
	private OutputPort matrixOutput = getOutputPorts().createPort("transition matrix");

	public RainflowMatrixOperator(OperatorDescription description) {
		super(description);

		exampleSetInput.addPrecondition(new AttributeSetPrecondition(exampleSetInput, AttributeSetPrecondition
				.getAttributesByParameter(this, PARAMETER_ATTRIBUTE), Ontology.NOMINAL));

		getTransformer().addPassThroughRule(exampleSetInput, exampleSetOutput);
		getTransformer().addGenerationRule(matrixOutput, NumericalMatrix.class);
	}

	@Override
	public void doWork() throws OperatorException {
		ExampleSet exampleSet = exampleSetInput.getData(ExampleSet.class);

		String attributeName = getParameterAsString(PARAMETER_ATTRIBUTE);
		Attribute attribute = exampleSet.getAttributes().get(attributeName);
		if (attribute == null) {
			throw new AttributeNotFoundError(this, PARAMETER_ATTRIBUTE, attributeName);
		}
		if (!attribute.isNominal()) {
			throw new UserError(this, 103, "calculation of the Rainflow Matrix", attributeName);
		}

		// store all nominal values in our working list
		List<String> values = new ArrayList<>(exampleSet.size());
		for (Example example : exampleSet) {
			if (!Double.isNaN(example.getValue(attribute))) {
				values.add(example.getNominalValue(attribute));
			}
		}

		// remove all non extrema
		removeNonExtrema(values);

		// initialize matrix and "column" names
		List<String> allNames = new LinkedList<>();
		for (String name : attribute.getMapping().getValues()) {
			allNames.add(name);
		}
		String[] columnNames = new String[allNames.size()];
		allNames.toArray(columnNames);

		Map<String, Integer> indexMap = new HashMap<>();
		for (int i = 0; i < columnNames.length; i++) {
			indexMap.put(columnNames[i], i);
		}

		// fill matrix (4 point algorithm)
		double[][] counts = createRainflowCounts(values, indexMap);

		// wrap entries if symmetrical
		boolean symmetrical = getParameterAsBoolean(PARAMETER_SYMMETRICAL_MATRIX);
		if (symmetrical) {
			for (int x = 1; x < counts.length - 1; x++) {
				for (int y = 1; y < counts[x].length - 1; y++) {
					counts[y][x] += counts[x][y];
					counts[x][y] = 0;
				}
			}
		}

		// create and deliver results
		Matrix matrix = new Matrix(counts);
		String[] residuals = new String[values.size()];
		values.toArray(residuals);
		RainflowMatrix rainflowMatrix = new RainflowMatrix("Rainflow Matrix", columnNames, matrix, symmetrical, residuals);
		rainflowMatrix.setFirstAttributeName("From Value");
		rainflowMatrix.setSecondAttributeName("To Value");

		exampleSetOutput.deliver(exampleSet);
		matrixOutput.deliver(rainflowMatrix);
	}

	private double[][] createRainflowCounts(List<String> values, Map<String, Integer> indexMap) {
		double[][] counts = new double[indexMap.size()][indexMap.size()];

		int currentIndex = 4;
		while (currentIndex < values.size()) {
			String value1 = values.get(currentIndex - 4);
			String value2 = values.get(currentIndex - 3);
			String value3 = values.get(currentIndex - 2);
			String value4 = values.get(currentIndex - 1);

			boolean betweenIncreasing = value2.compareTo(value1) >= 0 && value2.compareTo(value4) <= 0
					&& value3.compareTo(value1) >= 0 && value3.compareTo(value4) <= 0;
					boolean betweenDecreasing = value2.compareTo(value1) <= 0 && value2.compareTo(value4) >= 0
							&& value3.compareTo(value1) <= 0 && value3.compareTo(value4) >= 0;
							if (betweenIncreasing || betweenDecreasing) {
								int fromIndex = indexMap.get(value2);
								int toIndex = indexMap.get(value3);
								counts[fromIndex][toIndex] = counts[fromIndex][toIndex] + 1;
								values.remove(currentIndex - 2);
								values.remove(currentIndex - 3);
								currentIndex = 4;
							} else { // nothing found
								currentIndex++;
							}
		}

		return counts;
	}

	private void removeNonExtrema(List<String> values) {
		for (int i = values.size() - 2; i >= 1; i--) {
			String currentValue = values.get(i);
			String beforeValue = values.get(i - 1);
			String afterValue = values.get(i + 1);

			boolean localMinimum = beforeValue.compareTo(currentValue) > 0 && afterValue.compareTo(currentValue) > 0;
			boolean localMaximum = beforeValue.compareTo(currentValue) < 0 && afterValue.compareTo(currentValue) < 0;

			if (!localMinimum && !localMaximum) {
				values.remove(i);
			}
		}
	}

	@Override
	public List<ParameterType> getParameterTypes() {
		List<ParameterType> types = super.getParameterTypes();
		types.add(new ParameterTypeAttribute(PARAMETER_ATTRIBUTE,
				"Indicates which attribute should be used as a base for the calculation of the Rainflow matrix.",
				exampleSetInput, false, Ontology.NOMINAL));
		types.add(new ParameterTypeBoolean(PARAMETER_SYMMETRICAL_MATRIX,
				"Indicates if the symmetrical matrix should be calculated.", false));
		return types;
	}
}
