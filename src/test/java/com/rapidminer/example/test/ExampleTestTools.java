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
package com.rapidminer.example.test;

import java.util.Arrays;
import java.util.LinkedList;
import java.util.List;
import java.util.Random;

import com.rapidminer.example.Attribute;
import com.rapidminer.example.Attributes;
import com.rapidminer.example.ExampleSet;
import com.rapidminer.example.table.AttributeFactory;
import com.rapidminer.example.table.DataRow;
import com.rapidminer.example.table.DataRowFactory;
import com.rapidminer.example.table.DataRowReader;
import com.rapidminer.example.table.DoubleArrayDataRow;
import com.rapidminer.example.table.ListDataRowReader;
import com.rapidminer.example.table.MemoryExampleTable;
import com.rapidminer.tools.Ontology;


/**
 * Provides factory methods for text fixtures.
 * 
 * @author Simon Fischer, Ingo Mierswa
 */
public class ExampleTestTools {

	/** Returns a DataRowReader returning the given values. */
	public static DataRowReader createDataRowReader(DataRowFactory factory, Attribute[] attributes, String[][] values) {
		List<DataRow> dataRows = new LinkedList<DataRow>();
		for (int i = 0; i < values.length; i++) {
			dataRows.add(factory.create(values[i], attributes));
		}
		return new ListDataRowReader(dataRows.iterator());
	}

	/** Returns a DataRowReader returning the given values. */
	public static DataRowReader createDataRowReader(double[][] values) {
		List<DataRow> dataRows = new LinkedList<DataRow>();
		for (int i = 0; i < values.length; i++) {
			dataRows.add(new DoubleArrayDataRow(values[i]));
		}
		return new ListDataRowReader(dataRows.iterator());
	}

	/**
	 * Returns a DataRowReader returning random values (generated with fixed
	 * random seed).
	 */
	public static DataRowReader createDataRowReader(int size, Attribute[] attributes) {
		Random random = new Random(0);
		List<DataRow> dataRows = new LinkedList<DataRow>();
		for (int i = 0; i < size; i++) {
			double[] data = new double[attributes.length];
			for (int j = 0; j < data.length; j++) {
				if (attributes[j].isNominal()) {
					data[j] = random.nextInt(attributes[j].getMapping().getValues().size());
				}
				if (attributes[j].getValueType() == Ontology.INTEGER) {
					data[j] = random.nextInt(200) - 100;
				} else {
					data[j] = 20.0 * random.nextDouble() - 10.0;
				}
			}
			dataRows.add(new DoubleArrayDataRow(data));
		}
		return new ListDataRowReader(dataRows.iterator());
	}

	public static MemoryExampleTable createMemoryExampleTable(int size) {
		Attribute[] attributes = createFourAttributes();
		return new MemoryExampleTable(Arrays.asList(attributes), createDataRowReader(size, attributes));
	}

	public static Attribute attributeDogCatMouse() {
		Attribute a = AttributeFactory.createAttribute("animal", Ontology.NOMINAL);
		a.getMapping().mapString("dog");
		a.getMapping().mapString("cat");
		a.getMapping().mapString("mouse");
		return a;
	}

	public static Attribute attributeYesNo() {
		Attribute a = AttributeFactory.createAttribute("decision", Ontology.NOMINAL);
		a.getMapping().mapString("no");
		a.getMapping().mapString("yes");
		return a;
	}

	public static Attribute attributeInt() {
		Attribute a = AttributeFactory.createAttribute("integer", Ontology.INTEGER);
		return a;
	}

	public static Attribute attributeReal() {
		Attribute a = AttributeFactory.createAttribute("real", Ontology.REAL);
		return a;
	}

	public static Attribute attributeReal(int index) {
		Attribute a = AttributeFactory.createAttribute("real" + index, Ontology.REAL);
		return a;
	}

	/**
	 * Creates four attributes: "animal" (dog/cat/mouse), "decision" (yes/no),
	 * "int", and "real".
	 */
	public static Attribute[] createFourAttributes() {
		Attribute[] attributes = new Attribute[4];
		attributes[0] = ExampleTestTools.attributeDogCatMouse();
		attributes[1] = ExampleTestTools.attributeYesNo();
		attributes[2] = ExampleTestTools.attributeInt();
		attributes[3] = ExampleTestTools.attributeReal();
		for (int i = 0; i < attributes.length; i++)
			attributes[i].setTableIndex(i);
		return attributes;
	}

	public static Attribute createPredictedLabel(ExampleSet exampleSet) {
		Attribute predictedLabel = AttributeFactory.createAttribute(exampleSet.getAttributes().getLabel(), Attributes.PREDICTION_NAME);
		exampleSet.getExampleTable().addAttribute(predictedLabel);
		exampleSet.getAttributes().setPredictedLabel(predictedLabel);
		return predictedLabel;
	}
}
