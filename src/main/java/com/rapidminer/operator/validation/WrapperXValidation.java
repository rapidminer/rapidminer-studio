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

import java.util.Iterator;
import java.util.List;

import com.rapidminer.example.Attribute;
import com.rapidminer.example.AttributeWeights;
import com.rapidminer.example.ExampleSet;
import com.rapidminer.example.set.AttributeWeightedExampleSet;
import com.rapidminer.example.set.SplittedExampleSet;
import com.rapidminer.operator.Model;
import com.rapidminer.operator.OperatorCapability;
import com.rapidminer.operator.OperatorDescription;
import com.rapidminer.operator.OperatorException;
import com.rapidminer.operator.OperatorVersion;
import com.rapidminer.operator.ValueDouble;
import com.rapidminer.operator.performance.PerformanceVector;
import com.rapidminer.operator.ports.metadata.AttributeMetaData;
import com.rapidminer.operator.ports.metadata.CapabilityPrecondition;
import com.rapidminer.operator.ports.metadata.Precondition;
import com.rapidminer.operator.ports.quickfix.ParameterSettingQuickFix;
import com.rapidminer.operator.ports.quickfix.QuickFix;
import com.rapidminer.parameter.ParameterType;
import com.rapidminer.parameter.ParameterTypeBoolean;
import com.rapidminer.parameter.ParameterTypeCategory;
import com.rapidminer.parameter.ParameterTypeInt;
import com.rapidminer.parameter.UndefinedParameterError;
import com.rapidminer.parameter.conditions.BooleanParameterCondition;
import com.rapidminer.tools.RandomGenerator;


/**
 * This operator evaluates the performance of feature weighting and selection algorithms. The first
 * subprocess contains the algorithm to be evaluated itself. It must return an attribute weights
 * vector which is then applied on the test data. The same fold {@link XValidation} of the data is
 * used to create a new model during the second subprocess. This model is evaluated in the third
 * subprocess which hence has to return a performance vector. This performance vector serves as a
 * performance indicator for the actual algorithm. This implementation of a MethodValidationChain
 * works similar to the {@link XValidation}.
 *
 * @see com.rapidminer.operator.validation.XValidation
 * @author Ingo Mierswa
 */
public class WrapperXValidation extends WrapperValidationChain {

	/** The parameter name for &quot;Number of subsets for the cross-validation&quot; */
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

	/** Total number of iterations. */
	private int number;

	/** Current iteration. */
	private int iteration;

	public WrapperXValidation(OperatorDescription description) {
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
		return new CapabilityPrecondition(this, exampleSetInput) {

			@Override
			protected List<QuickFix> getFixesForRegressionWhenClassificationSupported(AttributeMetaData labelMD) {
				List<QuickFix> fixes = super.getFixesForRegressionWhenClassificationSupported(labelMD);
				fixes.add(0, new ParameterSettingQuickFix(WrapperXValidation.this, PARAMETER_SAMPLING_TYPE,
						SplittedExampleSet.SHUFFLED_SAMPLING + "", "switch_to_shuffled_sampling"));
				return fixes;
			}
		};
	}

	@Override
	public void doWork() throws OperatorException {
		ExampleSet eSet = exampleSetInput.getData(ExampleSet.class);
		if (getParameterAsBoolean(PARAMETER_LEAVE_ONE_OUT)) {
			number = eSet.size();
		} else {
			number = getParameterAsInt(PARAMETER_NUMBER_OF_VALIDATIONS);
		}

		int samplingType = getParameterAsInt(PARAMETER_SAMPLING_TYPE);
		SplittedExampleSet inputSet = new SplittedExampleSet(eSet, number, samplingType,
				getParameterAsBoolean(RandomGenerator.PARAMETER_USE_LOCAL_RANDOM_SEED),
				getParameterAsInt(RandomGenerator.PARAMETER_LOCAL_RANDOM_SEED), getCompatibilityLevel().isAtMost(
						SplittedExampleSet.VERSION_SAMPLING_CHANGED));

		log("Starting " + number + "-fold method cross validation");

		// statistics init
		PerformanceVector performanceVector = null;
		AttributeWeights globalWeights = new AttributeWeights();
		for (Attribute attribute : eSet.getAttributes()) {
			globalWeights.setWeight(attribute.getName(), 0.0d);
		}

		getProgress().setTotal(number);
		getProgress().setCheckForStop(false);
		for (iteration = 0; iteration < number; iteration++) {
			// training
			inputSet.selectAllSubsetsBut(iteration);

			// apply weighting method
			AttributeWeights weights = useWeightingMethod(inputSet);
			SplittedExampleSet newInputSet = new SplittedExampleSet(inputSet);

			// learn on the same data
			Model model = learn(new AttributeWeightedExampleSet(newInputSet, weights, 0.0d).createCleanClone());

			// testing
			newInputSet.selectSingleSubset(iteration);
			PerformanceVector iterationPerformance = evaluate(
					new AttributeWeightedExampleSet(newInputSet, weights, 0.0d).createCleanClone(), model);

			// build performance average
			if (performanceVector == null) {
				performanceVector = iterationPerformance;
			} else {
				for (int i = 0; i < performanceVector.size(); i++) {
					performanceVector.getCriterion(i).buildAverage(iterationPerformance.getCriterion(i));
				}
			}

			// build weights average
			handleWeights(globalWeights, weights);

			setResult(iterationPerformance.getMainCriterion());
			inApplyLoop();
			getProgress().step();
		}

		// end of cross validation
		getProgress().complete();

		// build average of weights
		Iterator<String> i = globalWeights.getAttributeNames().iterator();
		while (i.hasNext()) {
			String currentName = i.next();
			globalWeights.setWeight(currentName, globalWeights.getWeight(currentName) / number);
		}

		setResult(performanceVector.getMainCriterion());

		performanceOutput.deliver(performanceVector);
		attributeWeightsOutput.deliver(globalWeights);
		getProgress().complete();
	}

	private void handleWeights(AttributeWeights globalWeights, AttributeWeights currentWeights) {
		Iterator<String> i = currentWeights.getAttributeNames().iterator();
		while (i.hasNext()) {
			String currentName = i.next();
			double globalWeight = globalWeights.getWeight(currentName);
			double currentWeight = currentWeights.getWeight(currentName);
			if (Double.isNaN(globalWeight)) {
				globalWeights.setWeight(currentName, currentWeight);
			} else {
				globalWeights.setWeight(currentName, globalWeight + currentWeight);
			}
		}
	}

	@Override
	public List<ParameterType> getParameterTypes() {
		List<ParameterType> types = super.getParameterTypes();
		types.add(new ParameterTypeBoolean(PARAMETER_LEAVE_ONE_OUT,
				"Set the number of validations to the number of examples. If set to true, number_of_validations is ignored",
				false, false));
		ParameterType type = new ParameterTypeInt(PARAMETER_NUMBER_OF_VALIDATIONS,
				"Number of subsets for the crossvalidation", 2, Integer.MAX_VALUE, 10);
		type.registerDependencyCondition(new BooleanParameterCondition(this, PARAMETER_LEAVE_ONE_OUT, true, false));
		type.setExpert(false);
		types.add(type);
		type = new ParameterTypeCategory(
				PARAMETER_SAMPLING_TYPE,
				"Defines the sampling type of the cross validation (linear = consecutive subsets, shuffled = random subsets, stratified = random subsets with class distribution kept constant)",
				SplittedExampleSet.SAMPLING_NAMES, SplittedExampleSet.STRATIFIED_SAMPLING, false);
		type.registerDependencyCondition(new BooleanParameterCondition(this, PARAMETER_LEAVE_ONE_OUT, true, false));
		types.add(type);
		types.addAll(RandomGenerator.getRandomGeneratorParameters(this));
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
