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
package com.rapidminer.operator.preprocessing;

import static org.junit.Assert.assertEquals;

import java.util.Arrays;
import java.util.Collection;
import java.util.Random;

import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.junit.runners.Parameterized;
import org.junit.runners.Parameterized.Parameters;

import com.rapidminer.RapidMiner;
import com.rapidminer.example.Attribute;
import com.rapidminer.example.ExampleSet;
import com.rapidminer.example.table.DataRow;
import com.rapidminer.example.table.DataRowFactory;
import com.rapidminer.example.table.MemoryExampleTable;
import com.rapidminer.example.table.internal.ColumnarExampleTable;
import com.rapidminer.example.test.ExampleTestTools;
import com.rapidminer.test_utils.RapidAssert;
import com.rapidminer.tools.Ontology;
import com.rapidminer.tools.ParameterService;


/**
 * Tests the materialize data functionality.
 *
 * @author Marcel Michel
 */
@RunWith(value = Parameterized.class)
public class MaterializeDataInMemoryTest {

	public MaterializeDataInMemoryTest(boolean legacyMode) {
		ParameterService.setParameterValue(RapidMiner.PROPERTY_RAPIDMINER_SYSTEM_LEGACY_DATA_MGMT,
				String.valueOf(legacyMode));
	}

	@Parameters(name = "legacyMode={0}")
	public static Collection<Object> params() {
		return Arrays.asList(true, false);
	}

	@Before
	public void setup() {
		RapidMiner.initAsserters();
	}

	/**
	 * Creates random example sets and materializes them using the
	 * {@link MaterializeDataInMemory#materializeExampleSet(ExampleSet)} method. The test will
	 * compare the data management types and the actual table values of the source and result sets.
	 */
	@Test
	public void dataMangementTest() {
		for (int i = DataRowFactory.FIRST_TYPE_INDEX; i <= DataRowFactory.LAST_TYPE_INDEX; i++) {
			ExampleSet sourceSet = createMemoryExampleTable(100, i).createExampleSet();
			ExampleSet materializedSet = MaterializeDataInMemory.materializeExampleSet(sourceSet);
			RapidAssert.assertEquals("ExampleSets are not equal", sourceSet, materializedSet);
			if (materializedSet.getExampleTable() instanceof ColumnarExampleTable) {
				// when a ColumnarExampleTable was created, then the data type can only be
				// DataRowFactory.TYPE_COLUMN_VIEW
				assertEquals(DataRowFactory.TYPE_COLUMN_VIEW, findDataRowType(materializedSet));
			} else if (i == DataRowFactory.TYPE_COLUMN_VIEW) {
				// for a DataRowFactory.TYPE_COLUMN_VIEW a row of type
				// DataRowFactory.TYPE_DOUBLE_ARRAY is created
				assertEquals(DataRowFactory.TYPE_DOUBLE_ARRAY, findDataRowType(materializedSet));
			} else {
				// otherwise the type should stay the same
				assertEquals(i, findDataRowType(materializedSet));
			}
		}
	}

	/**
	 * Creates a {@link MemoryExampleTable} with random values.
	 *
	 * @param size
	 *            the number of rows
	 * @param dataManagement
	 *            the data management strategy (see {@link DataRowFactory} for more information)
	 * @return the created example set as {@link MemoryExampleTable}
	 */
	private static MemoryExampleTable createMemoryExampleTable(int size, int dataManagement) {
		Attribute[] attributes = ExampleTestTools.createFourAttributes();
		MemoryExampleTable exampleTable = new MemoryExampleTable(attributes);

		DataRowFactory rowFactory = new DataRowFactory(dataManagement, '.');
		Random random = new Random(0);
		for (int i = 0; i < size; i++) {
			DataRow row = rowFactory.create(attributes.length);
			for (int j = 0; j < attributes.length; j++) {
				if (attributes[j].isNominal()) {
					row.set(attributes[j], random.nextInt(attributes[j].getMapping().getValues().size()));
				} else if (attributes[j].getValueType() == Ontology.INTEGER) {
					row.set(attributes[j], random.nextInt(200) - 100);
				} else {
					row.set(attributes[j], 20.0 * random.nextDouble() - 10.0);
				}
			}
			exampleTable.addDataRow(row);
		}
		return exampleTable;
	}

	/**
	 * This method determines the current used data row implementation.
	 *
	 * @param exampleSet
	 *            the set which should be inspected
	 * @return the data type
	 */
	private static int findDataRowType(ExampleSet exampleSet) {
		if (exampleSet.size() > 0) {
			// then determine current representation: get first row
			DataRow usedRow = exampleSet.getExample(0).getDataRow();
			if (usedRow != null) {
				return usedRow.getType();
			}
		}
		// default type
		return DataRowFactory.TYPE_DOUBLE_ARRAY;
	}
}
