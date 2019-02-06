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
package com.rapidminer.operator.performance;

import com.rapidminer.tools.Tools;


/**
 * The average relative error in a strict way of calculation: <i>Sum(|label-predicted|/min(|label|,
 * |predicted|))/#examples</i>. The relative error of label 0 and prediction 0 is defined as 0. If
 * the minimum of label and prediction is 0, the relative error is defined as infinite.
 * 
 * @author Ingo Mierswa
 */
public class StrictRelativeError extends SimpleCriterion {

	private static final long serialVersionUID = 8055914052886853327L;

	public StrictRelativeError() {}

	public StrictRelativeError(StrictRelativeError sc) {
		super(sc);
	}

	@Override
	public double countExample(double label, double predictedLabel) {
		double diff = Math.abs(label - predictedLabel);
		double absLabel = Math.abs(label);
		double absPrediction = Math.abs(predictedLabel);
		if (Tools.isZero(diff)) {
			return 0.0d;
		} else {
			double min = Math.min(absLabel, absPrediction);
			if (Tools.isZero(min)) {
				return Double.POSITIVE_INFINITY;
			} else {
				return diff / min;
			}
		}
	}

	/**
	 * Indicates whether or not percentage format should be used in the {@link #toString} method.
	 * The default implementation returns false.
	 */
	@Override
	public boolean formatPercent() {
		return true;
	}

	@Override
	public String getName() {
		return "relative_error_strict";
	}

	@Override
	public String getDescription() {
		return "Average strict relative error (average of absolute deviation of the prediction from the actual value divided by minimum of the actual value and the prediction)";
	}
}
