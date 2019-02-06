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

import com.rapidminer.operator.OperatorDescription;


/**
 * <p>
 * This operator performs a quadratic discriminant analysis (QDA). QDA is closely related to linear
 * discriminant analysis (LDA), where it is assumed that the measurements are normally distributed.
 * Unlike LDA however, in QDA there is no assumption that the covariance of each of the classes is
 * identical.
 * </p>
 *
 * @see RegularizedDiscriminantAnalysis
 * @see LinearDiscriminantAnalysis
 * @author Sebastian Land, Jan Czogalla
 */
public class QuadraticDiscriminantAnalysis extends RegularizedDiscriminantAnalysis {

	/** The special alpha value for QDA */
	static final double QDA_ALPHA = 0d;

	public QuadraticDiscriminantAnalysis(OperatorDescription description) {
		super(description);
	}

	@Override
	protected boolean useAlphaParameter() {
		return false;
	}

	@Override
	protected double getAlpha() {
		return QDA_ALPHA;
	}

}
