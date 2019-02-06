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
package com.rapidminer.operator.learner.bayes;

import java.util.Arrays;
import java.util.List;

import com.rapidminer.example.Attribute;
import com.rapidminer.example.ExampleSet;
import com.rapidminer.example.Statistics;
import com.rapidminer.example.set.SplittedExampleSet;
import com.rapidminer.example.table.NominalMapping;
import com.rapidminer.operator.Model;
import com.rapidminer.operator.Operator;
import com.rapidminer.operator.OperatorCapability;
import com.rapidminer.operator.OperatorDescription;
import com.rapidminer.operator.OperatorException;
import com.rapidminer.operator.OperatorVersion;
import com.rapidminer.operator.UserError;
import com.rapidminer.operator.learner.AbstractLearner;
import com.rapidminer.operator.learner.PredictionModel;
import com.rapidminer.parameter.ParameterType;
import com.rapidminer.parameter.ParameterTypeBoolean;
import com.rapidminer.parameter.ParameterTypeDouble;
import com.rapidminer.parameter.UndefinedParameterError;
import com.rapidminer.tools.math.MathFunctions;
import com.rapidminer.tools.math.matrix.CovarianceMatrix;

import Jama.Matrix;


/**
 * <p>
 * This is a regularized discriminant analysis (RDA) which is a generalization of the LDA or QDA.
 * Both algorithms are special cases of this algorithm, using parameter alpha = 1 respectively alpha
 * = 0.
 * </p>
 *
 * @see LinearDiscriminantAnalysis
 * @see QuadraticDiscriminantAnalysis
 * @author Sebastian Land, Jan Czogalla
 */
public class RegularizedDiscriminantAnalysis extends AbstractLearner {

	public static final OperatorVersion PRE_FIXED_REGULARIZED_DA = new OperatorVersion(7, 4, 0);

	/** The parameter name of the alpha parameter */
	public static final String PARAMETER_ALPHA = "alpha";

	public static final String PARAMETER_APPROXIMATE_INVERSE = "approximate_covariance_inverse";

	public RegularizedDiscriminantAnalysis(OperatorDescription description) {
		super(description);
	}

	@Override
	public OperatorVersion[] getIncompatibleVersionChanges() {
		OperatorVersion[] versions = super.getIncompatibleVersionChanges();
		if (this.getClass() == RegularizedDiscriminantAnalysis.class) {
			versions = Arrays.copyOf(versions, versions.length + 1);
			versions[versions.length - 1] = PRE_FIXED_REGULARIZED_DA;
		}
		return versions;
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

		return getModel(exampleSet, labelValues, meanVectors, inverseCovariance);
	}

	/**
	 * Calculates the mean vectors for examples with the specified label strings, grouped by the
	 * label strings.
	 *
	 * @param exampleSet
	 *            the example set
	 * @param numberOfAttributes
	 *            the number of attributes
	 * @param labels
	 *            the relevant label strings
	 * @return the matrices containing all mean vectors
	 * @throws UserError
	 *             if at least one of the label strings is not present in the example set
	 */
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

	/**
	 * Returns the inverse covariance matrices for this XDA instance. Is a convenience method for
	 * {@link #getRegularizedInverseCovarianceMatrices(ExampleSet, String[], RegularizedDiscriminantAnalysis)
	 * getRegularizedInverseCovarianceMatrices}.
	 *
	 * @param exampleSet
	 *            the example set
	 * @param labels
	 *            the relevant label strings
	 * @return the inverse covariance matrices
	 */
	protected Matrix[] getInverseCovarianceMatrices(ExampleSet exampleSet, String[] labels)
			throws UndefinedParameterError, OperatorException {
		return getRegularizedInverseCovarianceMatrices(exampleSet, labels, this);
	}

	/**
	 * Returns the regularized inverse covariance matrices for a given XDA instance. Uses that
	 * instance to extract the alpha parameter.
	 *
	 * @param exampleSet
	 *            the example set
	 * @param labels
	 *            the relevant label strings
	 * @param xda
	 *            the instance of an XDA operator
	 * @return the inverse covariance matrices
	 */
	protected static Matrix[] getRegularizedInverseCovarianceMatrices(ExampleSet exampleSet, String[] labels,
			RegularizedDiscriminantAnalysis xda) throws UndefinedParameterError, OperatorException {
		double alpha = xda.getAlpha();

		// we don't always need both sets of matrices
		// alpha = 0 => QDA only
		// alpha = 1 => LDA only
		// else => both
		Matrix[] globalInverseCovariances = null;
		if (alpha > 0) {
			globalInverseCovariances = getLinearInverseCovarianceMatrices(exampleSet, labels, xda);
		}
		Matrix[] classInverseCovariances = null;
		if (alpha < 1) {
			classInverseCovariances = getQuadraticInverseCovarianceMatrices(exampleSet, labels, xda);
		}

		if (globalInverseCovariances == null) {
			return classInverseCovariances;
		} else if (classInverseCovariances == null) {
			return globalInverseCovariances;
		}

		// weighting of the matrices (0 < alpha < 1)
		Matrix[] regularizedMatrices = new Matrix[classInverseCovariances.length];
		for (int i = 0; i < labels.length; i++) {
			regularizedMatrices[i] = globalInverseCovariances[i].times(alpha)
					.plus(classInverseCovariances[i].times(1d - alpha));
		}
		return regularizedMatrices;
	}

	/**
	 * Returns the linear (global) inverse covariance matrices for a given XDA instance. Uses that
	 * instance to extract the alpha parameter.
	 *
	 * @param exampleSet
	 *            the example set
	 * @param labels
	 *            the relevant label strings
	 * @param op
	 *            the operator (if any) to check for process stop
	 * @return the linear inverse covariance matrices
	 */
	protected static Matrix[] getLinearInverseCovarianceMatrices(ExampleSet exampleSet, String[] labels, Operator op)
			throws OperatorException {
		boolean checkForStop = op != null;
		boolean approximateInverse = op == null || op.getParameterAsBoolean(PARAMETER_APPROXIMATE_INVERSE);
		Matrix[] classInverseCovariances = new Matrix[labels.length];
		Matrix inverse = MathFunctions.invertMatrix(CovarianceMatrix.getCovarianceMatrix(exampleSet, op), approximateInverse);
		if (inverse == null){
			throw new UserError(op, "regularized_discriminant_analysis.singular_covariance_matrix");
		}
		for (int i = 0; i < labels.length; i++) {
			if (checkForStop) {
				op.checkForStop();
			}
			classInverseCovariances[i] = inverse;
		}
		return classInverseCovariances;
	}

	/**
	 * Returns the quadratic (class) inverse covariance matrices for a given XDA instance. Uses that
	 * instance to extract the alpha parameter.
	 *
	 * @param exampleSet
	 *            the example set
	 * @param labels
	 *            the relevant label strings
	 * @param op
	 *            the operator (if any) to check for process stop
	 * @return the quadratic inverse covariance matrices
	 */
	protected static Matrix[] getQuadraticInverseCovarianceMatrices(ExampleSet exampleSet, String[] labels, Operator op)
			throws OperatorException {
		boolean checkForStop = op != null;
		boolean approximateInverse = op == null || op.getParameterAsBoolean(PARAMETER_APPROXIMATE_INVERSE);
		Matrix[] classInverseCovariances = new Matrix[labels.length];
		Attribute labelAttribute = exampleSet.getAttributes().getLabel();
		SplittedExampleSet labelSet = SplittedExampleSet.splitByAttribute(exampleSet, exampleSet.getAttributes().getLabel());
		int labelIndex = 0;
		for (String label : labels) {
			// select appropriate subset
			for (int i = 0; i < labels.length; i++) {
				labelSet.selectSingleSubset(i);
				if (labelSet.getExample(0).getNominalValue(labelAttribute).equals(label)) {
					break;
				}
			}
			if (checkForStop) {
				op.checkForStop();
			}
			// calculate inverse matrix
			Matrix inverse = MathFunctions.invertMatrix(CovarianceMatrix.getCovarianceMatrix(labelSet, op), approximateInverse);
			if (inverse == null){
				throw new UserError(op, "regularized_discriminant_analysis.singular_covariance_matrix");
			}
			classInverseCovariances[labelIndex] = inverse;
			labelIndex++;
		}
		return classInverseCovariances;
	}

	/**
	 * Convenience method for creating a {@link DiscriminantModel}. Uses
	 * {@link #getAprioriProbabilities(ExampleSet, String[]) getAprioriProbabilities} and
	 * {@link #getAlpha()}.
	 */
	protected DiscriminantModel getModel(ExampleSet exampleSet, String[] labels, Matrix[] meanVectors,
			Matrix[] inverseCovariances) throws UndefinedParameterError {
		return new DiscriminantModel(exampleSet, labels, meanVectors, inverseCovariances,
				getAprioriProbabilities(exampleSet, labels), getAlpha());
	}

	/**
	 * Indicates whether this operator should show the alpha parameter. Subclasses
	 * {@link LinearDiscriminantAnalysis} and {@link QuadraticDiscriminantAnalysis} override this
	 * method with false, since they have special alpha values.
	 */
	protected boolean useAlphaParameter() {
		return true;
	}

	/**
	 * Returns the alpha parameter. Subclasses may return special values.
	 *
	 * @throws UndefinedParameterError
	 */
	protected double getAlpha() throws UndefinedParameterError {
		return getCompatibilityLevel().isAbove(PRE_FIXED_REGULARIZED_DA) ? getParameterAsDouble(PARAMETER_ALPHA)
				: QuadraticDiscriminantAnalysis.QDA_ALPHA;
	}

	/**
	 * Returns the apriori probabilities for the given example set and label strings.
	 */
	private static double[] getAprioriProbabilities(ExampleSet exampleSet, String[] labels) {
		double[] aprioriProbabilites = new double[labels.length];
		double totalSize = exampleSet.size();
		Attribute labelAttribute = exampleSet.getAttributes().getLabel();
		SplittedExampleSet labelSet = SplittedExampleSet.splitByAttribute(exampleSet, exampleSet.getAttributes().getLabel());
		int labelIndex = 0;
		for (String label : labels) {
			// select appropriate subset
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

	@Override
	public List<ParameterType> getParameterTypes() {
		List<ParameterType> list = super.getParameterTypes();

		if (useAlphaParameter()) {
			list.add(new ParameterTypeDouble(PARAMETER_ALPHA,
					"This is the strength of regularization. 1: Only global covariance is used, 0: Only per class covariance is used.",
					0d, 1d, 0.5d, false));
		}

		ParameterType type = new ParameterTypeBoolean(PARAMETER_APPROXIMATE_INVERSE, "Indicate whether covariance matrix inverse should be approximated if a direct inverse does not exist.", true, true);
		list.add(type);
		return list;
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
