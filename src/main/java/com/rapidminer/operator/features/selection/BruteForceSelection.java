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
package com.rapidminer.operator.features.selection;

import java.util.LinkedList;
import java.util.List;

import com.rapidminer.example.ExampleSet;
import com.rapidminer.operator.OperatorDescription;
import com.rapidminer.operator.OperatorException;
import com.rapidminer.operator.UserError;
import com.rapidminer.operator.features.FeatureOperator;
import com.rapidminer.operator.features.Individual;
import com.rapidminer.operator.features.Population;
import com.rapidminer.operator.features.PopulationOperator;
import com.rapidminer.operator.ports.metadata.ExampleSetMetaData;
import com.rapidminer.parameter.ParameterType;
import com.rapidminer.parameter.ParameterTypeBoolean;
import com.rapidminer.parameter.ParameterTypeInt;
import com.rapidminer.parameter.UndefinedParameterError;
import com.rapidminer.parameter.conditions.BooleanParameterCondition;


/**
 * This feature selection operator selects the best attribute set by trying all possible
 * combinations of attribute selections. It returns the example set containing the subset of
 * attributes which produced the best performance. As this operator works on the powerset of the
 * attributes set it has exponential runtime.
 *
 * @author Simon Fischer, Ingo Mierswa
 */
public class BruteForceSelection extends FeatureOperator {

	public static final String PARAMETER_USE_EXACT_NUMBER = "use_exact_number_of_attributes";
	public static final String PARAMETER_RESTRICT_NUMBER = "restrict_maximum";
	public static final String PARAMETER_MIN_NUMBER_OF_ATTRIBUTES = "min_number_of_attributes";
	public static final String PARAMETER_MAX_NUMBER_OF_ATTRIBUTES = "max_number_of_attributes";
	public static final String PARAMETER_EXACT_NUMBER_OF_ATTRIBUTES = "exact_number_of_attributes";

	public BruteForceSelection(OperatorDescription description) {
		super(description);
	}

	@Override
	protected ExampleSetMetaData modifyInnerOutputExampleSet(ExampleSetMetaData metaData) {
		metaData.attributesAreSubset();
		return metaData;
	}

	@Override
	protected ExampleSetMetaData modifyOutputExampleSet(ExampleSetMetaData metaData) {
		metaData.attributesAreSubset();
		return metaData;
	}

	@Override
	public Population createInitialPopulation(ExampleSet es) throws OperatorException {
		int minNumberOfFeatures = getParameterAsInt(PARAMETER_MIN_NUMBER_OF_ATTRIBUTES);
		int maxNumberOfFeatures = getParameterAsInt(PARAMETER_MAX_NUMBER_OF_ATTRIBUTES);
		int exactNumberOfFeatures = getParameterAsInt(PARAMETER_EXACT_NUMBER_OF_ATTRIBUTES);
		boolean useExactNumber = getParameterAsBoolean(PARAMETER_USE_EXACT_NUMBER);

		if (useExactNumber) {
			logNote("Using exact number of features for feature selection (" + exactNumberOfFeatures
					+ "), ignoring possibly defined range for the number of features.");
		} else {
			if (!getParameterAsBoolean(PARAMETER_RESTRICT_NUMBER)) {
				maxNumberOfFeatures = es.getAttributes().size();
			} else {
				if (minNumberOfFeatures > maxNumberOfFeatures) {
					throw new UserError(this, 210, PARAMETER_MAX_NUMBER_OF_ATTRIBUTES, PARAMETER_MIN_NUMBER_OF_ATTRIBUTES);
				}
			}
		}

		Population pop = new Population();
		double[] weights = new double[es.getAttributes().size()];
		if (useExactNumber) {
			addAllWithExactNumber(pop, weights, 0, exactNumberOfFeatures);
		} else {
			addAllInRange(pop, weights, 0, minNumberOfFeatures, maxNumberOfFeatures);
		}

		return pop;
	}

	/** Add all attribute combinations with a fixed size to the population. */
	private void addAllWithExactNumber(Population pop, double[] weights, int startIndex, int exactNumberOfFeatures) {
		Individual individual = new Individual(weights);
		if (individual.getNumberOfUsedAttributes() > exactNumberOfFeatures) {
			return;
		}
		for (int i = startIndex; i < weights.length; i++) {
			double[] clone = individual.getWeightsClone();
			clone[i] = 1.0d;
			Individual newIndividual = new Individual(clone);
			if (newIndividual.getNumberOfUsedAttributes() == exactNumberOfFeatures) {
				pop.add(newIndividual);
			} else {
				addAllWithExactNumber(pop, clone, i + 1, exactNumberOfFeatures);
			}
		}
	}

	/** Recursive method to add all attribute combinations to the population. */
	private void addAllInRange(Population pop, double[] weights, int startIndex, int minNumberOfFeatures,
			int maxNumberOfFeatures) {
		if (startIndex >= weights.length) {
			return;
		}
		Individual individual = new Individual(weights);
		int numberOfFeatures = individual.getNumberOfUsedAttributes();
		if (maxNumberOfFeatures > 0) {
			if (numberOfFeatures > maxNumberOfFeatures) {
				return;
			}
		}

		// recursive call
		double[] clone = individual.getWeightsClone();
		clone[startIndex] = 0;
		addAllInRange(pop, clone, startIndex + 1, minNumberOfFeatures, maxNumberOfFeatures);

		double[] clone2 = individual.getWeightsClone();
		clone2[startIndex] = 1.0d;
		Individual newIndividual = new Individual(clone2);
		numberOfFeatures = newIndividual.getNumberOfUsedAttributes();
		if (numberOfFeatures > 0) {
			if ((maxNumberOfFeatures < 1 || numberOfFeatures <= maxNumberOfFeatures)
					&& numberOfFeatures >= minNumberOfFeatures) {
				pop.add(newIndividual);
			}
		}
		addAllInRange(pop, clone2, startIndex + 1, minNumberOfFeatures, maxNumberOfFeatures);
	}

	/** Does nothing. */
	@Override
	public List<PopulationOperator> getPreEvaluationPopulationOperators(ExampleSet input) throws OperatorException {
		return new LinkedList<PopulationOperator>();
	}

	/** Returns an empty list if the parameter debug_output is set to false. */
	@Override
	public List<PopulationOperator> getPostEvaluationPopulationOperators(ExampleSet input) throws OperatorException {
		return new LinkedList<PopulationOperator>();
	}

	/** Stops immediately. */
	@Override
	public boolean solutionGoodEnough(Population pop) {
		return true;
	}

	@Override
	public List<ParameterType> getParameterTypes() {
		List<ParameterType> types = new LinkedList<ParameterType>();
		ParameterType type = new ParameterTypeBoolean(PARAMETER_USE_EXACT_NUMBER,
				"Determines if only combinations containing this numbers of attributes should be tested.", false);
		type.setExpert(false);
		types.add(type);

		type = new ParameterTypeBoolean(PARAMETER_RESTRICT_NUMBER,
				"If checked the maximal number of attributes might be restricted. Otherwise all combinations of all number of attributes are generated and tested.",
				false);
		type.registerDependencyCondition(new BooleanParameterCondition(this, PARAMETER_USE_EXACT_NUMBER, false, false));
		type.setExpert(false);
		types.add(type);

		type = new ParameterTypeInt(PARAMETER_MIN_NUMBER_OF_ATTRIBUTES,
				"Determines the minimum number of features used for the combinations.", 1, Integer.MAX_VALUE, 1);
		type.registerDependencyCondition(new BooleanParameterCondition(this, PARAMETER_USE_EXACT_NUMBER, true, false));
		type.setExpert(false);
		types.add(type);
		type = new ParameterTypeInt(PARAMETER_MAX_NUMBER_OF_ATTRIBUTES,
				"Determines the maximum number of features used for the combinations.", 1, Integer.MAX_VALUE, 1);
		type.registerDependencyCondition(new BooleanParameterCondition(this, PARAMETER_RESTRICT_NUMBER, true, true));
		type.registerDependencyCondition(new BooleanParameterCondition(this, PARAMETER_USE_EXACT_NUMBER, true, false));
		type.setExpert(false);
		types.add(type);
		type = new ParameterTypeInt(PARAMETER_EXACT_NUMBER_OF_ATTRIBUTES,
				"Determines the exact number of features used for the combinations.", 1, Integer.MAX_VALUE, 1);
		type.registerDependencyCondition(new BooleanParameterCondition(this, PARAMETER_USE_EXACT_NUMBER, true, true));
		type.setExpert(false);
		types.add(type);

		types.addAll(super.getParameterTypes());
		return types;
	}

	@Override
	protected int getMaximumGenerations() throws UndefinedParameterError {
		// return null because we perform no generation step but exactly the initial evaluation will
		// be performed
		return 0;
	}
}
