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

import java.util.List;

import com.rapidminer.datatable.SimpleDataTable;
import com.rapidminer.datatable.SimpleDataTableRow;
import com.rapidminer.example.Attribute;
import com.rapidminer.example.Attributes;
import com.rapidminer.example.Example;
import com.rapidminer.example.ExampleSet;
import com.rapidminer.example.Tools;
import com.rapidminer.operator.Model;
import com.rapidminer.operator.Operator;
import com.rapidminer.operator.OperatorCreationException;
import com.rapidminer.operator.OperatorDescription;
import com.rapidminer.operator.OperatorException;
import com.rapidminer.operator.UserError;
import com.rapidminer.operator.learner.PredictionModel;
import com.rapidminer.operator.ports.InputPort;
import com.rapidminer.operator.ports.OutputPort;
import com.rapidminer.operator.ports.metadata.ExampleSetMetaData;
import com.rapidminer.operator.ports.metadata.ExampleSetPrecondition;
import com.rapidminer.operator.ports.metadata.MetaData;
import com.rapidminer.operator.ports.metadata.MetaDataInfo;
import com.rapidminer.operator.ports.metadata.SimplePrecondition;
import com.rapidminer.operator.preprocessing.NoiseOperator;
import com.rapidminer.operator.preprocessing.PreprocessingOperator;
import com.rapidminer.operator.preprocessing.discretization.AbsoluteDiscretization;
import com.rapidminer.operator.preprocessing.discretization.AbstractDiscretizationOperator;
import com.rapidminer.operator.preprocessing.discretization.BinDiscretization;
import com.rapidminer.operator.preprocessing.discretization.DiscretizationModel;
import com.rapidminer.operator.preprocessing.discretization.FrequencyDiscretization;
import com.rapidminer.operator.preprocessing.filter.attributes.RegexpAttributeFilter;
import com.rapidminer.operator.tools.AttributeSubsetSelector;
import com.rapidminer.parameter.ParameterType;
import com.rapidminer.parameter.ParameterTypeBoolean;
import com.rapidminer.parameter.ParameterTypeCategory;
import com.rapidminer.parameter.ParameterTypeInt;
import com.rapidminer.parameter.ParameterTypeString;
import com.rapidminer.parameter.conditions.BooleanParameterCondition;
import com.rapidminer.parameter.conditions.EqualTypeCondition;
import com.rapidminer.tools.Ontology;
import com.rapidminer.tools.OperatorService;


/**
 * This operator creates a Lift chart based on a Pareto plot for the discretized confidence values
 * for the given example set and model. The model will be applied on the example set and a lift
 * chart will be produced afterwards.
 *
 * Please note that a predicted label of the given example set will be removed during the
 * application of this operator.
 *
 * @author Ingo Mierswa
 */
public class LiftParetoChartGenerator extends Operator {

	public static final String PARAMETER_TARGET_CLASS = "target_class";

	public static final String PARAMETER_BINNING_TYPE = "binning_type";

	public static final String PARAMETER_NUMBER_OF_BINS = "number_of_bins";

	public static final String PARAMETER_SIZE_OF_BINS = "size_of_bins";

	public static final String PARAMETER_AUTOMATIC_NUMBER_OF_DIGITS = "automatic_number_of_digits";

	public static final String PARAMETER_NUMBER_OF_DIGITS = "number_of_digits";

	public static final String PARAMETER_SHOW_BAR_LABELS = "show_bar_labels";

	public static final String PARAMETER_SHOW_CUMULATIVE_LABELS = "show_cumulative_labels";

	public static final String PARAMETER_ROTATE_LABELS = "rotate_labels";

	public static final String[] BINNING_TYPES = { "simple", "absolute", "frequency" };

	public static final int BINNING_SIMPLE = 0;
	public static final int BINNING_ABSOLUTE = 1;
	public static final int BINNING_FREQUENCY = 2;

	private final InputPort exampleSetInput = getInputPorts().createPort("example set");
	private final InputPort modelInput = getInputPorts().createPort("model");

	private final OutputPort exampleSetOutput = getOutputPorts().createPort("example set");
	private final OutputPort modelOutput = getOutputPorts().createPort("model");
	private final OutputPort chartOutput = getOutputPorts().createPort("lift pareto chart");

	public LiftParetoChartGenerator(OperatorDescription description) {
		super(description);

		exampleSetInput
				.addPrecondition(new ExampleSetPrecondition(exampleSetInput, Attributes.LABEL_NAME, Ontology.NOMINAL));
		modelInput.addPrecondition(new SimplePrecondition(modelInput, new MetaData(Model.class)) {

			@Override
			protected boolean isMandatory() {
				MetaData metaData = exampleSetInput.getMetaData();
				if (metaData != null) {
					if (metaData instanceof ExampleSetMetaData) {
						ExampleSetMetaData emd = (ExampleSetMetaData) metaData;
						return emd.containsSpecialAttribute(Attributes.PREDICTION_NAME) == MetaDataInfo.NO;
					}
				}
				return true;
			}
		});

		getTransformer().addPassThroughRule(exampleSetInput, exampleSetOutput);
		getTransformer().addPassThroughRule(modelInput, modelOutput);
		getTransformer().addGenerationRule(chartOutput, LiftParetoChart.class);
	}

	@Override
	public void doWork() throws OperatorException {
		ExampleSet exampleSet = exampleSetInput.getData(ExampleSet.class);
		Tools.hasNominalLabels(exampleSet, getOperatorClassName());

		Attribute labelAttribute = exampleSet.getAttributes().getLabel();

		boolean cleanUp = false;
		Model model = modelInput.getData(Model.class);
		if (exampleSet.getAttributes().getPredictedLabel() != null) {
			logNote("Input example already has a predicted label which will be used by this operator without re-applying the model...");
		} else {
			exampleSet = model.apply(exampleSet);
			cleanUp = true;
		}

		if (exampleSet.getAttributes().getPredictedLabel() == null) {
			throw new UserError(this, 107);
		}

		// create chart
		String targetClass = getParameter(PARAMETER_TARGET_CLASS);

		// check if class is available
		int index = labelAttribute.getMapping().getIndex(targetClass);
		if (index < 0) {
			throw new UserError(this, 143, targetClass, labelAttribute.getName());
		}

		// check if class contains parentheses --> error
		if (targetClass.contains("(")) {
			throw new UserError(this, 207, new Object[] { targetClass, PARAMETER_TARGET_CLASS,
					"the class value is not allowed to contain parenthesis." });
		}
		if (targetClass.contains(")")) {
			throw new UserError(this, 207, new Object[] { targetClass, PARAMETER_TARGET_CLASS,
					"the class value is not allowed to contain parenthesis." });
		}

		// create and setup noise generator
		int binningType = getParameterAsInt(PARAMETER_BINNING_TYPE);
		PreprocessingOperator noiseGeneration;
		try {
			noiseGeneration = OperatorService.createOperator(NoiseOperator.class);
			noiseGeneration.setParameter(NoiseOperator.PARAMETER_LABEL_NOISE, 0d + "");
			noiseGeneration.setParameter(NoiseOperator.PARAMETER_DEFAULT_ATTRIBUTE_NOISE, 0.000001 + "");
			noiseGeneration.setParameter(PreprocessingOperator.PARAMETER_CREATE_VIEW, false + "");

			noiseGeneration.setParameter(AttributeSubsetSelector.PARAMETER_INCLUDE_SPECIAL_ATTRIBUTES, "true");
			noiseGeneration.setParameter(AttributeSubsetSelector.PARAMETER_FILTER_TYPE,
					AttributeSubsetSelector.CONDITION_NAMES[AttributeSubsetSelector.CONDITION_REGULAR_EXPRESSION]);
			noiseGeneration.setParameter(RegexpAttributeFilter.PARAMETER_REGULAR_EXPRESSION,
					Attributes.CONFIDENCE_NAME + "\\(" + targetClass + "\\)");
		} catch (OperatorCreationException e1) {
			// cannot happen
			throw new OperatorException(getName() + ": Cannot create noise operator (" + e1 + ")", e1);
		}
		;

		// create and specify discretization
		AbstractDiscretizationOperator discretization = null;
		int numberOfBins = getParameterAsInt(PARAMETER_NUMBER_OF_BINS);
		try {
			if (binningType == BINNING_SIMPLE) {
				discretization = OperatorService.createOperator(BinDiscretization.class);
				discretization.setParameter(BinDiscretization.PARAMETER_NUMBER_OF_BINS, numberOfBins + "");
				discretization.setParameter(BinDiscretization.PARAMETER_RANGE_NAME_TYPE,
						DiscretizationModel.RANGE_NAME_TYPES[DiscretizationModel.RANGE_NAME_INTERVAL]);
				discretization.setParameter(BinDiscretization.PARAMETER_AUTOMATIC_NUMBER_OF_DIGITS,
						getParameterAsBoolean(PARAMETER_AUTOMATIC_NUMBER_OF_DIGITS) + "");
				discretization.setParameter(BinDiscretization.PARAMETER_NUMBER_OF_DIGITS, numberOfBins + "");
			} else if (binningType == BINNING_ABSOLUTE) {
				discretization = OperatorService.createOperator(AbsoluteDiscretization.class);

				discretization.setParameter(AbsoluteDiscretization.PARAMETER_RANGE_NAME_TYPE,
						DiscretizationModel.RANGE_NAME_TYPES[DiscretizationModel.RANGE_NAME_INTERVAL]);
				discretization.setParameter(AbsoluteDiscretization.PARAMETER_AUTOMATIC_NUMBER_OF_DIGITS,
						getParameterAsBoolean(PARAMETER_AUTOMATIC_NUMBER_OF_DIGITS) + "");
				discretization.setParameter(AbsoluteDiscretization.PARAMETER_NUMBER_OF_DIGITS, numberOfBins + "");

			} else {
				discretization = OperatorService.createOperator(FrequencyDiscretization.class);
				discretization.setParameter(FrequencyDiscretization.PARAMETER_NUMBER_OF_BINS, numberOfBins + "");
				discretization.setParameter(FrequencyDiscretization.PARAMETER_RANGE_NAME_TYPE,
						DiscretizationModel.RANGE_NAME_TYPES[DiscretizationModel.RANGE_NAME_INTERVAL]);
				discretization.setParameter(FrequencyDiscretization.PARAMETER_AUTOMATIC_NUMBER_OF_DIGITS,
						getParameterAsBoolean(PARAMETER_AUTOMATIC_NUMBER_OF_DIGITS) + "");
				discretization.setParameter(FrequencyDiscretization.PARAMETER_NUMBER_OF_DIGITS, numberOfBins + "");
			}
			discretization.setParameter(PreprocessingOperator.PARAMETER_CREATE_VIEW, true + "");
			discretization.setParameter(AttributeSubsetSelector.PARAMETER_INCLUDE_SPECIAL_ATTRIBUTES, "true");
			discretization.setParameter(AttributeSubsetSelector.PARAMETER_FILTER_TYPE,
					AttributeSubsetSelector.CONDITION_NAMES[AttributeSubsetSelector.CONDITION_REGULAR_EXPRESSION]);
			discretization.setParameter(RegexpAttributeFilter.PARAMETER_REGULAR_EXPRESSION,
					Attributes.CONFIDENCE_NAME + "\\(" + targetClass + "\\)");
		} catch (OperatorCreationException e) {
			// cannot happen
			throw new OperatorException(getName() + ": Cannot create discretization operator (" + e + ")");
		}

		// apply discretization
		ExampleSet discretizedData = (ExampleSet) exampleSet.clone();
		if (binningType == BINNING_ABSOLUTE) {
			discretization.setParameter(AbsoluteDiscretization.PARAMETER_SIZE_OF_BINS,
					getParameterAsString(PARAMETER_SIZE_OF_BINS) + "");
			discretizedData = noiseGeneration.doWork(discretizedData);
			discretizedData = discretization.doWork(discretizedData);
		} else {
			// Frequency or Bin discretization
			int startNumber = numberOfBins;
			boolean valid = false;
			while (!valid && numberOfBins >= 2) {
				try {
					discretizedData = noiseGeneration.doWork(discretizedData);
					discretizedData = discretization.doWork(discretizedData);
				} catch (UserError e) {
					numberOfBins--;
					continue;
				}
				valid = true;
			}
			if (numberOfBins != startNumber) {
				logWarning("Cannot use specified number of bins (" + startNumber
						+ ") since the confidence values do not differ enough in order to distinguish enough bins. Using "
						+ numberOfBins + " instead.");
			}
		}

		// create lift data
		Attribute confidenceAttribute = discretizedData.getAttributes()
				.get(Attributes.CONFIDENCE_NAME + "(" + targetClass + ")");
		labelAttribute = discretizedData.getAttributes().getLabel();
		SimpleDataTable dataTable = new SimpleDataTable("Lift Data",
				new String[] { "Confidence for " + targetClass, labelAttribute.getName() });
		for (Example example : discretizedData) {
			String confidenceValue = example.getValueAsString(confidenceAttribute);
			String classValue = example.getNominalValue(labelAttribute);
			dataTable.add(new SimpleDataTableRow(
					new double[] { dataTable.mapString(0, confidenceValue), dataTable.mapString(1, classValue) }));
		}

		// clean up
		if (cleanUp) {
			PredictionModel.removePredictedLabel(exampleSet);
		}

		exampleSetOutput.deliver(exampleSet);
		modelOutput.deliver(model);
		chartOutput.deliver(new LiftParetoChart(dataTable, targetClass, getParameterAsBoolean(PARAMETER_SHOW_BAR_LABELS),
				getParameterAsBoolean(PARAMETER_SHOW_CUMULATIVE_LABELS), getParameterAsBoolean(PARAMETER_ROTATE_LABELS)));
	}

	@Override
	public List<ParameterType> getParameterTypes() {
		List<ParameterType> types = super.getParameterTypes();
		types.add(new ParameterTypeString(PARAMETER_TARGET_CLASS,
				"Indicates the target class for which the lift chart should be produced.", false, false));

		ParameterType type = new ParameterTypeCategory(PARAMETER_BINNING_TYPE,
				"Indicates the binning type of the confidences.", BINNING_TYPES, BINNING_FREQUENCY);
		type.setExpert(false);
		types.add(type);

		type = new ParameterTypeInt(PARAMETER_NUMBER_OF_BINS, "The confidence is discretized in this number of bins.", 2,
				Integer.MAX_VALUE, 10, false);
		type.registerDependencyCondition(new EqualTypeCondition(this, PARAMETER_BINNING_TYPE, BINNING_TYPES, false,
				BINNING_SIMPLE, BINNING_FREQUENCY));
		types.add(type);

		type = new ParameterTypeInt(PARAMETER_SIZE_OF_BINS,
				"The confidence is discretized so that each bin contains this amount of examples.", 1, Integer.MAX_VALUE,
				1000);
		type.setExpert(false);
		type.registerDependencyCondition(
				new EqualTypeCondition(this, PARAMETER_BINNING_TYPE, BINNING_TYPES, false, BINNING_ABSOLUTE));
		types.add(type);

		type = new ParameterTypeBoolean(PARAMETER_AUTOMATIC_NUMBER_OF_DIGITS,
				"Indicates if the number of digits should be automatically determined for the range names.", true);
		type.setExpert(false);
		types.add(type);

		type = new ParameterTypeInt(PARAMETER_NUMBER_OF_DIGITS,
				"The minimum number of digits used for the interval names (-1: determine minimal number automatically).", -1,
				Integer.MAX_VALUE, -1);
		type.setExpert(false);
		type.registerDependencyCondition(
				new BooleanParameterCondition(this, PARAMETER_AUTOMATIC_NUMBER_OF_DIGITS, false, false));
		types.add(type);

		types.add(new ParameterTypeBoolean(PARAMETER_SHOW_BAR_LABELS,
				"Indicates if the bars should display the size of the bin together with the amount of the target class in the corresponding bin.",
				true, false));
		types.add(new ParameterTypeBoolean(PARAMETER_SHOW_CUMULATIVE_LABELS,
				"Indicates if the cumulative line plot should display the cumulative sizes of the bins together with the cumulative amount of the target class in the corresponding bins.",
				false, false));
		types.add(new ParameterTypeBoolean(PARAMETER_ROTATE_LABELS, "Indicates if the labels of the bins should be rotated.",
				false, false));

		return types;
	}
}
