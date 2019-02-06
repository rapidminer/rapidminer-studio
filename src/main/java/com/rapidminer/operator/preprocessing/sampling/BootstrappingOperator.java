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

import org.apache.commons.lang.ArrayUtils;

import com.rapidminer.example.ExampleSet;
import com.rapidminer.example.Tools;
import com.rapidminer.example.set.MappedExampleSet;
import com.rapidminer.example.table.DataRowFactory;
import com.rapidminer.operator.OperatorDescription;
import com.rapidminer.operator.OperatorException;
import com.rapidminer.operator.OperatorVersion;
import com.rapidminer.operator.annotation.ResourceConsumptionEstimator;
import com.rapidminer.operator.ports.metadata.ExampleSetMetaData;
import com.rapidminer.operator.ports.metadata.MDInteger;
import com.rapidminer.operator.preprocessing.MaterializeDataInMemory;
import com.rapidminer.operator.validation.IteratingPerformanceAverage;
import com.rapidminer.parameter.ParameterType;
import com.rapidminer.parameter.ParameterTypeBoolean;
import com.rapidminer.parameter.ParameterTypeCategory;
import com.rapidminer.parameter.ParameterTypeDouble;
import com.rapidminer.parameter.ParameterTypeInt;
import com.rapidminer.parameter.UndefinedParameterError;
import com.rapidminer.parameter.conditions.EqualTypeCondition;
import com.rapidminer.tools.OperatorResourceConsumptionHandler;
import com.rapidminer.tools.RandomGenerator;


/**
 * This operator constructs a bootstrapped sample from the given example set. That means that a
 * sampling with replacement will be performed. The usual sample size is the number of original
 * examples. This operator also offers the possibility to create the inverse example set, i.e. an
 * example set containing all examples which are not part of the bootstrapped example set. This
 * inverse example set might be used for a bootstrapped validation (together with an
 * {@link IteratingPerformanceAverage} operator.
 *
 * @author Ingo Mierswa, Tobias Malbrecht
 */
public class BootstrappingOperator extends AbstractSamplingOperator {

	public static final String PARAMETER_SAMPLE = "sample";

	public static final String[] SAMPLE_MODES = { "absolute", "relative" };

	public static final int SAMPLE_ABSOLUTE = 0;

	public static final int SAMPLE_RELATIVE = 1;

	/** The parameter name for &quot;The fraction of examples which should be sampled&quot; */
	public static final String PARAMETER_SAMPLE_SIZE = "sample_size";

	/** The parameter name for &quot;This ratio determines the size of the new example set.&quot; */
	public static final String PARAMETER_SAMPLE_RATIO = "sample_ratio";

	public static final String PARAMETER_USE_WEIGHTS = "use_weights";

	private static final OperatorVersion VERSION_6_4_0 = new OperatorVersion(6, 4, 0);

	public BootstrappingOperator(OperatorDescription description) {
		super(description);
	}

	@Override
	protected MDInteger getSampledSize(ExampleSetMetaData emd) throws UndefinedParameterError {
		switch (getParameterAsInt(PARAMETER_SAMPLE)) {
			case SAMPLE_ABSOLUTE:
				return new MDInteger(getParameterAsInt(PARAMETER_SAMPLE_SIZE));
			case SAMPLE_RELATIVE:
				MDInteger number = emd.getNumberOfExamples();
				number.multiply(getParameterAsDouble(PARAMETER_SAMPLE_RATIO));
				return number;
			default:
				return new MDInteger();
		}
	}

	@Override
	public ExampleSet apply(ExampleSet exampleSet) throws OperatorException {
		// cannot bootstrap without any examples
		Tools.isNonEmpty(exampleSet);
		int size = exampleSet.size();

		RandomGenerator random = RandomGenerator.getRandomGenerator(this);
		switch (getParameterAsInt(PARAMETER_SAMPLE)) {
			case SAMPLE_ABSOLUTE:
				size = getParameterAsInt(PARAMETER_SAMPLE_SIZE);
				break;
			case SAMPLE_RELATIVE:
				size = (int) Math.round(exampleSet.size() * getParameterAsDouble(PARAMETER_SAMPLE_RATIO));
				break;
		}

		int[] mapping = null;
		if (getParameterAsBoolean(PARAMETER_USE_WEIGHTS) && exampleSet.getAttributes().getWeight() != null) {
			mapping = MappedExampleSet.createWeightedBootstrappingMapping(exampleSet, size, random);
		} else {
			mapping = MappedExampleSet.createBootstrappingMapping(exampleSet, size, random);
		}

		// create and materialize example set
		ExampleSet mappedExampleSet = new MappedExampleSet(exampleSet, mapping, true);
		if (getCompatibilityLevel().isAbove(VERSION_6_4_0)) {
			int type = DataRowFactory.TYPE_DOUBLE_ARRAY;
			if (exampleSet.size() > 0) {
				type = exampleSet.getExampleTable().getDataRow(0).getType();
			}
			mappedExampleSet = MaterializeDataInMemory.materializeExampleSet(mappedExampleSet, type);
		}
		return mappedExampleSet;
	}

	@Override
	public List<ParameterType> getParameterTypes() {
		List<ParameterType> types = super.getParameterTypes();
		ParameterType type = new ParameterTypeCategory(PARAMETER_SAMPLE, "Determines how the amount of data is specified.",
				SAMPLE_MODES, SAMPLE_RELATIVE);
		type.setExpert(false);
		types.add(type);
		type = new ParameterTypeInt(PARAMETER_SAMPLE_SIZE, "The number of examples which should be sampled", 1,
				Integer.MAX_VALUE, 100);
		type.registerDependencyCondition(new EqualTypeCondition(this, PARAMETER_SAMPLE, SAMPLE_MODES, true, SAMPLE_ABSOLUTE));
		type.setExpert(false);
		types.add(type);
		type = new ParameterTypeDouble(PARAMETER_SAMPLE_RATIO, "This ratio determines the size of the new example set.",
				0.0d, Double.POSITIVE_INFINITY, 1.0d);
		type.registerDependencyCondition(new EqualTypeCondition(this, PARAMETER_SAMPLE, SAMPLE_MODES, true, SAMPLE_RELATIVE));
		type.setExpert(false);
		types.add(type);
		type = new ParameterTypeBoolean(PARAMETER_USE_WEIGHTS,
				"If checked, example weights will be considered during the bootstrapping if such weights are present.", true);
		type.setExpert(false);
		types.add(type);
		types.addAll(RandomGenerator.getRandomGeneratorParameters(this));
		return types;
	}

	@Override
	public OperatorVersion[] getIncompatibleVersionChanges() {
		return (OperatorVersion[]) ArrayUtils.addAll(super.getIncompatibleVersionChanges(),
				new OperatorVersion[] { VERSION_6_4_0 });
	}

	@Override
	public ResourceConsumptionEstimator getResourceConsumptionEstimator() {
		return OperatorResourceConsumptionHandler.getResourceConsumptionEstimator(getInputPort(),
				BootstrappingOperator.class, null);
	}
}
