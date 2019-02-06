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

import java.util.List;

import com.rapidminer.example.Attribute;
import com.rapidminer.example.ExampleSet;
import com.rapidminer.operator.OperatorChain;
import com.rapidminer.operator.OperatorDescription;
import com.rapidminer.operator.OperatorException;
import com.rapidminer.operator.UserError;
import com.rapidminer.operator.ValueDouble;
import com.rapidminer.operator.ValueString;
import com.rapidminer.operator.ports.InputPort;
import com.rapidminer.operator.ports.OutputPort;
import com.rapidminer.operator.ports.metadata.ExampleSetPassThroughRule;
import com.rapidminer.operator.ports.metadata.SetRelation;
import com.rapidminer.operator.ports.metadata.SubprocessTransformRule;
import com.rapidminer.parameter.ParameterType;
import com.rapidminer.parameter.ParameterTypeBoolean;
import com.rapidminer.parameter.ParameterTypeInt;
import com.rapidminer.parameter.conditions.BooleanParameterCondition;
import com.rapidminer.tools.math.CombinationGenerator;
import com.rapidminer.tools.math.MathFunctions;


/**
 * <p>
 * This meta operator iterates through all possible feature subsets within the specified range and
 * applies the inner operators on the feature subsets. This might be useful in combination with the
 * ProcessLog operator and, for example, a performance evaluation. In contrast to the BruteForce
 * feature selection, which performs a similar task, this iterative approach needs much less memory
 * and can be performed on larger data sets.
 * </p>
 *
 * @author Ingo Mierswa
 */
public class FeatureSubsetIteration extends OperatorChain {

	private final InputPort exampleSetInput = getInputPorts().createPort("example set", ExampleSet.class);
	private final OutputPort exampleSetOutput = getOutputPorts().createPort("example set");
	private final OutputPort exampleSetInnerSource = getSubprocess(0).getInnerSources().createPort("example set");

	public static final String PARAMETER_LIMIT_MAX = "limit_max_number";
	public static final String PARAMETER_MAX_NUMBER_OF_ATTRIBUTES = "max_number_of_attributes";

	public static final String PARAMETER_MIN_NUMBER_OF_ATTRIBUTES = "min_number_of_attributes";

	public static final String PARAMETER_USE_EXACT_NUMBER = "use_exact_number";
	public static final String PARAMETER_EXACT_NUMBER_OF_ATTRIBUTES = "exact_number_of_attributes";

	private int iteration = -1;

	private int featureNumber = -1;

	private String featureNames = null;

	public FeatureSubsetIteration(OperatorDescription description) {
		super(description, "Subprocess");

		getTransformer().addRule(new ExampleSetPassThroughRule(exampleSetInput, exampleSetInnerSource, SetRelation.SUBSET));
		getTransformer().addRule(new SubprocessTransformRule(getSubprocess(0)));
		getTransformer().addRule(new ExampleSetPassThroughRule(exampleSetInput, exampleSetOutput, SetRelation.SUBSET));

		addValue(new ValueDouble("iteration", "The current iteration.") {

			@Override
			public double getDoubleValue() {
				return iteration;
			}
		});

		addValue(new ValueDouble("feature_number", "The number of used features in the current iteration.") {

			@Override
			public double getDoubleValue() {
				return featureNumber;
			}
		});

		addValue(new ValueString("feature_names", "The names of the used features in the current iteration.") {

			@Override
			public String getStringValue() {
				return featureNames;
			}
		});
	}

	@Override
	public void doWork() throws OperatorException {
		ExampleSet exampleSet = exampleSetInput.getData(ExampleSet.class);
		Attribute[] allAttributes = exampleSet.getAttributes().createRegularAttributeArray();

		// init
		int minNumberOfFeatures = getParameterAsInt(PARAMETER_MIN_NUMBER_OF_ATTRIBUTES);
		int maxNumberOfFeatures = getParameterAsBoolean(PARAMETER_LIMIT_MAX) ? getParameterAsInt(PARAMETER_MAX_NUMBER_OF_ATTRIBUTES)
				: -1;
		int exactNumberOfFeatures = getParameterAsBoolean(PARAMETER_USE_EXACT_NUMBER) ? getParameterAsInt(PARAMETER_EXACT_NUMBER_OF_ATTRIBUTES)
				: -1;

		// checks
		if (exactNumberOfFeatures > 0) {
			log("Using exact number of features for feature subset iteration (" + exactNumberOfFeatures
					+ "), ignoring possibly defined range for the number of features.");
		} else {
			if (maxNumberOfFeatures > 0 && minNumberOfFeatures > maxNumberOfFeatures) {
				throw new UserError(this, 210, PARAMETER_MAX_NUMBER_OF_ATTRIBUTES, PARAMETER_MIN_NUMBER_OF_ATTRIBUTES);
			}
			if (maxNumberOfFeatures > allAttributes.length) {
				throw new UserError(this, 207, new Object[] { maxNumberOfFeatures + "", PARAMETER_MAX_NUMBER_OF_ATTRIBUTES,
						" the parameter value must be smaller than the number of attributes of the input example set." });
			}
			if (maxNumberOfFeatures == -1) {
				maxNumberOfFeatures = exampleSet.getAttributes().size();
			}
		}

		// run
		this.iteration = 0;
		this.featureNumber = 0;
		this.featureNames = "?";
		if (exactNumberOfFeatures > 0) {
			if (exactNumberOfFeatures > allAttributes.length) {
				throw new UserError(this, 207, new Object[] { exactNumberOfFeatures + "",
						PARAMETER_EXACT_NUMBER_OF_ATTRIBUTES,
						" the parameter value must be larger than the number of attributes of the input example set." });
			}
			// init Operator progress
			getProgress().setTotal(
					MathFunctions.factorial(exampleSet.getAttributes().size())
							/ MathFunctions.factorial(exactNumberOfFeatures)
							* MathFunctions.factorial(exampleSet.getAttributes().size() - exactNumberOfFeatures));

			applyOnAllWithExactNumber(exampleSet, allAttributes, exactNumberOfFeatures);
		} else {
			// init Operator progress
			int totalIterations = 0;
			for (int i = minNumberOfFeatures; i <= maxNumberOfFeatures; i++) {
				totalIterations += MathFunctions.factorial(exampleSet.getAttributes().size())
						/ (MathFunctions.factorial(i) * MathFunctions.factorial(exampleSet.getAttributes().size() - i));
			}
			if (totalIterations > 0 && totalIterations <= Integer.MAX_VALUE) {
				getProgress().setTotal(totalIterations);
			}

			applyOnAllInRange(exampleSet, allAttributes, minNumberOfFeatures, maxNumberOfFeatures);
		}
		getProgress().complete();

		exampleSetOutput.deliver(exampleSet);
	}

	private void applyInnerOperators(ExampleSet exampleSet) throws OperatorException {
		exampleSetInnerSource.deliver(exampleSet);
		getSubprocess(0).execute();
	}

	/** Add all attribute combinations with a fixed size to the population. */
	private void applyOnAllWithExactNumber(ExampleSet exampleSet, Attribute[] allAttributes, int exactNumberOfFeatures)
			throws OperatorException {
		ExampleSet workingSet = (ExampleSet) exampleSet.clone();
		this.featureNumber = exactNumberOfFeatures;
		if (exactNumberOfFeatures == 1) {
			for (int i = 0; i < allAttributes.length; i++) {
				workingSet.getAttributes().clearRegular();
				workingSet.getAttributes().addRegular(allAttributes[i]);

				// apply inner
				this.iteration++;
				this.featureNames = allAttributes[i].getName();
				applyInnerOperators(workingSet);
				getProgress().setCompleted(iteration);
			}
		} else if (exactNumberOfFeatures == allAttributes.length) {
			// create current example set
			StringBuffer nameBuffer = new StringBuffer();
			boolean first = true;
			workingSet.getAttributes().clearRegular();
			for (int i = 0; i < allAttributes.length; i++) {
				Attribute attribute = allAttributes[i];
				workingSet.getAttributes().addRegular(attribute);
				if (!first) {
					nameBuffer.append(", ");
				}
				nameBuffer.append(attribute.getName());
				first = false;
			}

			// apply inner
			this.iteration++;
			this.featureNames = nameBuffer.toString();
			applyInnerOperators(workingSet);
			getProgress().setCompleted(iteration);
		} else {
			CombinationGenerator combinationGenerator = new CombinationGenerator(allAttributes.length, exactNumberOfFeatures);
			while (combinationGenerator.hasMore()) {
				int[] indices = combinationGenerator.getNext();

				// create current example set
				StringBuffer nameBuffer = new StringBuffer();
				boolean first = true;
				workingSet.getAttributes().clearRegular();
				for (int i = 0; i < indices.length; i++) {
					Attribute attribute = allAttributes[indices[i]];
					workingSet.getAttributes().addRegular(attribute);
					if (!first) {
						nameBuffer.append(", ");
					}
					nameBuffer.append(attribute.getName());
					first = false;
				}

				// apply inner
				this.iteration++;
				this.featureNames = nameBuffer.toString();
				applyInnerOperators(workingSet);
				getProgress().setCompleted(iteration);
			}
		}
	}

	/** Recursive method to add all attribute combinations to the population. */
	private void applyOnAllInRange(ExampleSet exampleSet, Attribute[] allAttributes, int minNumberOfFeatures,
			int maxNumberOfFeatures) throws OperatorException {
		for (int i = minNumberOfFeatures; i <= maxNumberOfFeatures; i++) {
			applyOnAllWithExactNumber(exampleSet, allAttributes, i);
		}
	}

	@Override
	public List<ParameterType> getParameterTypes() {
		List<ParameterType> types = super.getParameterTypes();
		ParameterType type = new ParameterTypeBoolean(PARAMETER_USE_EXACT_NUMBER,
				"If checked, it will be iterated over all combination with a specified number of attributes.", false, false);
		types.add(type);

		type = new ParameterTypeInt(PARAMETER_EXACT_NUMBER_OF_ATTRIBUTES,
				"Determines the exact number of attributes used for the combinations.", -1, Integer.MAX_VALUE, -1);
		type.registerDependencyCondition(new BooleanParameterCondition(this, PARAMETER_USE_EXACT_NUMBER, true, true));
		type.setExpert(false);
		types.add(type);

		type = new ParameterTypeInt(PARAMETER_MIN_NUMBER_OF_ATTRIBUTES,
				"Determines the minimum number of attributes used for the combinations.", 1, Integer.MAX_VALUE, 1);
		type.registerDependencyCondition(new BooleanParameterCondition(this, PARAMETER_USE_EXACT_NUMBER, true, false));
		type.setExpert(false);
		types.add(type);

		type = new ParameterTypeBoolean(PARAMETER_LIMIT_MAX,
				"If checked, it will be iterated over all combination with at most a specified number of attributes.",
				false, false);
		types.add(type);
		type = new ParameterTypeInt(PARAMETER_MAX_NUMBER_OF_ATTRIBUTES,
				"Determines the maximum number of attributes used for the combinations.", -1, Integer.MAX_VALUE, -1);
		type.registerDependencyCondition(new BooleanParameterCondition(this, PARAMETER_LIMIT_MAX, true, true));
		type.setExpert(false);
		types.add(type);
		return types;
	}

}
