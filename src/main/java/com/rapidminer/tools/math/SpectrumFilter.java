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
 * Creates a series of peaks (a spectrum) from the series of complex values calculated for example
 * by a fourier transform. Since the FT input values must not be equivalent the magnitudes of the
 * spectrum do not correspond to the real amplitudes. Like the human ear this filter tries to
 * calculate more accurate values (see ERB or Bark filters).
 * 
 * @author Ingo Mierswa
 */
public class SpectrumFilter {

	public static final int NONE = 0;

	public static final int ERB = 1;

	public static final int LOG = 2;

	public static final int LOG2 = 3;

	private int weightType;

	public SpectrumFilter(int weightType) {
		this.weightType = weightType;
	}

	public Peak[] filter(Complex[] spectrum, int totalSize) {
		Peak[] result = new Peak[spectrum.length];
		for (int i = 0; i < result.length; i++) {
			result[i] = new Peak(FastFourierTransform.convertFrequency(i, spectrum.length, totalSize), filterValue(
					spectrum[i].getMagnitude(spectrum.length * 2), i));
		}
		return result;
	}

	private double filterValue(double value, int index) {
		switch (weightType) {
			case ERB:
				return value * (21.4d * Math.log(0.00437d * index + 1.0d) / Math.log(10.0d));
			case LOG:
				return value * (10.0d * Math.log(index + 1) / Math.log(20.0d));
			case LOG2:
				return value * (0.7d * Math.log(5 * index + 1));
			default:
				return value;
		}
	}
}
