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
package com.rapidminer.tools.math.function.window;

import java.lang.reflect.InvocationTargetException;


/**
 * A window function applies weights to the data points in a series window. Left, right or center
 * justification allows to adjust which data points should get the highest weights. In case of
 * center justification the window function is symmetric.
 * 
 * @author Tobias Malbrecht
 */
public abstract class WindowFunction {

	@SuppressWarnings("unchecked")
	public static final Class<? extends WindowFunction>[] FUNCTIONS = new Class[] { RectangularWindowFunction.class,
			TriangularWindowFunction.class, GaussianWindowFunction.class, HannWindowFunction.class,
			HammingWindowFunction.class, BlackmanWindowFunction.class, BlackmanHarrisWindowFunction.class,
			BartlettWindowFunction.class };

	public static final String[] FUNCTION_NAMES = { "Rectangular", "Triangular", "Gaussian", "Hann", "Hamming", "Blackman",
			"Blackman-Harris", "Bartlett" };

	public static final int RECTANGULAR = 0;

	public static final int TRIANGULAR = 1;

	public static final int GAUSSIAN = 2;

	public static final int HANN = 3;

	public static final int HAMMING = 4;

	public static final int BLACKMAN = 5;

	public static final int BLACKMAN_HARRIS = 6;

	public static final int BARTLETT = 7;

	public static final int JUSTIFY_CENTER = 0;

	public static final int JUSTIFY_LEFT = 1;

	public static final int JUSTIFY_RIGHT = 2;

	private int width;

	private int justifiedWidth;

	private int justifiedOffset;

	@SuppressWarnings("unchecked")
	public static WindowFunction createWindowFunction(String functionName, int justification, int width)
			throws InstantiationException, IllegalAccessException, ClassNotFoundException, NoSuchMethodException,
			InvocationTargetException {
		int typeIndex = -1;
		for (int i = 0; i < FUNCTION_NAMES.length; i++) {
			if (FUNCTION_NAMES[i].equals(functionName)) {
				typeIndex = i;
				break;
			}
		}
		Class<? extends WindowFunction> clazz = null;
		if (typeIndex < 0) {
			clazz = (Class<? extends WindowFunction>) Class.forName(functionName);
		} else {
			clazz = FUNCTIONS[typeIndex];
		}
		return clazz.getConstructor(Integer.class, Integer.class).newInstance(width, justification);
	}

	public static WindowFunction createWindowFunction(String functionName, int width) throws InstantiationException,
			IllegalAccessException, ClassNotFoundException, NoSuchMethodException, InvocationTargetException {
		return createWindowFunction(functionName, JUSTIFY_CENTER, width);
	}

	public static WindowFunction createWindowFunction(int typeIndex, int justification, int width)
			throws InstantiationException, IllegalAccessException, NoSuchMethodException, InvocationTargetException {
		if (typeIndex >= 0 && typeIndex < FUNCTION_NAMES.length) {
			Class<? extends WindowFunction> clazz = FUNCTIONS[typeIndex];
			return clazz.getConstructor(Integer.class, Integer.class).newInstance(width, justification);
		} else {
			throw new InstantiationException();
		}
	}

	public static WindowFunction createWindowFunction(int typeIndex, int width)
			throws InstantiationException, IllegalAccessException, NoSuchMethodException, InvocationTargetException {
		return createWindowFunction(typeIndex, JUSTIFY_CENTER, width);
	}

	public WindowFunction(int width) {
		this(width, JUSTIFY_CENTER);
	}

	public WindowFunction(int width, int justification) {
		this.width = width;
		switch (justification) {
			case JUSTIFY_CENTER:
				this.justifiedWidth = width;
				this.justifiedOffset = 0;
				break;
			case JUSTIFY_LEFT:
				this.justifiedWidth = width * 2;
				this.justifiedOffset = width;
				break;
			case JUSTIFY_RIGHT:
				this.justifiedWidth = width * 2;
				this.justifiedOffset = 0;
				break;
		}
	}

	public double[] getWeights() {
		double[] weights = new double[width];
		for (int i = 0; i < width; i++) {
			weights[i] = getValue(justifiedWidth, justifiedOffset + i);
		}
		return weights;
	}

	protected abstract double getValue(int width, int n);

	public double getValue(int n) {
		return getValue(width, n);
	}
}
