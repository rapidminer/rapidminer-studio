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

import java.util.Iterator;
import java.util.List;
import java.util.logging.Level;

import com.rapidminer.operator.Operator;
import com.rapidminer.operator.OperatorDescription;
import com.rapidminer.operator.OperatorException;
import com.rapidminer.operator.UserError;
import com.rapidminer.operator.ValueDouble;
import com.rapidminer.operator.performance.PerformanceVector;
import com.rapidminer.operator.ports.CollectingPortPairExtender;
import com.rapidminer.operator.ports.OutputPort;
import com.rapidminer.operator.ports.PortPairExtender;
import com.rapidminer.parameter.ParameterType;
import com.rapidminer.parameter.ParameterTypeBoolean;
import com.rapidminer.parameter.value.ParameterValueRange;
import com.rapidminer.parameter.value.ParameterValues;


/**
 * <p>
 * In contrast to the {@link GridSearchParameterOptimizationOperator} operator this operators simply
 * uses the defined parameters and perform the inner operators for all possible combinations. This
 * can be especially useful for plotting or logging purposes and sometimes also for simply
 * configuring the parameters for the inner operators as a sort of meta step (e.g. learning curve
 * generation).
 * </p>
 *
 * <p>
 * This operator iterates through a set of parameters by using all possible parameter combinations.
 * The parameter <var>parameters</var> is a list of key value pairs where the keys are of the form
 * <code>operator_name.parameter_name</code> and the value is either a comma separated list of
 * values (e.g. 10,15,20,25) or an interval definition in the format [start;end;stepsize] (e.g.
 * [10;25;5]). Additionally, the format [start;end;steps;scale] is allowed.
 * </p>
 *
 * <p>
 * Please note that this operator has two modes: synchronized and non-synchronized. In the latter,
 * all parameter combinations are generated and the inner operators are applied for each
 * combination. In the synchronized mode, no combinations are generated but the set of all pairs of
 * the increasing number of parameters are used. For the iteration over a single parameter there is
 * no difference between both modes. Please note that the number of parameter possibilities must be
 * the same for all parameters in the synchronized mode.
 * </p>
 *
 * Compatibility note: This operator no longer returns all of its input. In most applications all
 * that can be done with such a collection of IOOBjects is iterating over them again, and that can
 * as well be done inside the ParameterIteration. Where this is not possible, please group them into
 * a collection (using a IOCollector) and store the collection using an IOStorage operator.
 *
 * @author Ingo Mierswa, Tobias Malbrecht
 */
public class ParameterIteration extends ParameterIteratingOperatorChain {

	/** The parameter name for &quot;A list of parameters to optimize&quot; */
	public static final String PARAMETER_PARAMETERS = "parameters";

	/** The parameter name for &quot;Synchronize parameter iteration&quot; */
	public static final String PARAMETER_SYNCHRONIZE = "synchronize";

	// /** Deprecated: The parameter name for &quot;Keep the output of the last operator in the
	// operator chain&quot; */
	// public static final String PARAMETER_KEEP_OUTPUT = "keep_output";

	private PerformanceVector performance;

	private int iteration = 0;

	public ParameterIteration(OperatorDescription description) {
		super(description);
		addValue(new ValueDouble("performance", "The last performance.") {

			@Override
			public double getDoubleValue() {
				if (performance != null) {
					return performance.getMainCriterion().getAverage();
				} else {
					return Double.NaN;
				}
			}
		});
		addValue(new ValueDouble("iteration", "The current iteration.") {

			@Override
			public double getDoubleValue() {
				return iteration;
			}
		});
	}

	@Override
	protected boolean isPerformanceRequired() {
		return false;
	}

	@Override
	public int getParameterValueMode() {
		return ParameterConfigurator.VALUE_MODE_DISCRETE;
	}

	@Override
	public void doWork() throws OperatorException {
		((CollectingPortPairExtender) getInnerSinkExtender()).reset();
		boolean isSynchronized = getParameterAsBoolean(PARAMETER_SYNCHRONIZE);

		// check parameter values
		List<String[]> parameterList = getParameterList(PARAMETER_PARAMETERS);
		List<ParameterValues> parameterValuesList = parseParameterValues(parameterList);
		int numberOfCombinations = 1;
		int lastNumberOfValues = -1;
		for (Iterator<ParameterValues> iterator = parameterValuesList.iterator(); iterator.hasNext();) {
			ParameterValues parameterValues = iterator.next();
			if (parameterValues instanceof ParameterValueRange) {
				getLogger().warning(
						"Found (and deleted) parameter values range (" + parameterValues.getKey()
								+ ") which makes no sense in grid parameter optimization");
				iterator.remove();
			}
			numberOfCombinations *= parameterValues.getNumberOfValues();
		}

		// init Operator progress
		getProgress().setTotal(numberOfCombinations);
		getProgress().setCheckForStop(false);

		// initialize data structures
		Operator[] operators = new Operator[parameterList.size()];
		String[] parameters = new String[parameterList.size()];
		String[][] values = new String[parameterList.size()][];
		int[] currentIndex = new int[parameterList.size()];

		// get parameter values and fill data structures
		int index = 0;
		for (Iterator<ParameterValues> iterator = parameterValuesList.iterator(); iterator.hasNext();) {
			ParameterValues parameterValues = iterator.next();
			operators[index] = parameterValues.getOperator();
			parameters[index] = parameterValues.getParameterType().getKey();
			values[index] = parameterValues.getValuesArray();
			if (!isSynchronized) {
				numberOfCombinations *= values[index].length;
			} else {
				numberOfCombinations = values[index].length;
				if (lastNumberOfValues < 0) {
					lastNumberOfValues = values[index].length;
				} else {
					if (lastNumberOfValues != values[index].length) {
						throw new UserError(this, 926);
					}
				}
			}
			index++;
		}

		if (numberOfCombinations < 1 || values.length == 0) {
			throw new UserError(this, 958);
		}

		// iterate parameter combinations
		this.iteration = 0;
		while (true) {
			String[] currentValues = new String[parameters.length];
			// set all parameter values
			for (int j = 0; j < operators.length; j++) {
				currentValues[j] = values[j][currentIndex[j]].trim();
				// operators[j].setParameter(parameters[j], values[j][currentIndex[j]].trim());
			}
			ParameterSet set = new ParameterSet(operators, parameters, currentValues, null);

			evaluateParameterSet(set);

			this.iteration++;

			boolean ok = true;
			if (!isSynchronized) {
				// next parameter values
				int k = 0;
				while (!(++currentIndex[k] < values[k].length)) {
					currentIndex[k] = 0;
					k++;
					if (k >= currentIndex.length) {
						ok = false;
						break;
					}
				}
			} else {
				for (int k = 0; k < currentIndex.length; k++) {
					currentIndex[k]++;
				}
				if (!(currentIndex[0] < values[0].length)) {
					ok = false;
					break;
				}
			}

			if (!ok) {
				break;
			}

			inApplyLoop();
			getProgress().step();
		}
		getProgress().complete();
	}

	protected void evaluateParameterSet(ParameterSet set) throws OperatorException {
		if (getLogger().isLoggable(Level.FINE)) {
			getLogger().fine("Evaluating parameter set: " + set.toString());
		}
		set.applyAll(getProcess(), null);
		this.performance = super.getPerformanceVector();
		((CollectingPortPairExtender) getInnerSinkExtender()).collect();
		if (performance == null) {
			getLogger().info(
					"Inner operators of " + getName()
							+ " do not provide performance vectors. Performance cannot be plotted.");
		}
	}

	@Override
	protected PortPairExtender makeInnerSinkExtender() {
		return new CollectingPortPairExtender("result", getSubprocess(0).getInnerSinks(), getOutputPorts());
	}

	@Override
	public boolean shouldAddNonConsumedInput() {
		return false;
	}

	@Override
	public boolean shouldAutoConnect(OutputPort outputPort) {
		if (outputPort.getName().startsWith("result")) {
			return getParameterAsBoolean("keep_output");
		} else {
			return super.shouldAutoConnect(outputPort);
		}
	}

	@Override
	public List<ParameterType> getParameterTypes() {
		List<ParameterType> types = super.getParameterTypes();
		types.add(new ParameterTypeBoolean(PARAMETER_SYNCHRONIZE, "Synchronize parameter iteration", false));
		// types.add(new ParameterTypeBoolean(PARAMETER_KEEP_OUTPUT,
		// "Delivers the merged output of the last operator of all the iterations, delivers the original input otherwise.",
		// false));
		return types;
	}
}
