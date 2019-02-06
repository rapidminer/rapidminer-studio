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
package com.rapidminer.example.set;

import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

import org.junit.Assert;
import org.junit.Test;

import com.rapidminer.example.Attribute;
import com.rapidminer.example.Attributes;
import com.rapidminer.example.ExampleSet;
import com.rapidminer.example.Statistics;
import com.rapidminer.example.test.ExampleTestTools;
import com.rapidminer.example.utils.ExampleSets;


/**
 *
 * Parallel test of the methods {@link AbstractExampleSet#recalculateAllAttributeStatistics()} and
 * {@link AbstractExampleSet#getStatistics(Attribute, String)} where a
 * ConcurrentModificationException existed.
 *
 * @author Andreas Timm
 *
 */
public class ConcurrentExampleSetModificationTest {

	private final static int AMOUNT_PARALLEL_EXECUTIONS = 1000;

	private final static Attribute attribute1 = ExampleTestTools.attributeDogCatMouse();

	private final static String[] ALL_STATISTICS_TYPES = { Statistics.AVERAGE, Statistics.AVERAGE_WEIGHTED, Statistics.COUNT,
			Statistics.LEAST, Statistics.MAXIMUM, Statistics.MINIMUM, Statistics.MODE, Statistics.SUM,
			Statistics.SUM_WEIGHTED, Statistics.UNKNOWN, Statistics.VARIANCE, Statistics.VARIANCE_WEIGHTED };

	@Test
	public void testConcurrentModification() throws Exception {
		ExampleSet testit = createExampleSet(new double[][] { { 5.0 }, { 3.0 }, { -1.0 }, { -4.0 }, { 0.0 }, { 2.0 } });

		testit.recalculateAllAttributeStatistics();
		testit.getStatistics(attribute1, Statistics.AVERAGE, null);
		ExecutorService executor = Executors.newFixedThreadPool(AMOUNT_PARALLEL_EXECUTIONS);
		int i = 0;
		while (i++ < AMOUNT_PARALLEL_EXECUTIONS) {
			executor.execute(new RWThread(testit));
		}

		while (i-- > 0) {
			for (String statisticsName : ALL_STATISTICS_TYPES) {
				double statistics = testit.getStatistics(attribute1, statisticsName, "dog");
				if (statisticsName == Statistics.COUNT) {
					Assert.assertEquals(1, statistics, 0);
				}
			}
		}
	}

	private class RWThread implements Runnable {

		private ExampleSet aes;

		public RWThread(ExampleSet exampleSet) {
			aes = exampleSet;
		}

		@Override
		public void run() {

			for (String statisticsName : ALL_STATISTICS_TYPES) {
				double statistics = aes.getStatistics(attribute1, statisticsName, "dog");
				aes.recalculateAllAttributeStatistics();
				// looking for miscalculations from concurrent modifications
				if (statisticsName == Statistics.COUNT) {
					Assert.assertEquals(1, statistics, 0);
				}
			}
		}

	}

	private ExampleSet createExampleSet(double[][] labelValues) throws Exception {
		Attribute label = attribute1;

		ExampleSet exampleSet = ExampleSets.from(label).withDataRowReader(ExampleTestTools.createDataRowReader(labelValues))
				.withRole(label, Attributes.LABEL_NAME).build();

		return exampleSet;
	}

}
