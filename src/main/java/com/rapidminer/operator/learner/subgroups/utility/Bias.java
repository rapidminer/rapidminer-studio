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
 * Calculates the bias.
 * 
 * @author Tobias Malbrecht
 */
public class Bias extends UtilityFunction {

	private static final long serialVersionUID = 1L;

	public Bias(double totalWeight, double totalPositiveWeight) {
		super(totalWeight, totalPositiveWeight);
	}

	@Override
	public double utility(Rule rule) {
		return rule.getPredictionWeight() / rule.getCoveredWeight()
				- priors[rule.predictsPositive() ? POSITIVE_CLASS : NEGATIVE_CLASS];
	}

	@Override
	public double optimisticEstimate(Hypothesis hypothesis) {
		return 1;
	}

	@Override
	public String getName() {
		return "Bias";
	}

	@Override
	public String getAbbreviation() {
		return "Bias";
	}

}
