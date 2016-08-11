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
package com.rapidminer.operator.learner.bayes;

import Jama.Matrix;

import com.rapidminer.example.Attribute;
import com.rapidminer.example.ExampleSet;
import com.rapidminer.example.Statistics;
import com.rapidminer.example.set.SplittedExampleSet;
import com.rapidminer.example.table.NominalMapping;
import com.rapidminer.operator.Model;
import com.rapidminer.operator.OperatorCapability;
import com.rapidminer.operator.OperatorDescription;
import com.rapidminer.operator.OperatorException;
import com.rapidminer.operator.UserError;
import com.rapidminer.operator.learner.AbstractLearner;
import com.rapidminer.operator.learner.PredictionModel;
import com.rapidminer.parameter.UndefinedParameterError;
import com.rapidminer.tools.math.MathFunctions;
import com.rapidminer.tools.math.matrix.CovarianceMatrix;


/**
 * <p>
 * This operator performs a linear discriminant analysis (LDA). This method tries to find the linear
 * combination of features which best separate two or more classes of examples. The resulting
 * combination is then used as a linear classifier. LDA is closely related to ANOVA (analysis of
 * variance) and regression analysis, which also attempt to express one dependent variable as a
 * linear combination of other features or measurements. In the other two methods however, the
 * dependent variable is a numerical quantity, while for LDA it is a categorical variable (i.e. the
 * class label).
 * </p>
 *
 * <p>
 * LDA is also closely related to principal component analysis (PCA) and factor analysis in that
 * both look for linear combinations of variables which best explain the data. LDA explicitly
 * attempts to model the difference between the classes of data. PCA on the other hand does not take
 * into account any difference in class.
 * </p>
 *
 * @author Sebastian Land
 */
public class LinearDiscriminantAnalysis extends AbstractLearner {

	public LinearDiscriminantAnalysis(OperatorDescription description) {
		super(description);
	}

	@Override
	public Model learn(ExampleSet exampleSet) throws OperatorException {
		int numberOfNumericalAttributes = 0;
		for (Attribute attribute : exampleSet.getAttributes()) {
			this.checkForStop();
			if (attribute.isNumerical()) {
				numberOfNumericalAttributes++;
			}
		}

		NominalMapping labelMapping = exampleSet.getAttributes().getLabel().getMapping();
		String[] labelValues = new String[labelMapping.size()];
		for (int i = 0; i < labelMapping.size(); i++) {
			this.checkForStop();
			labelValues[i] = labelMapping.mapIndex(i);
		}

		Matrix[] meanVectors = getMeanVectors(exampleSet, numberOfNumericalAttributes, labelValues);
		Matrix[] inverseCovariance = getInverseCovarianceMatrices(exampleSet, labelValues);

		return getModel(exampleSet, labelValues, meanVectors, inverseCovariance,
				getAprioriProbabilities(exampleSet, labelValues));
	}

	protected DiscriminantModel getModel(ExampleSet exampleSet, String[] labels, Matrix[] meanVectors,
			Matrix[] inverseCovariances, double[] aprioriProbabilities) throws UndefinedParameterError {
		return new DiscriminantModel(exampleSet, labels, meanVectors, inverseCovariances, aprioriProbabilities, 0d);
	}

	private double[] getAprioriProbabilities(ExampleSet exampleSet, String[] labels) {
		double[] aprioriProbabilites = new double[labels.length];
		double totalSize = exampleSet.size();
		Attribute labelAttribute = exampleSet.getAttributes().getLabel();
		SplittedExampleSet labelSet = SplittedExampleSet.splitByAttribute(exampleSet, exampleSet.getAttributes().getLabel());
		int labelIndex = 0;
		for (String label : labels) {
			// select apropriate subset
			for (int i = 0; i < labels.length; i++) {
				labelSet.selectSingleSubset(i);
				if (labelSet.getExample(0).getNominalValue(labelAttribute).equals(label)) {
					break;
				}
			}
			// calculate apriori Prob
			aprioriProbabilites[labelIndex] = labelSet.size() / totalSize;
			labelIndex++;
		}
		return aprioriProbabilites;
	}

	protected Matrix[] getMeanVectors(ExampleSet exampleSet, int numberOfAttributes, String[] labels)
			throws OperatorException {
		Matrix[] classMeanVectors = new Matrix[labels.length];
		Attribute labelAttribute = exampleSet.getAttributes().getLabel();
		SplittedExampleSet labelSet = SplittedExampleSet.splitByAttribute(exampleSet, exampleSet.getAttributes().getLabel());
		if (labelSet.getNumberOfSubsets() != labels.length) {
			throw new UserError(this, 118, labelAttribute, labelSet.getNumberOfSubsets(), 2);
		}
		int labelIndex = 0;
		for (String label : labels) {
			// select apropriate subset
			for (int i = 0; i < labels.length; i++) {
				this.checkForStop();
				labelSet.selectSingleSubset(i);
				if (labelSet.getExample(0).getNominalValue(labelAttribute).equals(label)) {
					break;
				}
			}
			// calculate mean
			this.checkForStop();
			labelSet.recalculateAllAttributeStatistics();
			double[] meanValues = new double[numberOfAttributes];
			int i = 0;
			for (Attribute attribute : labelSet.getAttributes()) {
				if (attribute.isNumerical()) {
					meanValues[i] = labelSet.getStatistics(attribute, Statistics.AVERAGE);
				}
				i++;
			}
			classMeanVectors[labelIndex] = new Matrix(meanValues, 1);
			labelIndex++;
		}
		return classMeanVectors;
	}

	protected Matrix[] getInverseCovarianceMatrices(ExampleSet exampleSet, String[] labels) throws UndefinedParameterError,
	OperatorException {
		Matrix[] classInverseCovariances = new Matrix[labels.length];
		Matrix inverse = MathFunctions.invertMatrix(CovarianceMatrix.getCovarianceMatrix(exampleSet, this));
		for (int i = 0; i < labels.length; i++) {
			this.checkForStop();
			classInverseCovariances[i] = inverse;
		}
		return classInverseCovariances;
	}

	@Override
	public Class<? extends PredictionModel> getModelClass() {
		return DiscriminantModel.class;
	}

	@Override
	public boolean supportsCapability(OperatorCapability capability) {
		if (capability.equals(OperatorCapability.NUMERICAL_ATTRIBUTES)) {
			return true;
		}
		if (capability.equals(OperatorCapability.BINOMINAL_LABEL)) {
			return true;
		}
		if (capability.equals(OperatorCapability.POLYNOMINAL_LABEL)) {
			return true;
		}
		return false;
	}

}
