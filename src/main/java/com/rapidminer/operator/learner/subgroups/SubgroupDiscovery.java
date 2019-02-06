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

import com.rapidminer.example.Example;
import com.rapidminer.example.ExampleSet;
import com.rapidminer.operator.Model;
import com.rapidminer.operator.OperatorCapability;
import com.rapidminer.operator.OperatorDescription;
import com.rapidminer.operator.OperatorException;
import com.rapidminer.operator.learner.AbstractLearner;
import com.rapidminer.operator.learner.PredictionModel;
import com.rapidminer.operator.learner.subgroups.hypothesis.Hypothesis;
import com.rapidminer.operator.learner.subgroups.hypothesis.Rule;
import com.rapidminer.operator.learner.subgroups.utility.UtilityFunction;
import com.rapidminer.parameter.ParameterType;
import com.rapidminer.parameter.ParameterTypeCategory;
import com.rapidminer.parameter.ParameterTypeDouble;
import com.rapidminer.parameter.ParameterTypeInt;

import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.Iterator;
import java.util.LinkedList;
import java.util.List;


/**
 * This operator discovers subgroups (or induces a rule set, respectively) by generating hypotheses
 * exhaustively. Generation is done by stepwise refining the empty hypothesis (which contains no
 * literals). The loop for this task hence iterates over the depth of the search space, i.e. the
 * number of literals of the generated hypotheses. The maximum depth of the search can be specified.
 * Furthermore the search space can be pruned by specifying a minimum coverage of the hypothesis or
 * by using only a given amount of hypotheses which have the highest coverage.
 * 
 * From the hypotheses, rules are derived according to the users preference. The operator allows the
 * derivation of positive rules (Y+) and negative rules (Y-) separately or the combination by
 * deriving both rules or only the one which is the most probable due to the examples covered by the
 * hypothesis (hence: the actual prediction for that subset).
 * 
 * All generated rules are evaluated on the example set by a user specified utility function and
 * stored in the final rule set if they (1) exceed a minimum utility threshold or (2) are among the
 * k best rules. The desired behavior can be specified as well.
 * 
 * @author Tobias Malbrecht
 */
public class SubgroupDiscovery extends AbstractLearner {

	// comparator class that compares rules according to
	// the specified utility function
	private static class RuleComparator implements Comparator<Rule> {

		Class<? extends UtilityFunction> functionClass;

		public RuleComparator(Class<? extends UtilityFunction> functionClass) {
			this.functionClass = functionClass;
		}

		@Override
		public int compare(Rule firstRule, Rule secondRule) {
			return Double.compare(secondRule.getUtility(functionClass), firstRule.getUtility(functionClass));
		}
	}

	private static class HypothesisComparator implements Comparator<Hypothesis> {

		@Override
		public int compare(Hypothesis firstHypothesis, Hypothesis secondHypothesis) {
			return Double.compare(secondHypothesis.getCoveredWeight(), firstHypothesis.getCoveredWeight());
		}
	}

	public static final String PARAMETER_DISCOVERY_MODE = "mode";

	public static final String[] DISCOVERY_MODES = { "above minimum utility", "k best rules" };

	public static final int DISCOVERY_MODE_ABOVE_MINIMUM_UTILITY = 0;

	public static final int DISCOVERY_MODE_K_BEST_RULES = 1;

	public static final String PARAMETER_UTILITY_FUNCTION = "utility_function";

	public static final String PARAMETER_RULE_GENERATION = "rule_generation";

	public static final String[] RULE_GENERATION_MODES = Hypothesis.RULE_GENERATION_MODES;

	public static final String PARAMETER_MAX_DEPTH = "max_depth";

	public static final String PARAMETER_MIN_UTILITY = "min_utility";

	public static final String PARAMETER_K_BEST_RULES = "k_best_rules";

	public static final String PARAMETER_MIN_COVERAGE = "min_coverage";

	public static final String PARAMETER_MAX_CACHE = "max_cache";

	public SubgroupDiscovery(OperatorDescription description) {
		super(description);
	}

	@Override
	public Model learn(ExampleSet exampleSet) throws OperatorException {
		int mode = getParameterAsInt(PARAMETER_DISCOVERY_MODE);
		int maxDepth = getParameterAsInt(PARAMETER_MAX_DEPTH);
		double minUtility = getParameterAsDouble(PARAMETER_MIN_UTILITY);
		int kBestRules = getParameterAsInt(PARAMETER_K_BEST_RULES);
		int ruleGenerationMode = getParameterAsInt(PARAMETER_RULE_GENERATION);
		double coverageThreshold = getParameterAsDouble(PARAMETER_MIN_COVERAGE);
		int maxCache = getParameterAsInt(PARAMETER_MAX_CACHE);

		// determine a priori statistics
		int numberOfAttributes = exampleSet.getAttributes().size();
		double totalWeight = 0.0d;
		double totalPositiveWeight = 0.0d;
		for (Example example : exampleSet) {
			double weight = 1.0d;
			if (exampleSet.getAttributes().getWeight() != null) {
				weight = example.getWeight();
			}
			totalWeight += weight;
			if (example.getLabel() == example.getAttributes().getLabel().getMapping().getPositiveIndex()) {
				totalPositiveWeight += weight;
			}
		}

		// initialise utility functions
		UtilityFunction[] utilityFunctions = UtilityFunction.getUtilityFunctions(totalWeight, totalPositiveWeight);
		UtilityFunction mainUtilityFunction = utilityFunctions[getParameterAsInt(PARAMETER_UTILITY_FUNCTION)];
		RuleComparator ruleComparator = new RuleComparator(mainUtilityFunction.getClass());

		LinkedList<Rule> acceptedRules = new LinkedList<Rule>();
		ArrayList<Rule> bestRules = new ArrayList<Rule>(kBestRules);

		// create initial hypotheses
		LinkedList<Hypothesis> hypotheses = new LinkedList<Hypothesis>();
		Hypothesis emptyHypothesis = new Hypothesis();
		hypotheses.addAll(emptyHypothesis.restrictedRefine(exampleSet.getAttributes()));

		for (int i = 0; i < (maxDepth > numberOfAttributes ? numberOfAttributes : maxDepth); i++) {

			if (hypotheses.size() == 0) {
				break;
			}

			// evaluate hypotheses on data set
			log("evaluating " + hypotheses.size() + " hypotheses with " + (i + 1) + " literals");
			for (Example example : exampleSet) {
				for (Hypothesis hypothesis : hypotheses) {
					hypothesis.apply(example);
				}
			}

			int discarded = 0;
			for (Iterator<Hypothesis> iterator = hypotheses.iterator(); iterator.hasNext();) {
				Hypothesis hypothesis = iterator.next();
				// discard hypotheses which cover too few examples
				if ((hypothesis.getCoveredWeight() / totalWeight) <= coverageThreshold) {
					iterator.remove();
					discarded++;
					continue;
				}
			}
			if (discarded > 0) {
				log("removed " + discarded + " hypotheses not exceeding min coverage");
			}

			if (maxCache != -1) {
				Collections.sort(hypotheses, new HypothesisComparator());
				int deleteHypotheses = hypotheses.size() - maxCache;
				for (int j = 0; j < deleteHypotheses; j++) {
					hypotheses.removeLast();
				}
				if (deleteHypotheses > 0) {
					log("removed " + deleteHypotheses + " hypotheses with the lowest coverage");
				}
			}

			log("generating rules from " + hypotheses.size() + " hypotheses");
			LinkedList<Hypothesis> nextHypotheses = new LinkedList<Hypothesis>();
			for (Iterator<Hypothesis> iterator = hypotheses.iterator(); iterator.hasNext();) {
				Hypothesis hypothesis = iterator.next();

				LinkedList<Rule> rules = hypothesis.generateRules(ruleGenerationMode, exampleSet.getAttributes().getLabel());
				for (Rule rule : rules) {

					// utility evaluation
					for (int j = 0; j < utilityFunctions.length; j++) {
						rule.setUtility(utilityFunctions[j], utilityFunctions[j].utility(rule));
					}
					double utility = mainUtilityFunction.utility(rule);

					// add rule to result rule set ...
					switch (mode) {

					// ... if it exceeds a utility threshold ...
						case DISCOVERY_MODE_ABOVE_MINIMUM_UTILITY:
							if (utility >= minUtility) {
								acceptedRules.add(rule);
								log("scored: " + rule);
							}
							break;

						// ... or if it is among the (current) k best rules
						case DISCOVERY_MODE_K_BEST_RULES:
							if (bestRules.size() < kBestRules) {
								bestRules.add(rule);
								log("scored: " + rule + " [q(h)=" + utility + "]");
								Collections.sort(bestRules, ruleComparator);
								break;
							}
							if (utility > bestRules.get(kBestRules - 1).getUtility(mainUtilityFunction.getClass())) {
								bestRules.set(kBestRules - 1, rule);
								minUtility = utility;
								log("scored: " + rule + " [q(h)=" + utility + "]");
								Collections.sort(bestRules, ruleComparator);
							}
							break;
					}

				}

				// prune (do not consider hypothesis further) or add refinements to new hypothesis
				// list
				double optimisticEstimate = mainUtilityFunction.optimisticEstimate(hypothesis);
				if (optimisticEstimate >= minUtility) {
					for (Hypothesis nextHypothesis : hypothesis.restrictedRefine()) {
						nextHypotheses.add(nextHypothesis);
					}
				}
			}
			hypotheses = nextHypotheses;
		}

		// create model
		RuleSet model = new RuleSet(exampleSet);
		switch (mode) {
			case DISCOVERY_MODE_ABOVE_MINIMUM_UTILITY:
				Collections.sort(acceptedRules, ruleComparator);
				for (Rule rule : acceptedRules) {
					model.addRule(rule);
				}
				break;
			case DISCOVERY_MODE_K_BEST_RULES:
				Collections.sort(bestRules, ruleComparator);
				for (Rule rule : bestRules) {
					model.addRule(rule);
				}
				break;
		}
		return model;
	}

	@Override
	public Class<? extends PredictionModel> getModelClass() {
		return RuleSet.class;
	}

	@Override
	public boolean supportsCapability(OperatorCapability lc) {
		if (lc == com.rapidminer.operator.OperatorCapability.POLYNOMINAL_ATTRIBUTES) {
			return true;
		}
		if (lc == com.rapidminer.operator.OperatorCapability.BINOMINAL_ATTRIBUTES) {
			return true;
		}
		if (lc == com.rapidminer.operator.OperatorCapability.BINOMINAL_LABEL) {
			return true;
		}
		if (lc == com.rapidminer.operator.OperatorCapability.WEIGHTED_EXAMPLES) {
			return true;
		}
		return false;
	}

	@Override
	public List<ParameterType> getParameterTypes() {
		List<ParameterType> types = super.getParameterTypes();
		types.add(new ParameterTypeCategory(PARAMETER_DISCOVERY_MODE, "Discovery mode.", DISCOVERY_MODES, 1, false));
		types.add(new ParameterTypeCategory(PARAMETER_UTILITY_FUNCTION, "Utility function.", UtilityFunction.FUNCTIONS,
				UtilityFunction.WRACC));
		types.add(new ParameterTypeDouble(PARAMETER_MIN_UTILITY, "Minimum quality which has to be reached.",
				Double.NEGATIVE_INFINITY, Double.POSITIVE_INFINITY, 0.0));
		types.add(new ParameterTypeInt(PARAMETER_K_BEST_RULES, "Report the k best rules.", 1, Integer.MAX_VALUE, 10, false));
		types.add(new ParameterTypeCategory(PARAMETER_RULE_GENERATION, "Determines which rules are generated.",
				RULE_GENERATION_MODES, Hypothesis.POSITIVE_AND_NEGATIVE_RULES));
		types.add(new ParameterTypeInt(PARAMETER_MAX_DEPTH, "Maximum depth of BFS.", 0, Integer.MAX_VALUE, 5));
		types.add(new ParameterTypeDouble(PARAMETER_MIN_COVERAGE,
				"Only consider rules which exceed the given coverage threshold.", 0, 1, 0));
		types.add(new ParameterTypeInt(PARAMETER_MAX_CACHE,
				"Bounds the number of rules which are evaluated (only the most supported rules are used).", -1,
				Integer.MAX_VALUE, -1));
		return types;
	}
}
