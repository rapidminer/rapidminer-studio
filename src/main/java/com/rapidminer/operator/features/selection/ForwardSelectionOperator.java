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
package com.rapidminer.operator.features.selection;

import com.rapidminer.example.Attribute;
import com.rapidminer.example.AttributeWeights;
import com.rapidminer.example.Attributes;
import com.rapidminer.example.ExampleSet;
import com.rapidminer.operator.OperatorChain;
import com.rapidminer.operator.OperatorDescription;
import com.rapidminer.operator.OperatorException;
import com.rapidminer.operator.performance.PerformanceVector;
import com.rapidminer.operator.ports.InputPort;
import com.rapidminer.operator.ports.OutputPort;
import com.rapidminer.operator.ports.metadata.ExampleSetPassThroughRule;
import com.rapidminer.operator.ports.metadata.GenerateNewMDRule;
import com.rapidminer.operator.ports.metadata.PassThroughRule;
import com.rapidminer.operator.ports.metadata.SetRelation;
import com.rapidminer.operator.ports.metadata.SubprocessTransformRule;
import com.rapidminer.parameter.ParameterType;
import com.rapidminer.parameter.ParameterTypeInt;

import java.util.Iterator;
import java.util.List;


/**
 * This operator realizes a simple forward selection.
 * 
 * This class has been replaced by the {@link ForwardAttributeSelectionOperator} class which offers
 * many additional functionalities.
 * 
 * @author Sebastian Land
 * 
 */
@Deprecated
public class ForwardSelectionOperator extends OperatorChain {

	public static final String PARAMETER_NUMBER_OF_STEPS = "number_of_steps";

	private final InputPort exampleSetInput = getInputPorts().createPort("training set", ExampleSet.class);
	private final OutputPort innerExampleSource = getSubprocess(0).getInnerSources().createPort("training set");
	private final InputPort innerPerformanceSink = getSubprocess(0).getInnerSinks().createPort("performance vector",
			PerformanceVector.class);
	private final OutputPort performanceVectorOutput = getOutputPorts().createPort("performance vector");
	private final OutputPort exampleSetOutput = getOutputPorts().createPort("example set");
	private final OutputPort attributeWeightsOutput = getOutputPorts().createPort("attribute weights");

	public ForwardSelectionOperator(OperatorDescription description) {
		super(description, "Learning Process");
		getTransformer().addRule(new PassThroughRule(exampleSetInput, innerExampleSource, true));
		getTransformer().addRule(new SubprocessTransformRule(getSubprocess(0)));
		getTransformer().addRule(new PassThroughRule(innerPerformanceSink, performanceVectorOutput, true));
		getTransformer().addRule(new ExampleSetPassThroughRule(exampleSetInput, exampleSetOutput, SetRelation.SUBSET));
		getTransformer().addRule(new GenerateNewMDRule(attributeWeightsOutput, AttributeWeights.class));
	}

	@Override
	public void doWork() throws OperatorException {
		ExampleSet exampleSetOriginal = exampleSetInput.getData(ExampleSet.class);
		ExampleSet exampleSet = (ExampleSet) exampleSetOriginal.clone();
		int numberOfSteps = getParameterAsInt(PARAMETER_NUMBER_OF_STEPS);
		int numberOfAttributes = exampleSet.getAttributes().size();
		Attributes attributes = exampleSet.getAttributes();
		Attribute[] attributeArray = new Attribute[numberOfAttributes];
		int i = 0;
		Iterator<Attribute> iterator = attributes.iterator();
		while (iterator.hasNext()) {
			Attribute attribute = iterator.next();
			attributeArray[i] = attribute;
			i++;
			iterator.remove();
		}

		boolean[] selected = new boolean[numberOfAttributes];
		PerformanceVector bestPerformance = null;
		for (i = 0; i < numberOfSteps; i++) {
			int bestIndex = 0;
			boolean gain = false;
			for (int current = 0; current < numberOfAttributes; current++) {
				if (!selected[current]) {
					// switching on
					attributes.addRegular(attributeArray[current]);

					// evaluate performance
					innerExampleSource.deliver(exampleSet);
					getSubprocess(0).execute();

					PerformanceVector performance = innerPerformanceSink.getData(PerformanceVector.class);
					if (bestPerformance == null || performance.compareTo(bestPerformance) > 0) {
						bestIndex = current;
						bestPerformance = performance;
						gain = true;
					}

					// switching off
					attributes.remove(attributeArray[current]);
				}
			}
			// if there had been a gain, then continue and switch best additional feature on
			if (gain) {
				// switching best index on
				attributes.addRegular(attributeArray[bestIndex]);
				selected[bestIndex] = true;
			} else {
				break;
			}
		}

		AttributeWeights weights = new AttributeWeights();
		i = 0;
		for (Attribute attribute : attributeArray) {
			if (selected[i]) {
				weights.setWeight(attribute.getName(), 1d);
			} else {
				weights.setWeight(attribute.getName(), 0d);
			}
			i++;
		}

		performanceVectorOutput.deliver(bestPerformance);
		attributeWeightsOutput.deliver(weights);
		exampleSetOutput.deliver(exampleSet);
	}

	@Override
	public List<ParameterType> getParameterTypes() {
		List<ParameterType> types = super.getParameterTypes();
		types.add(new ParameterTypeInt(PARAMETER_NUMBER_OF_STEPS, "number of forward selection steps", 1, Integer.MAX_VALUE,
				10));
		return types;
	}
}
