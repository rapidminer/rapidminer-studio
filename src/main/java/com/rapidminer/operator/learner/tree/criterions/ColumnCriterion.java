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

import com.rapidminer.operator.learner.tree.ColumnExampleTable;


/**
 * The criterion for a splitting the selected examples based on a {@link ColumnExampleTable} and a
 * selection. Possible implementations are for example accuracy or information gain. The calculation
 * can be done in parallel.
 *
 * @author Sebastian Land, Ingo Mierswa, Gisa Schaefer
 * @since 6.2.000
 */
public interface ColumnCriterion {

	/**
	 * Calculates the benefit for splitting the current selection of the columnTable at the nominal
	 * attribute represented by the attributeNumber
	 *
	 * @param columnTable
	 * @param selection
	 * @param attributeNumber
	 * @return
	 */
	public double getNominalBenefit(ColumnExampleTable columnTable, int[] selection, int attributeNumber);

	/**
	 * Calculates the benefit for splitting the current selection of the columnTable at the split
	 * value of the numerical attribute represented by the attributeNumber
	 *
	 * @param columnTable
	 * @param selection
	 * @param attributeNumber
	 * @param splitValue
	 * @return
	 */
	public double getNumericalBenefit(ColumnExampleTable columnTable, int[] selection, int attributeNumber, double splitValue);

	/**
	 * @return <code>true</code> if the benefit can be calculated out of a
	 *         {@link WeightDistribution}.
	 */
	public boolean supportsIncrementalCalculation();

	/**
	 * Calculates the {@link WeightDistribution} for at the beginning of the incrental calculation.
	 *
	 * @param columnTable
	 * @param selection
	 * @param attributeNumber
	 * @return
	 */
	public WeightDistribution startIncrementalCalculation(ColumnExampleTable columnTable, int[] selection,
			int attributeNumber);

	/**
	 * Updates the weight distribution when going to the next example.
	 *
	 * @param columnTable
	 * @param row
	 * @param distribution
	 */
	public void updateWeightDistribution(ColumnExampleTable columnTable, int row, WeightDistribution distribution);

	/**
	 * Calculates the benefit out of the distribution.
	 *
	 * @param distribution
	 * @return
	 */
	public double getIncrementalBenefit(WeightDistribution distribution);

	/**
	 * This method will return the calculated benefit if the weights would have distributed over the
	 * labels as given. The first index specifies the split fraction, the second index the label.
	 * For splits on nominal attributes, the number of split sides is determined by the number of
	 * possible values. For splits on numerical attributes, there are 2 split sides if there are no
	 * missing values, 3 otherwise.
	 */
	public double getBenefit(double[][] weightCounts);
}
