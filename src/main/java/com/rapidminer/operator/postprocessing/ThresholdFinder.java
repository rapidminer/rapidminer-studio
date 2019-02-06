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

import java.util.List;

import com.rapidminer.example.Attribute;
import com.rapidminer.example.Attributes;
import com.rapidminer.example.ExampleSet;
import com.rapidminer.example.Tools;
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
import com.rapidminer.parameter.conditions.BooleanParameterCondition;
import com.rapidminer.tools.Ontology;
import com.rapidminer.tools.math.ROCBias;
import com.rapidminer.tools.math.ROCData;
import com.rapidminer.tools.math.ROCDataGenerator;


/**
 * This operator finds the best threshold for crisp classifying based on user defined costs.
 *
 * @author Martin Scholz, Ingo Mierswa
 */
public class ThresholdFinder extends Operator {

	public static final String PARAMETER_DEFINE_LABELS = "define_labels";

	public static final String PARAMETER_FIRST_LABEL = "first_label";
	public static final String PARAMETER_SECOND_LABEL = "second_label";

	public static final String PARAMETER_MISCLASSIFICATION_COSTS_FIRST = "misclassification_costs_first";

	public static final String PARAMETER_MISCLASSIFICATION_COSTS_SECOND = "misclassification_costs_second";

	public static final String PARAMETER_SHOW_ROC_PLOT = "show_roc_plot";

	public static final String PARAMETER_USE_EXAMPLE_WEIGHTS = "use_example_weights";

	private InputPort exampleSetInput = getInputPorts().createPort("example set", ExampleSet.class);
	private OutputPort exampleSetOutput = getOutputPorts().createPort("example set");
	private OutputPort thresholdOutput = getOutputPorts().createPort("threshold");

	public ThresholdFinder(OperatorDescription description) {
		super(description);

		exampleSetInput.addPrecondition(new ExampleSetPrecondition(exampleSetInput, Ontology.VALUE_TYPE,
				Attributes.LABEL_NAME, Attributes.PREDICTION_NAME, Attributes.CONFIDENCE_NAME));
		getTransformer().addPassThroughRule(exampleSetInput, exampleSetOutput);
		getTransformer().addGenerationRule(thresholdOutput, Threshold.class);
	}

	@Override
	public void doWork() throws OperatorException {
		// sanity checks
		ExampleSet exampleSet = exampleSetInput.getData(ExampleSet.class);

		// checking preconditions
		Tools.hasNominalLabels(exampleSet, getOperatorClassName());
		Attribute label = exampleSet.getAttributes().getLabel();
		exampleSet.recalculateAttributeStatistics(label);
		NominalMapping mapping = label.getMapping();
		if (mapping.size() != 2) {
			throw new UserError(this, 118, label, Integer.valueOf(mapping.getValues().size()), Integer.valueOf(2));
		}
		if (exampleSet.getAttributes().getPredictedLabel() == null) {
			throw new UserError(this, 107);
		}
		boolean useExplictLabels = getParameterAsBoolean(PARAMETER_DEFINE_LABELS);

		double secondCost = getParameterAsDouble(PARAMETER_MISCLASSIFICATION_COSTS_SECOND);
		double firstCost = getParameterAsDouble(PARAMETER_MISCLASSIFICATION_COSTS_FIRST);
		if (useExplictLabels) {
			String firstLabel = getParameterAsString(PARAMETER_FIRST_LABEL);
			String secondLabel = getParameterAsString(PARAMETER_SECOND_LABEL);

			if (mapping.getIndex(firstLabel) == -1) {
				throw new UserError(this, 143, firstLabel, label.getName());
			}
			if (mapping.getIndex(secondLabel) == -1) {
				throw new UserError(this, 143, secondLabel, label.getName());
			}

			// if explicit order differs from order in data: internally swap costs.
			if (mapping.getIndex(firstLabel) > mapping.getIndex(secondLabel)) {
				double temp = firstCost;
				firstCost = secondCost;
				secondCost = temp;
			}
		}

		// check whether the confidence attributes are available
		if (exampleSet.getAttributes().getConfidence(mapping.getPositiveString()) == null) {
			throw new UserError(this, 113, Attributes.CONFIDENCE_NAME + "_" + mapping.getPositiveString());
		}
		if (exampleSet.getAttributes().getConfidence(mapping.getNegativeString()) == null) {
			throw new UserError(this, 113, Attributes.CONFIDENCE_NAME + "_" + mapping.getNegativeString());
		}
		// create ROC data
		ROCDataGenerator rocDataGenerator = new ROCDataGenerator(firstCost, secondCost);
		ROCData rocData = rocDataGenerator.createROCData(exampleSet, getParameterAsBoolean(PARAMETER_USE_EXAMPLE_WEIGHTS),
				ROCBias.getROCBiasParameter(this));

		// create plotter
		if (getParameterAsBoolean(PARAMETER_SHOW_ROC_PLOT)) {
			rocDataGenerator.createROCPlotDialog(rocData, true, true);
		}

		// create and return output
		exampleSetOutput.deliver(exampleSet);
		thresholdOutput.deliver(new Threshold(rocDataGenerator.getBestThreshold(), mapping.getNegativeString(), mapping
				.getPositiveString()));
	}

	@Override
	public List<ParameterType> getParameterTypes() {
		List<ParameterType> list = super.getParameterTypes();
		list.add(new ParameterTypeBoolean(PARAMETER_DEFINE_LABELS,
				"If checked, you can define explicitly which is the first and the second label.", false));
		ParameterTypeString type = new ParameterTypeString(PARAMETER_FIRST_LABEL, "The first label.");
		type.registerDependencyCondition(new BooleanParameterCondition(this, PARAMETER_DEFINE_LABELS, true, true));
		list.add(type);
		type = new ParameterTypeString(PARAMETER_SECOND_LABEL, "The second label.");
		type.registerDependencyCondition(new BooleanParameterCondition(this, PARAMETER_DEFINE_LABELS, true, true));
		list.add(type);

		list.add(new ParameterTypeDouble(PARAMETER_MISCLASSIFICATION_COSTS_FIRST,
				"The costs assigned when an example of the first class is classified as one of the second.", 0,
				Double.POSITIVE_INFINITY, 1, false));
		list.add(new ParameterTypeDouble(PARAMETER_MISCLASSIFICATION_COSTS_SECOND,
				"The costs assigned when an example of the second class is classified as one of the first.", 0,
				Double.POSITIVE_INFINITY, 1, false));
		list.add(new ParameterTypeBoolean(PARAMETER_SHOW_ROC_PLOT, "Display a plot of the ROC curve.", false));
		list.add(new ParameterTypeBoolean(PARAMETER_USE_EXAMPLE_WEIGHTS, "Indicates if example weights should be used.",
				true));
		list.add(ROCBias.makeParameterType());
		return list;
	}
}
