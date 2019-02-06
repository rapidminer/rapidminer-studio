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

import java.util.Random;

import com.rapidminer.operator.learner.tree.criterions.ColumnCriterion;
import com.rapidminer.tools.Tools;


/**
 * Random splitter for numerical attributes.
 *
 * @author Gisa Meier
 * @since 8.0
 */
public class ColumnNumericalRandomSplitter extends ColumnNumericalSplitter {

	private final ColumnCriterion criterion;
	private final ColumnExampleTable columnTable;
	private final Random random;

	public ColumnNumericalRandomSplitter(ColumnExampleTable columnTable, ColumnCriterion criterion, int seed) {
		super(columnTable, criterion);
		this.criterion = criterion;
		this.columnTable = columnTable;
		random = new Random(seed);
	}

	/**
	 * Randomly Splits at the given attribute number.
	 */
	@Override
	public ParallelBenefit getBestSplitBenefit(int[] selectedExamples, int attributeNumber) {
		// find min and max (attribute column is sorted wrt selected examples)
		double[] attributeValues = columnTable.getNumericalAttributeColumn(attributeNumber);

		double min = attributeValues[selectedExamples[0]];
		if (Double.isNaN(min)) {
			// all values are NaN
			return null;
		}

		int lastNonNan = selectedExamples.length - 1;
		double max = attributeValues[selectedExamples[lastNonNan]];
		while (Double.isNaN(max) && lastNonNan > 0) {
			lastNonNan--;
			max = attributeValues[selectedExamples[lastNonNan]];
		}

		if (Tools.isEqual(min, max) && lastNonNan == selectedExamples.length - 1) {
			// all values are the same
			return null;
		}

		// draw split value uniformly in [min,max]
		double splitValue = random.nextDouble() * (max - min) + min;

		double benefit = this.criterion.getNumericalBenefit(columnTable, selectedExamples, attributeNumber,
				splitValue);

		if (Double.isNaN(benefit)) {
			return null;
		}

		return new ParallelBenefit(benefit, attributeNumber, splitValue);
	}

}
