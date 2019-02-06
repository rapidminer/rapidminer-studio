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

import org.junit.Test;

import com.rapidminer.example.Attribute;
import com.rapidminer.example.Attributes;
import com.rapidminer.example.Example;
import com.rapidminer.example.ExampleSet;
import com.rapidminer.example.table.DataRowFactory;
import com.rapidminer.example.test.ExampleTestTools;
import com.rapidminer.example.utils.ExampleSetBuilder;
import com.rapidminer.example.utils.ExampleSets;
import com.rapidminer.operator.performance.AbstractPerformanceEvaluator;
import com.rapidminer.operator.performance.BinaryClassificationPerformance;
import com.rapidminer.operator.performance.MultiClassificationPerformance;
import com.rapidminer.operator.performance.PerformanceCriterion;
import com.rapidminer.operator.performance.PerformanceVector;


/**
 * Tests classification criteria.
 *
 * @author Simon Fischer, Ingo Mierswa ingomierswa Exp $
 */
public class ClassificationCriterionTest extends AbstractCriterionTestCase {

	@Test
	public void testClassificationError() throws Exception {
		Attribute label = ExampleTestTools.attributeYesNo();
		label.setTableIndex(0);
		int no = label.getMapping().mapString("no"); // negative class
		int yes = label.getMapping().mapString("yes"); // positive class
		List<Attribute> attributeList = new LinkedList<Attribute>();
		attributeList.add(label);
		ExampleSetBuilder builder = ExampleSets.from(attributeList)
				.withDataRowReader(ExampleTestTools.createDataRowReader(
						new DataRowFactory(DataRowFactory.TYPE_DOUBLE_ARRAY, '.'), new Attribute[] { label },
						new String[][] { { "no" }, { "yes" }, { "yes" }, { "no" }, { "yes" }, { "no" }, { "yes" }, { "yes" },
								{ "yes" }, { "no" }, { "no" }, { "yes" } }));

		ExampleSet eSet = builder.withRole(label, Attributes.LABEL_NAME).build();
		Attribute predictedLabel = ExampleTestTools.createPredictedLabel(eSet);

		// eSet.createPredictedLabel();
		Iterator<Example> r = eSet.iterator();
		Example e;
		e = r.next();
		e.setValue(predictedLabel, no); // nn
		e = r.next();
		e.setValue(predictedLabel, yes); // yy
		e = r.next();
		e.setValue(predictedLabel, no); // yn
		e = r.next();
		e.setValue(predictedLabel, yes); // ny
		e = r.next();
		e.setValue(predictedLabel, yes); // yy
		e = r.next();
		e.setValue(predictedLabel, no); // nn
		e = r.next();
		e.setValue(predictedLabel, yes); // yy
		e = r.next();
		e.setValue(predictedLabel, no); // yn
		e = r.next();
		e.setValue(predictedLabel, no); // yn
		e = r.next();
		e.setValue(predictedLabel, no); // nn
		e = r.next();
		e.setValue(predictedLabel, yes); // ny
		e = r.next();
		e.setValue(predictedLabel, yes); // yy
		// 4x yy (TP)
		// 3x nn (TN)
		// 3x yn (FN)
		// 2x ny (FP)

		PerformanceVector pv = new PerformanceVector();
		for (int i = 0; i < MultiClassificationPerformance.NAMES.length; i++)
			pv.addCriterion(new MultiClassificationPerformance(i));
		for (int i = 0; i < BinaryClassificationPerformance.NAMES.length; i++)
			pv.addCriterion(new BinaryClassificationPerformance(i));
		AbstractPerformanceEvaluator.evaluate(null, eSet, pv, new LinkedList<PerformanceCriterion>(), false, true);

		assertEquals("accuracy", 7.0 / 12.0, pv.getCriterion(MultiClassificationPerformance.NAMES[MultiClassificationPerformance.ACCURACY]).getAverage(), 0.00000001);
		assertEquals("classification_error", 5.0 / 12.0, pv.getCriterion(MultiClassificationPerformance.NAMES[MultiClassificationPerformance.ERROR]).getAverage(), 0.00000001);
		assertEquals("precision", 4.0 / 6.0, pv.getCriterion(BinaryClassificationPerformance.NAMES[BinaryClassificationPerformance.PRECISION]).getAverage(), 0.00000001);
		assertEquals("recall", 4.0 / 7.0, pv.getCriterion(BinaryClassificationPerformance.NAMES[BinaryClassificationPerformance.RECALL]).getAverage(), 0.00000001);
		assertEquals("fallout", 2.0 / 5.0, pv.getCriterion(BinaryClassificationPerformance.NAMES[BinaryClassificationPerformance.FALLOUT]).getAverage(), 0.00000001);
		assertEquals("true_pos", 4, pv.getCriterion(BinaryClassificationPerformance.NAMES[BinaryClassificationPerformance.TRUE_POSITIVE]).getAverage(), 0.00000001);
		assertEquals("true_neg", 3, pv.getCriterion(BinaryClassificationPerformance.NAMES[BinaryClassificationPerformance.TRUE_NEGATIVE]).getAverage(), 0.00000001);
		assertEquals("false_pos", 2, pv.getCriterion(BinaryClassificationPerformance.NAMES[BinaryClassificationPerformance.FALSE_POSITIVE]).getAverage(), 0.00000001);
		assertEquals("false_neg", 3, pv.getCriterion(BinaryClassificationPerformance.NAMES[BinaryClassificationPerformance.FALSE_NEGATIVE]).getAverage(), 0.00000001);
	}

	@Test
	public void testUCCClone() {
		double counter[][] = { { 3, 5 }, { 4, 6 } };
		cloneTest("", new BinaryClassificationPerformance(BinaryClassificationPerformance.TRUE_POSITIVE, counter));
		cloneTest("", new BinaryClassificationPerformance(BinaryClassificationPerformance.TRUE_NEGATIVE, counter));
		cloneTest("", new BinaryClassificationPerformance(BinaryClassificationPerformance.FALSE_POSITIVE, counter));
		cloneTest("", new BinaryClassificationPerformance(BinaryClassificationPerformance.FALSE_NEGATIVE, counter));

	}

	@Test
	public void testUCCAverage() {
		double counter1[][] = { { 3, 5 }, { 4, 6 } };
		double counter2[][] = { { 5, 8 }, { 2, 9 } };
		double sum[][] = { { 8, 13 }, { 6, 15 } };
		BinaryClassificationPerformance[] ucc1 = new BinaryClassificationPerformance[4];
		ucc1[0] = new BinaryClassificationPerformance(BinaryClassificationPerformance.TRUE_POSITIVE, counter1);
		ucc1[1] = new BinaryClassificationPerformance(BinaryClassificationPerformance.TRUE_NEGATIVE, counter1);
		ucc1[2] = new BinaryClassificationPerformance(BinaryClassificationPerformance.FALSE_POSITIVE, counter1);
		ucc1[3] = new BinaryClassificationPerformance(BinaryClassificationPerformance.FALSE_NEGATIVE, counter1);

		BinaryClassificationPerformance[] ucc2 = new BinaryClassificationPerformance[4];
		ucc2[0] = new BinaryClassificationPerformance(BinaryClassificationPerformance.TRUE_POSITIVE, counter2);
		ucc2[1] = new BinaryClassificationPerformance(BinaryClassificationPerformance.TRUE_NEGATIVE, counter2);
		ucc2[2] = new BinaryClassificationPerformance(BinaryClassificationPerformance.FALSE_POSITIVE, counter2);
		ucc2[3] = new BinaryClassificationPerformance(BinaryClassificationPerformance.FALSE_NEGATIVE, counter2);

		BinaryClassificationPerformance[] avg = new BinaryClassificationPerformance[4];
		avg[0] = new BinaryClassificationPerformance(BinaryClassificationPerformance.TRUE_POSITIVE, sum);
		avg[1] = new BinaryClassificationPerformance(BinaryClassificationPerformance.TRUE_NEGATIVE, sum);
		avg[2] = new BinaryClassificationPerformance(BinaryClassificationPerformance.FALSE_POSITIVE, sum);
		avg[3] = new BinaryClassificationPerformance(BinaryClassificationPerformance.FALSE_NEGATIVE, sum);

		for (int i = 0; i < ucc1.length; i++) {
			ucc1[i].buildAverage(ucc2[i]);
			assertEquals(ucc1[i].getName(), avg[i].getMikroAverage(), ucc1[i].getMikroAverage(), 0.0000001);
		}
	}
}
