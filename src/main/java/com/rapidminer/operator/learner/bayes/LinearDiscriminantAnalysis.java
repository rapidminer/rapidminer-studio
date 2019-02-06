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
 * @see RegularizedDiscriminantAnalysis
 * @see QuadraticDiscriminantAnalysis
 * @author Sebastian Land, Jan Czogalla
 */
public class LinearDiscriminantAnalysis extends RegularizedDiscriminantAnalysis {

	/** The special alpha value for LDA */
	static final double LDA_ALPHA = 1d;

	public LinearDiscriminantAnalysis(OperatorDescription description) {
		super(description);
	}

	@Override
	protected boolean useAlphaParameter() {
		return false;
	}

	@Override
	protected double getAlpha() {
		return LDA_ALPHA;
	}

}
