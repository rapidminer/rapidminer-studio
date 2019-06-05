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

import java.util.Collection;
import java.util.Collections;
import java.util.LinkedList;
import java.util.List;

import com.rapidminer.RapidMiner;
import com.rapidminer.gui.properties.ConfigureParameterOptimizationDialogCreator;
import com.rapidminer.operator.IOObject;
import com.rapidminer.operator.Operator;
import com.rapidminer.operator.OperatorChain;
import com.rapidminer.operator.OperatorDescription;
import com.rapidminer.operator.OperatorException;
import com.rapidminer.operator.OperatorVersion;
import com.rapidminer.operator.ProcessSetupError.Severity;
import com.rapidminer.operator.SimpleProcessSetupError;
import com.rapidminer.operator.UserError;
import com.rapidminer.operator.performance.PerformanceVector;
import com.rapidminer.operator.ports.InputPort;
import com.rapidminer.operator.ports.PortPairExtender;
import com.rapidminer.operator.ports.metadata.MetaData;
import com.rapidminer.operator.ports.metadata.SimplePrecondition;
import com.rapidminer.operator.ports.metadata.SubprocessTransformRule;
import com.rapidminer.operator.ports.quickfix.ParameterSettingQuickFix;
import com.rapidminer.parameter.ParameterType;
import com.rapidminer.parameter.ParameterTypeCategory;
import com.rapidminer.parameter.ParameterTypeConfiguration;
import com.rapidminer.parameter.ParameterTypeList;
import com.rapidminer.parameter.ParameterTypeOperatorParameterTupel;
import com.rapidminer.parameter.ParameterTypeParameterValue;
import com.rapidminer.parameter.ParameterTypeTupel;
import com.rapidminer.parameter.UndefinedParameterError;
import com.rapidminer.parameter.conditions.AboveOperatorVersionCondition;
import com.rapidminer.parameter.value.ParameterValueGrid;
import com.rapidminer.parameter.value.ParameterValueList;
import com.rapidminer.parameter.value.ParameterValueRange;
import com.rapidminer.parameter.value.ParameterValues;
import com.rapidminer.tools.ParameterService;


/**
 * Provides an operator chain which operates on given parameters depending on specified values for
 * these parameters.
 * 
 * @author Tobias Malbrecht
 */
public abstract class ParameterIteratingOperatorChain extends OperatorChain implements ParameterConfigurator {

	/** Last version where errors in the inner process were not handled properly. */
	public static final OperatorVersion CHANGE_6_0_3_ERROR_HANDLING = new OperatorVersion(6, 0, 3);

	/** @deprecated since 8.0. Use {@link ParameterConfigurator#PARAMETER_PARAMETERS} instead */
	@Deprecated
	public static final String PARAMETER_PARAMETERS = ParameterConfigurator.PARAMETER_PARAMETERS;
	/** @deprecated since 8.0. Use {@link ParameterConfigurator#PARAMETER_VALUES} instead */
	@Deprecated
	public static final String PARAMETER_VALUES = ParameterConfigurator.PARAMETER_VALUES;

	/** @deprecated since 8.0. Use {@link ParameterConfigurator#VALUE_MODE_DISCRETE} instead */
	@Deprecated
	public static final int VALUE_MODE_DISCRETE = ParameterConfigurator.VALUE_MODE_DISCRETE;
	/** @deprecated since 8.0. Use {@link ParameterConfigurator#VALUE_MODE_CONTINUOUS} instead */
	@Deprecated
	public static final int VALUE_MODE_CONTINUOUS = ParameterConfigurator.VALUE_MODE_CONTINUOUS;

	private static final int PARAMETER_VALUES_ARRAY_LENGTH_RANGE = 2;

	private static final int PARAMETER_VALUES_ARRAY_LENGTH_GRID = 3;

	private static final int PARAMETER_VALUES_ARRAY_LENGTH_SCALED_GRID = 4;

	private static final String PARAMETER_OPERATOR_PARAMETER_PAIR = "operator_parameter_pair";

	public static final String PARAMETER_ERROR_HANDLING = "error_handling";

	public static final String[] ERROR_HANDLING_METHOD = new String[] { "fail on error", "ignore error" };

	public static final int ERROR_FAIL = 0;
	public static final int ERROR_IGNORE = 1;

	private final PortPairExtender inputExtender = new PortPairExtender("input", getInputPorts(), getSubprocess(0)
			.getInnerSources());
	private final InputPort performanceInnerSink = getSubprocess(0).getInnerSinks().createPort("performance");
	private final PortPairExtender innerSinkExtender;

	public ParameterIteratingOperatorChain(OperatorDescription description) {
		this(description, "Subprocess");
	}

	public ParameterIteratingOperatorChain(OperatorDescription description, String subprocessName) {
		super(description, subprocessName);
		innerSinkExtender = makeInnerSinkExtender();
		inputExtender.start();
		innerSinkExtender.start();
		getPerformanceInnerSink().addPrecondition(
				new SimplePrecondition(getPerformanceInnerSink(), new MetaData(PerformanceVector.class),
						isPerformanceRequired()));
		getTransformer().addRule(inputExtender.makePassThroughRule());
		getTransformer().addRule(new SubprocessTransformRule(getSubprocess(0)));
		getTransformer().addRule(innerSinkExtender.makePassThroughRule());
	}

	protected abstract PortPairExtender makeInnerSinkExtender();

	protected PortPairExtender getInnerSinkExtender() {
		return innerSinkExtender;
	}

	protected InputPort getPerformanceInnerSink() {
		return performanceInnerSink;
	}

	/** Signals whether the subprocess must create a performance vector. */
	protected abstract boolean isPerformanceRequired();

	/**
	 * Parses a parameter list and creates the corresponding data structures.
	 */
	@Override
	public List<ParameterValues> parseParameterValues(List<String[]> parameterList) throws OperatorException {
		if (getProcess() == null) {
			getLogger().warning("Cannot parse parameters while operator is not attached to a process.");
			return Collections.<ParameterValues> emptyList();
		}
		List<ParameterValues> parameterValuesList = new LinkedList<ParameterValues>();
		for (String[] pair : parameterList) {
			String[] operatorParameter = ParameterTypeTupel.transformString2Tupel(pair[0]);
			if (operatorParameter.length != 2) {
				throw new UserError(this, 907, pair[0]);
			}
			Operator operator = lookupOperator(operatorParameter[0]);
			if (operator == null) {
				throw new UserError(this, 109, operatorParameter[0]);
			}
			ParameterType parameterType = operator.getParameters().getParameterType(operatorParameter[1]);
			if (parameterType == null) {
				throw new UserError(this, 906, operatorParameter[0] + "." + operatorParameter[1]);
			}
			String parameterValuesString = pair[1];
			ParameterValues parameterValues = null;
			try {
				int startIndex = parameterValuesString.indexOf("[");
				if (startIndex >= 0) {
					int endIndex = parameterValuesString.indexOf("]");
					if (endIndex > startIndex) {
						String[] parameterValuesArray = parameterValuesString.substring(startIndex + 1, endIndex).trim()
								.split("[;:,]");
						switch (parameterValuesArray.length) {
							case PARAMETER_VALUES_ARRAY_LENGTH_RANGE: {
								// value range: [minValue;maxValue]
								parameterValues = new ParameterValueRange(operator, parameterType, parameterValuesArray[0],
										parameterValuesArray[1]);
							}
								break;
							case PARAMETER_VALUES_ARRAY_LENGTH_GRID: {
								// value grid: [minValue;maxValue;stepSize]
								parameterValues = new ParameterValueGrid(operator, parameterType, parameterValuesArray[0],
										parameterValuesArray[1], parameterValuesArray[2]);
							}
								break;
							case PARAMETER_VALUES_ARRAY_LENGTH_SCALED_GRID: {
								// value grid: [minValue;maxValue;noOfSteps;scale]
								parameterValues = new ParameterValueGrid(operator, parameterType, parameterValuesArray[0],
										parameterValuesArray[1], parameterValuesArray[2], parameterValuesArray[3]);
							}
								break;
							default:
								throw new Exception("parameter values string could not be parsed (too many arguments)");
						}
					} else {
						throw new Exception("']' was missing");
					}
				} else {
					int colonIndex = parameterValuesString.indexOf(":");
					if (colonIndex >= 0) {
						// maintain compatibility for evolutionary parameter optimization (old
						// format: startValue:endValue without parantheses)
						String[] parameterValuesArray = parameterValuesString.trim().split(":");
						if (parameterValuesArray.length != 2) {
							throw new Exception("wrong parameter range format");
						} else {
							parameterValues = new ParameterValueRange(operator, parameterType, parameterValuesArray[0],
									parameterValuesArray[1]);
						}
					} else {
						// usual parameter value list: value1,value2,value3,...
						if (parameterValuesString.length() != 0) {
							String[] values = parameterValuesString.split(",");
							parameterValues = new ParameterValueList(operator, parameterType, values);
						}
					}
				}
			} catch (Throwable e) {
				throw new UserError(this, 116, pair[0], "Unknown parameter value specification format: '" + pair[1]
						+ "'. Error: " + e.getMessage());
			}
			if (parameterValues != null) {
				parameterValuesList.add(parameterValues);
			}
		}
		return parameterValuesList;
	}

	protected void executeSubprocess() throws OperatorException {
		getSubprocess(0).execute();
	}

	/**
	 * Applies the inner operator and employs the PerformanceEvaluator for calculating a list of
	 * performance criteria which is returned.
	 * 
	 * @deprecated As of version 6.0.4, replaced by {@link #getPerformanceVector()}
	 */
	@Deprecated
	protected PerformanceVector getPerformance() {
		return getPerformance(true);
	}

	/**
	 * @deprecated As of version 6.0.4, replaced by {@link #getPerformanceVector()}
	 */
	@Deprecated
	protected PerformanceVector getPerformance(boolean cloneInput) {
		try {
			return getPerformanceVector();
		} catch (OperatorException e) {
			StringBuilder builder = new StringBuilder();
			builder.append(this.getName());
			builder.append(": Cannot evaluate performance for current parameter combination because of an error in one of the inner operators: ");
			builder.append(e.getMessage());
			getLogger().severe(builder.toString());
			// getLogger().severe("Cannot evaluate performance for current parameter combination: "
			// + e.getMessage());
			if (Boolean.parseBoolean(ParameterService.getParameterValue(RapidMiner.PROPERTY_RAPIDMINER_GENERAL_DEBUGMODE))) {
				e.printStackTrace();
			}
			return null;
		}
	}

	/**
	 * Applies the inner operator and employs the PerformanceEvaluator for calculating a list of
	 * performance criteria which is returned.
	 */
	protected PerformanceVector getPerformanceVector() throws OperatorException {
		try {
			inputExtender.passDataThrough();
			executeSubprocess();
			if (isPerformanceRequired()) {
				return getPerformanceInnerSink().getData(PerformanceVector.class);
			} else {
				return getPerformanceInnerSink().getDataOrNull(PerformanceVector.class);
			}
		} catch (OperatorException e) {
			if (getCompatibilityLevel().isAtMost(CHANGE_6_0_3_ERROR_HANDLING)
					|| getParameterAsInt(PARAMETER_ERROR_HANDLING) == ERROR_IGNORE) {
				return null;
			} else {
				throw e;
			}
		}
	}

	/**
	 * Returns the results at the inner sink port extender. Does not include a possible performance
	 * vector at the respective input. {@link #executeSubprocess()} or
	 * {@link #getPerformanceVector()} must have been called earlier.
	 * 
	 * @throws UserError
	 */
	protected Collection<IOObject> getInnerResults() throws UserError {
		return innerSinkExtender.getData(IOObject.class);
	}

	/** Passes data from the inner sinks to the output ports. */
	public void passResultsThrough() {
		innerSinkExtender.passDataThrough();
	}

	@Override
	public List<ParameterType> getParameterTypes() {
		List<ParameterType> types = super.getParameterTypes();
		ParameterType type = new ParameterTypeConfiguration(ConfigureParameterOptimizationDialogCreator.class, this);
		type.setExpert(false);
		types.add(type);
		type = new ParameterTypeList(ParameterConfigurator.PARAMETER_PARAMETERS, "The parameters.",
				new ParameterTypeOperatorParameterTupel(PARAMETER_OPERATOR_PARAMETER_PAIR, "The operator and it's parameter"),
				new ParameterTypeParameterValue(ParameterConfigurator.PARAMETER_VALUES,
						"The value specifications for the parameters."));
		type.setHidden(true);
		types.add(type);
		type = new ParameterTypeCategory(PARAMETER_ERROR_HANDLING,
				"This selects the method for handling errors occuring during the execution of the inner process.",
				ERROR_HANDLING_METHOD, ERROR_FAIL, false);
		type.registerDependencyCondition(new AboveOperatorVersionCondition(this, CHANGE_6_0_3_ERROR_HANDLING));
		types.add(type);

		return types;
	}

	@Override
	public OperatorVersion[] getIncompatibleVersionChanges() {
		OperatorVersion[] incompatibleVersionChanges = super.getIncompatibleVersionChanges();
		OperatorVersion[] newIncompatibleVersionChanges = new OperatorVersion[incompatibleVersionChanges.length + 1];
		for (int i = 0; i < incompatibleVersionChanges.length; ++i) {
			newIncompatibleVersionChanges[i] = incompatibleVersionChanges[i];
		}
		newIncompatibleVersionChanges[newIncompatibleVersionChanges.length - 1] = CHANGE_6_0_3_ERROR_HANDLING;
		return newIncompatibleVersionChanges;
	}

	@Override
	public int checkProperties() {
		boolean parametersPresent = false;
		try {
			List<ParameterValues> list = parseParameterValues(getParameterList(ParameterConfigurator.PARAMETER_PARAMETERS));
			if (list != null && list.size() > 0) {
				parametersPresent = true;
			}
		} catch (UndefinedParameterError e) {
		} catch (OperatorException e) {
		}

		if (!parametersPresent) {
			addError(new SimpleProcessSetupError(Severity.ERROR, this.getPortOwner(),
					Collections.singletonList(new ParameterSettingQuickFix(this,
							ParameterTypeConfiguration.PARAMETER_DEFAULT_CONFIGURATION_NAME)),
					"parameter_combination_undefined"));
		}

		return super.checkProperties();
	}
}
