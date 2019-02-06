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
package com.rapidminer.tools.math.distribution;

import com.rapidminer.tools.Tools;

import java.util.ArrayList;


/**
 * The distribution for a discrete variable.
 * 
 * @author Tobias Malbrecht
 */
public class DiscreteDistribution implements Distribution {

	private static final long serialVersionUID = 7573474548080998479L;

	private String attributeName;

	private double[] probabilities;

	private String[] valueNames;

	public DiscreteDistribution(String attributeName, double[] probabilities, String[] valueNames) {
		this.attributeName = attributeName;
		this.probabilities = probabilities;
		this.valueNames = valueNames;
	}

	@Override
	public final boolean isDiscrete() {
		return true;
	}

	@Override
	public final boolean isContinuous() {
		return false;
	}

	@Override
	public double getProbability(double value) {
		int index = (int) value;
		if (index >= 0 && index < probabilities.length) {
			return probabilities[index];
		} else {
			return Double.NaN;
		}
	}

	/** Returns the name of the attribute the distribution belongs to. */
	@Override
	public String getAttributeName() {
		return attributeName;
	}

	/**
	 * This method returns a collection of all nominal attribute values. TODO: This is not fully
	 * legally: not guaranteed that the indices are in a sequence, starting with 0!!!
	 * */
	public ArrayList<Double> getValues() {
		ArrayList<Double> values = new ArrayList<Double>();
		for (int i = 0; i < probabilities.length; i++) {
			values.add((double) i);
		}
		return values;
	}

	@Override
	public String mapValue(double value) {
		int index = (int) value;
		if (index >= 0 && index < valueNames.length) {
			return valueNames[index];
		} else {
			return null;
		}
	}

	@Override
	public String toString() {
		StringBuffer distributionDescription = new StringBuffer();
		boolean first = true;
		for (int i = 0; i < valueNames.length; i++) {
			if (!first) {
				distributionDescription.append("\t");
			}
			distributionDescription.append(valueNames[i]);
			first = false;
		}
		first = true;
		distributionDescription.append(Tools.getLineSeparator());
		for (int i = 0; i < valueNames.length; i++) {
			if (!first) {
				distributionDescription.append("\t");
			}
			distributionDescription.append(Tools.formatNumber(probabilities[i]));
			first = false;
		}
		return distributionDescription.toString();
	}

	@Override
	public int getNumberOfParameters() {
		return valueNames.length;
	}

	@Override
	public String getParameterName(int index) {
		return "value=" + valueNames[index];
	}

	@Override
	public double getParameterValue(int index) {
		return probabilities[index];
	}

	public String[] getValueNames() {
		return valueNames;
	}
}
