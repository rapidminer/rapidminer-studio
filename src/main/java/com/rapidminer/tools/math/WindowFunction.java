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
package com.rapidminer.tools.math;

/**
 * Window functions apply a weight to each value of a value series depending on the length of the
 * series. Window Functions like Hanning windows are usually applied before a Fourier
 * transformation.
 * 
 * TODO: remove class and transfer calls to com.rapidminer.tools.math.function.window.WindowFunction
 * 
 * @author Ingo Mierswa
 */
public class WindowFunction {

	/** The currently implemented window functions. */
	public static final String[] FUNCTIONS = { "None", "Hanning", "Hamming", "Blackman", "Blackman-Harris", "Bartlett",
			"Rectangle" };

	/** The constant for the function Hanning. */
	public static final int NONE = 0;

	/** The constant for the function Hanning. */
	public static final int HANNING = 1;

	/** The constant for the function Hamming. */
	public static final int HAMMING = 2;

	/** The constant for the function Blackman. */
	public static final int BLACKMAN = 3;

	/** The constant for the function Blackman-Harris. */
	public static final int BLACKMAN_HARRIS = 4;

	/** The constant for the function Bartlett. */
	public static final int BARTLETT = 5;

	/** The constant for the function Rectangle. */
	public static final int RECTANGLE = 6;

	private int type;

	private int length;

	public WindowFunction(int type, int maxIndex) {
		this.type = type;
		this.length = maxIndex;
	}

	/**
	 * Returns the weighting factor for the current value n in a window of the given length. The
	 * calculation of this factor is done in dependance of the function type.
	 */
	public double getFactor(int n) {
		switch (type) {
			case HANNING:
				return 0.5 - 0.5 * Math.cos(2 * Math.PI * n / length);
			case HAMMING:
				return 0.54 - 0.46 * Math.cos(2 * Math.PI * n / length);
			case BLACKMAN:
				return 0.42 - 0.5 * Math.cos(2 * Math.PI * n / length) + 0.08 * Math.cos(4 * Math.PI * n / length);
			case BLACKMAN_HARRIS:
				return 0.35875 - 0.48829 * Math.cos(2 * Math.PI * n / length) + 0.14128 * Math.cos(4 * Math.PI * n / length)
						- 0.01168 * Math.cos(6 * Math.PI * n / length);
			case BARTLETT:
				return 1.0 - Math.abs(2 * (double) (n - length / 2) / length);
			default:
				return 1.0;
		}
	}
}
