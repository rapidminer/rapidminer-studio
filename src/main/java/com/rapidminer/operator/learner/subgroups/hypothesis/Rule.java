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
package com.rapidminer.operator.learner.subgroups.hypothesis;

import java.io.Serializable;
import java.util.Collection;
import java.util.LinkedHashMap;

import com.rapidminer.example.Example;
import com.rapidminer.operator.learner.subgroups.utility.UtilityFunction;
import com.rapidminer.tools.Tools;


/**
 * A rule for subgroup discovery.
 * 
 * @author Tobias Malbrecht
 */
public class Rule implements Serializable {

	private static final long serialVersionUID = 1L;

	Hypothesis hypothesis;

	Literal prediction;

	LinkedHashMap<UtilityFunction, Double> utilityMap = new LinkedHashMap<>();

	public Rule(Hypothesis hypothesis, Literal prediction) {
		this.hypothesis = hypothesis;
		this.prediction = prediction;
	}

	public boolean applicable(Example example) {
		return hypothesis.applicable(example);
	}

	public double getCoveredWeight() {
		return hypothesis.getCoveredWeight();
	}

	public double getPositiveWeight() {
		return hypothesis.getPositiveWeight();
	}

	public double getNegativeWeight() {
		return hypothesis.getCoveredWeight() - hypothesis.getPositiveWeight();
	}

	public double getPredictionWeight() {
		if (predictsPositive()) {
			return getPositiveWeight();
		} else {
			return getNegativeWeight();
		}
	}

	public boolean predictsPositive() {
		return prediction.getValue() == prediction.getAttribute().getMapping().getPositiveIndex();
	}

	public double getPrediction() {
		return prediction.getValue();
	}

	public Hypothesis getHypothesis() {
		return hypothesis;
	}

	public void setUtility(UtilityFunction function, double utility) {
		utilityMap.put(function, utility);
	}

	public double getUtility(Class<? extends UtilityFunction> functionClass) {
		for (UtilityFunction function : utilityMap.keySet()) {
			if (function.getClass().equals(functionClass)) {
				return utilityMap.get(function);
			}
		}
		return Double.NaN;
	}

	public UtilityFunction getUtilityFunction(Class<? extends UtilityFunction> functionClass) {
		for (UtilityFunction function : utilityMap.keySet()) {
			if (function.getClass().equals(functionClass)) {
				return function;
			}
		}
		return null;
	}

	public Collection<UtilityFunction> getUtilityFunctions() {
		return utilityMap.keySet();
	}

	@Override
	public boolean equals(Object object) {
		if (object == null) {
			return false;
		}
		if (this.getClass() != object.getClass()) {
			return false;
		}
		Rule otherRule = (Rule) object;
		return hypothesis.equals(otherRule.hypothesis) && prediction.equals(otherRule.prediction);
	}

	private String utilityString() {
		StringBuffer stringBuffer = new StringBuffer("[");
		stringBuffer.append("Pos=" + getPositiveWeight() + ", ");
		stringBuffer.append("Neg=" + getNegativeWeight() + ", ");
		stringBuffer.append("Size=" + getCoveredWeight() + ", ");
		for (UtilityFunction function : utilityMap.keySet()) {
			stringBuffer.append(function.getAbbreviation() + "=" + Tools.formatIntegerIfPossible(utilityMap.get(function))
					+ ", ");
		}
		stringBuffer.subSequence(0, stringBuffer.length() - 2);
		stringBuffer.append("]");
		return stringBuffer.toString();
	}

	public String toStringScored() {
		return toString() + "  " + utilityString();
	}

	@Override
	public String toString() {
		return hypothesis + " --> " + prediction;
	}

	public Hypothesis getPremise() {
		return hypothesis;
	}

	public Literal getConclusion() {
		return prediction;
	}
}
