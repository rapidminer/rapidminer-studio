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


import org.junit.After;
import org.junit.Before;
import org.junit.Test;

import com.rapidminer.example.Attribute;
import com.rapidminer.example.table.DataRow;
import com.rapidminer.example.table.DataRowFactory;
import com.rapidminer.test_utils.RapidAssert;


/**
 * Tests creation and parsing of sparse and dense data rows, including missing
 * values.
 * 
 * @author Simon Fischer
 */
public class DataRowTest {

	private Attribute[] attributes;

	private double[] expected;

	@Before
	public void setUp() throws Exception {
		//TestUtils.initRapidMiner();
		attributes = ExampleTestTools.createFourAttributes();
		expected = new double[] { 1, Double.NaN, 5.0, 2.3 };
	}
	
	@After
	public void tearDown() throws Exception {
		attributes = null;
		expected = null;
	}

	private void assertDataRow(String message, DataRow dataRow, double[] expected) {
		for (int i = 0; i < expected.length; i++) {
			RapidAssert.assertEqualsNaN(message + " " + attributes[i].getName(), expected[i], dataRow.get(attributes[i]));
		}
	}

	private void objectTest(String message, DataRowFactory factory) {
		DataRow dataRow = factory.create(new Object[] { "cat", null, 5, 2.3d }, attributes);
		assertDataRow(message + " object", dataRow, expected);
	}

	private void stringTest(String message, DataRowFactory factory) {
		DataRow dataRow = factory.create(new String[] { "cat", "?", "5", "2.3" }, attributes);
		assertDataRow(message + " string", dataRow, expected);
	}

	@Test
	public void testDoubleArrayStrings() {
		objectTest("double_array", new DataRowFactory(DataRowFactory.TYPE_DOUBLE_ARRAY, '.'));
	}

	@Test
	public void testDoubleArrayObjects() {
		stringTest("double_array", new DataRowFactory(DataRowFactory.TYPE_DOUBLE_ARRAY, '.'));
	}

	@Test
	public void testSparseMapStrings() {
		stringTest("sparse_map", new DataRowFactory(DataRowFactory.TYPE_SPARSE_MAP, '.'));
	}

	@Test
	public void testSparseMapObjects() {
		objectTest("sparse_map", new DataRowFactory(DataRowFactory.TYPE_SPARSE_MAP, '.'));
	}
}
