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

import com.rapidminer.example.ExampleSet;
import com.rapidminer.operator.OperatorDescription;
import com.rapidminer.operator.OperatorException;
import com.rapidminer.operator.features.FeatureOperator;
import com.rapidminer.operator.features.KeepBest;
import com.rapidminer.operator.features.Population;
import com.rapidminer.operator.features.PopulationOperator;
import com.rapidminer.operator.features.RedundanceRemoval;
import com.rapidminer.parameter.ParameterType;
import com.rapidminer.parameter.ParameterTypeInt;
import com.rapidminer.parameter.ParameterTypeString;

import java.util.LinkedList;
import java.util.List;


/**
 * This operator performs the weighting under the naive assumption that the features are independent
 * from each other. Each attribute is weighted with a linear search. This approach may deliver good
 * results after short time if the features indeed are not highly correlated. <br />
 * The ideas of forward selection and backward elimination can easily be used for the weighting with
 * help of a {@link SimpleWeighting}.
 * 
 * @author Ingo Mierswa Exp $
 */
public abstract class FeatureWeighting extends FeatureOperator {

	/** The parameter name for &quot;Keep the best n individuals in each generation.&quot; */
	public static final String PARAMETER_KEEP_BEST = "keep_best";

	/**
	 * The parameter name for &quot;Stop after n generations without improvement of the
	 * performance.&quot;
	 */
	public static final String PARAMETER_GENERATIONS_WITHOUT_IMPROVAL = "generations_without_improval";

	/**
	 * The parameter name for &quot;Use these weights for the creation of individuals in each
	 * generation.&quot;
	 */
	public static final String PARAMETER_WEIGHTS = "weights";
	private List<PopulationOperator> preOps = new LinkedList<PopulationOperator>();

	private List<PopulationOperator> postOps = new LinkedList<PopulationOperator>();

	private int generationsWOImp = 0;

	public abstract PopulationOperator getWeightingOperator(String parameter);

	public FeatureWeighting(OperatorDescription description) {
		super(description);
	}

	@Override
	public void doWork() throws OperatorException {
		generationsWOImp = getParameterAsInt(PARAMETER_GENERATIONS_WITHOUT_IMPROVAL);

		preOps = new LinkedList<PopulationOperator>();
		preOps.add(new KeepBest(getParameterAsInt(PARAMETER_KEEP_BEST)));
		preOps.add(getWeightingOperator(getParameterAsString(PARAMETER_WEIGHTS)));
		preOps.add(new RedundanceRemoval());

		postOps = new LinkedList<PopulationOperator>();

		super.doWork();
	}

	@Override
	public boolean solutionGoodEnough(Population population) {
		boolean stop = population.empty() || (population.getGenerationsWithoutImproval() >= generationsWOImp);
		return stop;
	}

	@Override
	public List<PopulationOperator> getPreEvaluationPopulationOperators(ExampleSet eSet) {
		return preOps;
	}

	@Override
	public List<PopulationOperator> getPostEvaluationPopulationOperators(ExampleSet eSet) {
		return postOps;
	}

	@Override
	public List<ParameterType> getParameterTypes() {
		List<ParameterType> types = new LinkedList<ParameterType>();
		ParameterType type = new ParameterTypeInt(PARAMETER_KEEP_BEST, "Keep the best n individuals in each generation.", 1,
				Integer.MAX_VALUE, 1);
		type.setExpert(false);
		types.add(type);
		type = new ParameterTypeInt(PARAMETER_GENERATIONS_WITHOUT_IMPROVAL,
				"Stop after n generations without improvement of the performance.", 1, Integer.MAX_VALUE, 1);
		type.setExpert(false);
		types.add(type);
		types.add(new ParameterTypeString(PARAMETER_WEIGHTS,
				"Use these weights for the creation of individuals in each generation.", true));

		types.addAll(super.getParameterTypes());
		return types;
	}
}
