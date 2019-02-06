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
import com.rapidminer.operator.features.FeatureOperator;
import com.rapidminer.operator.features.Individual;
import com.rapidminer.operator.features.KeepBest;
import com.rapidminer.operator.features.Population;
import com.rapidminer.operator.features.PopulationOperator;
import com.rapidminer.operator.features.RedundanceRemoval;
import com.rapidminer.operator.ports.metadata.ExampleSetMetaData;
import com.rapidminer.parameter.ParameterType;
import com.rapidminer.parameter.ParameterTypeBoolean;
import com.rapidminer.parameter.ParameterTypeCategory;
import com.rapidminer.parameter.ParameterTypeInt;
import com.rapidminer.parameter.UndefinedParameterError;
import com.rapidminer.parameter.conditions.BooleanParameterCondition;


/**
 * <p>
 * This operator realizes the two deterministic greedy feature selection algorithms forward
 * selection and backward elimination. However, we added some enhancements to the standard
 * algorithms which are described below:
 * </p>
 *
 * <h4>Forward Selection</h4>
 * <ol>
 * <li>Create an initial population with {@rapidminer.math n} individuals where {@rapidminer.math n}
 * is the input example set's number of attributes. Each individual will use exactly one of the
 * features.</li>
 * <li>Evaluate the attribute sets and select only the best {@rapidminer.math k}.</li>
 * <li>For each of the {@rapidminer.math k} attribute sets do: If there are {@rapidminer.math j}
 * unused attributes, make {@rapidminer.math j} copies of the attribute set and add exactly one of
 * the previously unused attributes to the attribute set.</li>
 * <li>As long as the performance improved in the last {@rapidminer.math p} iterations go to 2</li>
 * </ol>
 *
 * <h4>Backward Elimination</h4>
 * <ol>
 * <li>Start with an attribute set which uses all features.</li>
 * <li>Evaluate all attribute sets and select the best {@rapidminer.math k}.</li>
 * <li>For each of the {@rapidminer.math k} attribute sets do: If there are {@rapidminer.math j}
 * attributes used, make {@rapidminer.math j} copies of the attribute set and remove exactly one of
 * the previously used attributes from the attribute set.</li>
 * <li>As long as the performance improved in the last {@rapidminer.math p} iterations go to 2</li>
 * </ol>
 *
 * <p>
 * The parameter {@rapidminer.math k} can be specified by the parameter <code>keep_best</code>, the
 * parameter {@rapidminer.math p} can be specified by the parameter
 * <code>generations_without_improval</code>. These parameters have default values 1 which means
 * that the standard selection algorithms are used. Using other values increase the runtime but
 * might help to avoid local extrema in the search for the global optimum.
 * </p>
 *
 * <p>
 * Another unusual parameter is <code>maximum_number_of_generations</code>. This parameter bounds
 * the number of iterations to this maximum of feature selections / deselections. In combination
 * with <code>generations_without_improval</code> this allows several different selection schemes
 * (which are described for forward selection, backward elimination works analogous):
 *
 * <ul>
 * <li><code>maximum_number_of_generations</code> = {@rapidminer.math m} and
 * <code>generations_without_improval</code> = {@rapidminer.math p}: Selects maximal
 * {@rapidminer.math m} features. The selection stops if not performance improvement was measured in
 * the last {@rapidminer.math p} generations.</li>
 * <li><code>maximum_number_of_generations</code> = {@rapidminer.math -1} and
 * <code>generations_without_improval</code> = {@rapidminer.math p}: Tries to selects new features
 * until no performance improvement was measured in the last {@rapidminer.math p} generations.</li>
 * <li><code>maximum_number_of_generations</code> = {@rapidminer.math m} and
 * <code>generations_without_improval</code> = {@rapidminer.math -1}: Selects maximal
 * {@rapidminer.math m} features. The selection stops is not stopped until all combinations with
 * maximal {@rapidminer.math m} were tried. However, the result might contain less features than
 * these.</li>
 * <li><code>maximum_number_of_generations</code> = {@rapidminer.math -1} and
 * <code>generations_without_improval</code> = {@rapidminer.math -1}: Test all combinations of
 * attributes (brute force, this might take a very long time and should only be applied to small
 * attribute sets).</li>
 * </ul>
 * </p>
 *
 * @author Simon Fischer, Ingo Mierswa
 */
public class FeatureSelectionOperator extends FeatureOperator {

	/** The parameter name for &quot;Forward selection or backward elimination.&quot; */
	public static final String PARAMETER_SELECTION_DIRECTION = "selection_direction";

	/** The parameter name for &quot;Keep the best n individuals in each generation.&quot; */
	public static final String PARAMETER_KEEP_BEST = "keep_best";

	/**
	 * The parameter name for &quot;Stop after n generations without improvement of the performance
	 * (-1: stops if the maximum_number_of_generations is reached).&quot;
	 */
	public static final String PARAMETER_GENERATIONS_WITHOUT_IMPROVAL = "generations_without_improval";
	public static final String PARAMETER_LIMIT_GENERATIONS_WITHOUT_IMPROVAL = "limit_generations_without_improval";

	/**
	 * The parameter name for &quot;Delivers the maximum amount of generations (-1: might use or
	 * deselect all features).&quot;
	 */

	public static final String PARAMETER_LIMIT_NUMBER_OF_GENERATIONS = "limit_number_of_generations";
	public static final String PARAMETER_MAXIMUM_NUMBER_OF_GENERATIONS = "maximum_number_of_generations";

	public static final int FORWARD_SELECTION = 0;

	public static final int BACKWARD_ELIMINATION = 1;

	private static final String[] DIRECTIONS = { "forward", "backward" };

	private int generationsWOImp;

	private int maxGenerations;

	public FeatureSelectionOperator(OperatorDescription description) {
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
	public void doWork() throws OperatorException {
		this.maxGenerations = getParameterAsBoolean(PARAMETER_LIMIT_NUMBER_OF_GENERATIONS) ? getParameterAsInt(PARAMETER_MAXIMUM_NUMBER_OF_GENERATIONS)
				: -1;
		this.generationsWOImp = getParameterAsBoolean(PARAMETER_LIMIT_GENERATIONS_WITHOUT_IMPROVAL) ? getParameterAsInt(PARAMETER_GENERATIONS_WITHOUT_IMPROVAL)
				: -1;
		super.doWork();
	}

	int getDefaultDirection() {
		return FORWARD_SELECTION;
	}

	/**
	 * May <tt>es</tt> have <i>n</i> features. The initial population contains (depending on whether
	 * forward selection or backward elimination is used) either
	 * <ul>
	 * <li><i>n</i> elements with exactly 1 feature switched on or
	 * <li>1 element with all <i>n</i> features switched on.
	 * </ul>
	 */
	@Override
	public Population createInitialPopulation(ExampleSet es) throws UndefinedParameterError {
		int direction = getParameterAsInt(PARAMETER_SELECTION_DIRECTION);
		Population initP = new Population();
		if (direction == FORWARD_SELECTION) {
			for (int a = 0; a < es.getAttributes().size(); a++) {
				double[] weights = new double[es.getAttributes().size()];
				weights[a] = 1.0d;
				initP.add(new Individual(weights));
			}
		} else {
			double[] weights = new double[es.getAttributes().size()];
			for (int a = 0; a < es.getAttributes().size(); a++) {
				weights[a] = 1.0d;
			}
			initP.add(new Individual(weights));
		}
		return initP;
	}

	/**
	 * The operators performs two steps:
	 * <ol>
	 * <li>forward selection/backward elimination
	 * <li>kick out all but the <tt>keep_best</tt> individuals
	 * <li>remove redundant individuals
	 * </ol>
	 */
	@Override
	public List<PopulationOperator> getPreEvaluationPopulationOperators(ExampleSet input) throws OperatorException {
		int direction = getParameterAsInt(PARAMETER_SELECTION_DIRECTION);
		int keepBest = getParameterAsInt(PARAMETER_KEEP_BEST);
		List<PopulationOperator> preOp = new LinkedList<PopulationOperator>();
		preOp.add(new KeepBest(keepBest));
		if (direction == FORWARD_SELECTION) {
			preOp.add(new ForwardSelection());
			if (this.maxGenerations <= 0) {
				this.maxGenerations = input.getAttributes().size() - 1;
			} else {
				this.maxGenerations--; // ensures the correct number of
				// features
			}
		} else {
			preOp.add(new BackwardElimination());
			if (this.maxGenerations <= 0) {
				this.maxGenerations = input.getAttributes().size();
			}
		}
		preOp.add(new RedundanceRemoval());
		return preOp;
	}

	/** empty list */
	@Override
	public List<PopulationOperator> getPostEvaluationPopulationOperators(ExampleSet input) throws OperatorException {
		return new LinkedList<PopulationOperator>();
	}

	/**
	 * Returns true if the best individual is not better than the last generation's best individual.
	 */
	@Override
	public boolean solutionGoodEnough(Population pop) throws OperatorException {
		return pop.empty() || generationsWOImp > 0 && pop.getGenerationsWithoutImproval() >= generationsWOImp
				|| pop.getGeneration() >= maxGenerations;
	}

	@Override
	public List<ParameterType> getParameterTypes() {
		List<ParameterType> types = new LinkedList<ParameterType>();
		ParameterType type = new ParameterTypeCategory(PARAMETER_SELECTION_DIRECTION,
				"Forward selection or backward elimination.", DIRECTIONS, getDefaultDirection());
		type.setExpert(false);
		types.add(type);

		type = new ParameterTypeBoolean(
				PARAMETER_LIMIT_GENERATIONS_WITHOUT_IMPROVAL,
				"Indicates if the optimization should be aborted if this number of generations showed no improvement. If unchecked, always the maximal number of generations will be used.",
				true);
		type.setExpert(false);
		types.add(type);
		type = new ParameterTypeInt(PARAMETER_GENERATIONS_WITHOUT_IMPROVAL,
				"Stop after n generations without improval of the performance.", 1, Integer.MAX_VALUE, 1);
		type.registerDependencyCondition(new BooleanParameterCondition(this, PARAMETER_LIMIT_GENERATIONS_WITHOUT_IMPROVAL,
				false, true));
		types.add(type);

		types.add(new ParameterTypeBoolean(PARAMETER_LIMIT_NUMBER_OF_GENERATIONS,
				"Defines if the number of generations should be limited on a specific number.", false, false));
		type = new ParameterTypeInt(PARAMETER_MAXIMUM_NUMBER_OF_GENERATIONS, "Defines the maximum amount of generations.",
				1, Integer.MAX_VALUE, 10);
		type.registerDependencyCondition(new BooleanParameterCondition(this, PARAMETER_LIMIT_NUMBER_OF_GENERATIONS, true,
				true));
		type.setExpert(false);

		types.add(new ParameterTypeInt(PARAMETER_KEEP_BEST, "Keep the best n individuals in each generation.", 1,
				Integer.MAX_VALUE, 1));
		types.add(type);

		types.addAll(super.getParameterTypes());
		return types;
	}

	@Override
	protected int getMaximumGenerations() throws UndefinedParameterError {
		return getParameterAsBoolean(PARAMETER_LIMIT_NUMBER_OF_GENERATIONS) ? getParameterAsInt(PARAMETER_MAXIMUM_NUMBER_OF_GENERATIONS)
				: -1;
	}
}
