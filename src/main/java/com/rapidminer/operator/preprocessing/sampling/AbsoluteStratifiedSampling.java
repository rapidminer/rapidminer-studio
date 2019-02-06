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

import java.util.Collections;
import java.util.LinkedList;
import java.util.List;


/**
 * Stratified sampling operator. This operator performs a random sampling of a given size. In
 * contrast to the simple sampling operator, this operator performs a stratified sampling for data
 * sets with nominal label attributes, i.e. the class distributions remains (almost) the same after
 * sampling. Hence, this operator cannot be applied on data sets without a label or with a numerical
 * label. In these cases a simple sampling without stratification is performed. In some cases it
 * might happen that not the exact desired number of examples is sampled, e.g. if the desired number
 * is 100 from three qually distributed classes the resulting number will be 99 (33 of each class).
 * 
 * @author Sebastian Land
 */
@SuppressWarnings("deprecation")
public class AbsoluteStratifiedSampling extends AbstractStratifiedSampling {

	/** The parameter name for &quot;The fraction of examples which should be sampled&quot; */
	public static final String PARAMETER_SAMPLE_SIZE = "sample_size";

	public AbsoluteStratifiedSampling(OperatorDescription description) {
		super(description);
	}

	@Override
	protected MDInteger getSampledSize(ExampleSetMetaData emd) throws UndefinedParameterError {
		int absoluteNumber = getParameterAsInt(PARAMETER_SAMPLE_SIZE);

		if (emd.getNumberOfExamples().isAtLeast(absoluteNumber) == MetaDataInfo.NO) {
			getExampleSetInputPort().addError(
					new SimpleMetaDataError(Severity.ERROR, getExampleSetInputPort(), Collections
							.singletonList(new ParameterSettingQuickFix(this, PARAMETER_SAMPLE_SIZE, emd
									.getNumberOfExamples().getValue().toString())), "need_more_examples", absoluteNumber
							+ ""));
		}
		return new MDInteger(absoluteNumber);
	}

	@Override
	public double getRatio(ExampleSet exampleSet) throws OperatorException {
		double targetSize = getParameterAsInt(PARAMETER_SAMPLE_SIZE);
		if (targetSize > exampleSet.size()) {
			return 1d;
		} else {
			return targetSize / (exampleSet.size());
		}
	}

	@Override
	public List<ParameterType> getParameterTypes() {
		List<ParameterType> types = new LinkedList<ParameterType>();
		ParameterType type = new ParameterTypeInt(PARAMETER_SAMPLE_SIZE, "The number of examples which should be sampled",
				1, Integer.MAX_VALUE, 100);
		type.setExpert(false);
		types.add(type);
		types.addAll(super.getParameterTypes());
		return types;
	}
}
