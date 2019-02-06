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

import com.rapidminer.example.Attribute;
import com.rapidminer.example.Attributes;
import com.rapidminer.example.Example;
import com.rapidminer.example.ExampleSet;
import com.rapidminer.example.Statistics;
import com.rapidminer.example.set.SplittedExampleSet;
import com.rapidminer.operator.Model;
import com.rapidminer.operator.OperatorCapability;
import com.rapidminer.operator.OperatorDescription;
import com.rapidminer.operator.OperatorException;
import com.rapidminer.operator.learner.AbstractLearner;
import com.rapidminer.operator.learner.PredictionModel;
import com.rapidminer.operator.learner.tree.LessEqualsSplitCondition;
import com.rapidminer.operator.learner.tree.NominalSplitCondition;
import com.rapidminer.operator.learner.tree.SplitCondition;


/**
 * This operator concentrates on one single attribute and determines the best splitting terms for
 * minimizing the training error. The result will be a single rule containing all these terms.
 *
 * @author Ingo Mierswa, Sebastian Land
 */
public class SingleRuleLearner extends AbstractLearner {

	private NumericalSplitter splitter = new NumericalSplitter();

	public SingleRuleLearner(OperatorDescription description) {
		super(description);
	}

	@Override
	public Model learn(ExampleSet inputSet) throws OperatorException {
		ExampleSet exampleSet = (ExampleSet) inputSet.clone();

		// learn all models
		Collection<RuleModel> models = new ArrayList<>();
		for (Attribute attribute : exampleSet.getAttributes()) {
			if (attribute.isNominal()) {
				models.add(createNominalRuleModel(exampleSet, attribute));
			} else {
				models.add(createNumericalRuleModel(exampleSet, attribute));
			}
		}

		// select and return best model
		return getBestModel(models, exampleSet, true);
	}

	private RuleModel createNumericalRuleModel(ExampleSet trainingSet, Attribute attribute) {
		RuleModel model = new RuleModel(trainingSet);

		// split by best attribute
		int oldSize = -1;
		while (trainingSet.size() > 0 && trainingSet.size() != oldSize) {
			Split bestSplit = splitter.getBestSplit(trainingSet, attribute, null);
			double bestSplitValue = bestSplit.getSplitPoint();
			if (!Double.isNaN(bestSplitValue)) {
				SplittedExampleSet splittedSet = SplittedExampleSet.splitByAttribute(trainingSet, attribute, bestSplitValue);
				Attribute label = splittedSet.getAttributes().getLabel();
				splittedSet.selectSingleSubset(0);
				SplitCondition condition = new LessEqualsSplitCondition(attribute, bestSplitValue);

				splittedSet.recalculateAttributeStatistics(label);
				int labelValue = (int) splittedSet.getStatistics(label, Statistics.MODE);
				String labelName = label.getMapping().mapIndex(labelValue);
				Rule rule = new Rule(labelName, condition);

				int[] frequencies = new int[label.getMapping().size()];
				int counter = 0;
				for (String value : label.getMapping().getValues()) {
					frequencies[counter++] = (int) splittedSet.getStatistics(label, Statistics.COUNT, value);
				}
				rule.setFrequencies(frequencies);
				model.addRule(rule);
				oldSize = trainingSet.size();
				trainingSet = rule.removeCovered(trainingSet);
			} else {
				break;
			}
		}

		// add default rule if some examples were not yet covered
		if (trainingSet.size() > 0) {
			Attribute label = trainingSet.getAttributes().getLabel();
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
			model.addRule(defaultRule);
		}

		return model;
	}

	private RuleModel createNominalRuleModel(ExampleSet exampleSet, Attribute attribute) {
		RuleModel model = new RuleModel(exampleSet);
		SplittedExampleSet splittedSet = SplittedExampleSet.splitByAttribute(exampleSet, attribute);
		Attribute label = splittedSet.getAttributes().getLabel();
		for (int i = 0; i < splittedSet.getNumberOfSubsets(); i++) {
			splittedSet.selectSingleSubset(i);
			splittedSet.recalculateAttributeStatistics(label);
			SplitCondition term = new NominalSplitCondition(attribute, attribute.getMapping().mapIndex(i));

			int labelValue = (int) splittedSet.getStatistics(label, Statistics.MODE);
			String labelName = label.getMapping().mapIndex(labelValue);
			Rule rule = new Rule(labelName, term);

			int[] frequencies = new int[label.getMapping().size()];
			int counter = 0;
			for (String value : label.getMapping().getValues()) {
				frequencies[counter++] = (int) splittedSet.getStatistics(label, Statistics.COUNT, value);
			}
			rule.setFrequencies(frequencies);
			model.addRule(rule);
		}
		return model;
	}

	private RuleModel getBestModel(Collection<RuleModel> models, ExampleSet exampleSet, boolean useExampleWeights) {
		Attribute exampleWeightAttribute = exampleSet.getAttributes().getSpecial(Attributes.WEIGHT_NAME);
		useExampleWeights = useExampleWeights && exampleWeightAttribute != null;

		// calculating weighted error for rules
		double[] weightedError = new double[models.size()];
		for (Example example : exampleSet) {
			int i = 0;
			double currentWeight = 1;
			if (useExampleWeights) {
				currentWeight = example.getValue(exampleWeightAttribute);
			}
			double currentLabel = example.getLabel();
			for (RuleModel currentModel : models) {
				if (currentLabel != currentModel.getPrediction(example)) {
					weightedError[i] += currentWeight;
				}
				i++;
			}
		}

		// finding best rule
		int i = 0;
		double bestError = Double.POSITIVE_INFINITY;
		RuleModel bestModel = null;
		for (RuleModel currentModel : models) {
			if (weightedError[i] < bestError) {
				bestError = weightedError[i];
				bestModel = currentModel;
			}
			i++;
		}
		return bestModel;
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
}
