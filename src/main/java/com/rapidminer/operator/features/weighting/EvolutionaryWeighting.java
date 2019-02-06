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
package com.rapidminer.operator.features.weighting;

import java.util.LinkedList;
import java.util.List;

import com.rapidminer.example.Attribute;
import com.rapidminer.example.AttributeWeights;
import com.rapidminer.example.Attributes;
import com.rapidminer.example.ExampleSet;
import com.rapidminer.operator.OperatorDescription;
import com.rapidminer.operator.OperatorException;
import com.rapidminer.operator.features.Individual;
import com.rapidminer.operator.features.Population;
import com.rapidminer.operator.features.PopulationOperator;
import com.rapidminer.operator.features.selection.AbstractGeneticAlgorithm;
import com.rapidminer.operator.features.selection.SelectionCrossover;
import com.rapidminer.operator.ports.InputPort;
import com.rapidminer.operator.ports.metadata.MetaData;
import com.rapidminer.operator.ports.metadata.SimplePrecondition;
import com.rapidminer.parameter.ParameterType;
import com.rapidminer.parameter.ParameterTypeBoolean;
import com.rapidminer.parameter.ParameterTypeCategory;
import com.rapidminer.parameter.ParameterTypeDouble;
import com.rapidminer.parameter.UndefinedParameterError;
import com.rapidminer.parameter.conditions.BooleanParameterCondition;


/**
 * This operator performs the weighting of features with an evolutionary strategies approach. The
 * variance of the gaussian additive mutation can be adapted by a 1/5-rule.
 *
 * @author Ingo Mierswa, Sebastian Land
 */
public class EvolutionaryWeighting extends AbstractGeneticAlgorithm {

	/** The parameter name for &quot;The (initial) variance for each mutation.&quot; */
	public static final String PARAMETER_MUTATION_VARIANCE = "mutation_variance";

	/**
	 * The parameter name for &quot;If set to true, the 1/5 rule for variance adaption is
	 * used.&quot;
	 */
	public static final String PARAMETER_1_5_RULE = "1_5_rule";

	/** The parameter name for &quot;If set to true, the weights are bounded between 0 and 1.&quot; */
	public static final String PARAMETER_BOUNDED_MUTATION = "bounded_mutation";

	/**
	 * The parameter name for &quot;Probability for an individual to be selected for
	 * crossover.&quot;
	 */
	public static final String PARAMETER_P_CROSSOVER = "p_crossover";

	/** The parameter name for &quot;Type of the crossover.&quot; */
	public static final String PARAMETER_CROSSOVER_TYPE = "crossover_type";

	public static final String PARAMETER_INITIALIZE_WITH_INPUT_WEIGHTS = "initialize_with_input_weights";

	public static final String PARAMETER_NOMINAL_MUTATION_RATE = "nominal_mutation_rate";

	public static final String PARAMETER_DEFAULT_NOMINAL_MUTATION_RATE = "use_default_mutation_rate";

	private WeightingMutation weighting = null;

	private boolean useBoundedMutation = false;

	private final InputPort attributeWeightsInput = getInputPorts().createPort("attribute weights in");

	public EvolutionaryWeighting(OperatorDescription description) {
		super(description);

		attributeWeightsInput.addPrecondition(new SimplePrecondition(attributeWeightsInput, new MetaData(
				AttributeWeights.class), false));
	}

	@Override
	public PopulationOperator getCrossoverPopulationOperator(ExampleSet eSet) throws UndefinedParameterError {
		return new SelectionCrossover(getParameterAsInt(PARAMETER_CROSSOVER_TYPE),
				getParameterAsDouble(PARAMETER_P_CROSSOVER), getRandom(), 1, eSet.getAttributes().size(), -1);
	}

	@Override
	public PopulationOperator getMutationPopulationOperator(ExampleSet eSet) throws UndefinedParameterError {
		Attributes attributes = eSet.getAttributes();
		boolean[] isNominal = new boolean[attributes.size()];
		int i = 0;
		for (Attribute attribute : attributes) {
			isNominal[i] = attribute.isNominal();
			i++;
		}
		double nominalMutationProb = 1d / attributes.size();
		if (!getParameterAsBoolean(PARAMETER_DEFAULT_NOMINAL_MUTATION_RATE)) {
			nominalMutationProb = getParameterAsDouble(PARAMETER_NOMINAL_MUTATION_RATE);
		}

		this.weighting = new WeightingMutation(getParameterAsDouble(PARAMETER_MUTATION_VARIANCE), useBoundedMutation,
				isNominal, nominalMutationProb, getRandom());
		return weighting;
	}

	@Override
	protected List<PopulationOperator> getPostProcessingPopulationOperators(ExampleSet eSet) throws UndefinedParameterError {
		List<PopulationOperator> otherPostOps = new LinkedList<PopulationOperator>();
		if (getParameterAsBoolean(PARAMETER_1_5_RULE)) {
			otherPostOps.add(new VarianceAdaption(weighting, eSet.getAttributes().size()));
		}
		return otherPostOps;
	}

	@Override
	public void doWork() throws OperatorException {
		// first test if nominal attributes are present to make warning and check bound mutation for
		// nominal values
		// handling
		boolean useBoundedMutation = getParameterAsBoolean(PARAMETER_BOUNDED_MUTATION);
		if (!useBoundedMutation) {
			ExampleSet exampleSet = getExampleSetInput().getData(ExampleSet.class);
			boolean containsNominalAttributes = false;
			for (Attribute attribute : exampleSet.getAttributes()) {
				if (attribute.isNominal()) {
					containsNominalAttributes = true;
					break;
				}
			}
			if (containsNominalAttributes) {
				useBoundedMutation = true;
				logWarning("If ExampleSet contains nominal attributes, bounded mutation must be used: Switched to bounded mutation automatically.");
			}
		}
		super.doWork();
	}

	@Override
	public Population createInitialPopulation(ExampleSet exampleSet) throws OperatorException {
		Population initPop = new Population();
		int numberOfIndividuals = getParameterAsInt(PARAMETER_POPULATION_SIZE);

		double[] initialWeights = null;
		if (getParameterAsBoolean(PARAMETER_INITIALIZE_WITH_INPUT_WEIGHTS)) {
			AttributeWeights inputWeights = null;
			inputWeights = attributeWeightsInput.getData(AttributeWeights.class);
			initialWeights = new double[exampleSet.getAttributes().size()];
			int index = 0;
			for (Attribute attribute : exampleSet.getAttributes()) {
				double weight = inputWeights.getWeight(attribute.getName());
				if (Double.isNaN(weight)) {
					weight = getRandom().nextDouble();
				}
				initialWeights[index++] = weight;
			}
			initPop.add(new Individual(initialWeights));
		}

		// fill with variants of the initial weights
		if (initialWeights != null) {
			while (initPop.getNumberOfIndividuals() < numberOfIndividuals / 2) {
				double[] weights = new double[exampleSet.getAttributes().size()];
				for (int w = 0; w < weights.length; w++) {
					weights[w] = Math.min(1.0d, Math.max(0.0d, initialWeights[w] + getRandom().nextGaussian() * 0.1d));
				}
				initPop.add(new Individual(weights));
			}
		}

		// fill up with random individuals
		while (initPop.getNumberOfIndividuals() < numberOfIndividuals) {
			double[] weights = new double[exampleSet.getAttributes().size()];
			for (int w = 0; w < weights.length; w++) {
				weights[w] = getRandom().nextDouble();
			}
			initPop.add(new Individual(weights));
		}

		return initPop;
	}

	@Override
	public List<ParameterType> getParameterTypes() {
		List<ParameterType> types = super.getParameterTypes();
		types.add(new ParameterTypeDouble(PARAMETER_MUTATION_VARIANCE, "The (initial) variance for each mutation.", 0.0d,
				Double.POSITIVE_INFINITY, 1.0d));
		types.add(new ParameterTypeBoolean(PARAMETER_1_5_RULE,
				"If set to true, the 1/5 rule for variance adaption is used.", true));
		types.add(new ParameterTypeBoolean(PARAMETER_BOUNDED_MUTATION,
				"If set to true, the weights are bounded between 0 and 1.", false));
		ParameterType type = new ParameterTypeDouble(PARAMETER_P_CROSSOVER,
				"Probability for an individual to be selected for crossover.", 0.0d, 1.0d, 0.0d);
		types.add(type);
		types.add(new ParameterTypeCategory(PARAMETER_CROSSOVER_TYPE, "Type of the crossover.",
				SelectionCrossover.CROSSOVER_TYPES, SelectionCrossover.UNIFORM));
		types.add(new ParameterTypeBoolean(PARAMETER_DEFAULT_NOMINAL_MUTATION_RATE,
				"Use the default mutation rate for nominal attributes.", true));
		type = new ParameterTypeDouble(PARAMETER_NOMINAL_MUTATION_RATE,
				"The probability to switch nominal attributes between 0 and 1.", 0, 1);
		type.registerDependencyCondition(new BooleanParameterCondition(this, PARAMETER_DEFAULT_NOMINAL_MUTATION_RATE, true,
				false));
		types.add(type);
		types.add(new ParameterTypeBoolean(
				PARAMETER_INITIALIZE_WITH_INPUT_WEIGHTS,
				"Indicates if this operator should look for attribute weights in the given input and use the input weights of all known attributes as starting point for the optimization.",
				false));

		return types;
	}

}
