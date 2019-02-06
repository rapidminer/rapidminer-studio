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
package com.rapidminer.operator.learner.tree.criterions;

import static com.rapidminer.operator.learner.tree.AbstractTreeLearner.CRITERIA_CLASSES;
import static com.rapidminer.operator.learner.tree.AbstractTreeLearner.CRITERIA_NAMES;
import static com.rapidminer.operator.learner.tree.AbstractTreeLearner.PARAMETER_CRITERION;

import com.rapidminer.example.Attribute;
import com.rapidminer.example.Example;
import com.rapidminer.example.ExampleSet;
import com.rapidminer.operator.OperatorException;
import com.rapidminer.operator.learner.tree.FrequencyCalculator;
import com.rapidminer.operator.learner.tree.MinimalGainHandler;
import com.rapidminer.parameter.ParameterHandler;
import com.rapidminer.tools.Tools;


/**
 * This criterion class can be used for the incremental calculation of benefits.
 *
 * @author Sebastian Land
 */
public abstract class AbstractCriterion implements Criterion {

	// data for incremental calculation

	protected double leftWeight;
	protected double rightWeight;
	protected double totalWeight;
	protected double[] totalLabelWeights;
	protected double[] leftLabelWeights;
	protected double[] rightLabelWeights;
	protected Attribute labelAttribute;
	protected Attribute weightAttribute;

	@Override
	public boolean supportsIncrementalCalculation() {
		return false;
	}

	@Override
	public void startIncrementalCalculation(ExampleSet exampleSet) {
		FrequencyCalculator calculator = new FrequencyCalculator();
		rightLabelWeights = calculator.getLabelWeights(exampleSet);
		leftLabelWeights = new double[rightLabelWeights.length];
		totalLabelWeights = new double[rightLabelWeights.length];
		System.arraycopy(rightLabelWeights, 0, totalLabelWeights, 0, rightLabelWeights.length);
		leftWeight = 0;
		rightWeight = calculator.getTotalWeight(totalLabelWeights);
		totalWeight = rightWeight;

		labelAttribute = exampleSet.getAttributes().getLabel();
		weightAttribute = exampleSet.getAttributes().getWeight();
	}

	@Override
	public void swapExample(Example example) {
		double weight = 1;
		if (weightAttribute != null) {
			weight = example.getValue(weightAttribute);
		}
		int label = (int) example.getValue(labelAttribute);
		leftWeight += weight;
		rightWeight -= weight;
		leftLabelWeights[label] += weight;
		rightLabelWeights[label] -= weight;
	}

	@Override
	public double getIncrementalBenefit() {
		return 0;
	}

	/**
	 * This method returns the criterion specified by the respective parameters.
	 */
	public static Criterion createCriterion(ParameterHandler handler, double minimalGain) throws OperatorException {
		String criterionName = handler.getParameterAsString(PARAMETER_CRITERION);
		Class<?> criterionClass = null;
		for (int i = 0; i < CRITERIA_NAMES.length; i++) {
			if (CRITERIA_NAMES[i].equals(criterionName)) {
				criterionClass = CRITERIA_CLASSES[i];
			}
		}

		if (criterionClass == null && criterionName != null) {
			try {
				criterionClass = Tools.classForName(criterionName);
			} catch (ClassNotFoundException e) {
				throw new OperatorException("Cannot find criterion '" + criterionName
						+ "' and cannot instantiate a class with this name.");
			}
		}

		if (criterionClass != null) {
			try {
				Criterion criterion = (Criterion) criterionClass.newInstance();
				if (criterion instanceof MinimalGainHandler) {
					((MinimalGainHandler) criterion).setMinimalGain(minimalGain);
				}
				return criterion;
			} catch (InstantiationException e) {
				throw new OperatorException("Cannot instantiate criterion class '" + criterionClass.getName() + "'.");
			} catch (IllegalAccessException e) {
				throw new OperatorException("Cannot access criterion class '" + criterionClass.getName() + "'.");
			}
		} else {
			throw new OperatorException("No relevance criterion defined.");
		}
	}

}
