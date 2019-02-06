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
package com.rapidminer.example.table.internal;

import java.util.Arrays;


/**
 * Utility class for auto column calculations.
 *
 * @author Gisa Schaefer
 * @since 7.3.1
 */
final class AutoColumnUtils {

	/** Chunks of 128MB of double or 64MB of integer values */
	static final int CHUNK_SIZE_EXP = 24;
	/** size of a chunk, always a power of 2 */
	static final int CHUNK_SIZE = 1 << CHUNK_SIZE_EXP;
	static final int CHUNK_MODULO_MASK = -1 >>> 32 - CHUNK_SIZE_EXP;

	/** the maximal number of chunks per column */
	static final int MAXIMAL_CHUNKS = Integer.MAX_VALUE / CHUNK_SIZE + 1;

	/** determines after how many values the check for sparse is done */
	static final int THRESHOLD_CHECK_FOR_SPARSE = 2048;

	/** the threshold densitity to change to sparse representation in auto mode */
	static final double THRESHOLD_HIGH_SPARSITY_DENSITY = 0.01;

	/**
	 * the threshold densitity to change to sparse representation in memory-optimized mode for
	 * double values
	 */
	static final double THRESHOLD_DOUBLE_MEDIUM_SPARSITY_DENSITY = 0.5;

	/**
	 * the threshold densitity to change to sparse representation in memory-optimized mode for
	 * integer values
	 */
	static final double THRESHOLD_INTEGER_MEDIUM_SPARSITY_DENSITY = 0.4;

	/** the maximal density a sparse column in auto mode should have */
	static final double THRESHOLD_HIGH_SPARSITY_MAXIMAL_DENSITY = 0.02;

	/** the maximal density a sparse double column in memory-optimized mode should have */
	static final double THRESHOLD_DOUBLE_MEDIUM_SPARSITY_MAXIMAL_DENSITY = 0.55;

	/** the maximal density a sparse integer column in memory-optimized mode should have */
	static final double THRESHOLD_INTEGER_MEDIUM_SPARSITY_MAXIMAL_DENSITY = 0.45;

	/** empty double array to use for empty chunks */
	static final double[] EMPTY_DOUBLE_ARRAY = new double[0];

	/** empty int array to use for empty chunks */
	static final int[] EMPTY_INTEGER_ARRAY = new int[0];

	/** empty byte array to use for empty chunks */
	static final byte[] EMPTY_BYTE_ARRAY = new byte[0];

	/** the missing values for integers */
	static final int INTEGER_NAN = Integer.MIN_VALUE;

	private AutoColumnUtils() {
		// Utility class constructor
	}

	/**
	 * Container for the result of {@link AutoColumnUtils#checkDensity(double[])}. Stores the
	 * density and the most frequent value.
	 *
	 * @author Gisa Schaefer
	 */
	static class DensityResult {

		final double density;
		final double mostFrequentValue;

		DensityResult(double density, double mostFrequentValue) {
			this.density = density;
			this.mostFrequentValue = mostFrequentValue;
		}
	}

	/**
	 * Calculates the most frequent value in the first {@link #THRESHOLD_CHECK_FOR_SPARSE} entries
	 * of the data array and the density of the other values.
	 *
	 * @param data
	 *            the array to check
	 * @return the density of the other values and the most frequent value
	 */
	static DensityResult checkDensity(double[] data) {
		int length = AutoColumnUtils.THRESHOLD_CHECK_FOR_SPARSE;
		double[] sorted = Arrays.copyOf(data, length);
		Arrays.sort(sorted);

		double mostFrequent = sorted[0];
		int mostFrequentCount = 1;

		double currentValue = mostFrequent;
		int currentCount = mostFrequentCount;
		for (int i = 1; i < length; i++) {
			if (sorted[i] == currentValue) {
				currentCount++;
				if (currentCount > mostFrequentCount) {
					mostFrequentCount = currentCount;
					mostFrequent = currentValue;
				}
			} else {
				currentValue = sorted[i];
				currentCount = 1;
			}
		}

		double density = 1.0 - mostFrequentCount / (double) length;
		return new DensityResult(density, mostFrequent);
	}

	/**
	 * Calculates the most frequent value in the in the first {@link #THRESHOLD_CHECK_FOR_SPARSE}
	 * entries of the data array and the density of the other values.
	 *
	 * @param data
	 *            the array to check
	 * @return the density of the other values and the most frequent value
	 */
	static DensityResult checkDensity(int[] data) {
		int length = AutoColumnUtils.THRESHOLD_CHECK_FOR_SPARSE;
		int[] sorted = Arrays.copyOf(data, length);
		Arrays.sort(sorted);

		int mostFrequent = sorted[0];
		int mostFrequentCount = 1;

		int currentValue = mostFrequent;
		int currentCount = mostFrequentCount;
		for (int i = 1; i < length; i++) {
			if (sorted[i] == currentValue) {
				currentCount++;
				if (currentCount > mostFrequentCount) {
					mostFrequentCount = currentCount;
					mostFrequent = currentValue;
				}
			} else {
				currentValue = sorted[i];
				currentCount = 1;
			}
		}

		double density = 1.0 - mostFrequentCount / (double) length;
		return new DensityResult(density, mostFrequent == INTEGER_NAN ? Double.NaN : mostFrequent);
	}

	/**
	 * Copies the end of src after srcOff to dest starting at destOff. Copies the beginning of src
	 * until index to dest until if src and dest are not the same array.
	 *
	 * @param src
	 *            the source array
	 * @param dest
	 *            the destination array
	 * @param index
	 *            the index until which dest should be the same as source
	 * @param srcOff
	 *            from where on to copy the end of src
	 * @param destOff
	 *            from where on to paste the end of src into dest
	 * @param length
	 *            the length to which src is filled
	 */
	static void copy(Object src, Object dest, int index, int srcOff, int destOff, int length) {
		// copy indices after index if exist
		if (length - srcOff > 0) {
			System.arraycopy(src, srcOff, dest, destOff, length - srcOff);
		}
		if (src != dest && index != 0) {
			// copy indices before index
			System.arraycopy(src, 0, dest, 0, index);
		}
	}

}
