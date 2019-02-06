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
package com.rapidminer.operator.features.construction;

import java.util.LinkedList;
import java.util.List;

import com.rapidminer.example.ExampleSet;
import com.rapidminer.generator.FeatureGenerator;
import com.rapidminer.operator.OperatorDescription;
import com.rapidminer.parameter.ParameterType;
import com.rapidminer.parameter.ParameterTypeBoolean;
import com.rapidminer.parameter.ParameterTypeDouble;
import com.rapidminer.parameter.ParameterTypeInt;
import com.rapidminer.parameter.UndefinedParameterError;
import com.rapidminer.parameter.conditions.BooleanParameterCondition;


/**
 * In contrast to the class {@link com.rapidminer.operator.features.selection.GeneticAlgorithm}, the
 * {@link GeneratingGeneticAlgorithm} generates new attributes and thus can change the length of an
 * individual. Therfore specialized mutation and crossover operators are being applied. Generators
 * are chosen at random from a list of generators specified by boolean parameters. <br/>
 * 
 * Since this operator does not contain algorithms to extract features from value series, it is
 * restricted to example sets with only single attributes. For automatic feature extraction from
 * values series the value series plugin for RapidMiner written by Ingo Mierswa should be used. It
 * is available at <a href="http://rapidminer.com">http://rapidminer.com</a>
 * 
 * @rapidminer.reference Ritthoff/etal/2001a
 * @author Ingo Mierswa, Simon Fischer ingomierswa Exp $
 */
public class GeneratingGeneticAlgorithm extends AbstractGeneratingGeneticAlgorithm {

	/**
	 * The parameter name for &quot;Max number of attributes to generate for an individual in one
	 * generation.&quot;
	 */
	public static final String PARAMETER_MAX_NUMBER_OF_NEW_ATTRIBUTES = "max_number_of_new_attributes";

	/**
	 * The parameter name for &quot;Max total number of attributes in all generations (-1: no
	 * maximum).&quot;
	 */
	public static final String PARAMETER_MAX_TOTAL_NUMBER_OF_ATTRIBUTES = "max_total_number_of_attributes";
	public static final String PARAMETER_LIMIT_MAX_TOTAL_NUMBER_OF_ATTRIBUTES = "limit_max_total_number_of_attributes";

	/**
	 * The parameter name for &quot;Probability for an individual to be selected for
	 * generation.&quot;
	 */
	public static final String PARAMETER_P_GENERATE = "p_generate";

	/**
	 * The parameter name for &quot;Probability for an attribute to be changed (-1: 1 /
	 * numberOfAtts).&quot;
	 */
	public static final String PARAMETER_P_MUTATION = "p_mutation";
	public static final String PARAMETER_USE_HEURISTIC_MUTATION_PROBABILITY = "use_heuristic_mutation_probability";

	public GeneratingGeneticAlgorithm(OperatorDescription description) {
		super(description);
	}

	/**
	 * Returns an operator that performs the mutation. Can be overridden by subclasses.
	 */
	@Override
	protected ExampleSetBasedPopulationOperator getMutationPopulationOperator(ExampleSet eSet)
			throws UndefinedParameterError {
		double pMutation = getParameterAsBoolean(PARAMETER_USE_HEURISTIC_MUTATION_PROBABILITY) ? -1
				: getParameterAsDouble(PARAMETER_P_MUTATION);
		return new ExampleSetBasedSelectionMutation(pMutation, getRandom(), 1,
				getParameterAsInt(PARAMETER_MAX_TOTAL_NUMBER_OF_ATTRIBUTES), -1);
	}

	/** Returns a specialized mutation, i.e. a <code>AttributeGenerator</code> */
	@Override
	protected ExampleSetBasedPopulationOperator getGeneratingPopulationOperator(ExampleSet eSet)
			throws UndefinedParameterError {
		List<FeatureGenerator> generators = getGenerators();
		if (generators.size() == 0) {
			logWarning("No FeatureGenerators specified for " + getName() + ".");
		}
		int noOfNewAttributes = getParameterAsInt(PARAMETER_MAX_NUMBER_OF_NEW_ATTRIBUTES);
		int totalNoOfNewAttributes = getParameterAsBoolean(PARAMETER_LIMIT_MAX_TOTAL_NUMBER_OF_ATTRIBUTES) ? -1
				: getParameterAsInt(PARAMETER_MAX_TOTAL_NUMBER_OF_ATTRIBUTES);
		double pGenerate = getParameterAsDouble(PARAMETER_P_GENERATE);

		return new AttributeGenerator(pGenerate, noOfNewAttributes, totalNoOfNewAttributes, generators, getRandom());
	}

	@Override
	public List<ParameterType> getParameterTypes() {
		List<ParameterType> types = new LinkedList<ParameterType>();
		ParameterType type = new ParameterTypeInt(PARAMETER_MAX_NUMBER_OF_NEW_ATTRIBUTES,
				"Max number of attributes to generate for an individual in one generation.", 0, Integer.MAX_VALUE, 1);
		type.setExpert(false);
		types.add(type);

		type = new ParameterTypeBoolean(PARAMETER_LIMIT_MAX_TOTAL_NUMBER_OF_ATTRIBUTES,
				"Indicates if the total number of attributes in all generations should be limited.", false);
		types.add(type);
		type = new ParameterTypeInt(PARAMETER_MAX_TOTAL_NUMBER_OF_ATTRIBUTES,
				"Max total number of attributes in all generations.", 1, Integer.MAX_VALUE, 1);
		type.registerDependencyCondition(new BooleanParameterCondition(this, PARAMETER_LIMIT_MAX_TOTAL_NUMBER_OF_ATTRIBUTES,
				false, true));
		types.add(type);

		types.addAll(super.getParameterTypes());

		type = new ParameterTypeDouble(PARAMETER_P_GENERATE, "Probability for an individual to be selected for generation.",
				0, 1, 0.1);
		types.add(type);
		types.add(new ParameterTypeBoolean(PARAMETER_USE_HEURISTIC_MUTATION_PROBABILITY,
				"If checked the probability for mutations will be chosen as 1/number of attributes.", true));
		type = new ParameterTypeDouble(PARAMETER_P_MUTATION, "Probability for mutation.", 0, 1);
		type.registerDependencyCondition(new BooleanParameterCondition(this, PARAMETER_USE_HEURISTIC_MUTATION_PROBABILITY,
				false, false));
		types.add(type);
		return types;
	}
}
