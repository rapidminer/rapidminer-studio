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
package com.rapidminer.operator.performance.cost;

import java.util.HashMap;

import com.rapidminer.example.Attribute;
import com.rapidminer.example.Attributes;
import com.rapidminer.example.Example;
import com.rapidminer.example.ExampleSet;
import com.rapidminer.operator.performance.MeasuredPerformance;
import com.rapidminer.tools.math.Averagable;


/**
 * This performance Criterion works with given ranking costs. If the real class is on the x-th rank
 * of confidences, the costs given for that rank are added.
 *
 * @author Sebastian Land
 */
public class RankingCriterion extends MeasuredPerformance {

	private static final long serialVersionUID = -7466139591781285005L;

	private double costs;
	private Attribute label;
	private Attribute[] confidenceAttributes;
	private double exampleCount;
	private int[] rankIntervallStarts;
	private double[] rankIntervallCost;
	private HashMap<String, Integer> confidenceAttributesMap = new HashMap<String, Integer>();

	public RankingCriterion(int[] rankIntervallStarts, double[] rankIntervallCost, ExampleSet exampleSet) {
		label = exampleSet.getAttributes().getLabel();
		this.rankIntervallStarts = rankIntervallStarts;
		this.rankIntervallCost = rankIntervallCost;
		confidenceAttributes = new Attribute[label.getMapping().size()];
		int i = 0;
		for (String labelValue : label.getMapping().getValues()) {
			confidenceAttributes[i] = exampleSet.getAttributes().getSpecial(Attributes.CONFIDENCE_NAME + "_" + labelValue);
			confidenceAttributesMap.put(labelValue, i);
			i++;
		}
		costs = 0;
	}

	@Override
	public String getDescription() {
		return "This Criterion delievers the ranking costs";
	}

	@Override
	public String getName() {
		return "RankingCosts";
	}

	@Override
	public void countExample(Example example) {
		// finding current rank
		int indexOfCorrect = confidenceAttributesMap.get(example.getNominalValue(label));
		double confidenceOfCorrect = example.getValue(confidenceAttributes[indexOfCorrect]);
		int rank = 0;
		for (int i = 0; i < confidenceAttributes.length; i++) {
			double currentConfidence = example.getValue(confidenceAttributes[i]);
			if (currentConfidence > confidenceOfCorrect) {
				rank++;
			}
		}

		// getting costs for rank
		int intervallIndex = 0;
		while (intervallIndex < rankIntervallStarts.length - 1 && rankIntervallStarts[intervallIndex + 1] <= rank) {
			intervallIndex++;
		}
		if (rank >= rankIntervallStarts[0]) {
			// otherwise not defined costs: Assume 0
			costs += rankIntervallCost[intervallIndex];
		}
		exampleCount++;
	}

	@Override
	public double getExampleCount() {
		return exampleCount;
	}

	@Override
	public double getFitness() {
		return -costs;
	}

	@Override
	protected void buildSingleAverage(Averagable averagable) {}

	@Override
	public double getMikroAverage() {
		return costs / exampleCount;
	}

	@Override
	public double getMikroVariance() {
		return 0;
	}

}
