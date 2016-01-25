/**
 * Copyright (C) 2001-2016 by RapidMiner and the contributors
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

import java.util.LinkedList;
import java.util.List;

import com.rapidminer.example.ExampleSet;
import com.rapidminer.operator.OperatorCapability;
import com.rapidminer.operator.OperatorDescription;
import com.rapidminer.operator.OperatorException;
import com.rapidminer.parameter.ParameterType;
import com.rapidminer.studio.internal.Resources;


/**
 * <p>
 * This operator learns decision trees from both nominal and numerical data. Decision trees are
 * powerful classification methods which often can also easily be understood. This decision tree
 * learner works similar to Quinlan's C4.5 or CART.
 * </p>
 *
 * <p>
 * The actual type of the tree is determined by the criterion, e.g. using gain_ratio or Gini for
 * CART / C4.5.
 * </p>
 *
 * @rapidminer.index C4.5
 * @rapidminer.index CART
 *
 * @author Sebastian Land, Ingo Mierswa, Gisa Schaefer
 */
public class ParallelDecisionTreeLearner extends AbstractParallelTreeLearner {

	public ParallelDecisionTreeLearner(OperatorDescription description) {
		super(description);
	}

	@Override
	public Pruner getPruner() throws OperatorException {
		if (getParameterAsBoolean(PARAMETER_PRUNING)) {
			return new TreebasedPessimisticPruner(getParameterAsDouble(PARAMETER_CONFIDENCE), null);
		} else {
			return null;
		}
	}

	@Override
	public List<ColumnTerminator> getTerminationCriteria(ExampleSet exampleSet) throws OperatorException {
		List<ColumnTerminator> result = new LinkedList<>();
		result.add(new ColumnSingleLabelTermination());
		result.add(new ColumnNoAttributeLeftTermination());
		result.add(new ColumnEmptyTermination());
		int maxDepth = getParameterAsInt(PARAMETER_MAXIMAL_DEPTH);
		if (maxDepth <= 0) {
			maxDepth = exampleSet.size();
		}
		result.add(new ColumnMaxDepthTermination(maxDepth));
		return result;
	}

	@Override
	public boolean supportsCapability(OperatorCapability capability) {
		switch (capability) {
			case BINOMINAL_ATTRIBUTES:
			case POLYNOMINAL_ATTRIBUTES:
			case NUMERICAL_ATTRIBUTES:
			case POLYNOMINAL_LABEL:
			case BINOMINAL_LABEL:
			case WEIGHTED_EXAMPLES:
			case MISSING_VALUES:
				return true;
			default:
				return false;
		}
	}

	@Override
	protected AbstractParallelTreeBuilder getTreeBuilder(ExampleSet exampleSet) throws OperatorException {
		if (Resources.getConcurrencyContext(this).getParallelism() > 1) {
			return new ConcurrentTreeBuilder(this, createCriterion(), getTerminationCriteria(exampleSet), getPruner(),
					getSplitPreprocessing(0), getParameterAsBoolean(PARAMETER_PRE_PRUNING),
					getParameterAsInt(PARAMETER_NUMBER_OF_PREPRUNING_ALTERNATIVES),
					getParameterAsInt(PARAMETER_MINIMAL_SIZE_FOR_SPLIT), getParameterAsInt(PARAMETER_MINIMAL_LEAF_SIZE));
		}
		return new NonParallelTreeBuilder(this, createCriterion(), getTerminationCriteria(exampleSet), getPruner(),
				getSplitPreprocessing(0), getParameterAsBoolean(PARAMETER_PRE_PRUNING),
				getParameterAsInt(PARAMETER_NUMBER_OF_PREPRUNING_ALTERNATIVES),
				getParameterAsInt(PARAMETER_MINIMAL_SIZE_FOR_SPLIT), getParameterAsInt(PARAMETER_MINIMAL_LEAF_SIZE));
	}

	@Override
	public List<ParameterType> getParameterTypes() {
		List<ParameterType> types = super.getParameterTypes();

		return types;

	}

}
