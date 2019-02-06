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

import static org.junit.Assert.assertEquals;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

import org.junit.Test;

import com.rapidminer.example.Attribute;
import com.rapidminer.example.table.AttributeFactory;
import com.rapidminer.example.table.DataRow;
import com.rapidminer.example.table.ExampleTable;
import com.rapidminer.example.table.IntArrayDataRow;
import com.rapidminer.example.test.ExampleTestTools;
import com.rapidminer.example.utils.ExampleSetBuilder.DataManagement;
import com.rapidminer.tools.Ontology;


/**
 * Tests for the {@link ColumnarExampleTable}.
 *
 * @author Gisa Schaefer
 * @since 7.3
 */
public class ColumnarExampleTableTest {

	@Test
	public void createNoRowsTest() {
		ExampleTable table = new ColumnarExampleTable(Arrays.asList(ExampleTestTools.attributeInt()));

		assertEquals(1, table.getAttributeCount());
		assertEquals(0, table.size());
	}

	@Test
	public void createNoRowsTwoAttributesRemoveTest() {
		ExampleTable table = new ColumnarExampleTable(
				Arrays.asList(ExampleTestTools.attributeInt(), ExampleTestTools.attributeReal()));
		table.removeAttribute(1);

		assertEquals(1, table.getAttributeCount());
		assertEquals(0, table.size());
	}

	@Test
	public void createTwoRowsTest() {
		Attribute attribute = ExampleTestTools.attributeInt();

		ColumnarExampleTable table = new ColumnarExampleTable(Arrays.asList(attribute));
		table.addBlankRows(2);

		assertEquals(1, table.getAttributeCount());
		assertEquals(2, table.size());
		assertEquals(0, table.getDataRow(0).get(attribute), 0);
		assertEquals(0, table.getDataRow(0).get(attribute), 0);
	}

	@Test
	public void createManyExpectedTest() {
		ColumnarExampleTable table = new ColumnarExampleTable(Arrays.asList(ExampleTestTools.attributeInt()));
		table.setExpectedSize(2 * AutoColumnUtils.CHUNK_SIZE + 1);
	}

	@Test
	public void createManyExpectedTestCompletable() {
		ColumnarExampleTable table = new ColumnarExampleTable(Arrays.asList(ExampleTestTools.attributeInt()),
				DataManagement.AUTO, true);
		table.setExpectedSize(2 * AutoColumnUtils.CHUNK_SIZE + 1);
		table.complete();
	}

	@Test
	public void createManyExpectedTestNom() {
		ColumnarExampleTable table = new ColumnarExampleTable(Arrays.asList(ExampleTestTools.attributeDogCatMouse()));
		table.setExpectedSize(2 * AutoColumnUtils.CHUNK_SIZE + 1);
	}

	@Test
	public void createManyExpectedTestNomCompletable() {
		ColumnarExampleTable table = new ColumnarExampleTable(Arrays.asList(ExampleTestTools.attributeDogCatMouse()),
				DataManagement.AUTO, true);
		table.setExpectedSize(2 * AutoColumnUtils.CHUNK_SIZE + 1);
		table.complete();
	}

	@Test
	public void createManyExpectedTestBinom() {
		Attribute a = AttributeFactory.createAttribute(Ontology.BINOMINAL);
		ColumnarExampleTable table = new ColumnarExampleTable(Arrays.asList(a));
		table.setExpectedSize(2 * AutoColumnUtils.CHUNK_SIZE + 1);
	}

	@Test
	public void createManyExpectedTestBinomCompletable() {
		Attribute a = AttributeFactory.createAttribute(Ontology.BINOMINAL);
		ColumnarExampleTable table = new ColumnarExampleTable(Arrays.asList(a), DataManagement.AUTO, true);
		table.setExpectedSize(2 * AutoColumnUtils.CHUNK_SIZE + 1);
		table.complete();
	}

	@Test
	public void createFilledRowsTest() {
		Attribute attribute1 = ExampleTestTools.attributeInt();
		Attribute attribute2 = ExampleTestTools.attributeReal();

		ColumnarExampleTable table = new ColumnarExampleTable(Arrays.asList(attribute1, attribute2));
		table.addRow(new double[] { 1, 2.5 });
		table.addRow(new double[] { 7, 11.5 });

		assertEquals(2, table.getAttributeCount());
		assertEquals(2, table.size());
		assertEquals(1, table.getDataRow(0).get(attribute1), 0);
		assertEquals(7, table.getDataRow(1).get(attribute1), 0);
		assertEquals(2.5, table.getDataRow(0).get(attribute2), 1.0e-12);
		assertEquals(11.5, table.getDataRow(1).get(attribute2), 1.0e-12);
	}

	@Test
	public void removeAttributeTest() {
		Attribute attribute1 = ExampleTestTools.attributeInt();
		Attribute attribute2 = ExampleTestTools.attributeReal();

		ColumnarExampleTable table = new ColumnarExampleTable(Arrays.asList(attribute1, attribute2));
		table.addRow(new double[] { 1, 2.5 });
		table.addRow(new double[] { 7, 11.5 });

		table.removeAttribute(attribute1);

		assertEquals(1, table.getAttributeCount());
		assertEquals(2, table.size());
		table.getDataRow(0).get(attribute1);
		assertEquals(Double.NaN, table.getDataRow(0).get(attribute1), 0);
		assertEquals(Double.NaN, table.getDataRow(1).get(attribute1), 0);
		assertEquals(2.5, table.getDataRow(0).get(attribute2), 1.0e-12);
		assertEquals(11.5, table.getDataRow(1).get(attribute2), 1.0e-12);
	}

	@Test
	public void addAttributesTest() {
		Attribute attribute1 = ExampleTestTools.attributeInt();
		Attribute attribute2 = ExampleTestTools.attributeReal();
		Attribute attribute3 = ExampleTestTools.attributeYesNo();

		ColumnarExampleTable table = new ColumnarExampleTable(Arrays.asList(attribute1));
		table.addRow(new double[] { 1 });
		table.addRow(new double[] { 7 });

		table.addAttributes(Arrays.asList(attribute2, attribute3));
		table.getDataRow(1).set(attribute3, 10);
		table.getDataRow(0).set(attribute2, 11);

		assertEquals(3, table.getAttributeCount());
		assertEquals(2, table.size());
		assertEquals(0, table.getDataRow(0).get(attribute3), 0);
		assertEquals(10, table.getDataRow(1).get(attribute3), 0);
		assertEquals(11, table.getDataRow(0).get(attribute2), 0);
		assertEquals(0, table.getDataRow(1).get(attribute2), 0);
		assertEquals(1, table.getDataRow(0).get(attribute1), 0);
		assertEquals(7, table.getDataRow(1).get(attribute1), 0);
	}

	@Test
	public void removeAndAddAttributeTest() {
		Attribute attribute1 = ExampleTestTools.attributeInt();
		Attribute attribute2 = ExampleTestTools.attributeReal();
		Attribute attribute3 = ExampleTestTools.attributeYesNo();

		ColumnarExampleTable table = new ColumnarExampleTable(Arrays.asList(attribute1, attribute2));
		table.addRow(new double[] { 1, 2.5 });
		table.addRow(new double[] { 7, 11.5 });

		table.removeAttribute(attribute1);
		table.addAttribute(attribute3);
		table.getDataRow(1).set(attribute3, 10);

		assertEquals(2, table.getAttributeCount());
		assertEquals(2, table.size());
		assertEquals(0, table.getDataRow(0).get(attribute3), 0);
		assertEquals(10, table.getDataRow(1).get(attribute3), 0);
		assertEquals(2.5, table.getDataRow(0).get(attribute2), 1.0e-12);
		assertEquals(11.5, table.getDataRow(1).get(attribute2), 1.0e-12);
	}

	@Test
	public void addDataRowsTest() {
		Attribute attribute1 = ExampleTestTools.attributeInt();
		Attribute attribute2 = ExampleTestTools.attributeDogCatMouse();
		DataRow dataRow1 = new IntArrayDataRow(new int[] { 1, attribute2.getMapping().getIndex("cat") });
		DataRow dataRow2 = new IntArrayDataRow(new int[] { 7, attribute2.getMapping().getIndex("dog") });

		ColumnarExampleTable table = new ColumnarExampleTable(Arrays.asList(attribute1, attribute2));

		table.addDataRow(dataRow1);
		table.addDataRow(dataRow2);

		assertEquals(2, table.getAttributeCount());
		assertEquals(2, table.size());
		assertEquals(1, table.getDataRow(0).get(attribute1), 0);
		assertEquals(7, table.getDataRow(1).get(attribute1), 0);
		assertEquals("cat", attribute2.getMapping().mapIndex((int) table.getDataRow(0).get(attribute2)));
		assertEquals("dog", attribute2.getMapping().mapIndex((int) table.getDataRow(1).get(attribute2)));
	}

	@Test
	public void addDataRowsExpectedSizeAfterTest() {
		Attribute attribute1 = ExampleTestTools.attributeInt();
		Attribute attribute2 = ExampleTestTools.attributeDogCatMouse();
		DataRow dataRow1 = new IntArrayDataRow(new int[] { 1, attribute2.getMapping().getIndex("cat") });
		DataRow dataRow2 = new IntArrayDataRow(new int[] { 7, attribute2.getMapping().getIndex("dog") });

		ColumnarExampleTable table = new ColumnarExampleTable(Arrays.asList(attribute1, attribute2));

		table.addDataRow(dataRow1);
		table.addDataRow(dataRow2);
		table.setExpectedSize(100);

		assertEquals(2, table.getAttributeCount());
		assertEquals(2, table.size());
		assertEquals(1, table.getDataRow(0).get(attribute1), 0);
		assertEquals(7, table.getDataRow(1).get(attribute1), 0);
		assertEquals("cat", attribute2.getMapping().mapIndex((int) table.getDataRow(0).get(attribute2)));
		assertEquals("dog", attribute2.getMapping().mapIndex((int) table.getDataRow(1).get(attribute2)));
	}

	@Test
	public void createfillColumnsTest() {
		Attribute attribute1 = ExampleTestTools.attributeInt();
		Attribute attribute2 = ExampleTestTools.attributeReal();

		ColumnarExampleTable table = new ColumnarExampleTable(Arrays.asList(attribute1, attribute2));
		table.addBlankRows(10);
		table.fillColumn(attribute1, i -> 42);
		table.fillColumn(attribute2, i -> 2 * i);

		assertEquals(2, table.getAttributeCount());
		assertEquals(10, table.size());
		assertEquals(42, table.getDataRow(0).get(attribute1), 0);
		assertEquals(42, table.getDataRow(9).get(attribute1), 0);
		assertEquals(0, table.getDataRow(0).get(attribute2), 0);
		assertEquals(2 * 9, table.getDataRow(9).get(attribute2), 0);
	}

	@Test
	public void createfillColumnTest() {
		Attribute attribute1 = ExampleTestTools.attributeInt();
		Attribute attribute2 = ExampleTestTools.attributeReal();

		ColumnarExampleTable table = new ColumnarExampleTable(Arrays.asList(attribute1, attribute2));
		table.setExpectedSize(10);
		table.addBlankRows(10);
		table.fillColumn(attribute1, i -> 42);

		assertEquals(2, table.getAttributeCount());
		assertEquals(10, table.size());
		assertEquals(42, table.getDataRow(0).get(attribute1), 0);
		assertEquals(42, table.getDataRow(9).get(attribute1), 0);
		assertEquals(0, table.getDataRow(0).get(attribute2), 0);
		assertEquals(0, table.getDataRow(9).get(attribute2), 0);
	}

	@Test(expected = ArrayIndexOutOfBoundsException.class)
	public void accessUnknownAttributeTest() {
		Attribute attribute1 = ExampleTestTools.attributeInt();
		Attribute attribute2 = ExampleTestTools.attributeReal();
		Attribute attribute3 = ExampleTestTools.attributeYesNo();

		ColumnarExampleTable table = new ColumnarExampleTable(Arrays.asList(attribute1, attribute2));
		table.addRow(new double[] { 1, 2.5 });

		table.getDataRow(0).get(attribute3);
	}

	@Test(expected = ArrayIndexOutOfBoundsException.class)
	public void accessUnknownAttribute2Test() {
		Attribute attribute1 = ExampleTestTools.attributeInt();
		Attribute attribute2 = ExampleTestTools.attributeReal();
		Attribute attribute3 = ExampleTestTools.attributeYesNo();

		ColumnarExampleTable table = new ColumnarExampleTable(Arrays.asList(attribute1, attribute2));
		table.addRow(new double[] { 1, 2.5 });

		attribute3.setTableIndex(100);

		table.getDataRow(0).get(attribute3);
	}

	@Test
	public void accessNullColumnTest() {
		List<Attribute> atts = new ArrayList<>(8);
		for (int i = 0; i < 8; i++) {
			atts.add(ExampleTestTools.attributeInt());
		}
		ColumnarExampleTable table = new ColumnarExampleTable(atts);
		table.addAttribute(ExampleTestTools.attributeReal());
		table.addRow(new double[table.getNumberOfAttributes()]);
	}

	@Test
	public void binominalColumnTest() {
		Attribute attribute = AttributeFactory.createAttribute(Ontology.BINOMINAL);
		ColumnarExampleTable table = new ColumnarExampleTable(Arrays.asList(attribute));
		table.addBlankRows(11);
		table.fillColumn(attribute, i -> attribute.getMapping().mapString(String.valueOf(i % 3 == 0)));
		table.getDataRow(10).set(attribute, Double.NaN);

		for (int i = 0; i < 10; i++) {
			assertEquals(i % 3 == 0 ? 0 : 1, table.getDataRow(i).get(attribute), 0);
		}
		assertEquals(true, Double.isNaN(table.getDataRow(10).get(attribute)));
	}

}
