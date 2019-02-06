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
package com.rapidminer.operator.learner.functions.kernel;

import com.rapidminer.example.Attribute;
import com.rapidminer.example.ExampleSet;
import com.rapidminer.operator.OperatorDescription;
import com.rapidminer.operator.OperatorException;
import com.rapidminer.operator.UserError;
import com.rapidminer.operator.annotation.ResourceConsumptionEstimator;
import com.rapidminer.operator.learner.functions.kernel.jmysvm.examples.SVMExamples;
import com.rapidminer.operator.learner.functions.kernel.jmysvm.kernel.Kernel;
import com.rapidminer.operator.learner.functions.kernel.jmysvm.svm.SVMInterface;
import com.rapidminer.operator.learner.functions.kernel.jmysvm.svm.SVMpattern;
import com.rapidminer.operator.learner.functions.kernel.jmysvm.svm.SVMregression;
import com.rapidminer.operator.performance.EstimatedPerformance;
import com.rapidminer.operator.performance.PerformanceVector;
import com.rapidminer.parameter.ParameterType;
import com.rapidminer.parameter.ParameterTypeBoolean;
import com.rapidminer.parameter.ParameterTypeDouble;
import com.rapidminer.tools.OperatorResourceConsumptionHandler;
import com.rapidminer.tools.RandomGenerator;

import java.util.List;


/**
 * This learner uses the Java implementation of the support vector machine <em>mySVM</em> by Stefan
 * R&uuml;ping. This learning method can be used for both regression and classification and provides
 * a fast algorithm and good results for many learning tasks.
 * 
 * @rapidminer.reference Rueping/2000a
 * @rapidminer.reference Vapnik/98a
 * @rapidminer.index SVM
 * 
 * @author Ingo Mierswa
 */
public class JMySVMLearner extends AbstractMySVMLearner {

	/**
	 * The parameter name for &quot;Indicates if this learner should also return a performance
	 * estimation.&quot;
	 */
	public static final String PARAMETER_ESTIMATE_PERFORMANCE = "estimate_performance";

	/**
	 * The parameter name for &quot;A factor for the SVM complexity constant for positive
	 * examples&quot;
	 */
	public static final String PARAMETER_L_POS = "L_pos";

	/**
	 * The parameter name for &quot;A factor for the SVM complexity constant for negative
	 * examples&quot;
	 */
	public static final String PARAMETER_L_NEG = "L_neg";

	/**
	 * The parameter name for &quot;Insensitivity constant. No loss if prediction lies this close to
	 * true value&quot;
	 */
	public static final String PARAMETER_EPSILON = "epsilon";

	/** The parameter name for &quot;Epsilon for positive deviation only&quot; */
	public static final String PARAMETER_EPSILON_PLUS = "epsilon_plus";

	/** The parameter name for &quot;Epsilon for negative deviation only&quot; */
	public static final String PARAMETER_EPSILON_MINUS = "epsilon_minus";

	/** The parameter name for &quot;Adapts Cpos and Cneg to the relative size of the classes&quot; */
	public static final String PARAMETER_BALANCE_COST = "balance_cost";

	/** The parameter name for &quot;Use quadratic loss for positive deviation&quot; */
	public static final String PARAMETER_QUADRATIC_LOSS_POS = "quadratic_loss_pos";

	/** The parameter name for &quot;Use quadratic loss for negative deviation&quot; */
	public static final String PARAMETER_QUADRATIC_LOSS_NEG = "quadratic_loss_neg";

	public JMySVMLearner(OperatorDescription description) {
		super(description);
	}

	/** Indicates if the SVM is used for classification learning. */
	private boolean pattern = true;

	@Override
	public boolean shouldEstimatePerformance() {
		return getParameterAsBoolean(PARAMETER_ESTIMATE_PERFORMANCE);
	}

	@Override
	public boolean canEstimatePerformance() {
		return true;
	}

	/**
	 * Returns the estimated performances of this SVM. Does only work for classification tasks.
	 */
	@Override
	public PerformanceVector getEstimatedPerformance() throws OperatorException {
		if (!pattern) {
			throw new UserError(this, 912, this, "Cannot calculate leave one out estimation of error for regression tasks!");
		}
		double[] estVector = ((SVMpattern) getSVM()).getXiAlphaEstimation(getKernel());
		PerformanceVector pv = new PerformanceVector();
		pv.addCriterion(new EstimatedPerformance("xialpha_error", estVector[0], 1, true));
		pv.addCriterion(new EstimatedPerformance("xialpha_precision", estVector[1], 1, false));
		pv.addCriterion(new EstimatedPerformance("xialpha_recall", estVector[2], 1, false));
		pv.setMainCriterionName("xialpha_error");
		return pv;
	}

	@Override
	public AbstractMySVMModel createSVMModel(ExampleSet exampleSet, SVMExamples sVMExamples, Kernel kernel, int kernelType) {
		return new JMySVMModel(exampleSet, sVMExamples, kernel, kernelType);
	}

	@Override
	public SVMInterface createSVM(Attribute label, Kernel kernel, SVMExamples sVMExamples,
			com.rapidminer.example.ExampleSet rapidMinerExamples) throws OperatorException {
		if (label.isNominal()) {
			this.pattern = true;
			return new SVMpattern(this, kernel, sVMExamples, rapidMinerExamples, RandomGenerator.getGlobalRandomGenerator());
		} else {
			this.pattern = false;
			return new SVMregression(this, kernel, sVMExamples, rapidMinerExamples,
					RandomGenerator.getGlobalRandomGenerator());
		}
	}

	@Override
	public List<ParameterType> getParameterTypes() {
		List<ParameterType> types = super.getParameterTypes();
		types.add(new ParameterTypeDouble(PARAMETER_L_POS, "A factor for the SVM complexity constant for positive examples",
				0, Double.POSITIVE_INFINITY, 1.0d));
		types.add(new ParameterTypeDouble(PARAMETER_L_NEG, "A factor for the SVM complexity constant for negative examples",
				0, Double.POSITIVE_INFINITY, 1.0d));
		types.add(new ParameterTypeDouble(PARAMETER_EPSILON,
				"Insensitivity constant. No loss if prediction lies this close to true value", 0.0d,
				Double.POSITIVE_INFINITY, 0.0d));
		types.add(new ParameterTypeDouble(PARAMETER_EPSILON_PLUS, "Epsilon for positive deviation only", 0.0d,
				Double.POSITIVE_INFINITY, 0.0d));
		types.add(new ParameterTypeDouble(PARAMETER_EPSILON_MINUS, "Epsilon for negative deviation only", 0.0d,
				Double.POSITIVE_INFINITY, 0.0d));
		types.add(new ParameterTypeBoolean(PARAMETER_BALANCE_COST,
				"Adapts Cpos and Cneg to the relative size of the classes", false));
		types.add(new ParameterTypeBoolean(PARAMETER_QUADRATIC_LOSS_POS, "Use quadratic loss for positive deviation", false));
		types.add(new ParameterTypeBoolean(PARAMETER_QUADRATIC_LOSS_NEG, "Use quadratic loss for negative deviation", false));

		// deprecated parameters
		ParameterType type = new ParameterTypeBoolean(PARAMETER_ESTIMATE_PERFORMANCE,
				"Indicates if this learner should also return a performance estimation.", false);
		type.setDeprecated();
		types.add(type);
		return types;
	}

	@Override
	public ResourceConsumptionEstimator getResourceConsumptionEstimator() {
		return OperatorResourceConsumptionHandler.getResourceConsumptionEstimator(getExampleSetInputPort(),
				JMySVMLearner.class, null);
	}
}
