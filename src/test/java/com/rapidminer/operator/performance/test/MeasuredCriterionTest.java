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

import java.util.Iterator;
import java.util.LinkedList;
import java.util.List;

import org.junit.After;
import org.junit.Before;
import org.junit.Test;

import com.rapidminer.example.Attribute;
import com.rapidminer.example.Attributes;
import com.rapidminer.example.Example;
import com.rapidminer.example.ExampleSet;
import com.rapidminer.example.test.ExampleTestTools;
import com.rapidminer.example.utils.ExampleSets;
import com.rapidminer.operator.performance.AbsoluteError;
import com.rapidminer.operator.performance.AbstractPerformanceEvaluator;
import com.rapidminer.operator.performance.PerformanceCriterion;
import com.rapidminer.operator.performance.PerformanceVector;
import com.rapidminer.operator.performance.SquaredError;


/**
 * Tests regression critetia.
 *
 * @author Simon Fischer, Ingo Mierswa ingomierswa Exp $
 */
public class MeasuredCriterionTest extends AbstractCriterionTestCase {

	private ExampleSet exampleSet1, exampleSet2;

	private ExampleSet createExampleSet(double[][] labelValues, double[] predictedValues) throws Exception {
		Attribute label = ExampleTestTools.attributeReal();
		label.setTableIndex(0);
		List<Attribute> attributeList = new LinkedList<Attribute>();
		attributeList.add(label);

		ExampleSet exampleSet = ExampleSets.from(attributeList)
				.withDataRowReader(ExampleTestTools.createDataRowReader(labelValues)).withRole(label, Attributes.LABEL_NAME)
				.build();

		Attribute predictedLabel = ExampleTestTools.createPredictedLabel(exampleSet);
		Iterator<Example> r = exampleSet.iterator();
		for (int i = 0; i < predictedValues.length; i++) {
			r.next().setValue(predictedLabel, predictedValues[i]);
		}

		return exampleSet;
	}

	@Before
	public void setUp() throws Exception {
		exampleSet1 = createExampleSet(new double[][] { { 5.0 }, { 3.0 }, { -1.0 }, { -4.0 }, { 0.0 }, { 2.0 } },
				new double[] { 6.0, 1.0, 0.0, -1.0, 3.0, -2.0 });
		exampleSet2 = createExampleSet(new double[][] { { 3.0 }, { 6.0 }, { -1.0 } }, new double[] { 1.0, 8.0, -4.0 });
	}

	@After
	public void tearDown() throws Exception {
		exampleSet1 = exampleSet2 = null;
	}

	/** Tests calculation, average, and clone. */
	private void criterionTest(PerformanceCriterion c1, PerformanceCriterion c2, double expected1, double expected2, double expectedOverall) throws Exception {
		PerformanceVector pv1 = new PerformanceVector();
		pv1.addCriterion(c1);
		AbstractPerformanceEvaluator.evaluate(null, exampleSet1, pv1, new LinkedList<PerformanceCriterion>(), false, true);
		assertEquals(c1.getName() + " 1", expected1, c1.getAverage(), 0.00000001);
		assertEquals(c1.getName() + " 1 clone", expected1, ((PerformanceCriterion) c1.clone()).getAverage(), 0.00000001);

		PerformanceVector pv2 = new PerformanceVector();
		pv2.addCriterion(c2);
		AbstractPerformanceEvaluator.evaluate(null, exampleSet2, pv2, new LinkedList<PerformanceCriterion>(), false, true);
		assertEquals(c2.getName() + " 2", expected2, c2.getAverage(), 0.00000001);
		assertEquals(c2.getName() + " 2 clone", expected2, ((PerformanceCriterion) c2.clone()).getAverage(), 0.00000001);

		c1.buildAverage(c2);
		assertEquals(c1.getName() + " average", expectedOverall, c1.getMikroAverage(), 0.00000001);
		assertEquals(c1.getName() + " macro average", (expected1 + expected2) / 2.0, c1.getMakroAverage(), 0.00000001);
	}

	@Test
	public void testAbsoluteError() throws Exception {
		criterionTest(new AbsoluteError(), new AbsoluteError(), 14.0 / 6.0, 7.0 / 3.0, 21.0 / 9.0);
	}

	@Test
	public void testSquaredError() throws Exception {
		criterionTest(new SquaredError(), new SquaredError(), 40.0 / 6.0, 17.0 / 3.0, 57.0 / 9.0);
	}

	// public void testScaledError() throws Exception {
	// criterionTest(new ScaledError(), new ScaledError(),
	// 14.0/6.0/9.0,
	// 7.0/3.0/7.0,
	// (14.0/9.0 + 7.0/7.0) / 9.0);
	// }
}
