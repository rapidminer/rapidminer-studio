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
import com.rapidminer.operator.OperatorDescription;
import com.rapidminer.operator.OperatorException;
import com.rapidminer.operator.ProcessSetupError.Severity;
import com.rapidminer.operator.ports.metadata.ExampleSetMetaData;
import com.rapidminer.operator.ports.metadata.MDInteger;
import com.rapidminer.operator.ports.metadata.MetaDataInfo;
import com.rapidminer.operator.ports.metadata.SimpleMetaDataError;
import com.rapidminer.operator.ports.quickfix.ParameterSettingQuickFix;
import com.rapidminer.parameter.ParameterType;
import com.rapidminer.parameter.ParameterTypeInt;
import com.rapidminer.parameter.UndefinedParameterError;
import com.rapidminer.tools.RandomGenerator;

import java.util.Collections;
import java.util.List;


/**
 * Absolute sampling operator. This operator takes a random sample with the given size. For example,
 * if the sample size is set to 50, the result will have exactly 50 examples randomly drawn from the
 * complete data set. Please note that this operator does not sample during a data scan but jumps to
 * the rows. It should therefore only be used in case of memory data management and not, for
 * example, for database or file management.
 * 
 * @author Ingo Mierswa
 */
public class AbsoluteSampling extends AbstractSamplingOperator {

	/** The parameter name for &quot;The number of examples which should be sampled&quot; */
	public static final String PARAMETER_SAMPLE_SIZE = "sample_size";

	public AbsoluteSampling(OperatorDescription description) {
		super(description);
	}

	@Override
	protected MDInteger getSampledSize(ExampleSetMetaData emd) throws UndefinedParameterError {
		int absoluteNumber = getParameterAsInt(PARAMETER_SAMPLE_SIZE);

		if (emd.getNumberOfExamples().isAtLeast(absoluteNumber) == MetaDataInfo.NO) {
			getExampleSetInputPort().addError(
					new SimpleMetaDataError(Severity.ERROR, getExampleSetInputPort(), Collections
							.singletonList(new ParameterSettingQuickFix(this, PARAMETER_SAMPLE_SIZE, emd
									.getNumberOfExamples().getValue().toString())), "exampleset.need_more_examples",
							absoluteNumber + ""));
		}
		return new MDInteger(absoluteNumber);
	}

	@Override
	public ExampleSet apply(ExampleSet exampleSet) throws OperatorException {
		return exampleSet;
	}

	@Override
	public List<ParameterType> getParameterTypes() {
		List<ParameterType> types = super.getParameterTypes();
		ParameterType type = new ParameterTypeInt(PARAMETER_SAMPLE_SIZE, "The number of examples which should be sampled",
				1, Integer.MAX_VALUE, 100);
		type.setExpert(false);
		types.add(type);

		types.addAll(RandomGenerator.getRandomGeneratorParameters(this));

		return types;
	}
}
