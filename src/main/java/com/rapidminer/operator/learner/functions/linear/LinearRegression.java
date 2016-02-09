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
package com.rapidminer.operator.learner.functions.linear;

import java.util.Iterator;
import java.util.LinkedHashMap;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;

import org.apache.commons.math3.distribution.FDistribution;
import org.apache.commons.math3.linear.LUDecomposition;
import org.apache.commons.math3.linear.MatrixUtils;
import org.apache.commons.math3.linear.RealMatrix;

import Jama.Matrix;

import com.rapidminer.example.Attribute;
import com.rapidminer.example.AttributeWeights;
import com.rapidminer.example.Attributes;
import com.rapidminer.example.Example;
import com.rapidminer.example.ExampleSet;
import com.rapidminer.example.Statistics;
import com.rapidminer.example.table.AttributeFactory;
import com.rapidminer.operator.Model;
import com.rapidminer.operator.OperatorCapability;
import com.rapidminer.operator.OperatorDescription;
import com.rapidminer.operator.OperatorException;
import com.rapidminer.operator.ProcessStoppedException;
import com.rapidminer.operator.UserError;
import com.rapidminer.operator.annotation.ResourceConsumptionEstimator;
import com.rapidminer.operator.learner.AbstractLearner;
import com.rapidminer.operator.learner.PredictionModel;
import com.rapidminer.operator.learner.functions.LinearRegressionModel;
import com.rapidminer.operator.learner.functions.linear.LinearRegressionMethod.LinearRegressionResult;
import com.rapidminer.operator.ports.OutputPort;
import com.rapidminer.parameter.ParameterType;
import com.rapidminer.parameter.ParameterTypeBoolean;
import com.rapidminer.parameter.ParameterTypeCategory;
import com.rapidminer.parameter.ParameterTypeDouble;
import com.rapidminer.parameter.UndefinedParameterError;
import com.rapidminer.parameter.conditions.BooleanParameterCondition;
import com.rapidminer.parameter.conditions.EqualTypeCondition;
import com.rapidminer.tools.Ontology;
import com.rapidminer.tools.OperatorResourceConsumptionHandler;
import com.rapidminer.tools.Tools;
import com.rapidminer.tools.math.MathFunctions;


/**
 * <p>
 * This operator calculates a linear regression model. It supports several different mechanisms for
 * model selection: - M5Prime using Akaike criterion for model selection. - A greedy implementation
 * - A T-Test based selection - No selection. Further selections can be added using the static
 * method
 *
 * </p>
 *
 * @author Ingo Mierswa
 */
public class LinearRegression extends AbstractLearner {

	/**
	 * The parameter name for &quot;The feature selection method used during regression.&quot;
	 */
	public static final String PARAMETER_FEATURE_SELECTION = "feature_selection";

	/**
	 * The parameter name for &quot;Indicates if the algorithm should try to delete colinear
	 * features during the regression.&quot;
	 */
	public static final String PARAMETER_ELIMINATE_COLINEAR_FEATURES = "eliminate_colinear_features";

	public static final String PARAMETER_USE_BIAS = "use_bias";

	/**
	 * The parameter name for &quot;The minimum tolerance for the removal of colinear
	 * features.&quot;
	 */
	public static final String PARAMETER_MIN_TOLERANCE = "min_tolerance";

	/**
	 * The parameter name for &quot;The ridge parameter used during ridge regression.&quot;
	 */
	public static final String PARAMETER_RIDGE = "ridge";

	public static final Map<String, Class<? extends LinearRegressionMethod>> SELECTION_METHODS = new LinkedHashMap<>();

	static {
		SELECTION_METHODS.put("none", PlainLinearRegressionMethod.class);
		SELECTION_METHODS.put("M5 prime", M5PLinearRegressionMethod.class);
		SELECTION_METHODS.put("greedy", GreedyLinearRegressionMethod.class);
		SELECTION_METHODS.put("T-Test", TTestLinearRegressionMethod.class);
		SELECTION_METHODS.put("Iterative T-Test", IterativeTTestLinearRegressionMethod.class);
	}

	/** Attribute selection method: No attribute selection */
	public static final int NO_SELECTION = 0;

	/** Attribute selection method: M5 method */
	public static final int M5_PRIME = 1;

	/** Attribute selection method: Greedy method */
	public static final int GREEDY = 2;

	private OutputPort weightOutput = getOutputPorts().createPort("weights");

	public LinearRegression(OperatorDescription description) {
		super(description);

		getTransformer().addGenerationRule(weightOutput, AttributeWeights.class);
	}

	@Override
	public Model learn(ExampleSet exampleSet) throws OperatorException {
		// initializing data and parameter values.
		Attribute label = exampleSet.getAttributes().getLabel();
		Attribute workingLabel = label;
		boolean cleanUpLabel = false;
		String firstClassName = null;
		String secondClassName = null;
		getProgress().setTotal(9);

		com.rapidminer.example.Tools.onlyNonMissingValues(exampleSet, getOperatorClassName(), this, Attributes.LABEL_NAME);

		boolean useBias = getParameterAsBoolean(PARAMETER_USE_BIAS);
		boolean removeColinearAttributes = getParameterAsBoolean(PARAMETER_ELIMINATE_COLINEAR_FEATURES);
		double ridge = getParameterAsDouble(PARAMETER_RIDGE);
		double minTolerance = getParameterAsDouble(PARAMETER_MIN_TOLERANCE);

		// prepare for classification by translating into 0-1 coding.
		if (label.isNominal()) {
			if (label.getMapping().size() == 2) {
				firstClassName = label.getMapping().getNegativeString();
				secondClassName = label.getMapping().getPositiveString();

				int firstIndex = label.getMapping().getNegativeIndex();

				workingLabel = AttributeFactory.createAttribute("regression_label", Ontology.REAL);
				exampleSet.getExampleTable().addAttribute(workingLabel);

				for (Example example : exampleSet) {
					double index = example.getValue(label);
					if (index == firstIndex) {
						example.setValue(workingLabel, 0.0d);
					} else {
						example.setValue(workingLabel, 1.0d);
					}
				}

				exampleSet.getAttributes().setLabel(workingLabel);
				cleanUpLabel = true;
			}
		}

		getProgress().step();

		// search all attributes and keep numerical
		int numberOfAttributes = exampleSet.getAttributes().size();
		boolean[] isUsedAttribute = new boolean[numberOfAttributes];
		int counter = 0;
		String[] attributeNames = new String[numberOfAttributes];
		for (Attribute attribute : exampleSet.getAttributes()) {
			isUsedAttribute[counter] = attribute.isNumerical();
			attributeNames[counter] = attribute.getName();
			counter++;
		}

		getProgress().step();

		// compute and store statistics and turn off attributes with zero
		// standard deviation
		exampleSet.recalculateAllAttributeStatistics();
		double[] means = new double[numberOfAttributes];
		double[] standardDeviations = new double[numberOfAttributes];
		counter = 0;
		Attribute[] allAttributes = new Attribute[exampleSet.getAttributes().size()];
		for (Attribute attribute : exampleSet.getAttributes()) {
			allAttributes[counter] = attribute;
			if (isUsedAttribute[counter]) {
				means[counter] = exampleSet.getStatistics(attribute, Statistics.AVERAGE_WEIGHTED);
				standardDeviations[counter] = Math.sqrt(exampleSet.getStatistics(attribute, Statistics.VARIANCE_WEIGHTED));
				if (standardDeviations[counter] == 0) {
					isUsedAttribute[counter] = false;
				}
			}
			counter++;
		}

		double labelMean = exampleSet.getStatistics(workingLabel, Statistics.AVERAGE_WEIGHTED);
		double labelStandardDeviation = Math.sqrt(exampleSet.getStatistics(workingLabel, Statistics.VARIANCE_WEIGHTED));

		int numberOfExamples = exampleSet.size();

		getProgress().step();

		// determine the number of used attributes + 1
		int numberOfUsedAttributes = 1;
		for (int i = 0; i < isUsedAttribute.length; i++) {
			if (isUsedAttribute[i]) {
				numberOfUsedAttributes++;
			}
		}

		getProgress().step();

		// remove colinear attributes
		double[] coefficientsOnFullData = performRegression(exampleSet, isUsedAttribute, means, labelMean, ridge);
		if (removeColinearAttributes) {
			boolean eliminateMore = true;
			while (eliminateMore) {
				int maxIndex = -1;
				double maxTolerance = 1;
				boolean found = false;
				for (int i = 0; i < isUsedAttribute.length; i++) {
					if (isUsedAttribute[i]) {
						double tolerance = getTolerance(exampleSet, isUsedAttribute, i, ridge, useBias);
						if (tolerance < minTolerance) {
							if (tolerance <= maxTolerance) {
								maxTolerance = tolerance;
								maxIndex = i;
								found = true;
							}
						}
					}
				}
				if (found) {
					isUsedAttribute[maxIndex] = false;
				} else {
					eliminateMore = false;
				}
				coefficientsOnFullData = performRegression(exampleSet, isUsedAttribute, means, labelMean, ridge);
			}
		} else {
			coefficientsOnFullData = performRegression(exampleSet, isUsedAttribute, means, labelMean, ridge);
		}

		getProgress().step();

		// calculate error on full data
		double errorOnFullData = getSquaredError(exampleSet, isUsedAttribute, coefficientsOnFullData, useBias);

		getProgress().step();

		// apply attribute selection method

		int selectionMethodIndex = getParameterAsInt(PARAMETER_FEATURE_SELECTION);
		String[] selectionMethodNames = SELECTION_METHODS.keySet().toArray(new String[SELECTION_METHODS.size()]);
		String selectedMethod = selectionMethodNames[selectionMethodIndex]; // getParameterAsString(PARAMETER_FEATURE_SELECTION);
		Class<? extends LinearRegressionMethod> methodClass = SELECTION_METHODS.get(selectedMethod);
		if (methodClass == null) {
			throw new UserError(this, 904, PARAMETER_FEATURE_SELECTION, "unknown method");
		}
		LinearRegressionMethod method;
		try {
			method = methodClass.newInstance();
		} catch (InstantiationException e) {
			throw new UserError(this, 904, PARAMETER_FEATURE_SELECTION, e.getMessage());
		} catch (IllegalAccessException e) {
			throw new UserError(this, 904, PARAMETER_FEATURE_SELECTION, e.getMessage());
		}

		// apply feature selection technique
		LinearRegressionResult result = method.applyMethod(this, useBias, ridge, exampleSet, isUsedAttribute,
				numberOfExamples, numberOfUsedAttributes, means, labelMean, standardDeviations, labelStandardDeviation,
				coefficientsOnFullData, errorOnFullData);

		// clean up eventually if was classification const
		if (cleanUpLabel) {
			exampleSet.getAttributes().remove(workingLabel);
			exampleSet.getExampleTable().removeAttribute(workingLabel);
			exampleSet.getAttributes().setLabel(label);
		}

		getProgress().step();

		// +++++++++++++++++++++++++++++++++++++++++++++
		// calculating statistics of the resulting model
		// +++++++++++++++++++++++++++++++++++++++++++++
		int length = result.coefficients.length;
		FDistribution fdistribution;
		if (exampleSet.size() - length <= 0) {
			// In this case the F-distribution is not defined (the second parameter of the
			// F-distribution has to be >0).
			fdistribution = null;
		} else {
			fdistribution = new FDistribution(1, exampleSet.size() - length);
		}
		double[] standardErrors = new double[length];
		double[] standardizedCoefficients = new double[length];
		double[] tolerances = new double[length];
		double[] tStatistics = new double[length];
		double[] pValues = new double[length];

		int finalNumberOfAttributes = 0;
		for (boolean b : result.isUsedAttribute) {
			if (b) {
				finalNumberOfAttributes++;
			}
		}

		getProgress().step();

		// calculating standard error matrix, (containing the error of
		// intercept and estimated coefficients)
		int degreeOfFreedom = finalNumberOfAttributes + 1;

		double mse = result.error / (exampleSet.size() - degreeOfFreedom);

		// add a additional column of 1s to the design matrix for the intercept
		double[][] data = new double[exampleSet.size()][finalNumberOfAttributes + 1];

		for (int i = 0; i < exampleSet.size(); i++) {
			data[i][0] = 1;
		}

		int eIndex = 0;
		for (Example e : exampleSet) {
			int aIndex = 0;
			int aCounter = 1;
			for (Attribute a : exampleSet.getAttributes()) {
				if (result.isUsedAttribute[aIndex]) {
					data[eIndex][aCounter] = e.getValue(a);
					aCounter++;
				}
				aIndex++;
			}
			eIndex++;
		}

		getProgress().step();

		RealMatrix matrix = MatrixUtils.createRealMatrix(data);
		RealMatrix matrixT = matrix.transpose();
		RealMatrix productMatrix = matrixT.multiply(matrix);
		RealMatrix invertedMatrix = null;
		try {
			// try to invert matrix
			invertedMatrix = new LUDecomposition(productMatrix).getSolver().getInverse();

			int index = 0;
			for (int i = 0; i < result.isUsedAttribute.length; i++) {
				if (result.isUsedAttribute[i]) {

					tolerances[index] = getTolerance(exampleSet, result.isUsedAttribute, i, ridge, useBias);
					standardErrors[index] = Math.sqrt(mse * invertedMatrix.getEntry(index + 1, index + 1));
					// calculate standardized Coefficients

					// Be careful, use in the calculation of standardizedCoefficients the i instead
					// of index for standardDeviations, because all other arrays refer to the
					// selected attributes, whereas standardDeviations refers to all attributes
					standardizedCoefficients[index] = result.coefficients[index]
							* (standardDeviations[i] / labelStandardDeviation);

					if (!Tools.isZero(standardErrors[index]) && fdistribution != null) {
						tStatistics[index] = result.coefficients[index] / standardErrors[index];
						double probability = fdistribution.cumulativeProbability(tStatistics[index] * tStatistics[index]);
						pValues[index] = 1.0d - probability;
					} else {
						if (Tools.isZero(result.coefficients[index])) {
							tStatistics[index] = 0.0d;
							pValues[index] = 1.0d;
						} else {
							tStatistics[index] = Double.POSITIVE_INFINITY;
							pValues[index] = 0.0d;
						}
					}
					index++;
				}
			}
		} catch (Throwable e) {

			// calculate approximate value if matrix can not be inverted
			double generalCorrelation = getCorrelation(exampleSet, isUsedAttribute, coefficientsOnFullData, useBias);
			generalCorrelation = Math.min(generalCorrelation * generalCorrelation, 1.0d);

			int index = 0;
			for (int i = 0; i < result.isUsedAttribute.length; i++) {
				if (result.isUsedAttribute[i]) {
					// calculating standard error and tolerance
					double tolerance = getTolerance(exampleSet, result.isUsedAttribute, i, ridge, useBias);
					standardErrors[index] = Math.sqrt((1.0d - generalCorrelation)
							/ (tolerance * (exampleSet.size() - exampleSet.getAttributes().size() - 1.0d)))
							* labelStandardDeviation / standardDeviations[i];
					tolerances[index] = tolerance;

					// calculating beta and test statistics
					// calculate standardized coefficients

					// Be careful, use in the calculation of standardizedCoefficients the i instead
					// of index for standardDeviations, because all other arrays refer to the
					// selected attributes, whereas standardDeviations refers to all attributes
					standardizedCoefficients[index] = result.coefficients[index]
							* (standardDeviations[i] / labelStandardDeviation);

					if (!Tools.isZero(standardErrors[index]) && fdistribution != null) {
						tStatistics[index] = result.coefficients[index] / standardErrors[index];
						double probability = fdistribution.cumulativeProbability(tStatistics[index] * tStatistics[index]);
						pValues[index] = 1.0d - probability;
					} else {
						if (Tools.isZero(result.coefficients[index])) {
							tStatistics[index] = 0.0d;
							pValues[index] = 1.0d;
						} else {
							tStatistics[index] = Double.POSITIVE_INFINITY;
							pValues[index] = 0.0d;
						}
					}
					index++;
				}
			}
		}

		// Set all values for intercept
		if (invertedMatrix == null) {
			standardErrors[standardErrors.length - 1] = Double.POSITIVE_INFINITY;
		} else {
			standardErrors[standardErrors.length - 1] = Math.sqrt(mse * invertedMatrix.getEntry(0, 0));
		}
		tolerances[tolerances.length - 1] = Double.NaN;
		standardizedCoefficients[standardizedCoefficients.length - 1] = Double.NaN;
		if (!Tools.isZero(standardErrors[standardErrors.length - 1]) && fdistribution != null) {
			tStatistics[tStatistics.length - 1] = result.coefficients[result.coefficients.length - 1]
					/ standardErrors[standardErrors.length - 1];
			double probability = fdistribution.cumulativeProbability(tStatistics[tStatistics.length - 1]
					* tStatistics[tStatistics.length - 1]);
			pValues[pValues.length - 1] = 1.0d - probability;
		} else {
			if (Tools.isZero(result.coefficients[result.coefficients.length - 1])) {
				tStatistics[tStatistics.length - 1] = 0.0d;
				pValues[pValues.length - 1] = 1.0d;
			} else {
				tStatistics[tStatistics.length - 1] = Double.POSITIVE_INFINITY;
				pValues[pValues.length - 1] = 0.0d;
			}
		}

		// delivering weights
		if (weightOutput.isConnected()) {
			AttributeWeights weights = new AttributeWeights(exampleSet);
			int selectedAttributes = 0;
			for (int i = 0; i < attributeNames.length; i++) {
				if (isUsedAttribute[i]) {
					weights.setWeight(attributeNames[i], result.coefficients[selectedAttributes]);
					selectedAttributes++;
				} else {
					weights.setWeight(attributeNames[i], 0);
				}
			}
			weightOutput.deliver(weights);
		}

		getProgress().complete();

		return new LinearRegressionModel(exampleSet, result.isUsedAttribute, result.coefficients, standardErrors,
				standardizedCoefficients, tolerances, tStatistics, pValues, useBias, firstClassName, secondClassName);
	}

	double getTolerance(ExampleSet exampleSet, boolean[] isUsedAttribute, int testAttributeIndex, double ridge,
			boolean useIntercept) throws UndefinedParameterError, ProcessStoppedException {
		List<Attribute> attributeList = new LinkedList<>();
		Attribute currentAttribute = null;
		int resultAIndex = 0;
		for (Attribute a : exampleSet.getAttributes()) {
			if (isUsedAttribute[resultAIndex]) {
				if (resultAIndex != testAttributeIndex) {
					attributeList.add(a);
				} else {
					currentAttribute = a;
				}
			}
			resultAIndex++;
		}

		Attribute[] usedAttributes = new Attribute[attributeList.size()];
		attributeList.toArray(usedAttributes);

		double[] localCoefficients = performRegression(exampleSet, usedAttributes, currentAttribute, ridge);
		double[] attributeValues = new double[exampleSet.size()];
		double[] predictedValues = new double[exampleSet.size()];
		int eIndex = 0;
		for (Example e : exampleSet) {
			attributeValues[eIndex] = e.getValue(currentAttribute);
			int aIndex = 0;
			double prediction = 0.0d;
			for (Attribute a : usedAttributes) {
				prediction += localCoefficients[aIndex] * e.getValue(a);
				aIndex++;
			}
			if (useIntercept) {
				prediction += localCoefficients[localCoefficients.length - 1];
			}
			predictedValues[eIndex] = prediction;
			eIndex++;
		}

		double correlation = MathFunctions.correlation(attributeValues, predictedValues);
		double tolerance = 1.0d - correlation * correlation;
		return tolerance;
	}

	/**
	 * Calculates the squared error of a regression model on the training data.
	 *
	 * @throws ProcessStoppedException
	 */
	double getSquaredError(ExampleSet exampleSet, boolean[] selectedAttributes, double[] coefficients, boolean useIntercept)
			throws ProcessStoppedException {
		double error = 0;
		Iterator<Example> i = exampleSet.iterator();
		while (i.hasNext()) {
			checkForStop();
			Example example = i.next();
			double prediction = regressionPrediction(example, selectedAttributes, coefficients, useIntercept);
			double diff = prediction - example.getLabel();
			error += diff * diff;
		}
		return error;
	}

	double getCorrelation(ExampleSet exampleSet, boolean[] selectedAttributes, double[] coefficients, boolean useIntercept) {
		double[] labelValues = new double[exampleSet.size()];
		double[] predictions = new double[exampleSet.size()];
		int index = 0;
		for (Example e : exampleSet) {
			labelValues[index] = e.getLabel();
			predictions[index] = regressionPrediction(e, selectedAttributes, coefficients, useIntercept);
			index++;
		}
		return MathFunctions.correlation(labelValues, predictions);
	}

	/** Calculates the prediction for the given example. */
	private double regressionPrediction(Example example, boolean[] selectedAttributes, double[] coefficients,
			boolean useIntercept) {
		double prediction = 0;
		int index = 0;
		int counter = 0;
		for (Attribute attribute : example.getAttributes()) {
			if (selectedAttributes[counter++]) {
				prediction += coefficients[index] * example.getValue(attribute);
				index++;
			}
		}

		if (useIntercept) {
			prediction += coefficients[index];
		}

		return prediction;
	}

	/**
	 * Calculate a linear regression only from the selected attributes. The method returns the
	 * calculated coefficients.
	 *
	 * @throws ProcessStoppedException
	 */
	double[] performRegression(ExampleSet exampleSet, boolean[] selectedAttributes, double[] means, double labelMean,
			double ridge) throws UndefinedParameterError, ProcessStoppedException {
		int currentlySelectedAttributes = 0;
		for (int i = 0; i < selectedAttributes.length; i++) {
			if (selectedAttributes[i]) {
				currentlySelectedAttributes++;
			}
		}

		Matrix independent = null;
		Matrix dependent = null;
		double[] weights = null;
		if (currentlySelectedAttributes > 0) {
			independent = new Matrix(exampleSet.size(), currentlySelectedAttributes);
			dependent = new Matrix(exampleSet.size(), 1);
			int exampleIndex = 0;
			Iterator<Example> i = exampleSet.iterator();
			weights = new double[exampleSet.size()];
			Attribute weightAttribute = exampleSet.getAttributes().getWeight();
			while (i.hasNext()) {
				Example example = i.next();
				int attributeIndex = 0;
				dependent.set(exampleIndex, 0, example.getLabel());
				int counter = 0;
				for (Attribute attribute : exampleSet.getAttributes()) {
					checkForStop();
					if (selectedAttributes[counter]) {
						double value = example.getValue(attribute) - means[counter];
						independent.set(exampleIndex, attributeIndex, value);
						attributeIndex++;
					}
					counter++;
				}
				if (weightAttribute != null) {
					weights[exampleIndex] = example.getValue(weightAttribute);
				} else {
					weights[exampleIndex] = 1.0d;
				}
				exampleIndex++;
			}
		}

		double[] coefficients = new double[currentlySelectedAttributes + 1];
		if (currentlySelectedAttributes > 0) {
			double[] coefficientsWithoutIntercept = com.rapidminer.tools.math.LinearRegression.performRegression(
					independent, dependent, weights, ridge);
			System.arraycopy(coefficientsWithoutIntercept, 0, coefficients, 0, currentlySelectedAttributes);
		}
		coefficients[currentlySelectedAttributes] = labelMean;

		int coefficientIndex = 0;
		for (int i = 0; i < selectedAttributes.length; i++) {
			if (selectedAttributes[i]) {
				coefficients[coefficients.length - 1] -= coefficients[coefficientIndex] * means[i];
				coefficientIndex++;
			}
		}

		return coefficients;
	}

	/**
	 * Calculate a linear regression only from the selected attributes. The method returns the
	 * calculated coefficients.
	 *
	 * @throws ProcessStoppedException
	 */
	double[] performRegression(ExampleSet exampleSet, Attribute[] usedAttributes, Attribute label, double ridge)
			throws UndefinedParameterError, ProcessStoppedException {
		Matrix independent = null;
		Matrix dependent = null;
		double[] weights = null;
		if (usedAttributes.length > 0) {
			independent = new Matrix(exampleSet.size(), usedAttributes.length);
			dependent = new Matrix(exampleSet.size(), 1);
			int exampleIndex = 0;
			Iterator<Example> i = exampleSet.iterator();
			weights = new double[exampleSet.size()];
			Attribute weightAttribute = exampleSet.getAttributes().getWeight();
			while (i.hasNext()) {
				Example example = i.next();
				int attributeIndex = 0;
				dependent.set(exampleIndex, 0, example.getLabel());
				for (Attribute attribute : usedAttributes) {
					checkForStop();
					double value = example.getValue(attribute) - exampleSet.getStatistics(attribute, Statistics.AVERAGE);
					independent.set(exampleIndex, attributeIndex, value);
					attributeIndex++;
				}
				if (weightAttribute != null) {
					weights[exampleIndex] = example.getValue(weightAttribute);
				} else {
					weights[exampleIndex] = 1.0d;
				}
				exampleIndex++;
			}
		}

		double[] coefficients = new double[usedAttributes.length + 1];
		if (usedAttributes.length > 0) {
			double[] coefficientsWithoutIntercept = com.rapidminer.tools.math.LinearRegression.performRegression(
					independent, dependent, weights, ridge);
			System.arraycopy(coefficientsWithoutIntercept, 0, coefficients, 0, usedAttributes.length);
		}
		coefficients[usedAttributes.length] = exampleSet.getStatistics(label, Statistics.AVERAGE);

		for (int i = 0; i < usedAttributes.length; i++) {
			coefficients[coefficients.length - 1] -= coefficients[i]
					* exampleSet.getStatistics(usedAttributes[i], Statistics.AVERAGE);
		}

		return coefficients;
	}

	@Override
	public Class<? extends PredictionModel> getModelClass() {
		return LinearRegressionModel.class;
	}

	@Override
	public boolean supportsCapability(OperatorCapability lc) {
		if (lc.equals(OperatorCapability.NUMERICAL_ATTRIBUTES)) {
			return true;
		}
		if (lc.equals(OperatorCapability.NUMERICAL_LABEL)) {
			return true;
		}
		if (lc.equals(OperatorCapability.BINOMINAL_LABEL)) {
			return true;
		}
		if (lc == OperatorCapability.WEIGHTED_EXAMPLES) {
			return true;
		}
		return false;
	}

	@Override
	public List<ParameterType> getParameterTypes() {
		List<ParameterType> types = super.getParameterTypes();
		String[] availableSelectionMethods = SELECTION_METHODS.keySet().toArray(new String[SELECTION_METHODS.size()]);
		types.add(new ParameterTypeCategory(PARAMETER_FEATURE_SELECTION,
				"The feature selection method used during regression.", availableSelectionMethods, M5_PRIME));

		// adding parameter of methods
		int i = 0;
		for (Entry<String, Class<? extends LinearRegressionMethod>> entry : SELECTION_METHODS.entrySet()) {
			try {
				LinearRegressionMethod method = entry.getValue().newInstance();
				for (ParameterType methodType : method.getParameterTypes()) {
					types.add(methodType);
					methodType.registerDependencyCondition(new EqualTypeCondition(this, PARAMETER_FEATURE_SELECTION,
							availableSelectionMethods, true, i));
				}
			} catch (InstantiationException e) { // can't do anything about this
			} catch (IllegalAccessException e) {
			}
			i++;
		}

		types.add(new ParameterTypeBoolean(PARAMETER_ELIMINATE_COLINEAR_FEATURES,
				"Indicates if the algorithm should try to delete colinear features during the regression.", true));
		ParameterType type = new ParameterTypeDouble(PARAMETER_MIN_TOLERANCE,
				"The minimum tolerance for the removal of colinear features.", 0.0d, 1.0d, 0.05d);
		type.registerDependencyCondition(new BooleanParameterCondition(this, PARAMETER_ELIMINATE_COLINEAR_FEATURES, true,
				true));
		types.add(type);

		types.add(new ParameterTypeBoolean(PARAMETER_USE_BIAS, "Indicates if an intercept value should be calculated.", true));
		types.add(new ParameterTypeDouble(
				PARAMETER_RIDGE,
				"The ridge parameter used for ridge regression. A value of zero switches to ordinary least squares estimate.",
				0.0d, Double.POSITIVE_INFINITY, 1.0E-8));
		return types;
	}

	@Override
	public ResourceConsumptionEstimator getResourceConsumptionEstimator() {
		return OperatorResourceConsumptionHandler.getResourceConsumptionEstimator(getExampleSetInputPort(),
				LinearRegression.class, null);
	}
}
