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

import com.rapidminer.example.Attribute;
import com.rapidminer.example.Example;
import com.rapidminer.example.ExampleSet;
import com.rapidminer.operator.OperatorException;
import com.rapidminer.operator.UserError;
import com.rapidminer.tools.container.Tupel;

import java.util.ArrayList;
import java.util.Collections;


/**
 * <p>
 * Performs a FastFourierTransform on an array of complex values. The runtime is {@rapidminer.math
 * O(n log n)}.
 * </p>
 * <p>
 * The used direction simply defines used norm factors, it can be omitted but the resulting values
 * may be quantitative wrong.
 * </p>
 * 
 * @author Ingo Mierswa
 */
public class FastFourierTransform {

	/** Specifies the transformation from time into frequency domain. */
	public static final int TIME2FREQUENCY = 0;

	/** Specifies the transformation from frequency into time domain. */
	public static final int FREQUENCY2TIME = 1;

	/** The window function which should be applied before calculating the FT. */
	private int windowFunctionType = WindowFunction.HANNING;

	public FastFourierTransform(int windowFunctionType) {
		this.windowFunctionType = windowFunctionType;
	}

	/**
	 * Builds the fourier transform from the values of the first attribute onto the second.
	 */
	public Complex[] getFourierTransform(ExampleSet exampleSet, Attribute source, Attribute target) throws OperatorException {
		ArrayList<Tupel<Double, Double>> values = new ArrayList<Tupel<Double, Double>>();
		for (Example example : exampleSet) {
			Tupel<Double, Double> tupel = new Tupel<Double, Double>(example.getValue(target), Double.valueOf(example
					.getValue(source)));
			values.add(tupel);
		}

		Collections.sort(values);
		Complex[] complex = new Complex[exampleSet.size()];
		int k = 0;
		for (Tupel<Double, Double> tupel : values) {
			complex[k++] = new Complex(tupel.getSecond(), 0.0d);
		}
		return getFourierTransform(complex, TIME2FREQUENCY, windowFunctionType);
	}

	/** Normalizes the frequency to the correct value. */
	public static double convertFrequency(double frequency, int nyquist, int totalLength) {
		return frequency / (2.0d * Math.PI) * ((double) totalLength / (double) nyquist);
	}

	/**
	 * Performs a fourier transformation in the specified direction. The window function type
	 * defines one of the possible window functions.
	 */
	public Complex[] getFourierTransform(Complex[] series, int direction, int functionType) throws OperatorException {
		int n = getGreatestPowerOf2LessThan(series.length);
		WindowFunction filter = new WindowFunction(functionType, n);
		if (n < 2) {
			throw new UserError(null, 110, "4");
		}
		int nu = (int) (Math.log(n) / Math.log(2));
		int n2 = n / 2;
		int nu1 = nu - 1;
		double[] xre = new double[n];
		double[] xim = new double[n];
		double tr, ti, p, arg, c, s;
		for (int i = 0; i < n; i++) {
			xre[i] = filter.getFactor(i) * series[i].getReal();
			xim[i] = filter.getFactor(i) * series[i].getImaginary();
		}
		int k = 0;

		for (int l = 1; l <= nu; l++) {
			while (k < n) {
				for (int i = 1; i <= n2; i++) {
					p = bitrev(k >> nu1, nu);
					arg = 2 * Math.PI * p / n;
					c = Math.cos(arg);
					s = Math.sin(arg);
					tr = xre[k + n2] * c + xim[k + n2] * s;
					ti = xim[k + n2] * c - xre[k + n2] * s;
					xre[k + n2] = xre[k] - tr;
					xim[k + n2] = xim[k] - ti;
					xre[k] += tr;
					xim[k] += ti;
					k++;
				}
				k += n2;
			}
			k = 0;
			nu1--;
			n2 = n2 / 2;
		}
		k = 0;
		int r;
		while (k < n) {
			r = bitrev(k, nu);
			if (r > k) {
				tr = xre[k];
				ti = xim[k];
				xre[k] = xre[r];
				xim[k] = xim[r];
				xre[r] = tr;
				xim[r] = ti;
			}
			k++;
		}

		int nyquist = n / 2;
		Complex[] result = new Complex[nyquist];
		switch (direction) {
			case TIME2FREQUENCY:
				for (int i = 0; i < nyquist; i++) {
					result[i] = new Complex(-xre[i], xim[i]);
				}
				break;
			case FREQUENCY2TIME:
				for (int i = 0; i < nyquist; i++) {
					result[i] = new Complex(-xre[i], xim[i]);
				}
				break;
		}
		return result;
	}

	/**
	 * Calculates the greatest power of 2 which is smaller than the given number.
	 */
	public static int getGreatestPowerOf2LessThan(int n) {
		int power = (int) (Math.log(n) / Math.log(2));
		return (int) Math.pow(2, power);
	}

	/** Calculates ... */
	private int bitrev(int j, double nu) {
		int j2;
		int j1 = j;
		int k = 0;
		for (int i = 1; i <= nu; i++) {
			j2 = j1 / 2;
			k = 2 * k + j1 - 2 * j2;
			j1 = j2;
		}
		return k;
	}
}
