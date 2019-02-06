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

import java.util.LinkedList;
import java.util.List;

import com.rapidminer.example.ExampleSet;
import com.rapidminer.operator.OperatorCapability;
import com.rapidminer.operator.OperatorDescription;
import com.rapidminer.operator.OperatorException;


/**
 * This operator learns decision trees without pruning using both nominal and numerical attributes.
 * Decision trees are powerful classification methods which often can also easily be understood.
 * This decision tree learner works similar to Quinlan's ID3.
 *
 * @author Ingo Mierswa
 *
 * @deprecated This learner is not used anymore.
 */
@Deprecated
public class ID3NumericalLearner extends AbstractTreeLearner {

	public ID3NumericalLearner(OperatorDescription description) {
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
		result.add(new MaxDepthTermination(exampleSet.size()));
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
	protected TreeBuilder getTreeBuilder(ExampleSet exampleSet) throws OperatorException {
		return new TreeBuilder(createCriterion(getParameterAsDouble(PARAMETER_MINIMAL_GAIN)),
				getTerminationCriteria(exampleSet), getPruner(), getSplitPreprocessing(), new DecisionTreeLeafCreator(),
				true, 0, getParameterAsInt(PARAMETER_MINIMAL_SIZE_FOR_SPLIT), getParameterAsInt(PARAMETER_MINIMAL_LEAF_SIZE));
	}
}
