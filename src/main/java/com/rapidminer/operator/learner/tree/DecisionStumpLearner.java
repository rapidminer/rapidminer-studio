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

import com.rapidminer.example.ExampleSet;
import com.rapidminer.operator.OperatorCapability;
import com.rapidminer.operator.OperatorDescription;
import com.rapidminer.operator.OperatorException;
import com.rapidminer.parameter.ParameterType;

import java.util.LinkedList;
import java.util.List;


/**
 * This operator learns decision stumps, i.e. a small decision tree with only one single split. This
 * decision stump works on both numerical and nominal attributes.
 * 
 * @author Ingo Mierswa
 */
public class DecisionStumpLearner extends AbstractTreeLearner {

	public DecisionStumpLearner(OperatorDescription description) {
		super(description);
	}

	@Override
	public Pruner getPruner() throws OperatorException {
		return null;
	}

	@Override
	public List<Terminator> getTerminationCriteria(ExampleSet exampleSet) {
		List<Terminator> result = new LinkedList<Terminator>();
		result.add(new SingleLabelTermination());
		result.add(new NoAttributeLeftTermination());
		result.add(new EmptyTermination());
		result.add(new MaxDepthTermination(2));
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
	public List<ParameterType> getParameterTypes() {
		List<ParameterType> types = new LinkedList<ParameterType>();
		for (ParameterType type : super.getParameterTypes()) {
			if (type.getKey().equals(PARAMETER_MINIMAL_LEAF_SIZE)) {
				type.setDefaultValue(Integer.valueOf(1));
			}

			if (!type.getKey().equals(PARAMETER_MINIMAL_GAIN) && !type.getKey().equals(PARAMETER_MINIMAL_SIZE_FOR_SPLIT)) {
				types.add(type);
			}
		}
		return types;
	}

	@Override
	protected TreeBuilder getTreeBuilder(ExampleSet exampleSet) throws OperatorException {
		return new TreeBuilder(createCriterion(0.0), getTerminationCriteria(exampleSet), getPruner(),
				getSplitPreprocessing(), new DecisionTreeLeafCreator(), true, 0, 0,
				getParameterAsInt(PARAMETER_MINIMAL_LEAF_SIZE));
	}
}
