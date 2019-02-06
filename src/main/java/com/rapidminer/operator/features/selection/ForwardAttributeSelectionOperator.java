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
import java.util.Iterator;
import java.util.List;

import com.rapidminer.example.Attribute;
import com.rapidminer.example.AttributeWeights;
import com.rapidminer.example.Attributes;
import com.rapidminer.example.ExampleSet;
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
 * This operator starts with an empty selection of attributes and, in each round, it adds each
 * unused attribute of the given set of examples. For each added attribute, the performance is
 * estimated using inner operators, e.g. a cross-validation. Only the attribute giving the highest
 * increase of performance is added to the selection. Then a new round is started with the modified
 * selection. This implementation will avoid any additional memory consumption beside the memory
 * used originally for storing the data and the memory which might be needed for applying the inner
 * operators. A parameter specifies when the iteration will be aborted. There are three different
 * behaviors possible:
 * <ul>
 * <li><b>without increase</b>runs as long as there is any increase in performance</li>
 * <li><b>without increase of at least</b> runs as long as the increase is at least as high as
 * specified, either relative or absolute.</li>
 * <li><b>without significant increase</b> stops as soon as the increase isn't significant to the
 * specified level.</li>
 * </ul>
 *
 * The parameter speculative_rounds defines how many rounds will be performed in a row, after a
 * first time the stopping criterion was fulfilled. If the performance increases again during the
 * speculative rounds, the selection will be continued. Otherwise all additionally selected
 * attributes will be removed, as if no speculative rounds would have been executed. This might help
 * to avoid getting stuck in local optima. A following backward elimination operator might remove
 * unneeded attributes again.
 *
 * The operator provides a value for logging the performance in each round using a ProcessLog.
 *
 * @author Sebastian Land
 *
 */
public class ForwardAttributeSelectionOperator extends OperatorChain {

	public static final String PARAMETER_STOPPING_BEHAVIOR = "stopping_behavior";
	public static final String PARAMETER_MAX_ATTRIBUTES = "maximal_number_of_attributes";
	public static final String PARAMETER_MIN_RELATIVE_INCREASE = "minimal_relative_increase";
	public static final String PARAMETER_MIN_ABSOLUT_INCREASE = "minimal_absolute_increase";
	public static final String PARAMETER_USE_RELATIVE_INCREASE = "use_relative_increase";
	public static final String PARAMETER_ALPHA = "alpha";
	public static final String PARAMETER_ALLOWED_CONSECUTIVE_FAILS = "speculative_rounds";

	public static final String[] STOPPING_BEHAVIORS = new String[] { "without increase", "without increase of at least",
			"without significant increase" };

	public static final int WITHOUT_INCREASE = 0;
	public static final int WITHOUT_INCREASE_OF_AT_LEAST = 1;
	public static final int WITHOUT_INCREASE_SIGNIFICANT = 2;

	private double currentNumberOfFeatures = 0;
	private Attributes currentAttributes;

	private InputPort exampleSetInput = getInputPorts().createPort("example set", ExampleSet.class);

	private OutputPort innerExampleSetSource = getSubprocess(0).getInnerSources().createPort("example set");
	private InputPort innerPerformanceSink = getSubprocess(0).getInnerSinks().createPort("performance",
			PerformanceVector.class);

	private OutputPort exampleSetOutput = getOutputPorts().createPort("example set");
	private OutputPort weightsOutput = getOutputPorts().createPort("attribute weights");
	private OutputPort performanceOutput = getOutputPorts().createPort("performance");

	public ForwardAttributeSelectionOperator(OperatorDescription description) {
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

		int maxNumberOfAttributes = Math.min(getParameterAsInt(PARAMETER_MAX_ATTRIBUTES), numberOfAttributes);
		int maxNumberOfFails = getParameterAsInt(PARAMETER_ALLOWED_CONSECUTIVE_FAILS);
		int behavior = getParameterAsInt(PARAMETER_STOPPING_BEHAVIOR);

		boolean useRelativeIncrease = behavior == WITHOUT_INCREASE_OF_AT_LEAST
				? getParameterAsBoolean(PARAMETER_USE_RELATIVE_INCREASE) : false;
		double minimalIncrease = 0;
		if (useRelativeIncrease) {
			minimalIncrease = useRelativeIncrease ? getParameterAsDouble(PARAMETER_MIN_RELATIVE_INCREASE)
					: getParameterAsDouble(PARAMETER_MIN_ABSOLUT_INCREASE);
		}
		double alpha = behavior == WITHOUT_INCREASE_SIGNIFICANT ? getParameterAsDouble(PARAMETER_ALPHA) : 0d;

		// remembering attributes and removing all from example set
		Attribute[] attributeArray = new Attribute[numberOfAttributes];
		int i = 0;
		Iterator<Attribute> iterator = attributes.iterator();
		while (iterator.hasNext()) {
			Attribute attribute = iterator.next();
			attributeArray[i] = attribute;
			i++;
			iterator.remove();
		}

		boolean[] selected = new boolean[numberOfAttributes];
		boolean earlyAbort = false;
		List<Integer> speculativeList = new ArrayList<Integer>(maxNumberOfFails);
		int numberOfFails = maxNumberOfFails;
		PerformanceVector lastPerformance = null;
		PerformanceVector bestPerformanceEver = null;
		// init operator progress
		getProgress().setTotal(100);

		for (i = 0; i < maxNumberOfAttributes && !earlyAbort; i++) {
			// setting values for logging
			currentNumberOfFeatures = i + 1;

			// performing a round
			int bestIndex = 0;
			PerformanceVector currentBestPerformance = null;
			for (int current = 0; current < numberOfAttributes; current++) {
				if (!selected[current]) {
					// switching on
					attributes.addRegular(attributeArray[current]);
					currentAttributes = attributes;

					// evaluate performance
					innerExampleSetSource.deliver(exampleSet);

					getSubprocess(0).execute();

					PerformanceVector performance = innerPerformanceSink.getData(PerformanceVector.class);
					if (currentBestPerformance == null || performance.compareTo(currentBestPerformance) > 0) {
						bestIndex = current;
						currentBestPerformance = performance;
					}

					// switching off
					attributes.remove(attributeArray[current]);
					currentAttributes = null;
				}

				// update operator progress
				getProgress().setCompleted((int) (100.0 * (i * numberOfAttributes + current + 1)
						/ (maxNumberOfAttributes * numberOfAttributes)));
			}
			double currentFitness = currentBestPerformance.getMainCriterion().getFitness();
			if (i != 0) {
				double lastFitness = lastPerformance.getMainCriterion().getFitness();
				// switch stopping behavior
				switch (behavior) {
					case WITHOUT_INCREASE:
						if (lastFitness >= currentFitness) {
							earlyAbort = true;
						}
						break;
					case WITHOUT_INCREASE_OF_AT_LEAST:
						if (useRelativeIncrease) {
							// relative increase testing
							if (!(currentFitness > lastFitness + lastFitness * minimalIncrease)) {
								earlyAbort = true;
							}
						} else {
							// absolute increase testing
							if (!(currentFitness > lastFitness + minimalIncrease)) {
								earlyAbort = true;
							}
						}
						break;
					case WITHOUT_INCREASE_SIGNIFICANT:
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
						if (lastFitness <= currentFitness || result.getProbability() < alpha) {
							earlyAbort = true;
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
				if (currentBestPerformance.compareTo(lastPerformance) > 0) {
					lastPerformance = currentBestPerformance;
				}
			} else {
				// resetting maximal number of fails.
				numberOfFails = maxNumberOfFails;
				speculativeList.clear();
				lastPerformance = currentBestPerformance;
				bestPerformanceEver = currentBestPerformance;
			}

			// switching best index on
			attributes.addRegular(attributeArray[bestIndex]);
			selected[bestIndex] = true;
		}
		// removing predictively added attributes: speculative execution did not yield good result
		for (Integer removeIndex : speculativeList) {
			selected[removeIndex] = false;
			attributes.remove(attributeArray[removeIndex]);
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

	@Override
	public List<ParameterType> getParameterTypes() {
		List<ParameterType> types = super.getParameterTypes();
		ParameterType type = new ParameterTypeInt(PARAMETER_MAX_ATTRIBUTES,
				"The maximal number of forward selection steps and hence the maximal number of attributes.", 1,
				Integer.MAX_VALUE, 10);
		type.setExpert(false);
		types.add(type);

		type = new ParameterTypeInt(PARAMETER_ALLOWED_CONSECUTIVE_FAILS,
				"Defines the number of times, the stopping criterion might be consecutivly ignored before the selection is actually stopped. A number higher than one might help not to stack in the local optima.",
				0, Integer.MAX_VALUE, 0);
		type.setExpert(false);
		types.add(type);

		type = new ParameterTypeCategory(PARAMETER_STOPPING_BEHAVIOR, "Defines on what criterias the selection is stopped.",
				STOPPING_BEHAVIORS, 0);
		type.setExpert(false);
		types.add(type);

		type = new ParameterTypeBoolean(PARAMETER_USE_RELATIVE_INCREASE,
				"If checked, the relative performance increase will be used as stopping criterion.", true);
		type.setExpert(false);
		type.registerDependencyCondition(new EqualTypeCondition(this, PARAMETER_STOPPING_BEHAVIOR, STOPPING_BEHAVIORS, false,
				WITHOUT_INCREASE_OF_AT_LEAST));
		types.add(type);

		type = new ParameterTypeDouble(PARAMETER_MIN_ABSOLUT_INCREASE,
				"If the absolute performance increase to the last step drops below this threshold, the selection will be stopped.",
				0d, Double.POSITIVE_INFINITY, true);
		type.setExpert(false);
		type.registerDependencyCondition(new BooleanParameterCondition(this, PARAMETER_USE_RELATIVE_INCREASE, true, false));
		type.registerDependencyCondition(new EqualTypeCondition(this, PARAMETER_STOPPING_BEHAVIOR, STOPPING_BEHAVIORS, false,
				WITHOUT_INCREASE_OF_AT_LEAST));
		types.add(type);
		type = new ParameterTypeDouble(PARAMETER_MIN_RELATIVE_INCREASE,
				"If the relative performance increase to the last step drops below this threshold, the selection will be stopped.",
				0d, 1d, true);
		type.setExpert(false);
		type.registerDependencyCondition(new BooleanParameterCondition(this, PARAMETER_USE_RELATIVE_INCREASE, true, true));
		type.registerDependencyCondition(new EqualTypeCondition(this, PARAMETER_STOPPING_BEHAVIOR, STOPPING_BEHAVIORS, false,
				WITHOUT_INCREASE_OF_AT_LEAST));
		types.add(type);

		type = new ParameterTypeDouble(PARAMETER_ALPHA,
				"The probability threshold which determines if differences are considered as significant.", 0.0d, 1.0d,
				0.05d);
		type.registerDependencyCondition(new EqualTypeCondition(this, PARAMETER_STOPPING_BEHAVIOR, STOPPING_BEHAVIORS, true,
				WITHOUT_INCREASE_SIGNIFICANT));
		types.add(type);
		return types;
	}
}
