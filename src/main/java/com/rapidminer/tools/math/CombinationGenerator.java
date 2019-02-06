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

import java.math.BigInteger;


/**
 * This class can be used to iterate over all combinations of r elements out of n. Usage:
 * <code>CombinationGenerator x = new CombinationGenerator(elements.length, 3);</code><br />
 * <code>while (x.hasMore()) {</code><br />
 * <code>int[] indices = x.getNext();</code><br />
 * <code>}</code>
 * 
 * @author Ingo Mierswa
 */
public class CombinationGenerator {

	private int[] a;
	private int n;
	private int r;
	private BigInteger numLeft;
	private BigInteger total;

	public CombinationGenerator(int n, int r) {
		if (r > n) {
			throw new IllegalArgumentException();
		}
		if (n < 1) {
			throw new IllegalArgumentException();
		}
		this.n = n;
		this.r = r;
		a = new int[r];
		BigInteger nFact = getFactorial(n);
		BigInteger rFact = getFactorial(r);
		BigInteger nminusrFact = getFactorial(n - r);
		total = nFact.divide(rFact.multiply(nminusrFact));
		reset();
	}

	/** Resets this combination generator. */
	public void reset() {
		for (int i = 0; i < a.length; i++) {
			a[i] = i;
		}
		numLeft = new BigInteger(total.toString());
	}

	/** Return number of combinations not yet generated. */
	public BigInteger getNumberOfCombinationsLeft() {
		return numLeft;
	}

	/** Returns true if there are more combinations left. */
	public boolean hasMore() {
		return numLeft.compareTo(BigInteger.ZERO) == 1;
	}

	/** Returns the total number of combinations. */
	public BigInteger getTotal() {
		return total;
	}

	/** Computes the factorial of the given number. */
	private static BigInteger getFactorial(int n) {
		BigInteger fact = BigInteger.ONE;
		for (int i = n; i > 1; i--) {
			fact = fact.multiply(new BigInteger(Integer.toString(i)));
		}
		return fact;
	}

	/** Generates the next combination by using the algorithm proposed by Rosen. */
	public int[] getNext() {
		if (numLeft.equals(total)) {
			numLeft = numLeft.subtract(BigInteger.ONE);
			return a;
		}

		int i = r - 1;
		while (a[i] == n - r + i) {
			i--;
		}
		a[i] = a[i] + 1;
		for (int j = i + 1; j < r; j++) {
			a[j] = a[i] + j - i;
		}

		numLeft = numLeft.subtract(BigInteger.ONE);
		return a;
	}
}
