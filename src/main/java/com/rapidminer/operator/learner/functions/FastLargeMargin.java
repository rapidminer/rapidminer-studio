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
package com.rapidminer.operator.learner.functions;

import com.rapidminer.example.Attribute;
import com.rapidminer.example.Example;
import com.rapidminer.example.ExampleSet;
import com.rapidminer.example.FastExample2SparseTransform;
import com.rapidminer.operator.Model;
import com.rapidminer.operator.OperatorCapability;
import com.rapidminer.operator.OperatorDescription;
import com.rapidminer.operator.OperatorException;
import com.rapidminer.operator.UserError;
import com.rapidminer.operator.learner.AbstractLearner;
import com.rapidminer.operator.learner.PredictionModel;
import com.rapidminer.parameter.ParameterType;
import com.rapidminer.parameter.ParameterTypeBoolean;
import com.rapidminer.parameter.ParameterTypeCategory;
import com.rapidminer.parameter.ParameterTypeDouble;
import com.rapidminer.parameter.ParameterTypeList;
import com.rapidminer.parameter.ParameterTypeString;
import de.bwaldvogel.liblinear.FeatureNode;
import de.bwaldvogel.liblinear.Linear;
import de.bwaldvogel.liblinear.Parameter;
import de.bwaldvogel.liblinear.Problem;
import de.bwaldvogel.liblinear.SolverType;

import java.util.Iterator;
import java.util.LinkedList;
import java.util.List;


/**
 * Applies a fast margin learner based on the linear support vector learning scheme proposed by
 * R.-E. Fan, K.-W. Chang, C.-J. Hsieh, X.-R. Wang, and C.-J. Lin. Although the result is similar to
 * those delivered by classical SVM or logistic regression implementations, this linear classifier
 * is able to work on data set with millions of examples and attributes.
 *
 * @rapidminer.index SVM
 * @author Ingo Mierswa
 */
public class FastLargeMargin extends AbstractLearner {

	public static final String PARAMETER_SOLVER = "solver";

	/** The parameter name for &quot;The cost parameter C for c_svc, epsilon_svr, and nu_svr.&quot; */
	public static final String PARAMETER_C = "C";

	/** The parameter name for &quot;Tolerance of termination criterion.&quot; */
	public static final String PARAMETER_EPSILON = "epsilon";

	/**
	 * The parameter name for &quot;The weights w for all classes (first column: class name, second
	 * column: weight), i.e. set the parameters C of each class w * C (empty: using 1 for all
	 * classes where the weight was not defined).&quot;
	 */
	public static final String PARAMETER_CLASS_WEIGHTS = "class_weights";

	public static final String PARAMETER_USE_BIAS = "use_bias";

	/*
	 * What to do for a new LibLinear version (current version 1.33):
	 * -------------------------------------------------------------- - set field DEBUG_OUTPUT in
	 * class Linear to false - remove some additional system.out statements - make some fields
	 * public of Model (nr_class, l, nSV, label etc.)
	 */

	/** The different SVM types implemented by the LibSVM package. */
	public static final String[] SOLVER = { "L2 SVM Dual", "L2 SVM Primal", "L2 Logistic Regression", "L1 SVM Dual" };

	public static final int SOLVER_L2_SVM_DUAL = 0;
	public static final int SOLVER_L2_SVM_PRIMAL = 1;
	public static final int SOLVER_L2_LR = 2;
	public static final int SOLVER_L1_SVM_DUAL = 3;

	public FastLargeMargin(OperatorDescription description) {
		super(description);
	}

	@Override
	public boolean supportsCapability(OperatorCapability lc) {
		switch (lc) {
			case NUMERICAL_ATTRIBUTES:
			case BINOMINAL_LABEL:
				return true;
			default:
				return false;
		}
	}

	/**
	 * Creates a data node row for the LibSVM (sparse format, i.e. each node keeps the index and the
	 * value if not default).
	 */
	public static FeatureNode[] makeNodes(Example e, FastExample2SparseTransform ripper, boolean useBias) {
		int[] nonDefaultIndices = ripper.getNonDefaultAttributeIndices(e);
		double[] nonDefaultValues = ripper.getNonDefaultAttributeValues(e, nonDefaultIndices);
		int offset = 0;
		if (useBias) {
			offset = 1;
		}
		FeatureNode[] nodeArray = new FeatureNode[nonDefaultIndices.length + offset];
		for (int a = 0; a < nonDefaultIndices.length; a++) {
			FeatureNode node = new FeatureNode(nonDefaultIndices[a] + 1, nonDefaultValues[a]);
			nodeArray[a] = node;
		}
		if (useBias) {
			// bias index is number of attributes +1
			nodeArray[nodeArray.length - 1] = new FeatureNode(e.getAttributes().size() + 1, 1);
		}

		return nodeArray;
	}

	/**
	 * Creates a support vector problem for the LibSVM.
	 *
	 * @throws UserError
	 */
	private Problem getProblem(ExampleSet exampleSet) throws UserError {
		log("Creating LibLinear problem.");
		FastExample2SparseTransform ripper = new FastExample2SparseTransform(exampleSet);
		int nodeCount = 0;
		Problem problem = new Problem();
		problem.l = exampleSet.size();

		boolean useBias = getParameterAsBoolean(PARAMETER_USE_BIAS);
		if (useBias) {
			problem.n = exampleSet.getAttributes().size() + 1;
		} else {
			problem.n = exampleSet.getAttributes().size();
		}

		problem.y = new double[exampleSet.size()];
		problem.x = new FeatureNode[exampleSet.size()][];
		Iterator<Example> i = exampleSet.iterator();
		Attribute label = exampleSet.getAttributes().getLabel();
		int j = 0;

		int firstIndex = label.getMapping().getNegativeIndex();

		boolean class0 = false, class1 = false;
		while (i.hasNext()) {
			Example e = i.next();
			problem.x[j] = makeNodes(e, ripper, useBias);
			problem.y[j] = (int) e.getValue(label) == firstIndex ? 0 : 1;
			if (problem.y[j] == 0) {
				class0 = true;
			} else {
				class1 = true;
			}
			nodeCount += problem.x[j].length;
			j++;
		}
		if (!(class0 && class1)) {
			throw new UserError(this, 503, this.getName());
		}
		log("Created " + nodeCount + " nodes for " + j + " examples.");
		return problem;
	}

	/**
	 * Creates a LibSVM parameter object based on the user defined parameters. If gamma is set to
	 * zero, it will be overwritten by 1 divided by the number of attributes.
	 */
	private Parameter getParameters(ExampleSet exampleSet) throws OperatorException {
		SolverType solverType = null;

		int solverTypeParameter = getParameterAsInt(PARAMETER_SOLVER);
		switch (solverTypeParameter) {
			case SOLVER_L2_SVM_DUAL:
				solverType = SolverType.L2R_L2LOSS_SVC_DUAL;
				break;
			case SOLVER_L2_SVM_PRIMAL:
				solverType = SolverType.L2R_L2LOSS_SVC;
				break;
			case SOLVER_L2_LR:
				solverType = SolverType.L2R_LR;
				break;
			case SOLVER_L1_SVM_DUAL:
				solverType = SolverType.L2R_L1LOSS_SVC_DUAL;
				break;
			default:
				solverType = SolverType.L2R_L2LOSS_SVC_DUAL;
				break;
		}
		double c = getParameterAsDouble(PARAMETER_C);
		double epsilon = getParameterAsDouble(PARAMETER_EPSILON);
		Parameter parameter = new Parameter(solverType, c, epsilon);

		// class weights (if set)
		if (isParameterSet(PARAMETER_CLASS_WEIGHTS)) {
			double[] weights = new double[2];
			int[] weightLabelIndices = new int[2];
			for (int i = 0; i < weights.length; i++) {
				weights[i] = 1.0d;
				weightLabelIndices[i] = i;
			}
			List<String[]> classWeights = getParameterList(PARAMETER_CLASS_WEIGHTS);
			Iterator<String[]> i = classWeights.iterator();
			Attribute label = exampleSet.getAttributes().getLabel();
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

			parameter.setWeights(weights, weightLabelIndices);
		}
		return parameter;
	}

	/** Learns a new SVM model with the LibSVM package. */
	@Override
	public Model learn(ExampleSet exampleSet) throws OperatorException {

		Parameter params = getParameters(exampleSet);

		if (exampleSet.size() < 2) {
			throw new UserError(this, 110, 2);
		}

		Linear.resetRandom();
		Linear.disableDebugOutput();
		Problem problem = getProblem(exampleSet);
		de.bwaldvogel.liblinear.Model model = Linear.train(problem, params);

		return new FastMarginModel(exampleSet, model, getParameterAsBoolean(PARAMETER_USE_BIAS));
	}

	@Override
	public Class<? extends PredictionModel> getModelClass() {
		return FastMarginModel.class;
	}

	@Override
	public List<ParameterType> getParameterTypes() {
		List<ParameterType> types = super.getParameterTypes();
		ParameterType type = new ParameterTypeCategory(PARAMETER_SOLVER, "The solver type for this fast margin method.",
				SOLVER, SOLVER_L2_SVM_DUAL);
		type.setExpert(false);
		types.add(type);

		type = new ParameterTypeDouble(PARAMETER_C, "The cost parameter C for c_svc, epsilon_svr, and nu_svr.", 0,
				Double.POSITIVE_INFINITY, 1);
		type.setExpert(false);
		types.add(type);

		types.add(new ParameterTypeDouble(PARAMETER_EPSILON, "Tolerance of termination criterion.",
				Double.NEGATIVE_INFINITY, Double.POSITIVE_INFINITY, 0.01));

		types.add(new ParameterTypeList(
				PARAMETER_CLASS_WEIGHTS,
				"The weights w for all classes, i.e. set the parameters C of each class w * C (empty: using 1 for all classes where the weight was not defined).",
				new ParameterTypeString("class_name", "The class name (possible value of your label attribute)."),
				new ParameterTypeDouble("weight", "The weight for this class.", 0.0d, Double.POSITIVE_INFINITY, 1.0d)));

		types.add(new ParameterTypeBoolean(PARAMETER_USE_BIAS, "Indicates if an intercept value should be calculated.", true));

		return types;
	}
}
