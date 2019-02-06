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
 * This class is a very simple implementation of a FeatureGenerator. It has two numerical input
 * attributes and one output attribute. Depending on the mode specified in the constructor the
 * result will be the sum, difference, product or quotient. The four modes are numered from 0 to 3
 * int this order.
 * 
 * @author Simon Fischer, Ingo Mierswa 15:35:40 ingomierswa Exp $
 */
public class BasicArithmeticOperationGenerator extends BinaryNumericalGenerator {

	public static final int SUM = 0;

	public static final int DIFFERENCE = 1;

	public static final int PRODUCT = 2;

	public static final int QUOTIENT = 3;

	private static final String[] FUNCTION_NAMES = { "+", "-", "*", "/" };

	private int mode;

	public BasicArithmeticOperationGenerator(int mode) {
		this.mode = mode;
	}

	public BasicArithmeticOperationGenerator() {}

	@Override
	public boolean isSelfApplicable() {
		return mode == PRODUCT;
	}

	@Override
	public boolean isCommutative() {
		return ((mode == PRODUCT) || (mode == SUM));
	}

	@Override
	public FeatureGenerator newInstance() {
		return new BasicArithmeticOperationGenerator(mode);
	}

	@Override
	public double calculateValue(double o1, double o2) {
		double r = 0.0d;
		switch (mode) {
			case SUM:
				r = o1 + o2;
				break;
			case DIFFERENCE:
				r = o1 - o2;
				break;
			case PRODUCT:
				r = o1 * o2;
				break;
			case QUOTIENT:
				r = o1 / o2;
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
		LogService.getRoot().log(Level.SEVERE,
				"com.rapidminer.generator.BasicArithmeticOperationGenerator.illegal_function_name",
				new Object[] { name, getClass().getName() });
	}

	@Override
	public String getFunction() {
		return FUNCTION_NAMES[mode];
	}

	@Override
	public boolean equals(Object o) {
		return (super.equals(o) && (this.mode == ((BasicArithmeticOperationGenerator) o).mode));
	}

	@Override
	public int hashCode() {
		return super.hashCode() ^ Integer.valueOf(mode).hashCode();
	}
}
