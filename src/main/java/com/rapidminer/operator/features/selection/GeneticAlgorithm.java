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

import com.rapidminer.example.Attribute;
import com.rapidminer.example.AttributeWeights;
import com.rapidminer.example.ExampleSet;
import com.rapidminer.operator.OperatorDescription;
import com.rapidminer.operator.OperatorException;
import com.rapidminer.operator.UserError;
import com.rapidminer.operator.features.Individual;
import com.rapidminer.operator.features.Population;
import com.rapidminer.operator.features.PopulationOperator;
import com.rapidminer.operator.ports.InputPort;
import com.rapidminer.operator.ports.metadata.CompatibilityLevel;
import com.rapidminer.operator.ports.metadata.ExampleSetMetaData;
import com.rapidminer.operator.ports.metadata.MetaData;
import com.rapidminer.operator.ports.metadata.SimplePrecondition;
import com.rapidminer.parameter.ParameterType;
import com.rapidminer.parameter.ParameterTypeBoolean;
import com.rapidminer.parameter.ParameterTypeCategory;
import com.rapidminer.parameter.ParameterTypeDouble;
import com.rapidminer.parameter.ParameterTypeInt;
import com.rapidminer.parameter.UndefinedParameterError;
import com.rapidminer.parameter.conditions.BooleanParameterCondition;


/**
 * A genetic algorithm for feature selection (mutation=switch features on and off,
 * crossover=interchange used features). Selection is done by roulette wheel. Genetic algorithms are
 * general purpose optimization / search algorithms that are suitable in case of no or little
 * problem knowledge. <br/>
 *
 * A genetic algorithm works as follows
 * <ol>
 * <li>Generate an initial population consisting of <code>population_size</code> individuals. Each
 * attribute is switched on with probability <code>p_initialize</code></li>
 * <li>For all individuals in the population
 * <ul>
 * <li>Perform mutation, i.e. set used attributes to unused with probability <code>p_mutation</code>
 * and vice versa.</li>
 * <li>Choose two individuals from the population and perform crossover with probability
 * <code>p_crossover</code>. The type of crossover can be selected by <code>crossover_type</code>.</li>
 * </ul>
 * </li>
 * <li>Perform selection, map all individuals to sections on a roulette wheel whose size is
 * proportional to the individual's fitness and draw <code>population_size</code> individuals at
 * random according to their probability.</li>
 * <li>As long as the fitness improves, go to 2</li>
 * </ol>
 *
 * If the example set contains value series attributes with blocknumbers, the whole block will be
 * switched on and off.
 *
 * @author Ingo Mierswa, Simon Fischer
 */
public class GeneticAlgorithm extends AbstractGeneticAlgorithm {

	/** The parameter name for &quot;Initial probability for an attribute to be switched on.&quot; */
	public static final String PARAMETER_P_INITIALIZE = "p_initialize";

	/**
	 * The parameter name for &quot;Probability for an attribute to be changed (-1: 1 /
	 * numberOfAtt).&quot;
	 */
	public static final String PARAMETER_P_MUTATION = "p_mutation";

	/**
	 * The parameter name for &quot;Probability for an individual to be selected for
	 * crossover.&quot;
	 */
	public static final String PARAMETER_P_CROSSOVER = "p_crossover";

	/** The parameter name for &quot;Type of the crossover.&quot; */
	public static final String PARAMETER_CROSSOVER_TYPE = "crossover_type";

	public static final String PARAMETER_MAX_NUMBER_OF_ATTRIBUTES = "max_number_of_attributes";

	public static final String PARAMETER_MIN_NUMBER_OF_ATTRIBUTES = "min_number_of_attributes";

	public static final String PARAMETER_EXACT_NUMBER_OF_ATTRIBUTES = "exact_number_of_attributes";

	public static final String PARAMETER_INITIALIZE_WITH_INPUT_WEIGHTS = "initialize_with_input_weights";

	public static final String PARAMETER_USE_EXACT_NUMBER = "use_exact_number_of_attributes";
	public static final String PARAMETER_RESTRICT_NUMBER = "restrict_maximum";

	private InputPort attributeWeightsInput = getInputPorts().createPort("attribute weights in");

	public GeneticAlgorithm(OperatorDescription description) {
		super(description);

		attributeWeightsInput.addPrecondition(new SimplePrecondition(attributeWeightsInput, new MetaData(
				AttributeWeights.class), false) {

			@Override
			public boolean isCompatible(MetaData input, CompatibilityLevel level) {
				if (isParameterSet(PARAMETER_INITIALIZE_WITH_INPUT_WEIGHTS)) {
					if (input.getObjectClass().equals(AttributeWeights.class)) {
						return getParameterAsBoolean(PARAMETER_INITIALIZE_WITH_INPUT_WEIGHTS);
					}
				}
				return false;
			}

			@Override
			protected boolean isMandatory() {
				if (isParameterSet(PARAMETER_INITIALIZE_WITH_INPUT_WEIGHTS)) {
					return getParameterAsBoolean(PARAMETER_INITIALIZE_WITH_INPUT_WEIGHTS);
				}
				return false;
			}
		});
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

	/**
	 * Sets up a population of given size and creates ExampleSets with randomly selected attributes
	 * (the probability to be switched on is controlled by pInitialize).
	 */
	@Override
	public Population createInitialPopulation(ExampleSet es) throws OperatorException {
		int minNumber = 1;
		int maxNumber = 1;
		int exactNumber = 0;
		boolean useExactNumber = false;
		boolean restrictMaxNumber = false;

		int numberOfAttributes = es.getAttributes().size();
		if (getParameterAsBoolean(PARAMETER_USE_EXACT_NUMBER)) {
			useExactNumber = true;
			exactNumber = getParameterAsInt(PARAMETER_EXACT_NUMBER_OF_ATTRIBUTES);
			logNote("Using exact number of features for feature selection (" + exactNumber
					+ "), ignoring possibly defined range for the number of features and / or input attribute weights.");
			if (exactNumber > numberOfAttributes) {
				throw new UserError(this, 125, numberOfAttributes, exactNumber);
			}
		} else {
			minNumber = getParameterAsInt(PARAMETER_MIN_NUMBER_OF_ATTRIBUTES);
			if (getParameterAsBoolean(PARAMETER_RESTRICT_NUMBER)) {
				maxNumber = getParameterAsInt(PARAMETER_MAX_NUMBER_OF_ATTRIBUTES);
				restrictMaxNumber = true;
				if (minNumber > maxNumber) {
					throw new UserError(this, 210, PARAMETER_MAX_NUMBER_OF_ATTRIBUTES, PARAMETER_MIN_NUMBER_OF_ATTRIBUTES);
				}
			} else {
				maxNumber = numberOfAttributes;
			}
			if (minNumber > numberOfAttributes) {
				throw new UserError(this, 125, numberOfAttributes, minNumber);
			}
		}

		Population initP = new Population();

		double[] initialWeights = null;
		double p_initialize = getParameterAsDouble(PARAMETER_P_INITIALIZE);
		if (attributeWeightsInput.isConnected()) {
			AttributeWeights inputWeights = null;
			inputWeights = attributeWeightsInput.getData(AttributeWeights.class);
			initialWeights = new double[numberOfAttributes];
			int index = 0;
			for (Attribute attribute : es.getAttributes()) {
				double weight = inputWeights.getWeight(attribute.getName());
				if (Double.isNaN(weight)) {
					weight = getRandom().nextDouble();
				}
				if (weight < 0.0d) {
					weight = 0.0d;
				}
				if (weight > 1.0d) {
					weight = 1.0d;
				}
				if (weight > 0 && weight < 1.0d) {
					if (weight < 1.0d - p_initialize) {
						weight = 1.0d;
					} else {
						weight = 0.0d;
					}
				}
				initialWeights[index++] = weight;
			}
			if (!useExactNumber) {
				Individual individual = new Individual(initialWeights);
				int numberOfFeatures = individual.getNumberOfUsedAttributes();
				if ((!restrictMaxNumber || numberOfFeatures <= maxNumber) && numberOfFeatures >= minNumber) {
					initP.add(individual);
				} else {
					logWarning("Input attribute weights found but number of selected features do not match specified minimum and maximum number, ignoring input weights.");
					initialWeights = null;
				}
			}
		}

		int populationSize = getParameterAsInt(PARAMETER_POPULATION_SIZE);
		if (useExactNumber) { // exact feature number
			while (initP.getNumberOfIndividuals() < populationSize) {
				double[] weights = new double[numberOfAttributes];

				double prob = 1.0d / weights.length * exactNumber;
				for (int i = 0; i < weights.length; i++) {
					if (getRandom().nextDouble() < prob) {
						weights[i] = 1.0d;
					} else {
						weights[i] = 0.0d;
					}
				}

				// add result with exact number of features
				Individual individual = new Individual(weights);
				int numberOfFeatures = individual.getNumberOfUsedAttributes();
				if (exactNumber == numberOfFeatures) {
					initP.add(individual);
				}
			}
		} else { // within range
			while (initP.getNumberOfIndividuals() < populationSize) {
				double[] weights = new double[numberOfAttributes];

				if (initialWeights != null && getRandom().nextBoolean()) {
					for (int i = 0; i < weights.length; i++) {
						if (getRandom().nextDouble() < 1.0d / initialWeights.length) {
							if (initialWeights[i] > 0.0d) {
								weights[i] = 0.0d;
							} else {
								weights[i] = 1.0d;
							}
						} else {
							weights[i] = initialWeights[i];
						}
					}
				} else {
					double p = p_initialize;
					for (int i = 0; i < weights.length; i++) {
						if (getRandom().nextDouble() < 1.0d - p) {
							weights[i] = 1.0d;
						}
					}
				}
				Individual individual = new Individual(weights);
				int numberOfSelectedAttributes = individual.getNumberOfUsedAttributes();
				// increase number of selected if needed
				while (numberOfSelectedAttributes < minNumber && numberOfAttributes >= minNumber) {
					int random = getRandom().nextInt(numberOfAttributes);
					if (weights[random] == 0) {
						weights[random] = 1.0d;
						numberOfSelectedAttributes++;
					}
				}
				// deselect attributes if more than needed
				if (restrictMaxNumber && maxNumber > minNumber) {
					while (numberOfSelectedAttributes > maxNumber) {
						// double probability to converge faster
						double deSelectProb = (numberOfSelectedAttributes - maxNumber)
								/ ((double) numberOfSelectedAttributes - 1);
						for (int i = 0; i < numberOfAttributes; i++) {
							if (weights[i] > 0 && getRandom().nextDouble() < deSelectProb) {
								weights[i] = 0;
								numberOfSelectedAttributes--;
							}
						}
					}
				}
				if ((!restrictMaxNumber || numberOfSelectedAttributes <= maxNumber)
						&& numberOfSelectedAttributes >= minNumber) {
					initP.add(individual);
				}
			}
		}
		return initP;
	}

	/**
	 * Returns an operator that performs the mutation. Can be overridden by subclasses.
	 */
	@Override
	protected PopulationOperator getMutationPopulationOperator(ExampleSet eSet) throws UndefinedParameterError {
		double pMutation = getParameterAsDouble(PARAMETER_P_MUTATION);
		int minNumber = 1;
		int maxNumber = 1;
		int exactNumber = 0;
		boolean useExactNumber = false;
		boolean restrictMaxNumber = false;

		if (getParameterAsBoolean(PARAMETER_USE_EXACT_NUMBER)) {
			useExactNumber = true;
			exactNumber = getParameterAsInt(PARAMETER_EXACT_NUMBER_OF_ATTRIBUTES);
			logNote("Using exact number of features for feature selection (" + exactNumber
					+ "), ignoring possibly defined range for the number of features and / or input attribute weights.");
		} else {
			minNumber = getParameterAsInt(PARAMETER_MIN_NUMBER_OF_ATTRIBUTES);
			if (getParameterAsBoolean(PARAMETER_RESTRICT_NUMBER)) {
				maxNumber = getParameterAsInt(PARAMETER_MAX_NUMBER_OF_ATTRIBUTES);
				restrictMaxNumber = true;
			} else {
				maxNumber = eSet.getAttributes().size();
			}
		}

		return new SelectionMutation(pMutation, getRandom(), minNumber, restrictMaxNumber ? maxNumber : -1,
				useExactNumber ? exactNumber : -1);
	}

	/**
	 * Returns an operator that performs crossover. Can be overridden by subclasses.
	 */
	@Override
	protected PopulationOperator getCrossoverPopulationOperator(ExampleSet eSet) throws UndefinedParameterError {
		double pCrossover = getParameterAsDouble(PARAMETER_P_CROSSOVER);
		int crossoverType = getParameterAsInt(PARAMETER_CROSSOVER_TYPE);
		int minNumber = 1;
		int maxNumber = 1;
		int exactNumber = 0;
		boolean useExactNumber = false;
		boolean restrictMaxNumber = false;

		if (getParameterAsBoolean(PARAMETER_USE_EXACT_NUMBER)) {
			useExactNumber = true;
			exactNumber = getParameterAsInt(PARAMETER_EXACT_NUMBER_OF_ATTRIBUTES);
			logNote("Using exact number of features for feature selection (" + exactNumber
					+ "), ignoring possibly defined range for the number of features and / or input attribute weights.");
		} else {
			minNumber = getParameterAsInt(PARAMETER_MIN_NUMBER_OF_ATTRIBUTES);
			if (getParameterAsBoolean(PARAMETER_RESTRICT_NUMBER)) {
				maxNumber = getParameterAsInt(PARAMETER_MAX_NUMBER_OF_ATTRIBUTES);
				restrictMaxNumber = true;
			} else {
				maxNumber = eSet.getAttributes().size();
			}
		}
		return new SelectionCrossover(crossoverType, pCrossover, getRandom(), minNumber, restrictMaxNumber ? maxNumber : -1,
				useExactNumber ? exactNumber : -1);
	}

	@Override
	public List<ParameterType> getParameterTypes() {
		List<ParameterType> types = new LinkedList<ParameterType>();

		ParameterType type = new ParameterTypeBoolean(PARAMETER_USE_EXACT_NUMBER,
				"Determines if only combinations containing this numbers of attributes should be tested.", false);
		type.setExpert(false);
		types.add(type);

		type = new ParameterTypeBoolean(
				PARAMETER_RESTRICT_NUMBER,
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

		type = new ParameterTypeBoolean(
				PARAMETER_INITIALIZE_WITH_INPUT_WEIGHTS,
				"Indicates if this operator should look for attribute weights in the given input and use the input weights of all known attributes as starting point for the optimization.",
				false);
		type.setDeprecated();
		types.add(type);

		types.addAll(super.getParameterTypes());

		type = new ParameterTypeDouble(PARAMETER_P_INITIALIZE, "Initial probability for an attribute to be switched on.", 0,
				1, 0.5);
		types.add(type);
		type = new ParameterTypeDouble(
				PARAMETER_P_MUTATION,
				"Probability for an attribute to be changed. If this parameter is set to -1, then the probability will be 1/numberOfAttributes.",
				-1.0d, 1.0d, -1.0d);
		types.add(type);
		type = new ParameterTypeDouble(PARAMETER_P_CROSSOVER, "Probability for an individual to be selected for crossover.",
				0.0d, 1.0d, 0.5d);
		types.add(type);
		type = new ParameterTypeCategory(PARAMETER_CROSSOVER_TYPE, "Type of the crossover.",
				SelectionCrossover.CROSSOVER_TYPES, SelectionCrossover.UNIFORM);
		type.setExpert(true);
		types.add(type);
		return types;
	}

}
