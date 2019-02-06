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
package com.rapidminer.example.utils;

import static org.junit.Assert.assertEquals;

import java.util.Arrays;
import java.util.Collection;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

import org.junit.Test;
import org.junit.runner.RunWith;
import org.junit.runners.Parameterized;
import org.junit.runners.Parameterized.Parameters;

import com.rapidminer.RapidMiner;
import com.rapidminer.example.Attribute;
import com.rapidminer.example.Attributes;
import com.rapidminer.example.ExampleSet;
import com.rapidminer.example.table.DataRow;
import com.rapidminer.example.table.DataRowFactory;
import com.rapidminer.example.table.DataRowReader;
import com.rapidminer.example.table.DoubleArrayDataRow;
import com.rapidminer.example.table.GrowingExampleTable;
import com.rapidminer.example.table.IntArrayDataRow;
import com.rapidminer.example.test.ExampleTestTools;
import com.rapidminer.example.utils.ExampleSetBuilder.DataManagement;
import com.rapidminer.tools.ParameterService;


/**
 * Tests the {@link ExampleSet} creation with the builders from {@link ExampleSets}.
 *
 * @author Gisa Schaefer
 *
 */
@RunWith(value = Parameterized.class)
public class ExampleSetsTest {

	public ExampleSetsTest(boolean legacyMode) {
		ParameterService.setParameterValue(RapidMiner.PROPERTY_RAPIDMINER_SYSTEM_LEGACY_DATA_MGMT,
				String.valueOf(legacyMode));
	}

	@Parameters(name = "legacyMode={0}")
	public static Collection<Object> params() {
		return Arrays.asList(true, false);
	}

	@Test
	public void createNoRowsTest() {
		ExampleSet testSet = ExampleSets.from(ExampleTestTools.attributeInt()).build();

		assertEquals(1, testSet.getAttributes().allSize());
		assertEquals(0, testSet.size());
	}

	@Test
	public void createNoRowsTwoAttributesTest() {
		ExampleSet testSet = ExampleSets.from(ExampleTestTools.attributeInt(), ExampleTestTools.attributeReal()).build();

		assertEquals(2, testSet.getAttributes().allSize());
		assertEquals(0, testSet.size());
	}

	@Test
	public void createSpecialAttributesTest() {
		Attribute weight = ExampleTestTools.attributeReal();
		Attribute label = ExampleTestTools.attributeYesNo();

		ExampleSet testSet = ExampleSets.from(weight, ExampleTestTools.attributeInt(), label)//
				.withRole(label, Attributes.LABEL_NAME)//
				.withRole(weight, Attributes.WEIGHT_NAME)//
				.build();

		assertEquals(3, testSet.getAttributes().allSize());
		assertEquals(1, testSet.getAttributes().size());
		assertEquals(label, testSet.getAttributes().getLabel());
		assertEquals(weight, testSet.getAttributes().getWeight());
		assertEquals(0, testSet.size());
	}

	@Test
	public void createSpecialAttributesMapTest() {
		List<Attribute> attributes = Arrays.asList(ExampleTestTools.createFourAttributes());
		Map<Attribute, String> specialAttributes = new LinkedHashMap<>();
		for (Attribute attribute : attributes) {
			specialAttributes.put(attribute, attribute.getName());
		}

		ExampleSet testSet = ExampleSets.from(attributes)//
				.withRoles(specialAttributes)//
				.build();

		assertEquals(4, testSet.getAttributes().allSize());
		assertEquals(0, testSet.getAttributes().size());
		assertEquals(0, testSet.size());
	}

	@Test
	public void createTwoRowsTest() {
		Attribute attribute = ExampleTestTools.attributeInt();

		ExampleSet testSet = ExampleSets.from(attribute)//
				.withBlankSize(2)//
				.build();

		assertEquals(1, testSet.getAttributes().allSize());
		assertEquals(2, testSet.size());
		assertEquals(0, testSet.getExample(0).getValue(attribute), 0);
		assertEquals(0, testSet.getExample(1).getValue(attribute), 0);
	}

	@Test
	public void createSpeedOptimizationHintTest() {
		Attribute attribute = ExampleTestTools.attributeInt();

		ExampleSet testSet = ExampleSets.from(attribute)//
				.withOptimizationHint(DataManagement.SPEED_OPTIMIZED)//
				.withBlankSize(2)//
				.build();

		assertEquals(1, testSet.getAttributes().allSize());
		assertEquals(2, testSet.size());
		assertEquals(0, testSet.getExample(0).getValue(attribute), 0);
		assertEquals(0, testSet.getExample(1).getValue(attribute), 0);
	}

	@Test
	public void creatememoryOptimizationHintTest() {
		Attribute attribute = ExampleTestTools.attributeInt();

		ExampleSet testSet = ExampleSets.from(attribute)//
				.withOptimizationHint(DataManagement.MEMORY_OPTIMIZED)//
				.withBlankSize(2)//
				.build();

		assertEquals(1, testSet.getAttributes().allSize());
		assertEquals(2, testSet.size());
		assertEquals(0, testSet.getExample(0).getValue(attribute), 0);
		assertEquals(0, testSet.getExample(1).getValue(attribute), 0);
	}

	@Test
	public void createSpeedOptimizationHintAndRowsTest() {
		Attribute attribute1 = ExampleTestTools.attributeInt();
		Attribute attribute2 = ExampleTestTools.attributeReal();

		ExampleSet testSet = ExampleSets.from(attribute1, attribute2)//
				.withOptimizationHint(DataManagement.SPEED_OPTIMIZED)//
				.addRow(new double[] { 1, 2.5 })//
				.addRow(new double[] { 7, 11.5 })//
				.build();

		assertEquals(2, testSet.getAttributes().allSize());
		assertEquals(2, testSet.size());
		assertEquals(1, testSet.getExample(0).getValue(attribute1), 0);
		assertEquals(7, testSet.getExample(1).getValue(attribute1), 0);
		assertEquals(2.5, testSet.getExample(0).getValue(attribute2), 1.0e-12);
		assertEquals(11.5, testSet.getExample(1).getValue(attribute2), 1.0e-12);
	}

	@Test
	public void createMemoryOptimizationHintAndRowsTest() {
		Attribute attribute1 = ExampleTestTools.attributeInt();
		Attribute attribute2 = ExampleTestTools.attributeReal();

		ExampleSet testSet = ExampleSets.from(attribute1, attribute2)//
				.withOptimizationHint(DataManagement.MEMORY_OPTIMIZED)//
				.addRow(new double[] { 1, 2.5 })//
				.addRow(new double[] { 7, 11.5 })//
				.build();

		assertEquals(2, testSet.getAttributes().allSize());
		assertEquals(2, testSet.size());
		assertEquals(1, testSet.getExample(0).getValue(attribute1), 0);
		assertEquals(7, testSet.getExample(1).getValue(attribute1), 0);
		assertEquals(2.5, testSet.getExample(0).getValue(attribute2), 1.0e-12);
		assertEquals(11.5, testSet.getExample(1).getValue(attribute2), 1.0e-12);
	}

	@Test
	public void expectedSizeTestTest() {
		Attribute attribute = ExampleTestTools.attributeInt();

		ExampleSet testSet = ExampleSets.from(attribute)//
				.withExpectedSize(10)//
				.build();

		assertEquals(0, testSet.size());
	}

	@Test
	public void createFilledRowsTest() {
		Attribute attribute1 = ExampleTestTools.attributeInt();
		Attribute attribute2 = ExampleTestTools.attributeReal();

		ExampleSet testSet = ExampleSets.from(attribute1, attribute2)//
				.addRow(new double[] { 1, 2.5 })//
				.addRow(new double[] { 7, 11.5 })//
				.build();

		assertEquals(2, testSet.getAttributes().allSize());
		assertEquals(2, testSet.size());
		assertEquals(1, testSet.getExample(0).getValue(attribute1), 0);
		assertEquals(7, testSet.getExample(1).getValue(attribute1), 0);
		assertEquals(2.5, testSet.getExample(0).getValue(attribute2), 1.0e-12);
		assertEquals(11.5, testSet.getExample(1).getValue(attribute2), 1.0e-12);
	}

	@Test
	public void createFilledDataRowsTest() {
		Attribute attribute1 = ExampleTestTools.attributeInt();
		Attribute attribute2 = ExampleTestTools.attributeDogCatMouse();
		DataRow dataRow1 = new IntArrayDataRow(new int[] { 1, attribute2.getMapping().getIndex("cat") });
		DataRow dataRow2 = new IntArrayDataRow(new int[] { 7, attribute2.getMapping().getIndex("dog") });

		ExampleSet testSet = ExampleSets.from(attribute1, attribute2)//
				.addDataRow(dataRow1)//
				.addDataRow(dataRow2)//
				.build();

		assertEquals(2, testSet.getAttributes().allSize());
		assertEquals(2, testSet.size());
		assertEquals(1, testSet.getExample(0).getValue(attribute1), 0);
		assertEquals(7, testSet.getExample(1).getValue(attribute1), 0);
		assertEquals("cat", testSet.getExample(0).getNominalValue(attribute2));
		assertEquals("dog", testSet.getExample(1).getNominalValue(attribute2));
	}

	@Test
	public void createDataRowsFromAttributesTest() {
		Attribute attribute1 = ExampleTestTools.attributeInt();
		Attribute attribute2 = ExampleTestTools.attributeReal();

		ExampleSetBuilder builder = ExampleSets.from(attribute1, attribute2)//
				.withExpectedSize(3);

		DataRowFactory factory = new DataRowFactory(DataRowFactory.TYPE_SHORT_ARRAY,
				DataRowFactory.POINT_AS_DECIMAL_CHARACTER);
		for (int i = 0; i < 3; i++) {
			DataRow row = factory.create(2);
			row.set(attribute1, 1);
			row.set(attribute2, 2);

			builder.addDataRow(row);
		}

		ExampleSet testSet = builder.build();

		assertEquals(2, testSet.getAttributes().allSize());
		assertEquals(3, testSet.size());
		assertEquals(1, testSet.getExample(0).getValue(attribute1), 0);
		assertEquals(1, testSet.getExample(2).getValue(attribute1), 0);
		assertEquals(2, testSet.getExample(0).getValue(attribute2), 0);
		assertEquals(2, testSet.getExample(2).getValue(attribute2), 0);
	}

	@Test
	public void createDataRowReaderTest() {
		Attribute attribute1 = ExampleTestTools.attributeInt();
		Attribute attribute2 = ExampleTestTools.attributeReal();
		DataRowReader reader = ExampleTestTools.createDataRowReader(new double[][] { { 1, 2.5 }, { 7, 11.5 } });

		ExampleSet testSet = ExampleSets.from(attribute1, attribute2)//
				.withDataRowReader(reader)//
				.build();

		assertEquals(2, testSet.getAttributes().allSize());
		assertEquals(2, testSet.size());
		assertEquals(1, testSet.getExample(0).getValue(attribute1), 0);
		assertEquals(7, testSet.getExample(1).getValue(attribute1), 0);
		assertEquals(2.5, testSet.getExample(0).getValue(attribute2), 1.0e-12);
		assertEquals(11.5, testSet.getExample(1).getValue(attribute2), 1.0e-12);
	}

	@Test
	public void createfillColumnsTest() {
		Attribute attribute1 = ExampleTestTools.attributeInt();
		Attribute attribute2 = ExampleTestTools.attributeReal();

		ExampleSet testSet = ExampleSets.from(attribute1, attribute2)//
				.withBlankSize(10)//
				.withColumnFiller(attribute1, i -> 42)//
				.withColumnFiller(attribute2, i -> 2 * i)//
				.build();

		assertEquals(2, testSet.getAttributes().allSize());
		assertEquals(10, testSet.size());
		assertEquals(42, testSet.getExample(0).getValue(attribute1), 0);
		assertEquals(42, testSet.getExample(9).getValue(attribute1), 0);
		assertEquals(0, testSet.getExample(0).getValue(attribute2), 0);
		assertEquals(2 * 9, testSet.getExample(9).getValue(attribute2), 0);
	}

	@Test
	public void createfillColumnsAndOptimizationTest() {
		Attribute attribute1 = ExampleTestTools.attributeInt();
		Attribute attribute2 = ExampleTestTools.attributeReal();

		ExampleSet testSet = ExampleSets.from(attribute1, attribute2)//
				.withBlankSize(10)//
				.withColumnFiller(attribute1, i -> 42)//
				.withOptimizationHint(DataManagement.MEMORY_OPTIMIZED)//
				.withColumnFiller(attribute2, i -> 2 * i)//
				.build();

		assertEquals(2, testSet.getAttributes().allSize());
		assertEquals(10, testSet.size());
		assertEquals(42, testSet.getExample(0).getValue(attribute1), 0);
		assertEquals(42, testSet.getExample(9).getValue(attribute1), 0);
		assertEquals(0, testSet.getExample(0).getValue(attribute2), 0);
		assertEquals(2 * 9, testSet.getExample(9).getValue(attribute2), 0);
	}

	@Test
	public void createfillColumnTest() {
		Attribute attribute1 = ExampleTestTools.attributeInt();
		Attribute attribute2 = ExampleTestTools.attributeReal();

		ExampleSet testSet = ExampleSets.from(attribute1, attribute2)//
				.withBlankSize(10)//
				.withColumnFiller(attribute1, i -> 42)//
				.build();

		assertEquals(2, testSet.getAttributes().allSize());
		assertEquals(10, testSet.size());
		assertEquals(42, testSet.getExample(0).getValue(attribute1), 0);
		assertEquals(42, testSet.getExample(9).getValue(attribute1), 0);
		assertEquals(0, testSet.getExample(0).getValue(attribute2), 0);
		assertEquals(0, testSet.getExample(9).getValue(attribute2), 0);
	}

	@Test
	public void createWithEverythingOrderCheck() {
		Attribute attribute1 = ExampleTestTools.attributeInt();
		Attribute attribute2 = ExampleTestTools.attributeReal();
		DataRowReader reader1 = ExampleTestTools.createDataRowReader(new double[][] { { 1, 1.5 }, { 2, 2.5 } });
		DataRowReader reader2 = ExampleTestTools.createDataRowReader(new double[][] { { 1, 1.5 }, { 2, 2.5 } });

		ExampleSet testSet1 = ExampleSets.from(attribute1, attribute2)//
				.withColumnFiller(attribute1, i -> 5)//
				.withBlankSize(1)//
				.addDataRow(new DoubleArrayDataRow(new double[] { 3, 3.5 }))//
				.withDataRowReader(reader1)//
				.addRow(new double[] { 4, 4.5 })//
				.build();

		ExampleSet testSet2 = ExampleSets.from(attribute1, attribute2)//
				.addDataRow(new DoubleArrayDataRow(new double[] { 3, 3.5 }))//
				.addRow(new double[] { 4, 4.5 })//
				.withDataRowReader(reader2)//
				.withBlankSize(1)//
				.withColumnFiller(attribute1, i -> 5)//
				.build();

		assertEquals(5, testSet1.size(), 1.0e-12);
		assertEquals(5, testSet2.size(), 1.0e-12);
		for (int i = 0; i < testSet1.size(); i++) {
			assertEquals(testSet1.getExample(i).getValue(attribute1), testSet2.getExample(i).getValue(attribute1), 1.0e-12);
			assertEquals(testSet1.getExample(i).getValue(attribute2), testSet2.getExample(i).getValue(attribute2), 1.0e-12);
		}
	}

	@Test
	public void growingTableTestRowSetColumnRow() {
		Attribute attribute1 = ExampleTestTools.attributeInt();
		GrowingExampleTable table = ExampleSets.createTableFrom(Collections.emptyList());
		table.addDataRow(new DoubleArrayDataRow(new double[0]));
		table.addAttribute(attribute1);
		table.getDataRow(0).set(attribute1, -1);
		table.addDataRow(new DoubleArrayDataRow(new double[] { 1 }));

		assertEquals(-1, table.getDataRow(0).get(attribute1), 0);
		assertEquals(1, table.getDataRow(1).get(attribute1), 0);
	}

	@Test
	public void growingTableTestRowColumnRow() {
		Attribute attribute1 = ExampleTestTools.attributeInt();
		GrowingExampleTable table = ExampleSets.createTableFrom(Collections.emptyList());
		table.addDataRow(new DoubleArrayDataRow(new double[0]));
		table.addAttribute(attribute1);
		table.addDataRow(new DoubleArrayDataRow(new double[] { 1 }));

		assertEquals(0, table.getDataRow(0).get(attribute1), 0);
		assertEquals(1, table.getDataRow(1).get(attribute1), 0);
	}

	@Test
	public void growingTableTestColumnRowRowColumn() {
		Attribute attribute1 = ExampleTestTools.attributeInt();
		Attribute attribute2 = ExampleTestTools.attributeReal();
		GrowingExampleTable table = ExampleSets.createTableFrom(Collections.emptyList());
		table.addAttribute(attribute1);
		table.addDataRow(new DoubleArrayDataRow(new double[] { 1 }));
		table.addDataRow(new DoubleArrayDataRow(new double[] { -1 }));

		assertEquals(1, table.getDataRow(0).get(attribute1), 0);
		assertEquals(-1, table.getDataRow(1).get(attribute1), 0);

		table.addAttribute(attribute2);

		assertEquals(1, table.getDataRow(0).get(attribute1), 0);
		assertEquals(-1, table.getDataRow(1).get(attribute1), 0);
		assertEquals(0, table.getDataRow(0).get(attribute2), 0);
		assertEquals(0, table.getDataRow(1).get(attribute2), 0);
	}

	@Test
	public void growingTableTestColumnRowColumnRow() {
		Attribute attribute1 = ExampleTestTools.attributeInt();
		Attribute attribute2 = ExampleTestTools.attributeReal();
		GrowingExampleTable table = ExampleSets.createTableFrom(Arrays.asList(new Attribute[] { attribute1 }));
		table.addDataRow(new DoubleArrayDataRow(new double[] { 5 }));
		table.addAttribute(attribute2);
		table.addDataRow(new DoubleArrayDataRow(new double[] { 6, 1 }));

		assertEquals(0, table.getDataRow(0).get(attribute2), 0);
		assertEquals(1, table.getDataRow(1).get(attribute2), 0);
		assertEquals(5, table.getDataRow(0).get(attribute1), 0);
		assertEquals(6, table.getDataRow(1).get(attribute1), 0);
	}

	@Test
	public void castingTestOneAttributeAndBlankSize() {
		Attribute attribute1 = ExampleTestTools.attributeInt();

		ExampleSet set = ExampleSets.from(attribute1).withBlankSize(3).build();
		set.getExample(1).setValue(attribute1, 3);

		GrowingExampleTable table = (GrowingExampleTable) set.getExampleTable();
		table.addDataRow(new DoubleArrayDataRow(new double[] { 7 }));

		assertEquals(0, set.getExample(0).getValue(attribute1), 0);
		assertEquals(3, set.getExample(1).getValue(attribute1), 0);
		assertEquals(0, set.getExample(2).getValue(attribute1), 0);
		assertEquals(7, set.getExample(3).getValue(attribute1), 0);
	}

	@Test
	public void castingTestOneAttributeAndAddSecond() {
		Attribute attribute1 = ExampleTestTools.attributeInt();
		Attribute attribute2 = ExampleTestTools.attributeReal();

		ExampleSet set = ExampleSets.from(attribute1).withBlankSize(2).withColumnFiller(attribute1, i -> i + 1).build();

		GrowingExampleTable table = (GrowingExampleTable) set.getExampleTable();
		table.addDataRow(new DoubleArrayDataRow(new double[] { 3 }));

		table.addAttribute(attribute2);
		table.addDataRow(new DoubleArrayDataRow(new double[] { 4, -1 }));

		assertEquals(1, set.getExample(0).getValue(attribute1), 0);
		assertEquals(2, set.getExample(1).getValue(attribute1), 0);
		assertEquals(3, set.getExample(2).getValue(attribute1), 0);
		assertEquals(4, set.getExample(3).getValue(attribute1), 0);
		assertEquals(0, set.getExample(0).getValue(attribute2), 0);
		assertEquals(0, set.getExample(1).getValue(attribute2), 0);
		assertEquals(0, set.getExample(2).getValue(attribute2), 0);
		assertEquals(-1, set.getExample(3).getValue(attribute2), 0);
	}

}
