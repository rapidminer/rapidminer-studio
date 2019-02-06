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

import java.util.LinkedList;
import java.util.List;

import com.rapidminer.example.Attribute;
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
import com.rapidminer.operator.learner.tree.AbstractTreeLearner;
import com.rapidminer.operator.learner.tree.EmptyTermination;
import com.rapidminer.operator.learner.tree.NoAttributeLeftTermination;
import com.rapidminer.operator.learner.tree.SplitCondition;
import com.rapidminer.operator.learner.tree.Terminator;
import com.rapidminer.parameter.ParameterType;
import com.rapidminer.parameter.ParameterTypeDouble;
import com.rapidminer.parameter.ParameterTypeStringCategory;
import com.rapidminer.parameter.UndefinedParameterError;
import com.rapidminer.tools.RandomGenerator;
import com.rapidminer.tools.Tools;


/**
 * <p>
 * This operator works similar to the propositional rule learner named Repeated Incremental Pruning
 * to Produce Error Reduction (RIPPER, Cohen 1995). Starting with the less prevalent classes, the
 * algorithm iteratively grows and prunes rules until there are no positive examples left or the
 * error rate is greater than 50%.
 * </p>
 *
 * <p>
 * In the growing phase, for each rule greedily conditions are added to the rule until the rule is
 * perfect (i.e. 100% accurate). The procedure tries every possible value of each attribute and
 * selects the condition with highest information gain.
 * </p>
 *
 * <p>
 * In the prune phase, for each rule any final sequences of the antecedents is pruned with the
 * pruning metric p/(p+n).
 * </p>
 *
 * @author Sebastian Land, Ingo Mierswa
 */
public class RuleLearner extends AbstractLearner {

	private static final String PARAMETER_SAMPLE_RATIO = "sample_ratio";

	private static final String PARAMETER_MINIMAL_PRUNE_BENEFIT = "minimal_prune_benefit";

	public static final String[] CRITERIA_NAMES = { "information_gain", "accuracy" };

	public static final Class<?>[] CRITERIA_CLASSES = { InfoGainCriterion.class, AccuracyCriterion.class };

	public static final int CRITERION_INFO_GAIN = 0;

	public static final int CRITERION_ACCURACY = 1;

	private List<Terminator> terminators = new LinkedList<Terminator>();

	public RuleLearner(OperatorDescription description) {
		super(description);
	}

	@Override
	public Model learn(ExampleSet exampleSet) throws OperatorException {
		// init
		terminators.add(new EmptyTermination());
		terminators.add(new NoAttributeLeftTermination());

		double pureness = getParameterAsDouble(SimpleRuleLearner.PARAMETER_PURENESS);
		double sampleRatio = getParameterAsDouble(PARAMETER_SAMPLE_RATIO);
		double minimalPruneBenefit = getParameterAsDouble(PARAMETER_MINIMAL_PRUNE_BENEFIT);
		boolean useLocalRandomSeed = getParameterAsBoolean(RandomGenerator.PARAMETER_USE_LOCAL_RANDOM_SEED);
		int localRandomSeed = getParameterAsInt(RandomGenerator.PARAMETER_LOCAL_RANDOM_SEED);

		Attribute label = exampleSet.getAttributes().getLabel();
		RuleModel ruleModel = new RuleModel(exampleSet);
		TermDetermination termDetermination = new TermDetermination(createCriterion());
		ExampleSet trainingSet = (ExampleSet) exampleSet.clone();
		trainingSet.recalculateAttributeStatistics(label);

		while (!shouldStop(trainingSet)) {
			String labelName = getNextLabel(trainingSet);
			Rule rule = new Rule(labelName);
			ExampleSet oldTrainingSet = (ExampleSet) trainingSet.clone();

			SplittedExampleSet growPruneSet = new SplittedExampleSet(trainingSet, sampleRatio,
					SplittedExampleSet.STRATIFIED_SAMPLING, useLocalRandomSeed, true, localRandomSeed);

			// growing
			SplittedExampleSet growingSet = new SplittedExampleSet(growPruneSet);
			growingSet.selectSingleSubset(0);
			SplittedExampleSet pruneSet = growPruneSet;
			pruneSet.selectSingleSubset(1);

			int growOldSize = -1;
			ExampleSet growSet = growingSet;
			while (growSet.size() > 0 && growSet.size() != growOldSize && !rule.isPure(growSet, pureness)
					&& growSet.getAttributes().size() > 0) {
				SplitCondition term = termDetermination.getBestTerm(growSet, labelName);
				if (term == null) {
					break;
				}

				// before adding: Check benefit if not added
				double prunedBenefit = 0;
				if (pruneSet.size() > 0) {
					prunedBenefit = getPruneBenefit(rule, pruneSet);
				}

				// add term
				rule.addTerm(term);

				// pruning
				if (pruneSet.size() > 0) {
					double unprunedBenefit = getPruneBenefit(rule, pruneSet);
					if (unprunedBenefit < prunedBenefit - minimalPruneBenefit) {
						rule.removeLastTerm();
						// if best new term is pruned: no further extension of the rule
						break;
					}
				}

				growOldSize = growSet.size();
				// removing uncovered rules
				growSet = rule.getCovered(growSet);
				// removing attribute
				Attribute splitAttribute = growSet.getAttributes().get(term.getAttributeName());
				if (splitAttribute.isNominal()) {
					growSet.getAttributes().remove(splitAttribute);
				}
				checkForStop();
			}

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

				trainingSet = rule.removeCovered(oldTrainingSet);
			} else {
				break;
			}
			trainingSet.recalculateAttributeStatistics(label);
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
				frequencies[counter++] = (int) (trainingSet.getStatistics(label, Statistics.COUNT, value) * sampleRatio);
			}
			defaultRule.setFrequencies(frequencies);
			ruleModel.addRule(defaultRule);
		}

		return ruleModel;
	}

	private double getPruneBenefit(Rule rule, ExampleSet exampleSet) {
		Attribute label = exampleSet.getAttributes().getLabel();
		Attribute weight = exampleSet.getAttributes().getWeight();
		double pTotal = 0.0d;
		double nTotal = 0.0d;
		double p = 0.0d;
		double n = 0.0d;
		for (Example e : exampleSet) {
			double currentWeight = 1.0d;
			if (weight != null) {
				currentWeight = e.getValue(weight);
			}
			if (e.getValue(label) == label.getMapping().getIndex(rule.getLabel())) {
				pTotal += currentWeight;
			} else {
				nTotal += currentWeight;
			}
			if (rule.coversExample(e)) {
				if (e.getValue(label) == label.getMapping().getIndex(rule.getLabel())) {
					p += currentWeight;
				} else {
					n += currentWeight;
				}
			}
		}
		return (p + nTotal - n) / (pTotal + nTotal);
	}

	private String getNextLabel(ExampleSet exampleSet) {
		Attribute label = exampleSet.getAttributes().getLabel();
		int index = (int) exampleSet.getStatistics(label, Statistics.MODE);
		return label.getMapping().mapIndex(index);
	}

	private boolean shouldStop(ExampleSet exampleSet) {
		for (Terminator terminator : terminators) {
			if (terminator.shouldStop(exampleSet, 0)) {
				return true;
			}
		}
		return false;
	}

	private Criterion createCriterion() throws UndefinedParameterError {
		String criterionName = getParameterAsString(AbstractTreeLearner.PARAMETER_CRITERION);
		Class<?> criterionClass = null;
		for (int i = 0; i < CRITERIA_NAMES.length; i++) {
			if (CRITERIA_NAMES[i].equals(criterionName)) {
				criterionClass = CRITERIA_CLASSES[i];
			}
		}

		if (criterionClass == null && criterionName != null) {
			try {
				criterionClass = Tools.classForName(criterionName);
			} catch (ClassNotFoundException e) {
				logWarning("Cannot find criterion '" + criterionName
						+ "' and cannot instantiate a class with this name. Using gain ratio criterion instead.");
			}
		}

		if (criterionClass != null) {
			try {
				return (Criterion) criterionClass.newInstance();
			} catch (InstantiationException e) {
				logWarning("Cannot instantiate criterion class '" + criterionClass.getName()
						+ "'. Using gain ratio criterion instead.");
				return new InfoGainCriterion();
			} catch (IllegalAccessException e) {
				logWarning("Cannot access criterion class '" + criterionClass.getName()
						+ "'. Using gain ratio criterion instead.");
				return new InfoGainCriterion();
			}
		} else {
			log("No relevance criterion defined, using gain ratio...");
			return new InfoGainCriterion();
		}
	}

	@Override
	public Class<? extends PredictionModel> getModelClass() {
		return RuleModel.class;
	}

	@Override
	public boolean supportsCapability(OperatorCapability capability) {
		switch (capability) {
			case BINOMINAL_ATTRIBUTES:
			case POLYNOMINAL_ATTRIBUTES:
			case NUMERICAL_ATTRIBUTES:
			case POLYNOMINAL_LABEL:
			case BINOMINAL_LABEL:
			case WEIGHTED_EXAMPLES:
			case MISSING_VALUES:
				return true;
			default:
				return false;
		}
	}

	@Override
	public List<ParameterType> getParameterTypes() {
		List<ParameterType> types = super.getParameterTypes();
		ParameterType type = new ParameterTypeStringCategory(AbstractTreeLearner.PARAMETER_CRITERION,
				"Specifies the used criterion for selecting attributes and numerical splits.", CRITERIA_NAMES,
				CRITERIA_NAMES[CRITERION_INFO_GAIN], false);
		type.setExpert(false);
		types.add(type);
		type = new ParameterTypeDouble(PARAMETER_SAMPLE_RATIO,
				"The sample ratio of training data used for growing and pruning.", 0.0d, 1.0d, 0.9d);
		type.setExpert(false);
		types.add(type);
		types.add(new ParameterTypeDouble(SimpleRuleLearner.PARAMETER_PURENESS,
				"The desired pureness, i.e. the necessary amount of the major class in a covered subset in order become pure.",
				0.0d, 1.0d, 0.9d, false));
		types.add(new ParameterTypeDouble(PARAMETER_MINIMAL_PRUNE_BENEFIT,
				"The minimum amount of benefit which must be exceeded over unpruned benefit in order to be pruned.", 0.0d,
				1.0d, 0.25d));

		types.addAll(RandomGenerator.getRandomGeneratorParameters(this));

		return types;
	}
}
