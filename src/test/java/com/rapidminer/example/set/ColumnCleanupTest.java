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
import static org.junit.Assert.assertNotSame;

import java.util.Arrays;

import org.junit.AfterClass;
import org.junit.Test;

import com.rapidminer.RapidMiner;
import com.rapidminer.example.Attribute;
import com.rapidminer.example.Attributes;
import com.rapidminer.example.Example;
import com.rapidminer.example.ExampleSet;
import com.rapidminer.example.table.ExampleTable;
import com.rapidminer.example.test.ExampleTestTools;
import com.rapidminer.example.utils.ExampleSets;
import com.rapidminer.operator.UserError;
import com.rapidminer.operator.preprocessing.filter.NominalToNumeric;
import com.rapidminer.operator.preprocessing.filter.NominalToNumericModel;
import com.rapidminer.operator.tools.ExpressionEvaluationException;
import com.rapidminer.tools.ParameterService;
import com.rapidminer.tools.math.similarity.numerical.EuclideanDistance;


/**
 * Test the method {@link ExampleSet#columnCleanup}.
 *
 * @author Gisa Schaefer
 */
public class ColumnCleanupTest {

	private final static Attribute attribute1 = ExampleTestTools.attributeDogCatMouse();
	private final static Attribute attribute2 = ExampleTestTools.attributeInt();
	private final static Attribute attribute3 = ExampleTestTools.attributeYesNo();
	private final static Attribute attribute4 = ExampleTestTools.attributeReal();

	private final static int ROWS = 4;

	@Test
	public void simpleExampleSetActivatedTest() {
		ParameterService.setParameterValue(RapidMiner.PROPERTY_RAPIDMINER_SYSTEM_LEGACY_DATA_MGMT, String.valueOf(false));

		ExampleSet simpleExampleSet = ExampleSets.from(attribute1, attribute2, attribute3, attribute4).withBlankSize(ROWS)
				.build();

		setValues(simpleExampleSet);
		removeAttributes(simpleExampleSet);
		ExampleTable oldTable = simpleExampleSet.getExampleTable();

		simpleExampleSet.cleanup();

		testForActivated(simpleExampleSet, oldTable);
		testValues(simpleExampleSet);
	}

	@Test
	public void simpleExampleSetDeactivatedTest() {
		ParameterService.setParameterValue(RapidMiner.PROPERTY_RAPIDMINER_SYSTEM_LEGACY_DATA_MGMT, String.valueOf(true));

		ExampleSet simpleExampleSet = ExampleSets.from(attribute1, attribute2, attribute3, attribute4).withBlankSize(ROWS)
				.build();

		setValues(simpleExampleSet);
		removeAttributes(simpleExampleSet);
		ExampleTable oldTable = simpleExampleSet.getExampleTable();

		simpleExampleSet.cleanup();

		testForDeactivated(simpleExampleSet, oldTable);
		testValues(simpleExampleSet);
	}

	@Test
	public void conditionedExampleSetActivatedTest() throws ExpressionEvaluationException {
		ParameterService.setParameterValue(RapidMiner.PROPERTY_RAPIDMINER_SYSTEM_LEGACY_DATA_MGMT, String.valueOf(false));

		ExampleSet simpleExampleSet = ExampleSets.from(attribute1, attribute2, attribute3, attribute4).withBlankSize(ROWS)
				.build();
		ExampleSet conditionedExampleSet = new ConditionedExampleSet(simpleExampleSet, new AcceptAllCondition());

		setValues(conditionedExampleSet);
		removeAttributes(conditionedExampleSet);
		ExampleTable oldTable = conditionedExampleSet.getExampleTable();

		conditionedExampleSet.cleanup();

		testForActivated(conditionedExampleSet, oldTable);
		testValues(conditionedExampleSet);
	}

	@Test
	public void conditionedExampleSetDeactivatedTest() throws ExpressionEvaluationException {
		ParameterService.setParameterValue(RapidMiner.PROPERTY_RAPIDMINER_SYSTEM_LEGACY_DATA_MGMT, String.valueOf(true));

		ExampleSet simpleExampleSet = ExampleSets.from(attribute1, attribute2, attribute3, attribute4).withBlankSize(ROWS)
				.build();
		ExampleSet conditionedExampleSet = new ConditionedExampleSet(simpleExampleSet, new AcceptAllCondition());

		setValues(conditionedExampleSet);
		removeAttributes(conditionedExampleSet);
		ExampleTable oldTable = conditionedExampleSet.getExampleTable();

		conditionedExampleSet.cleanup();

		testForDeactivated(conditionedExampleSet, oldTable);
		testValues(conditionedExampleSet);
	}

	@Test
	public void mappedExampleSetActivatedTest() throws ExpressionEvaluationException {
		ParameterService.setParameterValue(RapidMiner.PROPERTY_RAPIDMINER_SYSTEM_LEGACY_DATA_MGMT, String.valueOf(false));

		ExampleSet simpleExampleSet = ExampleSets.from(attribute1, attribute2, attribute3, attribute4).withBlankSize(ROWS)
				.build();
		int[] indices = new int[ROWS];
		Arrays.setAll(indices, i -> i);
		ExampleSet mappedExampleSet = new MappedExampleSet(simpleExampleSet, indices);

		setValues(mappedExampleSet);
		removeAttributes(mappedExampleSet);
		ExampleTable oldTable = mappedExampleSet.getExampleTable();

		mappedExampleSet.cleanup();

		testForActivated(mappedExampleSet, oldTable);
		testValues(mappedExampleSet);
	}

	@Test
	public void mappedExampleSetDeactivatedTest() throws ExpressionEvaluationException {
		ParameterService.setParameterValue(RapidMiner.PROPERTY_RAPIDMINER_SYSTEM_LEGACY_DATA_MGMT, String.valueOf(true));

		ExampleSet simpleExampleSet = ExampleSets.from(attribute1, attribute2, attribute3, attribute4).withBlankSize(ROWS)
				.build();
		int[] indices = new int[ROWS];
		Arrays.setAll(indices, i -> i);
		ExampleSet mappedExampleSet = new MappedExampleSet(simpleExampleSet, indices);

		setValues(mappedExampleSet);
		removeAttributes(mappedExampleSet);
		ExampleTable oldTable = mappedExampleSet.getExampleTable();

		mappedExampleSet.cleanup();

		testForDeactivated(mappedExampleSet, oldTable);
		testValues(mappedExampleSet);
	}

	@Test
	public void splittedExampleSetActivatedTest() throws UserError {
		ParameterService.setParameterValue(RapidMiner.PROPERTY_RAPIDMINER_SYSTEM_LEGACY_DATA_MGMT, String.valueOf(false));

		ExampleSet simpleExampleSet = ExampleSets.from(attribute1, attribute2, attribute3, attribute4).withBlankSize(ROWS)
				.build();
		ExampleSet splittedExampleSet = new SplittedExampleSet(simpleExampleSet, 0.5, SplittedExampleSet.AUTOMATIC, false,
				0);

		setValues(splittedExampleSet);
		removeAttributes(splittedExampleSet);
		ExampleTable oldTable = splittedExampleSet.getExampleTable();

		splittedExampleSet.cleanup();

		testForActivated(splittedExampleSet, oldTable);
		testValues(splittedExampleSet);
	}

	@Test
	public void splittedExampleSetDeactivatedTest() throws UserError {
		ParameterService.setParameterValue(RapidMiner.PROPERTY_RAPIDMINER_SYSTEM_LEGACY_DATA_MGMT, String.valueOf(true));

		ExampleSet simpleExampleSet = ExampleSets.from(attribute1, attribute2, attribute3, attribute4).withBlankSize(ROWS)
				.build();
		ExampleSet splittedExampleSet = new SplittedExampleSet(simpleExampleSet, 0.5, SplittedExampleSet.AUTOMATIC, false,
				0);

		setValues(splittedExampleSet);
		removeAttributes(splittedExampleSet);
		ExampleTable oldTable = splittedExampleSet.getExampleTable();

		splittedExampleSet.cleanup();

		testForDeactivated(splittedExampleSet, oldTable);
		testValues(splittedExampleSet);
	}

	@Test
	public void sortedExampleSetActivatedTest() throws UserError {
		ParameterService.setParameterValue(RapidMiner.PROPERTY_RAPIDMINER_SYSTEM_LEGACY_DATA_MGMT, String.valueOf(false));

		ExampleSet simpleExampleSet = ExampleSets.from(attribute1, attribute2, attribute3, attribute4).withBlankSize(ROWS)
				.build();
		ExampleSet sortedExampleSet = new SortedExampleSet(simpleExampleSet, attribute1, SortedExampleSet.DECREASING);

		setValues(sortedExampleSet);
		removeAttributes(sortedExampleSet);
		ExampleTable oldTable = sortedExampleSet.getExampleTable();

		sortedExampleSet.cleanup();

		testForActivated(sortedExampleSet, oldTable);
		testValues(sortedExampleSet);
	}

	@Test
	public void sortedExampleSetDeactivatedTest() throws UserError {
		ParameterService.setParameterValue(RapidMiner.PROPERTY_RAPIDMINER_SYSTEM_LEGACY_DATA_MGMT, String.valueOf(true));

		ExampleSet simpleExampleSet = ExampleSets.from(attribute1, attribute2, attribute3, attribute4).withBlankSize(ROWS)
				.build();
		ExampleSet sortedExampleSet = new SortedExampleSet(simpleExampleSet, attribute1, SortedExampleSet.DECREASING);

		setValues(sortedExampleSet);
		removeAttributes(sortedExampleSet);
		ExampleTable oldTable = sortedExampleSet.getExampleTable();

		sortedExampleSet.cleanup();

		testForDeactivated(sortedExampleSet, oldTable);
		testValues(sortedExampleSet);
	}

	@Test
	public void singleExampleSetActivatedTest() throws UserError {
		ParameterService.setParameterValue(RapidMiner.PROPERTY_RAPIDMINER_SYSTEM_LEGACY_DATA_MGMT, String.valueOf(false));

		ExampleSet simpleExampleSet = ExampleSets.from(attribute1, attribute2, attribute3, attribute4).withBlankSize(ROWS)
				.build();
		ExampleSet singleExampleSet = new SingleExampleExampleSet(simpleExampleSet, simpleExampleSet.getExample(0));

		setValues(singleExampleSet);
		removeAttributes(singleExampleSet);
		ExampleTable oldTable = singleExampleSet.getExampleTable();

		singleExampleSet.cleanup();

		testForActivated(singleExampleSet, oldTable);
		testValues(singleExampleSet);
	}

	@Test
	public void singleExampleSetDeactivatedTest() throws UserError {
		ParameterService.setParameterValue(RapidMiner.PROPERTY_RAPIDMINER_SYSTEM_LEGACY_DATA_MGMT, String.valueOf(true));

		ExampleSet simpleExampleSet = ExampleSets.from(attribute1, attribute2, attribute3, attribute4).withBlankSize(ROWS)
				.build();
		ExampleSet singleExampleSet = new SingleExampleExampleSet(simpleExampleSet, simpleExampleSet.getExample(0));

		setValues(singleExampleSet);
		removeAttributes(singleExampleSet);
		ExampleTable oldTable = singleExampleSet.getExampleTable();

		singleExampleSet.cleanup();

		testForDeactivated(singleExampleSet, oldTable);
		testValues(singleExampleSet);
	}

	@Test
	public void similarityExampleSetActivatedTest() throws UserError {
		ParameterService.setParameterValue(RapidMiner.PROPERTY_RAPIDMINER_SYSTEM_LEGACY_DATA_MGMT, String.valueOf(false));

		ExampleSet simpleExampleSet = ExampleSets.from(attribute1, attribute2, attribute3, attribute4).withBlankSize(ROWS)
				.withRole(attribute1, Attributes.ID_NAME).build();
		ExampleSet similarityExampleSet = new SimilarityExampleSet(simpleExampleSet, new EuclideanDistance());

		// a {@link SimilarityExampleSet} has no example table and no stored values so we have to
		// remove attributes from the simpleExampleSet
		setValues(simpleExampleSet);
		removeAttributes(simpleExampleSet);
		ExampleTable oldTable = simpleExampleSet.getExampleTable();

		similarityExampleSet.cleanup();

		testForActivated(simpleExampleSet, oldTable);
		testValues(simpleExampleSet);
	}

	@Test
	public void similarityExampleSetDeactivatedTest() throws UserError {
		ParameterService.setParameterValue(RapidMiner.PROPERTY_RAPIDMINER_SYSTEM_LEGACY_DATA_MGMT, String.valueOf(true));

		ExampleSet simpleExampleSet = ExampleSets.from(attribute1, attribute2, attribute3, attribute4).withBlankSize(ROWS)
				.withRole(attribute1, Attributes.ID_NAME).build();
		ExampleSet similarityExampleSet = new SimilarityExampleSet(simpleExampleSet, new EuclideanDistance());

		// a {@link SimilarityExampleSet} has no example table and no stored values so we have to
		// remove attributes from the simpleExampleSet
		setValues(simpleExampleSet);
		removeAttributes(simpleExampleSet);
		ExampleTable oldTable = simpleExampleSet.getExampleTable();

		similarityExampleSet.cleanup();

		testForDeactivated(simpleExampleSet, oldTable);
		testValues(simpleExampleSet);
	}

	@Test
	public void replaceMissingExampleSetActivatedTest() throws UserError {
		ParameterService.setParameterValue(RapidMiner.PROPERTY_RAPIDMINER_SYSTEM_LEGACY_DATA_MGMT, String.valueOf(false));

		ExampleSet simpleExampleSet = ExampleSets.from(attribute1, attribute2, attribute3, attribute4).withBlankSize(ROWS)
				.build();
		ExampleSet replaceExampleSet = new ReplaceMissingExampleSet(simpleExampleSet);

		setValues(replaceExampleSet);
		removeAttributes(replaceExampleSet);
		ExampleTable oldTable = replaceExampleSet.getExampleTable();

		replaceExampleSet.cleanup();

		testForActivated(replaceExampleSet, oldTable);
		testValues(replaceExampleSet);
	}

	@Test
	public void replaceMissingExampleSetDeactivatedTest() throws UserError {
		ParameterService.setParameterValue(RapidMiner.PROPERTY_RAPIDMINER_SYSTEM_LEGACY_DATA_MGMT, String.valueOf(true));

		ExampleSet simpleExampleSet = ExampleSets.from(attribute1, attribute2, attribute3, attribute4).withBlankSize(ROWS)
				.build();
		ExampleSet replaceExampleSet = new ReplaceMissingExampleSet(simpleExampleSet);

		setValues(replaceExampleSet);
		removeAttributes(replaceExampleSet);
		ExampleTable oldTable = replaceExampleSet.getExampleTable();

		replaceExampleSet.cleanup();

		testForDeactivated(replaceExampleSet, oldTable);
		testValues(replaceExampleSet);
	}

	@SuppressWarnings("deprecation")
	@Test
	public void remappedExampleSetActivatedTest() throws UserError {
		ParameterService.setParameterValue(RapidMiner.PROPERTY_RAPIDMINER_SYSTEM_LEGACY_DATA_MGMT, String.valueOf(false));

		ExampleSet simpleExampleSet = ExampleSets.from(attribute1, attribute2, attribute3, attribute4).withBlankSize(ROWS)
				.build();
		ExampleSet remappedExampleSet = new RemappedExampleSet(simpleExampleSet,
				ExampleSets.from((Attribute) attribute2.clone(), (Attribute) attribute1.clone(),
						(Attribute) attribute4.clone(), (Attribute) attribute3.clone()).build());

		setValues(remappedExampleSet);
		removeAttributes(remappedExampleSet);
		ExampleTable oldTable = remappedExampleSet.getExampleTable();

		remappedExampleSet.cleanup();

		testForActivated(remappedExampleSet, oldTable);
		testValues(remappedExampleSet);
	}

	@SuppressWarnings("deprecation")
	@Test
	public void remappedExampleSetDeactivatedTest() throws UserError {
		ParameterService.setParameterValue(RapidMiner.PROPERTY_RAPIDMINER_SYSTEM_LEGACY_DATA_MGMT, String.valueOf(true));

		ExampleSet simpleExampleSet = ExampleSets.from(attribute1, attribute2, attribute3, attribute4).withBlankSize(ROWS)
				.build();
		ExampleSet remappedExampleSet = new RemappedExampleSet(simpleExampleSet,
				ExampleSets.from((Attribute) attribute2.clone(), (Attribute) attribute1.clone(),
						(Attribute) attribute4.clone(), (Attribute) attribute3.clone()).build());

		setValues(remappedExampleSet);
		removeAttributes(remappedExampleSet);
		ExampleTable oldTable = remappedExampleSet.getExampleTable();

		remappedExampleSet.cleanup();

		testForDeactivated(remappedExampleSet, oldTable);
		testValues(remappedExampleSet);
	}

	@SuppressWarnings("deprecation")
	@Test
	public void nonSpecialExampleSetActivatedTest() throws UserError {
		ParameterService.setParameterValue(RapidMiner.PROPERTY_RAPIDMINER_SYSTEM_LEGACY_DATA_MGMT, String.valueOf(false));

		ExampleSet simpleExampleSet = ExampleSets.from(attribute1, attribute2, attribute3, attribute4).withBlankSize(ROWS)
				.withRole(attribute1, Attributes.LABEL_NAME).build();
		ExampleSet nonSpecialExampleSet = new NonSpecialAttributesExampleSet(simpleExampleSet);

		setValues(nonSpecialExampleSet);
		removeAttributes(nonSpecialExampleSet);
		ExampleTable oldTable = nonSpecialExampleSet.getExampleTable();

		nonSpecialExampleSet.cleanup();

		testForActivated(nonSpecialExampleSet, oldTable);
		testValues(nonSpecialExampleSet);
	}

	@SuppressWarnings("deprecation")
	@Test
	public void nonSpecialExampleSetDeactivatedTest() throws UserError {
		ParameterService.setParameterValue(RapidMiner.PROPERTY_RAPIDMINER_SYSTEM_LEGACY_DATA_MGMT, String.valueOf(true));

		ExampleSet simpleExampleSet = ExampleSets.from(attribute1, attribute2, attribute3, attribute4).withBlankSize(ROWS)
				.withRole(attribute1, Attributes.LABEL_NAME).build();
		ExampleSet nonSpecialExampleSet = new NonSpecialAttributesExampleSet(simpleExampleSet);

		setValues(nonSpecialExampleSet);
		removeAttributes(nonSpecialExampleSet);
		ExampleTable oldTable = nonSpecialExampleSet.getExampleTable();

		nonSpecialExampleSet.cleanup();

		testForDeactivated(nonSpecialExampleSet, oldTable);
		testValues(nonSpecialExampleSet);
	}

	@SuppressWarnings("deprecation")
	@Test
	public void modelViewExampleSetActivatedTest() throws UserError {
		ParameterService.setParameterValue(RapidMiner.PROPERTY_RAPIDMINER_SYSTEM_LEGACY_DATA_MGMT, String.valueOf(false));

		ExampleSet simpleExampleSet = ExampleSets.from(attribute1, attribute2, attribute3, attribute4).withBlankSize(ROWS)
				.build();

		// a {@link ModelViewExampleSet} has no example table and no stored values so we have to
		// remove attributes from the simpleExampleSet and we have to do it before the
		// modelViewExampleSet is created because the parent is cloned

		removeAttributes(simpleExampleSet);

		ExampleSet modelViewExampleSet = new ModelViewExampleSet(simpleExampleSet, new NominalToNumericModel(
				simpleExampleSet, NominalToNumeric.DUMMY_CODING, false, null, null, null, false, 0));

		ExampleTable oldTable = simpleExampleSet.getExampleTable();

		modelViewExampleSet.cleanup();

		testForActivated(modelViewExampleSet, oldTable);

	}

	@SuppressWarnings("deprecation")
	@Test
	public void modelViewExampleSetDeactivatedTest() throws UserError {
		ParameterService.setParameterValue(RapidMiner.PROPERTY_RAPIDMINER_SYSTEM_LEGACY_DATA_MGMT, String.valueOf(true));

		ExampleSet simpleExampleSet = ExampleSets.from(attribute1, attribute2, attribute3, attribute4).withBlankSize(ROWS)
				.build();

		// a {@link ModelViewExampleSet} has no example table and no stored values so we have to
		// remove attributes from the simpleExampleSet and we have to do it before the
		// modelViewExampleSet is created because the parent is cloned
		removeAttributes(simpleExampleSet);
		ExampleSet modelViewExampleSet = new ModelViewExampleSet(simpleExampleSet, new NominalToNumericModel(
				simpleExampleSet, NominalToNumeric.DUMMY_CODING, false, null, null, null, false, 0));
		ExampleTable oldTable = simpleExampleSet.getExampleTable();

		modelViewExampleSet.cleanup();

		testForDeactivated(simpleExampleSet, oldTable);

	}

	@Test
	public void attributeWeightedExampleSetActivatedTest() throws UserError {
		ParameterService.setParameterValue(RapidMiner.PROPERTY_RAPIDMINER_SYSTEM_LEGACY_DATA_MGMT, String.valueOf(false));

		ExampleSet simpleExampleSet = ExampleSets.from(attribute1, attribute2, attribute3, attribute4).withBlankSize(ROWS)
				.build();
		ExampleSet attributeWeightedExampleSet = new AttributeWeightedExampleSet(simpleExampleSet);

		setValues(attributeWeightedExampleSet);
		removeAttributes(attributeWeightedExampleSet);
		ExampleTable oldTable = attributeWeightedExampleSet.getExampleTable();

		attributeWeightedExampleSet.cleanup();

		testForActivated(attributeWeightedExampleSet, oldTable);
		testValues(attributeWeightedExampleSet);
	}

	@Test
	public void attributeWeightedExampleSetDeactivatedTest() throws UserError {
		ParameterService.setParameterValue(RapidMiner.PROPERTY_RAPIDMINER_SYSTEM_LEGACY_DATA_MGMT, String.valueOf(true));

		ExampleSet simpleExampleSet = ExampleSets.from(attribute1, attribute2, attribute3, attribute4).withBlankSize(ROWS)
				.build();
		ExampleSet attributeWeightedExampleSet = new AttributeWeightedExampleSet(simpleExampleSet);

		setValues(attributeWeightedExampleSet);
		removeAttributes(attributeWeightedExampleSet);
		ExampleTable oldTable = attributeWeightedExampleSet.getExampleTable();

		attributeWeightedExampleSet.cleanup();

		testForDeactivated(attributeWeightedExampleSet, oldTable);
		testValues(attributeWeightedExampleSet);
	}

	@SuppressWarnings("deprecation")
	@Test
	public void attributeSelectionExampleSetActivatedTest() throws UserError {
		ParameterService.setParameterValue(RapidMiner.PROPERTY_RAPIDMINER_SYSTEM_LEGACY_DATA_MGMT, String.valueOf(false));

		ExampleSet simpleExampleSet = ExampleSets.from(attribute1, attribute2, attribute3, attribute4).withBlankSize(ROWS)
				.build();
		ExampleSet attributeSelectionExampleSet = new AttributeSelectionExampleSet(simpleExampleSet,
				new boolean[] { true, true, false, true });

		setValues(attributeSelectionExampleSet);
		removeAttributes(attributeSelectionExampleSet);
		ExampleTable oldTable = attributeSelectionExampleSet.getExampleTable();

		attributeSelectionExampleSet.cleanup();

		testForActivated(attributeSelectionExampleSet, oldTable);
		testValues(attributeSelectionExampleSet);
	}

	@SuppressWarnings("deprecation")
	@Test
	public void attributeSelectionExampleSetDeactivatedTest() throws UserError {
		ParameterService.setParameterValue(RapidMiner.PROPERTY_RAPIDMINER_SYSTEM_LEGACY_DATA_MGMT, String.valueOf(true));

		ExampleSet simpleExampleSet = ExampleSets.from(attribute1, attribute2, attribute3, attribute4).withBlankSize(ROWS)
				.build();
		ExampleSet attributeSelectionExampleSet = new AttributeSelectionExampleSet(simpleExampleSet,
				new boolean[] { true, true, false, true });

		setValues(attributeSelectionExampleSet);
		removeAttributes(attributeSelectionExampleSet);
		ExampleTable oldTable = attributeSelectionExampleSet.getExampleTable();

		attributeSelectionExampleSet.cleanup();

		testForDeactivated(attributeSelectionExampleSet, oldTable);
		testValues(attributeSelectionExampleSet);
	}

	@SuppressWarnings("deprecation")
	@Test
	public void moreWrappedExampleSetsTest() throws UserError, ExpressionEvaluationException {
		ParameterService.setParameterValue(RapidMiner.PROPERTY_RAPIDMINER_SYSTEM_LEGACY_DATA_MGMT, String.valueOf(false));

		ExampleSet simpleExampleSet = ExampleSets.from(attribute1, attribute2, attribute3, attribute4).withBlankSize(ROWS)
				.build();
		int[] indices = new int[ROWS];
		Arrays.setAll(indices, i -> i);
		ExampleSet mappedExampleSet = new MappedExampleSet(simpleExampleSet, indices);
		ExampleSet conditionedExampleSet = new ConditionedExampleSet(mappedExampleSet, new AcceptAllCondition());
		ExampleSet attributeSelectionExampleSet = new AttributeSelectionExampleSet(conditionedExampleSet,
				new boolean[] { true, true, false, true });

		setValues(attributeSelectionExampleSet);
		removeAttributes(attributeSelectionExampleSet);
		ExampleTable oldTable = attributeSelectionExampleSet.getExampleTable();

		attributeSelectionExampleSet.cleanup();

		testForActivated(attributeSelectionExampleSet, oldTable);
		testValues(attributeSelectionExampleSet);
	}

	/**
	 * The table stays the same when beta features are not activated.
	 */
	private void testForDeactivated(ExampleSet testSet, ExampleTable oldTable) {
		assertEquals(4, testSet.getExampleTable().getAttributeCount());
		assertEquals(oldTable, testSet.getExampleTable());
	}

	/**
	 * The size of the table decreases on columnCleanup and the new instance has a different table
	 * and different attributes.
	 */
	private void testForActivated(ExampleSet testSet, ExampleTable oldTable) {
		assertEquals(2, testSet.getExampleTable().getAttributeCount());
		assertNotSame(oldTable, testSet.getExampleTable());
	}

	/**
	 * Removing 2 attributes keeps the table attributes the same but lowers the testSet attributes.
	 */
	private void removeAttributes(ExampleSet testSet) {
		assertEquals(4, testSet.getExampleTable().getAttributeCount());

		testSet.getAttributes().remove(attribute1);
		testSet.getAttributes().remove(attribute3);

		assertEquals(4, testSet.getExampleTable().getAttributeCount());
		assertEquals(2, testSet.getAttributes().allSize());
	}

	/**
	 * The annotations stay the same.
	 */
	private void testValues(ExampleSet testSet) {
		int i = 1;
		for (Example example : testSet) {
			assertEquals(example.getValue(attribute2), 2 * i, 0);
			assertEquals(example.getValue(attribute4), 1.5 * i, 1e-12);
			i++;
		}
	}

	/**
	 * @param testSet
	 */
	private void setValues(ExampleSet testSet) {
		int i = 1;
		for (Example example : testSet) {
			example.setValue(attribute2, 2 * i);
			example.setValue(attribute4, 1.5 * i);
			i++;
		}
	}

	@AfterClass
	public static void setDown() {
		ParameterService.setParameterValue(RapidMiner.PROPERTY_RAPIDMINER_SYSTEM_LEGACY_DATA_MGMT, String.valueOf(false));
	}
}
