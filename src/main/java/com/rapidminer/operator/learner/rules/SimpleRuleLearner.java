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

import java.util.List;

import com.rapidminer.example.Attribute;
import com.rapidminer.example.ExampleSet;
import com.rapidminer.example.Statistics;
import com.rapidminer.operator.Model;
import com.rapidminer.operator.OperatorCapability;
import com.rapidminer.operator.OperatorDescription;
import com.rapidminer.operator.OperatorException;
import com.rapidminer.operator.learner.AbstractLearner;
import com.rapidminer.operator.learner.PredictionModel;
import com.rapidminer.operator.learner.tree.SplitCondition;
import com.rapidminer.parameter.ParameterType;
import com.rapidminer.parameter.ParameterTypeDouble;


/**
 * This operator builds an unpruned rule set of classification rules. It is based on the paper
 * Cendrowska, 1987: PRISM: An algorithm for inducing modular rules.
 *
 * @author Sebastian Land, Ingo Mierswa
 */
public class SimpleRuleLearner extends AbstractLearner {

	public static final String PARAMETER_PURENESS = "pureness";

	public SimpleRuleLearner(OperatorDescription description) {
		super(description);
	}

	@Override
	public Model learn(ExampleSet exampleSet) throws OperatorException {
		Attribute label = exampleSet.getAttributes().getLabel();
		RuleModel ruleModel = new RuleModel(exampleSet);

		double pureness = getParameterAsDouble(PARAMETER_PURENESS);
		TermDetermination termDetermination = new TermDetermination(new AccuracyCriterion(), 0.5d);
		ExampleSet trainingSet = (ExampleSet) exampleSet.clone();

		for (String labelName : label.getMapping().getValues()) {
			trainingSet.recalculateAttributeStatistics(label);
			int oldSize = -1;
			while (trainingSet.size() > 0 && trainingSet.size() != oldSize
					&& trainingSet.getStatistics(label, Statistics.COUNT, labelName) > 0) {
				Rule rule = new Rule(labelName);
				ExampleSet oldTrainingSet = (ExampleSet) trainingSet.clone();

				// grow rule
				int growOldSize = -1;
				ExampleSet growSet = (ExampleSet) trainingSet.clone();
				while (growSet.size() > 0 && growSet.size() != growOldSize && !rule.isPure(growSet, pureness)
						&& growSet.getAttributes().size() > 0) {
					SplitCondition term = termDetermination.getBestTerm(growSet, labelName);
					if (term == null) {
						break;
					}

					rule.addTerm(term);

					Attribute splitAttribute = growSet.getAttributes().get(term.getAttributeName());
					growSet.getAttributes().remove(splitAttribute);
					growOldSize = growSet.size();
					growSet = rule.getCovered(growSet);
				}

				// add rule if not empty
				if (rule.getTerms().size() > 0) {
					growSet = rule.getCovered(trainingSet);
					growSet.recalculateAttributeStatistics(label);
					int[] frequencies = new int[label.getMapping().size()];
					int counter = 0;
					for (String value : label.getMapping().getValues()) {
						frequencies[counter++] = (int) growSet.getStatistics(label, Statistics.COUNT, value);
					}
					rule.setFrequencies(frequencies);
					ruleModel.addRule(rule);
					oldSize = trainingSet.size();

					trainingSet = rule.removeCovered(oldTrainingSet);
				} else {
					break; // no other terms found for this class --> next class
				}

				trainingSet.recalculateAttributeStatistics(label);
			}
			checkForStop();
		}

		// training set not empty? add default rule
		if (trainingSet.size() > 0) {
			trainingSet.recalculateAttributeStatistics(label);
			int index = (int) trainingSet.getStatistics(label, Statistics.MODE);
			String defaultLabel = label.getMapping().mapIndex(index);
			Rule defaultRule = new Rule(defaultLabel);
			int[] frequencies = new int[label.getMapping().size()];
			int counter = 0;
			for (String value : label.getMapping().getValues()) {
				frequencies[counter++] = (int) trainingSet.getStatistics(label, Statistics.COUNT, value);
			}
			defaultRule.setFrequencies(frequencies);
			ruleModel.addRule(defaultRule);
		}

		return ruleModel;
	}

	@Override
	public Class<? extends PredictionModel> getModelClass() {
		return RuleModel.class;
	}

	@Override
	public boolean supportsCapability(OperatorCapability capability) {
		if (capability == com.rapidminer.operator.OperatorCapability.BINOMINAL_ATTRIBUTES) {
			return true;
		}
		if (capability == com.rapidminer.operator.OperatorCapability.POLYNOMINAL_ATTRIBUTES) {
			return true;
		}
		if (capability == com.rapidminer.operator.OperatorCapability.NUMERICAL_ATTRIBUTES) {
			return true;
		}
		if (capability == com.rapidminer.operator.OperatorCapability.POLYNOMINAL_LABEL) {
			return true;
		}
		if (capability == com.rapidminer.operator.OperatorCapability.BINOMINAL_LABEL) {
			return true;
		}
		if (capability == com.rapidminer.operator.OperatorCapability.WEIGHTED_EXAMPLES) {
			return true;
		}
		return false;
	}

	@Override
	public List<ParameterType> getParameterTypes() {
		List<ParameterType> types = super.getParameterTypes();
		types.add(new ParameterTypeDouble(
				PARAMETER_PURENESS,
				"The desired pureness, i.e. the necessary amount of the major class in a covered subset in order become pure.",
				0.0d, 1.0d, 0.9d, false));
		return types;
	}
}
