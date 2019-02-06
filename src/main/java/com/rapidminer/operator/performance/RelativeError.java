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
 * The average relative error: <i>Sum(|label-predicted|/label)/#examples</i>. The relative error of
 * label 0 and prediction 0 is defined as 0, the relative error of label 0 and prediction != 0 is
 * infinite.
 * 
 * @author Stefan Rueping, Ingo Mierswa
 */
public class RelativeError extends SimpleCriterion {

	private static final long serialVersionUID = 203943264201733699L;

	public RelativeError() {}

	public RelativeError(RelativeError sc) {
		super(sc);
	}

	@Override
	public double countExample(double label, double predictedLabel) {
		double diff = Math.abs(label - predictedLabel);
		double absLabel = Math.abs(label);
		if (Tools.isZero(absLabel)) {
			return Double.NaN;
		} else {
			return diff / absLabel;
		}
	}

	/**
	 * Indicates wether or not percentage format should be used in the {@link #toString} method. The
	 * default implementation returns false.
	 */
	@Override
	public boolean formatPercent() {
		return true;
	}

	@Override
	public String getName() {
		return "relative_error";
	}

	@Override
	public String getDescription() {
		return "Average relative error (average of absolute deviation of the prediction from the actual value divided by actual value)";
	}
}
