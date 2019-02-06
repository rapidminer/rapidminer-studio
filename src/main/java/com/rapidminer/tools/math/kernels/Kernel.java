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
package com.rapidminer.tools.math.kernels;

import java.io.Serializable;
import java.util.Collection;
import java.util.LinkedList;

import com.rapidminer.example.Attribute;
import com.rapidminer.example.Example;
import com.rapidminer.example.ExampleSet;
import com.rapidminer.operator.Operator;
import com.rapidminer.operator.learner.functions.kernel.SupportVector;
import com.rapidminer.parameter.ParameterHandler;
import com.rapidminer.parameter.ParameterType;
import com.rapidminer.parameter.ParameterTypeCategory;
import com.rapidminer.parameter.ParameterTypeDouble;
import com.rapidminer.parameter.UndefinedParameterError;
import com.rapidminer.parameter.conditions.EqualTypeCondition;


/**
 * Returns the distance of two examples. The method {@link #init(ExampleSet)} must be invoked before
 * the correct distances can be returned. Please note that subclasses must provide an empty
 * constructor to allow kernel creation via reflection (for reading kernels from disk).
 *
 * @author Ingo Mierswa
 */
public abstract class Kernel implements Serializable {

	private static final long serialVersionUID = 581189377433816413L;

	/** The parameter name for &quot;The SVM kernel type&quot; */
	public static final String PARAMETER_KERNEL_TYPE = "kernel_type";

	/** The parameter name for &quot;The SVM kernel parameter gamma (RBF, anova).&quot; */
	public static final String PARAMETER_KERNEL_GAMMA = "kernel_gamma";

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
	 * The parameter name for &quot;The SVM kernel parameter degree (polynomial, anova,
	 * Epanechnikov).&quot;
	 */
	public static final String PARAMETER_KERNEL_DEGREE = "kernel_degree";

	/**
	 * The parameter name for &quot;The SVM kernel parameter shift (polynomial, Multiquadric).&quot;
	 */
	public static final String PARAMETER_KERNEL_SHIFT = "kernel_shift";

	/** The parameter name for &quot;The SVM kernel parameter a (neural).&quot; */
	public static final String PARAMETER_KERNEL_A = "kernel_a";

	/** The parameter name for &quot;The SVM kernel parameter b (neural).&quot; */
	public static final String PARAMETER_KERNEL_B = "kernel_b";

	/** The kernels which can be used for the EvoSVM. */
	public static final String[] KERNEL_TYPES = { "dot", "radial", "polynomial", "sigmoid", "anova", "epanechnikov",
			"gausian_combination", "multiquadric" };

	/** Indicates a linear kernel. */
	public static final int KERNEL_DOT = 0;

	/** Indicates a rbf kernel. */
	public static final int KERNEL_RADIAL = 1;

	/** Indicates a polynomial kernel. */
	public static final int KERNEL_POLYNOMIAL = 2;

	/** Indicates a sigmoid kernel. */
	public static final int KERNEL_SIGMOID = 3;

	/** Indicates an anova kernel. */
	public static final int KERNEL_ANOVA = 4;

	/** Indicates a Epanechnikov kernel. */
	public static final int KERNEL_EPANECHNIKOV = 5;

	/** Indicates a Gaussian combination kernel. */
	public static final int KERNEL_GAUSSIAN_COMBINATION = 6;

	/** Indicates a multiquadric kernel. */
	public static final int KERNEL_MULTIQUADRIC = 7;

	/** The complete distance matrix for this kernel and a given example set. */
	private transient KernelCache cache;

	private ExampleSet exampleSet;

	/**
	 * Must return one out of KERNEL_DOT, KERNEL_RADIAL, KERNEL_POLYNOMIAL, KERNEL_SIGMOID,
	 * KERNEL_ANOVA, KERNEL_EPANECHNIKOV, KERNEL_GAUSSIAN_COMBINATION, or KERNEL_MULTIQUADRIC.
	 */
	public abstract int getType();

	/** Subclasses must implement this method. */
	public abstract double calculateDistance(double[] x1, double[] x2);

	public abstract String getDistanceFormula(double[] x, String[] attributeNames);

	/**
	 * Calculates all distances and store them in a matrix to speed up optimization.
	 */
	public void init(ExampleSet exampleSet) {
		this.exampleSet = exampleSet;
		int exampleSetSize = exampleSet.size();
		if (exampleSetSize < 8000) {
			this.cache = new FullCache(exampleSet, this);
		} else {
			this.cache = new MapBasedCache(exampleSetSize);
		}
	}

	/** Returns the distance between the examples with the given indices. */
	public double getDistance(int x1, int x2) {
		double result = cache.get(x1, x2);
		if (Double.isNaN(result)) {
			result = calculateDistance(getAttributeValues(x1), getAttributeValues(x2));
			cache.store(x1, x2, result);
		}
		return result;
	}

	public double[] getAttributeValues(int i) {
		Example example = this.exampleSet.getExample(i);
		double[] values = new double[this.exampleSet.getAttributes().size()];
		int x = 0;
		for (Attribute attribute : exampleSet.getAttributes()) {
			values[x++] = example.getValue(attribute);
		}
		return values;
	}

	/** Calculates the inner product of the given vectors. */
	public double innerProduct(double[] x1, double[] x2) {
		double result = 0.0d;
		for (int i = 0; i < x1.length; i++) {
			result += x1[i] * x2[i];
		}
		return result;
	}

	/** Calculates the L2-norm, i.e. ||x-y||^2. */
	public double norm2(double[] x1, double[] x2) {
		double result = 0;
		for (int i = 0; i < x1.length; i++) {
			double factor = x1[i] - x2[i];
			result += factor * factor;
		}
		return result;
	}

	/** Calculates w*x from the given support vectors using this kernel function. */
	public double getSum(Collection<SupportVector> supportVectors, double[] currentX) {
		double sum = 0;
		for (SupportVector sv : supportVectors) {
			sum += sv.getY() * sv.getAlpha() * calculateDistance(sv.getX(), currentX);
		}
		return sum;
	}

	public static Kernel createKernel(ParameterHandler handler) throws UndefinedParameterError {
		int kernelType = handler.getParameterAsInt(PARAMETER_KERNEL_TYPE);
		if (kernelType == KERNEL_DOT) {
			return new DotKernel();
		} else if (kernelType == KERNEL_RADIAL) {
			RBFKernel kernel = new RBFKernel();
			kernel.setGamma(handler.getParameterAsDouble(PARAMETER_KERNEL_GAMMA));
			return kernel;
		} else if (kernelType == KERNEL_POLYNOMIAL) {
			PolynomialKernel kernel = new PolynomialKernel();
			kernel.setPolynomialParameters(handler.getParameterAsDouble(PARAMETER_KERNEL_DEGREE),
					handler.getParameterAsDouble(PARAMETER_KERNEL_SHIFT));
			return kernel;
		} else if (kernelType == KERNEL_SIGMOID) {
			SigmoidKernel kernel = new SigmoidKernel();
			kernel.setSigmoidParameters(handler.getParameterAsDouble(PARAMETER_KERNEL_A),
					handler.getParameterAsDouble(PARAMETER_KERNEL_B));
			return kernel;
		} else if (kernelType == KERNEL_ANOVA) {
			AnovaKernel kernel = new AnovaKernel();
			kernel.setGamma(handler.getParameterAsDouble(PARAMETER_KERNEL_GAMMA));
			kernel.setDegree(handler.getParameterAsDouble(PARAMETER_KERNEL_DEGREE));
			return kernel;
		} else if (kernelType == KERNEL_EPANECHNIKOV) {
			EpanechnikovKernel kernel = new EpanechnikovKernel();
			kernel.setSigma(handler.getParameterAsDouble(PARAMETER_KERNEL_SIGMA1));
			kernel.setDegree(handler.getParameterAsDouble(PARAMETER_KERNEL_DEGREE));
			return kernel;
		} else if (kernelType == KERNEL_GAUSSIAN_COMBINATION) {
			GaussianCombinationKernel kernel = new GaussianCombinationKernel();
			kernel.setSigma1(handler.getParameterAsDouble(PARAMETER_KERNEL_SIGMA1));
			kernel.setSigma2(handler.getParameterAsDouble(PARAMETER_KERNEL_SIGMA2));
			kernel.setSigma3(handler.getParameterAsDouble(PARAMETER_KERNEL_SIGMA3));
			return kernel;
		} else if (kernelType == KERNEL_MULTIQUADRIC) {
			MultiquadricKernel kernel = new MultiquadricKernel();
			kernel.setSigma(handler.getParameterAsDouble(PARAMETER_KERNEL_SIGMA1));
			kernel.setShift(handler.getParameterAsDouble(PARAMETER_KERNEL_SHIFT));
			return kernel;
		} else {
			return null;
		}
	}

	public static Collection<ParameterType> getParameters(Operator parameterHandler) {
		Collection<ParameterType> types = new LinkedList<ParameterType>();
		ParameterType type = new ParameterTypeCategory(PARAMETER_KERNEL_TYPE, "The kernel type", Kernel.KERNEL_TYPES,
				Kernel.KERNEL_RADIAL);
		type.setExpert(false);
		types.add(type);
		type = new ParameterTypeDouble(PARAMETER_KERNEL_GAMMA, "The kernel parameter gamma.", 0.0d,
				Double.POSITIVE_INFINITY, 1.0d);
		type.setExpert(false);
		type.registerDependencyCondition(new EqualTypeCondition(parameterHandler, PARAMETER_KERNEL_TYPE,
				Kernel.KERNEL_TYPES, false, new int[] { Kernel.KERNEL_RADIAL, Kernel.KERNEL_ANOVA }));
		types.add(type);
		type = new ParameterTypeDouble(PARAMETER_KERNEL_SIGMA1, "The kernel parameter sigma1.", 0.0d,
				Double.POSITIVE_INFINITY, 1.0d);
		type.registerDependencyCondition(new EqualTypeCondition(parameterHandler, PARAMETER_KERNEL_TYPE,
				Kernel.KERNEL_TYPES, false, new int[] { Kernel.KERNEL_EPANECHNIKOV, Kernel.KERNEL_GAUSSIAN_COMBINATION,
						KERNEL_MULTIQUADRIC }));
		types.add(type);
		type = new ParameterTypeDouble(PARAMETER_KERNEL_SIGMA2, "The kernel parameter sigma2.", 0.0d,
				Double.POSITIVE_INFINITY, 0.0d);
		type.registerDependencyCondition(new EqualTypeCondition(parameterHandler, PARAMETER_KERNEL_TYPE,
				Kernel.KERNEL_TYPES, false, new int[] { Kernel.KERNEL_GAUSSIAN_COMBINATION }));
		types.add(type);
		type = new ParameterTypeDouble(PARAMETER_KERNEL_SIGMA3, "The kernel parameter sigma3.", 0.0d,
				Double.POSITIVE_INFINITY, 2.0d);
		type.registerDependencyCondition(new EqualTypeCondition(parameterHandler, PARAMETER_KERNEL_TYPE,
				Kernel.KERNEL_TYPES, false, new int[] { Kernel.KERNEL_GAUSSIAN_COMBINATION }));
		types.add(type);
		type = new ParameterTypeDouble(PARAMETER_KERNEL_DEGREE, "The kernel parameter degree.", 0.0d,
				Double.POSITIVE_INFINITY, 3.0d);
		type.setExpert(false);
		type.registerDependencyCondition(new EqualTypeCondition(parameterHandler, PARAMETER_KERNEL_TYPE,
				Kernel.KERNEL_TYPES, false, new int[] { Kernel.KERNEL_POLYNOMIAL, Kernel.KERNEL_ANOVA, KERNEL_EPANECHNIKOV }));
		types.add(type);
		type = new ParameterTypeDouble(PARAMETER_KERNEL_SHIFT, "The kernel parameter shift.", Double.NEGATIVE_INFINITY,
				Double.POSITIVE_INFINITY, 1.0d);
		type.registerDependencyCondition(new EqualTypeCondition(parameterHandler, PARAMETER_KERNEL_TYPE,
				Kernel.KERNEL_TYPES, false, new int[] { Kernel.KERNEL_POLYNOMIAL, KERNEL_MULTIQUADRIC }));
		types.add(type);
		type = new ParameterTypeDouble(PARAMETER_KERNEL_A, "The kernel parameter a.", Double.NEGATIVE_INFINITY,
				Double.POSITIVE_INFINITY, 1.0d);
		type.registerDependencyCondition(new EqualTypeCondition(parameterHandler, PARAMETER_KERNEL_TYPE,
				Kernel.KERNEL_TYPES, false, new int[] { Kernel.KERNEL_SIGMOID }));
		types.add(type);
		type = new ParameterTypeDouble(PARAMETER_KERNEL_B, "The kernel parameter b.", Double.NEGATIVE_INFINITY,
				Double.POSITIVE_INFINITY, 0.0d);
		type.registerDependencyCondition(new EqualTypeCondition(parameterHandler, PARAMETER_KERNEL_TYPE,
				Kernel.KERNEL_TYPES, false, new int[] { Kernel.KERNEL_SIGMOID }));
		types.add(type);
		return types;
	}
}
