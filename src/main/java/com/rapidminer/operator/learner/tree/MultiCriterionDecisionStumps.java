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
package com.rapidminer.operator.learner.tree;

import java.util.Arrays;
import java.util.Comparator;
import java.util.Iterator;
import java.util.List;

import com.rapidminer.example.Attribute;
import com.rapidminer.example.Example;
import com.rapidminer.example.ExampleSet;
import com.rapidminer.example.set.ExampleSetUtilities;
import com.rapidminer.operator.Model;
import com.rapidminer.operator.OperatorCapability;
import com.rapidminer.operator.OperatorDescription;
import com.rapidminer.operator.OperatorException;
import com.rapidminer.operator.learner.AbstractLearner;
import com.rapidminer.operator.learner.PredictionModel;
import com.rapidminer.operator.learner.SimplePredictionModel;
import com.rapidminer.parameter.ParameterType;
import com.rapidminer.parameter.ParameterTypeCategory;
import com.rapidminer.parameter.UndefinedParameterError;
import com.rapidminer.tools.Tools;


/**
 * A DecisionStump clone that allows to specify different utility functions. It is quick for nominal
 * attributes, but does not yet apply pruning for continuous attributes. Currently it can only
 * handle boolean class labels.
 *
 * @author Martin Scholz
 *
 * @deprecated This learner is not used anymore.
 */
@Deprecated
public class MultiCriterionDecisionStumps extends AbstractLearner {

	private static final String ACC = "accuracy"; // TP + TN = p + N - n ~ p - n
	private static final String ENTROPY = "entropy";
	private static final String SQRT_PN = "sqrt(TP*FP) + sqrt(FN*TN)"; // sqrt(pn) +
	// sqrt((P-p)(N-n))
	private static final String GINI = "gini index"; // sqrt(pn) + sqrt((P-p)(N-n))
	private static final String CHI_SQUARE = "chi square test";

	private static final String[] UTILITY_FUNCTION_LIST = new String[] { ENTROPY, ACC, SQRT_PN, GINI, CHI_SQUARE };
	private static final String PARAMETER_UTILITY_FUNCTION = "utility_function";

	public static class DecisionStumpModel extends SimplePredictionModel {

		private static final long serialVersionUID = -261158567126510415L;

		private final Attribute testAttribute;
		private final double testValue;
		private final boolean prediction;
		private boolean includeNaNs;
		private final boolean numerical;

		// nominal attribute: test is "equals"
		// numerical attribute: test is "<="
		// if true, then the provided prediction is made
		public DecisionStumpModel(Attribute attribute, double testValue, ExampleSet exampleSet, boolean prediction,
				boolean includeNaNs) {
			super(exampleSet, ExampleSetUtilities.SetsCompareOption.USE_INTERSECTION,
					ExampleSetUtilities.TypesCompareOption.ALLOW_SAME_PARENTS);

			this.prediction = prediction;
			this.includeNaNs = includeNaNs;
			this.testAttribute = attribute;
			this.testValue = testValue;
			if (testAttribute == null || !testAttribute.isNominal()) {
				this.numerical = true;
			} else {
				this.numerical = false;
			}
		}

		@Override
		public double predict(Example example) {
			boolean evaluatesToTrue;

			if (this.testAttribute == null) {
				evaluatesToTrue = true;
			} else {
				double exampleValue = example.getValue(testAttribute);
				if (Double.isNaN(exampleValue)) {
					evaluatesToTrue = includeNaNs;
				} else if (this.numerical) {
					evaluatesToTrue = example.getValue(testAttribute) <= testValue;
				} else {
					evaluatesToTrue = example.getValue(testAttribute) == testValue;
				}
			}

			if (evaluatesToTrue == prediction) {
				return this.getLabel().getMapping().getPositiveIndex();
			} else {
				return this.getLabel().getMapping().getNegativeIndex();
			}
		}

		@Override
		protected boolean supportsConfidences(Attribute label) {
			return false;
		}

		/** @return a <code>String</code> representation of this rule model. */
		@Override
		public String toString() {
			String posIndexS = getLabel().getMapping().getPositiveString();
			String negIndexS = getLabel().getMapping().getNegativeString();

			StringBuffer result = new StringBuffer(super.toString());
			result.append(Tools.getLineSeparator() + " (" + this.getLabel().getName() + "=");
			result.append((prediction ? posIndexS : negIndexS) + ") <-- ");
			result.append(testAttribute != null ? testAttribute.getName()
					+ (numerical ? " <= " + testValue : " = " + testAttribute.getMapping().mapIndex((int) testValue)) : "");
			result.append(Tools.getLineSeparator() + " unknown: predict '" + (includeNaNs ? posIndexS : negIndexS) + "'.");
			return result.toString();
		}
	}

	private int posIndex;
	private double globalP;
	private double globalN;

	private Model bestModel;
	private double bestScore;

	private String utilityFunction;

	public MultiCriterionDecisionStumps(OperatorDescription description) {
		super(description);
	}

	@Override
	public Class<? extends PredictionModel> getModelClass() {
		return DecisionStumpModel.class;
	}

	@Override
	public boolean supportsCapability(OperatorCapability lc) {
		if (lc == com.rapidminer.operator.OperatorCapability.NUMERICAL_ATTRIBUTES) {
			return true;
		}
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
		this.bestModel = null;
		this.bestScore = Double.NEGATIVE_INFINITY;
	}

	/** @return the best decision stump found */
	protected Model getBestModel() {
		return this.bestModel;
	}

	private void setBestModel(DecisionStumpModel model, double score) {
		this.bestModel = model;
		this.bestScore = score;
	}

	@Override
	public Model learn(ExampleSet exampleSet) throws OperatorException {
		this.utilityFunction = UTILITY_FUNCTION_LIST[this.getParameterAsInt(PARAMETER_UTILITY_FUNCTION)];
		this.initHighscore();

		this.posIndex = exampleSet.getAttributes().getLabel().getMapping().getPositiveIndex();

		double[] globalCounts = this.computePriors(exampleSet);
		this.globalP = globalCounts[0];
		this.globalN = globalCounts[1];

		{ // init with better on eof the default models
			boolean defaultModelPrecition = this.getScore(globalCounts, true) >= this.getScore(globalCounts, false);

			this.setBestModel(new DecisionStumpModel(null, 0, exampleSet, defaultModelPrecition, true),
					this.getScore(globalCounts, defaultModelPrecition));
		}

		this.evaluateNominalAttributes(exampleSet);
		this.evaluateNumericalAttributes(exampleSet);

		return this.getBestModel();
	}

	private void evaluateNumericalAttributes(ExampleSet exampleSet) throws OperatorException {
		int numAttr = exampleSet.getAttributes().size();
		int[] mapAttribToIndex = new int[numAttr];
		Attribute[] mapIndexToAttrib = new Attribute[numAttr];

		int index = 0;
		{
			int i = 0;
			for (Attribute attribute : exampleSet.getAttributes()) {
				if (!attribute.isNominal()) {
					mapIndexToAttrib[index] = attribute;
					mapAttribToIndex[i] = index++;
				} else {
					mapAttribToIndex[i] = -1;
				}
				i++;
			}

		}
		if (index == 0) {
			return;
		}

		boolean hasWeight = exampleSet.getAttributes().getWeight() != null;
		double[][] weightedLabel = new double[exampleSet.size()][2];
		double[][][] values = new double[index][exampleSet.size()][];

		Iterator<Example> reader = exampleSet.iterator();
		int exampleNum = 0;
		double[] weightedPriors = new double[2];
		while (reader.hasNext()) {
			Example example = reader.next();
			int label = example.getLabel() == posIndex ? 0 : 1;
			double weight = hasWeight ? example.getWeight() : 1.0d;
			weightedPriors[label] += weight;
			weightedLabel[exampleNum] = new double[] { label, weight };

			for (int i = 0; i < index; i++) {
				double attribValue = example.getValue(mapIndexToAttrib[i]);
				values[i][exampleNum] = new double[] { attribValue, exampleNum };
			}
			exampleNum++;
		}

		final boolean predictNaN = weightedPriors[0] >= weightedPriors[1];

		Comparator<double[]> cmp = new Comparator<double[]>() {

			@Override
			public int compare(double[] arg0, double[] arg1) {
				return Double.compare(arg0[0], arg1[0]);
			}
		};

		for (int i = 0; i < index; i++) {
			final Attribute currentAttribute = mapIndexToAttrib[i];
			final double[][] currentAttributeValues = values[i];
			Arrays.sort(currentAttributeValues, cmp);

			final double counts[] = new double[exampleSet.getAttributes().getLabel().getMapping().size()];

			double lastValue = Double.NEGATIVE_INFINITY;
			double lastScore = Double.NEGATIVE_INFINITY;
			boolean betterPrediction = false;

			for (int j = 0; j < currentAttributeValues.length; j++) {
				final double curAttribValue = currentAttributeValues[j][0];
				if (Double.isNaN(curAttribValue) || curAttribValue == Double.POSITIVE_INFINITY) {
					break;
				}

				final int curExampleNumber = (int) currentAttributeValues[j][1];
				final int curLabel = (int) weightedLabel[curExampleNumber][0];
				final double curWeight = weightedLabel[curExampleNumber][1];

				if (curAttribValue != lastValue && lastScore > this.bestScore) {
					double testValue = (curAttribValue + lastValue) / 2.0d;
					boolean includeNaNs = predictNaN == betterPrediction;
					DecisionStumpModel dsm = new DecisionStumpModel(currentAttribute, testValue, exampleSet,
							betterPrediction, includeNaNs);
					this.setBestModel(dsm, lastScore);
				}

				counts[curLabel] += curWeight;
				double scorePos = this.getScore(counts, true);
				double scoreNeg = this.getScore(counts, false);
				lastScore = Math.max(scorePos, scoreNeg);
				betterPrediction = scorePos >= scoreNeg;
				lastValue = curAttribValue;
			}
		}

	}

	private void evaluateNominalAttributes(ExampleSet exampleSet) throws OperatorException {
		int numAttr = exampleSet.getAttributes().size();
		int[] mapAttribToIndex = new int[numAttr];
		Attribute[] mapIndexToAttrib = new Attribute[numAttr];

		int index = 0;
		{
			int i = 0;
			for (Attribute attribute : exampleSet.getAttributes()) {
				if (attribute.isNominal()) {
					mapIndexToAttrib[index] = attribute;
					mapAttribToIndex[i] = index++;
				} else {
					mapAttribToIndex[i] = -1;
				}
				i++;
			}

		}
		if (index == 0) {
			return;
		}

		double[][][] counter = new double[index][][];
		double[][] countNaNs = new double[index][exampleSet.getAttributes().getLabel().getMapping().size()];
		for (int i = 0; i < index; i++) {
			int numValues = mapIndexToAttrib[i].getMapping().size();
			counter[i] = new double[numValues][exampleSet.getAttributes().getLabel().getMapping().size()];
		}

		Attribute weightAttr = exampleSet.getAttributes().getWeight();
		Iterator<Example> reader = exampleSet.iterator();
		while (reader.hasNext()) {
			Example example = reader.next();
			double weight = weightAttr == null ? 1.0d : example.getWeight();

			int label = example.getLabel() == posIndex ? 0 : 1;

			for (int i = 0; i < index; i++) {
				double attributeValue = example.getValue(mapIndexToAttrib[i]);
				if (Double.isNaN(attributeValue)) {
					countNaNs[i][label] += weight;
				} else {
					counter[i][(int) attributeValue][label] += weight;
				}
			}
		}

		for (int i = 0; i < index; i++) {
			double[][] attributeMatrix = counter[i];
			for (int j = 0; j < attributeMatrix.length; j++) {
				ScoreNaNInfo snp = this.getScore(attributeMatrix[j], countNaNs[i]);
				if (snp.score > this.bestScore) {
					Attribute attribute = mapIndexToAttrib[i];
					double testValue = j;
					this.setBestModel(new DecisionStumpModel(attribute, testValue, exampleSet, snp.predicted,
							snp.includeNaNs), snp.score);
				}
			}
		}
	}

	// Helper class.
	private static class ScoreNaNInfo {

		public double score;
		public boolean includeNaNs;
		public boolean predicted;

		ScoreNaNInfo(double score, boolean includeNaNs, boolean predicted) {
			this.score = score;
			this.includeNaNs = includeNaNs;
			this.predicted = predicted;
		}

		public ScoreNaNInfo max(ScoreNaNInfo other) {
			if (this.score >= other.score) {
				return this;
			} else {
				return other;
			}
		}
	}

	// Evaluate all four combinations: with and without including NaNs, predicting pos or neg class
	private ScoreNaNInfo getScore(double[] counts, double[] countNaNs) throws UndefinedParameterError {
		ScoreNaNInfo snp, snp2;

		// exclude NaNs, predict true
		double score = this.getScore(counts, true);
		snp = new ScoreNaNInfo(score, false, true);

		// exclude NaNs, predict false
		score = this.getScore(counts, false);
		snp2 = new ScoreNaNInfo(score, false, false);
		snp = snp.max(snp2);

		if (countNaNs[0] > 0 || countNaNs[1] > 0) {
			counts[0] += countNaNs[0];
			counts[1] += countNaNs[1];

			// include NaNs, predict true
			score = this.getScore(counts, true);
			snp2 = new ScoreNaNInfo(score, true, true);
			snp = snp.max(snp2);

			// include NaNs, predict false
			score = this.getScore(counts, false);
			snp2 = new ScoreNaNInfo(score, true, false);
			snp = snp.max(snp2);
		}

		return snp;
	}

	/**
	 * Computes the score for the specified utility function, the provided counts and class.
	 */
	protected double getScore(double[] counts, boolean predictPositives) {
		double p = counts[0];
		double n = counts[1];

		double score;

		if (this.utilityFunction.equals(ACC)) {
			score = predictPositives ? p - n : n - p;
		} else if (this.utilityFunction.equals(ENTROPY)) {
			if (p - n >= 0 ^ predictPositives) {
				return Double.NEGATIVE_INFINITY;
			}

			double cov = p + n;
			double uncov = globalP + globalN - cov;

			double scoreCovered = cov == 0 ? 0 : entropyLog2(p / cov) + entropyLog2(n / cov);
			double scoreUncovered = uncov == 0 ? 0 : entropyLog2((globalP - p) / uncov) + entropyLog2((globalN - n) / uncov);

			score = (cov * scoreCovered + uncov * scoreUncovered) / (cov + uncov);
			score = -score; // maximization problem assumed
		} else if (this.utilityFunction.equals(SQRT_PN)) {
			if (p - n >= 0 ^ predictPositives) {
				return Double.NEGATIVE_INFINITY;
			}

			score = Math.sqrt(p * n) + Math.sqrt((globalP - p) * (globalN - n));
			score = -score; // maximization problem assumed
		} else if (this.utilityFunction.equals(GINI)) {
			if (p - n >= 0 ^ predictPositives) {
				return Double.NEGATIVE_INFINITY;
			}

			double cov = p + n;
			double uncov = globalP + globalN - cov;

			double scoreCovered = cov == 0 ? 0 : p / cov * (n / cov);
			double scoreUncovered = uncov == 0 ? 0 : (globalP - p) / uncov * ((globalN - n) / uncov);

			score = (cov * scoreCovered + uncov * scoreUncovered) / (cov + uncov);
			score = -score; // maximization problem assumed
		} else if (this.utilityFunction.equals(CHI_SQUARE)) {
			double q = globalP - p;
			double r = globalN - n;
			double cov = p + n;
			double uncov = q + r;
			double total = cov + uncov;

			double c11, c12, c21, c22;
			c11 = cov * globalP / total;
			c12 = cov * globalN / total;
			c21 = uncov * globalP / total;
			c22 = uncov * globalN / total;

			if (cov > 0 && uncov > 0) {
				score = (p - c11) * (p - c11) / c11 + (n - c12) * (n - c12) / c12 + (q - c21) * (q - c21) / c21 + (r - c22)
						* (r - c22) / c22;
			} else {
				score = 0;
			}
		} else {
			score = Double.NaN;
			logWarning("Found unknown utility function: " + this.utilityFunction);
		}

		return score;
	}

	// more intuitive than log_e, although it should make no difference
	private double entropyLog2(double p) {
		if (Double.isNaN(p) || p == 0) { // NaN may e.g. occur when coverage is 0
			return 0;
		} else {
			return -p * Math.log(p) / Math.log(2.0d);
		}
	}

	/**
	 * @param exampleSet
	 *            the exampleSet to get the weighted priors for
	 * @return a double[2] object, first parameter is p, second is n.
	 */
	protected double[] computePriors(ExampleSet exampleSet) {
		Attribute weightAttr = exampleSet.getAttributes().getWeight();
		double p = 0;
		double n = 0;
		Iterator<Example> reader = exampleSet.iterator();
		while (reader.hasNext()) {
			Example example = reader.next();
			double weight = weightAttr == null ? 1 : example.getValue(weightAttr);
			if (example.getLabel() == posIndex) {
				p += weight;
			} else {
				n += weight;
			}
		}
		return new double[] { p, n };
	}

	/**
	 * Adds the parameter &utility function&quot;.
	 */
	@Override
	public List<ParameterType> getParameterTypes() {
		List<ParameterType> types = super.getParameterTypes();
		types.add(new ParameterTypeCategory(PARAMETER_UTILITY_FUNCTION, "The function to be optimized by the rule.",
				UTILITY_FUNCTION_LIST, 0));
		return types;
	}
}
