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
package com.rapidminer.operator.performance;

import java.util.ArrayList;
import java.util.Iterator;
import java.util.LinkedList;
import java.util.List;

import com.rapidminer.example.Attribute;
import com.rapidminer.example.ExampleSet;
import com.rapidminer.operator.OperatorCapability;
import com.rapidminer.operator.OperatorDescription;
import com.rapidminer.operator.OperatorException;
import com.rapidminer.operator.ValueDouble;
import com.rapidminer.parameter.ParameterType;
import com.rapidminer.parameter.ParameterTypeList;
import com.rapidminer.parameter.ParameterTypeString;
import com.rapidminer.parameter.ParameterTypeStringCategory;
import com.rapidminer.parameter.UndefinedParameterError;


/**
 * <p>
 * This performance evaluator operator should be used for regression tasks, i.e. in cases where the
 * label attribute has a numerical value type. The operator expects a test {@link ExampleSet} as
 * input, whose elements have both true and predicted labels, and delivers as output a list of
 * performance values according to a list of performance criteria that it calculates. If an input
 * performance vector was already given, this is used for keeping the performance values.
 * </p>
 *
 * <p>
 * Additional user-defined implementations of {@link PerformanceCriterion} can be specified by using
 * the parameter list <var>additional_performance_criteria</var>. Each key/value pair in this list
 * must specify a fully qualified classname (as the key), and a string parameter (as value) that is
 * passed to the constructor. Please make sure that the class files are in the classpath (this is
 * the case if the implementations are supplied by a plugin) and that they implement a one-argument
 * constructor taking a string parameter. It must also be ensured that these classes extend
 * {@link MeasuredPerformance} since the PerformanceEvaluator operator will only support these
 * criteria. Please note that only the first three user defined criteria can be used as logging
 * value with names &quot;user1&quot;, ... , &quot;user3&quot;.
 * </p>
 *
 * <p>
 * The resulting performance vectors are usually compared with a standard performance comparator
 * which only compares the fitness values of the main criterion. Other implementations than this
 * simple comparator can be specified using the parameter <var>comparator_class</var>. This may for
 * instance be useful if you want to compare performance vectors according to the weighted sum of
 * the individual criteria. In order to implement your own comparator, simply subclass
 * {@link PerformanceComparator}. Please note that for true multi-objective optimization usually
 * another selection scheme is used instead of simply replacing the performance comparator.
 * </p>
 *
 * @author Ingo Mierswa
 */
public class UserBasedPerformanceEvaluator extends AbstractPerformanceEvaluator {

	/**
	 * The parameter name for &quot;List of classes that implement
	 * com.rapidminer..operator.performance.PerformanceCriterion.&quot;
	 */
	public static final String PARAMETER_ADDITIONAL_PERFORMANCE_CRITERIA = "additional_performance_criteria";

	/**
	 * The names of allowed user criteria. These are necessary for plotting purposes and the
	 * definition of the main criterion.
	 */
	public static final String[] USER_CRITERIA_NAMES = { "user1", "user2", "user3" };

	/** Used for logging. */
	private List<PerformanceCriterion> userCriteria = new ArrayList<PerformanceCriterion>();

	public UserBasedPerformanceEvaluator(OperatorDescription description) {
		super(description);
		for (int i = 0; i < USER_CRITERIA_NAMES.length; i++) {
			addUserPerformanceValue(USER_CRITERIA_NAMES[i], "The user defined performance criterion " + i);
		}
	}

	private void addUserPerformanceValue(final String name, String description) {
		addValue(new ValueDouble(name, description) {

			@Override
			public double getDoubleValue() {
				int index = Integer.parseInt(name.substring(4)) - 1;
				PerformanceCriterion c = userCriteria.get(index);
				return c.getAverage();
			}
		});
	}

	/** Does nothing. */
	@Override
	protected void checkCompatibility(ExampleSet exampleSet) throws OperatorException {}

	/** Returns null. */
	@Override
	protected double[] getClassWeights(Attribute label) throws UndefinedParameterError {
		return null;
	}

	/** Returns false. */
	@Override
	protected boolean showCriteriaParameter() {
		return false;
	}

	@Override
	public List<PerformanceCriterion> getCriteria() {
		if (this.userCriteria != null) {
			this.userCriteria.clear();
		}
		List<PerformanceCriterion> performanceCriteria = new LinkedList<PerformanceCriterion>();
		Iterator<String[]> i = null;
		try {
			i = getParameterList(PARAMETER_ADDITIONAL_PERFORMANCE_CRITERIA).iterator();
		} catch (UndefinedParameterError e1) {
			logError("No additional performance criteria defined. No criteria will be calculated...");
		}
		if (i != null) {
			while (i.hasNext()) {
				String[] keyValue = i.next();
				String className = keyValue[0];
				String parameter = keyValue[1];
				Class<?> criterionClass = null;
				try {
					criterionClass = com.rapidminer.tools.Tools.classForName(className);
					if (PerformanceCriterion.class.isAssignableFrom(criterionClass)) {
						PerformanceCriterion c = null;
						if (parameter != null && parameter.trim().length() > 0) {
							@SuppressWarnings("rawtypes")
							java.lang.reflect.Constructor constructor = criterionClass
							.getConstructor(new Class[] { String.class });
							c = (PerformanceCriterion) constructor.newInstance(new Object[] { parameter });
						} else {
							c = (PerformanceCriterion) criterionClass.newInstance();
						}
						if (!(c instanceof MeasuredPerformance)) {
							logError("Only subclasses of MeasuredPerformance are supported as user based criteria. Skipping '"
									+ className + "'...");
						} else {
							performanceCriteria.add(c);
							if (userCriteria != null) {
								userCriteria.add(c);
							}
						}
					} else {
						logError("Only subclasses of MeasuredPerformance are supported as user based criteria. Skipping '"
								+ className + "'...");
					}
				} catch (ClassNotFoundException e) {
					logError("Class not found: skipping '" + className + "'...");
				} catch (InstantiationException e) {
					logError("Cannot instantiate: skipping '" + className + "'...");
				} catch (IllegalAccessException e) {
					logError("Cannot access: skipping '" + className + "'...");
				} catch (NoSuchMethodException e) {
					logError("No appropriate constructor found: skipping '" + className + "'...");
				} catch (java.lang.reflect.InvocationTargetException e) {
					logError("Cannot instantiate constructor: skipping '" + className + "'...");
				}
			}
		}
		return performanceCriteria;
	}

	@Override
	public List<ParameterType> getParameterTypes() {
		List<ParameterType> types = super.getParameterTypes();

		ParameterType type = new ParameterTypeStringCategory(PARAMETER_MAIN_CRITERION,
				"The criterion used for comparing performance vectors.", USER_CRITERIA_NAMES, USER_CRITERIA_NAMES[0]);
		type.setExpert(false);
		types.add(type);

		types.add(new ParameterTypeList(PARAMETER_ADDITIONAL_PERFORMANCE_CRITERIA,
				"List of classes that implement com.rapidminer.operator.performance.PerformanceCriterion.",
				new ParameterTypeString("qualified_class_name", "Must be a fully qualified classname."),
				new ParameterTypeString("optional_parameter", "This string is passed to the constructor of the class.", "")));
		return types;
	}

	@Override
	protected boolean canEvaluate(int valueType) {
		return true;
	}

	@Override
	public boolean supportsCapability(OperatorCapability capability) {
		switch (capability) {
			case NUMERICAL_LABEL:
			case BINOMINAL_LABEL:
			case POLYNOMINAL_LABEL:
			case ONE_CLASS_LABEL:
				return true;
			case POLYNOMINAL_ATTRIBUTES:
			case BINOMINAL_ATTRIBUTES:
			case NUMERICAL_ATTRIBUTES:
			case WEIGHTED_EXAMPLES:
			case MISSING_VALUES:
				return true;
			case NO_LABEL:
			case UPDATABLE:
			case FORMULA_PROVIDER:
			default:
				return false;
		}
	}
}
