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

import com.rapidminer.example.Attribute;
import com.rapidminer.example.ExampleSet;
import com.rapidminer.operator.OperatorCapability;
import com.rapidminer.operator.OperatorDescription;
import com.rapidminer.operator.OperatorException;
import com.rapidminer.parameter.UndefinedParameterError;
import com.rapidminer.tools.math.ROCBias;

import java.util.LinkedList;
import java.util.List;


/**
 * <p>
 * In contrast to the other performance evaluation methods, this performance evaluator operator can
 * be used for all types of learning tasks. It will automatically determine the learning task type
 * and will calculate the most common criteria for this type. For more sophisticated performance
 * calculations, you should check the operators {@link RegressionPerformanceEvaluator},
 * {@link PolynominalClassificationPerformanceEvaluator}, or
 * {@link BinominalClassificationPerformanceEvaluator}. You can even simply write your own
 * performance measure and calculate it with the operator {@link UserBasedPerformanceEvaluator}.
 * </p>
 * 
 * <p>
 * The operator expects a test {@link ExampleSet} as input, whose elements have both true and
 * predicted labels, and delivers as output a list of most common performance values for the
 * provided learning task type (regression or (binary) classification. If an input performance
 * vector was already given, this is used for keeping the performance values.
 * </p>
 * 
 * @author Ingo Mierswa
 */
public class SimplePerformanceEvaluator extends AbstractPerformanceEvaluator {

	private ExampleSet testSet = null;

	public SimplePerformanceEvaluator(OperatorDescription description) {
		super(description);
	}

	/** Does nothing. */
	@Override
	protected void checkCompatibility(ExampleSet exampleSet) throws OperatorException {}

	/** Returns null. */
	@Override
	protected double[] getClassWeights(Attribute label) throws UndefinedParameterError {
		return null;
	}

	/** Uses this example set in order to create appropriate criteria. */
	@Override
	protected void init(ExampleSet testSet) {
		this.testSet = testSet;
	}

	/** Returns false. */
	@Override
	protected boolean showSkipNaNLabelsParameter() {
		return false;
	}

	/** Returns false. */
	@Override
	protected boolean showComparatorParameter() {
		return false;
	}

	@Override
	public List<PerformanceCriterion> getCriteria() {
		List<PerformanceCriterion> allCriteria = new LinkedList<PerformanceCriterion>();
		if (this.testSet != null) {
			Attribute label = this.testSet.getAttributes().getLabel();
			if (label != null) {
				if (label.isNominal()) {
					if (label.getMapping().size() == 2) {
						// add most important binominal classification criteria
						allCriteria.add(new MultiClassificationPerformance(MultiClassificationPerformance.ACCURACY));
						allCriteria.add(new BinaryClassificationPerformance(BinaryClassificationPerformance.PRECISION));
						allCriteria.add(new BinaryClassificationPerformance(BinaryClassificationPerformance.RECALL));
						allCriteria.add(new AreaUnderCurve(ROCBias.OPTIMISTIC));
						allCriteria.add(new AreaUnderCurve(ROCBias.NEUTRAL));
						allCriteria.add(new AreaUnderCurve(ROCBias.PESSIMISTIC));
					} else {
						// add most important polynominal classification criteria
						allCriteria.add(new MultiClassificationPerformance(MultiClassificationPerformance.ACCURACY));
						allCriteria.add(new MultiClassificationPerformance(MultiClassificationPerformance.KAPPA));
					}
				} else {
					// add most important regression criteria
					allCriteria.add(new RootMeanSquaredError());
					allCriteria.add(new SquaredError());
				}
			}
		}
		this.testSet = null;
		return allCriteria;
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
