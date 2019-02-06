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
import com.rapidminer.operator.ProcessSetupError.Severity;
import com.rapidminer.operator.ports.metadata.MDInteger;
import com.rapidminer.operator.ports.metadata.MetaDataInfo;
import com.rapidminer.operator.ports.metadata.SimpleMetaDataError;
import com.rapidminer.operator.ports.quickfix.ParameterSettingQuickFix;
import com.rapidminer.parameter.ParameterType;
import com.rapidminer.parameter.ParameterTypeBoolean;
import com.rapidminer.parameter.ParameterTypeCategory;
import com.rapidminer.parameter.ParameterTypeInt;
import com.rapidminer.parameter.UndefinedParameterError;
import com.rapidminer.parameter.conditions.BooleanParameterCondition;
import com.rapidminer.tools.RandomGenerator;

import java.util.Collections;
import java.util.List;


/**
 * <p>
 * An operator chain that split an {@link ExampleSet} into two disjunct parts and applies the first
 * child operator on the first part and applies the second child on the second part and the result
 * of the first child. The total result is the result of the second operator.
 * </p>
 * 
 * <p>
 * The input example set will be splitted based on a user defined absolute numbers.
 * </p>
 * 
 * @author Peter B. Volk, Ingo Mierswa
 */
public class AbsoluteSplitChain extends AbstractSplitChain {

	/** The parameter name for &quot;Defines the sampling type of this operator.&quot; */
	public static final String PARAMETER_SAMPLING_TYPE = "sampling_type";

	private static final String PARAMETER_NUMBER_TRAINING_EXAMPLES = "first_set_size";

	private static final String PARAMETER_NUMBER_TEST_EXAMPLES = "second_set_size";

	private static final String PARAMETER_RESTRICT_FIRST = "restrict_first";

	public AbsoluteSplitChain(OperatorDescription description) {
		super(description);
	}

	@Override
	protected MDInteger getNumberOfExamplesFirst(MDInteger numberOfExamples) throws UndefinedParameterError {
		int desiredSize;
		if (getParameterAsBoolean(PARAMETER_RESTRICT_FIRST)) {
			desiredSize = getParameterAsInt(PARAMETER_NUMBER_TRAINING_EXAMPLES);
		} else {
			desiredSize = getParameterAsInt(PARAMETER_NUMBER_TEST_EXAMPLES);
		}
		if (numberOfExamples.isAtLeast(desiredSize) == MetaDataInfo.NO) {
			if (getParameterAsBoolean(PARAMETER_RESTRICT_FIRST)) {
				getExampleSetInputPort().addError(
						new SimpleMetaDataError(Severity.ERROR, getExampleSetInputPort(), Collections
								.singletonList(new ParameterSettingQuickFix(this, PARAMETER_NUMBER_TRAINING_EXAMPLES,
										numberOfExamples.getNumber().toString())), "exampleset.need_more_examples",
								desiredSize + ""));
			} else {
				getExampleSetInputPort().addError(
						new SimpleMetaDataError(Severity.ERROR, getExampleSetInputPort(), Collections
								.singletonList(new ParameterSettingQuickFix(this, PARAMETER_NUMBER_TEST_EXAMPLES,
										numberOfExamples.getNumber().toString())), "exampleset.need_more_examples",
								desiredSize + ""));
			}
		}
		if (getParameterAsBoolean(PARAMETER_RESTRICT_FIRST)) {
			return new MDInteger(desiredSize);
		} else {
			return numberOfExamples.subtract(desiredSize);
		}
	}

	@Override
	protected MDInteger getNumberOfExamplesSecond(MDInteger numberOfExamples) throws UndefinedParameterError {
		if (getParameterAsBoolean(PARAMETER_RESTRICT_FIRST)) {
			return numberOfExamples.subtract(getParameterAsInt(PARAMETER_NUMBER_TRAINING_EXAMPLES));
		} else {
			return new MDInteger(getParameterAsInt(PARAMETER_NUMBER_TEST_EXAMPLES));
		}

		// checking already performed in method above
	}

	@Override
	protected SplittedExampleSet createSplittedExampleSet(ExampleSet inputSet) throws OperatorException {
		int size = -1;
		if (getParameterAsBoolean(PARAMETER_RESTRICT_FIRST)) {
			size = getParameterAsInt(PARAMETER_NUMBER_TRAINING_EXAMPLES);
		} else {
			size = inputSet.size() - getParameterAsInt(PARAMETER_NUMBER_TEST_EXAMPLES);
		}

		return new SplittedExampleSet(inputSet, (double) size / (double) (inputSet.size()),
				getParameterAsInt(PARAMETER_SAMPLING_TYPE),
				getParameterAsBoolean(RandomGenerator.PARAMETER_USE_LOCAL_RANDOM_SEED),
				getParameterAsInt(RandomGenerator.PARAMETER_LOCAL_RANDOM_SEED));
	}

	@Override
	public List<ParameterType> getParameterTypes() {
		List<ParameterType> types = super.getParameterTypes();
		types.add(new ParameterTypeBoolean(
				PARAMETER_RESTRICT_FIRST,
				"If checked, the size if the first set is fixed. Otherwise the size of the second part might be specified. However, the not fixed part will reciev all remaining examples.",
				true, false));
		ParameterType type = new ParameterTypeInt(PARAMETER_NUMBER_TRAINING_EXAMPLES,
				"Absolute size of the training set. -1 equal to not defined", 1, Integer.MAX_VALUE, 1);
		type.registerDependencyCondition(new BooleanParameterCondition(this, PARAMETER_RESTRICT_FIRST, true, true));
		type.setExpert(false);
		types.add(type);
		type = new ParameterTypeInt(PARAMETER_NUMBER_TEST_EXAMPLES,
				"Absolute size of the test set. -1 equal to not defined", 1, Integer.MAX_VALUE, 1);
		type.registerDependencyCondition(new BooleanParameterCondition(this, PARAMETER_RESTRICT_FIRST, true, false));
		type.setExpert(false);
		types.add(type);
		types.add(new ParameterTypeCategory(PARAMETER_SAMPLING_TYPE, "Defines the sampling type of this operator.",
				SplittedExampleSet.SAMPLING_NAMES, SplittedExampleSet.SHUFFLED_SAMPLING));

		types.addAll(RandomGenerator.getRandomGeneratorParameters(this));

		return types;
	}
}
