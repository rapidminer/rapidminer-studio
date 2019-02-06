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

import java.util.List;

import com.rapidminer.example.ExampleSet;
import com.rapidminer.example.set.SplittedExampleSet;
import com.rapidminer.operator.Operator;
import com.rapidminer.operator.OperatorDescription;
import com.rapidminer.operator.OperatorException;
import com.rapidminer.operator.ProcessSetupError.Severity;
import com.rapidminer.operator.SimpleProcessSetupError;
import com.rapidminer.operator.UserError;
import com.rapidminer.operator.annotation.ResourceConsumptionEstimator;
import com.rapidminer.operator.ports.InputPort;
import com.rapidminer.operator.ports.OutputPort;
import com.rapidminer.operator.ports.OutputPortExtender;
import com.rapidminer.operator.ports.metadata.AttributeMetaData;
import com.rapidminer.operator.ports.metadata.ExampleSetMetaData;
import com.rapidminer.operator.ports.metadata.MetaData;
import com.rapidminer.operator.ports.metadata.OneToManyPassThroughRule;
import com.rapidminer.parameter.ParameterType;
import com.rapidminer.parameter.ParameterTypeCategory;
import com.rapidminer.parameter.ParameterTypeDouble;
import com.rapidminer.parameter.ParameterTypeEnumeration;
import com.rapidminer.parameter.UndefinedParameterError;
import com.rapidminer.tools.OperatorResourceConsumptionHandler;
import com.rapidminer.tools.RandomGenerator;


/**
 * Divides a data set into the defined partitions and deliver the subsets.
 *
 * @author Tobias Malbrecht
 */
public class PartitionOperator extends Operator {

	public static final String PARAMETER_PARTITIONS = "partitions";

	public static final String PARAMETER_RATIO = "ratio";

	public static final String PARAMETER_SAMPLING_TYPE = "sampling_type";

	private InputPort exampleSetInput = getInputPorts().createPort("example set", ExampleSet.class);
	private OutputPortExtender outExtender = new OutputPortExtender("partition", getOutputPorts());

	public PartitionOperator(OperatorDescription description) {
		super(description);
		outExtender.start();
		getTransformer().addRule(new OneToManyPassThroughRule(exampleSetInput, outExtender.getManagedPorts()) {

			@Override
			public void transformMD() {
				super.transformMD();
				// check sanity of parameters
				try {
					String[] ratioList = ParameterTypeEnumeration
							.transformString2Enumeration(getParameterAsString(PARAMETER_PARTITIONS));
					double[] ratios = new double[ratioList.length];
					int i = 0;
					double sum = 0;
					for (String entry : ratioList) {
						ratios[i] = Double.valueOf(entry);
						sum += ratios[i];
						i++;
					}
					if (sum != 1d) {
						addError(new SimpleProcessSetupError(Severity.WARNING, getPortOwner(),
								"parameter_enumeration_forbidden_sum", PARAMETER_PARTITIONS, "1"));
					}
				} catch (UndefinedParameterError e) {
				} catch (NumberFormatException e) {
				}

			}

			@Override
			public MetaData modifyMetaData(MetaData unmodifiedMetaData, int outputIndex) {
				if (unmodifiedMetaData instanceof ExampleSetMetaData) {
					try {
						unmodifiedMetaData = modifiyMetaData((ExampleSetMetaData) unmodifiedMetaData, outputIndex);
					} catch (UndefinedParameterError e) {
					}
				}
				return unmodifiedMetaData;
			}
		});
	}

	protected ExampleSetMetaData modifiyMetaData(ExampleSetMetaData metaData, int outputIndex)
			throws UndefinedParameterError {
		if (metaData.getNumberOfExamples().isKnown()) {
			String[] ratioList = ParameterTypeEnumeration
					.transformString2Enumeration(getParameterAsString(PARAMETER_PARTITIONS));
			double[] ratios = new double[ratioList.length];
			if (outputIndex < ratios.length) {
				int i = 0;
				double sum = 0;
				for (String entry : ratioList) {
					ratios[i] = Double.valueOf(entry);
					sum += ratios[i];
					i++;
				}
				metaData.setNumberOfExamples((int) (ratios[outputIndex] / sum * metaData.getNumberOfExamples().getValue()));
				for (AttributeMetaData amd : metaData.getAllAttributes()) {
					amd.getNumberOfMissingValues().reduceByUnknownAmount();
				}

			} else {
				return null;
			}
		}
		return metaData;
	}

	@Override
	public void doWork() throws OperatorException {
		String[] ratioList = ParameterTypeEnumeration
				.transformString2Enumeration(getParameterAsString(PARAMETER_PARTITIONS));

		if (ratioList.length == 0) {
			throw new UserError(this, 217, PARAMETER_PARTITIONS, getName(), "");
		}

		double[] ratios = new double[ratioList.length];
		int i = 0;
		double sum = 0;
		for (String entry : ratioList) {
			try {
				ratios[i] = Double.valueOf(entry);
			} catch (NumberFormatException e) {
				throw new UserError(this, 211, PARAMETER_PARTITIONS, entry);
			}
			sum += ratios[i];
			i++;
		}
		for (int j = 0; j < ratios.length; j++) {
			ratios[j] /= sum;
		}
		ExampleSet originalSet = exampleSetInput.getData(ExampleSet.class);
		SplittedExampleSet e = new SplittedExampleSet(originalSet, ratios, getParameterAsInt(PARAMETER_SAMPLING_TYPE),
				getParameterAsBoolean(RandomGenerator.PARAMETER_USE_LOCAL_RANDOM_SEED),
				getParameterAsInt(RandomGenerator.PARAMETER_LOCAL_RANDOM_SEED));
		List<OutputPort> outputs = outExtender.getManagedPorts();
		for (int j = 0; j < ratioList.length; j++) {
			SplittedExampleSet b = new SplittedExampleSet(e);
			b.selectSingleSubset(j);
			if (outputs.size() > j) {
				outputs.get(j).deliver(b);
			}
		}
	}

	@Override
	public List<ParameterType> getParameterTypes() {
		List<ParameterType> types = super.getParameterTypes();
		ParameterTypeEnumeration type = new ParameterTypeEnumeration(PARAMETER_PARTITIONS, "The partitions that should be created.",
				new ParameterTypeDouble(PARAMETER_RATIO, "The relative size of this partition.", 0, 1), false);
		type.setPrimary(true);
		types.add(type);
		types.add(new ParameterTypeCategory(PARAMETER_SAMPLING_TYPE, "Defines the sampling type of this operator.",
				SplittedExampleSet.SAMPLING_NAMES, SplittedExampleSet.AUTOMATIC, false));
		types.addAll(RandomGenerator.getRandomGeneratorParameters(this));
		return types;
	}

	@Override
	public ResourceConsumptionEstimator getResourceConsumptionEstimator() {
		return OperatorResourceConsumptionHandler.getResourceConsumptionEstimator(getInputPorts().getPortByIndex(0),
				PartitionOperator.class, null);
	}
}
