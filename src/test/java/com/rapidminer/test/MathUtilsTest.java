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
package com.rapidminer.test;

import static junit.framework.Assert.assertEquals;

import org.junit.Test;

import com.rapidminer.tools.math.MathFunctions;

/**
 * A test for the {@link MathFunctions}.
 * 
 * @author Michael Wurst, Ingo Mierswa
 */
public class MathUtilsTest {

	@Test
	public void testVariance() {
		assertEquals(MathFunctions.variance(new double[] { 0.1, 0.1, 0.0, -0.1 }, Double.NEGATIVE_INFINITY), 0.006875, 0.001);
		assertEquals(MathFunctions.variance(new double[] { 0.0, 0.0, 0.0 }, -1.0), 0.0);
	}

	@Test
	public void testCorrelation() {
		assertEquals(MathFunctions.correlation(new double[] { 0.1, 0.2, -0.3, 0.0 }, new double[] { 0.0, 0.1, 0.1, -0.1 }), -0.161, 0.001);
	}
}
