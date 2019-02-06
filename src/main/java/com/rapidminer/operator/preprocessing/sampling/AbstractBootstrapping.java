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

import com.rapidminer.example.ExampleSet;
import com.rapidminer.example.set.MappedExampleSet;
import com.rapidminer.operator.OperatorDescription;
import com.rapidminer.operator.OperatorException;
import com.rapidminer.operator.ports.metadata.ExampleSetMetaData;
import com.rapidminer.operator.ports.metadata.MDInteger;
import com.rapidminer.operator.validation.IteratingPerformanceAverage;
import com.rapidminer.parameter.ParameterType;
import com.rapidminer.parameter.ParameterTypeDouble;
import com.rapidminer.parameter.UndefinedParameterError;
import com.rapidminer.tools.RandomGenerator;

import java.util.List;
import java.util.Random;


/**
 * This operator constructs a bootstrapped sample from the given example set. That means that a
 * sampling with replacement will be performed. The usual sample size is the number of original
 * examples. This operator also offers the possibility to create the inverse example set, i.e. an
 * example set containing all examples which are not part of the bootstrapped example set. This
 * inverse example set might be used for a bootstrapped validation (together with an
 * {@link IteratingPerformanceAverage} operator.
 * 
 * @author Ingo Mierswa
 */
public abstract class AbstractBootstrapping extends AbstractSamplingOperator {

	/** The parameter name for &quot;This ratio determines the size of the new example set.&quot; */
	public static final String PARAMETER_SAMPLE_RATIO = "sample_ratio";

	public AbstractBootstrapping(OperatorDescription description) {
		super(description);
	}

	@Override
	protected MDInteger getSampledSize(ExampleSetMetaData emd) throws UndefinedParameterError {
		if (emd.getNumberOfExamples().isKnown()) {
			return new MDInteger((int) getParameterAsDouble(PARAMETER_SAMPLE_RATIO) * emd.getNumberOfExamples().getValue());
		}
		return new MDInteger();
	}

	public abstract int[] createMapping(ExampleSet exampleSet, int size, Random random) throws OperatorException;

	@Override
	public ExampleSet apply(ExampleSet exampleSet) throws OperatorException {
		RandomGenerator random = RandomGenerator.getRandomGenerator(this);
		int[] mapping = createMapping(exampleSet,
				(int) Math.round(exampleSet.size() * getParameterAsDouble(PARAMETER_SAMPLE_RATIO)), random);
		MappedExampleSet bootstrappedExampleSet = new MappedExampleSet(exampleSet, mapping, true);
		return bootstrappedExampleSet;
	}

	@Override
	public List<ParameterType> getParameterTypes() {
		List<ParameterType> types = super.getParameterTypes();
		ParameterType type = new ParameterTypeDouble(PARAMETER_SAMPLE_RATIO,
				"This ratio determines the size of the new example set.", 0.0d, Double.POSITIVE_INFINITY, 1.0d);
		type.setExpert(false);
		types.add(type);

		types.addAll(RandomGenerator.getRandomGeneratorParameters(this));

		return types;
	}
}
