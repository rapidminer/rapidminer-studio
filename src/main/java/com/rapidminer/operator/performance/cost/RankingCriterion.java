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
package com.rapidminer.operator.performance.cost;

import java.util.HashMap;

import com.rapidminer.example.Attribute;
import com.rapidminer.example.Attributes;
import com.rapidminer.example.Example;
import com.rapidminer.example.ExampleSet;
import com.rapidminer.operator.OperatorVersion;
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

	/**
	 * Behaves like the pre 9.0.3
	 * @since 9.0.3
	 */
	private static final int BROKEN_FITNESS = 0;
	/**
	 * Reserved for future use
	 * @since 9.0.3
	 */
	private static final int AFTER_BROKEN_FITNESS = 1;
	/**
	 * If the version field is missing from a serialized object, the default value is used (0 = BROKEN_FITNESS)
	 * @since 9.0.3
	 */
	private int version = AFTER_BROKEN_FITNESS;

	private double costs;
	private Attribute label;
	private Attribute[] confidenceAttributes;
	private double exampleCount;
	private int[] rankIntervallStarts;
	private double[] rankIntervallCost;
	private HashMap<String, Integer> confidenceAttributesMap = new HashMap<String, Integer>();

	/**
	 * Clone Constructor
	 * @since 9.0.3
	 */
	public RankingCriterion(RankingCriterion other) {
		super(other);
		this.costs = other.costs;
		if (other.label != null) {
			this.label = (Attribute) other.label.clone();
		}
		if (other.confidenceAttributes != null) {
			this.confidenceAttributes = new Attribute[other.confidenceAttributes.length];
			for (int i = 0; i < other.confidenceAttributes.length; i++) {
				if (other.confidenceAttributes[i] != null) {
					this.confidenceAttributes[i] = (Attribute) other.confidenceAttributes[i].clone();
				} else {
					this.confidenceAttributes[i] = null;
				}
			}
		}
		this.exampleCount = other.exampleCount;
		this.rankIntervallStarts = other.rankIntervallStarts != null ? other.rankIntervallStarts.clone() : null;
		this.rankIntervallCost = other.rankIntervallCost != null ? other.rankIntervallCost.clone() : null;
		this.confidenceAttributesMap = other.confidenceAttributesMap != null ? new HashMap<>(other.confidenceAttributesMap) : null;
		this.version = other.version;
	}

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
		if (rankIntervallStarts.length > 0 && rank >= rankIntervallStarts[0]) {
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
		if (version == BROKEN_FITNESS) {
			return -costs;
		} else {
			return -getMikroAverage();
		}
	}

	@Override
	protected void buildSingleAverage(Averagable averagable) {
		if (version != BROKEN_FITNESS) {
			RankingCriterion criterion = (RankingCriterion) averagable;
			this.costs += criterion.costs;
			this.exampleCount += criterion.exampleCount;
		}
	}

	@Override
	public double getMikroAverage() {
		return costs / exampleCount;
	}

	@Override
	public double getMikroVariance() {
		if (version == BROKEN_FITNESS) {
			return 0;
		} else {
			return Double.NaN;
		}
	}

	/**
	 * Makes this criterion behave like the given version
	 *
	 * @param compatibilityLevel
	 * 		The compatibility level
	 * @since 9.0.3
	 */
	public void setVersion(OperatorVersion compatibilityLevel) {
		if (compatibilityLevel.isAtMost(RankingEvaluator.WRONG_FITNESS)) {
			this.version = BROKEN_FITNESS;
		} else {
			this.version = AFTER_BROKEN_FITNESS;
		}
	}

}
