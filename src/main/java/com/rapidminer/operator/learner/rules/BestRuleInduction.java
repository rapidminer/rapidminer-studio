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

import java.util.Arrays;
import java.util.Collection;
import java.util.Iterator;
import java.util.List;
import java.util.Vector;

import com.rapidminer.example.Attribute;
import com.rapidminer.example.Example;
import com.rapidminer.example.ExampleSet;
import com.rapidminer.operator.Model;
import com.rapidminer.operator.OperatorCapability;
import com.rapidminer.operator.OperatorDescription;
import com.rapidminer.operator.OperatorException;
import com.rapidminer.operator.learner.AbstractLearner;
import com.rapidminer.operator.learner.PredictionModel;
import com.rapidminer.parameter.ParameterType;
import com.rapidminer.parameter.ParameterTypeBoolean;
import com.rapidminer.parameter.ParameterTypeCategory;
import com.rapidminer.parameter.ParameterTypeInt;
import com.rapidminer.parameter.UndefinedParameterError;


/**
 * This operator returns the best rule regarding WRAcc using exhaustive search. Features like the
 * incorporation of other metrics and the search for more than a single rule are prepared.
 *
 * The search strategy is BFS, with save pruning whenever applicable. This operator can easily be
 * extended to support other search strategies.
 *
 * @author Martin Scholz
 */
public class BestRuleInduction extends AbstractLearner {

	/** Helper class containing a rule and an upper bound for the score. */
	public static class RuleWithScoreUpperBound implements Comparable<Object> {

		private final ConjunctiveRuleModel rule;

		private final double scoreUpperBound;

		public RuleWithScoreUpperBound(ConjunctiveRuleModel rule, double scoreUpperBound) {
			this.rule = rule;
			this.scoreUpperBound = scoreUpperBound;
		}

		public ConjunctiveRuleModel getRule() {
			return this.rule;
		}

		public double getScoreBound() {
			return this.scoreUpperBound;
		}

		@Override
		public int compareTo(Object obj) {
			if (obj instanceof RuleWithScoreUpperBound) {
				double otherScore = ((RuleWithScoreUpperBound) obj).getScoreBound();
				if (this.getScoreBound() < otherScore) {
					return -1;
				} else if (this.getScoreBound() > otherScore) {
					return 1;
				} else {
					return 0;
				}
			} else {
				return this.getClass().getName().compareTo(obj.getClass().getName());
			}
		}

		@Override
		public boolean equals(Object o) {
			if (!(o instanceof RuleWithScoreUpperBound)) {
				return false;
			} else {
				return this.rule.equals(((RuleWithScoreUpperBound) o).rule);
			}
		}

		@Override
		public int hashCode() {
			return this.rule.hashCode();
		}
	}

	private static final String PARAMETER_MAX_DEPTH = "max_depth";

	private static final String PARAMETER_UTILITY_FUNCTION = "utility_function";

	private static final String PARAMETER_MAX_CACHE = "max_cache";

	private static final String PARAMETER_RELATIVE_TO_PREDICTIONS = "relative_to_predictions";

	private static final String WRACC = "weighted relative accuracy";

	private static final String BINOMIAL = "binomial test function";

	private static final String[] UTILITY_FUNCTION_LIST = new String[] { WRACC, BINOMIAL };

	private double globalP;

	private double globalN;

	protected ConjunctiveRuleModel bestRule;

	private double bestScore;

	private int maxDepth;

	// nodes under consideration
	private final Vector<RuleWithScoreUpperBound> openNodes = new Vector<RuleWithScoreUpperBound>();

	// keep track of rules that have been pruned, to avoid
	// evaluations for any kind of refinements
	private final Vector<ConjunctiveRuleModel> prunedNodes = new Vector<ConjunctiveRuleModel>();

	public BestRuleInduction(OperatorDescription description) {
		super(description);
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

	protected void initHighscore() {
		this.bestRule = null;
		this.bestScore = Double.NEGATIVE_INFINITY;
	}

	/**
	 * Adds a rule to the set of best rules if its score is high enough. Currently just a single
	 * rule is stored. Additionally it is checked whether the rule is bad enough to be pruned.
	 *
	 * @return true iff the rule can be pruned
	 */
	protected boolean communicateToHighscore(ConjunctiveRuleModel rule, double[] counts) throws UndefinedParameterError {
		double optimisticScore = this.getOptimisticScore(counts);
		if (optimisticScore <= this.getPruningScore()) {
			return true; // indicates pruning
		} else {
			double posScore = this.getScore(counts, true);
			double negScore = this.getScore(counts, false);
			if (posScore > this.bestScore) {
				this.bestRule = rule;
				this.bestScore = posScore;
			}
			if (negScore > this.bestScore) {
				ConjunctiveRuleModel negRule = new ConjunctiveRuleModel(rule,
						rule.getLabel().getMapping().getNegativeIndex());
				this.bestRule = negRule;
				this.bestScore = negScore;
			}

			return false; // no pruning
		}
	}

	/** @return the best rule found */
	protected ConjunctiveRuleModel getBestRule() {
		return this.bestRule;
	}

	/** @return the lowest score of the stored best rules for pruning */
	protected double getPruningScore() {
		return this.bestScore;
	}

	@Override
	public Model learn(ExampleSet exampleSet) throws OperatorException {
		this.initHighscore();

		int positiveLabel = exampleSet.getAttributes().getLabel().getMapping().getPositiveIndex();
		// int negativeLabel = exampleSet.getLabel().getNegativeIndex();

		ConjunctiveRuleModel defaultRule = new ConjunctiveRuleModel(exampleSet, positiveLabel);
		// ConjunctiveRuleModel negRule = new
		// ConjunctiveRuleModel(exampleSet.getLabel(), negativeLabel);

		double[] globalCounts = this.getCounts(defaultRule, exampleSet);
		this.globalP = globalCounts[0];
		this.globalN = globalCounts[1];
		this.communicateToHighscore(defaultRule, globalCounts);

		double optimisticScore = this.getOptimisticScore(globalCounts);
		this.openNodes.clear();
		this.prunedNodes.clear();
		this.addRulesToOpenNodes(defaultRule.getAllRefinedRules(exampleSet), optimisticScore);

		int length = 1;
		maxDepth = this.getParameterAsInt(PARAMETER_MAX_DEPTH);
		int maxCache = this.getParameterAsInt(PARAMETER_MAX_CACHE);

		while (!this.openNodes.isEmpty() && length <= maxDepth) {
			int ignored = 0;

			log("Evaluating " + this.openNodes.size() + " rules of length " + length);
			if (this.openNodes.size() > maxCache) {
				log("Ignoring all but the " + maxCache + " rules with highest support.");
			}

			RuleWithScoreUpperBound[] ruleArray = new RuleWithScoreUpperBound[this.openNodes.size()];
			this.openNodes.toArray(ruleArray);
			Arrays.sort(ruleArray);
			int stopAtIndex = Math.max(0, ruleArray.length - maxCache);

			this.openNodes.clear();
			for (int i = ruleArray.length - 1; i >= stopAtIndex; i--) {
				RuleWithScoreUpperBound rulePlusScore = ruleArray[i];
				ConjunctiveRuleModel rule = rulePlusScore.getRule();

				if (this.isRefinementOfPrunedRule(rule)) {
					ignored++;
				} else if (rulePlusScore.getScoreBound() <= this.getPruningScore()) {
					ignored++;
					// This pruning could not be derived from prunedNodes and
					// may be useful
					// later on for refined rules with a less precise optimistic
					// estimate.
					this.prunedNodes.add(rulePlusScore.getRule());
				} else {
					this.expandNode(rule, exampleSet);
				}
				checkForStop();
			}

			log("Could ignore " + ignored + " rules as refinements of pruned rules or by optimistic estimates.");
			log("Number of pruned rules in cache: " + this.prunedNodes.size());
			log("Best rule is " + this.getBestRule().toString());
			log("Score is " + this.getPruningScore());

			length++;
		}

		this.openNodes.clear();
		this.prunedNodes.clear();
		return this.getBestRule();
	}

	/**
	 * Annotates the collection of ConjunctiveRuleModels with an optimistic score they may achieve
	 * in the best case and adds them to the collection of open nodes.
	 */
	private void addRulesToOpenNodes(Collection<ConjunctiveRuleModel> rules, double scoreUpperBound) {
		if (scoreUpperBound <= this.getPruningScore()) {
			return;
		}

		for (ConjunctiveRuleModel rule : rules) {
			this.openNodes.add(new RuleWithScoreUpperBound(rule, scoreUpperBound));
		}

	}

	/**
	 * Evaluates a single rule by computing its score, and the best possible score after refining
	 * this rule. If this cannot improve over the currently best rules, then the refinements are
	 * pruned. Otherwise all refinements plus optimistic estimates are added to the collection of
	 * open nodes.
	 *
	 * If the evaluated rule is good enough, then it is stored toghether with its score.
	 */
	private void expandNode(ConjunctiveRuleModel rule, ExampleSet exampleSet) throws OperatorException {
		// Compute counts:
		double[] counts = this.getCounts(rule, exampleSet);

		// Store in highscore if necessary and check whether it may be pruned.
		boolean pruning = this.communicateToHighscore(rule, counts);
		if (pruning == true) {
			this.prunedNodes.add(rule);
			// Nothing to add to the collection of open nodes ..
		} else if (rule.getRuleLength() < maxDepth) {
			// Store all the refinements for later investigation:
			this.addRulesToOpenNodes(rule.getAllRefinedRules(exampleSet), this.getOptimisticScore(counts));
		}
	}

	/**
	 * @param rule
	 *            a ConjuctiveRuleModel for which it is checked whether a more general rule has
	 *            already been pruned.
	 * @return true, if this rule is a refinement of a pruned rule. The rules are compared using the
	 *         method <code>ConjunctiveRuleModel.isRefinementOf(ConjunctiveRuleModel model)</code>
	 */
	public boolean isRefinementOfPrunedRule(ConjunctiveRuleModel rule) {
		for (ConjunctiveRuleModel prunedRule : prunedNodes) {
			// In this collection all rules predict positive, but the scores are
			// computed for the best label. For this reason the following
			// refinement test is valid.
			if (rule.isRefinementOf(prunedRule)) {
				return true;
			}
		}
		return false;
	}

	/**
	 * Computes the WRAcc or BINOMIAL TEST FUNCTION based on p, n, and the global values P and N
	 * stored in this object. First two entries of counts are p and n, optionally estimates for p
	 * and n can be supplied as further parameters.
	 */
	protected double getScore(double[] counts, boolean predictPositives) throws UndefinedParameterError {
		double p = counts[0];
		double n = counts[1];
		double cov = (p + n) / (globalP + globalN);
		double pnRel = predictPositives ? p : n;

		String function = UTILITY_FUNCTION_LIST[this.getParameterAsInt(PARAMETER_UTILITY_FUNCTION)];
		UndefinedParameterError upe = new UndefinedParameterError(PARAMETER_UTILITY_FUNCTION, this);

		double score;
		if (this.getParameterAsBoolean(PARAMETER_RELATIVE_TO_PREDICTIONS) == false || counts.length != 4) {
			double pnAbs = predictPositives ? globalP : globalN;

			if (function.equals(WRACC)) {
				score = cov * (pnRel / (p + n) - pnAbs / (globalP + globalN));
			} else if (function.equals(BINOMIAL)) {
				score = Math.sqrt(cov) * (pnRel / (p + n) - pnAbs / (globalP + globalN));
			} else {
				throw upe;
			}
		} else {
			double estP = counts[2];
			double estN = counts[3];
			double pnEst = predictPositives ? estP : estN;

			if (function.equals(WRACC)) {
				score = cov * (pnRel / (p + n) - pnEst / (estP + estN));
			} else if (function.equals(BINOMIAL)) {
				score = Math.sqrt(cov) * (pnRel / (p + n) - pnEst / (estP + estN));
			} else {
				throw upe;
			}
		}
		return score;
	}

	/**
	 * Computes the best possible score that might be achieved by refining the rule. During learning
	 * the conclusion is normalized to "positive", so the better of the estimates of the better
	 * conclusion is returned.
	 */
	protected double getOptimisticScore(double[] counts) throws UndefinedParameterError {
		double p = counts[0];
		double n = counts[1];

		if (this.getParameterAsBoolean(PARAMETER_RELATIVE_TO_PREDICTIONS) == false || counts.length != 4) {
			// For reasonable utility functions adding just negatives decreases
			// the score.
			return Math.max(this.getScore(new double[] { p, 0 }, true), this.getScore(new double[] { 0, n }, false));
		} else {
			// Improvement for positive rules: discard all negatives, which are
			// at the same time considered to be positives with confidence of 1
			// by the given prediction. As a complex second step discarding
			// further
			// positives might help to improve the score, since this allows to
			// lower the estimated precision term.
			// To keep things simple a non-tight optimistic score is computed:
			// 1. Keep all positives, discard all negatives: p'=p, n'=0
			// 2. Lower the estimated confidence to 0, simply estP' = 0, estN' =
			// 0.
			// Analogously for the negatively predicting rule.
			double estP = counts[2];
			double estN = counts[3];

			return Math.max(this.getScore(new double[] { p, 0, 0, estN }, true),
					this.getScore(new double[] { 0, n, 0, estP }, false));
		}

	}

	/**
	 * @param rule
	 *            the rule to evaluate
	 * @param exampleSet
	 *            the exampleSet to get the counts for
	 * @return a double[2] object, first parameter is p, second is n. If rule discovery relative to
	 *         a predicted label is activated, then a double[4] is returned, which contains the
	 *         estimated positives and negatives in the covered part as fields 3 and 4.
	 * @throws OperatorException
	 */
	protected double[] getCounts(ConjunctiveRuleModel rule, ExampleSet exampleSet) throws OperatorException {
		Attribute weightAttr = exampleSet.getAttributes().getWeight();
		Attribute predictedLabel = exampleSet.getAttributes().getPredictedLabel();
		boolean relToPred = predictedLabel != null && this.getParameterAsBoolean(PARAMETER_RELATIVE_TO_PREDICTIONS);

		int coveredPart = rule.getConclusion();
		int posIndex = exampleSet.getAttributes().getLabel().getMapping().getPositiveIndex();
		int negIndex = exampleSet.getAttributes().getLabel().getMapping().getNegativeIndex();
		String posS = null;
		String negS = null;
		if (relToPred) {
			posS = predictedLabel.getMapping().mapIndex(posIndex);
			negS = predictedLabel.getMapping().mapIndex(negIndex);
		}

		double p = 0;
		double n = 0;
		double estP = 0;
		double estN = 0;

		Iterator<Example> reader = exampleSet.iterator();
		while (reader.hasNext()) {
			Example example = reader.next();
			double weight = weightAttr == null ? 1 : example.getValue(weightAttr);
			if (rule.predict(example) == coveredPart) {
				if (example.getValue(example.getAttributes().getLabel()) == posIndex) {
					p += weight;
				} else {
					n += weight;
				}

				if (relToPred) {
					double sum = example.getConfidence(posS) + example.getConfidence(negS);
					estP += weight * example.getConfidence(posS) / sum;
					estN += weight * example.getConfidence(negS) / sum;
				}
			}
		}

		return relToPred ? new double[] { p, n, estP, estN } : new double[] { p, n };
	}

	@Override
	public Class<? extends PredictionModel> getModelClass() {
		return ConjunctiveRuleModel.class;
	}

	/**
	 * Adds the parameters &quot;number of iterations&quot; and &quot;model file&quot;.
	 */
	@Override
	public List<ParameterType> getParameterTypes() {
		List<ParameterType> types = super.getParameterTypes();
		types.add(new ParameterTypeInt(PARAMETER_MAX_DEPTH, "An upper bound for the number of literals.", 1,
				Integer.MAX_VALUE, 2));
		types.add(new ParameterTypeCategory(PARAMETER_UTILITY_FUNCTION, "The function to be optimized by the rule.",
				UTILITY_FUNCTION_LIST, 0));
		types.add(new ParameterTypeInt(PARAMETER_MAX_CACHE,
				"Bounds the number of rules considered per depth to avoid high memory consumption, but leads to incomplete search.",
				1, Integer.MAX_VALUE, 10000));
		types.add(new ParameterTypeBoolean(PARAMETER_RELATIVE_TO_PREDICTIONS,
				"Searches for rules with a maximum difference to the predited label.", false));
		return types;
	}
}
