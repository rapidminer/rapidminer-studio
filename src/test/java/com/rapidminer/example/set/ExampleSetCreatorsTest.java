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

import static org.junit.Assert.assertEquals;

import java.util.Iterator;

import org.junit.Test;

import com.rapidminer.example.Attribute;
import com.rapidminer.example.Attributes;
import com.rapidminer.example.Example;
import com.rapidminer.example.ExampleSet;
import com.rapidminer.example.table.AttributeFactory;
import com.rapidminer.example.test.ExampleTestTools;
import com.rapidminer.example.utils.ExampleSets;
import com.rapidminer.operator.ViewModel;
import com.rapidminer.operator.preprocessing.filter.NominalToNumeric;
import com.rapidminer.operator.preprocessing.filter.NominalToNumericModel;
import com.rapidminer.tools.Ontology;


/**
 * Test the methods {@link AttributeSelectionExampleSet#create}, {@link ModelViewExampleSet#create},
 * {@link NonSpecialAttributesExampleSet#create}, {@link RemappedExampleSet#create}, and
 * {@link ReplaceMissingExampleSet#create}.
 *
 * @author Gisa Schaefer
 */
public class ExampleSetCreatorsTest {

	private final static Attribute attribute1 = ExampleTestTools.attributeDogCatMouse();
	private final static Attribute attribute2 = ExampleTestTools.attributeInt();
	private final static Attribute attribute3 = ExampleTestTools.attributeYesNo();
	private final static Attribute attribute4 = ExampleTestTools.attributeReal();

	@Test
	public void attributeSelectionExampleSet() {
		ExampleSet simpleExampleSet = ExampleSets.from(attribute1, attribute2, attribute3, attribute4)
				.addRow(new double[] { 2, 5, 1, 5.6 }).build();

		boolean[] selection = new boolean[] { true, false, false, true };

		@SuppressWarnings("deprecation")
		ExampleSet constructorSet = new AttributeSelectionExampleSet(simpleExampleSet, selection);

		ExampleSet creatorSet = AttributeSelectionExampleSet.create(simpleExampleSet, selection);

		compare(constructorSet, creatorSet);

	}

	@Test
	public void modelViewExampleSet() {
		ExampleSet simpleExampleSet = ExampleSets.from(attribute1, attribute2, attribute3, attribute4)
				.addRow(new double[] { 2, 5, 1, 5.6 }).build();

		ViewModel model = new NominalToNumericModel(simpleExampleSet, NominalToNumeric.INTEGERS_CODING, false, null, null,
				null, false, 0);

		@SuppressWarnings("deprecation")
		ExampleSet constructorSet = new ModelViewExampleSet(simpleExampleSet, model);

		ExampleSet creatorSet = ModelViewExampleSet.create(simpleExampleSet, model);

		compare(constructorSet, creatorSet);

	}

	@Test
	public void nonSpecialAttributesExampleSet() {
		ExampleSet simpleExampleSet = ExampleSets.from(attribute1, attribute2, attribute3, attribute4)
				.addRow(new double[] { 2, 5, 1, 5.6 }).withRole(attribute1, Attributes.LABEL_NAME)
				.withRole(attribute4, Attributes.ID_NAME).build();

		@SuppressWarnings("deprecation")
		ExampleSet constructorSet = new NonSpecialAttributesExampleSet(simpleExampleSet);

		ExampleSet creatorSet = NonSpecialAttributesExampleSet.create(simpleExampleSet);

		assertEquals(constructorSet.getAttributes().specialSize(), creatorSet.getAttributes().specialSize(), 0);

		compare(constructorSet, creatorSet);

	}

	@Test
	public void RemappedExampleSet() {
		ExampleSet mappingExampleSet = ExampleSets.from(attribute1, attribute2, attribute3, attribute4)
				.addRow(new double[] { 2, 5, 1, 5.6 }).build();

		Attribute attribute1otherMapping = AttributeFactory.createAttribute("animal", Ontology.NOMINAL);
		attribute1otherMapping.getMapping().mapString("mouse");
		attribute1otherMapping.getMapping().mapString("dog");
		attribute1otherMapping.getMapping().mapString("cat");

		Attribute attribute3otherMapping = AttributeFactory.createAttribute("decision", Ontology.NOMINAL);
		attribute3otherMapping.getMapping().mapString("yes");
		attribute3otherMapping.getMapping().mapString("no");

		ExampleSet simpleExampleSet = ExampleSets.from(attribute1otherMapping, ExampleTestTools.attributeReal(5),
				attribute3otherMapping, attribute4, attribute2).addRow(new double[] { 2, 5.1, 0, 5.6, 7 }).build();

		@SuppressWarnings("deprecation")
		ExampleSet constructorSet = new RemappedExampleSet(simpleExampleSet, mappingExampleSet, false, true);

		ExampleSet creatorSet = RemappedExampleSet.create(simpleExampleSet, mappingExampleSet, false, true);

		compare(constructorSet, creatorSet);

	}

	@Test
	public void replaceMissingExampleSet() {
		ExampleSet simpleExampleSet = ExampleSets.from(attribute1, attribute2, attribute3, attribute4)
				.addRow(new double[] { 2, Double.NaN, Double.NaN, 5.6 }).addRow(new double[] { 2, 5, 1, Double.NaN })
				.addRow(new double[] { Double.NaN, 5, 1, 5.6 }).build();

		ExampleSet constructorSet = new ReplaceMissingExampleSet(simpleExampleSet);

		ExampleSet creatorSet = ReplaceMissingExampleSet.create(simpleExampleSet, null);

		compare(constructorSet, creatorSet);

	}

	/**
	 * Compares the example sets.
	 */
	private void compare(ExampleSet constructorSet, ExampleSet creatorSet) {
		assertEquals(constructorSet.size(), creatorSet.size(), 0);
		assertEquals(constructorSet.getAttributes().size(), creatorSet.getAttributes().size(), 0);

		Iterator<Attribute> constructorIterator = constructorSet.getAttributes().iterator();
		Iterator<Attribute> creatorIterator = creatorSet.getAttributes().iterator();

		while (constructorIterator.hasNext()) {
			Attribute constructorAttribute = constructorIterator.next();
			Attribute creatorAttribute = creatorIterator.next();

			assertEquals(constructorAttribute.getName(), creatorAttribute.getName());
			assertEquals(constructorAttribute.getValueType(), creatorAttribute.getValueType());

			Iterator<Example> constructorSetIterator = constructorSet.iterator();
			Iterator<Example> creatorSetIterator = creatorSet.iterator();

			while (constructorSetIterator.hasNext()) {
				Example constructorExample = constructorSetIterator.next();
				Example creatorExample = creatorSetIterator.next();

				assertEquals(constructorExample.getValue(constructorAttribute), creatorExample.getValue(creatorAttribute),
						1e-15);
			}

		}
	}

}
