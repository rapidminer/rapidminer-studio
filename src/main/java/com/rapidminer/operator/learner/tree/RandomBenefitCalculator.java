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
import java.util.Map;

import com.rapidminer.operator.Operator;
import com.rapidminer.operator.OperatorException;
import com.rapidminer.operator.learner.tree.criterions.ColumnCriterion;


/**
 * Used to calculate the benefit for splitting at a certain attribute where numerical attributes are split randomly.
 * This benefit calculator does not support a parallel calculation in order to yield reproducible results. Therefore,
 * the parallel caluclation method is overwritten by the sequential one.
 *
 * @author Gisa Meier
 * @since 8.0
 */
public class RandomBenefitCalculator extends BenefitCalculator {


	public RandomBenefitCalculator(ColumnExampleTable columnTable, ColumnCriterion criterion, Operator operator,
			int seed) {
		super(columnTable, criterion, operator, new ColumnNumericalRandomSplitter(columnTable, criterion, seed));
	}

	@Override
	public List<ParallelBenefit> calculateAllBenefitsParallel(final Map<Integer, int[]> allSelectedExamples,
			final int[] selectedAttributes) throws OperatorException {
		// not done in parallel to be reproducible
		return calculateAllBenefits(allSelectedExamples, selectedAttributes);
	}

}
