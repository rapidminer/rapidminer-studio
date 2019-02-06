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

import static com.rapidminer.operator.learner.functions.linear.LinearRegression.M5_PRIME;
import static com.rapidminer.operator.learner.functions.linear.LinearRegression.PARAMETER_ELIMINATE_COLINEAR_FEATURES;
import static com.rapidminer.operator.learner.functions.linear.LinearRegression.PARAMETER_FEATURE_SELECTION;
import static com.rapidminer.operator.learner.functions.linear.LinearRegression.PARAMETER_MIN_TOLERANCE;
import static com.rapidminer.operator.learner.functions.linear.LinearRegression.PARAMETER_RIDGE;
import static com.rapidminer.operator.learner.functions.linear.LinearRegression.PARAMETER_USE_BIAS;
import static com.rapidminer.operator.learner.functions.linear.LinearRegression.SELECTION_METHODS;

import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;
import java.util.Map.Entry;

import Jama.Matrix;

import com.rapidminer.example.Attribute;
import com.rapidminer.example.Attributes;
import com.rapidminer.example.Example;
import com.rapidminer.example.ExampleSet;
import com.rapidminer.operator.Model;
import com.rapidminer.operator.OperatorCapability;
import com.rapidminer.operator.OperatorCreationException;
import com.rapidminer.operator.OperatorDescription;
import com.rapidminer.operator.OperatorException;
import com.rapidminer.operator.ProcessSetupError;
import com.rapidminer.operator.SimpleProcessSetupError;
import com.rapidminer.operator.UserError;
import com.rapidminer.operator.learner.AbstractLearner;
import com.rapidminer.operator.learner.functions.linear.LinearRegression;
import com.rapidminer.operator.learner.functions.linear.LinearRegressionMethod;
import com.rapidminer.operator.ports.InputPortExtender;
import com.rapidminer.parameter.ParameterType;
import com.rapidminer.parameter.ParameterTypeBoolean;
import com.rapidminer.parameter.ParameterTypeCategory;
import com.rapidminer.parameter.ParameterTypeDouble;
import com.rapidminer.parameter.conditions.BooleanParameterCondition;
import com.rapidminer.parameter.conditions.EqualTypeCondition;
import com.rapidminer.tools.OperatorService;
import com.rapidminer.tools.container.Pair;
import com.rapidminer.tools.math.matrix.CovarianceMatrix;


/**
 * This operator performs a seemingly unrelated regression from several data sets to make use of
 * common effects in the label that are not explainable from attributes.
 *
 * @author Sebastian Land
 */
public class SeeminglyUnrelatedRegressionOperator extends AbstractLearner {

	private InputPortExtender unrelatedExampleSets = new InputPortExtender("unrelated example sets", getInputPorts());

	public SeeminglyUnrelatedRegressionOperator(OperatorDescription description) {
		super(description);

		unrelatedExampleSets.start();
	}

	@Override
	protected void performAdditionalChecks() {
		if (unrelatedExampleSets.getManagedPorts().size() <= 1) {
			addError(new SimpleProcessSetupError(ProcessSetupError.Severity.ERROR, getPortOwner(), "seemingly_unrelated_regression.too_few_inputs"));
		} else if (unrelatedExampleSets.getManagedPorts().size() <= 2) {
			addError(new SimpleProcessSetupError(ProcessSetupError.Severity.WARNING, getPortOwner(), "seemingly_unrelated_regression.too_few_inputs"));
		}
	}

	@Override
	public Model learn(ExampleSet mainSet) throws OperatorException {
		List<ExampleSet> dataSets = unrelatedExampleSets.getData(ExampleSet.class, true);
		return learn(mainSet, dataSets);
	}

	public SeeminglyUnrelatedRegressionModel learn(ExampleSet mainSet, List<ExampleSet> dataSets) throws
			OperatorException {
		// check if each data set is part of the mainSet and has the same size
		int numberOfExamples = mainSet.size();
		int numberOfSets = dataSets.size();

		if (numberOfSets < 1) {
			throw new UserError(this, "seemingly_unrelated_regression.too_few_inputs");
		}

		for (ExampleSet exampleSet : dataSets) {
			if (exampleSet.size() != numberOfExamples) {
				throw new UserError(this, 951);
			}
		}

		Attributes mainAttributes = mainSet.getAttributes();
		int exampleSetIndex = 1;
		for (ExampleSet testSet : dataSets) {
			Attributes attributes = testSet.getAttributes();
			for (Attribute attribute : attributes) {
				if (mainAttributes.get(attribute.getName()) == null) {
					throw new UserError(this, 952, attribute.getName(), exampleSetIndex + "");
				}
			}
			exampleSetIndex++;
		}

		// first perform linear regression on each dataSet
		ArrayList<Pair<Attribute, Attribute>> labelPredictionAttributes = new ArrayList<>(
				dataSets.size());
		ArrayList<Iterator<Example>> setIterators = new ArrayList<>(dataSets.size());
		ArrayList<String[]> usedAttributeNames = new ArrayList<>(dataSets.size());
		ArrayList<String> labelNames = new ArrayList<>(dataSets.size());
		try {
			// create linear regression and set parameters
			LinearRegression regression = OperatorService.createOperator(LinearRegression.class);
			regression.setParameter(PARAMETER_ELIMINATE_COLINEAR_FEATURES,
					getParameterAsString(PARAMETER_ELIMINATE_COLINEAR_FEATURES));
			regression.setParameter(PARAMETER_FEATURE_SELECTION, getParameterAsString(PARAMETER_FEATURE_SELECTION));
			regression.setParameter(PARAMETER_MIN_TOLERANCE, getParameterAsString(PARAMETER_MIN_TOLERANCE));
			regression.setParameter(PARAMETER_RIDGE, getParameterAsString(PARAMETER_RIDGE));
			regression.setParameter(PARAMETER_USE_BIAS, "true");

			for (ExampleSet exampleSet : dataSets) {
				LinearRegressionModel model = (LinearRegressionModel) regression.learn(exampleSet);

				// now apply for getting residuals
				ExampleSet resultSet = model.apply(exampleSet);
				Attribute label = resultSet.getAttributes().getLabel();
				labelPredictionAttributes.add(new Pair<>(label, resultSet.getAttributes()
						.getPredictedLabel()));
				labelNames.add(label.getName());
				usedAttributeNames.add(model.getSelectedAttributeNames());
				setIterators.add(resultSet.iterator());
			}
		} catch (OperatorCreationException e) {
			throw new UserError(this, 950, LinearRegression.class.getSimpleName(), e.getMessage());
		}

		// now build covariance matrix of residuals
		checkForStop();
		double[][] residualMatrix = new double[numberOfExamples][numberOfSets];
		for (int i = 0; i < numberOfExamples; i++) {
			// search residuals from examples of each set
			for (int j = 0; j < numberOfSets; j++) {
				Example example = setIterators.get(j).next();
				Pair<Attribute, Attribute> labelPredictionPair = labelPredictionAttributes.get(j);
				residualMatrix[i][j] = example.getValue(labelPredictionPair.getFirst())
						- example.getValue(labelPredictionPair.getSecond());
			}
		}
		Matrix covarianceMatrix = CovarianceMatrix.getCovarianceMatrix(residualMatrix, this);
		Matrix wMatrixInverse = new Matrix(numberOfSets * numberOfExamples, numberOfSets * numberOfExamples, 0d);
		for (int i = 0; i < numberOfSets; i++) {
			for (int j = 0; j < numberOfSets; j++) {
				Matrix partlyResult = Matrix.identity(numberOfExamples, numberOfExamples).times(covarianceMatrix.get(i, j));
				wMatrixInverse.setMatrix(i * numberOfExamples, (i + 1) * numberOfExamples - 1, j * numberOfExamples, (j + 1)
						* numberOfExamples - 1, partlyResult);
			}
		}

		checkForStop();
		wMatrixInverse = wMatrixInverse.times(1d / numberOfExamples);
		if (!wMatrixInverse.lu().isNonsingular()) {
			throw new UserError(this, "seemingly_unrelated_regression.singular_matrix_found");
		}
		wMatrixInverse = wMatrixInverse.inverse();

		// now build data matrices and merge to single matrix
		checkForStop();
		int width = 0;
		for (String[] selectedAttributes : usedAttributeNames) {
			width += selectedAttributes.length + 1; // bias
		}
		int height = numberOfSets * numberOfExamples;
		Matrix xMatrix = new Matrix(height, width, 0d);

		// fill matrix
		int i = 0;
		int currentColumn = 0;
		for (ExampleSet unrelatedSet : dataSets) {
			checkForStop();
			Attributes attributes = unrelatedSet.getAttributes();
			Attribute[] selectedAttributes = new Attribute[usedAttributeNames.get(i).length];
			int j = 0;
			for (String selectedAttributeName : usedAttributeNames.get(i)) {
				selectedAttributes[j] = attributes.get(selectedAttributeName);
				j++;
			}

			double[][] subMatrixData = new double[numberOfExamples][selectedAttributes.length + 1];
			int y = 0;
			for (Example example : unrelatedSet) {
				subMatrixData[y][0] = 1d; // bias
				int x = 1;
				for (Attribute attribute : selectedAttributes) {
					subMatrixData[y][x] = example.getValue(attribute);
					x++;
				}
				y++;
			}

			// now set submatrix
			xMatrix.setMatrix(i * numberOfExamples, (i + 1) * numberOfExamples - 1, currentColumn, currentColumn
					+ selectedAttributes.length, new Matrix(subMatrixData));

			currentColumn += selectedAttributes.length + 1;
			i++;
		}

		// build y matrix
		double[][] yMatrixData = new double[height][1];
		int currentRow = 0;
		for (ExampleSet unrelatedSet : dataSets) {
			checkForStop();
			Attribute label = unrelatedSet.getAttributes().getLabel();
			for (Example example : unrelatedSet) {
				yMatrixData[currentRow][0] = example.getValue(label);
				currentRow++;
			}
		}
		Matrix yMatrix = new Matrix(yMatrixData);

		// now make matrix calculations
		checkForStop();
		Matrix result = xMatrix.transpose().times(wMatrixInverse);
		checkForStop();
		result = result.times(xMatrix).inverse();
		checkForStop();
		result = result.times(xMatrix.transpose());
		checkForStop();
		result = result.times(wMatrixInverse);
		checkForStop();
		result = result.times(yMatrix);
		checkForStop();
		return new SeeminglyUnrelatedRegressionModel(mainSet, usedAttributeNames, labelNames, result.getRowPackedCopy());
	}

	@Override
	public boolean supportsCapability(OperatorCapability lc) {
		return lc.equals(OperatorCapability.NUMERICAL_ATTRIBUTES) || lc.equals(OperatorCapability.NUMERICAL_LABEL);
	}


	/**
	 * This is a copy of the linear regression parameters, because we can't just copy them because of the dependencies
	 */
	@Override
	public List<ParameterType> getParameterTypes() {
		List<ParameterType> types = super.getParameterTypes();

		String[] availableSelectionMethods = SELECTION_METHODS.keySet().toArray(new String[0]);
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
			} catch (InstantiationException | IllegalAccessException e) { // can't do anything about this
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
}
