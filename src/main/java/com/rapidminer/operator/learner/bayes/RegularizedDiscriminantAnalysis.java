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

import java.util.List;

import Jama.Matrix;

import com.rapidminer.example.Attribute;
import com.rapidminer.example.ExampleSet;
import com.rapidminer.example.set.SplittedExampleSet;
import com.rapidminer.operator.OperatorDescription;
import com.rapidminer.operator.OperatorException;
import com.rapidminer.parameter.ParameterType;
import com.rapidminer.parameter.ParameterTypeDouble;
import com.rapidminer.parameter.UndefinedParameterError;
import com.rapidminer.tools.math.MathFunctions;
import com.rapidminer.tools.math.matrix.CovarianceMatrix;


/**
 * <p>
 * This is a regularized discriminant analysis (RDA) which is a generalization of the LDA or QDA.
 * Both algorithms are special cases of this algorithm, using parameter alpha = 1 respectively alpha
 * = 0.
 * </p>
 *
 * @author Sebastian Land
 */
public class RegularizedDiscriminantAnalysis extends LinearDiscriminantAnalysis {

	public static final String PARAMETER_ALPHA = "alpha";

	public RegularizedDiscriminantAnalysis(OperatorDescription description) {
		super(description);
	}

	@Override
	protected Matrix[] getInverseCovarianceMatrices(ExampleSet exampleSet, String[] labels) throws UndefinedParameterError,
			OperatorException {
		double alpha = getParameterAsDouble(PARAMETER_ALPHA);
		Matrix[] globalInverseCovariances = super.getInverseCovarianceMatrices(exampleSet, labels);

		Matrix[] classInverseCovariances = new Matrix[labels.length];
		Attribute labelAttribute = exampleSet.getAttributes().getLabel();
		SplittedExampleSet labelSet = SplittedExampleSet.splitByAttribute(exampleSet, exampleSet.getAttributes().getLabel());
		int labelIndex = 0;
		for (String label : labels) {
			this.checkForStop();
			// select appropriate subset
			for (int i = 0; i < labels.length; i++) {
				labelSet.selectSingleSubset(i);
				if (labelSet.getExample(0).getNominalValue(labelAttribute).equals(label)) {
					break;
				}
			}
			// calculate inverse matrix
			Matrix inverse = MathFunctions.invertMatrix(CovarianceMatrix.getCovarianceMatrix(labelSet, this));
			classInverseCovariances[labelIndex] = inverse;
			labelIndex++;
		}

		// weighting of the matrices
		Matrix[] regularizedMatrices = new Matrix[classInverseCovariances.length];
		for (int i = 0; i < labels.length; i++) {
			regularizedMatrices[i] = globalInverseCovariances[i].times(alpha).plus(
					classInverseCovariances[i].times(1d - alpha));
		}
		return classInverseCovariances;
	}

	@Override
	protected DiscriminantModel getModel(ExampleSet exampleSet, String[] labels, Matrix[] meanVectors,
			Matrix[] inverseCovariances, double[] aprioriProbabilities) throws UndefinedParameterError {
		return new DiscriminantModel(exampleSet, labels, meanVectors, inverseCovariances, aprioriProbabilities,
				getParameterAsDouble(PARAMETER_ALPHA));
	}

	@Override
	public List<ParameterType> getParameterTypes() {
		List<ParameterType> list = super.getParameterTypes();

		list.add(new ParameterTypeDouble(
				PARAMETER_ALPHA,
				"This is the strength of regularization. 1: Only global covariance is used, 0: Only per class covariance is used.",
				0d, 1d, 0.5d, false));

		return list;
	}
}
