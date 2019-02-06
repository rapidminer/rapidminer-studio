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
package com.rapidminer.operator.generator;

/**
 * The label is positive if att1 < 0 or att2 > 0 and att3 < 0.
 * 
 * @author Ingo Mierswa
 */
public class InteractionClassificationFunction extends ClassificationFunction {

	@Override
	public double calculate(double[] att) throws FunctionException {
		if (att.length < 3) {
			throw new FunctionException("Interaction classification function", "needs at least 3 attributes!");
		}
		if ((att[0] < 0.0d) || (att[1] > 0.0d) && (att[2] < 0.0d)) {
			return getLabel().getMapping().mapString("positive");
		} else {
			return getLabel().getMapping().mapString("negative");
		}
	}

	@Override
	public int getMinNumberOfAttributes() {
		return 3;
	}
}
