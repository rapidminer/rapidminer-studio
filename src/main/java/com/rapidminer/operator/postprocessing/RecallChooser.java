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
package com.rapidminer.operator.postprocessing;

import com.rapidminer.example.Attribute;
import com.rapidminer.example.Attributes;
import com.rapidminer.example.Example;
import com.rapidminer.example.ExampleSet;
import com.rapidminer.example.Tools;
import com.rapidminer.example.set.SortedExampleSet;
import com.rapidminer.example.table.NominalMapping;
import com.rapidminer.operator.Operator;
import com.rapidminer.operator.OperatorDescription;
import com.rapidminer.operator.OperatorException;
import com.rapidminer.operator.UserError;
import com.rapidminer.operator.ports.InputPort;
import com.rapidminer.operator.ports.OutputPort;
import com.rapidminer.operator.ports.metadata.ExampleSetPrecondition;
import com.rapidminer.parameter.ParameterType;
import com.rapidminer.parameter.ParameterTypeBoolean;
import com.rapidminer.parameter.ParameterTypeDouble;
import com.rapidminer.parameter.ParameterTypeString;
import com.rapidminer.tools.Ontology;

import java.util.List;


/**
 * This operator finds the lowest threshold which reaches a given recall.
 * 
 * @author Marius Helf
 */
public class RecallChooser extends Operator {

	// The parameters of this operator:
	public static final String PARAMETER_USE_EXAMPLE_WEIGHTS = "use_example_weights";
	public static final String PARAMETER_RECALL = "min_recall";
	public static final String PARAMETER_POSITIVE_LABEL = "positive_label";

	private InputPort exampleSetInput = getInputPorts().createPort("example set", ExampleSet.class);
	private OutputPort exampleSetOutput = getOutputPorts().createPort("example set");
	private OutputPort thresholdOutput = getOutputPorts().createPort("threshold");

	public RecallChooser(OperatorDescription description) {
		super(description);

		exampleSetInput.addPrecondition(new ExampleSetPrecondition(exampleSetInput, Ontology.VALUE_TYPE,
				Attributes.LABEL_NAME, Attributes.PREDICTION_NAME, Attributes.CONFIDENCE_NAME));
		getTransformer().addPassThroughRule(exampleSetInput, exampleSetOutput);
		getTransformer().addGenerationRule(thresholdOutput, Threshold.class);
	}

	@Override
	public void doWork() throws OperatorException {
		ExampleSet exampleSet = exampleSetInput.getData(ExampleSet.class);
		boolean useWeights = getParameterAsBoolean(PARAMETER_USE_EXAMPLE_WEIGHTS);

		// checking preconditions
		Tools.hasNominalLabels(exampleSet, getOperatorClassName());
		Attribute label = exampleSet.getAttributes().getLabel();
		exampleSet.recalculateAttributeStatistics(label);
		NominalMapping mapping = label.getMapping();
		if (mapping.size() != 2) {
			throw new UserError(this, 118,
					new Object[] { label, Integer.valueOf(mapping.getValues().size()), Integer.valueOf(2) });
		}
		if (exampleSet.getAttributes().getPredictedLabel() == null) {
			throw new UserError(this, 107);
		}

		// find positive class
		String positiveClassName = null;
		double positiveIndex;
		if (isParameterSet(PARAMETER_POSITIVE_LABEL)) {
			positiveClassName = getParameterAsString(PARAMETER_POSITIVE_LABEL);
			positiveIndex = mapping.getIndex(positiveClassName);
			if (positiveIndex < 0) {
				throw new UserError(this, 143, positiveClassName, label.getName());
			}
		} else {
			positiveIndex = mapping.getPositiveIndex();
			positiveClassName = mapping.getPositiveString();
		}

		// calculate weighted count of positive class
		double totalSum = 0;
		for (Example e : exampleSet) {
			if (e.getLabel() == positiveIndex) {
				if (useWeights) {
					double w = e.getWeight();
					if (Double.isNaN(w)) {
						w = 1.0;
					}
					totalSum += w;
				} else {
					totalSum += 1.0;
				}
			}
		}

		// now find the actual threshold
		double currentSum = 0;
		double desiredRecall = getParameterAsDouble(PARAMETER_RECALL);
		double thresholdValue = 0;
		Attribute confidenceAttribute = exampleSet.getAttributes().getSpecial(
				Attributes.CONFIDENCE_NAME + "_" + positiveClassName);
		SortedExampleSet sortedExampleSet = new SortedExampleSet(exampleSet, confidenceAttribute,
				SortedExampleSet.INCREASING);
		for (Example e : sortedExampleSet) {
			if (e.getLabel() == positiveIndex) {
				if (useWeights) {
					double w = e.getWeight();
					if (Double.isNaN(w)) {
						w = 1.0;
					}
					currentSum += w;
				} else {
					currentSum += 1.0;
				}
				if (currentSum / totalSum >= 1 - desiredRecall) {
					break;
				}
				thresholdValue = (e.getConfidence(positiveClassName) + thresholdValue) / 2.0;
			}
		}

		// create and return output
		exampleSetOutput.deliver(exampleSet);
		thresholdOutput.deliver(new Threshold(thresholdValue, label.getMapping().getNegativeString(), label.getMapping()
				.getPositiveString()));
	}

	@Override
	public List<ParameterType> getParameterTypes() {
		List<ParameterType> list = super.getParameterTypes();
		list.add(new ParameterTypeDouble(PARAMETER_RECALL, "The minimal desired recall on the positive class.", 0, 1, .7,
				false));
		list.add(new ParameterTypeBoolean(PARAMETER_USE_EXAMPLE_WEIGHTS, "Indicates if example weights should be used.",
				true));
		list.add(new ParameterTypeString(PARAMETER_POSITIVE_LABEL,
				"If set, this value of the label attribute is treated as positive.", true));
		return list;
	}
}
