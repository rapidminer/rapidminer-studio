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
package com.rapidminer.operator.performance.test;

import static junit.framework.Assert.assertEquals;
import static com.rapidminer.test_utils.RapidAssert.assertEqualsNaN;

import com.rapidminer.operator.performance.PerformanceCriterion;


/**
 * Tests the given performance  criterion.
 * 
 * @author Simon Fischer
 *          Exp $
 */
public abstract class AbstractCriterionTestCase {

	public static void assertAllValuesEqual(String message, PerformanceCriterion expected, PerformanceCriterion actual) {
		message += " " + expected.getName();
		assertEquals(message + " value", expected.getMikroAverage(), actual.getMikroAverage(), 0.000000001);
		assertEqualsNaN(message + " variance", expected.getMikroVariance(), actual.getMikroVariance());
		assertEqualsNaN(message + " macro value", expected.getMakroAverage(), actual.getMakroAverage());
		assertEqualsNaN(message + " macro variance", expected.getMakroVariance(), actual.getMakroVariance());
		assertEquals(message + " name", expected.getName(), actual.getName());
		assertEquals(message + " class", expected.getClass(), actual.getClass());
	}

	public void cloneTest(String message, PerformanceCriterion pc) {
		try {
			assertAllValuesEqual(message, pc, (PerformanceCriterion) pc.clone());
		} catch (CloneNotSupportedException e) {
			throw new RuntimeException(e.getMessage());
		}
	}
}
