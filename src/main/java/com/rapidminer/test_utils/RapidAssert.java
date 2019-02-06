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
package com.rapidminer.test_utils;

import java.text.MessageFormat;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import java.util.Scanner;
import java.util.Set;

import org.junit.Assert;
import org.junit.ComparisonFailure;

import com.rapidminer.example.Attribute;
import com.rapidminer.example.AttributeRole;
import com.rapidminer.example.Attributes;
import com.rapidminer.example.Example;
import com.rapidminer.example.table.NominalMapping;
import com.rapidminer.operator.Annotations;
import com.rapidminer.operator.IOObject;
import com.rapidminer.operator.meta.ParameterValue;
import com.rapidminer.tools.Ontology;

import junit.framework.AssertionFailedError;


/**
 * Extension for JUnit's Assert for testing RapidMiner objects.
 *
 * @author Simon Fischer, Marcin Skirzynski, Marius Helf
 *
 */
public class RapidAssert extends Assert {

	public static final double DELTA = 0.000000001;
	public static final double MAX_RELATIVE_ERROR = 0.000000001;

	public static final AsserterRegistry ASSERTER_REGISTRY = new AsserterRegistry();
	private static boolean ignoreRepositoryNameForSourceAnnotation = true;

	/**
	 * Returns <code>true</code> if the ioobjects class is supported for comparison in the test
	 * extension and <code>false</code> otherwise.
	 */
	public static boolean comparable(IOObject ioobject) {
		return ASSERTER_REGISTRY.getAsserterForObject(ioobject) != null;
	}

	/**
	 * Returns <code>true</code> if both ioobject classes are comparable to each other and
	 * <code>false</code> otherwise.
	 */
	public static boolean comparable(IOObject ioobject1, IOObject ioobject2) {
		return ASSERTER_REGISTRY.getAsserterForObjects(ioobject1, ioobject2) != null;
	}

	/**
	 * Extends the Junit assertEquals method by additionally checking the doubles for NaN.
	 *
	 * @param message
	 *            message to display if an error occurs
	 * @param expected
	 *            expected value
	 * @param actual
	 *            actual value
	 */
	public static void assertEqualsNaN(String message, double expected, double actual) {
		if (Double.isNaN(expected)) {
			if (!Double.isNaN(actual)) {
				throw new AssertionFailedError(message + " expected: <" + expected + "> but was: <" + actual + ">");
			}
		} else {
			assertEquals(message, expected, actual, DELTA);
		}
	}

	/**
	 * Attention: Does not work with values near 0!!
	 */
	public static void assertEqualsWithRelativeErrorOrBothNaN(String message, double expected, double actual) {
		if (expected == actual) {
			return;
		}

		if (Double.isNaN(expected) && !Double.isNaN(actual)) {
			throw new AssertionFailedError(message + " expected: <" + expected + "> but was: <" + actual + ">");
		}

		if (!Double.isNaN(expected) && Double.isNaN(actual)) {
			throw new AssertionFailedError(message + " expected: <" + expected + "> but was: <" + actual + ">");
		}

		double relativeError;
		if (Math.abs(actual) > Math.abs(expected)) {
			relativeError = Math.abs((expected - actual) / actual);
		} else {
			relativeError = Math.abs((expected - actual) / expected);
		}
		if (relativeError > MAX_RELATIVE_ERROR) {
			throw new AssertionFailedError(message + " expected: <" + expected + "> but was: <" + actual + ">");
		}
	}

	/**
	 * Tests if the special names of the attribute roles are equal and the associated attributes
	 * themselves.
	 *
	 * @param message
	 *            message to display if an error occurs
	 * @param expected
	 *            expected value
	 * @param actual
	 *            actual value
	 * @param compareDefaultValues
	 */
	public static void assertEquals(String message, AttributeRole expected, AttributeRole actual,
			boolean compareDefaultValues) {
		Assert.assertEquals(message + " (attribute role)", expected.getSpecialName(), actual.getSpecialName());
		Attribute expectedAttribute = expected.getAttribute();
		Attribute actualAttribute = actual.getAttribute();
		assertEquals(message, expectedAttribute, actualAttribute, compareDefaultValues);
	}

	/**
	 * Tests two attributes by using the name, type, block, type, default value and the nominal
	 * mapping
	 *
	 * @param message
	 *            message to display if an error occurs
	 * @param expected
	 *            expected value
	 * @param actual
	 *            actual value
	 */
	public static void assertEquals(String message, Attribute expected, Attribute actual, boolean compareDefaultValues) {
		Assert.assertEquals(message + " (attribute name)", expected.getName(), actual.getName());
		Assert.assertEquals(message + " (attribute type of attribute '" + expected.getName() + "': expected '"
				+ Ontology.ATTRIBUTE_VALUE_TYPE.mapIndex(expected.getValueType()) + "' but was '"
				+ Ontology.ATTRIBUTE_VALUE_TYPE.mapIndex(actual.getValueType()) + "')", expected.getValueType(),
				actual.getValueType());
		Assert.assertEquals(message + " (attribute block type of attribute '" + expected.getName() + ": expected '"
				+ Ontology.ATTRIBUTE_BLOCK_TYPE.mapIndex(expected.getBlockType()) + "' but was '"
				+ Ontology.ATTRIBUTE_BLOCK_TYPE.mapIndex(actual.getBlockType()) + "')", expected.getBlockType(),
				actual.getBlockType());

		if (compareDefaultValues) {
			assertEquals(message + " (default value of attribute '" + expected.getName() + ")", expected.getDefault(),
					actual.getDefault(), DELTA);
		}
		if (expected.isNominal()) {
			assertEqualsIgnoreOrder(message + " (nominal mapping of attribute '" + expected.getName() + ")",
					expected.getMapping(), actual.getMapping());
		}
	}

	/**
	 * Tests two nominal mappings for its size and values.
	 *
	 * @param message
	 *            message to display if an error occurs
	 * @param expected
	 *            expected value
	 * @param actual
	 *            actual value
	 * @param ignoreOrder
	 *            if <code>true</code> the order of the mappings is not checked, but only the size
	 *            of the mapping and that all values of <code>expected</code> are present in
	 *            <code>actual</code>.
	 */
	public static void assertEquals(String message, NominalMapping expected, NominalMapping actual, boolean ignoreOrder) {
		if (expected == actual) {
			return;
		}
		Assert.assertTrue(expected == null && actual == null || expected != null && actual != null);
		if (expected == null || actual == null) {
			return;
		}

		Assert.assertEquals(message + " (nominal mapping size)", expected.size(), actual.size());

		List<String> expectedValues = expected.getValues();
		List<String> actualValues = actual.getValues();

		// check that we have the same values in both mappings:
		Set<String> expectedValuesSet = new HashSet<String>(expectedValues);
		Set<String> actualValuesSet = new HashSet<String>(actualValues);
		Assert.assertEquals(message + " (different nominal values)", expectedValuesSet, actualValuesSet);

		if (!ignoreOrder) {
			// check order

			Iterator<String> expectedIt = expectedValues.iterator();
			while (expectedIt.hasNext()) {
				String expectedValue = expectedIt.next();
				Assert.assertEquals(message + " (index of nominal value '" + expectedValue + "')",
						expected.mapString(expectedValue), actual.mapString(expectedValue));
			}
		}
	}

	/**
	 * Tests two nominal mappings for its size and values.
	 *
	 * @param message
	 *            message to display if an error occurs
	 * @param expected
	 *            expected value
	 * @param actual
	 *            actual value
	 */
	public static void assertEqualsIgnoreOrder(String message, NominalMapping expected, NominalMapping actual) {
		assertEquals(message, expected, actual, true);
	}

	/**
	 * Tests all objects in the array.
	 *
	 * @param expected
	 *            array with expected objects
	 * @param actual
	 *            array with actual objects
	 */
	public static void assertArrayEquals(String message, Object[] expected, Object[] actual) {
		if (expected == null) {
			assertEquals((Object) null, actual);
			return;
		}
		if (actual == null) {
			throw new AssertionFailedError(message + " (expected " + Arrays.toString(expected) + " , but is null)");
		}
		assertEquals(message + " (array length is not equal)", expected.length, actual.length);
		for (int i = 0; i < expected.length; i++) {
			assertEquals(message, expected[i], actual[i]);
		}
	}

	public static void assertArrayEquals(String message, byte[] expected, byte[] actual) {
		if (expected == null) {
			assertEquals((Object) null, actual);
			return;
		}
		if (actual == null) {
			throw new AssertionFailedError(message + " (expected " + Arrays.toString(expected) + " , but is null)");
		}
		assertEquals(message + " (array length is not equal)", expected.length, actual.length);
		for (int i = 0; i < expected.length; i++) {
			assertEquals(message, expected[i], actual[i]);
		}
	}

	/**
	 * Compares a string linewise, i.e. ignores different linebreak characters.
	 *
	 * Does this by incrementally reading all expected an actual lines and comparing them linewise.
	 *
	 * @param message
	 * @param expected
	 * @param actual
	 */
	public static void assertLinewiseEquals(String message, String expected, String actual) {
		try (Scanner expectedScanner = new Scanner(expected); Scanner actualScanner = new Scanner(actual);) {

			String expectedLine = null;
			String actualLine = null;
			int lineCounter = 1;
			while (expectedScanner.hasNextLine()) {
				expectedLine = expectedScanner.nextLine();
				if (actualScanner.hasNextLine()) {
					actualLine = actualScanner.nextLine();
				} else {
					fail("Line " + lineCounter + ": actual input has less lines then expected result! Expected: "
							+ expectedLine);
				}
				assertEquals("Line " + lineCounter + ": " + message + "\n\nExpected:\n" + expected + "\nActual:\n" + actual,
						expectedLine, actualLine);
				++lineCounter;
			}
		}
	}

	/**
	 * Tests all objects in the array.
	 *
	 * @param message
	 *            message to display if an error occurs
	 * @param expected
	 *            array with expected objects
	 * @param actual
	 *            array with actual objects
	 */
	public static void assertArrayEquals(Object[] expected, Object[] actual) {
		assertArrayEquals("", expected, actual);
	}

	/**
	 * Tests if both list of ioobjects are equal.
	 *
	 * @param expected
	 *            expected value
	 * @param actual
	 *            actual value
	 */
	public static void assertEquals(String message, List<IOObject> expected, List<IOObject> actual) {
		assertSize(expected, actual);

		Iterator<IOObject> expectedIter = expected.iterator();
		Iterator<IOObject> actualIter = actual.iterator();

		int objectIndex = 1;
		while (expectedIter.hasNext() && actualIter.hasNext()) {
			IOObject expectedIOO = expectedIter.next();
			IOObject actualIOO = actualIter.next();
			String subMessage = message + " IOObject \"" + actualIOO.getSource() + "\" at position " + objectIndex
					+ " does not match the expected value ";
			assertEquals(subMessage, expectedIOO, actualIOO);
			objectIndex++;
		}

	}

	/**
	 * Tests if both list of ioobjects are equal.
	 *
	 * @param expected
	 *            expected value
	 * @param actual
	 *            actual value
	 */
	public static void assertEquals(List<IOObject> expected, List<IOObject> actual) {
		RapidAssert.assertEquals("", expected, actual);
	}

	/**
	 * Tests if both lists of IOObjects have the same size.
	 *
	 * @param expected
	 * @param actual
	 */
	public static void assertSize(List<IOObject> expected, List<IOObject> actual) {
		assertEquals(
				"Number of connected output ports in the process is not equal with the number of ioobjects contained in the same folder with the format 'processname-expected-port-1', 'processname-expected-port-2', ...",
				expected.size(), actual.size());
	}

	/**
	 * Tests if the two IOObjects are equal.
	 *
	 * @param expectedIOO
	 * @param actualIOO
	 */
	public static void assertEquals(IOObject expectedIOO, IOObject actualIOO) {
		RapidAssert.assertEquals("", expectedIOO, actualIOO);
	}

	/**
	 * Tests if the two IOObjects are equal and appends the given message.
	 *
	 * @param expectedIOO
	 * @param actualIOO
	 */
	public static void assertEquals(String message, IOObject expectedIOO, IOObject actualIOO) {
		assertEquals(message, expectedIOO, actualIOO, false);
	}

	/**
	 * Tests if the two IOObjects are equal and appends the given message.
	 *
	 * @param assertEqualAnnotations
	 *            if true, annotations will be compared. If false, they will be ignored.
	 * @param expectedIOO
	 * @param actualIOO
	 */
	public static void assertEquals(String message, IOObject expectedIOO, IOObject actualIOO, boolean assertEqualAnnotations) {

		/*
		 * Do not forget to add a newly supported class to the ASSERTER_REGISTRY!!!
		 */
		List<Asserter> asserterList = ASSERTER_REGISTRY.getAsserterForObjects(expectedIOO, actualIOO);
		if (asserterList != null) {
			for (Asserter asserter : asserterList) {
				asserter.assertEquals(message, expectedIOO, actualIOO);
			}
		} else {
			throw new ComparisonFailure("Comparison of the two given IOObject classes " + expectedIOO.getClass() + " and "
					+ actualIOO.getClass() + " is not supported. ", expectedIOO.toString(), actualIOO.toString());
		}

		// last, compare annotations:
		if (assertEqualAnnotations) {
			Annotations expectedAnnotations = expectedIOO.getAnnotations();
			Annotations actualAnnotations = actualIOO.getAnnotations();

			if (ignoreRepositoryNameForSourceAnnotation) {
				// compare annotations one by one. For the Source annotation, ignore the repository
				// name
				// (that's what all the regular expressions here are for)
				for (String key : expectedAnnotations.getKeys()) {
					String expectedValue = expectedAnnotations.getAnnotation(key);
					String actualValue = actualAnnotations.getAnnotation(key);

					if (expectedValue != null) {
						Assert.assertNotNull(message + "objects are equal, but annotation '" + key + "' is missing",
								actualValue);
					}

					if (Annotations.KEY_SOURCE.equals(key)) {
						if (expectedValue != null && expectedValue.startsWith("//") && expectedValue.matches("//[^/]+/.*")) {
							expectedValue = expectedValue.replaceAll("^//[^/]+/", "//repository/");
							if (actualValue != null) {
								actualValue = actualValue.replaceAll("^//[^/]+/", "//repository/");
							}
						}
					}
					Assert.assertEquals(message + "objects are equal, but annotation '" + key + "' differs: ",
							expectedValue, actualValue);
				}
			} else {
				Assert.assertEquals(message + "objects are equal, but annotations differ: ", expectedAnnotations,
						actualAnnotations);
			}
		}
	}

	/**
	 * Tests the two examples by testing the value of the examples for every given attribute. This
	 * method is sensitive to the regular attribute ordering.
	 *
	 * @param message
	 *            message to display if an error occurs. If it contains "{0}" and "{1}", it will be
	 *            replaced with the attribute name and attribute type, if an inequality occurs.
	 * @param expected
	 *            expected value
	 * @param actual
	 *            actual value
	 */
	public static void assertEquals(String message, Example expected, Example actual) {
		Assert.assertEquals(message + " (number of attributes)", expected.getAttributes().allSize(), actual.getAttributes()
				.allSize());

		Assert.assertEquals(message + " (number of special attributes)", expected.getAttributes().specialSize(), actual
				.getAttributes().specialSize());

		// get all attributes as list
		Iterator<Attribute> allExpectedAttributesIterator = expected.getAttributes().allAttributes();
		Iterator<Attribute> allActualAttributesIterator = actual.getAttributes().allAttributes();
		List<Attribute> allExpectedAttributes = new ArrayList<Attribute>();
		while (allExpectedAttributesIterator.hasNext()) {
			allExpectedAttributes.add(allExpectedAttributesIterator.next());
		}
		List<Attribute> allActualAttributes = new ArrayList<Attribute>();
		while (allActualAttributesIterator.hasNext()) {
			allActualAttributes.add(allActualAttributesIterator.next());
		}

		// get regular attributes as iterator
		Iterator<Attribute> expectedAttributesToConsider = expected.getAttributes().iterator();
		Iterator<Attribute> actualAttributesToConsider = actual.getAttributes().iterator();

		// first check regular attributes sensitive to the attribute ordering
		while (expectedAttributesToConsider.hasNext() && actualAttributesToConsider.hasNext()) {
			Attribute a1 = expectedAttributesToConsider.next();
			Attribute a2 = actualAttributesToConsider.next();
			if (!a1.getName().equals(a2.getName())) {
				// this should have been detected by previous checks already
				throw new AssertionFailedError("Attribute ordering does not match: " + a1.getName() + "," + a2.getName());
			}

			if (a1.isNominal()) {
				Assert.assertEquals(MessageFormat.format(message, "nominal", a1.getName()), expected.getNominalValue(a1),
						actual.getNominalValue(a2));
			} else if (a1.isNumerical()) {
				assertEqualsWithRelativeErrorOrBothNaN(MessageFormat.format(message, "numerical", a1.getName()),
						expected.getValue(a1), actual.getValue(a2));
			} else {
				Assert.assertEquals(expected.getDateValue(a1), actual.getDateValue(a2));
			}

			// passed, so delete regular attribute from all attributes list.
			allExpectedAttributes.remove(a1);
			allActualAttributes.remove(a2);
		}

		// check the remaining special attributes
		for (int i = 0; i < allExpectedAttributes.size(); i++) {
			Attribute expectedSpecial = allExpectedAttributes.get(i);
			String expectedName = expectedSpecial.getName();
			String actualName = null;
			for (int j = 0; j < allActualAttributes.size(); j++) {
				Attribute actualSpecial = allActualAttributes.get(j);
				actualName = actualSpecial.getName();
				if (expectedName.equals(actualName)) {
					if (expectedSpecial.isNominal()) {
						Assert.assertEquals(MessageFormat.format(message, "nominal", expectedSpecial.getName()),
								expected.getNominalValue(expectedSpecial), actual.getNominalValue(actualSpecial));
					} else if (expectedSpecial.isNumerical()) {
						assertEqualsWithRelativeErrorOrBothNaN(
								MessageFormat.format(message, "numerical", expectedSpecial.getName()),
								expected.getValue(expectedSpecial), actual.getValue(actualSpecial));
					} else {
						Assert.assertEquals(expected.getDateValue(expectedSpecial), actual.getDateValue(actualSpecial));
					}
					// remove from list
					allExpectedAttributes.remove(expectedSpecial);
					allActualAttributes.remove(actualSpecial);
					i--;
					break;
				}
			}
			if (!expectedName.equals(actualName)) {
				throw new AssertionFailedError("Expected attribute not found: " + expectedSpecial.getName());
			}
		}

	}

	/**
	 * Tests if all attributes are equal. This method is sensitive to the regular attribute
	 * ordering.
	 *
	 * Optionally compares the default values of the attributes. The default value is only relevant
	 * for sparse data rows, so it should not be compared for non-sparse data.
	 *
	 * @param message
	 *            message to display if an error occurs
	 * @param expected
	 *            expected value
	 * @param actual
	 *            actual value
	 * @param compareDefaultValues
	 *            specifies if the attributes default values should be compared.
	 */
	public static void assertEquals(String message, Attributes expected, Attributes actual, boolean compareDefaultValues) {
		Assert.assertEquals(message + " (number of attributes)", expected.allSize(), actual.allSize());
		Assert.assertEquals(message + " (number of special attributes)", expected.specialSize(), actual.specialSize());

		Iterator<AttributeRole> expectedRoleIt = expected.regularAttributes();
		Iterator<AttributeRole> actualRoleIt = actual.regularAttributes();
		while (expectedRoleIt.hasNext()) {
			AttributeRole expectedRole = expectedRoleIt.next();
			AttributeRole actualRole = actualRoleIt.next();
			RapidAssert.assertEquals(message, expectedRole, actualRole, compareDefaultValues);
		}

		expectedRoleIt = expected.specialAttributes();
		while (expectedRoleIt.hasNext()) {
			AttributeRole expectedRole = expectedRoleIt.next();
			AttributeRole actualRole = actual.getRole(actual.getSpecial(expectedRole.getSpecialName()));
			RapidAssert.assertEquals(message, expectedRole, actualRole, compareDefaultValues);
		}
	}

	public static void assertEquals(String message, ParameterValue expected, ParameterValue actual) {
		Assert.assertEquals(message + " - operator", expected.getOperator(), actual.getOperator());
		Assert.assertEquals(message + " - parameterKey", expected.getParameterKey(), actual.getParameterKey());
		Assert.assertEquals(message + " - parameterValue", expected.getParameterValue(), actual.getParameterValue());
	}
}
