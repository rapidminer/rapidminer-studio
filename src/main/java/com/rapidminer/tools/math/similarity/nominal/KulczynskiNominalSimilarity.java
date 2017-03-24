/**
 * Copyright (C) 2001-2017 by RapidMiner and the contributors
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
package com.rapidminer.tools.math.similarity.nominal;

/**
 * Implements the Kulczynski similarity for nominal attributes.
 * 
 * @author Sebastian Land, Michael Wurst
 */
public class KulczynskiNominalSimilarity extends AbstractNominalSimilarity {

	private static final long serialVersionUID = -3272271856059376791L;

	@Override
	protected double calculateSimilarity(double equalNonFalseValues, double unequalValues, double falseValues) {
		return equalNonFalseValues / unequalValues;
	}

	@Override
	public String toString() {
		return "Kulczynski nominal similarity";
	}

}
