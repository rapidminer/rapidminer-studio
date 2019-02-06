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
import com.rapidminer.example.Example;
import com.rapidminer.example.ExampleSet;
import com.rapidminer.example.set.SortedExampleSet;


/**
 * Find the best split point for numerical attributes according to accuracy.
 * 
 * @author Ingo Mierswa
 */
public class NumericalSplitter {

	private Criterion criterion;

	private double minValue = 0.5d;

	public NumericalSplitter() {
		this.criterion = new AccuracyCriterion();
	}

	public NumericalSplitter(Criterion criterion) {
		this.criterion = criterion;
	}

	public Split getBestSplit(ExampleSet inputSet, Attribute attribute, String labelName) {
		SortedExampleSet exampleSet = new SortedExampleSet(inputSet, attribute, SortedExampleSet.INCREASING);

		Attribute labelAttribute = exampleSet.getAttributes().getLabel();
		int labelIndex = labelAttribute.getMapping().mapString(labelName);

		double oldLabel = Double.NaN;
		double bestSplit = Double.NaN;
		double lastValue = Double.NaN;
		double bestBenefit = Double.NEGATIVE_INFINITY;
		double bestTotalWeight = 0;
		int bestSplitType = Split.LESS_SPLIT;

		// initiating online counting of benefit: only 2 Datascans needed then
		criterion.reinitOnlineCounting(exampleSet);
		for (Example e : exampleSet) {
			double currentValue = e.getValue(attribute);
			double label = e.getValue(labelAttribute);
			if ((Double.isNaN(oldLabel)) || (oldLabel != label) && (lastValue != currentValue)) {
				double splitValue = (lastValue + currentValue) / 2.0d;

				double[] benefits;
				if (labelName == null) {
					benefits = criterion.getOnlineBenefit(e);
				} else {
					benefits = criterion.getOnlineBenefit(e, labelIndex);
				}
				// online method returns both possible relations in one array(greater / smaller) in
				// one array
				if ((benefits[0] > minValue)
						&& (benefits[0] > 0)
						&& (benefits[1] > 0)
						&& ((benefits[0] > bestBenefit) || ((benefits[0] == bestBenefit) && (benefits[1] > bestTotalWeight)))) {
					bestBenefit = benefits[0];
					bestSplit = splitValue;
					bestTotalWeight = benefits[1];
					bestSplitType = Split.LESS_SPLIT;
				}
				if ((benefits[2] > minValue)
						&& (benefits[2] > 0)
						&& (benefits[3] > 0)
						&& ((benefits[2] > bestBenefit) || ((benefits[2] == bestBenefit) && (benefits[3] > bestTotalWeight)))) {
					bestBenefit = benefits[2];
					bestSplit = splitValue;
					bestTotalWeight = benefits[3];
					bestSplitType = Split.GREATER_SPLIT;
				}
				oldLabel = label;
			}
			lastValue = currentValue;
			criterion.update(e);
		}
		return new Split(bestSplit, new double[] { bestBenefit, bestTotalWeight }, bestSplitType);
	}
}
