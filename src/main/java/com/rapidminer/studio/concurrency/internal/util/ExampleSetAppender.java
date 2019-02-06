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
package com.rapidminer.studio.concurrency.internal.util;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Set;

import com.rapidminer.example.Attribute;
import com.rapidminer.example.AttributeRole;
import com.rapidminer.example.Example;
import com.rapidminer.example.ExampleSet;
import com.rapidminer.example.table.AttributeFactory;
import com.rapidminer.example.table.DataRow;
import com.rapidminer.example.table.DataRowFactory;
import com.rapidminer.example.utils.ExampleSetBuilder;
import com.rapidminer.example.utils.ExampleSets;
import com.rapidminer.operator.MissingIOObjectException;
import com.rapidminer.operator.Operator;
import com.rapidminer.operator.OperatorException;
import com.rapidminer.operator.UserError;
import com.rapidminer.tools.Ontology;


/**
 * Provides utility methods to merge multiple {@link ExampleSet}s into a single one.
 *
 *
 * @author Sebastian Land
 * @since 7.4
 *
 */
public class ExampleSetAppender {

	/**
	 * Merge the given {@link ExampleSet}s into one ExampleSet. Checks for validity of the arguments
	 * and throws in case the provided sets cannot be merged.
	 *
	 * @param caller
	 *            the calling operator which is checked for stop; can be {@code null}
	 * @param allExampleSets
	 *            the {@link ExampleSet}s to merge together
	 * @return the merged {@link ExampleSet}
	 * @throws OperatorException
	 *             if the provided ExampleSets are incompatible with each other
	 */
	public static ExampleSet merge(Operator caller, ExampleSet... allExampleSets) throws OperatorException {
		if (allExampleSets == null) {
			throw new IllegalArgumentException("allExampleSets must not be null!");
		}
		return merge(caller, Arrays.asList(allExampleSets));
	}

	/**
	 * Merge the given {@link ExampleSet}s into one ExampleSet. Checks for validity of the arguments
	 * and throws in case the provided sets cannot be merged.
	 *
	 * @param caller
	 *            the calling operator which is checked for stop; can be {@code null}
	 * @param allExampleSets
	 *            the {@link ExampleSet}s to merge together
	 * @return the merged {@link ExampleSet}
	 * @throws OperatorException
	 *             if the provided ExampleSets are incompatible with each other
	 */
	public static ExampleSet merge(Operator caller, List<ExampleSet> allExampleSets) throws OperatorException {
		// input sanity checks
		if (allExampleSets == null) {
			throw new IllegalArgumentException("allExampleSets must not be null!");
		}
		if (allExampleSets.isEmpty()) {
			throw new MissingIOObjectException(ExampleSet.class);
		}

		// checks if all example sets have the same signature
		checkForCompatibility(allExampleSets, caller);

		// create new example table
		List<ExampleSet> remainingExampleSets = allExampleSets.subList(1, allExampleSets.size());
		ExampleSet firstSet = allExampleSets.get(0);
		int numberOfAtts = firstSet.getAttributes().allSize();
		List<Attribute> newAttributeList = new ArrayList<>(numberOfAtts);
		HashMap<String, Attribute> newAttributeNameMap = new HashMap<>(numberOfAtts, 1.0f);
		Map<Attribute, String> specialAttributesMap = new HashMap<>(numberOfAtts, 1.0f);
		Iterator<AttributeRole> a = firstSet.getAttributes().allAttributeRoles();
		while (a.hasNext()) {
			AttributeRole role = a.next();
			Attribute oldAttribute = role.getAttribute();

			int newType = oldAttribute.getValueType();
			if (oldAttribute.isNominal()) {
				// collect values to see if we have at least two
				Set<String> values = new HashSet<>();
				values.addAll(oldAttribute.getMapping().getValues());
				boolean hasPolynominal = false;
				for (ExampleSet otherExampleSet : remainingExampleSets) {
					Attribute otherAttribute = otherExampleSet.getAttributes().get(oldAttribute.getName());
					// At least one non-nominal -> throw
					if (!otherAttribute.isNominal()) {
						throwIncompatible(oldAttribute, otherAttribute, caller);
					}
					values.addAll(otherAttribute.getMapping().getValues());
					hasPolynominal |= Ontology.ATTRIBUTE_VALUE_TYPE.isA(otherAttribute.getValueType(), Ontology.POLYNOMINAL);
				}
				// binominals with more than 2 values cannot keep their value type, else try to
				// preserve value type is all have the same
				if (hasPolynominal || values.size() > 2) {
					newType = Ontology.POLYNOMINAL;
				}
			} else if (oldAttribute.isNumerical()) {
				boolean hasReal = false;
				for (ExampleSet otherExampleSet : remainingExampleSets) {
					Attribute otherAttribute = otherExampleSet.getAttributes().get(oldAttribute.getName());
					// At least one non-numerical -> throw
					if (!otherAttribute.isNumerical()) {
						throwIncompatible(oldAttribute, otherAttribute, caller);
					}

					hasReal |= Ontology.ATTRIBUTE_VALUE_TYPE.isA(otherAttribute.getValueType(), Ontology.REAL);
				}
				if (hasReal) {
					newType = Ontology.REAL;
				}
			} else if (oldAttribute.isDateTime()) {
				for (ExampleSet otherExampleSet : remainingExampleSets) {
					Attribute otherAttribute = otherExampleSet.getAttributes().get(oldAttribute.getName());
					// this case covers the date, time, date_time valueType
					// if all attribute valueTypes are the same keep it, otherwise switch to
					// date_time as the parent valueType
					if (otherAttribute.getValueType() != newType) {
						if (Ontology.ATTRIBUTE_VALUE_TYPE.isA(otherAttribute.getValueType(), Ontology.DATE)
								|| Ontology.ATTRIBUTE_VALUE_TYPE.isA(otherAttribute.getValueType(), Ontology.TIME)
								|| Ontology.ATTRIBUTE_VALUE_TYPE.isA(otherAttribute.getValueType(), Ontology.DATE_TIME)) {
							newType = Ontology.DATE_TIME;
						} else {
							// totally different valueType, cannot merge -> throw
							throwIncompatible(oldAttribute, otherAttribute, caller);
						}
					}
				}
			} else {
				// cannot happen
				throw new IllegalStateException(
						"Cannot merge example sets! One attribute was of unsupported type!" + oldAttribute);
			}

			if (newType == Ontology.NUMERICAL) {
				// we always kill numerical by converting to real
				newType = Ontology.REAL;
			} else if (newType == Ontology.NOMINAL) {
				// we always kill nominal by converting to polynominal
				newType = Ontology.POLYNOMINAL;
			}
			Attribute newAttribute = AttributeFactory.createAttribute(oldAttribute.getName(), newType,
					oldAttribute.getBlockType());
			newAttributeNameMap.put(newAttribute.getName(), newAttribute);
			newAttributeList.add(newAttribute);
			if (role.isSpecial()) {
				specialAttributesMap.put(newAttribute, role.getSpecialName());
			}
		}
		int finalSize = 0;
		for (ExampleSet otherExampleSet : allExampleSets) {
			finalSize += otherExampleSet.size();
		}
		ExampleSetBuilder builder = ExampleSets.from(newAttributeList).withExpectedSize(finalSize);

		// now fill table with rows, copied from source example sets
		DataRowFactory factory = new DataRowFactory(DataRowFactory.TYPE_DOUBLE_ARRAY, '.');
		int numberOfAttributes = newAttributeList.size();
		for (ExampleSet exampleSet : allExampleSets) {
			Attribute[] allAttributes = new Attribute[numberOfAttributes];
			int i = 0;
			for (Iterator<Attribute> iterator = exampleSet.getAttributes().allAttributes(); iterator.hasNext();) {
				allAttributes[i++] = iterator.next();
			}
			for (Example example : exampleSet) {
				DataRow dataRow = factory.create(numberOfAttributes);
				for (Attribute oldAttribute : allAttributes) {
					Attribute newAttribute = newAttributeNameMap.get(oldAttribute.getName());
					double oldValue = example.getValue(oldAttribute);
					if (Double.isNaN(oldValue)) {
						dataRow.set(newAttribute, oldValue);
					} else {
						if (oldAttribute.isNominal()) {
							dataRow.set(newAttribute,
									newAttribute.getMapping().mapString(oldAttribute.getMapping().mapIndex((int) oldValue)));
						} else {
							dataRow.set(newAttribute, oldValue);
						}
					}
				}
				// adding new row to table
				builder.addDataRow(dataRow);
			}

			if (caller != null) {
				caller.checkForStop();
			}
		}
		// create result example set
		ExampleSet resultSet = builder.withRoles(specialAttributesMap).build();
		resultSet.getAnnotations().addAll(firstSet.getAnnotations());
		return resultSet;
	}

	private static void throwIncompatible(Attribute oldAttribute, Attribute otherAttribute, Operator caller)
			throws UserError {
		throw new UserError(caller, 925,
				"Attribute '" + oldAttribute.getName() + "' has incompatible types ("
						+ Ontology.ATTRIBUTE_VALUE_TYPE.mapIndex(oldAttribute.getValueType()) + " and "
						+ Ontology.ATTRIBUTE_VALUE_TYPE.mapIndex(otherAttribute.getValueType()) + ") in two input sets.");
	}

	/**
	 * Checks whether all attributes in set 1 occur in the others as well. Types are (deliberately)
	 * not checked. Type check happens in {@link #merge(List)} itself.
	 *
	 * @throws OperatorException
	 *             on failed check
	 */
	private static void checkForCompatibility(List<ExampleSet> allExampleSets, Operator caller) throws OperatorException {
		Iterator<ExampleSet> i = allExampleSets.iterator();
		ExampleSet first = i.next();
		while (i.hasNext()) {
			checkForCompatibility(first, i.next(), caller);
		}
	}

	private static void checkForCompatibility(ExampleSet first, ExampleSet second, Operator caller)
			throws OperatorException {
		if (first.getAttributes().allSize() != second.getAttributes().allSize()) {
			throw new UserError(caller, 925, "numbers of attributes are different");
		}

		Iterator<Attribute> firstIterator = first.getAttributes().allAttributes();
		while (firstIterator.hasNext()) {
			Attribute firstAttribute = firstIterator.next();
			Attribute secondAttribute = second.getAttributes().get(firstAttribute.getName());
			if (secondAttribute == null) {
				throw new UserError(caller, 925,
						"Attribute with name '" + firstAttribute.getName() + "' is not part of second example set.");
			}
		}
	}

}
