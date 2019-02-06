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
import com.rapidminer.example.Example;
import com.rapidminer.example.ExampleSet;
import com.rapidminer.operator.Model;
import com.rapidminer.operator.OperatorCapability;
import com.rapidminer.operator.OperatorDescription;
import com.rapidminer.operator.OperatorException;
import com.rapidminer.operator.ProcessStoppedException;
import com.rapidminer.operator.UserError;
import com.rapidminer.operator.learner.functions.kernel.gaussianprocess.Parameter;
import com.rapidminer.operator.learner.functions.kernel.gaussianprocess.Regression;
import com.rapidminer.operator.learner.functions.kernel.gaussianprocess.RegressionProblem;
import com.rapidminer.operator.learner.functions.kernel.rvm.kernel.Kernel;
import com.rapidminer.operator.learner.functions.kernel.rvm.kernel.KernelCauchy;
import com.rapidminer.operator.learner.functions.kernel.rvm.kernel.KernelEpanechnikov;
import com.rapidminer.operator.learner.functions.kernel.rvm.kernel.KernelGaussianCombination;
import com.rapidminer.operator.learner.functions.kernel.rvm.kernel.KernelLaplace;
import com.rapidminer.operator.learner.functions.kernel.rvm.kernel.KernelMultiquadric;
import com.rapidminer.operator.learner.functions.kernel.rvm.kernel.KernelPoly;
import com.rapidminer.operator.learner.functions.kernel.rvm.kernel.KernelRadial;
import com.rapidminer.operator.learner.functions.kernel.rvm.kernel.KernelSigmoid;
import com.rapidminer.parameter.ParameterType;
import com.rapidminer.parameter.ParameterTypeCategory;
import com.rapidminer.parameter.ParameterTypeDouble;
import com.rapidminer.parameter.ParameterTypeInt;
import com.rapidminer.parameter.conditions.EqualTypeCondition;

import java.util.Iterator;
import java.util.List;


/**
 * Gaussian Process (GP) Learner. The GP is a probabilistic method both for classification and
 * regression.
 * 
 * @author Piotr Kasprzak, Ingo Mierswa
 * @rapidminer.index GP
 */
public class GPLearner extends AbstractKernelBasedLearner {

	/** The parameter name for &quot;Regression&quot; */
	public static final String PARAMETER_GP_TYPE = "gp_type";

	/** The parameter name for &quot;The kind of kernel.&quot; */
	public static final String PARAMETER_KERNEL_TYPE = "kernel_type";

	/**
	 * The parameter name for &quot;The lengthscale r for rbf kernel functions (exp{-1.0 * r^-2 *
	 * ||x - bla||}).&quot;
	 */
	public static final String PARAMETER_KERNEL_LENGTHSCALE = "kernel_lengthscale";

	/** The parameter name for &quot;The degree used in the poly kernel.&quot; */
	public static final String PARAMETER_KERNEL_DEGREE = "kernel_degree";

	/** The parameter name for &quot;The bias used in the poly kernel.&quot; */
	public static final String PARAMETER_KERNEL_BIAS = "kernel_bias";

	/**
	 * The parameter name for &quot;The SVM kernel parameter sigma1 (Epanechnikov, Gaussian
	 * Combination, Multiquadric).&quot;
	 */
	public static final String PARAMETER_KERNEL_SIGMA1 = "kernel_sigma1";

	/** The parameter name for &quot;The SVM kernel parameter sigma2 (Gaussian Combination).&quot; */
	public static final String PARAMETER_KERNEL_SIGMA2 = "kernel_sigma2";

	/** The parameter name for &quot;The SVM kernel parameter sigma3 (Gaussian Combination).&quot; */
	public static final String PARAMETER_KERNEL_SIGMA3 = "kernel_sigma3";

	/**
	 * The parameter name for &quot;The SVM kernel parameter shift (polynomial, Multiquadric).&quot;
	 */
	public static final String PARAMETER_KERNEL_SHIFT = "kernel_shift";

	/** The parameter name for &quot;The SVM kernel parameter a (neural).&quot; */
	public static final String PARAMETER_KERNEL_A = "kernel_a";

	/** The parameter name for &quot;The SVM kernel parameter b (neural).&quot; */
	public static final String PARAMETER_KERNEL_B = "kernel_b";

	/** The parameter name for &quot;Maximum number of basis vectors to be used.&quot; */
	public static final String PARAMETER_MAX_BASIS_VECTORS = "max_basis_vectors";

	/** The parameter name for &quot;Tolerance for gamma induced projections&quot; */
	public static final String PARAMETER_EPSILON_TOL = "epsilon_tol";

	/** The parameter name for &quot;Tolerance for geometry induced projections&quot; */
	public static final String PARAMETER_GEOMETRICAL_TOL = "geometrical_tol";
	// public static final String[] GP_TYPES = { "Regression", "Classification" };
	public static final String[] KERNEL_TYPES = { "rbf", "cauchy", "laplace", "poly", "sigmoid", "Epanechnikov",
			"gaussian combination", "multiquadric" };

	public GPLearner(OperatorDescription description) {
		super(description);
	}

	@Override
	public boolean supportsCapability(OperatorCapability lc) {

		if (lc == com.rapidminer.operator.OperatorCapability.NUMERICAL_ATTRIBUTES) {
			return true;
		}

		// if (lc == com.rapidminer.operator.OperatorCapability.BINOMINAL_LABEL)
		// return true;

		if (lc == com.rapidminer.operator.OperatorCapability.NUMERICAL_LABEL) {
			return true;
		}

		return false;
	}

	@Override
	public Model learn(ExampleSet exampleSet) throws OperatorException {

		log("Creating GP-Learner.");

		Parameter parameter = new Parameter();

		int numExamples = exampleSet.size();

		/** Get user defined control parameters from RapidMiner */

		/* Maximum number of basis vectors to be used */
		parameter.maxBasisVectors = getParameterAsInt(PARAMETER_MAX_BASIS_VECTORS);

		/*
		 * Tolerance value: we project the current basis vector if it has a orthogonal distance to
		 * the linear span of the other basis vectors smaller than epsilon_tol
		 */
		parameter.epsilon_tol = getParameterAsDouble(PARAMETER_EPSILON_TOL);

		parameter.geometrical_tol = getParameterAsDouble(PARAMETER_GEOMETRICAL_TOL);

		/** Transfer input / target vectors into array form */

		log("Creating input / output vectors.");

		double[][] x = new double[numExamples][exampleSet.getAttributes().size()];
		double[][] t = new double[numExamples][1];
		Iterator<Example> reader = exampleSet.iterator();
		int k = 0;
		while (reader.hasNext()) {
			checkForStop();
			double[] targetVector = new double[1];
			Example e = reader.next();
			targetVector[0] = e.getLabel();
			x[k] = RVMModel.makeInputVector(e);
			t[k] = targetVector;
			k++;
		}

		/** Init hyperparameters with more or less sensible values (shouldn't be too important) */

		Attribute label = exampleSet.getAttributes().getLabel();

		// double variance = label.getVariance();

		/** Create kernel */

		log("Creating kernel.");

		Kernel kernel = createKernel();

		/** Create GP-Learner and learn the model */

		// String GPType = GP_TYPES[getParameterAsInt(PARAMETER_GP_TYPE)];
		com.rapidminer.operator.learner.functions.kernel.gaussianprocess.Model model = null;

		if (label.isNominal()) {
			throw new UserError(this, 102, getName(), label.getName());

			/** Classification problem */
			/*
			 * int[] c = new int[numExamples]; for (k = 0; k < numExamples; k++) { c[k] = (new
			 * Double(t[k][0])).intValue(); }
			 */

			// ClassificationProblem problem = new ClassificationProblem(x, c, kernels);
			//
			// if (RVMType.equals("Classification-RVM")) {
			// RVMClassification RVM = new RVMClassification(problem, parameter);
			// model = RVM.learn();
			// } else {
			// throw new UserError();
			// }
		} else {

			/** Regression problem */

			RegressionProblem problem = new RegressionProblem(x, t, kernel);

			// if (GPType.equals("Regression")) {
			Regression GP = new Regression(problem, parameter, this);
			try {
				model = GP.learn();
			} catch (ProcessStoppedException e) {
				throw new ProcessStoppedException(this);
			} catch (Exception e) {
				throw new OperatorException(e.getMessage());
			}
		}
		// }

		return new GPModel(exampleSet, model);
	}

	/**
	 * Create the appropriate kernel function depending on the RapidMiner - ui settings.
	 */

	public Kernel createKernel() throws OperatorException {

		Kernel kernel = null;

		double lengthScale = getParameterAsDouble(PARAMETER_KERNEL_LENGTHSCALE);
		double bias = getParameterAsDouble(PARAMETER_KERNEL_BIAS);
		double degree = getParameterAsDouble(PARAMETER_KERNEL_DEGREE);
		double a = getParameterAsDouble(PARAMETER_KERNEL_A);
		double b = getParameterAsDouble(PARAMETER_KERNEL_B);
		double sigma1 = getParameterAsDouble(PARAMETER_KERNEL_SIGMA1);
		double sigma2 = getParameterAsDouble(PARAMETER_KERNEL_SIGMA2);
		double sigma3 = getParameterAsDouble(PARAMETER_KERNEL_SIGMA3);
		double shift = getParameterAsDouble(PARAMETER_KERNEL_SHIFT);

		switch (getParameterAsInt(PARAMETER_KERNEL_TYPE)) {
			case 0:
				kernel = new KernelRadial(lengthScale);
				break;
			case 1:
				kernel = new KernelCauchy(lengthScale);
				break;
			case 2:
				kernel = new KernelLaplace(lengthScale);
				break;
			case 3:
				kernel = new KernelPoly(lengthScale, bias, degree);
				break;
			case 4:
				kernel = new KernelSigmoid(a, b);
				break;
			case 5:
				kernel = new KernelEpanechnikov(sigma1, degree);
				break;
			case 6:
				kernel = new KernelGaussianCombination(sigma1, sigma2, sigma3);
				break;
			case 7:
				kernel = new KernelMultiquadric(sigma1, shift);
				break;
			default:
				kernel = new KernelRadial(lengthScale);
		}

		return kernel;
	}

	@Override
	public List<ParameterType> getParameterTypes() {

		List<ParameterType> types = super.getParameterTypes();
		ParameterType type;

		type = new ParameterTypeCategory(PARAMETER_KERNEL_TYPE, "The kind of kernel.", KERNEL_TYPES, 0);
		type.setExpert(false);
		types.add(type);

		type = new ParameterTypeDouble(PARAMETER_KERNEL_LENGTHSCALE, "The lengthscale used in all kernels.", 0,
				Double.POSITIVE_INFINITY, 3.0);
		type.registerDependencyCondition(new EqualTypeCondition(this, PARAMETER_KERNEL_TYPE, KERNEL_TYPES, false, 0, 1, 2, 3));
		type.setExpert(false);
		types.add(type);

		type = new ParameterTypeDouble(PARAMETER_KERNEL_DEGREE, "The degree used in the poly kernel.", 0.0d,
				Double.POSITIVE_INFINITY, 2.0d);
		type.registerDependencyCondition(new EqualTypeCondition(this, PARAMETER_KERNEL_TYPE, KERNEL_TYPES, false, 3, 5));
		type.setExpert(false);
		types.add(type);

		type = new ParameterTypeDouble(PARAMETER_KERNEL_BIAS, "The bias used in the poly kernel.", 0,
				Double.POSITIVE_INFINITY, 1.0);
		type.registerDependencyCondition(new EqualTypeCondition(this, PARAMETER_KERNEL_TYPE, KERNEL_TYPES, false, 3));
		type.setExpert(false);
		types.add(type);

		type = new ParameterTypeDouble(PARAMETER_KERNEL_SIGMA1,
				"The SVM kernel parameter sigma1 (Epanechnikov, Gaussian Combination, Multiquadric).", 0.0d,
				Double.POSITIVE_INFINITY, 1.0d);
		type.registerDependencyCondition(new EqualTypeCondition(this, PARAMETER_KERNEL_TYPE, KERNEL_TYPES, false, 5, 6, 7));
		type.setExpert(false);
		types.add(type);

		type = new ParameterTypeDouble(PARAMETER_KERNEL_SIGMA2, "The SVM kernel parameter sigma2 (Gaussian Combination).",
				0.0d, Double.POSITIVE_INFINITY, 0.0d);
		type.registerDependencyCondition(new EqualTypeCondition(this, PARAMETER_KERNEL_TYPE, KERNEL_TYPES, false, 6));
		type.setExpert(false);
		types.add(type);

		type = new ParameterTypeDouble(PARAMETER_KERNEL_SIGMA3, "The SVM kernel parameter sigma3 (Gaussian Combination).",
				0.0d, Double.POSITIVE_INFINITY, 2.0d);
		type.registerDependencyCondition(new EqualTypeCondition(this, PARAMETER_KERNEL_TYPE, KERNEL_TYPES, false, 6));
		type.setExpert(false);
		types.add(type);

		type = new ParameterTypeDouble(PARAMETER_KERNEL_SHIFT, "The SVM kernel parameter shift (Multiquadric).",
				Double.NEGATIVE_INFINITY, Double.POSITIVE_INFINITY, 1.0d);
		type.registerDependencyCondition(new EqualTypeCondition(this, PARAMETER_KERNEL_TYPE, KERNEL_TYPES, false, 7));
		type.setExpert(false);
		types.add(type);

		type = new ParameterTypeDouble(PARAMETER_KERNEL_A, "The SVM kernel parameter a (neural).", Double.NEGATIVE_INFINITY,
				Double.POSITIVE_INFINITY, 1.0d);
		type.registerDependencyCondition(new EqualTypeCondition(this, PARAMETER_KERNEL_TYPE, KERNEL_TYPES, false, 4));
		type.setExpert(false);
		types.add(type);

		type = new ParameterTypeDouble(PARAMETER_KERNEL_B, "The SVM kernel parameter b (neural).", Double.NEGATIVE_INFINITY,
				Double.POSITIVE_INFINITY, 0.0d);
		type.registerDependencyCondition(new EqualTypeCondition(this, PARAMETER_KERNEL_TYPE, KERNEL_TYPES, false, 4));
		type.setExpert(false);
		types.add(type);

		type = new ParameterTypeInt(PARAMETER_MAX_BASIS_VECTORS, "Maximum number of basis vectors to be used.", 1,
				Integer.MAX_VALUE, 100);
		type.setExpert(false);
		types.add(type);

		type = new ParameterTypeDouble(PARAMETER_EPSILON_TOL, "Tolerance for gamma induced projections", 0,
				Double.POSITIVE_INFINITY, 1e-7);
		type.setExpert(true);
		types.add(type);

		type = new ParameterTypeDouble(PARAMETER_GEOMETRICAL_TOL, "Tolerance for geometry induced projections", 0,
				Double.POSITIVE_INFINITY, 1e-7);
		type.setExpert(true);
		types.add(type);

		return types;
	}

}
