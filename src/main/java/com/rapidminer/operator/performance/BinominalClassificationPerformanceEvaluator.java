/**
 * Copyright (C) 2001-2016 by RapidMiner and the contributors
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

import java.util.LinkedList;
import java.util.List;
import java.util.logging.Level;

import com.rapidminer.example.Attribute;
import com.rapidminer.example.ExampleSet;
import com.rapidminer.example.Tools;
import com.rapidminer.operator.OperatorCapability;
import com.rapidminer.operator.OperatorDescription;
import com.rapidminer.operator.OperatorException;
import com.rapidminer.operator.UserError;
import com.rapidminer.parameter.UndefinedParameterError;
import com.rapidminer.tools.LogService;
import com.rapidminer.tools.Ontology;


/**
 * <p>
 * This performance evaluator operator should be used for classification tasks, i.e. in cases where
 * the label attribute has a binominal value type. Other polynominal classification tasks, i.e.
 * tasks with more than two classes can be handled by the
 * {@link PolynominalClassificationPerformanceEvaluator} operator. This operator expects a test
 * {@link ExampleSet} as input, whose elements have both true and predicted labels, and delivers as
 * output a list of performance values according to a list of performance criteria that it
 * calculates. If an input performance vector was already given, this is used for keeping the
 * performance values.
 * </p>
 *
 * <p>
 * All of the performance criteria can be switched on using boolean parameters. Their values can be
 * queried by a ProcessLogOperator using the same names. The main criterion is used for comparisons
 * and need to be specified only for processes where performance vectors are compared, e.g. feature
 * selection or other meta optimization process setups. If no other main criterion was selected, the
 * first criterion in the resulting performance vector will be assumed to be the main criterion.
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
public class BinominalClassificationPerformanceEvaluator extends AbstractPerformanceEvaluator {

	/** The proper criteria to the names. */
	private static final Class<?>[] SIMPLE_CRITERIA_CLASSES = {
		com.rapidminer.operator.performance.AreaUnderCurve.Optimistic.class,
		com.rapidminer.operator.performance.AreaUnderCurve.Optimistic.class,
		com.rapidminer.operator.performance.AreaUnderCurve.Neutral.class,
		com.rapidminer.operator.performance.AreaUnderCurve.Pessimistic.class };

	public BinominalClassificationPerformanceEvaluator(OperatorDescription description) {
		super(description);
	}

	@Override
	protected void checkCompatibility(ExampleSet exampleSet) throws OperatorException {
		Tools.isLabelled(exampleSet);
		Tools.isNonEmpty(exampleSet);

		Attribute label = exampleSet.getAttributes().getLabel();
		if (!label.isNominal()) {
			throw new UserError(this, 101, "the calculation of performance criteria for binominal classification tasks",
					label.getName());
		}

		if (label.getMapping().size() != 2) {
			throw new UserError(this, 114, "the calculation of performance criteria for binominal classification tasks",
					label.getName());
		}
	}

	/** Returns null. */
	@Override
	protected double[] getClassWeights(Attribute label) throws UndefinedParameterError {
		return null;
	}

	@Override
	public List<PerformanceCriterion> getCriteria() {
		List<PerformanceCriterion> performanceCriteria = new LinkedList<PerformanceCriterion>();

		// standard classification measures
		for (int i = 0; i < MultiClassificationPerformance.NAMES.length; i++) {
			performanceCriteria.add(new MultiClassificationPerformance(i));
		}

		for (int i = 0; i < SIMPLE_CRITERIA_CLASSES.length; i++) {
			try {
				performanceCriteria.add((PerformanceCriterion) SIMPLE_CRITERIA_CLASSES[i].newInstance());
			} catch (InstantiationException e) {
				// LogService.getGlobal().logError("Cannot instantiate " +
				// SIMPLE_CRITERIA_CLASSES[i] + ". Skipping...");
				LogService
				.getRoot()
				.log(Level.SEVERE,
						"com.rapidminer.operator.performance.BinominalClassificationPerformanceEvaluator.instantiating_simple_criteria_classes_error",
						SIMPLE_CRITERIA_CLASSES[i]);
			} catch (IllegalAccessException e) {
				// LogService.getGlobal().logError("Cannot instantiate " +
				// SIMPLE_CRITERIA_CLASSES[i] + ". Skipping...");
				LogService
				.getRoot()
				.log(Level.SEVERE,
						"com.rapidminer.operator.performance.BinominalClassificationPerformanceEvaluator.instantiating_simple_criteria_classes_error",
						SIMPLE_CRITERIA_CLASSES[i]);
			}
		}

		// binary classification criteria
		for (int i = 0; i < BinaryClassificationPerformance.NAMES.length; i++) {
			performanceCriteria.add(new BinaryClassificationPerformance(i));
		}
		return performanceCriteria;
	}

	@Override
	protected boolean canEvaluate(int valueType) {
		return Ontology.ATTRIBUTE_VALUE_TYPE.isA(valueType, Ontology.BINOMINAL);
	}

	@Override
	public boolean supportsCapability(OperatorCapability capability) {
		switch (capability) {
			case BINOMINAL_LABEL:
				return true;
			case POLYNOMINAL_LABEL:
			case NUMERICAL_LABEL:
			case ONE_CLASS_LABEL:
				return false;
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
