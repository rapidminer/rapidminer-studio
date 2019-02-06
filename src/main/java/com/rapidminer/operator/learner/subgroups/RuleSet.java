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
package com.rapidminer.operator.learner.subgroups;

import java.util.Iterator;
import java.util.LinkedHashSet;
import java.util.LinkedList;

import com.rapidminer.example.Attribute;
import com.rapidminer.example.Example;
import com.rapidminer.example.ExampleSet;
import com.rapidminer.example.set.ExampleSetUtilities;
import com.rapidminer.operator.OperatorException;
import com.rapidminer.operator.learner.SimplePredictionModel;
import com.rapidminer.operator.learner.subgroups.hypothesis.Rule;
import com.rapidminer.operator.learner.subgroups.utility.UtilityFunction;
import com.rapidminer.tools.Tools;


/**
 * A model consisting of rules which are scored by utility values. Only the best rule (according to
 * its utility) is used for prediction at the moment.
 * 
 * @author Tobias Malbrecht
 */
public class RuleSet extends SimplePredictionModel implements Iterable<Rule> {

	private boolean predictUncoveredRules = false;

	private static final long serialVersionUID = -47885282272818733L;

	private LinkedList<Rule> rules = null;

	private LinkedHashSet<UtilityFunction> utilityFunctions = null;

	public RuleSet(ExampleSet exampleSet) {
		super(exampleSet, ExampleSetUtilities.SetsCompareOption.ALLOW_SUPERSET,
				ExampleSetUtilities.TypesCompareOption.ALLOW_SAME_PARENTS);
		rules = new LinkedList<>();
		utilityFunctions = new LinkedHashSet<>();
	}

	public void addRule(Rule rule) {
		rules.add(rule);
		utilityFunctions.addAll(rule.getUtilityFunctions());
	}

	public Rule getRule(int index) {
		return rules.get(index);
	}

	public int getNumberOfRules() {
		return rules.size();
	}

	@Override
	public Iterator<Rule> iterator() {
		return rules.iterator();
	}

	public LinkedList<Rule> getPositiveRules() {
		LinkedList<Rule> positiveRules = new LinkedList<>();
		for (Rule rule : this) {
			if (rule.predictsPositive()) {
				positiveRules.add(rule);
			}
		}
		return positiveRules;
	}

	public LinkedList<Rule> getNegativeRules() {
		LinkedList<Rule> negativeRules = new LinkedList<>();
		for (Rule rule : this) {
			if (!rule.predictsPositive()) {
				negativeRules.add(rule);
			}
		}
		return negativeRules;
	}

	public int size() {
		return getNumberOfRules();
	}

	@Override
	public double predict(Example example) throws OperatorException {
		for (Rule rule : rules) {
			if (rule.applicable(example)) {
				return rule.getPrediction();
			}
		}
		return predictUncoveredRules ? example.getAttributes().getLabel().getMapping().getNegativeIndex() : Double.NaN;
	}

	public UtilityFunction[] getUtilityFunctions() {
		UtilityFunction[] functions = new UtilityFunction[utilityFunctions.size()];
		functions = utilityFunctions.toArray(functions);
		return functions;
	}

	@Override
	protected boolean supportsConfidences(Attribute label) {
		return false;
	}

	@Override
	public String toString() {
		StringBuffer stringBuffer = new StringBuffer();
		int i = 0;
		for (Rule rule : rules) {
			if (i < 10) {
				stringBuffer.append(rule.toStringScored());
				stringBuffer.append(Tools.getLineSeparator());
			}
			i++;
		}
		if (i > 10) {
			stringBuffer.append(Tools.getLineSeparators(2));
			stringBuffer.append("... and " + (i - 10) + " more rules!");
		}
		return stringBuffer.toString();
	}
}
