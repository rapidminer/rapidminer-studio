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
package com.rapidminer.operator.learner.tree;

import java.util.List;

import com.rapidminer.operator.OperatorDescription;
import com.rapidminer.parameter.ParameterType;
import com.rapidminer.parameter.ParameterTypeBoolean;
import com.rapidminer.parameter.ParameterTypeDouble;
import com.rapidminer.parameter.UndefinedParameterError;
import com.rapidminer.parameter.conditions.BooleanParameterCondition;
import com.rapidminer.tools.RandomGenerator;


/**
 * <p>
 * This operator learns decision trees from both nominal and numerical data. Decision trees are
 * powerful classification methods which often can also easily be understood. The random tree
 * learner works similar to Quinlan's C4.5 or CART but it selects a random subset of attributes
 * before it is applied. The size of the subset is defined by the parameter subset_ratio.
 * </p>
 *
 * @author Ingo Mierswa
 *
 */
@SuppressWarnings("deprecation")
public class RandomTreeLearner extends DecisionTreeLearner {

	public static final String PARAMETER_USE_HEURISTIC_SUBSET_RATION = "guess_subset_ratio";

	/** The parameter name for &quot;Ratio of randomly chosen attributes to test&quot; */
	public static final String PARAMETER_SUBSET_RATIO = "subset_ratio";

	public RandomTreeLearner(OperatorDescription description) {
		super(description);
	}

	/** Returns a random feature subset sampling. */
	@Override
	public SplitPreprocessing getSplitPreprocessing() {
		SplitPreprocessing preprocessing = null;
		try {
			preprocessing = new RandomSubsetPreprocessing(getParameterAsBoolean(PARAMETER_USE_HEURISTIC_SUBSET_RATION),
					getParameterAsDouble(PARAMETER_SUBSET_RATIO), RandomGenerator.getRandomGenerator(
							getParameterAsBoolean(RandomGenerator.PARAMETER_USE_LOCAL_RANDOM_SEED),
							getParameterAsInt(RandomGenerator.PARAMETER_LOCAL_RANDOM_SEED)));
		} catch (UndefinedParameterError e) {
			// cannot happen
		}
		return preprocessing;
	}

	@Override
	public List<ParameterType> getParameterTypes() {
		List<ParameterType> types = super.getParameterTypes();
		ParameterType type = new ParameterTypeBoolean(PARAMETER_USE_HEURISTIC_SUBSET_RATION,
				"Indicates that log(m) + 1 features are used, otherwise a ratio has to be specified.", true);
		type.setExpert(false);
		types.add(type);

		type = new ParameterTypeDouble(PARAMETER_SUBSET_RATIO, "Ratio of randomly chosen attributes to test", 0.0d, 1.0d,
				0.2d);
		type.registerDependencyCondition(new BooleanParameterCondition(this, PARAMETER_USE_HEURISTIC_SUBSET_RATION, false,
				false));
		type.setExpert(false);
		types.add(type);

		types.addAll(RandomGenerator.getRandomGeneratorParameters(this));

		return types;
	}
}
