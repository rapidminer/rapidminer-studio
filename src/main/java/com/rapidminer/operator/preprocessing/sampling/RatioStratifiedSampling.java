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
import com.rapidminer.operator.ports.metadata.ExampleSetMetaData;
import com.rapidminer.operator.ports.metadata.MDInteger;
import com.rapidminer.parameter.ParameterType;
import com.rapidminer.parameter.ParameterTypeDouble;
import com.rapidminer.parameter.UndefinedParameterError;

import java.util.LinkedList;
import java.util.List;


/**
 * Stratified sampling operator. This operator performs a random sampling of a given fraction. In
 * contrast to the simple sampling operator, this operator performs a stratified sampling for data
 * sets with nominal label attributes, i.e. the class distributions remains (almost) the same after
 * sampling. Hence, this operator cannot be applied on data sets without a label or with a numerical
 * label. In these cases a simple sampling without stratification is performed.
 * 
 * @author Ingo Mierswa
 */
@SuppressWarnings("deprecation")
public class RatioStratifiedSampling extends AbstractStratifiedSampling {

	/** The parameter name for &quot;The fraction of examples which should be sampled&quot; */
	public static final String PARAMETER_SAMPLE_RATIO = "sample_ratio";

	public RatioStratifiedSampling(OperatorDescription description) {
		super(description);
	}

	@Override
	protected MDInteger getSampledSize(ExampleSetMetaData emd) throws UndefinedParameterError {
		MDInteger number = emd.getNumberOfExamples();
		number.multiply(getParameterAsDouble(PARAMETER_SAMPLE_RATIO));
		return number;
	}

	@Override
	public double getRatio(ExampleSet exampleSet) throws OperatorException {
		return getParameterAsDouble(PARAMETER_SAMPLE_RATIO);
	}

	@Override
	public List<ParameterType> getParameterTypes() {
		List<ParameterType> types = new LinkedList<ParameterType>();
		ParameterType type = new ParameterTypeDouble(PARAMETER_SAMPLE_RATIO,
				"The fraction of examples which should be sampled", 0.0d, 1.0d, 0.1d);
		type.setExpert(false);
		types.add(type);

		types.addAll(super.getParameterTypes());
		return types;
	}

}
