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
package com.rapidminer.operator.learner.igss;

import java.util.Iterator;
import java.util.LinkedList;
import java.util.List;
import java.util.ListIterator;

import com.rapidminer.example.Attribute;
import com.rapidminer.example.Example;
import com.rapidminer.example.ExampleSet;
import com.rapidminer.example.Tools;
import com.rapidminer.operator.Model;
import com.rapidminer.operator.OperatorCapability;
import com.rapidminer.operator.OperatorDescription;
import com.rapidminer.operator.OperatorException;
import com.rapidminer.operator.UserError;
import com.rapidminer.operator.learner.AbstractLearner;
import com.rapidminer.operator.learner.PredictionModel;
import com.rapidminer.operator.learner.igss.hypothesis.GSSModel;
import com.rapidminer.operator.learner.igss.hypothesis.Hypothesis;
import com.rapidminer.operator.learner.igss.hypothesis.Rule;
import com.rapidminer.operator.learner.igss.utility.Accuracy;
import com.rapidminer.operator.learner.igss.utility.Binomial;
import com.rapidminer.operator.learner.igss.utility.Linear;
import com.rapidminer.operator.learner.igss.utility.Squared;
import com.rapidminer.operator.learner.igss.utility.Utility;
import com.rapidminer.operator.learner.igss.utility.WRAcc;
import com.rapidminer.operator.learner.meta.BayBoostBaseModelInfo;
import com.rapidminer.operator.learner.meta.BayBoostModel;
import com.rapidminer.operator.learner.meta.ContingencyMatrix;
import com.rapidminer.operator.learner.meta.WeightedPerformanceMeasures;
import com.rapidminer.parameter.ParameterType;
import com.rapidminer.parameter.ParameterTypeBoolean;
import com.rapidminer.parameter.ParameterTypeCategory;
import com.rapidminer.parameter.ParameterTypeDouble;
import com.rapidminer.parameter.ParameterTypeInt;
import com.rapidminer.tools.RandomGenerator;


/**
 * This operator implements the IteratingGSS algorithms presented in the diploma thesis 'Effiziente
 * Entdeckung unabh&auml;ngiger Subgruppen in grossen Datenbanken' at the Department of Computer
 * Science, University of Dortmund.
 *
 * @author Dirk Dach
 *
 * @deprecated This learner is not used anymore.
 */
@Deprecated
public class IteratingGSS extends AbstractLearner {

	/** The parameter name for &quot;approximation parameter&quot; */
	public static final String PARAMETER_EPSILON = "epsilon";

	/** The parameter name for &quot;desired confidence&quot; */
	public static final String PARAMETER_DELTA = "delta";

	/** The parameter name for &quot;minimum utility used for pruning&quot; */
	public static final String PARAMETER_MIN_UTILITY_PRUNING = "min_utility_pruning";

	/** The parameter name for &quot;minimum utility for the usefulness of a rule&quot; */
	public static final String PARAMETER_MIN_UTILITY_USEFUL = "min_utility_useful";

	/**
	 * The parameter name for &quot;the number of examples drawn before the next hypothesis
	 * update&quot;
	 */
	public static final String PARAMETER_STEPSIZE = "stepsize";

	/**
	 * The parameter name for &quot;the number of examples a hypothesis must cover before normal
	 * approximation is used&quot;
	 */
	public static final String PARAMETER_LARGE = "large";

	/** The parameter name for &quot;the maximum complexity of hypothesis&quot; */
	public static final String PARAMETER_MAX_COMPLEXITY = "max_complexity";

	/** The parameter name for &quot;the minimum complexity of hypothesis&quot; */
	public static final String PARAMETER_MIN_COMPLEXITY = "min_complexity";

	/** The parameter name for &quot;the number of iterations&quot; */
	public static final String PARAMETER_ITERATIONS = "iterations";

	/**
	 * The parameter name for &quot;Switch to binomial utility funtion before increasing
	 * complexity&quot;
	 */
	public static final String PARAMETER_USE_BINOMIAL = "use_binomial";

	/** The parameter name for &quot;the utility function to be used&quot; */
	public static final String PARAMETER_UTILITY_FUNCTION = "utility_function";

	/** The parameter name for &quot;use kbs to reweight examples after each iteration&quot; */
	public static final String PARAMETER_USE_KBS = "use_kbs";

	/** The parameter name for &quot;use rejection sampling instead of weighted examples&quot; */
	public static final String PARAMETER_REJECTION_SAMPLING = "rejection_sampling";

	/** The parameter name for &quot;criterion to decide if the complexity is increased &quot; */
	public static final String PARAMETER_USEFUL_CRITERION = "useful_criterion";

	/**
	 * The parameter name for &quot;used by example criterion to determine usefulness of a
	 * hypothesis&quot;
	 */
	public static final String PARAMETER_EXAMPLE_FACTOR = "example_factor";

	/** The parameter name for &quot;make all iterations even if termination criterion is met&quot; */
	public static final String PARAMETER_FORCE_ITERATIONS = "force_iterations";

	/** The parameter name for &quot;generate h->Y+/Y- or h->Y+ only.&quot; */
	public static final String PARAMETER_GENERATE_ALL_HYPOTHESIS = "generate_all_hypothesis";

	/** The parameter name for &quot;Set weights back to 1 when complexity is increased.&quot; */
	public static final String PARAMETER_RESET_WEIGHTS = "reset_weights";
	public static final String[] CRITERION_TYPES = { "worst_utility", "utility", "best_utility", "example" };

	public static final int FIRST_TYPE_INDEX = 0;

	public static final int TYPE_WORST_UTILITY = 0;

	public static final int TYPE_UTILITY = 1;

	public static final int TYPE_BEST_UTILITY = 2;

	public static final int TYPE_EXAMPLE = 3;

	public static final int LAST_TYPE_INDEX = 3;

	/** stores all results */
	private IGSSResult gssResult;

	/** The regular atributes */
	private Attribute[] regularAttributes;

	/** The label attribute */
	private Attribute label;

	/** The utility function */
	private Utility theUtility;

	/** global random generator */
	private RandomGenerator random;

	/** First hypothesis used to create all others. */
	private Hypothesis seed;

	/** Total weight used by GSS */
	private double totalWeight;

	/** Total positive weight used by GSS */
	private double totalPositiveWeight;

	/** Stores the k-best hypothesis. */
	private LinkedList<Hypothesis> bestList;

	/** Worst of the k best hypothesis */
	private Result minBest;

	/** Best of the hypothesis not among the k best */
	private Result maxRest;

	/** Parameter k of the GSS algorithm */
	private int numberOfSolutions;

	/** Remaining delta */
	private double currentDelta;

	/** Parameter epsilon of the GSS algorithm */
	private double epsilon;

	/** Parameter stepsize of the IGSS algorithm */
	private int stepsize;

	/** Maximum hypothesis complexity */
	private int maxComplexity;

	/** Minimum hypothesis complexity */
	private int minComplexity;

	/** Minimum utility used for pruning */
	private double min_utility_pruning;

	/** Minimum utility needed for a utility to be useful */
	private double min_utility_useful;

	/** Indicates if kbs should be used. */
	private boolean useKBS;

	/** Indicates if Binomial should be used before increasing complexity. */
	private boolean useBinomial;

	/** the useful criterion for the IGSS algorithm */
	private int useful_criterion;

	/** Always make all iterations? */
	private boolean forceIterations;

	/** Reset weights after complexity increase? */
	private boolean resetWeights;

	/** Factor needed by example_criterion. */
	private double exampleFactor;

	/** minimal model number for example_criterion */
	public int MIN_MODEL_NUMBER = 2;

	/** Use rejection sampling or weights directly. */
	private boolean rejectionSampling;

	/** Number of random experiments before a normal approximation is used. */
	private int large;

	/** The number of iterations for the IGSS algorithm. */
	private int iterations;

	/** Must pass the given object to the superclass. */
	public IteratingGSS(OperatorDescription description) {
		super(description);
	}

	/** Updates bestList,bestRest and minBest */
	private void updateLists(LinkedList<Hypothesis> hypothesisList, int n, double totalExampleWeight,
			double totalPositiveWeight, double delta_h_m) {
		this.bestList = new LinkedList<Hypothesis>();
		LinkedList<Result> bestList = new LinkedList<Result>(); // local variable covers global
		// field!!
		LinkedList<Result> restList = new LinkedList<Result>();
		this.minBest = null;
		this.maxRest = null;

		// Find n rules with best empirical utility and partition into bestList and restList
		for (Hypothesis hypo : hypothesisList) {
			if (hypo.getCoveredWeight() > 0.0d) {
				if (bestList.size() < n) {
					if (bestList.isEmpty()) {
						double util = theUtility.utility(totalExampleWeight, totalPositiveWeight, hypo);
						double conf = theUtility.confidenceIntervall(totalExampleWeight, totalPositiveWeight, hypo,
								delta_h_m);
						bestList.addLast(new Result(hypo, totalExampleWeight, totalPositiveWeight, util, conf));
					} else {
						double util = theUtility.utility(totalExampleWeight, totalPositiveWeight, hypo);
						double conf = theUtility.confidenceIntervall(totalExampleWeight, totalPositiveWeight, hypo,
								delta_h_m);
						ListIterator<Result> listIterator = bestList.listIterator(0);
						while (listIterator.hasNext()) {
							Result current = listIterator.next();
							if (util > current.getUtility()) {
								listIterator.previous();
								break;
							}
						}
						listIterator.add(new Result(hypo, totalExampleWeight, totalPositiveWeight, util, conf));
					}
				} else {
					double util = theUtility.utility(totalExampleWeight, totalPositiveWeight, hypo);
					double conf = theUtility.confidenceIntervall(totalExampleWeight, totalPositiveWeight, hypo, delta_h_m);
					if (util > bestList.getLast().getUtility()) {
						ListIterator<Result> listIterator = bestList.listIterator(0);
						while (listIterator.hasNext()) {
							Result current = listIterator.next();
							if (util > current.getUtility()) {
								listIterator.previous();
								break;
							}
						}
						listIterator.add(new Result(hypo, totalExampleWeight, totalPositiveWeight, util, conf));
						restList.addLast(bestList.removeLast());
					} else {
						restList.addLast(new Result(hypo, totalExampleWeight, totalPositiveWeight, util, conf));
					}
				}
			} else {
				double util = theUtility.utility(totalExampleWeight, totalPositiveWeight, hypo);
				double conf = theUtility.confidenceIntervall(totalExampleWeight, totalPositiveWeight, hypo, delta_h_m);
				restList.addLast(new Result(hypo, totalExampleWeight, totalPositiveWeight, util, conf));
			}
		}

		// Find min(bestList)
		Result r = bestList.getLast();
		double minimum = r.getUtility() - r.getConfidence();
		this.minBest = r;
		for (Result res : bestList) {
			double current = res.getUtility() - res.getConfidence();
			if (current < minimum) {
				minimum = current;
				this.minBest = res;
			}
		}
		// Find max(restList)
		r = restList.getLast();
		double maximum = r.getUtility() + r.getConfidence();
		this.maxRest = r;
		for (Result res : restList) {
			double current = res.getUtility() + res.getConfidence();
			if (current > maximum) {
				maximum = current;
				this.maxRest = res;
			}
		}

		for (Result res : bestList) {
			this.bestList.addLast(res.getHypothesis()); // Add hypothesis from
			// local variable to
			// global field.
		}
	}

	/** Returns the n best hypothesis with maximum error epsilon with confidence 1-delta. */
	public LinkedList<Result> gss(ExampleSet exampleSet, LinkedList<Hypothesis> hypothesisList, double delta, double epsilon)
			throws OperatorException {
		// Initialization.
		LinkedList<Hypothesis> delete = new LinkedList<Hypothesis>();// Stores deleted hypothesis.
		LinkedList<Hypothesis> output = new LinkedList<Hypothesis>();// Stores hypothesis that
		// became output.
		LinkedList<Result> results = new LinkedList<Result>();// Stores the results.
		this.bestList = new LinkedList<Hypothesis>();
		int n = this.numberOfSolutions;
		this.totalWeight = 0.0d;
		this.totalPositiveWeight = 0.0d;

		// Calculate m and current delta value
		double delta_h = delta / (2.0d * hypothesisList.size());
		double m = theUtility.calculateM(delta_h, epsilon);
		double delta_h_m = delta / (2.0d * hypothesisList.size() * Math.ceil(m / stepsize));

		double r = 0.0d;
		double weightToAdd = 1.0d;
		int nextUpdateValue = stepsize;

		// Draw random examples and apply all rules to each example.
		do {
			// Query a random example.
			int rand = random.nextInt(exampleSet.size());
			Example e = exampleSet.getExample(rand);

			// Get random value from [0,1] if rejection samplingis used or
			// the correct weight to add if normal weights are uesed.
			if (this.rejectionSampling) {
				r = random.nextDouble();
			} else {
				weightToAdd = e.getWeight();
			}
			if (r <= e.getWeight()) {

				// Apply the given example to all rules in the hypothesisList.
				hypothesisList.forEach(h -> h.apply(e));
				totalWeight += weightToAdd;
				if ((int) e.getLabel() == Hypothesis.POSITIVE_CLASS) {
					totalPositiveWeight += weightToAdd;
				}

				// Update rules that already became output. They are needed for pruning.
				output.forEach(h -> h.apply(e));

				// Update already removed rules. They are needed for pruning.
				delete.forEach(h -> h.apply(e));

				/*
				 * Update the utility of all rules and determine the best rule and output/delete
				 * rules that are good/bad enough. Only once per stepsize!
				 */
				if ((int) totalWeight >= nextUpdateValue) {
					nextUpdateValue += stepsize;
					updateLists(hypothesisList, n, totalWeight, totalPositiveWeight, delta_h_m);

					// Look for hypothesis to delete/output.
					ListIterator<Hypothesis> iter = hypothesisList.listIterator();
					while (iter.hasNext() && n > 0 && hypothesisList.size() != n) {
						Hypothesis hypo = iter.next();
						double util = theUtility.utility(totalWeight, totalPositiveWeight, hypo);
						double conf = theUtility.confidenceIntervall(totalWeight, totalPositiveWeight, hypo, delta_h_m);
						if (util >= conf + maxRest.getUtility() + maxRest.getConfidence() - epsilon
								&& bestList.contains(hypo)) {
							results.addLast(new Result(hypo.clone(), totalWeight, totalPositiveWeight, util, conf));
							output.addLast(hypo);
							iter.remove();
							n--;

							// Adapt values to new hypothesis list size.
							delta_h = delta / (2.0d * hypothesisList.size());
							delta_h_m = delta / (2.0d * hypothesisList.size() * Math.ceil(m / stepsize));
							if (n != 0) {
								this.updateLists(hypothesisList, n, totalWeight, totalPositiveWeight, delta_h_m);
							}
						} else if (util <= minBest.getUtility() - minBest.getConfidence() - conf) {
							delete.addLast(hypo);
							iter.remove();

							// Adapt values to new hypothesis list size.
							delta_h = delta / (2.0d * hypothesisList.size());
							delta_h_m = delta / (2.0d * hypothesisList.size() * Math.ceil(m / stepsize));
							if (hypo.equals(maxRest.getHypothesis()) && hypothesisList.size() > n) {
								this.updateLists(hypothesisList, n, totalWeight, totalPositiveWeight, delta_h_m);
							}
						}
					}

				}
			}
		} while (!(n == 0 || hypothesisList.size() == n || theUtility.confidenceIntervall(totalWeight, delta_h) <= epsilon / 2.0));

		if (n > 0) {
			// Add hypothesis to result that are still in the best list after the loop is exited.
			// Their confidence intervall is guranteed to be epsilon/2 at most.
			if (bestList.isEmpty()) { // Happens for big epsilon; then M<stepsize.
				updateLists(hypothesisList, n, totalWeight, totalPositiveWeight, delta_h_m);
			}

			while (!bestList.isEmpty()) {
				Hypothesis hypo = bestList.removeFirst();
				double util = theUtility.utility(totalWeight, totalPositiveWeight, hypo);
				double conf = theUtility.confidenceIntervall(totalWeight, totalPositiveWeight, hypo, delta_h_m); // confidence
				// calculated
				// for
				// bestList

				if (conf > epsilon / 2.0) {
					conf = epsilon / 2.0;
				}
				results.addLast(new Result(hypo.clone(), totalWeight, totalPositiveWeight, util, conf));
			}
		} else {
			this.currentDelta = this.currentDelta + delta / 2.0d;
		}

		hypothesisList.addAll(delete); // Add deleted rules again, because all rules are needed for
		// pruning.
		hypothesisList.addAll(output); // Add output rules again, because all rules are needed for
		// pruning.

		return results;

	}

	/**
	 * Reweights the examples according to knowledge based sampling. Normalizes weights to [0,1] if
	 * the parameter normalize is set to true.
	 */
	public ContingencyMatrix reweight(ExampleSet exampleSet, Model model, boolean normalize) throws OperatorException {
		exampleSet = model.apply(exampleSet); // apply and create predicted label
		WeightedPerformanceMeasures wpm = new WeightedPerformanceMeasures(exampleSet);
		WeightedPerformanceMeasures.reweightExamples(exampleSet, wpm.getContingencyMatrix(), false);

		if (normalize) {
			double maxWeight = Double.NEGATIVE_INFINITY;
			Iterator<Example> reader = exampleSet.iterator();
			while (reader.hasNext()) {
				Example e = reader.next();
				if (e.getWeight() > maxWeight) {
					maxWeight = e.getWeight();
				}
			}
			// Normalize with maximum weight.
			reader = exampleSet.iterator();
			while (reader.hasNext()) {
				Example e = reader.next();
				e.setValue(e.getAttributes().getWeight(), e.getWeight() / maxWeight);
			}
		}

		PredictionModel.removePredictedLabel(exampleSet);
		return wpm.getContingencyMatrix();
	}

	@Override
	public Model learn(ExampleSet eSet) throws OperatorException {
		// Initialization
		this.random = RandomGenerator.getGlobalRandomGenerator();
		this.epsilon = getParameterAsDouble(PARAMETER_EPSILON);
		this.currentDelta = getParameterAsDouble(PARAMETER_DELTA);
		this.stepsize = getParameterAsInt(PARAMETER_STEPSIZE);
		this.large = getParameterAsInt(PARAMETER_LARGE);
		this.useKBS = getParameterAsBoolean(PARAMETER_USE_KBS);
		this.rejectionSampling = getParameterAsBoolean(PARAMETER_REJECTION_SAMPLING);
		this.numberOfSolutions = 1;
		this.iterations = getParameterAsInt(PARAMETER_ITERATIONS);
		this.useful_criterion = getParameterAsInt(PARAMETER_USEFUL_CRITERION);
		this.min_utility_pruning = getParameterAsDouble(PARAMETER_MIN_UTILITY_PRUNING);
		this.min_utility_useful = getParameterAsDouble(PARAMETER_MIN_UTILITY_USEFUL);
		this.useBinomial = getParameterAsBoolean(PARAMETER_USE_BINOMIAL);
		this.maxComplexity = getParameterAsInt(PARAMETER_MAX_COMPLEXITY);
		this.minComplexity = getParameterAsInt(PARAMETER_MIN_COMPLEXITY);
		this.forceIterations = getParameterAsBoolean(PARAMETER_FORCE_ITERATIONS);
		this.resetWeights = getParameterAsBoolean(PARAMETER_RESET_WEIGHTS);
		this.exampleFactor = getParameterAsDouble(PARAMETER_EXAMPLE_FACTOR);

		if (this.minComplexity > this.maxComplexity) {
			throw new UserError(this, 116, "max_complexity", this.maxComplexity);
		}

		// Initialize label, weight.
		this.label = eSet.getAttributes().getLabel();
		Tools.createWeightAttribute(eSet);

		// Initialize Result.
		this.gssResult = new IGSSResult(eSet);

		// Initialize regularAttributes;
		regularAttributes = new Attribute[eSet.getAttributes().size()];
		int counter = 0;
		for (Attribute attribute : eSet.getAttributes()) {
			regularAttributes[counter++] = attribute;
		}

		// Initialize hypothesis space
		this.seed = new Rule(regularAttributes, label, this.rejectionSampling,
				getParameterAsBoolean(PARAMETER_GENERATE_ALL_HYPOTHESIS));

		// Initialize utility function.
		int utility_type = getParameterAsInt(PARAMETER_UTILITY_FUNCTION);
		switch (utility_type) {
			case Utility.TYPE_ACCURACY:
				theUtility = new Accuracy(gssResult.getPriors(), large);
				break;
			case Utility.TYPE_LINEAR:
				theUtility = new Linear(gssResult.getPriors(), large);
				break;
			case Utility.TYPE_SQUARED:
				theUtility = new Squared(gssResult.getPriors(), large);
				break;
			case Utility.TYPE_BINOMIAL:
				theUtility = new Binomial(gssResult.getPriors(), large);
				break;
			case Utility.TYPE_WRACC:
				theUtility = new WRAcc(gssResult.getPriors(), large);
				break;
			default:
		}

		Model model = learn2(eSet);
		return model;
	}

	private Model learn2(ExampleSet exampleSet) throws OperatorException {
		LinkedList<Hypothesis> hypothesisList = seed.init(this.minComplexity);
		LinkedList<Model> allModels = new LinkedList<Model>();
		int currentComplexity = this.minComplexity;
		boolean binomialTestPerformed = false;
		boolean switchedInThisIteration = false;
		Utility utilityStorage = this.theUtility; // needed if use_binomial=true
		LinkedList<BayBoostBaseModelInfo> modelInfo = new LinkedList<BayBoostBaseModelInfo>();
		LinkedList<Result> allResultsOfCurrentComplexity = new LinkedList<Result>();
		LinkedList<Hypothesis> deletedHypothesis = new LinkedList<Hypothesis>(); // stores deleted
		// hypos if no
		// kbs is used.

		for (int i = 0; i < this.iterations; i++) {
			// Reset hypothesis space
			hypothesisList.forEach(Hypothesis::reset);

			// calculate current delta values
			double deltaForGSS = 2.0d * this.currentDelta / (3.0d * (this.iterations - i));
			double deltaForPruning = this.currentDelta / (3.0d * (this.iterations - i));
			this.currentDelta = this.currentDelta - deltaForGSS - deltaForPruning;

			LinkedList<Result> currentResults = new LinkedList<Result>();
			currentResults.addAll(gss(exampleSet, hypothesisList, deltaForGSS, epsilon));

			// Create model.
			Hypothesis h = currentResults.getFirst().getHypothesis();
			double[] precisions = new double[2];
			if (h.getPrediction() == Hypothesis.POSITIVE_CLASS) {
				precisions[Hypothesis.POSITIVE_CLASS] = h.getPositiveWeight() / h.getCoveredWeight();
				precisions[Hypothesis.NEGATIVE_CLASS] = 1.0d - precisions[1];
			} else {
				precisions[Hypothesis.NEGATIVE_CLASS] = h.getPositiveWeight() / h.getCoveredWeight();
				precisions[Hypothesis.POSITIVE_CLASS] = 1.0d - precisions[1];
			}
			GSSModel model = new GSSModel(exampleSet, h, precisions);

			boolean increaseComplexity = false;
			// Test if model is useful according to the criterions in the method 'isUseful' or has
			// already been found.
			if (!isUseful(currentResults.getFirst(), allResultsOfCurrentComplexity, this.useful_criterion, exampleSet,
					this.MIN_MODEL_NUMBER) || allModels.contains(model)) {

				if (!binomialTestPerformed && this.useBinomial) { // Switch to binomial before
					// increasing complexity
					this.theUtility = new Binomial(gssResult.getPriors(), large);
					binomialTestPerformed = true;
					switchedInThisIteration = true;
				} else {
					if (currentComplexity < this.maxComplexity) { // Increase complexity after
						// binomial test if possible.
						increaseComplexity = true;
						currentComplexity++;
						this.theUtility = utilityStorage;
						binomialTestPerformed = false; // Reset for next complexity level.
					} else {
						if (!this.forceIterations) {
							break; // Break for (int i=0;i<this.iterations;i++)
						}
					}

				}

			}

			if (increaseComplexity) { // Do not add result. Prune and increase complexity. Reset
				// weight if resetWeight=true.
				if (!this.useKBS) {
					hypothesisList.addAll(deletedHypothesis); // put back all deleted hypothesis
					deletedHypothesis = new LinkedList<Hypothesis>(); // re-intialize
					// deletedHypothesis
				}
				LinkedList<Hypothesis> prunedList = new LinkedList<Hypothesis>();
				prunedList = this.prune(hypothesisList, min_utility_pruning, this.totalWeight, this.totalPositiveWeight,
						deltaForPruning);
				hypothesisList = new LinkedList<Hypothesis>();
				hypothesisList.addAll(generate(prunedList));
				allResultsOfCurrentComplexity = new LinkedList<Result>();
				if (this.resetWeights) {
					Tools.createWeightAttribute(exampleSet);
				}
			} else { // Add result(not directly after switch to Binomial). No pruning. Create model.
				// Reweight.
				if (!switchedInThisIteration) {
					allModels.addLast(model);
					this.gssResult.addResult(currentResults.getFirst());

					currentDelta = currentDelta + deltaForPruning; // deltaForPruning not needed
					ContingencyMatrix contingencyMatrix = null;
					if (this.useKBS) { // kbs used: reweight!
						contingencyMatrix = reweight(exampleSet, model, this.rejectionSampling);
					} else {// no kbs: don't reweight, remove found hypothesis so that it can't be
						// found again.
						WeightedPerformanceMeasures wpm = new WeightedPerformanceMeasures(exampleSet);
						contingencyMatrix = wpm.getContingencyMatrix();
						int hypoIndex = hypothesisList.indexOf(currentResults.getFirst().getHypothesis());
						deletedHypothesis.addLast(hypothesisList.remove(hypoIndex));
					}
					modelInfo.addLast(new BayBoostBaseModelInfo(model, contingencyMatrix));
					allResultsOfCurrentComplexity.addLast(currentResults.getFirst());
				} else {
					switchedInThisIteration = false;
				}
			}
		}

		// Create BayBoostModel
		double[] priors = new double[2];
		priors[Hypothesis.POSITIVE_CLASS] = gssResult.getPriors()[Hypothesis.POSITIVE_CLASS];
		priors[Hypothesis.NEGATIVE_CLASS] = gssResult.getPriors()[Hypothesis.NEGATIVE_CLASS];

		return new BayBoostModel(exampleSet, modelInfo, priors);
	}

	/** Test if the model is useful according to the given criterion. */
	public boolean isUseful(Result current, LinkedList<Result> otherResults, int criterion, ExampleSet exampleSet,
			int min_model_number) {

		boolean result = true;

		switch (criterion) {

			case IteratingGSS.TYPE_WORST_UTILITY:
				double worstUtility = current.getUtility() - current.getConfidence();
				if (worstUtility < this.min_utility_useful) {
					result = false;
				} else {
					result = true;
				}
				break;

			case IteratingGSS.TYPE_UTILITY:
				double utility = current.getUtility();
				if (utility < this.min_utility_useful) {
					result = false;
				} else {
					result = true;
				}
				break;

			case IteratingGSS.TYPE_BEST_UTILITY:
				double bestUtility = current.getUtility() + current.getConfidence();
				if (bestUtility < this.min_utility_useful) {
					result = false;
				} else {
					result = true;
				}
				break;

			case IteratingGSS.TYPE_EXAMPLE:

				if (otherResults.size() == 0 || otherResults.size() < min_model_number) {
					return true;
				}

				// Calculate average number of examples
				double sum = 0.0d;
				for (Result r : otherResults) {
					sum = sum + r.getTotalWeight();
				}
				double average = sum / otherResults.size();

				if (current.getTotalWeight() < this.exampleFactor * average) {
					result = true;
				} else {
					result = false;
				}
				break;
		}

		return result;
	}

	/**
	 * Prunes the given list of hypothesis. All hypothesis with an upper utility bound less than the
	 * parameter minUtility is pruned.
	 */
	public LinkedList<Hypothesis> prune(LinkedList<Hypothesis> hypoList, double minUtility, double totalWeight,
			double totalPositiveWeight, double delta_p) {
		double delta_hp = delta_p / hypoList.size();
		ListIterator<Hypothesis> it = hypoList.listIterator();
		while (it.hasNext()) {
			Hypothesis hypo = it.next();
			double upperBound = theUtility.getUpperBound(totalWeight, totalPositiveWeight, hypo, delta_hp);
			if (upperBound < minUtility) {
				it.remove();
			}
		}
		return hypoList;
	}

	/** Generates all successors of the hypothesis in the given list. */
	public LinkedList<Hypothesis> generate(LinkedList<Hypothesis> oldHypothesis) {
		LinkedList<Hypothesis> newHypothesis = new LinkedList<Hypothesis>();
		while (!oldHypothesis.isEmpty()) {
			Hypothesis hypo = oldHypothesis.removeFirst();
			if (hypo.canBeRefined()) {
				newHypothesis.addAll(hypo.refine());
			}
		}
		return newHypothesis;

	}

	/** Returns the logarithm to base 2 */
	public static double log2(double arg) {
		return Math.log(arg) / Math.log(2);
	}

	@Override
	public Class<? extends PredictionModel> getModelClass() {
		return BayBoostModel.class;
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
		return false;
	}

	@Override
	public List<ParameterType> getParameterTypes() {
		List<ParameterType> types = super.getParameterTypes();
		ParameterType type = new ParameterTypeInt(PARAMETER_ITERATIONS, "the number of iterations", 1, 50, 10);
		type.setExpert(false);
		types.add(type);
		type = new ParameterTypeBoolean(PARAMETER_FORCE_ITERATIONS,
				"make all iterations even if termination criterion is met", false);
		type.setExpert(false);
		types.add(type);

		types.add(new ParameterTypeDouble(PARAMETER_EPSILON, "approximation parameter", 0.01, 1.0, 0.04));
		types.add(new ParameterTypeDouble(PARAMETER_DELTA, "desired confidence", 0.01, 1.0, 0.1));
		types.add(new ParameterTypeDouble(PARAMETER_MIN_UTILITY_PRUNING, "minimum utility used for pruning", -1.0d, 1.0d,
				0.0d));
		types.add(new ParameterTypeDouble(PARAMETER_MIN_UTILITY_USEFUL, "minimum utility for the usefulness of a rule",
				-1.0, 1.0, 0.0d));
		types.add(new ParameterTypeInt(PARAMETER_STEPSIZE, "the number of examples drawn before the next hypothesis update",
				1, 10000, 100));
		types.add(new ParameterTypeInt(PARAMETER_LARGE,
				"the number of examples a hypothesis must cover before normal approximation is used", 1, 10000, 100));
		types.add(new ParameterTypeInt(PARAMETER_MAX_COMPLEXITY, "the maximum complexity of hypothesis", 1, 10, 1));
		types.add(new ParameterTypeInt(PARAMETER_MIN_COMPLEXITY, "the minimum complexity of hypothesis", 1, 10, 1));
		types.add(new ParameterTypeBoolean(PARAMETER_USE_BINOMIAL,
				"Switch to binomial utility funtion before increasing complexity", false));
		types.add(new ParameterTypeCategory(PARAMETER_UTILITY_FUNCTION, "the utility function to be used",
				Utility.UTILITY_TYPES, 4));
		types.add(new ParameterTypeBoolean(PARAMETER_USE_KBS, "use kbs to reweight examples after each iteration", true));
		types.add(new ParameterTypeBoolean(PARAMETER_REJECTION_SAMPLING,
				"use rejection sampling instead of weighted examples", true));
		types.add(new ParameterTypeCategory(PARAMETER_USEFUL_CRITERION,
				"criterion to decide if the complexity is increased ", IteratingGSS.CRITERION_TYPES, 1));
		types.add(new ParameterTypeDouble(PARAMETER_EXAMPLE_FACTOR,
				"used by example criterion to determine usefulness of a hypothesis", 1.0, 5.0, 1.5));
		types.add(new ParameterTypeBoolean(PARAMETER_GENERATE_ALL_HYPOTHESIS, "generate h->Y+/Y- or h->Y+ only.", false));
		types.add(new ParameterTypeBoolean(PARAMETER_RESET_WEIGHTS, "Set weights back to 1 when complexity is increased.",
				false));
		return types;
	}
}
