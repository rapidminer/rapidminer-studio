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
package com.rapidminer.operator.learner.subgroups.utility;

import com.rapidminer.operator.learner.subgroups.hypothesis.Hypothesis;
import com.rapidminer.operator.learner.subgroups.hypothesis.Rule;


/**
 * Calculates the odds ratio.
 * 
 * @author Tobias Malbrecht
 */
public class OddsRatio extends UtilityFunction {

	/**
	 * 
	 */
	private static final long serialVersionUID = 1L;

	public OddsRatio(double totalWeight, double totalPositiveWeight) {
		super(totalWeight, totalPositiveWeight);
	}

	@Override
	public double utility(Rule rule) {
		double totalPredictionWeight = rule.predictsPositive() ? totalPositiveWeight : totalNegativeWeight;
		double nominator = rule.getPredictionWeight()
				* (totalWeight - totalPredictionWeight - rule.getCoveredWeight() + rule.getPredictionWeight());
		double denominator = (totalWeight - rule.getCoveredWeight()) * (totalWeight - totalPredictionWeight);
		return nominator / denominator;
	}

	@Override
	public double optimisticEstimate(Hypothesis hypothesis) {
		return Integer.MAX_VALUE;
	}

	@Override
	public String getName() {
		return "Odds Ratio";
	}

	@Override
	public String getAbbreviation() {
		return "OR";
	}
}
