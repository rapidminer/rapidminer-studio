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
package com.rapidminer.operator.preprocessing.sampling;

import java.util.Collections;
import java.util.List;
import java.util.logging.Level;

import com.rapidminer.example.Attribute;
import com.rapidminer.example.Attributes;
import com.rapidminer.example.Example;
import com.rapidminer.example.ExampleSet;
import com.rapidminer.example.Tools;
import com.rapidminer.example.set.MappedExampleSet;
import com.rapidminer.example.set.SplittedExampleSet;
import com.rapidminer.operator.OperatorDescription;
import com.rapidminer.operator.OperatorException;
import com.rapidminer.operator.ProcessSetupError.Severity;
import com.rapidminer.operator.UserError;
import com.rapidminer.operator.annotation.ResourceConsumptionEstimator;
import com.rapidminer.operator.ports.metadata.ExampleSetMetaData;
import com.rapidminer.operator.ports.metadata.ExampleSetPrecondition;
import com.rapidminer.operator.ports.metadata.MDInteger;
import com.rapidminer.operator.ports.metadata.MetaDataInfo;
import com.rapidminer.operator.ports.metadata.ParameterConditionedPrecondition;
import com.rapidminer.operator.ports.metadata.SimpleMetaDataError;
import com.rapidminer.operator.ports.quickfix.ParameterSettingQuickFix;
import com.rapidminer.operator.preprocessing.sampling.sequences.AbsoluteSamplingSequenceGenerator;
import com.rapidminer.operator.preprocessing.sampling.sequences.ProbabilitySamplingSequenceGenerator;
import com.rapidminer.operator.preprocessing.sampling.sequences.RelativeSamplingSequenceGenerator;
import com.rapidminer.operator.preprocessing.sampling.sequences.SamplingSequenceGenerator;
import com.rapidminer.parameter.ParameterType;
import com.rapidminer.parameter.ParameterTypeBoolean;
import com.rapidminer.parameter.ParameterTypeCategory;
import com.rapidminer.parameter.ParameterTypeDouble;
import com.rapidminer.parameter.ParameterTypeInt;
import com.rapidminer.parameter.ParameterTypeList;
import com.rapidminer.parameter.ParameterTypeString;
import com.rapidminer.parameter.UndefinedParameterError;
import com.rapidminer.parameter.conditions.BooleanParameterCondition;
import com.rapidminer.parameter.conditions.EqualTypeCondition;
import com.rapidminer.tools.LogService;
import com.rapidminer.tools.Ontology;
import com.rapidminer.tools.OperatorResourceConsumptionHandler;
import com.rapidminer.tools.RandomGenerator;


/**
 * This operator will sample the given example set without replacement. Three modes are available:
 * Absolute returning a determined number, relative returning a determined fraction of the input set
 * and probability, that will return each example with the same probability.
 *
 * The operator offers the possibility to specify sampling parameter per class to re-balance the
 * data.
 *
 * @author Sebastian Land, Ingo Mierswa, Tobias Malbrecht
 */
public class SamplingOperator extends AbstractSamplingOperator {

	public static final String PARAMETER_SAMPLE = "sample";

	public static String[] SAMPLE_MODES = { "absolute", "relative", "probability" };

	public static final int SAMPLE_ABSOLUTE = 0;

	public static final int SAMPLE_RELATIVE = 1;

	public static final int SAMPLE_PROBABILITY = 2;

	/** The parameter name for &quot;The number of examples which should be sampled&quot; */
	public static final String PARAMETER_SAMPLE_SIZE = "sample_size";

	/** The parameter name for &quot;The fraction of examples which should be sampled&quot; */
	public static final String PARAMETER_SAMPLE_RATIO = "sample_ratio";

	public static final String PARAMETER_SAMPLE_PROBABILITY = "sample_probability";

	public static final String PARAMETER_BALANCE_DATA = "balance_data";

	public static final String PARAMETER_SAMPLE_SIZE_LIST = "sample_size_per_class";

	public static final String PARAMETER_SAMPLE_RATIO_LIST = "sample_ratio_per_class";

	public static final String PARAMETER_SAMPLE_PROBABILITY_LIST = "sample_probability_per_class";

	public SamplingOperator(OperatorDescription description) {
		super(description);

		ExampleSetPrecondition needNominalLabelCondition = new ExampleSetPrecondition(getExampleSetInputPort(),
				Attributes.LABEL_NAME, Ontology.NOMINAL);
		getExampleSetInputPort().addPrecondition(new ParameterConditionedPrecondition(getExampleSetInputPort(),
				needNominalLabelCondition, getParameterHandler(), PARAMETER_BALANCE_DATA, Boolean.toString(true)));
	}

	@Override
	protected MDInteger getSampledSize(ExampleSetMetaData emd) throws UndefinedParameterError {
		boolean balanceData = getParameterAsBoolean(PARAMETER_BALANCE_DATA);
		int absoluteNumber = 0;
		switch (getParameterAsInt(PARAMETER_SAMPLE)) {
			case SAMPLE_ABSOLUTE:
				if (balanceData) {
					List<String[]> parameterList = getParameterList(PARAMETER_SAMPLE_SIZE_LIST);
					for (String[] pair : parameterList) {
						absoluteNumber += Integer.valueOf(pair[1]);
					}
				} else {
					absoluteNumber = getParameterAsInt(PARAMETER_SAMPLE_SIZE);
				}
				if (emd.getNumberOfExamples().isAtLeast(absoluteNumber) == MetaDataInfo.NO) {
					getExampleSetInputPort().addError(new SimpleMetaDataError(Severity.ERROR, getExampleSetInputPort(),
							Collections.singletonList(new ParameterSettingQuickFix(this, PARAMETER_SAMPLE_SIZE,
									emd.getNumberOfExamples().getValue().toString())),
							"exampleset.need_more_examples", absoluteNumber + ""));
				}
				return new MDInteger(absoluteNumber);
			case SAMPLE_RELATIVE:
				if (balanceData) {
					MDInteger numberOfExamples = emd.getNumberOfExamples();
					numberOfExamples.reduceByUnknownAmount();
					return numberOfExamples;
				}
				if (emd.getNumberOfExamples().isKnown()) {
					return new MDInteger(
							(int) (getParameterAsDouble(PARAMETER_SAMPLE_RATIO) * emd.getNumberOfExamples().getValue()));
				}
				return new MDInteger();
			case SAMPLE_PROBABILITY:
				if (balanceData) {
					MDInteger numberOfExamples = emd.getNumberOfExamples();
					numberOfExamples.reduceByUnknownAmount();
					return numberOfExamples;
				}
				if (emd.getNumberOfExamples().isKnown()) {
					return new MDInteger((int) (getParameterAsDouble(PARAMETER_SAMPLE_PROBABILITY)
							* emd.getNumberOfExamples().getValue()));
				}
				return new MDInteger();
			default:
				return new MDInteger();
		}
	}

	@Override
	public ExampleSet apply(ExampleSet originalSet) throws OperatorException {
		int resultSize = 0;
		int[] usedIndices = new int[originalSet.size()];

		SplittedExampleSet perLabelSets = null;
		int numberOfIterations = 1; // if sampling not per class, just one iteration
		boolean balanceData = getParameterAsBoolean(PARAMETER_BALANCE_DATA);
		int sample = getParameterAsInt(PARAMETER_SAMPLE);
		Attribute label = null;
		List<String[]> pairs = null;
		ExampleSet exampleSet = null;
		if (balanceData) {
			Tools.hasNominalLabels(originalSet, this.getOperatorClassName());
			label = originalSet.getAttributes().getLabel();
				perLabelSets = SplittedExampleSet.splitByAttribute(originalSet, label);
				exampleSet = perLabelSets;

			switch (sample) {
				case SAMPLE_RELATIVE:
					pairs = getParameterList(PARAMETER_SAMPLE_RATIO_LIST);
					break;
				case SAMPLE_ABSOLUTE:
					pairs = getParameterList(PARAMETER_SAMPLE_SIZE_LIST);
					break;
				case SAMPLE_PROBABILITY:
				default:
					pairs = getParameterList(PARAMETER_SAMPLE_PROBABILITY_LIST);
			}
			numberOfIterations = pairs.size();

			int numberOfSubsets = perLabelSets.getNumberOfSubsets();
			if (numberOfSubsets < numberOfIterations) {
				LogService.getRoot().log(Level.WARNING,
						"com.rapidminer.operator.preprocessing.sampling.SamplingOperator.too_few_subsets",
						new Object[] { numberOfIterations - numberOfSubsets, numberOfIterations, numberOfSubsets });
				numberOfIterations = numberOfSubsets;
			}
		} else {
			exampleSet = originalSet;
		}

		double sample_ratio = getParameterAsDouble(PARAMETER_SAMPLE_RATIO);
		int sampleSize = getParameterAsInt(PARAMETER_SAMPLE_SIZE);
		double sampleProbability = getParameterAsDouble(PARAMETER_SAMPLE_PROBABILITY);

		// now iterate over all subsets
		for (int i = 0; i < numberOfIterations; i++) {
			SamplingSequenceGenerator sampleSequence = null;
			RandomGenerator randomGenerator = RandomGenerator.getRandomGenerator(this);
			if (balanceData) {
				perLabelSets.clearSelection();
				perLabelSets.selectAdditionalSubset(i);
				String parameter = "0";

				// finding parameter list of the selected sample method
				if (exampleSet.size() > 0) {
					// getting parameter value for current class
					Example next = exampleSet.iterator().next();
					String labelValue = next.getValueAsString(label);
					for (String[] pair : pairs) {
						if (labelValue.equals(pair[0])) {
							parameter = pair[1];
							break;
						}
					}
				}

				// now generate sampling sequence for this class's parameter value
				switch (sample) {
					case SAMPLE_RELATIVE:
						sampleSequence = new RelativeSamplingSequenceGenerator(exampleSet.size(), Double.valueOf(parameter),
								randomGenerator);
						break;
					case SAMPLE_ABSOLUTE:
						sampleSequence = new AbsoluteSamplingSequenceGenerator(exampleSet.size(), Integer.valueOf(parameter),
								randomGenerator);
						break;
					case SAMPLE_PROBABILITY:
					default:
						sampleSequence = new ProbabilitySamplingSequenceGenerator(Double.valueOf(parameter),
								randomGenerator);
						break;
				}

			} else {
				// just retrieve the standard parameters
				switch (sample) {
					case SAMPLE_RELATIVE:
						sampleSequence = new RelativeSamplingSequenceGenerator(exampleSet.size(), sample_ratio,
								randomGenerator);
						break;
					case SAMPLE_ABSOLUTE:
						if (sampleSize > exampleSet.size()) {
							throw new UserError(this, 110, sampleSize);
						}
						sampleSequence = new AbsoluteSamplingSequenceGenerator(exampleSet.size(), sampleSize,
								randomGenerator);
						break;
					case SAMPLE_PROBABILITY:
					default:
						sampleSequence = new ProbabilitySamplingSequenceGenerator(sampleProbability, randomGenerator);
						break;
				}
			}

			// add indices which are used
			for (int j = 0; j < exampleSet.size(); j++) {
				if (sampleSequence.useNext()) {
					if (balanceData) {
						usedIndices[resultSize] = perLabelSets.getActualParentIndex(j);
					} else {
						usedIndices[resultSize] = j;
					}
					resultSize++;
				}
			}
		}

		// create new filtered example set
		int[] resultIndices = new int[resultSize];
		System.arraycopy(usedIndices, 0, resultIndices, 0, resultSize);

		return new MappedExampleSet(originalSet, resultIndices, true, true);
	}

	@Override
	public List<ParameterType> getParameterTypes() {
		List<ParameterType> types = super.getParameterTypes();
		ParameterType type = new ParameterTypeCategory(PARAMETER_SAMPLE, "Determines how the amount of data is specified.",
				SAMPLE_MODES, SAMPLE_ABSOLUTE);
		type.setExpert(false);
		types.add(type);
		type = new ParameterTypeBoolean(PARAMETER_BALANCE_DATA,
				"If you need to sample differently for examples of a certain class, you might check this.", false, true);
		types.add(type);

		type = new ParameterTypeInt(PARAMETER_SAMPLE_SIZE, "The number of examples which should be sampled", 1,
				Integer.MAX_VALUE, 100);
		type.registerDependencyCondition(
				new EqualTypeCondition(this, PARAMETER_SAMPLE, SAMPLE_MODES, true, SAMPLE_ABSOLUTE));
		type.registerDependencyCondition(new BooleanParameterCondition(this, PARAMETER_BALANCE_DATA, true, false));
		type.setExpert(false);
		types.add(type);
		type = new ParameterTypeDouble(PARAMETER_SAMPLE_RATIO, "The fraction of examples which should be sampled", 0.0d,
				1.0d, 0.1d);
		type.registerDependencyCondition(
				new EqualTypeCondition(this, PARAMETER_SAMPLE, SAMPLE_MODES, true, SAMPLE_RELATIVE));
		type.registerDependencyCondition(new BooleanParameterCondition(this, PARAMETER_BALANCE_DATA, true, false));
		type.setExpert(false);
		types.add(type);
		type = new ParameterTypeDouble(PARAMETER_SAMPLE_PROBABILITY, "The sample probability for each example.", 0.0d, 1.0d,
				0.1d);
		type.registerDependencyCondition(
				new EqualTypeCondition(this, PARAMETER_SAMPLE, SAMPLE_MODES, true, SAMPLE_PROBABILITY));
		type.registerDependencyCondition(new BooleanParameterCondition(this, PARAMETER_BALANCE_DATA, true, false));
		type.setExpert(false);
		types.add(type);

		type = new ParameterTypeList(PARAMETER_SAMPLE_SIZE_LIST, "The absolute sample size per class.",
				new ParameterTypeString("class", "The class name this sample size applies to."),
				new ParameterTypeInt("size", "The number of sampled examples of this class.", 0, Integer.MAX_VALUE));
		type.registerDependencyCondition(
				new EqualTypeCondition(this, PARAMETER_SAMPLE, SAMPLE_MODES, true, SAMPLE_ABSOLUTE));
		type.registerDependencyCondition(new BooleanParameterCondition(this, PARAMETER_BALANCE_DATA, true, true));
		type.setExpert(false);
		types.add(type);
		type = new ParameterTypeList(PARAMETER_SAMPLE_RATIO_LIST, "The fraction per class.",
				new ParameterTypeString("class", "The class name this sample size applies to."),
				new ParameterTypeDouble("ratio", "The fractions of examples of this class.", 0, 1));
		type.registerDependencyCondition(
				new EqualTypeCondition(this, PARAMETER_SAMPLE, SAMPLE_MODES, true, SAMPLE_RELATIVE));
		type.registerDependencyCondition(new BooleanParameterCondition(this, PARAMETER_BALANCE_DATA, true, true));
		type.setExpert(false);
		types.add(type);
		type = new ParameterTypeList(PARAMETER_SAMPLE_PROBABILITY_LIST, "The fraction per class.",
				new ParameterTypeString("class", "The class name this sample size applies to."), new ParameterTypeDouble(
						"probability", "The probability of examples of this class to belong to the sample.", 0, 1));
		type.registerDependencyCondition(
				new EqualTypeCondition(this, PARAMETER_SAMPLE, SAMPLE_MODES, true, SAMPLE_PROBABILITY));
		type.registerDependencyCondition(new BooleanParameterCondition(this, PARAMETER_BALANCE_DATA, true, true));
		type.setExpert(false);
		types.add(type);

		types.addAll(RandomGenerator.getRandomGeneratorParameters(this));
		return types;
	}

	@Override
	public ResourceConsumptionEstimator getResourceConsumptionEstimator() {
		return OperatorResourceConsumptionHandler.getResourceConsumptionEstimator(getInputPort(), SamplingOperator.class,
				null);
	}
}
