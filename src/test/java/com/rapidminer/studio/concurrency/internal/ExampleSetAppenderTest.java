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
package com.rapidminer.studio.concurrency.internal;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.fail;

import java.util.List;

import org.junit.Before;
import org.junit.Test;

import com.rapidminer.example.Attribute;
import com.rapidminer.example.Attributes;
import com.rapidminer.example.ExampleSet;
import com.rapidminer.example.table.AttributeFactory;
import com.rapidminer.example.table.DataRow;
import com.rapidminer.example.table.DataRowFactory;
import com.rapidminer.example.utils.ExampleSets;
import com.rapidminer.operator.MissingIOObjectException;
import com.rapidminer.operator.OperatorException;
import com.rapidminer.operator.UserError;
import com.rapidminer.studio.concurrency.internal.util.ExampleSetAppender;
import com.rapidminer.tools.Ontology;


/**
 * Tests for the {@link ExampleSetAppender}.
 *
 * @author Marco Boeck
 *
 */
public class ExampleSetAppenderTest {

	private static final String ATT_NAME_1 = "att1";
	private static final String ATT_NAME_2 = "att2";

	private static final String MAPPING_VALUE_1 = "value1";
	private static final String MAPPING_VALUE_2 = "value2";

	private static final String ANNOTATION_KEY = "myKey";
	private static final String ANNOTATION_VALUE = "myValue";

	private Attribute nominalAttribute, polynominalAttribute, binominalAttribute, textAttribute, filepathAttribute;
	private Attribute numericalAttribute, integerAttribute, realAttribute;
	private Attribute dateAttribute, timeAttribute, datetimeAttribute;
	private Attribute twoAttAttribute, numericalWrongAttNameAttribute, binominalAttribute2, binominalAttribute3,
			mappingAttribute1_1, mappingAttribute1_2, mappingAttribute2_1, mappingAttribute2_2;

	private ExampleSet nominalSet, polynominalSet, binominalSet, textSet, filepathSet;
	private ExampleSet numericalSet, integerSet, realSet;
	private ExampleSet dateSet, timeSet, datetimeSet;
	private ExampleSet twoAttSet, numericalWrongAttNameSet, numericalLabelSet, binominalSet2, binominalSet3, mappingSet1,
			mappingSet2;

	@Before
	public void setupBeforeEachTest() {
		DataRowFactory rowFactory = new DataRowFactory(DataRowFactory.TYPE_DOUBLE_ARRAY, '.');

		// generate all attribute types
		nominalAttribute = AttributeFactory.createAttribute(ATT_NAME_1, Ontology.NOMINAL);
		nominalAttribute.setTableIndex(0);
		polynominalAttribute = AttributeFactory.createAttribute(ATT_NAME_1, Ontology.POLYNOMINAL);
		polynominalAttribute.setTableIndex(0);
		binominalAttribute = AttributeFactory.createAttribute(ATT_NAME_1, Ontology.BINOMINAL);
		binominalAttribute.setTableIndex(0);
		textAttribute = AttributeFactory.createAttribute(ATT_NAME_1, Ontology.STRING);
		textAttribute.setTableIndex(0);
		filepathAttribute = AttributeFactory.createAttribute(ATT_NAME_1, Ontology.FILE_PATH);
		filepathAttribute.setTableIndex(0);

		numericalAttribute = AttributeFactory.createAttribute(ATT_NAME_1, Ontology.NUMERICAL);
		numericalAttribute.setTableIndex(0);
		integerAttribute = AttributeFactory.createAttribute(ATT_NAME_1, Ontology.INTEGER);
		integerAttribute.setTableIndex(0);
		realAttribute = AttributeFactory.createAttribute(ATT_NAME_1, Ontology.REAL);
		realAttribute.setTableIndex(0);

		dateAttribute = AttributeFactory.createAttribute(ATT_NAME_1, Ontology.DATE);
		dateAttribute.setTableIndex(0);
		timeAttribute = AttributeFactory.createAttribute(ATT_NAME_1, Ontology.TIME);
		timeAttribute.setTableIndex(0);
		datetimeAttribute = AttributeFactory.createAttribute(ATT_NAME_1, Ontology.DATE_TIME);
		datetimeAttribute.setTableIndex(0);

		twoAttAttribute = AttributeFactory.createAttribute(ATT_NAME_2, Ontology.REAL);
		twoAttAttribute.setTableIndex(1);
		numericalWrongAttNameAttribute = AttributeFactory.createAttribute(ATT_NAME_2, Ontology.REAL);
		numericalWrongAttNameAttribute.setTableIndex(0);
		binominalAttribute2 = AttributeFactory.createAttribute(ATT_NAME_1, Ontology.BINOMINAL);
		binominalAttribute2.setTableIndex(0);
		binominalAttribute3 = AttributeFactory.createAttribute(ATT_NAME_1, Ontology.BINOMINAL);
		binominalAttribute3.setTableIndex(0);
		mappingAttribute1_1 = AttributeFactory.createAttribute(ATT_NAME_1, Ontology.POLYNOMINAL);
		mappingAttribute1_1.setTableIndex(0);
		mappingAttribute1_2 = AttributeFactory.createAttribute(ATT_NAME_2, Ontology.POLYNOMINAL);
		mappingAttribute1_2.setTableIndex(1);
		mappingAttribute2_1 = AttributeFactory.createAttribute(ATT_NAME_1, Ontology.POLYNOMINAL);
		mappingAttribute2_1.setTableIndex(0);
		mappingAttribute2_2 = AttributeFactory.createAttribute(ATT_NAME_2, Ontology.POLYNOMINAL);
		mappingAttribute2_2.setTableIndex(1);

		// generate single column data rows for all attribute types
		DataRow nominalRow = rowFactory.create(new String[] { "data - nominal" }, new Attribute[] { nominalAttribute });
		DataRow polynominalRow = rowFactory.create(new String[] { "data - polynominal" },
				new Attribute[] { polynominalAttribute });
		DataRow binominalRow = rowFactory.create(new String[] { "data - binominal" },
				new Attribute[] { binominalAttribute });
		DataRow textRow = rowFactory.create(new String[] { "data - text" }, new Attribute[] { textAttribute });
		DataRow filepathRow = rowFactory.create(new String[] { "data - filepath" }, new Attribute[] { filepathAttribute });

		DataRow numericalRow = rowFactory.create(new Double[] { 1.0d }, new Attribute[] { numericalAttribute });
		DataRow integerRow = rowFactory.create(new Double[] { 2.0d }, new Attribute[] { integerAttribute });
		DataRow realRow = rowFactory.create(new Double[] { 3.0d }, new Attribute[] { realAttribute });

		DataRow dateRow = rowFactory.create(new Double[] { 1477353600021.0d }, new Attribute[] { dateAttribute });
		DataRow timeRow = rowFactory.create(new Double[] { 1477353600021.0d }, new Attribute[] { timeAttribute });
		DataRow datetimeRow = rowFactory.create(new Double[] { 1477353600021.0d }, new Attribute[] { datetimeAttribute });

		DataRow twoAttRow = rowFactory.create(new Double[] { 42.0d, 128d },
				new Attribute[] { realAttribute, realAttribute });
		DataRow numericalWrongAttNameRow = rowFactory.create(new Double[] { 18.0d },
				new Attribute[] { numericalWrongAttNameAttribute });
		DataRow binominalRow2 = rowFactory.create(new String[] { "data - binominal2" },
				new Attribute[] { binominalAttribute2 });
		DataRow binominalRow3 = rowFactory.create(new String[] { "data - binominal3" },
				new Attribute[] { binominalAttribute3 });
		DataRow mappingRow1 = rowFactory.create(new String[] { MAPPING_VALUE_1, MAPPING_VALUE_2 },
				new Attribute[] { mappingAttribute1_1, mappingAttribute1_2 });
		DataRow mappingRow2 = rowFactory.create(new String[] { MAPPING_VALUE_2, MAPPING_VALUE_1 },
				new Attribute[] { mappingAttribute2_1, mappingAttribute2_2 });

		// generate ExampleSets of size 1x1 for all attribute types
		nominalSet = ExampleSets.from(nominalAttribute).withExpectedSize(1).addDataRow(nominalRow).build();
		polynominalSet = ExampleSets.from(polynominalAttribute).withExpectedSize(1).addDataRow(polynominalRow).build();
		binominalSet = ExampleSets.from(binominalAttribute).withExpectedSize(1).addDataRow(binominalRow).build();
		textSet = ExampleSets.from(textAttribute).withExpectedSize(1).addDataRow(textRow).build();
		filepathSet = ExampleSets.from(filepathAttribute).withExpectedSize(1).addDataRow(filepathRow).build();

		numericalSet = ExampleSets.from(numericalAttribute).withExpectedSize(1).addDataRow(numericalRow).build();
		integerSet = ExampleSets.from(integerAttribute).withExpectedSize(1).addDataRow(integerRow).build();
		realSet = ExampleSets.from(realAttribute).withExpectedSize(1).addDataRow(realRow).build();

		dateSet = ExampleSets.from(dateAttribute).withExpectedSize(1).addDataRow(dateRow).build();
		timeSet = ExampleSets.from(timeAttribute).withExpectedSize(1).addDataRow(timeRow).build();
		datetimeSet = ExampleSets.from(datetimeAttribute).withExpectedSize(1).addDataRow(datetimeRow).build();

		twoAttSet = ExampleSets.from(numericalAttribute, twoAttAttribute).withExpectedSize(1).addDataRow(twoAttRow).build();
		numericalWrongAttNameSet = ExampleSets.from(numericalWrongAttNameAttribute).withExpectedSize(1)
				.addDataRow(numericalWrongAttNameRow).build();
		numericalLabelSet = ExampleSets.from(realAttribute).withExpectedSize(1).addDataRow(realRow)
				.withRole(realAttribute, Attributes.LABEL_NAME).build();
		binominalSet2 = ExampleSets.from(binominalAttribute2).withExpectedSize(1).addDataRow(binominalRow2).build();
		binominalSet3 = ExampleSets.from(binominalAttribute3).withExpectedSize(1).addDataRow(binominalRow3).build();
		mappingSet1 = ExampleSets.from(mappingAttribute1_1, mappingAttribute1_2).withExpectedSize(1).addDataRow(mappingRow1)
				.build();
		mappingSet2 = ExampleSets.from(mappingAttribute2_1, mappingAttribute2_2).withExpectedSize(1).addDataRow(mappingRow2)
				.build();
	}

	@Test
	public void testInvalidArguments() {
		try {
			ExampleSetAppender.merge(null, (ExampleSet[]) null);
			fail("Null argument merge was successful but should have failed!");
		} catch (Exception e) {
			assertEquals("Error class unexpected!", IllegalArgumentException.class, e.getClass());
		}

		try {
			ExampleSetAppender.merge(null, (List<ExampleSet>) null);
			fail("Null argument merge was successful but should have failed!");
		} catch (Exception e) {
			assertEquals("Error class unexpected!", IllegalArgumentException.class, e.getClass());
		}

		try {
			ExampleSetAppender.merge(null);
			fail("Empty ExampleSet list merge was successful but should have failed!");
		} catch (Exception e) {
			assertEquals("Error class unexpected!", MissingIOObjectException.class, e.getClass());
		}
	}

	@Test
	public void testIncompatibleTypesNominal() {
		// nominal -> incompatible
		try {
			ExampleSetAppender.merge(null, nominalSet, numericalSet);
			fail("Incompatible type merge was successful but should have failed!");
		} catch (OperatorException e) {
			assertEquals("Error class unexpected!", UserError.class, e.getClass());
		}

		try {
			ExampleSetAppender.merge(null, nominalSet, realSet);
			fail("Incompatible type merge was successful but should have failed!");
		} catch (OperatorException e) {
			assertEquals("Error class unexpected!", UserError.class, e.getClass());
		}

		try {
			ExampleSetAppender.merge(null, nominalSet, integerSet);
			fail("Incompatible type merge was successful but should have failed!");
		} catch (OperatorException e) {
			assertEquals("Error class unexpected!", UserError.class, e.getClass());
		}

		try {
			ExampleSetAppender.merge(null, nominalSet, dateSet);
			fail("Incompatible type merge was successful but should have failed!");
		} catch (OperatorException e) {
			assertEquals("Error class unexpected!", UserError.class, e.getClass());
		}

		try {
			ExampleSetAppender.merge(null, nominalSet, timeSet);
			fail("Incompatible type merge was successful but should have failed!");
		} catch (OperatorException e) {
			assertEquals("Error class unexpected!", UserError.class, e.getClass());
		}

		try {
			ExampleSetAppender.merge(null, nominalSet, datetimeSet);
			fail("Incompatible type merge was successful but should have failed!");
		} catch (OperatorException e) {
			assertEquals("Error class unexpected!", UserError.class, e.getClass());
		}

		// polynominal -> incompatible
		try {
			ExampleSetAppender.merge(null, polynominalSet, numericalSet);
			fail("Incompatible type merge was successful but should have failed!");
		} catch (OperatorException e) {
			assertEquals("Error class unexpected!", UserError.class, e.getClass());
		}

		try {
			ExampleSetAppender.merge(null, polynominalSet, realSet);
			fail("Incompatible type merge was successful but should have failed!");
		} catch (OperatorException e) {
			assertEquals("Error class unexpected!", UserError.class, e.getClass());
		}

		try {
			ExampleSetAppender.merge(null, polynominalSet, integerSet);
			fail("Incompatible type merge was successful but should have failed!");
		} catch (OperatorException e) {
			assertEquals("Error class unexpected!", UserError.class, e.getClass());
		}

		try {
			ExampleSetAppender.merge(null, polynominalSet, dateSet);
			fail("Incompatible type merge was successful but should have failed!");
		} catch (OperatorException e) {
			assertEquals("Error class unexpected!", UserError.class, e.getClass());
		}

		try {
			ExampleSetAppender.merge(null, polynominalSet, timeSet);
			fail("Incompatible type merge was successful but should have failed!");
		} catch (OperatorException e) {
			assertEquals("Error class unexpected!", UserError.class, e.getClass());
		}

		try {
			ExampleSetAppender.merge(null, polynominalSet, datetimeSet);
			fail("Incompatible type merge was successful but should have failed!");
		} catch (OperatorException e) {
			assertEquals("Error class unexpected!", UserError.class, e.getClass());
		}

		// binominal -> incompatible
		try {
			ExampleSetAppender.merge(null, binominalSet, numericalSet);
			fail("Incompatible type merge was successful but should have failed!");
		} catch (OperatorException e) {
			assertEquals("Error class unexpected!", UserError.class, e.getClass());
		}

		try {
			ExampleSetAppender.merge(null, binominalSet, realSet);
			fail("Incompatible type merge was successful but should have failed!");
		} catch (OperatorException e) {
			assertEquals("Error class unexpected!", UserError.class, e.getClass());
		}

		try {
			ExampleSetAppender.merge(null, binominalSet, integerSet);
			fail("Incompatible type merge was successful but should have failed!");
		} catch (OperatorException e) {
			assertEquals("Error class unexpected!", UserError.class, e.getClass());
		}

		try {
			ExampleSetAppender.merge(null, binominalSet, dateSet);
			fail("Incompatible type merge was successful but should have failed!");
		} catch (OperatorException e) {
			assertEquals("Error class unexpected!", UserError.class, e.getClass());
		}

		try {
			ExampleSetAppender.merge(null, binominalSet, timeSet);
			fail("Incompatible type merge was successful but should have failed!");
		} catch (OperatorException e) {
			assertEquals("Error class unexpected!", UserError.class, e.getClass());
		}

		try {
			ExampleSetAppender.merge(null, binominalSet, datetimeSet);
			fail("Incompatible type merge was successful but should have failed!");
		} catch (OperatorException e) {
			assertEquals("Error class unexpected!", UserError.class, e.getClass());
		}

		// filepath -> incompatible
		try {
			ExampleSetAppender.merge(null, filepathSet, numericalSet);
			fail("Incompatible type merge was successful but should have failed!");
		} catch (OperatorException e) {
			assertEquals("Error class unexpected!", UserError.class, e.getClass());
		}

		try {
			ExampleSetAppender.merge(null, filepathSet, realSet);
			fail("Incompatible type merge was successful but should have failed!");
		} catch (OperatorException e) {
			assertEquals("Error class unexpected!", UserError.class, e.getClass());
		}

		try {
			ExampleSetAppender.merge(null, filepathSet, integerSet);
			fail("Incompatible type merge was successful but should have failed!");
		} catch (OperatorException e) {
			assertEquals("Error class unexpected!", UserError.class, e.getClass());
		}

		try {
			ExampleSetAppender.merge(null, filepathSet, dateSet);
			fail("Incompatible type merge was successful but should have failed!");
		} catch (OperatorException e) {
			assertEquals("Error class unexpected!", UserError.class, e.getClass());
		}

		try {
			ExampleSetAppender.merge(null, filepathSet, timeSet);
			fail("Incompatible type merge was successful but should have failed!");
		} catch (OperatorException e) {
			assertEquals("Error class unexpected!", UserError.class, e.getClass());
		}

		try {
			ExampleSetAppender.merge(null, filepathSet, datetimeSet);
			fail("Incompatible type merge was successful but should have failed!");
		} catch (OperatorException e) {
			assertEquals("Error class unexpected!", UserError.class, e.getClass());
		}
	}

	@Test
	public void testIncompatibleTypesNumerical() {
		// numerical -> incompatible
		try {
			ExampleSetAppender.merge(null, numericalSet, nominalSet);
			fail("Incompatible type merge was successful but should have failed!");
		} catch (OperatorException e) {
			assertEquals("Error class unexpected!", UserError.class, e.getClass());
		}

		try {
			ExampleSetAppender.merge(null, numericalSet, polynominalSet);
			fail("Incompatible type merge was successful but should have failed!");
		} catch (OperatorException e) {
			assertEquals("Error class unexpected!", UserError.class, e.getClass());
		}

		try {
			ExampleSetAppender.merge(null, numericalSet, binominalSet);
			fail("Incompatible type merge was successful but should have failed!");
		} catch (OperatorException e) {
			assertEquals("Error class unexpected!", UserError.class, e.getClass());
		}

		try {
			ExampleSetAppender.merge(null, numericalSet, filepathSet);
			fail("Incompatible type merge was successful but should have failed!");
		} catch (OperatorException e) {
			assertEquals("Error class unexpected!", UserError.class, e.getClass());
		}

		try {
			ExampleSetAppender.merge(null, numericalSet, dateSet);
			fail("Incompatible type merge was successful but should have failed!");
		} catch (OperatorException e) {
			assertEquals("Error class unexpected!", UserError.class, e.getClass());
		}

		try {
			ExampleSetAppender.merge(null, numericalSet, timeSet);
			fail("Incompatible type merge was successful but should have failed!");
		} catch (OperatorException e) {
			assertEquals("Error class unexpected!", UserError.class, e.getClass());
		}

		try {
			ExampleSetAppender.merge(null, numericalSet, datetimeSet);
			fail("Incompatible type merge was successful but should have failed!");
		} catch (OperatorException e) {
			assertEquals("Error class unexpected!", UserError.class, e.getClass());
		}

		// real -> incompatible
		try {
			ExampleSetAppender.merge(null, realSet, nominalSet);
			fail("Incompatible type merge was successful but should have failed!");
		} catch (OperatorException e) {
			assertEquals("Error class unexpected!", UserError.class, e.getClass());
		}

		try {
			ExampleSetAppender.merge(null, realSet, polynominalSet);
			fail("Incompatible type merge was successful but should have failed!");
		} catch (OperatorException e) {
			assertEquals("Error class unexpected!", UserError.class, e.getClass());
		}

		try {
			ExampleSetAppender.merge(null, realSet, binominalSet);
			fail("Incompatible type merge was successful but should have failed!");
		} catch (OperatorException e) {
			assertEquals("Error class unexpected!", UserError.class, e.getClass());
		}

		try {
			ExampleSetAppender.merge(null, realSet, filepathSet);
			fail("Incompatible type merge was successful but should have failed!");
		} catch (OperatorException e) {
			assertEquals("Error class unexpected!", UserError.class, e.getClass());
		}

		try {
			ExampleSetAppender.merge(null, realSet, dateSet);
			fail("Incompatible type merge was successful but should have failed!");
		} catch (OperatorException e) {
			assertEquals("Error class unexpected!", UserError.class, e.getClass());
		}

		try {
			ExampleSetAppender.merge(null, realSet, timeSet);
			fail("Incompatible type merge was successful but should have failed!");
		} catch (OperatorException e) {
			assertEquals("Error class unexpected!", UserError.class, e.getClass());
		}

		try {
			ExampleSetAppender.merge(null, realSet, datetimeSet);
			fail("Incompatible type merge was successful but should have failed!");
		} catch (OperatorException e) {
			assertEquals("Error class unexpected!", UserError.class, e.getClass());
		}

		// integer -> incompatible
		try {
			ExampleSetAppender.merge(null, integerSet, nominalSet);
			fail("Incompatible type merge was successful but should have failed!");
		} catch (OperatorException e) {
			assertEquals("Error class unexpected!", UserError.class, e.getClass());
		}

		try {
			ExampleSetAppender.merge(null, integerSet, polynominalSet);
			fail("Incompatible type merge was successful but should have failed!");
		} catch (OperatorException e) {
			assertEquals("Error class unexpected!", UserError.class, e.getClass());
		}

		try {
			ExampleSetAppender.merge(null, integerSet, binominalSet);
			fail("Incompatible type merge was successful but should have failed!");
		} catch (OperatorException e) {
			assertEquals("Error class unexpected!", UserError.class, e.getClass());
		}

		try {
			ExampleSetAppender.merge(null, integerSet, filepathSet);
			fail("Incompatible type merge was successful but should have failed!");
		} catch (OperatorException e) {
			assertEquals("Error class unexpected!", UserError.class, e.getClass());
		}

		try {
			ExampleSetAppender.merge(null, integerSet, dateSet);
			fail("Incompatible type merge was successful but should have failed!");
		} catch (OperatorException e) {
			assertEquals("Error class unexpected!", UserError.class, e.getClass());
		}

		try {
			ExampleSetAppender.merge(null, integerSet, timeSet);
			fail("Incompatible type merge was successful but should have failed!");
		} catch (OperatorException e) {
			assertEquals("Error class unexpected!", UserError.class, e.getClass());
		}

		try {
			ExampleSetAppender.merge(null, integerSet, datetimeSet);
			fail("Incompatible type merge was successful but should have failed!");
		} catch (OperatorException e) {
			assertEquals("Error class unexpected!", UserError.class, e.getClass());
		}
	}

	@Test
	public void testIncompatibleTypesDates() {
		// date -> incompatible
		try {
			ExampleSetAppender.merge(null, dateSet, nominalSet);
			fail("Incompatible type merge was successful but should have failed!");
		} catch (OperatorException e) {
			assertEquals("Error class unexpected!", UserError.class, e.getClass());
		}

		try {
			ExampleSetAppender.merge(null, dateSet, polynominalSet);
			fail("Incompatible type merge was successful but should have failed!");
		} catch (OperatorException e) {
			assertEquals("Error class unexpected!", UserError.class, e.getClass());
		}

		try {
			ExampleSetAppender.merge(null, dateSet, binominalSet);
			fail("Incompatible type merge was successful but should have failed!");
		} catch (OperatorException e) {
			assertEquals("Error class unexpected!", UserError.class, e.getClass());
		}

		try {
			ExampleSetAppender.merge(null, dateSet, filepathSet);
			fail("Incompatible type merge was successful but should have failed!");
		} catch (OperatorException e) {
			assertEquals("Error class unexpected!", UserError.class, e.getClass());
		}

		try {
			ExampleSetAppender.merge(null, dateSet, numericalSet);
			fail("Incompatible type merge was successful but should have failed!");
		} catch (OperatorException e) {
			assertEquals("Error class unexpected!", UserError.class, e.getClass());
		}

		try {
			ExampleSetAppender.merge(null, dateSet, realSet);
			fail("Incompatible type merge was successful but should have failed!");
		} catch (OperatorException e) {
			assertEquals("Error class unexpected!", UserError.class, e.getClass());
		}

		try {
			ExampleSetAppender.merge(null, dateSet, integerSet);
			fail("Incompatible type merge was successful but should have failed!");
		} catch (OperatorException e) {
			assertEquals("Error class unexpected!", UserError.class, e.getClass());
		}

		// time -> incompatible
		try {
			ExampleSetAppender.merge(null, timeSet, nominalSet);
			fail("Incompatible type merge was successful but should have failed!");
		} catch (OperatorException e) {
			assertEquals("Error class unexpected!", UserError.class, e.getClass());
		}

		try {
			ExampleSetAppender.merge(null, timeSet, polynominalSet);
			fail("Incompatible type merge was successful but should have failed!");
		} catch (OperatorException e) {
			assertEquals("Error class unexpected!", UserError.class, e.getClass());
		}

		try {
			ExampleSetAppender.merge(null, timeSet, binominalSet);
			fail("Incompatible type merge was successful but should have failed!");
		} catch (OperatorException e) {
			assertEquals("Error class unexpected!", UserError.class, e.getClass());
		}

		try {
			ExampleSetAppender.merge(null, timeSet, filepathSet);
			fail("Incompatible type merge was successful but should have failed!");
		} catch (OperatorException e) {
			assertEquals("Error class unexpected!", UserError.class, e.getClass());
		}

		try {
			ExampleSetAppender.merge(null, timeSet, numericalSet);
			fail("Incompatible type merge was successful but should have failed!");
		} catch (OperatorException e) {
			assertEquals("Error class unexpected!", UserError.class, e.getClass());
		}

		try {
			ExampleSetAppender.merge(null, timeSet, realSet);
			fail("Incompatible type merge was successful but should have failed!");
		} catch (OperatorException e) {
			assertEquals("Error class unexpected!", UserError.class, e.getClass());
		}

		try {
			ExampleSetAppender.merge(null, timeSet, integerSet);
			fail("Incompatible type merge was successful but should have failed!");
		} catch (OperatorException e) {
			assertEquals("Error class unexpected!", UserError.class, e.getClass());
		}

		// datetime -> incompatible
		try {
			ExampleSetAppender.merge(null, datetimeSet, nominalSet);
			fail("Incompatible type merge was successful but should have failed!");
		} catch (OperatorException e) {
			assertEquals("Error class unexpected!", UserError.class, e.getClass());
		}

		try {
			ExampleSetAppender.merge(null, datetimeSet, polynominalSet);
			fail("Incompatible type merge was successful but should have failed!");
		} catch (OperatorException e) {
			assertEquals("Error class unexpected!", UserError.class, e.getClass());
		}

		try {
			ExampleSetAppender.merge(null, datetimeSet, binominalSet);
			fail("Incompatible type merge was successful but should have failed!");
		} catch (OperatorException e) {
			assertEquals("Error class unexpected!", UserError.class, e.getClass());
		}

		try {
			ExampleSetAppender.merge(null, datetimeSet, filepathSet);
			fail("Incompatible type merge was successful but should have failed!");
		} catch (OperatorException e) {
			assertEquals("Error class unexpected!", UserError.class, e.getClass());
		}

		try {
			ExampleSetAppender.merge(null, datetimeSet, numericalSet);
			fail("Incompatible type merge was successful but should have failed!");
		} catch (OperatorException e) {
			assertEquals("Error class unexpected!", UserError.class, e.getClass());
		}

		try {
			ExampleSetAppender.merge(null, datetimeSet, realSet);
			fail("Incompatible type merge was successful but should have failed!");
		} catch (OperatorException e) {
			assertEquals("Error class unexpected!", UserError.class, e.getClass());
		}

		try {
			ExampleSetAppender.merge(null, datetimeSet, integerSet);
			fail("Incompatible type merge was successful but should have failed!");
		} catch (OperatorException e) {
			assertEquals("Error class unexpected!", UserError.class, e.getClass());
		}
	}

	@Test
	public void testIncompatibleAttributes() {
		try {
			ExampleSetAppender.merge(null, realSet, twoAttSet);
			fail("Incompatible attribute merge was successful but should have failed!");
		} catch (OperatorException e) {
			assertEquals("Error class unexpected!", UserError.class, e.getClass());
		}

		try {
			ExampleSetAppender.merge(null, realSet, numericalWrongAttNameSet);
			fail("Incompatible attribute merge was successful but should have failed!");
		} catch (OperatorException e) {
			assertEquals("Error class unexpected!", UserError.class, e.getClass());
		}
	}

	@Test
	public void testSingleMerge() throws OperatorException {
		ExampleSet result = ExampleSetAppender.merge(null, polynominalSet);
		assertEquals(1, result.size());
		assertEquals(1, getAttribute(result, ATT_NAME_1).getMapping().size());
		assertEquals("Attribute value type unexpected!", Ontology.POLYNOMINAL,
				getAttribute(result, ATT_NAME_1).getValueType());
	}

	@Test
	public void testNominalMappingMerge() throws OperatorException {
		ExampleSet result = ExampleSetAppender.merge(null, mappingSet1, mappingSet2);
		assertEquals(2, result.size());
		assertEquals(2, getAttribute(result, ATT_NAME_1).getMapping().size());
		assertEquals(2, getAttribute(result, ATT_NAME_2).getMapping().size());
		assertEquals(MAPPING_VALUE_1, result.getExample(0).getNominalValue(getAttribute(result, ATT_NAME_1)));
		assertEquals(MAPPING_VALUE_2, result.getExample(0).getNominalValue(getAttribute(result, ATT_NAME_2)));
		assertEquals(MAPPING_VALUE_2, result.getExample(1).getNominalValue(getAttribute(result, ATT_NAME_1)));
		assertEquals(MAPPING_VALUE_1, result.getExample(1).getNominalValue(getAttribute(result, ATT_NAME_2)));

		result = ExampleSetAppender.merge(null, mappingSet1, mappingSet2, mappingSet2);
		assertEquals(3, result.size());
		assertEquals(2, getAttribute(result, ATT_NAME_1).getMapping().size());
		assertEquals(2, getAttribute(result, ATT_NAME_2).getMapping().size());
		assertEquals(MAPPING_VALUE_1, result.getExample(0).getNominalValue(getAttribute(result, ATT_NAME_1)));
		assertEquals(MAPPING_VALUE_2, result.getExample(0).getNominalValue(getAttribute(result, ATT_NAME_2)));
		assertEquals(MAPPING_VALUE_2, result.getExample(1).getNominalValue(getAttribute(result, ATT_NAME_1)));
		assertEquals(MAPPING_VALUE_1, result.getExample(1).getNominalValue(getAttribute(result, ATT_NAME_2)));
		assertEquals(MAPPING_VALUE_2, result.getExample(2).getNominalValue(getAttribute(result, ATT_NAME_1)));
		assertEquals(MAPPING_VALUE_1, result.getExample(2).getNominalValue(getAttribute(result, ATT_NAME_2)));
	}

	@Test
	public void testNominalTypeRemoval() throws OperatorException {
		ExampleSet result = ExampleSetAppender.merge(null, nominalSet, nominalSet);
		assertEquals(2, result.size());
		assertEquals(1, getAttribute(result, ATT_NAME_1).getMapping().size());
		assertEquals("Attribute value type unexpected!", Ontology.POLYNOMINAL,
				getAttribute(result, ATT_NAME_1).getValueType());
	}

	@Test
	public void testNumericalTypeRemoval() throws OperatorException {
		ExampleSet result = ExampleSetAppender.merge(null, numericalSet, numericalSet);
		assertEquals(2, result.size());
		assertEquals("Attribute value type unexpected!", Ontology.REAL, getAttribute(result, ATT_NAME_1).getValueType());
	}

	@Test
	public void testAppendIdenticalTypes() throws OperatorException {
		ExampleSet result = ExampleSetAppender.merge(null, nominalSet, nominalSet);
		assertEquals(2, result.size());
		assertEquals(1, getAttribute(result, ATT_NAME_1).getMapping().size());
		assertEquals("Attribute value type unexpected!", Ontology.POLYNOMINAL,
				getAttribute(result, ATT_NAME_1).getValueType());

		result = ExampleSetAppender.merge(null, binominalSet, binominalSet);
		assertEquals(2, result.size());
		assertEquals(1, getAttribute(result, ATT_NAME_1).getMapping().size());
		assertEquals("Attribute value type unexpected!", Ontology.BINOMINAL,
				getAttribute(result, ATT_NAME_1).getValueType());

		result = ExampleSetAppender.merge(null, polynominalSet, polynominalSet);
		assertEquals(2, result.size());
		assertEquals(1, getAttribute(result, ATT_NAME_1).getMapping().size());
		assertEquals("Attribute value type unexpected!", Ontology.POLYNOMINAL,
				getAttribute(result, ATT_NAME_1).getValueType());

		result = ExampleSetAppender.merge(null, textSet, textSet);
		assertEquals(2, result.size());
		assertEquals(1, getAttribute(result, ATT_NAME_1).getMapping().size());
		assertEquals("Attribute value type unexpected!", Ontology.STRING, getAttribute(result, ATT_NAME_1).getValueType());

		result = ExampleSetAppender.merge(null, filepathSet, filepathSet);
		assertEquals(2, result.size());
		assertEquals(1, getAttribute(result, ATT_NAME_1).getMapping().size());
		assertEquals("Attribute value type unexpected!", Ontology.FILE_PATH,
				getAttribute(result, ATT_NAME_1).getValueType());

		result = ExampleSetAppender.merge(null, numericalSet, numericalSet);
		assertEquals(2, result.size());
		assertEquals("Attribute value type unexpected!", Ontology.REAL, getAttribute(result, ATT_NAME_1).getValueType());

		result = ExampleSetAppender.merge(null, integerSet, integerSet);
		assertEquals(2, result.size());
		assertEquals("Attribute value type unexpected!", Ontology.INTEGER, getAttribute(result, ATT_NAME_1).getValueType());

		result = ExampleSetAppender.merge(null, realSet, realSet);
		assertEquals(2, result.size());
		assertEquals("Attribute value type unexpected!", Ontology.REAL, getAttribute(result, ATT_NAME_1).getValueType());

		result = ExampleSetAppender.merge(null, dateSet, dateSet);
		assertEquals(2, result.size());
		assertEquals("Attribute value type unexpected!", Ontology.DATE, getAttribute(result, ATT_NAME_1).getValueType());

		result = ExampleSetAppender.merge(null, timeSet, timeSet);
		assertEquals(2, result.size());
		assertEquals("Attribute value type unexpected!", Ontology.TIME, getAttribute(result, ATT_NAME_1).getValueType());

		result = ExampleSetAppender.merge(null, datetimeSet, datetimeSet);
		assertEquals(2, result.size());
		assertEquals("Attribute value type unexpected!", Ontology.DATE_TIME,
				getAttribute(result, ATT_NAME_1).getValueType());
	}

	@Test
	public void testAppendNarrowerTypes() throws OperatorException {
		ExampleSet result = ExampleSetAppender.merge(null, nominalSet, polynominalSet);
		assertEquals(2, result.size());
		assertEquals(2, getAttribute(result, ATT_NAME_1).getMapping().size());
		assertEquals("Attribute value type unexpected!", Ontology.POLYNOMINAL,
				getAttribute(result, ATT_NAME_1).getValueType());

		result = ExampleSetAppender.merge(null, nominalSet, binominalSet);
		assertEquals(2, result.size());
		assertEquals(2, getAttribute(result, ATT_NAME_1).getMapping().size());
		assertEquals("Attribute value type unexpected!", Ontology.POLYNOMINAL,
				getAttribute(result, ATT_NAME_1).getValueType());

		result = ExampleSetAppender.merge(null, nominalSet, textSet);
		assertEquals(2, result.size());
		assertEquals(2, getAttribute(result, ATT_NAME_1).getMapping().size());
		assertEquals("Attribute value type unexpected!", Ontology.POLYNOMINAL,
				getAttribute(result, ATT_NAME_1).getValueType());

		result = ExampleSetAppender.merge(null, nominalSet, filepathSet);
		assertEquals(2, result.size());
		assertEquals(2, getAttribute(result, ATT_NAME_1).getMapping().size());
		assertEquals("Attribute value type unexpected!", Ontology.POLYNOMINAL,
				getAttribute(result, ATT_NAME_1).getValueType());

		result = ExampleSetAppender.merge(null, polynominalSet, binominalSet);
		assertEquals(2, result.size());
		assertEquals(2, getAttribute(result, ATT_NAME_1).getMapping().size());
		assertEquals("Attribute value type unexpected!", Ontology.POLYNOMINAL,
				getAttribute(result, ATT_NAME_1).getValueType());

		result = ExampleSetAppender.merge(null, numericalSet, integerSet);
		assertEquals(2, result.size());
		assertEquals("Attribute value type unexpected!", Ontology.REAL, getAttribute(result, ATT_NAME_1).getValueType());

		result = ExampleSetAppender.merge(null, numericalSet, realSet);
		assertEquals(2, result.size());
		assertEquals("Attribute value type unexpected!", Ontology.REAL, getAttribute(result, ATT_NAME_1).getValueType());

		result = ExampleSetAppender.merge(null, realSet, integerSet);
		assertEquals(2, result.size());
		assertEquals("Attribute value type unexpected!", Ontology.REAL, getAttribute(result, ATT_NAME_1).getValueType());

		result = ExampleSetAppender.merge(null, datetimeSet, dateSet);
		assertEquals(2, result.size());
		assertEquals("Attribute value type unexpected!", Ontology.DATE_TIME,
				getAttribute(result, ATT_NAME_1).getValueType());

		result = ExampleSetAppender.merge(null, datetimeSet, timeSet);
		assertEquals(2, result.size());
		assertEquals("Attribute value type unexpected!", Ontology.DATE_TIME,
				getAttribute(result, ATT_NAME_1).getValueType());
	}

	@Test
	public void testAppendBroaderTypes() throws OperatorException {
		ExampleSet result = ExampleSetAppender.merge(null, polynominalSet, nominalSet);
		assertEquals(2, result.size());
		assertEquals(2, getAttribute(result, ATT_NAME_1).getMapping().size());
		assertEquals("Attribute value type unexpected!", Ontology.POLYNOMINAL,
				getAttribute(result, ATT_NAME_1).getValueType());

		result = ExampleSetAppender.merge(null, binominalSet, polynominalSet);
		assertEquals(2, result.size());
		assertEquals(2, getAttribute(result, ATT_NAME_1).getMapping().size());
		assertEquals("Attribute value type unexpected!", Ontology.POLYNOMINAL,
				getAttribute(result, ATT_NAME_1).getValueType());

		result = ExampleSetAppender.merge(null, textSet, polynominalSet);
		assertEquals(2, result.size());
		assertEquals(2, getAttribute(result, ATT_NAME_1).getMapping().size());
		assertEquals("Attribute value type unexpected!", Ontology.POLYNOMINAL,
				getAttribute(result, ATT_NAME_1).getValueType());

		result = ExampleSetAppender.merge(null, filepathSet, polynominalSet);
		assertEquals(2, result.size());
		assertEquals(2, getAttribute(result, ATT_NAME_1).getMapping().size());
		assertEquals("Attribute value type unexpected!", Ontology.POLYNOMINAL,
				getAttribute(result, ATT_NAME_1).getValueType());

		result = ExampleSetAppender.merge(null, realSet, numericalSet);
		assertEquals(2, result.size());
		assertEquals("Attribute value type unexpected!", Ontology.REAL, getAttribute(result, ATT_NAME_1).getValueType());

		result = ExampleSetAppender.merge(null, integerSet, realSet);
		assertEquals(2, result.size());
		assertEquals("Attribute value type unexpected!", Ontology.REAL, getAttribute(result, ATT_NAME_1).getValueType());

		result = ExampleSetAppender.merge(null, dateSet, datetimeSet);
		assertEquals(2, result.size());
		assertEquals("Attribute value type unexpected!", Ontology.DATE_TIME,
				getAttribute(result, ATT_NAME_1).getValueType());

		result = ExampleSetAppender.merge(null, timeSet, datetimeSet);
		assertEquals(2, result.size());
		assertEquals("Attribute value type unexpected!", Ontology.DATE_TIME,
				getAttribute(result, ATT_NAME_1).getValueType());
	}

	@Test
	public void testAppendOtherTypes() throws OperatorException {
		ExampleSet result = ExampleSetAppender.merge(null, binominalSet, filepathSet);
		assertEquals(2, result.size());
		assertEquals(2, getAttribute(result, ATT_NAME_1).getMapping().size());
		assertEquals("Attribute value type unexpected!", Ontology.BINOMINAL,
				getAttribute(result, ATT_NAME_1).getValueType());

		result = ExampleSetAppender.merge(null, filepathSet, binominalSet);
		assertEquals(2, result.size());
		assertEquals(2, getAttribute(result, ATT_NAME_1).getMapping().size());
		assertEquals("Attribute value type unexpected!", Ontology.FILE_PATH,
				getAttribute(result, ATT_NAME_1).getValueType());

		result = ExampleSetAppender.merge(null, binominalSet, textSet);
		assertEquals(2, result.size());
		assertEquals(2, getAttribute(result, ATT_NAME_1).getMapping().size());
		assertEquals("Attribute value type unexpected!", Ontology.BINOMINAL,
				getAttribute(result, ATT_NAME_1).getValueType());

		result = ExampleSetAppender.merge(null, textSet, binominalSet);
		assertEquals(2, result.size());
		assertEquals(2, getAttribute(result, ATT_NAME_1).getMapping().size());
		assertEquals("Attribute value type unexpected!", Ontology.STRING, getAttribute(result, ATT_NAME_1).getValueType());

		result = ExampleSetAppender.merge(null, dateSet, timeSet);
		assertEquals(2, result.size());
		assertEquals("Attribute value type unexpected!", Ontology.DATE_TIME,
				getAttribute(result, ATT_NAME_1).getValueType());

		result = ExampleSetAppender.merge(null, timeSet, dateSet);
		assertEquals(2, result.size());
		assertEquals("Attribute value type unexpected!", Ontology.DATE_TIME,
				getAttribute(result, ATT_NAME_1).getValueType());
	}

	@Test
	public void testBinominalConversion() throws OperatorException {
		ExampleSet result = ExampleSetAppender.merge(null, binominalSet, filepathSet, textSet);
		assertEquals(3, result.size());
		assertEquals(3, getAttribute(result, ATT_NAME_1).getMapping().size());
		assertEquals("Attribute value type unexpected!", Ontology.POLYNOMINAL,
				getAttribute(result, ATT_NAME_1).getValueType());

		result = ExampleSetAppender.merge(null, binominalSet, filepathSet, filepathSet);
		assertEquals(3, result.size());
		assertEquals(2, getAttribute(result, ATT_NAME_1).getMapping().size());
		assertEquals("Attribute value type unexpected!", Ontology.BINOMINAL,
				getAttribute(result, ATT_NAME_1).getValueType());

		result = ExampleSetAppender.merge(null, binominalSet, binominalSet2, binominalSet3);
		assertEquals(3, result.size());
		assertEquals(3, getAttribute(result, ATT_NAME_1).getMapping().size());
		assertEquals("Attribute value type unexpected!", Ontology.POLYNOMINAL,
				getAttribute(result, ATT_NAME_1).getValueType());
	}

	@Test
	public void testSpecialRoles() throws OperatorException {
		ExampleSet result = ExampleSetAppender.merge(null, numericalLabelSet, realSet);
		assertEquals(2, result.size());
		assertEquals("Special role was unexpected!", Attributes.LABEL_NAME,
				result.getAttributes().getRole(getAttribute(result, ATT_NAME_1)).getSpecialName());

		result = ExampleSetAppender.merge(null, realSet, numericalLabelSet);
		assertEquals(2, result.size());
		assertEquals("Special role should not exist!", null,
				result.getAttributes().getRole(getAttribute(result, ATT_NAME_1)).getSpecialName());
	}

	@Test
	public void testAnnotations() throws OperatorException {
		ExampleSet annotatedSet = numericalSet;
		annotatedSet.getAnnotations().setAnnotation(ANNOTATION_KEY, ANNOTATION_VALUE);

		ExampleSet result = ExampleSetAppender.merge(null, annotatedSet, realSet);
		assertEquals(1, result.getAnnotations().size());
		assertEquals("Annotation value was unexpected!", ANNOTATION_VALUE,
				result.getAnnotations().getAnnotation(ANNOTATION_KEY));

		result = ExampleSetAppender.merge(null, realSet, annotatedSet);
		assertEquals("Annotation value should not exist!", null, result.getAnnotations().getAnnotation(ANNOTATION_KEY));
	}

	/**
	 * Returns the attribute identified by the given name from the passed input example set.
	 *
	 * @param data
	 *            the input {@link ExampleSet}
	 * @param attName
	 *            the name of the {@link Attribute} to find
	 * @return the attribute or {@code null} if no attribute exists in the data for the given name
	 */
	private Attribute getAttribute(ExampleSet data, String attName) {
		return data.getAttributes().get(attName);
	}
}
