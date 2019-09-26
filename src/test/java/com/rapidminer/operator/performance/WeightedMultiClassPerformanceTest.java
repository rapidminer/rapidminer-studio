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
package com.rapidminer.operator.performance;

import static org.junit.Assert.*;

import org.junit.Test;

import com.rapidminer.example.Attribute;
import com.rapidminer.example.Attributes;
import com.rapidminer.example.ExampleSet;
import com.rapidminer.example.table.AttributeFactory;
import com.rapidminer.example.utils.ExampleSets;
import com.rapidminer.operator.OperatorException;
import com.rapidminer.tools.Ontology;
import com.rapidminer.tools.RandomGenerator;


/**
 * Tests for {@link WeightedMultiClassPerformance}.
 *
 * @author Jan Czogalla
 * @since 9.4
 */
public class WeightedMultiClassPerformanceTest {

	private static final String[] ERROR_MSGS = {
			"default constructor",
			"with undefined type",
			"with weighted recall",
			"with weighted precision",
	};

	/** Test coyp constructor for each other constructor before and after counting */
	@Test
	public void testCopyConstructor() throws OperatorException {
		WeightedMultiClassPerformance[] performancesToTest = {new WeightedMultiClassPerformance(),
				new WeightedMultiClassPerformance(WeightedMultiClassPerformance.UNDEFINED),
				new WeightedMultiClassPerformance(WeightedMultiClassPerformance.WEIGHTED_RECALL),
				new WeightedMultiClassPerformance(WeightedMultiClassPerformance.WEIGHTED_PRECISION)};
		RandomGenerator rng = new RandomGenerator(RandomGenerator.DEFAULT_SEED);
		Attribute att = AttributeFactory.createAttribute("att", Ontology.REAL);
		Attribute label = AttributeFactory.createAttribute(Attributes.LABEL_NAME, Ontology.NOMINAL);
		label.getMapping().mapString("a");
		label.getMapping().mapString("b");
		Attribute prediction = AttributeFactory.createAttribute(Attributes.PREDICTION_NAME, Ontology.NOMINAL);
		prediction.getMapping().mapString("a");
		prediction.getMapping().mapString("b");
		ExampleSet set = ExampleSets.from(att, label, prediction).withBlankSize(10).withRole(label, label.getName())
				.withRole(prediction, prediction.getName())
				.withColumnFiller(att, i -> rng.nextDoubleInRange(-10, 10))
				.withColumnFiller(label, i -> rng.nextInt(2))
				.withColumnFiller(prediction, i -> rng.nextInt(2))
				.build();
		for (int i = 0; i < performancesToTest.length; i++) {
			WeightedMultiClassPerformance performance = performancesToTest[i];
			try {
				new WeightedMultiClassPerformance(performance);
			} catch (Exception e) {
				e.printStackTrace();
				fail("Can not clone weighted MCP from " + ERROR_MSGS[i]);
			}
			performance.startCounting(set, true);
			set.forEach(performance::countExample);
			try {
				new WeightedMultiClassPerformance(performance);
			} catch (Exception e) {
				e.printStackTrace();
				fail("Can not clone weighted MCP from " + ERROR_MSGS[i]);
			}
		}
	}

}