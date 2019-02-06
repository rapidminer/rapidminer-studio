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
package com.rapidminer.operator.learner.tree;

import com.rapidminer.example.ExampleSet;
import com.rapidminer.tools.Tools;


/**
 * A {@link Tree} with numerical prediction values instead of strings. Used to predict numerical labels.
 *
 * @author Gisa Meier
 * @since 8.0
 */
public class RegressionTree extends Tree {

	private static final long serialVersionUID = 6211804131385125306L;

	private double value = Double.NaN;

	public RegressionTree(ExampleSet trainingSet) {
		super(trainingSet);
	}

	@Override
	public boolean isNumerical() {
		return true;
	}

	/**
	 * Sets the numeric prediction value for a leaf.
	 *
	 * @param value
	 *            the value to set
	 */
	public void setLeaf(double value) {
		this.value = value;
	}

	/**
	 * @return the numeric prediction value or NaN if none was set
	 */
	public double getValue() {
		return value;
	}

	@Override
	public String getLabel() {
		return Tools.formatNumber(value);
	}

}
