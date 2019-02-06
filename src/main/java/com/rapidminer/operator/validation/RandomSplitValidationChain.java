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

import com.rapidminer.example.ExampleSet;
import com.rapidminer.example.set.SplittedExampleSet;
import com.rapidminer.operator.OperatorCapability;
import com.rapidminer.operator.OperatorDescription;
import com.rapidminer.operator.OperatorException;
import com.rapidminer.operator.OperatorVersion;
import com.rapidminer.operator.ports.metadata.AttributeMetaData;
import com.rapidminer.operator.ports.metadata.CapabilityPrecondition;
import com.rapidminer.operator.ports.metadata.MDInteger;
import com.rapidminer.operator.ports.metadata.Precondition;
import com.rapidminer.operator.ports.quickfix.ParameterSettingQuickFix;
import com.rapidminer.operator.ports.quickfix.QuickFix;
import com.rapidminer.operator.visualization.ProcessLogOperator;
import com.rapidminer.parameter.ParameterType;
import com.rapidminer.parameter.ParameterTypeCategory;
import com.rapidminer.parameter.ParameterTypeDouble;
import com.rapidminer.parameter.UndefinedParameterError;
import com.rapidminer.tools.RandomGenerator;

import java.util.List;


/**
 * <p>
 * A <code>RandomSplitValidationChain</code> splits up the example set into a training and test set
 * and evaluates the model. The first inner operator must accept an
 * {@link com.rapidminer.example.ExampleSet} while the second must accept an
 * {@link com.rapidminer.example.ExampleSet} and the output of the first (which is in most cases a
 * {@link com.rapidminer.operator.Model}) and must produce a
 * {@link com.rapidminer.operator.performance.PerformanceVector}.
 * </p>
 * 
 * <p>
 * This validation operator provides several values which can be logged by means of a
 * {@link ProcessLogOperator}. All performance estimation operators of RapidMiner provide access to
 * the average values calculated during the estimation. Since the operator cannot ensure the names
 * of the delivered criteria, the ProcessLog operator can access the values via the generic value
 * names:
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
 * @author Simon Fischer, Ingo Mierswa
 */
public class RandomSplitValidationChain extends ValidationChain {

	public static final String PARAMETER_SPLIT_RATIO = "split_ratio";

	public static final String PARAMETER_SAMPLING_TYPE = "sampling_type";

	public RandomSplitValidationChain(OperatorDescription description) {
		super(description);
	}

	@Override
	protected Precondition getCapabilityPrecondition() {
		return new CapabilityPrecondition(this, trainingSetInput) {

			@Override
			protected List<QuickFix> getFixesForRegressionWhenClassificationSupported(AttributeMetaData labelMD) {
				List<QuickFix> fixes = super.getFixesForRegressionWhenClassificationSupported(labelMD);
				fixes.add(0, new ParameterSettingQuickFix(RandomSplitValidationChain.this, PARAMETER_SAMPLING_TYPE,
						SplittedExampleSet.SHUFFLED_SAMPLING + "", "switch_to_shuffled_sampling"));
				return fixes;
			}
		};
	}

	@Override
	public void estimatePerformance(ExampleSet inputSet) throws OperatorException {
		double splitRatio = getParameterAsDouble(PARAMETER_SPLIT_RATIO);
		SplittedExampleSet eSet = new SplittedExampleSet(inputSet, splitRatio, getParameterAsInt(PARAMETER_SAMPLING_TYPE),
				getParameterAsBoolean(RandomGenerator.PARAMETER_USE_LOCAL_RANDOM_SEED),
				getParameterAsInt(RandomGenerator.PARAMETER_LOCAL_RANDOM_SEED), getCompatibilityLevel().isAtMost(
						SplittedExampleSet.VERSION_SAMPLING_CHANGED));

		eSet.selectSingleSubset(0);
		learn(eSet);
		eSet.selectSingleSubset(1);
		// IOContainer evalRes =
		evaluate(eSet);
	}

	@Override
	public List<ParameterType> getParameterTypes() {
		List<ParameterType> types = super.getParameterTypes();
		ParameterType type = new ParameterTypeDouble(PARAMETER_SPLIT_RATIO, "Relative size of the training set", 0.0d, 1.0d,
				0.7d);
		type.setExpert(false);
		types.add(type);
		types.add(new ParameterTypeCategory(
				PARAMETER_SAMPLING_TYPE,
				"Defines the sampling type of the cross validation (linear = consecutive subsets, shuffled = random subsets, stratified = random subsets with class distribution kept constant)",
				SplittedExampleSet.SAMPLING_NAMES, SplittedExampleSet.STRATIFIED_SAMPLING));
		types.addAll(RandomGenerator.getRandomGeneratorParameters(this));
		return types;
	}

	@Override
	protected MDInteger getTestSetSize(MDInteger originalSize) throws UndefinedParameterError {
		return originalSize.multiply(1d - getParameterAsDouble(PARAMETER_SPLIT_RATIO));
	}

	@Override
	protected MDInteger getTrainingSetSize(MDInteger originalSize) throws UndefinedParameterError {
		return originalSize.multiply(getParameterAsDouble(PARAMETER_SPLIT_RATIO));
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

	@Override
	public OperatorVersion[] getIncompatibleVersionChanges() {
		return new OperatorVersion[] { SplittedExampleSet.VERSION_SAMPLING_CHANGED };
	}
}
