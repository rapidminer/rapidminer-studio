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

import java.util.ArrayList;
import java.util.Collection;
import java.util.List;

import com.rapidminer.example.Example;
import com.rapidminer.example.ExampleSet;
import com.rapidminer.example.set.ExampleSetUtilities;
import com.rapidminer.operator.learner.SimplePredictionModel;
import com.rapidminer.report.Readable;
import com.rapidminer.tools.Tools;


/**
 * The basic rule model.
 * 
 * @author Sebastian Land, Ingo Mierswa
 */
public class RuleModel extends SimplePredictionModel implements Readable {

	private static final long serialVersionUID = 7792658268037025366L;

	private List<Rule> rules = new ArrayList<>();

	public RuleModel(ExampleSet exampleSet) {
		super(exampleSet, ExampleSetUtilities.SetsCompareOption.ALLOW_SUPERSET,
				ExampleSetUtilities.TypesCompareOption.ALLOW_SAME_PARENTS);
	}

	@Override
	public String getName() {
		return "RuleModel";
	}

	@Override
	public double predict(Example example) {
		for (Rule rule : rules) {
			if (rule.coversExample(example)) {
				double[] confidences = rule.getConfidences();
				for (int index = 0; index < confidences.length; index++) {
					example.setConfidence(getLabel().getMapping().mapIndex(index), confidences[index]);
				}
				return getLabel().getMapping().getIndex(rule.getLabel());
			}
		}
		return Double.NaN; // return unknown if no rule exists
	}

	public double getPrediction(Example example) {
		for (Rule rule : rules) {
			if (rule.coversExample(example)) {
				double label = getLabel().getMapping().getIndex(rule.getLabel());
				return label;
			}
		}
		return Double.NaN; // return unknown if no rule exists
	}

	public void addRule(Rule rule) {
		this.rules.add(rule);
	}

	public void addRules(Collection<Rule> newRules) {
		this.rules.addAll(newRules);
	}

	public List<Rule> getRules() {
		return this.rules;
	}

	@Override
	public String toString() {
		StringBuffer buffer = new StringBuffer();
		int correct = 0;
		int wrong = 0;
		for (Rule rule : rules) {
			buffer.append(rule.toString());
			buffer.append(Tools.getLineSeparator());
			int label = getLabel().getMapping().getIndex(rule.getLabel());
			int[] frequencies = rule.getFrequencies();
			if (frequencies != null) {
				for (int i = 0; i < frequencies.length; i++) {
					if (i == label) {
						correct += frequencies[i];
					} else {
						wrong += frequencies[i];
					}
				}
			}
		}
		buffer.append(Tools.getLineSeparator());
		buffer.append("correct: " + correct + " out of " + (correct + wrong) + " training examples.");
		return buffer.toString();
	}

	public int getNumberOfReadables() {
		return 1;
	}

	public Readable getReadable(int index) {
		return this;
	}

}
