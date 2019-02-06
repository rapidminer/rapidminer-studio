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
package com.rapidminer.operator.meta;

import java.util.List;

import org.jfree.util.Log;

import com.rapidminer.example.ExampleSet;
import com.rapidminer.operator.IOContainer;
import com.rapidminer.operator.OperatorDescription;
import com.rapidminer.operator.OperatorException;
import com.rapidminer.operator.ProcessSetupError.Severity;
import com.rapidminer.operator.SimpleProcessSetupError;
import com.rapidminer.operator.performance.PerformanceVector;
import com.rapidminer.operator.ports.InputPort;
import com.rapidminer.operator.ports.metadata.MetaData;
import com.rapidminer.operator.ports.metadata.SimplePrecondition;
import com.rapidminer.parameter.ParameterType;
import com.rapidminer.parameter.ParameterTypeBoolean;
import com.rapidminer.parameter.ParameterTypeCategory;
import com.rapidminer.parameter.ParameterTypeDouble;
import com.rapidminer.parameter.ParameterTypeInt;
import com.rapidminer.parameter.UndefinedParameterError;
import com.rapidminer.parameter.conditions.BooleanParameterCondition;


/**
 * Performs its inner operators until all given criteria are met or a timeout occurs.
 *
 * @author Stefan Rueping
 */
public class RepeatUntilOperatorChain extends AbstractIteratingOperatorChain {

	public static final String PARAMETER_CONDITION_EXAMPLES = "condition_on_data";

	/** The parameter name for &quot;Minimal number of attributes in first example set&quot; */
	public static final String PARAMETER_MIN_ATTRIBUTES = "min_attributes";

	/** The parameter name for &quot;Maximal number of attributes in first example set&quot; */
	public static final String PARAMETER_MAX_ATTRIBUTES = "max_attributes";

	/** The parameter name for &quot;Minimal number of examples in first example set&quot; */
	public static final String PARAMETER_MIN_EXAMPLES = "min_examples";

	/** The parameter name for &quot;Maximal number of examples in first example set&quot; */
	public static final String PARAMETER_MAX_EXAMPLES = "max_examples";

	/** The parameter name for &quot;Minimal main criterion in first performance vector&quot; */
	public static final String PARAMETER_MIN_CRITERION = "min_criterion";

	public static final String PARAMETER_CONDITION_PERFORMANCE = "condition_on_performance";

	/** The parameter name for &quot;Maximal main criterion in first performance vector&quot; */
	public static final String PARAMETER_MAX_CRITERION = "max_criterion";

	/** The parameter name for &quot;Maximum number of iterations&quot; */
	public static final String PARAMETER_MAX_ITERATIONS = "max_iterations";

	public static final String PARAMETER_LIMIT_TIME = "limit_time";

	/** The parameter name for &quot;Timeout in minutes (-1 = no timeout)&quot; */
	public static final String PARAMETER_TIMEOUT = "timeout";

	/**
	 * The parameter name for &quot;Stop when performance of inner chain behaves like this.&quot;
	 */
	public static final String PARAMETER_PERFORMANCE_CHANGE = "performance_change";

	/**
	 * The parameter name for &quot;Evaluate condition before inner chain is applied (true) or
	 * after?&quot;
	 */
	public static final String PARAMETER_CONDITION_BEFORE = "condition_before";

	public static final String[] COMPARISONS = { "none", "decreasing", "non-increasing" };

	public static final int NONE = 0;

	public static final int DECREASING = 1;

	public static final int NONINCREASING = 2;

	private long stoptime;

	private double fitness;

	private int maxit;
	private boolean conditionBefore;
	private boolean conditionPerformance;
	private int performanceChange;
	private double maxCrit;
	private double minCrit;
	private boolean conditionExamples;
	private int maxAttributes;
	private int minAttributes;
	private int maxEx;
	private int minEx;

	private final InputPort performanceConditionInput = getSubprocess(0).getInnerSinks().createPort("performance");
	private final InputPort exampleSetConditionInput = getSubprocess(0).getInnerSinks().createPort("example set");

	public RepeatUntilOperatorChain(OperatorDescription description) {
		super(description);
		performanceConditionInput
				.addPrecondition(new SimplePrecondition(performanceConditionInput, new MetaData(PerformanceVector.class)) {

					@Override
					protected boolean isMandatory() {
						return getParameterAsBoolean(PARAMETER_CONDITION_PERFORMANCE);
					}
				});
		exampleSetConditionInput
				.addPrecondition(new SimplePrecondition(exampleSetConditionInput, new MetaData(ExampleSet.class)) {

					@Override
					protected boolean isMandatory() {
						return getParameterAsBoolean(PARAMETER_CONDITION_EXAMPLES);
					}
				});
	}

	@Override
	public void performAdditionalChecks() {

		if (!getParameterAsBoolean(PARAMETER_CONDITION_PERFORMANCE)
				&& !getParameterAsBoolean(PARAMETER_CONDITION_EXAMPLES)) {
			this.addError(new SimpleProcessSetupError(Severity.ERROR, getPortOwner(), "need_one_parameter",
					PARAMETER_CONDITION_EXAMPLES, PARAMETER_CONDITION_PERFORMANCE));
		}

		try {
			if (getParameterAsBoolean(PARAMETER_CONDITION_PERFORMANCE)) {
				double maxCrit = getParameterAsDouble(PARAMETER_MAX_CRITERION);
				double minCrit = getParameterAsDouble(PARAMETER_MIN_CRITERION);
				if (maxCrit < Double.POSITIVE_INFINITY || minCrit > Double.NEGATIVE_INFINITY) {
					if (maxCrit < minCrit) {
						this.addError(new SimpleProcessSetupError(Severity.ERROR, getPortOwner(),
								"parameter_combination_forbidden_range", PARAMETER_MIN_CRITERION, PARAMETER_MAX_CRITERION));
					}
				}
			}

			if (getParameterAsBoolean(PARAMETER_CONDITION_EXAMPLES)) {
				int maxAtts = getParameterAsInt(PARAMETER_MAX_ATTRIBUTES);
				int minAtts = getParameterAsInt(PARAMETER_MIN_ATTRIBUTES);
				if (maxAtts < Double.POSITIVE_INFINITY || minAtts > Double.NEGATIVE_INFINITY) {
					if (maxAtts < minAtts) {
						this.addError(new SimpleProcessSetupError(Severity.ERROR, getPortOwner(),
								"parameter_combination_forbidden_range", PARAMETER_MIN_ATTRIBUTES,
								PARAMETER_MAX_ATTRIBUTES));
					}
				}

				int maxEx = getParameterAsInt(PARAMETER_MAX_EXAMPLES);
				int minEx = getParameterAsInt(PARAMETER_MIN_EXAMPLES);
				if (maxEx < Double.POSITIVE_INFINITY || minEx > Double.NEGATIVE_INFINITY) {
					if (maxEx < minEx) {
						this.addError(new SimpleProcessSetupError(Severity.ERROR, getPortOwner(),
								"parameter_combination_forbidden_range", PARAMETER_MIN_EXAMPLES, PARAMETER_MAX_EXAMPLES));
					}
				}
			}
		} catch (UndefinedParameterError e) {
			Log.error("parameter undefined", e);
		}

	}

	@Override
	public void doWork() throws OperatorException {
		stoptime = Long.MAX_VALUE;
		maxit = getParameterAsInt(PARAMETER_MAX_ITERATIONS);
		conditionBefore = getParameterAsBoolean(PARAMETER_CONDITION_BEFORE);
		conditionPerformance = getParameterAsBoolean(PARAMETER_CONDITION_PERFORMANCE);
		performanceChange = getParameterAsInt(PARAMETER_PERFORMANCE_CHANGE);
		maxCrit = getParameterAsDouble(PARAMETER_MAX_CRITERION);
		minCrit = getParameterAsDouble(PARAMETER_MIN_CRITERION);
		conditionExamples = getParameterAsBoolean(PARAMETER_CONDITION_EXAMPLES);
		maxAttributes = getParameterAsInt(PARAMETER_MAX_ATTRIBUTES);
		minAttributes = getParameterAsInt(PARAMETER_MIN_ATTRIBUTES);
		maxEx = getParameterAsInt(PARAMETER_MAX_EXAMPLES);
		minEx = getParameterAsInt(PARAMETER_MIN_EXAMPLES);
		if (getParameterAsBoolean(PARAMETER_LIMIT_TIME)) {
			stoptime = System.currentTimeMillis() + 60L * 1000 * getParameterAsInt(PARAMETER_TIMEOUT);
		}

		fitness = Double.NEGATIVE_INFINITY;
		getProgress().setTotal(maxit);
		super.doWork();
	}

	/**
	 * Evaluates whether the stopping condition is met
	 *
	 * @throws OperatorException
	 */
	private boolean evaluateCondition() throws OperatorException {
		if (getIteration() == 0 && !conditionBefore) {
			return false;
		}

		if (getIteration() >= maxit) {
			getLogger().fine("Maximum number of iterations met.");
			return true;
		}
		;
		if (java.lang.System.currentTimeMillis() > stoptime) {
			getLogger().fine("Runtime exceeded.");
			return true;
		}
		;

		// NOTE: This is not optional
		if (conditionPerformance) {
			PerformanceVector performanceVector = performanceConditionInput.getData(PerformanceVector.class);
			int changeType = performanceChange;
			if (changeType != NONE) {
				if (getIteration() > 0) {
					double currentFitness = performanceVector.getMainCriterion().getFitness();
					if (changeType == DECREASING && currentFitness < fitness) {
						return true;
					} else if (changeType == NONINCREASING && currentFitness <= fitness) {
						return true;
					}
					fitness = currentFitness;
					return false;
				}
			}

			if (maxCrit < Double.POSITIVE_INFINITY || minCrit > Double.NEGATIVE_INFINITY) {
				double crit = performanceVector.getMainCriterion().getAverage();
				if (crit > maxCrit || crit < minCrit) {
					return false;
				}
			}
		}
		if (conditionExamples) {
			ExampleSet exampleSet = exampleSetConditionInput.getData(ExampleSet.class);
			int maxAtts = maxAttributes;
			int minAtts = minAttributes;
			if (maxAtts < Integer.MAX_VALUE || minAtts > 0) {
				int nrAtts = exampleSet.getAttributes().size();
				if (nrAtts > maxAtts || nrAtts < minAtts) {
					return false;
				}
			}

			if (maxEx < Integer.MAX_VALUE || minEx > 0) {
				int nrEx = exampleSet.size();
				if (nrEx > maxEx || nrEx < minEx) {
					return false;
				}
			}
		}

		getLogger().fine("All criteria met.");
		return true;
	}

	@Override
	boolean shouldStop(IOContainer iterationResults) throws OperatorException {
		return evaluateCondition();
	}

	@Override
	public List<ParameterType> getParameterTypes() {
		List<ParameterType> types = super.getParameterTypes();

		ParameterType type = new ParameterTypeBoolean(PARAMETER_CONDITION_EXAMPLES,
				"If checked, conditions for the loop abortion based on the example set can be defined.", true);
		// type.registerDependencyCondition(new BooleanParameterCondition(this,
		// PARAMETER_CONDITION_PERFORMANCE, true, false));
		types.add(type);
		type = new ParameterTypeInt(PARAMETER_MIN_ATTRIBUTES, "Minimal number of attributes in first example set", 0,
				Integer.MAX_VALUE, 0);
		type.registerDependencyCondition(new BooleanParameterCondition(this, PARAMETER_CONDITION_EXAMPLES, false, true));
		type.setExpert(false);
		types.add(type);
		type = new ParameterTypeInt(PARAMETER_MAX_ATTRIBUTES, "Maximal number of attributes in first example set", 0,
				Integer.MAX_VALUE, 0);
		type.registerDependencyCondition(new BooleanParameterCondition(this, PARAMETER_CONDITION_EXAMPLES, false, true));
		type.setExpert(false);
		types.add(type);
		type = new ParameterTypeInt(PARAMETER_MIN_EXAMPLES, "Minimal number of examples in first example set", 0,
				Integer.MAX_VALUE, 0);
		type.registerDependencyCondition(new BooleanParameterCondition(this, PARAMETER_CONDITION_EXAMPLES, false, true));
		type.setExpert(false);
		types.add(type);
		type = new ParameterTypeInt(PARAMETER_MAX_EXAMPLES, "Maximal number of examples in first example set", 0,
				Integer.MAX_VALUE, Integer.MAX_VALUE);
		type.registerDependencyCondition(new BooleanParameterCondition(this, PARAMETER_CONDITION_EXAMPLES, false, true));
		type.setExpert(false);
		types.add(type);
		type = new ParameterTypeBoolean(PARAMETER_CONDITION_PERFORMANCE,
				"If checked, conditions for the loop abortion based on the performance can be defined.", true);
		types.add(type);
		type = new ParameterTypeDouble(PARAMETER_MIN_CRITERION, "Minimal main criterion in first performance vector",
				Double.NEGATIVE_INFINITY, Double.POSITIVE_INFINITY, Double.NEGATIVE_INFINITY);
		type.registerDependencyCondition(new BooleanParameterCondition(this, PARAMETER_CONDITION_PERFORMANCE, false, true));
		type.setExpert(false);
		types.add(type);
		type = new ParameterTypeDouble(PARAMETER_MAX_CRITERION, "Maximal main criterion in first performance vector",
				Double.NEGATIVE_INFINITY, Double.POSITIVE_INFINITY, Double.POSITIVE_INFINITY);
		type.registerDependencyCondition(new BooleanParameterCondition(this, PARAMETER_CONDITION_PERFORMANCE, false, true));
		type.setExpert(false);
		types.add(type);
		type = new ParameterTypeCategory(PARAMETER_PERFORMANCE_CHANGE,
				"Stop when performance of inner chain behaves like this. 'none' means to ignore any performance changes.",
				COMPARISONS, NONE);
		type.registerDependencyCondition(new BooleanParameterCondition(this, PARAMETER_CONDITION_PERFORMANCE, false, true));
		type.setExpert(false);
		types.add(type);
		type = new ParameterTypeInt(PARAMETER_MAX_ITERATIONS, "Maximum number of iterations", 0, Integer.MAX_VALUE,
				Integer.MAX_VALUE);
		type.setExpert(true);
		types.add(type);
		type = new ParameterTypeBoolean(PARAMETER_LIMIT_TIME,
				"If checked, the loop will be aborted at last after a specified time.", false);
		types.add(type);
		type = new ParameterTypeInt(PARAMETER_TIMEOUT, "Timeout in minutes", 1, Integer.MAX_VALUE, 1);
		type.registerDependencyCondition(new BooleanParameterCondition(this, PARAMETER_LIMIT_TIME, true, true));
		type.setExpert(true);
		types.add(type);
		type = new ParameterTypeBoolean(PARAMETER_CONDITION_BEFORE,
				"Evaluate condition before inner chain is applied (true) or after?", false);
		type.setExpert(true);
		types.add(type);
		return types;
	}
}
