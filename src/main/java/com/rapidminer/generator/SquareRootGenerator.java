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
package com.rapidminer.generator;

import com.rapidminer.tools.LogService;

import java.util.logging.Level;


/**
 * This class has one numerical input attribute and one output attribute. Calculates the value of
 * the square root of the input attribute.
 * 
 * @author Ingo Mierswa Exp $
 */
public class SquareRootGenerator extends SingularNumericalGenerator {

	public SquareRootGenerator() {}

	@Override
	public FeatureGenerator newInstance() {
		return new SquareRootGenerator();
	}

	@Override
	public double calculateValue(double value) {
		return Math.sqrt(value);
	}

	@Override
	public void setFunction(String name) {
		if (!name.equals("sqrt")) {
			// LogService.getGlobal().log("Illegal function name '" + name + "' for " +
			// getClass().getName() + ".", LogService.ERROR);
			LogService.getRoot().log(Level.SEVERE, "com.rapidminer.generator.SquareRootGenerator.illegal_function_name",
					new Object[] { name, getClass().getName() });
		}
	}

	@Override
	public String getFunction() {
		return "sqrt";
	}
}
