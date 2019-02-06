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
package com.rapidminer.operator.features.selection;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Iterator;
import java.util.List;

import com.rapidminer.example.Attribute;
import com.rapidminer.example.AttributeWeights;
import com.rapidminer.example.Attributes;
import com.rapidminer.example.ExampleSet;
import com.rapidminer.operator.MissingIOObjectException;
import com.rapidminer.operator.OperatorChain;
import com.rapidminer.operator.OperatorDescription;
import com.rapidminer.operator.OperatorException;
import com.rapidminer.operator.UserError;
import com.rapidminer.operator.ValueDouble;
import com.rapidminer.operator.ValueString;
import com.rapidminer.operator.performance.PerformanceCriterion;
import com.rapidminer.operator.performance.PerformanceVector;
import com.rapidminer.operator.ports.InputPort;
import com.rapidminer.operator.ports.OutputPort;
import com.rapidminer.operator.ports.metadata.SubprocessTransformRule;
import com.rapidminer.parameter.ParameterType;
import com.rapidminer.parameter.ParameterTypeBoolean;
import com.rapidminer.parameter.ParameterTypeCategory;
import com.rapidminer.parameter.ParameterTypeDouble;
import com.rapidminer.parameter.ParameterTypeInt;
import com.rapidminer.parameter.conditions.BooleanParameterCondition;
import com.rapidminer.parameter.conditions.EqualTypeCondition;
import com.rapidminer.tools.math.AnovaCalculator;
import com.rapidminer.tools.math.SignificanceCalculationException;
import com.rapidminer.tools.math.SignificanceTestResult;


/**
 * This operator starts with the full set of attributes and, in each round, it removes each
 * remaining attribute of the given set of examples. For each removed attribute, the performance is
 * estimated using inner operators, e.g. a cross-validation. Only the attribute giving the least
 * decrease of performance is finally removed from the selection. Then a new round is started with
 * the modified selection. This implementation will avoid any additional memory consumption beside
 * the memory used originally for storing the data and the memory which might be needed for applying
 * the inner operators. A parameter specifies when the iteration will be aborted. There are three
 * different behaviors possible:
 * <ul>
 * <li><b>with decrease</b>runs as long as there is any increase in performance</li>
 * <li><b>with decrease of more than</b>runs as long as the decrease is less than the specified
 * threshold, either relative or absolute.</li>
 * <li><b>with significant decrease</b> stops as soon as the decrease is significant to the
 * specified level.</li>
 * </ul>
 *
 * The parameter speculative_rounds defines how many rounds will be performed in a row, after a
 * first time the stopping criterion was fulfilled. If the performance increases again during the
 * speculative rounds, the elimination will be continued. Otherwise all additionally eliminated
 * attributes will be restored, as if no speculative rounds would have been executed. This might
 * help to avoid getting stuck in local optima.
 *
 * The operator provides a value for logging the performance in each round using a ProcessLog.
 *
 * @author Sebastian Land
 *
 */
public class BackwardAttributeEliminationOperator extends OperatorChain {

	public static final String PARAMETER_STOPPING_BEHAVIOR = "stopping_behavior";
	public static final String PARAMETER_MAX_ATTRIBUTES = "maximal_number_of_eliminations";
	public static final String PARAMETER_MAX_RELATIVE_DECREASE = "maximal_relative_decrease";
	public static final String PARAMETER_MAX_ABSOLUT_DECREASE = "maximal_absolute_decrease";
	public static final String PARAMETER_USE_RELATIVE_DECREASE = "use_relative_decrease";
	public static final String PARAMETER_ALPHA = "alpha";
	public static final String PARAMETER_ALLOWED_CONSECUTIVE_FAILS = "speculative_rounds";

	public static final String[] STOPPING_BEHAVIORS = new String[] { "with decrease", "with decrease of more than",
			"with significant decrease" };

	public static final int WITH_DECREASE = 0;
	public static final int WITH_DECREASE_EXCEEDS = 1;
	public static final int WITH_DECREASE_SIGNIFICANT = 2;

	private double currentNumberOfFeatures = 0;
	private Attributes currentAttributes;

	private InputPort exampleSetInput = getInputPorts().createPort("example set", ExampleSet.class);

	private OutputPort innerExampleSetSource = getSubprocess(0).getInnerSources().createPort("example set");
	private InputPort innerPerformanceSink = getSubprocess(0).getInnerSinks().createPort("performance",
			PerformanceVector.class);

	private OutputPort exampleSetOutput = getOutputPorts().createPort("example set");
	private OutputPort weightsOutput = getOutputPorts().createPort("attribute weights");
	private OutputPort performanceOutput = getOutputPorts().createPort("performance");

	public BackwardAttributeEliminationOperator(OperatorDescription description) {
		super(description, "Learning Process");

		getTransformer().addPassThroughRule(exampleSetInput, innerExampleSetSource);
		getTransformer().addRule(new SubprocessTransformRule(getSubprocess(0)));
		getTransformer().addPassThroughRule(exampleSetInput, exampleSetOutput);
		getTransformer().addGenerationRule(performanceOutput, PerformanceVector.class);
		getTransformer().addGenerationRule(weightsOutput, AttributeWeights.class);

		addValue(new ValueDouble("number of attributes", "The current number of attributes.") {

			@Override
			public double getDoubleValue() {
				return currentNumberOfFeatures;
			}
		});

		addValue(new ValueString("feature_names", "A comma separated list of all features of this round.") {

			@Override
			public String getStringValue() {
				if (currentAttributes == null) {
					return "This logging value is only available during execution of this operator's inner subprocess.";
				}

				StringBuffer buffer = new StringBuffer();
				for (Attribute attribute : currentAttributes) {
					if (buffer.length() > 0) {
						buffer.append(", ");
					}
					buffer.append(attribute.getName());
				}
				return buffer.toString();
			}
		});

	}

	@Override
	public void doWork() throws OperatorException {
		ExampleSet exampleSetOriginal = exampleSetInput.getData(ExampleSet.class);
		ExampleSet exampleSet = (ExampleSet) exampleSetOriginal.clone();
		int numberOfAttributes = exampleSet.getAttributes().size();
		Attributes attributes = exampleSet.getAttributes();

		int maxNumberOfAttributes = Math.min(getParameterAsInt(PARAMETER_MAX_ATTRIBUTES), numberOfAttributes - 1);
		int maxNumberOfFails = getParameterAsInt(PARAMETER_ALLOWED_CONSECUTIVE_FAILS);
		int behavior = getParameterAsInt(PARAMETER_STOPPING_BEHAVIOR);

		boolean useRelativeIncrease = behavior == WITH_DECREASE_EXCEEDS && getParameterAsBoolean(PARAMETER_USE_RELATIVE_DECREASE);
		double maximalDecrease = 0;
		if (useRelativeIncrease) {
			maximalDecrease = useRelativeIncrease ? getParameterAsDouble(PARAMETER_MAX_RELATIVE_DECREASE)
					: getParameterAsDouble(PARAMETER_MAX_ABSOLUT_DECREASE);
		}
		double alpha = behavior == WITH_DECREASE_SIGNIFICANT ? getParameterAsDouble(PARAMETER_ALPHA) : 0d;

		// remembering attributes and removing all from example set
		Attribute[] attributeArray = new Attribute[numberOfAttributes];
		int i = 0;
		Iterator<Attribute> iterator = attributes.iterator();
		while (iterator.hasNext()) {
			Attribute attribute = iterator.next();
			attributeArray[i] = attribute;
			i++;
		}

		boolean[] selected = new boolean[numberOfAttributes];
		Arrays.fill(selected, true);

		boolean earlyAbort = false;
		List<Integer> speculativeList = new ArrayList<Integer>(maxNumberOfFails);
		int numberOfFails = maxNumberOfFails;
		currentNumberOfFeatures = numberOfAttributes;
		currentAttributes = attributes;
		PerformanceVector lastPerformance = getPerformance(exampleSet);
		PerformanceVector bestPerformanceEver = lastPerformance;
		// init operator progress
		getProgress().setTotal(100);

		for (i = 0; i < maxNumberOfAttributes && !earlyAbort; i++) {
			// setting values for logging
			currentNumberOfFeatures = numberOfAttributes - i - 1;

			// performing a round
			int bestIndex = 0;
			PerformanceVector currentBestPerformance = null;
			for (int current = 0; current < numberOfAttributes; current++) {
				if (selected[current]) {
					// switching off
					attributes.remove(attributeArray[current]);
					currentAttributes = attributes;

					// evaluate performance
					PerformanceVector performance = getPerformance(exampleSet);
					if (currentBestPerformance == null || performance.compareTo(currentBestPerformance) > 0) {
						bestIndex = current;
						currentBestPerformance = performance;
					}

					// switching on
					attributes.addRegular(attributeArray[current]);
					currentAttributes = null; // removing reference
				}

				// update operator progress
				getProgress().setCompleted((int) (100.0 * (i * numberOfAttributes + current + 1)
						/ (maxNumberOfAttributes * numberOfAttributes)));
			}
			double currentFitness = currentBestPerformance != null ? currentBestPerformance.getMainCriterion().getFitness() : -1;
			if (i != 0) {
				double lastFitness = lastPerformance != null ? lastPerformance.getMainCriterion().getFitness() : -1;
				// switch stopping behavior
				switch (behavior) {
					case WITH_DECREASE:
						if (lastFitness >= currentFitness) {
							earlyAbort = true;
						}
						break;
					case WITH_DECREASE_EXCEEDS:
						if (useRelativeIncrease) {
							// relative increase testing
							if (currentFitness < lastFitness - Math.abs(lastFitness * maximalDecrease)) {
								earlyAbort = true;
							}
						} else {
							// absolute increase testing
							if (currentFitness < lastFitness - maximalDecrease) {
								earlyAbort = true;
							}
						}
						break;
					case WITH_DECREASE_SIGNIFICANT:
						if (currentBestPerformance != null && lastPerformance != null) {
							AnovaCalculator calculator = new AnovaCalculator();
							calculator.setAlpha(alpha);

							PerformanceCriterion pc = currentBestPerformance.getMainCriterion();
							calculator.addGroup(pc.getAverageCount(), pc.getAverage(), pc.getVariance());
							pc = lastPerformance.getMainCriterion();
							calculator.addGroup(pc.getAverageCount(), pc.getAverage(), pc.getVariance());

							SignificanceTestResult result;
							try {
								result = calculator.performSignificanceTest();
							} catch (SignificanceCalculationException e) {
								throw new UserError(this, 920, e.getMessage());
							}
							if (lastFitness > currentFitness && result.getProbability() < alpha) {
								earlyAbort = true;
							}
						}
				}
			}
			if (earlyAbort) {
				// check if there are some free tries left
				if (numberOfFails == 0) {
					break;
				}
				numberOfFails--;
				speculativeList.add(bestIndex);
				earlyAbort = false;

				// needs performance increase compared to better performance of current and last!
				if (currentBestPerformance != null && currentBestPerformance.compareTo(lastPerformance) > 0) {
					lastPerformance = currentBestPerformance;
				}
			} else {
				// resetting maximal number of fails.
				numberOfFails = maxNumberOfFails;
				speculativeList.clear();
				lastPerformance = currentBestPerformance;
				bestPerformanceEver = currentBestPerformance;
			}

			// switching best index off
			attributes.remove(attributeArray[bestIndex]);
			selected[bestIndex] = false;
		}
		// add predictively removed attributes: speculative execution did not yield good result
		for (Integer removeIndex : speculativeList) {
			selected[removeIndex] = true;
			attributes.addRegular(attributeArray[removeIndex]);
		}

		AttributeWeights weights = new AttributeWeights();
		i = 0;
		for (Attribute attribute : attributeArray) {
			if (selected[i]) {
				weights.setWeight(attribute.getName(), 1d);
			} else {
				weights.setWeight(attribute.getName(), 0d);
			}
			i++;
		}

		exampleSetOutput.deliver(exampleSet);
		performanceOutput.deliver(bestPerformanceEver);
		weightsOutput.deliver(weights);
	}

	private PerformanceVector getPerformance(ExampleSet exampleSet) throws OperatorException, MissingIOObjectException {
		innerExampleSetSource.deliver(exampleSet);

		getSubprocess(0).execute();

		return innerPerformanceSink.getData(PerformanceVector.class);
	}

	@Override
	public List<ParameterType> getParameterTypes() {
		List<ParameterType> types = super.getParameterTypes();
		ParameterType type = new ParameterTypeInt(PARAMETER_MAX_ATTRIBUTES,
				"The maximal number of backward eliminations. Hence the resulting number of attributes is maximal reduced by this number.",
				1, Integer.MAX_VALUE, 10);
		type.setExpert(false);
		types.add(type);

		type = new ParameterTypeInt(PARAMETER_ALLOWED_CONSECUTIVE_FAILS,
				"Defines the number of times, the stopping criterion might be consecutivly ignored before the elimination is actually stopped. A number higher than one might help not to stack in the local optima.",
				0, Integer.MAX_VALUE, 0);
		type.setExpert(false);
		types.add(type);

		type = new ParameterTypeCategory(PARAMETER_STOPPING_BEHAVIOR,
				"Defines on what criterias the elimination is stopped.", STOPPING_BEHAVIORS, 0);
		type.setExpert(false);
		types.add(type);

		type = new ParameterTypeBoolean(PARAMETER_USE_RELATIVE_DECREASE,
				"If checked, the relative performance decrease will be used as stopping criterion.", true);
		type.setExpert(false);
		type.registerDependencyCondition(
				new EqualTypeCondition(this, PARAMETER_STOPPING_BEHAVIOR, STOPPING_BEHAVIORS, false, WITH_DECREASE_EXCEEDS));
		types.add(type);

		type = new ParameterTypeDouble(PARAMETER_MAX_ABSOLUT_DECREASE,
				"If the absolute performance decrease to the last step exceeds this threshold, the selection will be stopped.",
				Double.NEGATIVE_INFINITY, Double.POSITIVE_INFINITY, true);
		type.setExpert(false);
		type.registerDependencyCondition(new BooleanParameterCondition(this, PARAMETER_USE_RELATIVE_DECREASE, true, false));
		type.registerDependencyCondition(
				new EqualTypeCondition(this, PARAMETER_STOPPING_BEHAVIOR, STOPPING_BEHAVIORS, false, WITH_DECREASE_EXCEEDS));
		types.add(type);
		type = new ParameterTypeDouble(PARAMETER_MAX_RELATIVE_DECREASE,
				"If the relative performance decrease to the last step exceeds this threshold, the selection will be stopped.",
				-1d, 1d, true);
		type.setExpert(false);
		type.registerDependencyCondition(new BooleanParameterCondition(this, PARAMETER_USE_RELATIVE_DECREASE, true, true));
		type.registerDependencyCondition(
				new EqualTypeCondition(this, PARAMETER_STOPPING_BEHAVIOR, STOPPING_BEHAVIORS, false, WITH_DECREASE_EXCEEDS));
		types.add(type);

		type = new ParameterTypeDouble(PARAMETER_ALPHA,
				"The probability threshold which determines if differences are considered as significant.", 0.0d, 1.0d,
				0.05d);
		type.registerDependencyCondition(new EqualTypeCondition(this, PARAMETER_STOPPING_BEHAVIOR, STOPPING_BEHAVIORS, true,
				WITH_DECREASE_SIGNIFICANT));
		types.add(type);
		return types;
	}
}
