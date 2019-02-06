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
package com.rapidminer.example.table;

import static org.junit.Assert.assertEquals;

import org.junit.Test;

import com.rapidminer.example.Attribute;
import com.rapidminer.tools.Ontology;


/**
 * Tests for correct behavior of {@link NominalAttribute} when setting values.
 *
 * @author Jan Czogalla
 * @since 7.4
 */
public class NominalAttributeTest {

	private static final String[] VALUES = { "value0", "value1" };
	private static final String MISSING_VALUE = "?";

	@Test
	public void setPositiveValueInRange() {
		Attribute att = AttributeFactory.createAttribute(Ontology.NOMINAL);
		att.setTableIndex(0);
		NominalMapping mapping = att.getMapping();
		mapping.mapString(VALUES[0]);
		mapping.mapString(VALUES[1]);

		double[] data = new double[1];
		DataRow row = new DoubleArrayDataRow(data);
		row.set(att, 1);

		assertEquals(1, data[0], 0);
		assertEquals(VALUES[1], att.getAsString(att.getValue(row), 0, false));
	}

	@Test
	public void setPositiveValueOutOfRange() {
		Attribute att = AttributeFactory.createAttribute(Ontology.NOMINAL);
		att.setTableIndex(0);
		NominalMapping mapping = att.getMapping();
		mapping.mapString(VALUES[0]);
		mapping.mapString(VALUES[1]);

		double[] data = new double[1];
		DataRow row = new DoubleArrayDataRow(data);
		row.set(att, 2);

		assertEquals(2, data[0], 0);
		assertEquals(MISSING_VALUE, att.getAsString(att.getValue(row), 0, false));
	}

	@Test
	public void setNegativeValue() {
		Attribute att = AttributeFactory.createAttribute(Ontology.NOMINAL);
		att.setTableIndex(0);
		NominalMapping mapping = att.getMapping();
		mapping.mapString(VALUES[0]);
		mapping.mapString(VALUES[1]);

		double[] data = new double[1];
		DataRow row = new DoubleArrayDataRow(data);
		row.set(att, -1);

		assertEquals(Double.NaN, data[0], 0);
		assertEquals(MISSING_VALUE, att.getAsString(att.getValue(row), 0, false));
	}

	@Test
	public void setMissing() {
		Attribute att = AttributeFactory.createAttribute(Ontology.NOMINAL);
		att.setTableIndex(0);
		NominalMapping mapping = att.getMapping();
		mapping.mapString(VALUES[0]);
		mapping.mapString(VALUES[1]);

		double[] data = new double[1];
		DataRow row = new DoubleArrayDataRow(data);
		row.set(att, Double.NaN);

		assertEquals(Double.NaN, data[0], 0);
		assertEquals(MISSING_VALUE, att.getAsString(att.getValue(row), 0, false));
	}

}
