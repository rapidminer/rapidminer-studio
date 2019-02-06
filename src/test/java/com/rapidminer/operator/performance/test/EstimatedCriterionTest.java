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

import org.junit.After;
import org.junit.Before;
import org.junit.Test;

import com.rapidminer.operator.performance.EstimatedPerformance;
import com.rapidminer.operator.performance.PerformanceCriterion;

/**
 * Tests {@link EstimatedPerformance}.
 * 
 * @author Simon Fischer, Ingo Mierswa
 *          ingomierswa Exp $
 */
public  class EstimatedCriterionTest extends AbstractCriterionTestCase {

	private EstimatedPerformance performance10x08, performance20x04;

	@Before
	public void setUp() throws Exception {
		performance10x08 = new EstimatedPerformance("test_performance", 0.8, 10, false);
		performance20x04 = new EstimatedPerformance("test_performance", 0.4, 20, false);
	}

	@After
	public void tearDown() throws Exception {
		performance10x08 = performance20x04 = null;
	}

	/**
	 * Tests micro and macro average. Since macro average is implemented in
	 * {@link PerformanceCriterion}, this does not have to be tested for
	 * measured performance criteria.
	 */
	@Test
	public void testAverage() {
		performance10x08.buildAverage(performance20x04);
		assertEquals("Wrong weighted average", (10 * 0.8 + 20 * 0.4) / (10 + 20), performance10x08.getMikroAverage(), 0.0000001);
		assertEquals("Wrong macro average", (0.8 + 0.4) / 2, performance10x08.getMakroAverage(), 0.0000001);
	}

	@Test
	public void testClone() {
		cloneTest("Clone of simple criterion", performance10x08);
		performance10x08.buildAverage(performance20x04);
		cloneTest("Clone of averaged criterion", performance10x08);
	}

}
