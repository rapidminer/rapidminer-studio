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
package com.rapidminer.operator.meta;

import com.rapidminer.example.ExampleSet;
import com.rapidminer.example.set.SplittedExampleSet;
import com.rapidminer.operator.OperatorDescription;
import com.rapidminer.operator.OperatorException;
import com.rapidminer.operator.ports.metadata.MDInteger;
import com.rapidminer.parameter.ParameterType;
import com.rapidminer.parameter.ParameterTypeCategory;
import com.rapidminer.parameter.ParameterTypeDouble;
import com.rapidminer.parameter.UndefinedParameterError;
import com.rapidminer.tools.RandomGenerator;

import java.util.List;


/**
 * <p>
 * An operator chain that split an {@link ExampleSet} into two disjunct parts and applies the first
 * child operator on the first part and applies the second child on the second part and the result
 * of the first child. The total result is the result of the second operator.
 * </p>
 * 
 * <p>
 * The input example set will be splitted based on a defined ratio between 0 and 1.
 * </p>
 * 
 * @author Ingo Mierswa
 */
public class RatioSplitChain extends AbstractSplitChain {

	/** The parameter name for &quot;Relative size of the training set.&quot; */
	public static final String PARAMETER_SPLIT_RATIO = "split_ratio";

	/** The parameter name for &quot;Defines the sampling type of this operator.&quot; */
	public static final String PARAMETER_SAMPLING_TYPE = "sampling_type";

	public RatioSplitChain(OperatorDescription description) {
		super(description);
	}

	@Override
	protected MDInteger getNumberOfExamplesFirst(MDInteger numberOfExamples) throws UndefinedParameterError {
		return numberOfExamples.multiply(getParameterAsDouble(PARAMETER_SPLIT_RATIO));
	}

	@Override
	protected MDInteger getNumberOfExamplesSecond(MDInteger numberOfExamples) throws UndefinedParameterError {
		return numberOfExamples.multiply(1d - getParameterAsDouble(PARAMETER_SPLIT_RATIO));
	}

	@Override
	protected SplittedExampleSet createSplittedExampleSet(ExampleSet inputSet) throws OperatorException {
		return new SplittedExampleSet(inputSet, getParameterAsDouble(PARAMETER_SPLIT_RATIO),
				getParameterAsInt(PARAMETER_SAMPLING_TYPE),
				getParameterAsBoolean(RandomGenerator.PARAMETER_USE_LOCAL_RANDOM_SEED),
				getParameterAsInt(RandomGenerator.PARAMETER_LOCAL_RANDOM_SEED));
	}

	@Override
	public List<ParameterType> getParameterTypes() {
		List<ParameterType> types = super.getParameterTypes();
		ParameterType type = new ParameterTypeDouble(PARAMETER_SPLIT_RATIO,
				"Relative size of the first set. The remaining examples will be part of the second set.", 0.0d, 1.0d, 0.7d);
		type.setExpert(false);
		types.add(type);
		types.add(new ParameterTypeCategory(PARAMETER_SAMPLING_TYPE, "Defines the sampling type of this operator.",
				SplittedExampleSet.SAMPLING_NAMES, SplittedExampleSet.SHUFFLED_SAMPLING));
		types.addAll(RandomGenerator.getRandomGeneratorParameters(this));
		return types;
	}

}
