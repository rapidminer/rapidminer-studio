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

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Set;
import java.util.TreeSet;

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
import com.rapidminer.tools.Ontology;
import com.rapidminer.tools.container.Pair;


/**
 * This operator calculates the transition matrix of a specified attribute, i.e. the operator counts
 * how often each possible nominal value follows after each other.
 *
 * @author Sebastian Land, Nils Woehler
 */
public class TransitionMatrixOperator extends Operator {

	public static final String PARAMETER_ATTRIBUTE = "attribute";

	private InputPort exampleSetInput = getInputPorts().createPort("example set");
	private OutputPort exampleSetOutput = getOutputPorts().createPort("example set");
	private OutputPort matrixOutput = getOutputPorts().createPort("transition matrix");

	public TransitionMatrixOperator(OperatorDescription description) {
		super(description);

		exampleSetInput.addPrecondition(new AttributeSetPrecondition(exampleSetInput, AttributeSetPrecondition
				.getAttributesByParameter(this, PARAMETER_ATTRIBUTE), Ontology.NOMINAL));

		getTransformer().addPassThroughRule(exampleSetInput, exampleSetOutput);
		getTransformer().addGenerationRule(matrixOutput, NumericalMatrix.class);
	}

	@Override
	public void doWork() throws OperatorException {
		ExampleSet exampleSet = exampleSetInput.getData(ExampleSet.class);
		Attribute attribute = exampleSet.getAttributes().get(getParameterAsString(PARAMETER_ATTRIBUTE));
		if (attribute == null) {
			throw new AttributeNotFoundError(this, PARAMETER_ATTRIBUTE, getParameterAsString(PARAMETER_ATTRIBUTE));
		}
		if (!attribute.isNominal()) {
			throw new UserError(this, 119, attribute.getName(), "TransitionMatrix");
		}

		Set<String> values = new TreeSet<>();
		Map<Pair<String, String>, Integer> transitions = new HashMap<>();

		int numberOfTransitions = exampleSet.size() - 1;
		String lastValue = null;
		for (Example example : exampleSet) {
			String currentValue = example.getNominalValue(attribute);
			values.add(currentValue);

			if (lastValue != null) {
				Pair<String, String> currentTupel = new Pair<>(lastValue, currentValue);
				if (transitions.containsKey(currentTupel)) {
					transitions.put(currentTupel, transitions.get(currentTupel) + 1);
				} else {
					transitions.put(currentTupel, 1);
				}
			}
			lastValue = currentValue;
		}
		String[] valueArray = values.toArray(new String[] {});
		// creating map for translation of string into matrix position
		Map<String, Integer> valuePositions = new HashMap<>();
		int i = 0;
		for (String value : valueArray) {
			valuePositions.put(value, i);
			i++;
		}

		NumericalMatrix matrix = new NumericalMatrix("Transition", valueArray, false);
		for (Entry<Pair<String, String>, Integer> entry : transitions.entrySet()) {
			matrix.setValue(valuePositions.get(entry.getKey().getFirst()), valuePositions.get(entry.getKey().getSecond()),
					(double) entry.getValue().intValue() / numberOfTransitions);
		}

		exampleSetOutput.deliver(exampleSet);
		matrixOutput.deliver(matrix);
	}

	@Override
	public List<ParameterType> getParameterTypes() {
		List<ParameterType> types = super.getParameterTypes();
		types.add(new ParameterTypeAttribute(PARAMETER_ATTRIBUTE, "Specifies the nominal attribute.", exampleSetInput,
				false, false, Ontology.NOMINAL));
		return types;
	}
}
