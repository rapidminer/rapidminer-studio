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
package com.rapidminer.operator.visualization;

import com.rapidminer.example.Attribute;
import com.rapidminer.example.Attributes;
import com.rapidminer.example.ExampleSet;
import com.rapidminer.example.Tools;
import com.rapidminer.operator.Model;
import com.rapidminer.operator.Operator;
import com.rapidminer.operator.OperatorDescription;
import com.rapidminer.operator.OperatorException;
import com.rapidminer.operator.ProcessSetupError.Severity;
import com.rapidminer.operator.UserError;
import com.rapidminer.operator.learner.PredictionModel;
import com.rapidminer.operator.performance.PerformanceEvaluator;
import com.rapidminer.operator.ports.InputPort;
import com.rapidminer.operator.ports.OutputPort;
import com.rapidminer.operator.ports.metadata.ExampleSetMetaData;
import com.rapidminer.operator.ports.metadata.ExampleSetPrecondition;
import com.rapidminer.operator.ports.metadata.MetaData;
import com.rapidminer.operator.ports.metadata.MetaDataInfo;
import com.rapidminer.operator.ports.metadata.SimplePrecondition;
import com.rapidminer.parameter.ParameterType;
import com.rapidminer.parameter.ParameterTypeBoolean;
import com.rapidminer.parameter.UndefinedParameterError;
import com.rapidminer.tools.Ontology;
import com.rapidminer.tools.math.ROCBias;
import com.rapidminer.tools.math.ROCData;
import com.rapidminer.tools.math.ROCDataGenerator;

import java.util.List;


/**
 * This operator creates a ROC chart for the given example set and model. The model will be applied
 * on the example set and a ROC chart will be produced afterwards. If you are interested in finding
 * an optimal threshold, the operator {@link com.rapidminer.operator.postprocessing.ThresholdFinder}
 * should be used. If you are interested in the performance criterion Area-Under-Curve (AUC) the
 * usual {@link PerformanceEvaluator} can be used. This operator just presents a ROC plot for a
 * given model and data set.
 * 
 * Please note that a predicted label of the given example set will be removed during the
 * application of this operator.
 * 
 * @author Ingo Mierswa
 * 
 */
public class ROCChartGenerator extends Operator {

	public static final String PARAMETER_USE_EXAMPLE_WEIGHTS = "use_example_weights";
	public static final String PARAMETER_USE_MODEL = "use_model";

	private InputPort exampleSetInput = getInputPorts().createPort("example set");
	private InputPort modelInput = getInputPorts().createPort("model");
	private OutputPort exampleSetOutput = getOutputPorts().createPort("example set");
	private OutputPort modelOutput = getOutputPorts().createPort("model");

	public ROCChartGenerator(OperatorDescription description) {
		super(description);

		exampleSetInput
				.addPrecondition(new ExampleSetPrecondition(exampleSetInput, Attributes.LABEL_NAME, Ontology.NOMINAL) {

					@Override
					public void makeAdditionalChecks(ExampleSetMetaData emd) throws UndefinedParameterError {
						MetaDataInfo contained = emd.containsSpecialAttribute(Attributes.PREDICTION_NAME);
						if (!getParameterAsBoolean(PARAMETER_USE_MODEL) && (contained != MetaDataInfo.YES)) {
							if (contained == MetaDataInfo.NO) {
								createError(Severity.ERROR, "exampleset.needs_prediction");
							} else {
								createError(Severity.WARNING, "exampleset.needs_prediction");
							}
						}
					}
				});
		modelInput.addPrecondition(new SimplePrecondition(modelInput, new MetaData(Model.class)) {

			@Override
			protected boolean isMandatory() {
				return getParameterAsBoolean(PARAMETER_USE_MODEL);
			}
		});

		getTransformer().addPassThroughRule(exampleSetInput, exampleSetOutput);
		getTransformer().addPassThroughRule(modelInput, modelOutput);
	}

	@Override
	public void doWork() throws OperatorException {
		ExampleSet exampleSet = exampleSetInput.getData(ExampleSet.class);
		Tools.hasNominalLabels(exampleSet, getOperatorClassName());
		Attribute label = exampleSet.getAttributes().getLabel();
		if (label.getMapping().size() != 2) {
			throw new UserError(this, 114, "ROC Charts", label);
		}

		if (exampleSet.getAttributes().getPredictedLabel() != null && getParameterAsBoolean(PARAMETER_USE_MODEL)) {
			getLogger().warning("Input example already has a predicted label which will be removed.");
			PredictionModel.removePredictedLabel(exampleSet);
		}
		if (exampleSet.getAttributes().getPredictedLabel() == null && !getParameterAsBoolean(PARAMETER_USE_MODEL)) {
			throw new UserError(this, 107);
		}
		Model model = null;
		if (getParameterAsBoolean(PARAMETER_USE_MODEL)) {
			model = modelInput.getData(Model.class);
			exampleSet = model.apply(exampleSet);
		}
		if (exampleSet.getAttributes().getPredictedLabel() == null) {
			throw new UserError(this, 107);
		}

		ROCDataGenerator rocDataGenerator = new ROCDataGenerator(1.0d, 1.0d);
		ROCData rocPoints = rocDataGenerator.createROCData(exampleSet, getParameterAsBoolean(PARAMETER_USE_EXAMPLE_WEIGHTS),
				ROCBias.getROCBiasParameter(this));
		rocDataGenerator.createROCPlotDialog(rocPoints);

		PredictionModel.removePredictedLabel(exampleSet);

		modelOutput.deliver(model);
		exampleSetOutput.deliver(exampleSet);
	}

	@Override
	public List<ParameterType> getParameterTypes() {
		List<ParameterType> types = super.getParameterTypes();
		types.add(new ParameterTypeBoolean(
				PARAMETER_USE_EXAMPLE_WEIGHTS,
				"Indicates if example weights should be used for calculations (use 1 as weights for each example otherwise).",
				true));
		types.add(new ParameterTypeBoolean(
				PARAMETER_USE_MODEL,
				"If checked a given model will be applied for generating ROCChart. If not the examples set must have a predicted label.",
				true));
		types.add(ROCBias.makeParameterType());
		return types;
	}
}
