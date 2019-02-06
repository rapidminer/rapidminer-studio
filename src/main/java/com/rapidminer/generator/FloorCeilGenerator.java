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
 * This class has one numerical input attribute and one output attribute. Depending on the mode
 * specified in the constructor the result will be the floor or the ceil function of the input
 * attribute.
 * 
 * @author Ingo Mierswa Exp $
 */
public class FloorCeilGenerator extends SingularNumericalGenerator {

	public static final int FLOOR = 0;

	public static final int CEIL = 1;

	public static final int ROUND = 2;

	private static final String[] FUNCTION_NAMES = { "floor", "ceil", "round" };

	private int mode;

	public FloorCeilGenerator(int mode) {
		this.mode = mode;
	}

	public FloorCeilGenerator() {}

	@Override
	public FeatureGenerator newInstance() {
		return new FloorCeilGenerator(mode);
	}

	@Override
	public double calculateValue(double value) {
		double r = 0;
		switch (mode) {
			case FLOOR:
				r = Math.floor(value);
				break;
			case CEIL:
				r = Math.ceil(value);
				break;
			case ROUND:
				r = Math.round(value);
				break;
		}
		return r;
	}

	@Override
	public void setFunction(String name) {
		for (int i = 0; i < FUNCTION_NAMES.length; i++) {
			if (FUNCTION_NAMES[i].equals(name)) {
				this.mode = i;
				return;
			}
		}
		// LogService.getGlobal().log("Illegal function name '" + name + "' for " +
		// getClass().getName() + ".", LogService.ERROR);
		LogService.getRoot().log(Level.SEVERE, "com.rapidminer.generator.FloorCeilGenerator.illegal_function_name",
				new Object[] { name, getClass().getName() });
	}

	@Override
	public String getFunction() {
		return FUNCTION_NAMES[mode];
	}

	@Override
	public boolean equals(Object o) {
		return (super.equals(o) && (this.mode == ((FloorCeilGenerator) o).mode));
	}

	@Override
	public int hashCode() {
		return super.hashCode() ^ Integer.valueOf(mode).hashCode();
	}
}
