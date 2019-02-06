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
import com.rapidminer.operator.Model;
import com.rapidminer.operator.OperatorCapability;
import com.rapidminer.operator.OperatorDescription;
import com.rapidminer.operator.OperatorException;
import com.rapidminer.operator.UserError;
import com.rapidminer.operator.annotation.ResourceConsumptionEstimator;
import com.rapidminer.operator.learner.functions.kernel.jmysvm.examples.SVMExamples;
import com.rapidminer.operator.learner.functions.kernel.jmysvm.kernel.Kernel;
import com.rapidminer.operator.learner.functions.kernel.jmysvm.kernel.KernelDot;
import com.rapidminer.operator.learner.functions.kernel.jmysvm.svm.SVMInterface;
import com.rapidminer.operator.learner.functions.kernel.jmysvm.svm.SVMpattern;
import com.rapidminer.operator.learner.functions.kernel.jmysvm.svm.SVMregression;
import com.rapidminer.parameter.ParameterType;
import com.rapidminer.parameter.ParameterTypeBoolean;
import com.rapidminer.parameter.ParameterTypeDouble;
import com.rapidminer.parameter.ParameterTypeInt;
import com.rapidminer.tools.OperatorResourceConsumptionHandler;
import com.rapidminer.tools.RandomGenerator;

import java.util.List;


/**
 * This class implements a special case of the MySVM by restricting it to the linear (dot) kernel.
 * This way the weights of the linear combination can be extracted and stored solely in the
 * resulting model. The model is optimized for small size / fast store and retrieve operations as
 * well as time efficient during application.
 * 
 * @author Sebastian Land
 */
public class LinearMySVMLearner extends AbstractKernelBasedLearner {

	/** The parameter name for &quot;Size of the cache for kernel evaluations im MB &quot; */
	public static final String PARAMETER_KERNEL_CACHE = "kernel_cache";

	/** The parameter name for &quot;Precision on the KKT conditions&quot; */
	public static final String PARAMETER_CONVERGENCE_EPSILON = "convergence_epsilon";

	/** The parameter name for &quot;Stop after this many iterations&quot; */
	public static final String PARAMETER_MAX_ITERATIONS = "max_iterations";

	/**
	 * The parameter name for &quot;Scale the example values and store the scaling parameters for
	 * test set.&quot;
	 */
	public static final String PARAMETER_SCALE = "scale";

	public static final String PARAMETER_C = "C";

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

	/** Indicates a linear kernel. */
	public static final int KERNEL_DOT = 0;

	/** The SVM example set. */
	private com.rapidminer.operator.learner.functions.kernel.jmysvm.examples.SVMExamples svmExamples;

	public LinearMySVMLearner(OperatorDescription description) {
		super(description);
	}

	protected SVMInterface createSVM(Attribute label, Kernel kernel, SVMExamples sVMExamples,
			com.rapidminer.example.ExampleSet rapidMinerExamples) throws OperatorException {
		if (label.isNominal()) {
			return new SVMpattern(this, kernel, sVMExamples, rapidMinerExamples, RandomGenerator.getGlobalRandomGenerator());
		} else {
			return new SVMregression(this, kernel, sVMExamples, rapidMinerExamples,
					RandomGenerator.getGlobalRandomGenerator());
		}
	}

	@Override
	public Model learn(ExampleSet exampleSet) throws OperatorException {
		Attribute label = exampleSet.getAttributes().getLabel();
		if ((label.isNominal()) && (label.getMapping().size() != 2)) {
			throw new UserError(this, 114, getName(), label.getName());
		}
		this.svmExamples = new com.rapidminer.operator.learner.functions.kernel.jmysvm.examples.SVMExamples(exampleSet,
				label, getParameterAsBoolean(PARAMETER_SCALE));

		// kernel
		int cacheSize = getParameterAsInt(PARAMETER_KERNEL_CACHE);
		Kernel kernel = new KernelDot();
		kernel.init(svmExamples, cacheSize);

		// SVM
		SVMInterface svm = createSVM(label, kernel, svmExamples, exampleSet);
		svm.init(kernel, svmExamples);
		svm.train();

		LinearMySVMModel model = new LinearMySVMModel(exampleSet, svmExamples, kernel, KERNEL_DOT);
		this.svmExamples = null;
		return model;
	}

	@Override
	public boolean supportsCapability(OperatorCapability lc) {
		if (lc == OperatorCapability.NUMERICAL_ATTRIBUTES) {
			return true;
		}
		if (lc == OperatorCapability.BINOMINAL_LABEL) {
			return true;
		}
		if (lc == OperatorCapability.NUMERICAL_LABEL) {
			return true;
		}
		if (lc == OperatorCapability.WEIGHTED_EXAMPLES) {
			return true;
		}
		if (lc == OperatorCapability.FORMULA_PROVIDER) {
			return true;
		}
		return false;
	}

	@Override
	public List<ParameterType> getParameterTypes() {
		List<ParameterType> types = super.getParameterTypes();
		types.add(new ParameterTypeInt(PARAMETER_KERNEL_CACHE, "Size of the cache for kernel evaluations im MB ", 0,
				Integer.MAX_VALUE, 200));
		ParameterType type = new ParameterTypeDouble(PARAMETER_C,
				"The SVM complexity constant. Use -1 for different C values for positive and negative.", -1,
				Double.POSITIVE_INFINITY, 0.0d);
		type.setExpert(false);
		types.add(type);
		type = new ParameterTypeDouble(PARAMETER_CONVERGENCE_EPSILON, "Precision on the KKT conditions", 0.0d,
				Double.POSITIVE_INFINITY, 1e-3);
		types.add(type);
		types.add(new ParameterTypeInt(PARAMETER_MAX_ITERATIONS, "Stop after this many iterations", 1, Integer.MAX_VALUE,
				100000));
		types.add(new ParameterTypeBoolean(PARAMETER_SCALE,
				"Scale the example values and store the scaling parameters for test set.", true));

		types.add(new ParameterTypeDouble(JMySVMLearner.PARAMETER_L_POS,
				"A factor for the SVM complexity constant for positive examples", 0, Double.POSITIVE_INFINITY, 1.0d));
		types.add(new ParameterTypeDouble(JMySVMLearner.PARAMETER_L_NEG,
				"A factor for the SVM complexity constant for negative examples", 0, Double.POSITIVE_INFINITY, 1.0d));
		types.add(new ParameterTypeDouble(JMySVMLearner.PARAMETER_EPSILON,
				"Insensitivity constant. No loss if prediction lies this close to true value", 0.0d,
				Double.POSITIVE_INFINITY, 0.0d));
		types.add(new ParameterTypeDouble(JMySVMLearner.PARAMETER_EPSILON_PLUS, "Epsilon for positive deviation only", 0.0d,
				Double.POSITIVE_INFINITY, 0.0d));
		types.add(new ParameterTypeDouble(JMySVMLearner.PARAMETER_EPSILON_MINUS, "Epsilon for negative deviation only",
				0.0d, Double.POSITIVE_INFINITY, 0.0d));
		types.add(new ParameterTypeBoolean(JMySVMLearner.PARAMETER_BALANCE_COST,
				"Adapts Cpos and Cneg to the relative size of the classes", false));
		types.add(new ParameterTypeBoolean(JMySVMLearner.PARAMETER_QUADRATIC_LOSS_POS,
				"Use quadratic loss for positive deviation", false));
		types.add(new ParameterTypeBoolean(JMySVMLearner.PARAMETER_QUADRATIC_LOSS_NEG,
				"Use quadratic loss for negative deviation", false));

		return types;
	}

	@Override
	public ResourceConsumptionEstimator getResourceConsumptionEstimator() {
		return OperatorResourceConsumptionHandler.getResourceConsumptionEstimator(getExampleSetInputPort(),
				LinearMySVMLearner.class, null);
	}
}
