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

import java.util.Collection;
import java.util.Iterator;

import com.rapidminer.example.Attribute;
import com.rapidminer.example.Example;
import com.rapidminer.example.ExampleSet;

import Jama.Matrix;


/**
 * This class provides mathematical functions not already provided by <tt>java.lang.Math</tt>:
 * <ul>
 * <li><tt>tanh()</tt> : tangens hyperbolicus, <i>y = tanh(x) = (e^x - e^-x) / (e^x + e^-x)</i></li>
 * </ul>
 * 
 * @author Ralf Klinkenberg, Ingo Mierswa
 */
public class MathFunctions {

	protected static final double log2 = Math.log(2.0d);

	/**
	 * coefficients for polynomials used in normalInverse() to estimate normal distribution;
	 */
	// coefficients for approximation of intervall 0,138... < probability <
	// 0,861...
	protected static final double DIVISOR_COEFFICIENTS_0[] = { -5.99633501014107895267E1, 9.80010754185999661536E1,
			-5.66762857469070293439E1, 1.39312609387279679503E1, -1.23916583867381258016E0, };

	protected static final double DIVIDER_COEFFICIENTS_0[] = { 1.00000000000000000000E0, 1.95448858338141759834E0,
			4.67627912898881538453E0, 8.63602421390890590575E1, -2.25462687854119370527E2, 2.00260212380060660359E2,
			-8.20372256168333339912E1, 1.59056225126211695515E1, -1.18331621121330003142E0, };

	// coefficients for approximation of intervall exp(-32) < probability <
	// 0,138...
	// or 0,861 < probability < 1 - exp(-32)
	protected static final double DIVISOR_COEFFICIENTS_1[] = { 4.05544892305962419923E0, 3.15251094599893866154E1,
			5.71628192246421288162E1, 4.40805073893200834700E1, 1.46849561928858024014E1, 2.18663306850790267539E0,
			-1.40256079171354495875E-1, -3.50424626827848203418E-2, -8.57456785154685413611E-4, };

	protected static final double DIVIDER_COEFFICIENTS_1[] = { 1.00000000000000000000E0, 1.57799883256466749731E1,
			4.53907635128879210584E1, 4.13172038254672030440E1, 1.50425385692907503408E1, 2.50464946208309415979E0,
			-1.42182922854787788574E-1, -3.80806407691578277194E-2, -9.33259480895457427372E-4, };

	// coefficients for approximation of intervall 0 < probability < exp(-32)
	// or 1 - exp(-32) < probability < 1
	protected static final double DIVISOR_COEFFICIENTS_3[] = { 3.23774891776946035970E0, 6.91522889068984211695E0,
			3.93881025292474443415E0, 1.33303460815807542389E0, 2.01485389549179081538E-1, 1.23716634817820021358E-2,
			3.01581553508235416007E-4, 2.65806974686737550832E-6, 6.23974539184983293730E-9, };

	protected static final double DIVIDER_COEFFICIENTS_3[] = { 1.00000000000000000000E0, 6.02427039364742014255E0,
			3.67983563856160859403E0, 1.37702099489081330271E0, 2.16236993594496635890E-1, 1.34204006088543189037E-2,
			3.28014464682127739104E-4, 2.89247864745380683936E-6, 6.79019408009981274425E-9, };

	// constants for invertMatrix
	private static final int INVERSE_ITERATIONS = 5;
	private static final double MINIMUM_ADDITION = Double.MIN_VALUE * Math.pow(10, INVERSE_ITERATIONS);
	private static final double MINIMUM_THRESHOLD = Double.MIN_VALUE * 100;
	private static final double ADDITION_FACTOR = 1.01;


	/**
	 * returns tangens hyperbolicus of <tt>x</tt>, i.e. <i>y = tanh(x) = (e^x - e^-x) / (e^x +
	 * e^-x)</i>.
	 */
	public static double tanh(double x) {
		return ((java.lang.Math.exp(x) - java.lang.Math.exp(-x)) / (java.lang.Math.exp(x) + java.lang.Math.exp(-x)));
	}

	/**
	 * Returns the value x for which the area under the normal probability density function
	 * (integrated from minus infinity to this value x) is equal to the given probability. The
	 * normal distribution has mean of zero and variance of one.
	 * 
	 * @param probability
	 *            the area under the normal pdf
	 * @return x
	 */
	public static double normalInverse(double probability) {

		final double smallArgumentEnd = Math.exp(-2);
		final double rootedPi = Math.sqrt(2.0 * Math.PI);

		if (probability <= 0.0) {
			throw new IllegalArgumentException();
		}
		if (probability >= 1.0) {
			throw new IllegalArgumentException();
		}

		boolean wrappedArround = false;
		if (probability > (1.0 - smallArgumentEnd)) {
			probability = 1.0 - probability;
			wrappedArround = true;
		}

		if (probability > smallArgumentEnd) {
			// approximation for intervall 0,138... < probability < 0,861...
			probability = probability - 0.5;
			double squaredProbability = probability * probability;
			double x = probability;
			x += probability
					* (squaredProbability * solvePolynomial(squaredProbability, DIVISOR_COEFFICIENTS_0) / solvePolynomial(
							squaredProbability, DIVIDER_COEFFICIENTS_0));
			x = x * rootedPi;
			return (x);
		} else {
			double x = Math.sqrt(-2.0 * Math.log(probability));
			double inversedX = 1.0 / x;
			if (x < 8.0) { // equal to probability > exp(-32)
				// approximation for intervall exp(-32) < probability < 0,138...
				// or 0,861 < probability < 1 - exp(-32)
				x = (x - Math.log(x) / x) - inversedX * solvePolynomial(inversedX, DIVISOR_COEFFICIENTS_1)
						/ solvePolynomial(inversedX, DIVIDER_COEFFICIENTS_1);
			} else {
				// approximation for intervall 0 < probability < exp(-32) or 1 -
				// exp(-32) < probability < 1
				x = (x - Math.log(x) / x) - inversedX * solvePolynomial(inversedX, DIVISOR_COEFFICIENTS_3)
						/ solvePolynomial(inversedX, DIVIDER_COEFFICIENTS_3);
			}
			if (!wrappedArround) {
				x = -x;
			}
			return (x);
		}
	}

	/**
	 * Solves a given polynomial at x. The polynomial is given by the coefficients. The coefficients
	 * are stored in natural order: coefficients[i] : c_i*x^i
	 */
	public static double solvePolynomial(double x, double[] coefficients) {
		double value = coefficients[0];
		for (int i = 1; i < coefficients.length; i++) {
			value += coefficients[i] * Math.pow(x, i);
		}
		return value;
	}

	/**
	 * @param v
	 *            a vector values
	 * @param a
	 *            a threeshold, only values greater equal this value are used in the calculation
	 * @return the variance
	 */
	public static double variance(double v[], double a) {
		// calc mean
		double sum = 0.0;
		int counter = 0;

		for (int i = 0; i < v.length; i++) {
			if (v[i] >= a) {
				sum = sum + v[i];
				counter++;
			}
		}
		double mean = sum / counter;

		sum = 0.0;
		counter = 0;
		for (int i = 0; i < v.length; i++) {
			if (v[i] >= a) {
				sum = sum + (v[i] - mean) * (v[i] - mean);
				counter++;
			}
		}
		double variance = sum / counter;
		return variance;
	}

	/** This method calculates the correlation between two (numerical) attributes of an example set. */
	public static double correlation(ExampleSet exampleSet, Attribute firstAttribute, Attribute secondAttribute,
									 boolean squared) {
		double sumProd = 0.0d;
		double sumFirst = 0.0d;
		double sumSecond = 0.0d;
		double sumFirstSquared = 0.0d;
		double sumSecondSquared = 0.0d;
		int counter = 0;

		Iterator<Example> reader = exampleSet.iterator();
		while (reader.hasNext()) {
			Example example = reader.next();
			double first = example.getValue(firstAttribute);
			double second = example.getValue(secondAttribute);
			double prod = first * second;
			if (!Double.isNaN(prod)) {
				sumProd += prod;
				sumFirst += first;
				sumFirstSquared += first * first;
				sumSecond += second;
				sumSecondSquared += second * second;
				counter++;
			}
		}
		double divisor = Math.sqrt((counter * sumFirstSquared - sumFirst * sumFirst)
				* (counter * sumSecondSquared - sumSecond * sumSecond));
		double r;
		if (divisor == 0) {
			// one or both of the standard deviations are 0 -> correlation is undefined
			r = Double.NaN;
		} else {
			r = (counter * sumProd - sumFirst * sumSecond) / divisor;
		}
		if (squared) {
			return r * r;
		} else {
			return r;
		}
	}

	public static double correlation(double[] x1, double[] x2) {
		// Calculate the mean and stddev
		int counter = 0;
		double sum1 = 0.0;
		double sum2 = 0.0;
		double sumS1 = 0.0;
		double sumS2 = 0.0;

		for (int i = 0; i < x1.length; i++) {
			sum1 = sum1 + x1[i];
			sum2 = sum2 + x2[i];
			counter++;
		}

		double mean1 = sum1 / counter;
		double mean2 = sum2 / counter;

		double sum = 0.0;
		counter = 0;

		for (int i = 0; i < x1.length; i++) {
			sum = sum + (x1[i] - mean1) * (x2[i] - mean2);
			sumS1 = sumS1 + (x1[i] - mean1) * (x1[i] - mean1);
			sumS2 = sumS2 + (x2[i] - mean2) * (x2[i] - mean2);
			counter++;
		}

		return sum / Math.sqrt(sumS1 * sumS2);
	}

	public static double robustMin(double m1, double m2) {
		double min = Math.min(m1, m2);
		if (!Double.isNaN(min)) {
			return min;
		} else {
			if (Double.isNaN(m1)) {
				return m2;
			} else {
				return m1;
			}
		}
	}

	public static double robustMax(double m1, double m2) {
		double max = Math.max(m1, m2);
		if (!Double.isNaN(max)) {
			return max;
		} else {
			if (Double.isNaN(m1)) {
				return m2;
			} else {
				return m1;
			}
		}
	}

	/**
	 * This method returns the logarithmus dualis from value
	 * 
	 * @param value
	 *            the value
	 * @return the log2 of value
	 */
	public static double ld(double value) {
		return Math.log(value) / log2;
	}

	/**
	 * Returns the greatest common divisor (GCD) of the given pair of values.
	 */
	public static long getGCD(long a, long b) {
		long c;
		while (b != 0) {
			c = a % b;
			a = b;
			b = c;
		}
		return a;
	}

	/**
	 * Returns the greatest common divisor (GCD) of the given pair of values.
	 */
	public static long getGCD(Collection<Long> collection) {
		boolean first = true;
		long currentGCD = 1;
		Iterator<Long> i = collection.iterator();
		while (i.hasNext()) {
			long value = i.next();
			if (first) {
				currentGCD = value;
				first = false;
			} else {
				currentGCD = getGCD(currentGCD, value);
			}
		}
		return currentGCD;
	}

	public static int factorial(int k) {
		int result = 1;
		for (int i = k; i > 1; i--) {
			result += i;
		}
		return result;
	}

	/**
	 * @deprecated since 8.1; please use {@link #invertMatrix(Matrix, boolean)} instead.
	 */
	@Deprecated
	public static Matrix invertMatrix(Matrix m) {
		double startFactor = 0.1d;
		while (true) {
			try {
				Matrix inverse = m.inverse();
				return (inverse);
			} catch (Exception e) {
				for (int x = 0; x < m.getColumnDimension(); x++) {
					for (int y = 0; y < m.getRowDimension(); y++) {
						m.set(x, y, m.get(x, y) + startFactor);
					}
				}
				startFactor *= 10d;
			}
		}
	}

	/**
	 * Tries to invert the given {@link Matrix}. If that is not possible and {@code approximate} is set to {@true}, an approximate inversion will be calculated.
	 * To do this, a small value is added to the diagonal of the matrix and then used for inversion. This procedure will be repeated only {@value #INVERSE_ITERATIONS} times.
	 *
	 * @param m
	 * 		the matrix to invert
	 * @param approximate
	 * 		if an approximation should be calculated
	 * @return the (approximated) inverted matrix or {@code null} if no inverse could be calculated
	 * @since 8.1
	 */
	public static Matrix invertMatrix(Matrix m, boolean approximate) {
		int dimension = Math.min(m.getRowDimension(), m.getColumnDimension());
		for (int i = 0; i < INVERSE_ITERATIONS; i++) {
			try {
				return m.inverse();
			} catch (Exception e) {
				if (!approximate) {
					return null;
				}
				for (int x = 0; x < dimension; x++) {
					double value = m.get(x, x);
					value = Math.abs(value) <= MINIMUM_THRESHOLD ? MINIMUM_ADDITION : value * ADDITION_FACTOR;
					m.set(x, x, value);
				}
			}
		}
		return null;
	}

	/**
	 * Function to square a double. Use this instead of {@link Math#pow}(value,2) for performance reasons.
	 *
	 * @param value
	 * 		the value to square
	 * @return the value to the power of two
	 * @since 9.4.1
	 */
	public static double square(double value){
		return value * value;
	}
}
