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

import com.rapidminer.example.ExampleSet;
import com.rapidminer.operator.GroupedModel;
import com.rapidminer.operator.Model;
import com.rapidminer.operator.OperatorCapability;
import com.rapidminer.operator.OperatorCreationException;
import com.rapidminer.operator.OperatorDescription;
import com.rapidminer.operator.OperatorException;
import com.rapidminer.operator.UserError;
import com.rapidminer.operator.learner.meta.AbstractMetaLearner;
import com.rapidminer.operator.preprocessing.PreprocessingOperator;
import com.rapidminer.operator.preprocessing.discretization.UserBasedDiscretization;
import com.rapidminer.operator.preprocessing.filter.attributes.RegexpAttributeFilter;
import com.rapidminer.operator.tools.AttributeSubsetSelector;
import com.rapidminer.parameter.ParameterTypeList;
import com.rapidminer.tools.OperatorService;
import com.rapidminer.tools.Tools;
import com.rapidminer.tools.container.Pair;

import java.util.Arrays;
import java.util.HashMap;
import java.util.Iterator;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;


/**
 * This operator is a meta learner for numerical tree builder. It might be used to flatten decision
 * trees, which consists of many splits on the same attribute. All numerical attributes used for at
 * least one decision in a tree will be discretized with the decisions' split points as borders. For
 * example, if attribute att1 is splitted on the points 4.5 and 2.1 then it will be discretized in
 * three values: -Infinity to 2.1, 2.1 to 4.5 and 4.5 to Infinity. After this, a new tree is grown
 * on the transformed data. Since the used attributes are now numerical, all splits will be made
 * immediately and hence the depth might be reduced. Please note: The resulting tree might be easier
 * to comprehend, but this have to make it perform neither better nor worse! To get an impression of
 * the reliability of the result perform a XValidation.
 * 
 * @author Sebastian Land
 */
public class MultiwayDecisionTree extends AbstractMetaLearner {

	/**
	 * @param description
	 */
	public MultiwayDecisionTree(OperatorDescription description) {
		super(description);

	}

	@Override
	public Model learn(ExampleSet exampleSet) throws OperatorException {
		GroupedModel groupedModel = new GroupedModel();

		// applying inner learner in order to get tree model
		TreeModel model = (TreeModel) applyInnerLearner(exampleSet);

		// searching numerical split point inside this tree model
		Map<String, List<Double>> attributePointMap = new HashMap<String, List<Double>>();
		addNodeSplitPoints(model.getRoot(), attributePointMap);

		try {
			// operator construction
			PreprocessingOperator userbasedDiscretization = OperatorService.createOperator(UserBasedDiscretization.class);

			// setting common parameters
			userbasedDiscretization.setParameter(AttributeSubsetSelector.PARAMETER_FILTER_TYPE,
					AttributeSubsetSelector.CONDITION_REGULAR_EXPRESSION + "");
			userbasedDiscretization.setParameter(PreprocessingOperator.PARAMETER_RETURN_PREPROCESSING_MODEL, "true");

			// iterating over all attributes which have to be transformed
			for (Entry<String, List<Double>> currentEntry : attributePointMap.entrySet()) {
				// sorting split points
				double[] splitPoints = new double[currentEntry.getValue().size()];
				int i = 0;
				String currentAttributeName = currentEntry.getKey();
				for (Double splitPoint : attributePointMap.get(currentAttributeName)) {
					splitPoints[i] = splitPoint;
					i++;
				}
				Arrays.sort(splitPoints);

				// setting attribute to consider
				userbasedDiscretization.setParameter(RegexpAttributeFilter.PARAMETER_REGULAR_EXPRESSION,
						currentAttributeName);

				// setting borders for splitting
				List<String[]> borders = new LinkedList<String[]>();
				double lowerBorder = Double.NEGATIVE_INFINITY;
				for (i = 0; i < splitPoints.length; i++) {
					String[] pointSpecifier = new String[] {
							currentEntry + " in " + Tools.formatNumber(lowerBorder, 2) + " to "
									+ Tools.formatNumber(splitPoints[i], 2), splitPoints[i] + "" };
					lowerBorder = splitPoints[i];
					borders.add(pointSpecifier);
				}
				String[] pointSpecifier = new String[] {
						currentEntry + " in " + Tools.formatNumber(lowerBorder, 2) + " to "
								+ Tools.formatNumber(Double.POSITIVE_INFINITY, 2), "Infinity" };
				borders.add(pointSpecifier);

				userbasedDiscretization.setParameter(UserBasedDiscretization.PARAMETER_RANGE_NAMES,
						ParameterTypeList.transformList2String(borders));

				// executing operators

				Pair<ExampleSet, Model> result = userbasedDiscretization.doWorkModel(exampleSet);
				exampleSet = result.getFirst();
				groupedModel.addModel(result.getSecond());
			}

			// now apply inner operator on transformed exampleset
			groupedModel.addModel(applyInnerLearner(exampleSet));
			return groupedModel;
		} catch (OperatorCreationException e) {
			throw new UserError(this, 904, "operators", e.getMessage());
		}
	}

	/**
	 * This method recursively adds splitting points from all numerical split conditions.
	 */
	private void addNodeSplitPoints(Tree root, Map<String, List<Double>> attributePointMap) {
		// add split point of this node
		if (!root.isLeaf()) {
			SplitCondition condition = root.childIterator().next().getCondition();
			if (condition instanceof GreaterSplitCondition) {
				addSplit(condition.getAttributeName(), ((GreaterSplitCondition) condition).getValue(), attributePointMap);
			} else if (condition instanceof LessEqualsSplitCondition) {
				addSplit(condition.getAttributeName(), ((LessEqualsSplitCondition) condition).getValue(), attributePointMap);
			}
		}

		// recursive descent
		Iterator<Edge> iterator = root.childIterator();
		while (iterator.hasNext()) {
			addNodeSplitPoints(iterator.next().getChild(), attributePointMap);
		}
	}

	private void addSplit(String attributeName, double value, Map<String, List<Double>> attributePointMap) {
		List<Double> valueList = attributePointMap.get(attributeName);
		if (valueList == null) {
			valueList = new LinkedList<Double>();
			attributePointMap.put(attributeName, valueList);
		}
		valueList.add(value);
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

}
