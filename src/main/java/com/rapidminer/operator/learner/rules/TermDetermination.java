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
package com.rapidminer.operator.learner.rules;

import com.rapidminer.example.Attribute;
import com.rapidminer.example.ExampleSet;
import com.rapidminer.example.set.SplittedExampleSet;
import com.rapidminer.operator.learner.tree.GreaterSplitCondition;
import com.rapidminer.operator.learner.tree.LessEqualsSplitCondition;
import com.rapidminer.operator.learner.tree.NominalSplitCondition;
import com.rapidminer.operator.learner.tree.SplitCondition;


/**
 * Determines the best term for the given example set with respect to the criterion.
 *
 * @author Sebastian Land, Ingo Mierswa
 */
public class TermDetermination {

	private Criterion criterion;

	private NumericalSplitter splitter;

	private double minValue;

	public TermDetermination(Criterion criterion) {
		this(criterion, Double.NEGATIVE_INFINITY);
	}

	public TermDetermination(Criterion criterion, double minValue) {
		this.criterion = criterion;
		splitter = new NumericalSplitter(criterion);
		this.minValue = minValue;
	}

	public SplitCondition getBestTerm(ExampleSet exampleSet, String labelName) {
		SplitCondition bestCondition = null;
		double bestBenefit = Double.NEGATIVE_INFINITY;
		double bestTotalWeight = 0;
		for (Attribute attribute : exampleSet.getAttributes()) {
			if (attribute.isNominal()) {
				SplittedExampleSet splitted = SplittedExampleSet.splitByAttribute(exampleSet, attribute);
				SplittedExampleSet posSet = new SplittedExampleSet(splitted);
				SplittedExampleSet negSet = splitted;
				for (int i = 0; i < splitted.getNumberOfSubsets(); i++) {
					posSet.selectSingleSubset(i);
					negSet.selectAllSubsetsBut(i);
					double[] benefits = this.criterion.getBenefit(posSet, negSet, labelName);
					if (benefits[0] > minValue && benefits[0] > 0 && benefits[1] > 0
							&& (benefits[0] > bestBenefit || benefits[0] == bestBenefit && benefits[1] > bestTotalWeight)) {
						bestBenefit = benefits[0];
						bestTotalWeight = benefits[1];
						bestCondition = new NominalSplitCondition(attribute,
								posSet.iterator().next().getValueAsString(attribute));
					}
				}
			} else {
				Split bestSplit = splitter.getBestSplit(exampleSet, attribute, labelName);
				double bestSplitValue = bestSplit.getSplitPoint();
				if (!Double.isNaN(bestSplitValue)) {
					double[] benefits = bestSplit.getBenefit();
					if (benefits[0] > minValue && benefits[0] > 0 && benefits[1] > 0
							&& (benefits[0] > bestBenefit || benefits[0] == bestBenefit && benefits[1] > bestTotalWeight)) {
						bestBenefit = benefits[0];
						bestTotalWeight = benefits[1];
						if (bestSplit.getSplitType() == Split.LESS_SPLIT) {
							bestCondition = new LessEqualsSplitCondition(attribute, bestSplitValue);
						} else {
							bestCondition = new GreaterSplitCondition(attribute, bestSplitValue);
						}
					}
				}
			}
		}
		return bestCondition;
	}
}
