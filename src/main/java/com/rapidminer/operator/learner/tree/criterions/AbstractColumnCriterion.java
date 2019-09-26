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

import static com.rapidminer.operator.learner.tree.AbstractParallelTreeLearner.CRITERIA_CLASSES;
import static com.rapidminer.operator.learner.tree.AbstractParallelTreeLearner.CRITERIA_NAMES;
import static com.rapidminer.operator.learner.tree.AbstractParallelTreeLearner.PARAMETER_CRITERION;

import com.rapidminer.operator.OperatorException;
import com.rapidminer.operator.learner.tree.ColumnExampleTable;
import com.rapidminer.operator.learner.tree.ColumnFrequencyCalculator;
import com.rapidminer.operator.learner.tree.MinimalGainHandler;
import com.rapidminer.parameter.ParameterHandler;
import com.rapidminer.tools.Tools;


/**
 * This criterion class can be used for the incremental calculation of benefits.
 *
 * @author Sebastian Land, Gisa Schaefer
 */
public abstract class AbstractColumnCriterion implements ColumnCriterion {

	@Override
	public boolean supportsIncrementalCalculation() {
		return false;
	}

	@Override
	public WeightDistribution startIncrementalCalculation(ColumnExampleTable columnTable, int[] selection,
			int numericalAttributeNumber) {
		return new WeightDistribution(columnTable, selection, numericalAttributeNumber);
	}

	@Override
	public void updateWeightDistribution(ColumnExampleTable columnTable, int row, WeightDistribution distribution) {
		double weight = 1;
		if (columnTable.getWeight() != null) {
			weight = columnTable.getWeightColumn()[row];
		}
		int label = columnTable.getLabelColumn()[row];
		distribution.increment(label, weight);
	}

	@Override
	public double getNominalBenefit(ColumnExampleTable columnTable, int[] selection, int attributeNumber) {
		double[][] weightCounts = ColumnFrequencyCalculator.getNominalWeightCounts(columnTable, selection, attributeNumber);
		return getBenefit(weightCounts);
	}

	@Override
	public double getNumericalBenefit(ColumnExampleTable columnTable, int[] selection, int attributeNumber, double splitValue) {
		double[][] weightCounts = ColumnFrequencyCalculator.getNumericalWeightCounts(columnTable, selection,
				attributeNumber, splitValue);
		return getBenefit(weightCounts);
	}


	/**
	 * This method returns the criterion specified by the respective parameters.
	 */
	public static ColumnCriterion createColumnCriterion(ParameterHandler handler, double minimalGain)
			throws OperatorException {
		return createColumnCriterion(handler, minimalGain, CRITERIA_CLASSES, CRITERIA_NAMES);
	}


	/**
	 * This method returns the criterion specified by the respective parameters.
	 *
	 * @param handler
	 * 		the handler for which to construct this
	 * @param minimalGain
	 * 		the minimal gain
	 * @param criteriaClasses
	 * 		the possible criteria classes to use
	 * @param criteriaNames
	 * 		the names for the criteria classes
	 * @return the matching criterion
	 * @throws OperatorException
	 * 		it the criterion could not be created
	 * @since 9.4.1
	 */
	public static ColumnCriterion createColumnCriterion(ParameterHandler handler, double minimalGain,
														Class<?>[] criteriaClasses, String[] criteriaNames) throws OperatorException {
		String criterionName = handler.getParameterAsString(PARAMETER_CRITERION);
		Class<?> criterionClass = null;
		for (int i = 0; i < criteriaClasses.length; i++) {
			if (criteriaNames[i].equals(criterionName)) {
				criterionClass = criteriaClasses[i];
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
				ColumnCriterion criterion = (ColumnCriterion) criterionClass.newInstance();
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
