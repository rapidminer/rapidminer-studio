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
package com.rapidminer.operator.validation;

import java.util.List;

import com.rapidminer.example.ExampleSet;
import com.rapidminer.example.set.SplittedExampleSet;
import com.rapidminer.operator.OperatorCapability;
import com.rapidminer.operator.OperatorDescription;
import com.rapidminer.operator.OperatorException;
import com.rapidminer.operator.OperatorVersion;
import com.rapidminer.operator.ProcessStoppedException;
import com.rapidminer.operator.ValueDouble;
import com.rapidminer.operator.ports.metadata.AttributeMetaData;
import com.rapidminer.operator.ports.metadata.CapabilityPrecondition;
import com.rapidminer.operator.ports.metadata.MDInteger;
import com.rapidminer.operator.ports.metadata.Precondition;
import com.rapidminer.operator.ports.quickfix.ParameterSettingQuickFix;
import com.rapidminer.operator.ports.quickfix.QuickFix;
import com.rapidminer.operator.visualization.ProcessLogOperator;
import com.rapidminer.parameter.ParameterType;
import com.rapidminer.parameter.ParameterTypeBoolean;
import com.rapidminer.parameter.ParameterTypeCategory;
import com.rapidminer.parameter.ParameterTypeInt;
import com.rapidminer.parameter.UndefinedParameterError;
import com.rapidminer.parameter.conditions.BooleanParameterCondition;
import com.rapidminer.parameter.conditions.EqualTypeCondition;
import com.rapidminer.tools.RandomGenerator;


/**
 * <p>
 * <code>XValidation</code> encapsulates a cross-validation process. The example set
 * {@rapidminer.math S} is split up into <var> number_of_validations</var> subsets
 * {@rapidminer.math S_i}. The inner operators are applied <var>number_of_validations</var> times
 * using {@rapidminer.math S_i} as the test set (input of the second inner operator) and
 * {@rapidminer.math S\backslash S_i} training set (input of the first inner operator).
 * </p>
 *
 * <p>
 * The first inner operator must accept an {@link com.rapidminer.example.ExampleSet} while the
 * second must accept an {@link com.rapidminer.example.ExampleSet} and the output of the first
 * (which is in most cases a {@link com.rapidminer.operator.Model}) and must produce a
 * {@link com.rapidminer.operator.performance.PerformanceVector}.
 * </p>
 *
 * <p>
 * Like other validation schemes the RapidMiner cross validation can use several types of sampling
 * for building the subsets. Linear sampling simply divides the example set into partitions without
 * changing the order of the examples. Shuffled sampling build random subsets from the data.
 * Stratifed sampling builds random subsets and ensures that the class distribution in the subsets
 * is the same as in the whole example set.
 * </p>
 *
 * <p>
 * The cross validation operator provides several values which can be logged by means of a
 * {@link ProcessLogOperator}. Of course the number of the current iteration can be logged which
 * might be useful for ProcessLog operators wrapped inside a cross validation. Beside that, all
 * performance estimation operators of RapidMiner provide access to the average values calculated
 * during the estimation. Since the operator cannot ensure the names of the delivered criteria, the
 * ProcessLog operator can access the values via the generic value names:
 * </p>
 * <ul>
 * <li>performance: the value for the main criterion calculated by this validation operator</li>
 * <li>performance1: the value of the first criterion of the performance vector calculated</li>
 * <li>performance2: the value of the second criterion of the performance vector calculated</li>
 * <li>performance3: the value of the third criterion of the performance vector calculated</li>
 * <li>for the main criterion, also the variance and the standard deviation can be accessed where
 * applicable.</li>
 * </ul>
 *
 * @rapidminer.index cross-validation
 * @author Ingo Mierswa
 * @deprecated use the {@link #CrossValidationOperator} from the concurrency extension instead.
 */
@Deprecated
public class XValidation extends ValidationChain {

	/** The parameter name for &quot;Number of subsets for the crossvalidation.&quot; */
	public static final String PARAMETER_NUMBER_OF_VALIDATIONS = "number_of_validations";

	/**
	 * The parameter name for &quot;Set the number of validations to the number of examples. If set
	 * to true, number_of_validations is ignored&quot;
	 */
	public static final String PARAMETER_LEAVE_ONE_OUT = "leave_one_out";

	/**
	 * The parameter name for &quot;Defines the sampling type of the cross validation (linear =
	 * consecutive subsets, shuffled = random subsets, stratified = random subsets with class
	 * distribution kept constant)&quot;
	 */
	public static final String PARAMETER_SAMPLING_TYPE = "sampling_type";

	/**
	 * The parameter name for &quot;Indicates if only performance vectors should be averaged or all
	 * types of averagable result vectors&quot;
	 */
	public static final String PARAMETER_AVERAGE_PERFORMANCES_ONLY = "average_performances_only";

	private int iteration;

	public XValidation(OperatorDescription description) {
		super(description);
		addValue(new ValueDouble("iteration", "The number of the current iteration.") {

			@Override
			public double getDoubleValue() {
				return iteration;
			}
		});
	}

	@Override
	protected Precondition getCapabilityPrecondition() {
		return new CapabilityPrecondition(this, trainingSetInput) {

			@Override
			protected List<QuickFix> getFixesForRegressionWhenClassificationSupported(AttributeMetaData labelMD) {
				List<QuickFix> fixes = super.getFixesForRegressionWhenClassificationSupported(labelMD);
				fixes.add(0, new ParameterSettingQuickFix(XValidation.this, PARAMETER_SAMPLING_TYPE,
						SplittedExampleSet.SHUFFLED_SAMPLING + "", "switch_to_shuffled_sampling"));
				return fixes;
			}
		};
	}

	@Override
	public void estimatePerformance(ExampleSet inputSet) throws OperatorException {
		int number;
		if (getParameterAsBoolean(PARAMETER_LEAVE_ONE_OUT)) {
			number = inputSet.size();
		} else {
			number = getParameterAsInt(PARAMETER_NUMBER_OF_VALIDATIONS);
		}
		getLogger().fine("Starting " + number + "-fold cross validation");

		// Split training / test set
		int samplingType = getParameterAsInt(PARAMETER_SAMPLING_TYPE);
		SplittedExampleSet splittedES = new SplittedExampleSet(inputSet, number, samplingType,
				getParameterAsBoolean(RandomGenerator.PARAMETER_USE_LOCAL_RANDOM_SEED),
				getParameterAsInt(RandomGenerator.PARAMETER_LOCAL_RANDOM_SEED),
				getCompatibilityLevel().isAtMost(SplittedExampleSet.VERSION_SAMPLING_CHANGED));

		// start crossvalidation
		if (modelOutput.isConnected()) {
			getProgress().setTotal(number + 1);
		} else {
			getProgress().setTotal(number);
		}
		getProgress().setCheckForStop(false);

		for (iteration = 0; iteration < number; iteration++) {
			performIteration(splittedES, iteration);
		}
	}

	protected void performIteration(SplittedExampleSet splittedES, int iteration)
			throws OperatorException, ProcessStoppedException {
		splittedES.selectAllSubsetsBut(iteration);
		learn(splittedES);

		splittedES.selectSingleSubset(iteration);
		evaluate(splittedES);

		inApplyLoop();
		getProgress().step();
	}

	@Override
	protected MDInteger getTestSetSize(MDInteger originalSize) throws UndefinedParameterError {
		if (getParameterAsBoolean(PARAMETER_LEAVE_ONE_OUT)) {
			return new MDInteger(1);
		} else {
			return originalSize.multiply(1d / getParameterAsDouble(PARAMETER_NUMBER_OF_VALIDATIONS));
		}
	}

	@Override
	protected MDInteger getTrainingSetSize(MDInteger originalSize) throws UndefinedParameterError {
		if (getParameterAsBoolean(PARAMETER_LEAVE_ONE_OUT)) {
			return originalSize.add(-1);
		} else {
			return originalSize.multiply(1d - 1d / getParameterAsDouble(PARAMETER_NUMBER_OF_VALIDATIONS));
		}
	}

	@Override
	public List<ParameterType> getParameterTypes() {
		List<ParameterType> types = super.getParameterTypes();
		types.add(new ParameterTypeBoolean(PARAMETER_AVERAGE_PERFORMANCES_ONLY,
				"Indicates if only performance vectors should be averaged or all types of averagable result vectors", true));
		types.add(new ParameterTypeBoolean(PARAMETER_LEAVE_ONE_OUT,
				"Set the number of validations to the number of examples. If set to true, number_of_validations is ignored",
				false, false));

		ParameterType type = new ParameterTypeInt(PARAMETER_NUMBER_OF_VALIDATIONS,
				"Number of subsets for the crossvalidation.", 2, Integer.MAX_VALUE, 10);
		type.registerDependencyCondition(new BooleanParameterCondition(this, PARAMETER_LEAVE_ONE_OUT, false, false));
		type.setExpert(false);
		types.add(type);

		type = new ParameterTypeCategory(PARAMETER_SAMPLING_TYPE,
				"Defines the sampling type of the cross validation (linear = consecutive subsets, shuffled = random subsets, stratified = random subsets with class distribution kept constant)",
				SplittedExampleSet.SAMPLING_NAMES, SplittedExampleSet.AUTOMATIC);
		type.setExpert(false);
		type.registerDependencyCondition(new BooleanParameterCondition(this, PARAMETER_LEAVE_ONE_OUT, false, false));
		types.add(type);

		for (ParameterType addType : RandomGenerator.getRandomGeneratorParameters(this)) {
			addType.registerDependencyCondition(new BooleanParameterCondition(this, PARAMETER_LEAVE_ONE_OUT, false, false));
			addType.registerDependencyCondition(new EqualTypeCondition(this, PARAMETER_SAMPLING_TYPE,
					SplittedExampleSet.SAMPLING_NAMES, false, SplittedExampleSet.SHUFFLED_SAMPLING,
					SplittedExampleSet.STRATIFIED_SAMPLING, SplittedExampleSet.AUTOMATIC));
			types.add(addType);
		}
		return types;
	}

	@Override
	public OperatorVersion[] getIncompatibleVersionChanges() {
		return new OperatorVersion[] { SplittedExampleSet.VERSION_SAMPLING_CHANGED };
	}

	@Override
	public boolean supportsCapability(OperatorCapability capability) {
		switch (capability) {
			case NO_LABEL:
				return false;
			case NUMERICAL_LABEL:
				try {
					return getParameterAsInt(PARAMETER_SAMPLING_TYPE) != SplittedExampleSet.STRATIFIED_SAMPLING;
				} catch (UndefinedParameterError e) {
					return false;
				}
			default:
				return true;
		}
	}
}
