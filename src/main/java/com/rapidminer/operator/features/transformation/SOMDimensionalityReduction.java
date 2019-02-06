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
package com.rapidminer.operator.features.transformation;

import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;

import com.rapidminer.example.Attribute;
import com.rapidminer.example.Example;
import com.rapidminer.example.ExampleSet;
import com.rapidminer.operator.Model;
import com.rapidminer.operator.Operator;
import com.rapidminer.operator.OperatorDescription;
import com.rapidminer.operator.OperatorException;
import com.rapidminer.operator.ProcessSetupError.Severity;
import com.rapidminer.operator.ProcessStoppedException;
import com.rapidminer.operator.SimpleProcessSetupError;
import com.rapidminer.operator.UserError;
import com.rapidminer.operator.ports.IncompatibleMDClassException;
import com.rapidminer.operator.ports.InputPort;
import com.rapidminer.operator.ports.OutputPort;
import com.rapidminer.operator.ports.metadata.AttributeMetaData;
import com.rapidminer.operator.ports.metadata.ExampleSetMetaData;
import com.rapidminer.operator.ports.metadata.ExampleSetPassThroughRule;
import com.rapidminer.operator.ports.metadata.ExampleSetPrecondition;
import com.rapidminer.operator.ports.metadata.SetRelation;
import com.rapidminer.operator.ports.quickfix.ParameterSettingQuickFix;
import com.rapidminer.operator.ports.quickfix.QuickFix;
import com.rapidminer.operator.preprocessing.PreprocessingOperator;
import com.rapidminer.parameter.ParameterType;
import com.rapidminer.parameter.ParameterTypeBoolean;
import com.rapidminer.parameter.ParameterTypeDouble;
import com.rapidminer.parameter.ParameterTypeInt;
import com.rapidminer.parameter.UndefinedParameterError;
import com.rapidminer.tools.Ontology;
import com.rapidminer.tools.math.container.Range;
import com.rapidminer.tools.math.som.KohonenNet;
import com.rapidminer.tools.math.som.RandomDataContainer;
import com.rapidminer.tools.math.som.RitterAdaptation;


/**
 * This operator performs a dimensionality reduction based on a SOM (Self Organizing Map, aka
 * Kohonen net).
 *
 * @author Sebastian Land, Ingo Mierswa
 */
public class SOMDimensionalityReduction extends Operator {

	/**
	 * The parameter name for &quot;Defines the number of dimensions, the data shall be
	 * reduced.&quot;
	 */
	public static final String PARAMETER_NUMBER_OF_DIMENSIONS = "number_of_dimensions";

	/**
	 * The parameter name for &quot;Defines the size of the SOM net, by setting the length of every
	 * edge of the net.&quot;
	 */
	public static final String PARAMETER_NET_SIZE = "net_size";

	/** The parameter name for &quot;Defines the number of training rounds&quot; */
	public static final String PARAMETER_TRAINING_ROUNDS = "training_rounds";

	/**
	 * The parameter name for &quot;Defines the strength of an adaption in the first round. The
	 * strength will decrease every round until it reaches the learning_rate_end in the last
	 * round.&quot;
	 */
	public static final String PARAMETER_LEARNING_RATE_START = "learning_rate_start";

	/**
	 * The parameter name for &quot;Defines the strength of an adaption in the last round. The
	 * strength will decrease to this value in last round, beginning with learning_rate_start in the
	 * first round.&quot;
	 */
	public static final String PARAMETER_LEARNING_RATE_END = "learning_rate_end";

	/**
	 * The parameter name for &quot;Defines the radius of the sphere around an stimulus, within an
	 * adaption occurs. This radius decreases every round, starting by adaption_radius_start in
	 * first round, to adaption_radius_end in last round.&quot;
	 */
	public static final String PARAMETER_ADAPTION_RADIUS_START = "adaption_radius_start";

	/**
	 * The parameter name for &quot;Defines the radius of the sphere around an stimulus, within an
	 * adaption occurs. This radius decreases every round, starting by adaption_radius_start in
	 * first round, to adaption_radius_end in last round.&quot;
	 */
	public static final String PARAMETER_ADAPTION_RADIUS_END = "adaption_radius_end";

	private static final String PROBLEM_MESSAGE_TOO_MUCH_RAM = "self_organizing_map.too_much_ram";

	private static final String PROBLEM_MESSAGE_TOO_HIGH_PARAMETER_VALUES = "self_organizing_map.too_high_parameter_values";

	private static final String USER_ERROR_TOO_HIGH_PARAMETER_VALUES = "som_dimension_reduction.too_high_parameter_values";

	/**
	 * This factor determines how many double values per byte the operator accepts before triggering
	 * a warning. Multiplied with the max memory (in byte) the factor can be used to calculate if
	 * the operator can run through with the entered net_size and number_of_dimensions parameters
	 * and the number of attributes. It should not make full use of the ram, because this value is
	 * only used to trigger a process setup error which does not have to be considered.
	 */
	private static final double MEMORY_FACTOR = 0.1; // The theoretical maximum is 0.125

	/**
	 * maxMemory of JVM in byte
	 */
	private long maxMem = Runtime.getRuntime().maxMemory();

	private InputPort exampleSetInput = getInputPorts().createPort("example set input", ExampleSet.class);

	private OutputPort exampleSetOutput = getOutputPorts().createPort("example set output");
	private OutputPort originalOutput = getOutputPorts().createPort("original");
	private OutputPort modelOutput = getOutputPorts().createPort("preprocessing model");

	public SOMDimensionalityReduction(OperatorDescription description) {
		super(description);

		exampleSetInput.addPrecondition(new ExampleSetPrecondition(exampleSetInput, Ontology.NUMERICAL));
		getTransformer().addGenerationRule(modelOutput, Model.class);
		getTransformer().addRule(new ExampleSetPassThroughRule(exampleSetInput, exampleSetOutput, SetRelation.EQUAL) {

			@Override
			public ExampleSetMetaData modifyExampleSet(ExampleSetMetaData metaData) throws UndefinedParameterError {
				metaData.clearRegular();
				int netSize = getParameterAsInt(PARAMETER_NET_SIZE);
				int numberOfDimensions = getParameterAsInt(PARAMETER_NUMBER_OF_DIMENSIONS);
				for (int i = 0; i < numberOfDimensions; i++) {
					AttributeMetaData newAMD = new AttributeMetaData("SOM_" + i, Ontology.REAL);
					newAMD.setValueRange(new Range(0, netSize - 1), SetRelation.EQUAL);
					metaData.addAttribute(newAMD);
				}
				return super.modifyExampleSet(metaData);
			}
		});
		getTransformer().addPassThroughRule(exampleSetInput, originalOutput);
	}

	@Override
	public void doWork() throws OperatorException {
		ExampleSet exampleSet = exampleSetInput.getData(ExampleSet.class);

		// Get and check parameter values
		int trainingRounds = getParameterAsInt(PARAMETER_TRAINING_ROUNDS);
		int netSize = getParameterAsInt(PARAMETER_NET_SIZE);
		int dimensions = getParameterAsInt(PARAMETER_NUMBER_OF_DIMENSIONS);
		double learningRateStart = getParameterAsDouble(PARAMETER_LEARNING_RATE_START);
		double learningRateEnd = getParameterAsDouble(PARAMETER_LEARNING_RATE_END);
		double adaptionRadiusStart = getParameterAsDouble(PARAMETER_ADAPTION_RADIUS_START);
		double adaptionRadiusEnd = getParameterAsDouble(PARAMETER_ADAPTION_RADIUS_END);

		// check for too big parameter values
		double numOfNodes = Math.pow(netSize, dimensions);
		if (numOfNodes > Integer.MAX_VALUE) {
			throw new UserError(this, USER_ERROR_TOO_HIGH_PARAMETER_VALUES);
		}

		// checking sanity of parameter values
		if (learningRateStart < learningRateEnd) {
			throw new UserError(this, 116, "learning_rate_start",
					"Learning rate at first round must be greater than in last round: (" + learningRateEnd + ")");
		}
		if (adaptionRadiusStart < adaptionRadiusEnd) {
			throw new UserError(this, 116, "adaption_radius_start",
					"Adaption radius at first round must be greater than in last round: (" + adaptionRadiusEnd + ")");
		}

		// train SOM
		KohonenNet net = prepareSOM(exampleSet, dimensions, netSize, trainingRounds, learningRateStart, learningRateEnd,
				adaptionRadiusStart, adaptionRadiusEnd);

		SOMDimensionalityReductionModel model = new SOMDimensionalityReductionModel(exampleSet, net, dimensions);

		if (exampleSetOutput.isConnected()) {
			exampleSetOutput.deliver(model.apply((ExampleSet) exampleSet.clone()));
		}
		originalOutput.deliver(exampleSet);
		modelOutput.deliver(model);
	}

	private KohonenNet prepareSOM(ExampleSet exampleSet, int netDimensions, int netSize, int trainingRounds,
			double learningRateStart, double learningRateEnd, double adaptionRadiusStart, double adaptionRadiusEnd)
			throws ProcessStoppedException {
		// generating data for SOM
		int dataDimension;
		RandomDataContainer data = new RandomDataContainer();
		synchronized (exampleSet) {
			Iterator<Example> iterator = exampleSet.iterator();
			dataDimension = exampleSet.getAttributes().size();
			while (iterator.hasNext()) {
				data.addData(getDoubleArrayFromExample(iterator.next()));
				this.checkForStop();
			}
		}
		// generating SOM
		KohonenNet net = new KohonenNet(data);
		RitterAdaptation adaptionFunction = new RitterAdaptation();
		adaptionFunction.setAdaptationRadiusStart(adaptionRadiusStart);
		adaptionFunction.setAdaptationRadiusEnd(adaptionRadiusEnd);
		adaptionFunction.setLearnRateStart(learningRateStart);
		adaptionFunction.setLearnRateEnd(learningRateEnd);
		net.setAdaptationFunction(adaptionFunction);
		int[] dimensions = new int[netDimensions];
		for (int i = 0; i < netDimensions; i++) {
			dimensions[i] = netSize;
		}
		net.init(dataDimension, dimensions, false);
		// train SOM
		net.setTrainingRounds(trainingRounds);
		this.checkForStop();
		net.train(this);
		return net;
	}

	public static double[] getDoubleArrayFromExample(Example example) {
		double[] doubleRow = new double[example.getAttributes().size()];
		int i = 0;
		for (Attribute attribute : example.getAttributes()) {
			doubleRow[i++] = example.getValue(attribute);
		}
		return doubleRow;
	}

	@Override
	public List<ParameterType> getParameterTypes() {
		List<ParameterType> types = super.getParameterTypes();
		types.add(new ParameterTypeBoolean(PreprocessingOperator.PARAMETER_RETURN_PREPROCESSING_MODEL,
				"Indicates if the preprocessing model should also be returned", false));
		ParameterType type = new ParameterTypeInt(PARAMETER_NUMBER_OF_DIMENSIONS,
				"Defines the number of dimensions, the data shall be reduced.", 1, Integer.MAX_VALUE, 2);
		type.setExpert(false);
		types.add(type);
		type = new ParameterTypeInt(PARAMETER_NET_SIZE,
				"Defines the size of the SOM net, by setting the length of every edge of the net.", 1, Integer.MAX_VALUE,
				10);
		type.setExpert(false);
		types.add(type);
		type = new ParameterTypeInt(PARAMETER_TRAINING_ROUNDS, "Defines the number of training rounds", 1, Integer.MAX_VALUE,
				30);
		type.setExpert(false);
		types.add(type);
		types.add(new ParameterTypeDouble(PARAMETER_LEARNING_RATE_START,
				"Defines the strength of an adaption in the first round. The strength will decrease every round until it reaches the learning_rate_end in the last round.",
				0.0d, Double.POSITIVE_INFINITY, 0.8d));
		types.add(new ParameterTypeDouble(PARAMETER_LEARNING_RATE_END,
				"Defines the strength of an adaption in the last round. The strength will decrease to this value in last round, beginning with learning_rate_start in the first round.",
				0.0d, Double.POSITIVE_INFINITY, 0.01d));
		types.add(new ParameterTypeDouble(PARAMETER_ADAPTION_RADIUS_START,
				"Defines the radius of the sphere around an stimulus, within an adaption occoures. This radius decreases every round, starting by adaption_radius_start in first round, to adaption_radius_end in last round.",
				0.0d, Double.POSITIVE_INFINITY, 10.0d));
		types.add(new ParameterTypeDouble(PARAMETER_ADAPTION_RADIUS_END,
				"Defines the radius of the sphere around an stimulus, within an adaption occoures. This radius decreases every round, starting by adaption_radius_start in first round, to adaption_radius_end in last round.",
				0.0d, Double.POSITIVE_INFINITY, 1.0d));

		return types;
	}

	@Override
	public void transformMetaData() {
		super.transformMetaData();
		try {
			// Check if the operator could need too much RAM
			int netSize = getParameterAsInt(PARAMETER_NET_SIZE);
			int dimensions = getParameterAsInt(PARAMETER_NUMBER_OF_DIMENSIONS);
			double numberOfNodes = Math.pow(netSize, dimensions);
			ExampleSetMetaData metaData = exampleSetInput.getMetaData(ExampleSetMetaData.class);
			if (metaData != null) {
				int numberOfRegularAttributes = metaData.getNumberOfRegularAttributes();

				// Better value calculation would fail for numberOfRegularAttributes = 0 and we can
				// assume that there is at least 1 attribute.
				if (numberOfRegularAttributes <= 0) {
					numberOfRegularAttributes = 1;
				}

				if (numberOfNodes * numberOfRegularAttributes > maxMem * MEMORY_FACTOR
						|| numberOfNodes > Integer.MAX_VALUE) {

					// Calculate better values for quick fixes
					int betterNetSize = (int) Math.pow(maxMem * MEMORY_FACTOR / numberOfRegularAttributes, 1.0 / dimensions);
					int betterDimensions = (int) (Math.log10(maxMem * MEMORY_FACTOR / numberOfRegularAttributes)
							/ Math.log10(netSize));

					ParameterSettingQuickFix fixNetSize = new ParameterSettingQuickFix(this, PARAMETER_NET_SIZE,
							Integer.toString(betterNetSize));
					ParameterSettingQuickFix fixDimensions = new ParameterSettingQuickFix(this,
							PARAMETER_NUMBER_OF_DIMENSIONS, Integer.toString(betterDimensions));
					List<QuickFix> quickFixList = new ArrayList<>();
					quickFixList.add(fixNetSize);
					quickFixList.add(fixDimensions);

					if (numberOfNodes > Integer.MAX_VALUE) {
						addError(new SimpleProcessSetupError(Severity.ERROR, getPortOwner(), quickFixList,
								PROBLEM_MESSAGE_TOO_HIGH_PARAMETER_VALUES));
					} else {
						addError(new SimpleProcessSetupError(Severity.WARNING, getPortOwner(), quickFixList,
								PROBLEM_MESSAGE_TOO_MUCH_RAM));
					}
				}
			}
		} catch (UndefinedParameterError | IncompatibleMDClassException e) {
			// Don't care, just don't show the ProcessSetupError
		}
	}
}
