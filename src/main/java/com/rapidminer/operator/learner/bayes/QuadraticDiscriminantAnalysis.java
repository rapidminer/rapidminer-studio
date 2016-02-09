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
import com.rapidminer.example.set.SplittedExampleSet;
import com.rapidminer.operator.OperatorDescription;
import com.rapidminer.operator.OperatorException;
import com.rapidminer.parameter.UndefinedParameterError;
import com.rapidminer.tools.math.matrix.CovarianceMatrix;


/**
 * <p>
 * This operator performs a quadratic discriminant analysis (QDA). QDA is closely related to linear
 * discriminant analysis (LDA), where it is assumed that the measurements are normally distributed.
 * Unlike LDA however, in QDA there is no assumption that the covariance of each of the classes is
 * identical.
 * </p>
 *
 * @author Sebastian Land
 */
public class QuadraticDiscriminantAnalysis extends LinearDiscriminantAnalysis {

	public QuadraticDiscriminantAnalysis(OperatorDescription description) {
		super(description);
	}

	@Override
	protected DiscriminantModel getModel(ExampleSet exampleSet, String[] labels, Matrix[] meanVectors,
			Matrix[] inverseCovariances, double[] aprioriProbabilities) {
		return new DiscriminantModel(exampleSet, labels, meanVectors, inverseCovariances, aprioriProbabilities, 1d);
	}

	@Override
	protected Matrix[] getInverseCovarianceMatrices(ExampleSet exampleSet, String[] labels) throws UndefinedParameterError,
	OperatorException {
		Matrix[] classInverseCovariances = new Matrix[labels.length];
		Attribute labelAttribute = exampleSet.getAttributes().getLabel();
		SplittedExampleSet labelSet = SplittedExampleSet.splitByAttribute(exampleSet, exampleSet.getAttributes().getLabel());
		int labelIndex = 0;
		for (String label : labels) {
			this.checkForStop();
			// select apropriate subset
			for (int i = 0; i < labels.length; i++) {
				labelSet.selectSingleSubset(i);
				if (labelSet.getExample(0).getNominalValue(labelAttribute).equals(label)) {
					break;
				}
			}
			// calculate inverse matrix
			Matrix inverse = CovarianceMatrix.getCovarianceMatrix(labelSet, this).inverse();
			classInverseCovariances[labelIndex] = inverse;
			labelIndex++;
		}
		return classInverseCovariances;
	}
}
