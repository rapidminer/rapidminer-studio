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
import com.rapidminer.example.Attributes;
import com.rapidminer.example.Example;
import com.rapidminer.example.ExampleSet;
import com.rapidminer.example.FastExample2SparseTransform;
import com.rapidminer.example.Tools;
import com.rapidminer.operator.Model;
import com.rapidminer.operator.OperatorCapability;
import com.rapidminer.operator.OperatorDescription;
import com.rapidminer.operator.OperatorException;
import com.rapidminer.operator.ProcessSetupError.Severity;
import com.rapidminer.operator.UserError;
import com.rapidminer.operator.annotation.ResourceConsumptionEstimator;
import com.rapidminer.operator.ports.metadata.AttributeMetaData;
import com.rapidminer.operator.ports.metadata.ExampleSetMetaData;
import com.rapidminer.operator.ports.metadata.MetaData;
import com.rapidminer.operator.ports.metadata.SimpleMetaDataError;
import com.rapidminer.operator.ports.metadata.SimplePrecondition;
import com.rapidminer.operator.ports.quickfix.CategorySelectionQuickFix;
import com.rapidminer.parameter.ParameterType;
import com.rapidminer.parameter.ParameterTypeBoolean;
import com.rapidminer.parameter.ParameterTypeCategory;
import com.rapidminer.parameter.ParameterTypeDouble;
import com.rapidminer.parameter.ParameterTypeInt;
import com.rapidminer.parameter.ParameterTypeList;
import com.rapidminer.parameter.ParameterTypeString;
import com.rapidminer.parameter.UndefinedParameterError;
import com.rapidminer.parameter.conditions.EqualTypeCondition;
import com.rapidminer.tools.OperatorResourceConsumptionHandler;

import java.util.Collections;
import java.util.Iterator;
import java.util.LinkedList;
import java.util.List;

import libsvm.Svm;
import libsvm.svm_model;
import libsvm.svm_node;
import libsvm.svm_parameter;
import libsvm.svm_problem;


/**
 * Applies the <a href="http://www.csie.ntu.edu.tw/~cjlin/libsvm">libsvm</a> learner by Chih-Chung
 * Chang and Chih-Jen Lin. The SVM is a powerful method for both classification and regression. This
 * operator supports the SVM types <code>C-SVC</code> and <code>nu-SVC</code> for classification
 * tasks and <code>epsilon-SVR</code> and <code>nu-SVR</code> for regression tasks. Supports also
 * multiclass learning and probability estimation based on Platt scaling for proper confidence
 * values after applying the learned model on a classification data set.
 * 
 * @rapidminer.index SVM
 * @rapidminer.reference Chang/Lin/2001a
 * @author Ingo Mierswa
 */
public class LibSVMLearner extends AbstractKernelBasedLearner {

	/**
	 * The parameter name for &quot;SVM for classification (C-SVC, nu-SVC), regression (epsilon-SVR,
	 * nu-SVR) and distribution estimation (one-class)&quot;
	 */
	public static final String PARAMETER_SVM_TYPE = "svm_type";

	/** The parameter name for &quot;The type of the kernel functions&quot; */
	public static final String PARAMETER_KERNEL_TYPE = "kernel_type";

	/** The parameter name for &quot;The degree for a polynomial kernel function.&quot; */
	public static final String PARAMETER_DEGREE = "degree";

	/**
	 * The parameter name for &quot;The parameter gamma for polynomial, rbf, and sigmoid kernel
	 * functions (0 means 1/#attributes).&quot;
	 */
	public static final String PARAMETER_GAMMA = "gamma";

	/**
	 * The parameter name for &quot;The parameter coef0 for polynomial and sigmoid kernel
	 * functions.&quot;
	 */
	public static final String PARAMETER_COEF0 = "coef0";

	/** The parameter name for &quot;The cost parameter C for c_svc, epsilon_svr, and nu_svr.&quot; */
	public static final String PARAMETER_C = "C";

	/** The parameter name for &quot;The parameter nu for nu_svc, one_class, and nu_svr.&quot; */
	public static final String PARAMETER_NU = "nu";

	/** The parameter name for &quot;Cache size in Megabyte.&quot; */
	public static final String PARAMETER_CACHE_SIZE = "cache_size";

	/** The parameter name for &quot;Tolerance of termination criterion.&quot; */
	public static final String PARAMETER_EPSILON = "epsilon";

	/** The parameter name for &quot;Tolerance of loss function of epsilon-SVR.&quot; */
	public static final String PARAMETER_P = "p";

	/**
	 * The parameter name for &quot;The weights w for all classes (first column: class name, second
	 * column: weight), i.e. set the parameters C of each class w * C (empty: using 1 for all
	 * classes where the weight was not defined).&quot;
	 */
	public static final String PARAMETER_CLASS_WEIGHTS = "class_weights";

	/** The parameter name for &quot;Whether to use the shrinking heuristics.&quot; */
	public static final String PARAMETER_SHRINKING = "shrinking";

	/**
	 * The parameter name for &quot;Indicates if proper confidence values should be
	 * calculated.&quot;
	 */
	public static final String PARAMETER_CALCULATE_CONFIDENCES = "calculate_confidences";

	/**
	 * The parameter name for &quot;Indicates if the traditional libsvm one-class classification
	 * behavior should be used.&quot;
	 */
	public static final String PARAMETER_ONECLASS_CLASSIFICATION = "one_class_classification";

	/**
	 * The parameter name for &quot;Indicates if proper confidence values should be
	 * calculated.&quot;
	 */
	public static final String PARAMETER_CONFIDENCE_FOR_MULTICLASS = "confidence_for_multiclass";

	/*
	 * What to do for a new LibSVM version (current version 2.84):
	 * ---------------------------------------------------------- - remove all System.out /
	 * System.err statements - make some fields public of svm_model (nr_class, l, nSV, label etc.) -
	 * replace Math.random() by RandomGenerator.getGlobalRandomGenerator().nextDouble() in svm.java
	 * - add labelValues to model and in train of Svm class - calculation of C for C=0 in
	 * checkParameters - add method getGenericKernel
	 */

	/** The different SVM types implemented by the LibSVM package. */
	public static final String[] SVM_TYPES = { "C-SVC", "nu-SVC", "one-class", "epsilon-SVR", "nu-SVR" };

	public static final int SVM_TYPE_C_SVC = 0;
	public static final int SVM_TYPE_NU_SVC = 1;
	public static final int SVM_TYPE_ONE_CLASS = 2;
	public static final int SVM_TYPE_EPS_SVR = 3;
	public static final int SVM_TYPE_NU_SVR = 4;

	/** The different kernel types implemented by the LibSVM package. */
	public static final String[] KERNEL_TYPES = { "linear", "poly", "rbf", "sigmoid", "precomputed" };

	public LibSVMLearner(OperatorDescription description) {
		super(description);
		getExampleSetInputPort().addPrecondition(new SimplePrecondition(getExampleSetInputPort(), null, false) {

			@Override
			public void makeAdditionalChecks(MetaData received) {
				if (received instanceof ExampleSetMetaData) {
					ExampleSetMetaData emd = (ExampleSetMetaData) received;
					switch (emd.hasSpecial(Attributes.LABEL_NAME)) {
						case NO:
						case UNKNOWN:
							return;
						case YES:
							try {
								AttributeMetaData label = emd.getLabelMetaData();
								if (label.isNominal()) {
									if (getParameterAsInt(PARAMETER_SVM_TYPE) == SVM_TYPE_NU_SVR
											|| getParameterAsInt(PARAMETER_SVM_TYPE) == SVM_TYPE_EPS_SVR) {
										getExampleSetInputPort()
												.addError(
														new SimpleMetaDataError(Severity.ERROR, getExampleSetInputPort(),
																Collections.singletonList(new CategorySelectionQuickFix(
																		LibSVMLearner.this, PARAMETER_SVM_TYPE,
																		new String[] { SVM_TYPES[SVM_TYPE_C_SVC],
																				SVM_TYPES[SVM_TYPE_NU_SVC],
																				SVM_TYPES[SVM_TYPE_ONE_CLASS] },
																		SVM_TYPES[getParameterAsInt(PARAMETER_SVM_TYPE)],
																		"Select appropriate " + PARAMETER_SVM_TYPE + " for "
																				+ OperatorCapability.POLYNOMINAL_LABEL)),
																"parameters.cannot_handle",
																OperatorCapability.POLYNOMINAL_LABEL, PARAMETER_SVM_TYPE,
																SVM_TYPES[getParameterAsInt(PARAMETER_SVM_TYPE)]));
									}
								} else if (label.isNumerical()) {
									if (getParameterAsInt(PARAMETER_SVM_TYPE) == SVM_TYPE_NU_SVC
											|| getParameterAsInt(PARAMETER_SVM_TYPE) == SVM_TYPE_C_SVC) {
										getExampleSetInputPort()
												.addError(
														new SimpleMetaDataError(Severity.ERROR, getExampleSetInputPort(),
																Collections.singletonList(new CategorySelectionQuickFix(
																		LibSVMLearner.this, PARAMETER_SVM_TYPE,
																		new String[] { SVM_TYPES[SVM_TYPE_NU_SVR],
																				SVM_TYPES[SVM_TYPE_EPS_SVR] },
																		SVM_TYPES[getParameterAsInt(PARAMETER_SVM_TYPE)],
																		"Select appropriate " + PARAMETER_SVM_TYPE + " for "
																				+ OperatorCapability.NUMERICAL_LABEL)),
																"parameters.cannot_handle",
																OperatorCapability.NUMERICAL_LABEL, PARAMETER_SVM_TYPE,
																SVM_TYPES[getParameterAsInt(PARAMETER_SVM_TYPE)]));
									}
								}
							} catch (UndefinedParameterError e) {
							}
					}
				}
			}
		});
	}

	@Override
	public boolean supportsCapability(OperatorCapability lc) {
		try {
			int type = getParameterAsInt(PARAMETER_SVM_TYPE);
			switch (lc) {
				case NUMERICAL_ATTRIBUTES:
				case FORMULA_PROVIDER:
					return true;
				case BINOMINAL_LABEL:
				case POLYNOMINAL_LABEL:
					if (type == SVM_TYPE_NU_SVC || type == SVM_TYPE_C_SVC) {
						return true;
					}
					break;
				case ONE_CLASS_LABEL:
					if (type == SVM_TYPE_ONE_CLASS) {
						return true;
					}
					break;
				case NUMERICAL_LABEL:
					if (type == SVM_TYPE_NU_SVR || type == SVM_TYPE_EPS_SVR) {
						return true;
					}
					break;
				default:
			}
		} catch (UndefinedParameterError e) {
		}
		return false;
	}

	/**
	 * Creates a data node row for the LibSVM (sparse format, i.e. each node keeps the index and the
	 * value if not default).
	 */
	protected static svm_node[] makeNodes(Example e, FastExample2SparseTransform ripper) {
		int[] nonDefaultIndices = ripper.getNonDefaultAttributeIndices(e);
		double[] nonDefaultValues = ripper.getNonDefaultAttributeValues(e, nonDefaultIndices);
		svm_node[] nodeArray = new svm_node[nonDefaultIndices.length];
		for (int a = 0; a < nonDefaultIndices.length; a++) {
			svm_node node = new svm_node();
			node.index = nonDefaultIndices[a];
			node.value = nonDefaultValues[a];
			nodeArray[a] = node;
		}
		return nodeArray;
	}

	/**
	 * Creates a support vector problem for the LibSVM.
	 * 
	 * @throws UserError
	 */
	private svm_problem getProblem(ExampleSet exampleSet) throws UserError {
		log("Creating LibSVM problem.");
		FastExample2SparseTransform ripper = new FastExample2SparseTransform(exampleSet);
		int nodeCount = 0;
		svm_problem problem = new svm_problem();
		problem.l = exampleSet.size();
		problem.y = new double[exampleSet.size()];
		problem.x = new svm_node[exampleSet.size()][];
		Iterator<Example> i = exampleSet.iterator();
		Attribute label = exampleSet.getAttributes().getLabel();
		int j = 0;
		while (i.hasNext()) {
			Example e = i.next();
			problem.x[j] = makeNodes(e, ripper);
			problem.y[j] = e.getValue(label);
			nodeCount += problem.x[j].length;
			j++;
		}
		log("Created " + nodeCount + " nodes for " + j + " examples.");
		return problem;
	}

	/**
	 * Creates a LibSVM parameter object based on the user defined parameters. If gamma is set to
	 * zero, it will be overwritten by 1 divided by the number of attributes.
	 */
	private svm_parameter getParameters(ExampleSet exampleSet) throws OperatorException {
		svm_parameter params = new svm_parameter();

		params.svm_type = getParameterAsInt(PARAMETER_SVM_TYPE);
		params.kernel_type = getParameterAsInt(PARAMETER_KERNEL_TYPE);
		params.degree = getParameterAsInt(PARAMETER_DEGREE);
		params.gamma = getParameterAsDouble(PARAMETER_GAMMA);
		if (params.gamma == 0) {
			params.gamma = 1.0 / exampleSet.size();
		}
		params.coef0 = getParameterAsDouble(PARAMETER_COEF0);
		params.nu = getParameterAsDouble(PARAMETER_NU);
		params.cache_size = getParameterAsInt(PARAMETER_CACHE_SIZE);
		params.C = getParameterAsDouble(PARAMETER_C);
		params.eps = getParameterAsDouble(PARAMETER_EPSILON);
		params.p = getParameterAsDouble(PARAMETER_P);
		params.shrinking = getParameterAsBoolean(PARAMETER_SHRINKING) ? 1 : 0;
		if (getParameterAsBoolean(PARAMETER_CALCULATE_CONFIDENCES)) {
			params.probability = 1; // necessary for probability estimation
		}

		// class weights (for C-SVC)
		if (params.svm_type == svm_parameter.C_SVC) {
			Attribute label = exampleSet.getAttributes().getLabel();
			if (label.isNominal()) {
				if (isParameterSet(PARAMETER_CLASS_WEIGHTS)) {
					double[] weights = new double[label.getMapping().size()];
					int[] weightLabelIndices = new int[label.getMapping().size()];
					for (int i = 0; i < weights.length; i++) {
						weights[i] = 1.0d;
						weightLabelIndices[i] = i;
					}
					List<String[]> classWeights = getParameterList(PARAMETER_CLASS_WEIGHTS);
					Iterator<String[]> i = classWeights.iterator();
					while (i.hasNext()) {
						String[] classWeightArray = i.next();
						String className = classWeightArray[0];
						double classWeight = Double.valueOf(classWeightArray[1]);
						int index = label.getMapping().getIndex(className);
						if ((index >= 0) && (index < weights.length)) {
							weights[index] = classWeight;
						}
					}
					// logging
					List<Double> weightList = new LinkedList<Double>();
					for (double d : weights) {
						weightList.add(d);
					}
					log(getName() + ": used class weights --> " + weightList);
					params.weight = weights;
					params.nr_weight = weights.length;
					params.weight_label = weightLabelIndices;
				}
			}
		}
		return params;
	}

	/** Learns a new SVM model with the LibSVM package. */
	@Override
	public Model learn(ExampleSet exampleSet) throws OperatorException {
		svm_parameter params = getParameters(exampleSet);

		if (exampleSet.size() < 2) {
			throw new UserError(this, 110, 2);
		}

		// check if example set contains missing values, if so fail because
		// this operator produces garbage with them
		Tools.onlyNonMissingValues(exampleSet, getOperatorClassName(), this);

		// check if svm type fits problem type
		Attribute label = exampleSet.getAttributes().getLabel();
		if (label.isNominal()) {
			if ((params.svm_type != SVM_TYPE_C_SVC) && (params.svm_type != SVM_TYPE_NU_SVC)
					&& (params.svm_type != SVM_TYPE_ONE_CLASS)) {
				throw new UserError(this, 102, SVM_TYPES[params.svm_type], label.getName());
			}

			// check for one class svm: Only works with mapping of label attribute of size 1
			if ((params.svm_type == SVM_TYPE_ONE_CLASS) && label.getMapping().size() > 1) {
				throw new UserError(this, 118, label.getName(), label.getMapping().size() + "", 1 + " for one-class svm");
			}
		} else {
			if ((params.svm_type != SVM_TYPE_EPS_SVR) && (params.svm_type != SVM_TYPE_NU_SVR)) {
				throw new UserError(this, 101, SVM_TYPES[params.svm_type], label.getName());
			}
		}

		svm_problem problem = getProblem(exampleSet);
		this.checkForStop();
		String errorMsg = Svm.svm_check_parameter(problem, params);
		if (errorMsg != null) {
			throw new UserError(this, 905, new Object[] { "libsvm", errorMsg });
		}
		log("Training LibSVM.");

		svm_model model = Svm.svm_train(problem, params, this);

		return new LibSVMModel(exampleSet, model, exampleSet.getAttributes().size(),
				getParameterAsBoolean(PARAMETER_CONFIDENCE_FOR_MULTICLASS));
	}

	@Override
	public List<ParameterType> getParameterTypes() {
		List<ParameterType> types = super.getParameterTypes();
		ParameterType type = new ParameterTypeCategory(
				PARAMETER_SVM_TYPE,
				"SVM for classification (C-SVC, nu-SVC), regression (epsilon-SVR, nu-SVR) and distribution estimation (one-class)",
				SVM_TYPES, 0);
		type.setExpert(false);
		types.add(type);
		type = new ParameterTypeCategory(PARAMETER_KERNEL_TYPE, "The type of the kernel functions", KERNEL_TYPES, 2);
		type.setExpert(false);
		types.add(type);
		type = new ParameterTypeInt(PARAMETER_DEGREE, "The degree for a polynomial kernel function.", 1, Integer.MAX_VALUE,
				3, false);
		type.registerDependencyCondition(new EqualTypeCondition(this, PARAMETER_KERNEL_TYPE, KERNEL_TYPES, false, 1));
		types.add(type);

		type = new ParameterTypeDouble(PARAMETER_GAMMA,
				"The parameter gamma for polynomial, rbf, and sigmoid kernel functions (0 means 1/#examples).", 0.0,
				Double.POSITIVE_INFINITY, 0.0d);
		type.registerDependencyCondition(new EqualTypeCondition(this, PARAMETER_KERNEL_TYPE, KERNEL_TYPES, false, 1, 2, 3));
		types.add(type);

		type = new ParameterTypeDouble(PARAMETER_COEF0, "The parameter coef0 for polynomial and sigmoid kernel functions.",
				Double.NEGATIVE_INFINITY, Double.POSITIVE_INFINITY, 0);
		type.registerDependencyCondition(new EqualTypeCondition(this, PARAMETER_KERNEL_TYPE, KERNEL_TYPES, false, 1, 4));
		types.add(type);

		type = new ParameterTypeDouble(PARAMETER_C, "The cost parameter C for c_svc, epsilon_svr, and nu_svr.", 0,
				Double.POSITIVE_INFINITY, 0);
		type.registerDependencyCondition(new EqualTypeCondition(this, PARAMETER_SVM_TYPE, SVM_TYPES, false, 0, 3, 4));
		type.setExpert(false);
		types.add(type);

		type = new ParameterTypeDouble(PARAMETER_NU, "The parameter nu for nu_svc, one_class, and nu_svr.", 0.0d, 0.5d, 0.5d);
		type.registerDependencyCondition(new EqualTypeCondition(this, PARAMETER_SVM_TYPE, SVM_TYPES, false, 1, 2, 4));
		type.setExpert(false);
		types.add(type);

		types.add(new ParameterTypeInt(PARAMETER_CACHE_SIZE, "Cache size in Megabyte.", 0, Integer.MAX_VALUE, 80));

		types.add(new ParameterTypeDouble(PARAMETER_EPSILON, "Tolerance of termination criterion.",
				Double.NEGATIVE_INFINITY, Double.POSITIVE_INFINITY, 0.001));
		type = new ParameterTypeDouble(PARAMETER_P, "Tolerance of loss function of epsilon-SVR.", Double.NEGATIVE_INFINITY,
				Double.POSITIVE_INFINITY, 0.1);
		type.registerDependencyCondition(new EqualTypeCondition(this, PARAMETER_SVM_TYPE, SVM_TYPES, false, 3));
		types.add(type);

		types.add(new ParameterTypeList(
				PARAMETER_CLASS_WEIGHTS,
				"The weights w for all classes (first column: class name, second column: weight), i.e. set the parameters C of each class w * C (empty: using 1 for all classes where the weight was not defined).",
				new ParameterTypeString("class_name", "The class name."), new ParameterTypeDouble("weight",
						"The weight for this class.", 0.0d, Double.POSITIVE_INFINITY, 1.0d)));
		types.add(new ParameterTypeBoolean(PARAMETER_SHRINKING, "Whether to use the shrinking heuristics.", true));
		type = new ParameterTypeBoolean(PARAMETER_CALCULATE_CONFIDENCES,
				"Indicates if proper confidence values should be calculated.", false);
		type.setExpert(false);
		types.add(type);
		types.add(new ParameterTypeBoolean(
				PARAMETER_CONFIDENCE_FOR_MULTICLASS,
				"Indicates if the class with the highest confidence should be selected in the multiclass setting. Uses binary majority vote over all 1-vs-1 classifiers otherwise (selected class must not be the one with highest confidence in that case).",
				true));

		return types;
	}

	@Override
	public ResourceConsumptionEstimator getResourceConsumptionEstimator() {
		return OperatorResourceConsumptionHandler.getResourceConsumptionEstimator(getExampleSetInputPort(),
				LibSVMLearner.class, null);
	}
}
