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
package com.rapidminer.tools.math.similarity.divergences;

import Jama.Matrix;

import com.rapidminer.example.ExampleSet;
import com.rapidminer.example.Tools;
import com.rapidminer.operator.OperatorException;
import com.rapidminer.tools.math.matrix.CovarianceMatrix;
import com.rapidminer.tools.math.similarity.BregmanDivergence;


/**
 * The &quot;Mahalanobis distance &quot;.
 *
 * @author Sebastian Land, Regina Fritsch
 */
public class MahalanobisDistance extends BregmanDivergence {

	private static final long serialVersionUID = -5986526237805285428L;
	private Matrix inverseCovariance;

	@Override
	public double calculateDistance(double[] value1, double[] value2) {

		Matrix x = new Matrix(value1, value1.length);
		Matrix y = new Matrix(value2, value2.length);

		Matrix deltaxy = x.minus(y);

		// compute the mahalanobis distance
		return Math.sqrt(deltaxy.transpose().times(inverseCovariance).times(deltaxy).get(0, 0));
	}

	@Override
	public void init(ExampleSet exampleSet) throws OperatorException {
		super.init(exampleSet);
		Tools.onlyNumericalAttributes(exampleSet, "value based similarities");
		inverseCovariance = CovarianceMatrix.getCovarianceMatrix(exampleSet, null).inverse();
	}

	@Override
	public String toString() {
		return "Mahalanobis distance";
	}
}
