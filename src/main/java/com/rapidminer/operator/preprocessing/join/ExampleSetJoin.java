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
package com.rapidminer.operator.preprocessing.join;

import java.util.Arrays;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.Set;

import com.rapidminer.example.Attribute;
import com.rapidminer.example.AttributeRole;
import com.rapidminer.example.Attributes;
import com.rapidminer.example.Example;
import com.rapidminer.example.ExampleSet;
import com.rapidminer.example.table.NominalMapping;
import com.rapidminer.example.utils.ExampleSetBuilder;
import com.rapidminer.example.utils.ExampleSets;
import com.rapidminer.operator.OperatorDescription;
import com.rapidminer.operator.OperatorException;
import com.rapidminer.operator.ProcessSetupError.Severity;
import com.rapidminer.operator.ProcessStoppedException;
import com.rapidminer.operator.SimpleProcessSetupError;
import com.rapidminer.operator.UserError;
import com.rapidminer.operator.annotation.ResourceConsumptionEstimator;
import com.rapidminer.operator.ports.metadata.AttributeMetaData;
import com.rapidminer.operator.ports.metadata.ExampleSetMetaData;
import com.rapidminer.operator.ports.metadata.ExampleSetPrecondition;
import com.rapidminer.operator.ports.metadata.ParameterConditionedPrecondition;
import com.rapidminer.operator.ports.metadata.SimpleMetaDataError;
import com.rapidminer.parameter.ParameterType;
import com.rapidminer.parameter.ParameterTypeAttribute;
import com.rapidminer.parameter.ParameterTypeBoolean;
import com.rapidminer.parameter.ParameterTypeCategory;
import com.rapidminer.parameter.ParameterTypeList;
import com.rapidminer.parameter.conditions.BooleanParameterCondition;
import com.rapidminer.tools.Ontology;
import com.rapidminer.tools.OperatorResourceConsumptionHandler;
import com.rapidminer.tools.container.Pair;


/**
 * <p>
 * Build the join of two example sets using the id attributes of the sets, i.e. both example sets
 * must have an id attribute where the same id indicate the same examples. If examples are missing
 * an exception will be thrown. The result example set will consist of the same number of examples
 * but the union set or the union list (depending on parameter setting double attributes will be
 * removed or renamed) of both feature sets. In case of removing double attribute the attribute
 * values must be the same for the examples of both example set, otherwise an exception will be
 * thrown.
 * </p>
 * <p>
 * Please note that this check for double attributes will only be applied for regular attributes.
 * Special attributes of the second input example set which do not exist in the first example set
 * will simply be added. If they already exist they are simply skipped.
 * </p>
 *
 * @author Ingo Mierswa, Tobias Malbrecht, Marius Helf
 *
 * @deprecated Use {@link BeltTableJoin} instead
 */
@Deprecated
public class ExampleSetJoin extends AbstractExampleSetJoin {

	public static class DoubleArrayWrapper {

		public DoubleArrayWrapper(double[] data) {
			this.data = data;
		}

		public double[] getData() {
			return data;
		}

		private double[] data;

		@Override
		public boolean equals(Object other) {
			if (!(other instanceof DoubleArrayWrapper)) {
				return false;
			}
			return Arrays.equals(data, ((DoubleArrayWrapper) other).data);
		}

		@Override
		public int hashCode() {
			return Arrays.hashCode(data);
		}
	}

	public static final String PARAMETER_JOIN_TYPE = "join_type";
	public static final String PARAMETER_LEFT_ATTRIBUTE_FOR_JOIN = "left_key_attributes";
	public static final String PARAMETER_RIGHT_ATTRIBUTE_FOR_JOIN = "right_key_attributes";
	public static final String PARAMETER_JOIN_ATTRIBUTES = "key_attributes";
	public static final String PARAMETER_USE_ID = "use_id_attribute_as_key";
	public static final String PARAMETER_KEEP_BOTH_JOIN_ATTRIBUTES = "keep_both_join_attributes";
	public static final String PARAMETER_FILL_LEFT_ID = "";

	public static final String[] JOIN_TYPES = { "inner", "left", "right", "outer" };

	public static final int JOIN_TYPE_INNER = 0;

	public static final int JOIN_TYPE_LEFT = 1;

	public static final int JOIN_TYPE_RIGHT = 2;

	public static final int JOIN_TYPE_OUTER = 3;

	public ExampleSetJoin(OperatorDescription description) {
		super(description);

		getLeftInput().addPrecondition(new ParameterConditionedPrecondition(getLeftInput(),
				new ExampleSetPrecondition(getLeftInput(), Ontology.ATTRIBUTE_VALUE, Attributes.ID_NAME), this,
				PARAMETER_USE_ID, "true"));
		getLeftInput().addPrecondition(new ParameterConditionedPrecondition(getLeftInput(),
				new ExampleSetPrecondition(getLeftInput()), this, PARAMETER_USE_ID, "false"));

		getRightInput().addPrecondition(new ParameterConditionedPrecondition(getRightInput(),
				new ExampleSetPrecondition(getRightInput(), Ontology.ATTRIBUTE_VALUE, Attributes.ID_NAME), this,
				PARAMETER_USE_ID, "true"));
		getRightInput().addPrecondition(new ParameterConditionedPrecondition(getRightInput(),
				new ExampleSetPrecondition(getRightInput()), this, PARAMETER_USE_ID, "false"));
	}

	/** Same as {@link getKeyAttributes}, but returns the MetaData of the KeyAttributes. **/
	private Pair<AttributeMetaData[], AttributeMetaData[]> getKeyAttributesMD(ExampleSetMetaData leftEMD,
			ExampleSetMetaData rightEMD) throws OperatorException {
		boolean useIdForJoin = getParameterAsBoolean(PARAMETER_USE_ID);
		boolean keepBothJoinAttributes = getParameterAsBoolean(PARAMETER_KEEP_BOTH_JOIN_ATTRIBUTES);
		Pair<AttributeMetaData[], AttributeMetaData[]> keyAttributes;

		if (!useIdForJoin) {
			List<String[]> parKeyAttributes;
			parKeyAttributes = getParameterList(PARAMETER_JOIN_ATTRIBUTES);
			int numKeyAttributes = parKeyAttributes.size();
			keyAttributes = new Pair<>(new AttributeMetaData[numKeyAttributes], new AttributeMetaData[numKeyAttributes]);
			int i = 0;

			// iterate user input
			for (String[] attributePair : parKeyAttributes) {
				// map user input to actual Attribute objects:
				AttributeMetaData amdLeft = leftEMD.getAttributeByName(attributePair[0]);
				AttributeMetaData amdRight = rightEMD.getAttributeByName(attributePair[1]);

				// check if attributes could be found:
				if (amdLeft == null) {
					getLeftInput().addError(
							new SimpleMetaDataError(Severity.ERROR, getLeftInput(), "missing_attribute", attributePair[0]));
					throw new UserError(this, "join.illegal_key_attribute", attributePair[0], "left", attributePair[1],
							"right");
				} else if (amdRight == null) {
					getRightInput().addError(
							new SimpleMetaDataError(Severity.ERROR, getRightInput(), "missing_attribute", attributePair[1]));
					throw new UserError(this, "join.illegal_key_attribute", attributePair[1], "right", attributePair[0],
							"left");
				}

				// check for incompatible types
				if (!Ontology.ATTRIBUTE_VALUE_TYPE.isA(amdLeft.getValueType(), amdRight.getValueType())
						&& !Ontology.ATTRIBUTE_VALUE_TYPE.isA(amdRight.getValueType(), amdLeft.getValueType())) {
					this.addError(new SimpleProcessSetupError(Severity.ERROR, getPortOwner(), "attributes_type_mismatch",
							attributePair[0], "left", attributePair[1], "right"));
					throw new UserError(this, "join.illegal_key_attribute", attributePair[1], "right", attributePair[0],
							"left");
				}

				// add attributes to list
				if (!keepBothJoinAttributes) {
					keyAttributes.getFirst()[i] = amdLeft;
					keyAttributes.getSecond()[i] = amdRight;
					++i;
				}
			}
		} else {
			keyAttributes = new Pair<>(new AttributeMetaData[] { leftEMD.getSpecial(Attributes.ID_NAME) },
					new AttributeMetaData[] { rightEMD.getSpecial(Attributes.ID_NAME) });
		}
		if (!keepBothJoinAttributes) {
			return keyAttributes;
		} else {
			return null;
		}
	}

	@Override
	protected ExampleSetBuilder joinData(ExampleSet leftExampleSet, ExampleSet rightExampleSet,
			List<AttributeSource> originalAttributeSources, List<Attribute> unionAttributeList) throws OperatorException {
		int joinType = getParameterAsInt(PARAMETER_JOIN_TYPE);
		leftExampleSet.remapIds();
		rightExampleSet.remapIds();

		// the attributes that are used in the left and the right table as key attributes:

		Pair<Attribute[], Attribute[]> keyAttributes = getKeyAttributes(leftExampleSet, rightExampleSet);
		copyMappings(joinType, keyAttributes, unionAttributeList);

		switch (joinType) {
			case JOIN_TYPE_INNER:
				getProgress().setTotal(leftExampleSet.size());
				return performInnerJoin(leftExampleSet, rightExampleSet, originalAttributeSources, unionAttributeList,
						keyAttributes);
			case JOIN_TYPE_LEFT:
				getProgress().setTotal(leftExampleSet.size());
				return performLeftJoin(leftExampleSet, rightExampleSet, originalAttributeSources, unionAttributeList,
						keyAttributes, null);
			case JOIN_TYPE_RIGHT:
				getProgress().setTotal(rightExampleSet.size());
				return performRightJoin(leftExampleSet, rightExampleSet, originalAttributeSources, unionAttributeList,
						keyAttributes);
			case JOIN_TYPE_OUTER:
				getProgress().setTotal(leftExampleSet.size() + rightExampleSet.size());
				return performOuterJoin(leftExampleSet, rightExampleSet, originalAttributeSources, unionAttributeList,
						keyAttributes);
			default:
				assert false;	// illegal join type
				return null;
		}
	}

	/**
	 * Checks if the mappings of the keyAttributes or their corresponding unionAttributes need to be copied depending on
	 * the joinType.
	 *
	 * @param joinType
	 *            the type of the join
	 * @param keyAttributes
	 *            the attributes to copy
	 * @param unionAttributes
	 *            the final attributes of the joined set
	 */
	private void copyMappings(int joinType, Pair<Attribute[], Attribute[]> keyAttributes,
			List<Attribute> unionAttributes) {
		// for right and outer the method addRightOnlyOccurences is called which might change the mappings of the union
		// attribute corresponding to the left keyAttribute
		if (joinType == JOIN_TYPE_RIGHT || joinType == JOIN_TYPE_OUTER) {
			copyUnionMappings(keyAttributes, unionAttributes);
		}

		// no id is used and inner or left then createKeyMapping(right,left) is called and left keyAttribute mappings
		// changed, outer calls left
		boolean useId = getParameterAsBoolean(PARAMETER_USE_ID);
		if (!useId && (joinType == JOIN_TYPE_INNER || joinType == JOIN_TYPE_LEFT || joinType == JOIN_TYPE_OUTER)) {
			copyMappings(keyAttributes, true);
		}

		// no id is used and right then createKeyMapping(left,right) is called and right keyAttribute mappings changed
		if (!useId && joinType == JOIN_TYPE_RIGHT) {
			copyMappings(keyAttributes, false);
		}
	}

	/**
	 * Copies the mapping of the union attributes corresponding to the left key Attributes.
	 *
	 * @param keyAttributes
	 *            the key attributes
	 * @param unionAttributeList
	 *            the final attributes of the joined set
	 */
	private void copyUnionMappings(Pair<Attribute[], Attribute[]> keyAttributes, List<Attribute> unionAttributeList) {
		Attribute[] copyAttributes = keyAttributes.getFirst();
		for (Attribute attribute : copyAttributes) {
			if (attribute.isNominal()) {
				Attribute unionAtt = findAttribute(unionAttributeList, attribute.getName());
				if (unionAtt != null) {
					unionAtt.setMapping((NominalMapping) unionAtt.getMapping().clone());
				}
			}
		}

	}

	/**
	 * Finds the attribute with the given name in the given list.
	 *
	 * @param attributeList
	 *            the list to check
	 * @param name
	 *            the attribute name to look for
	 * @return the attribute with the name or {@code null}
	 */
	private Attribute findAttribute(List<Attribute> attributeList, String name) {
		for (Attribute unionAttribute : attributeList) {
			if (unionAttribute.getName().equals(name)) {
				return unionAttribute;
			}
		}
		return null;
	}

	/**
	 * Copies the mappings of either the left or the right key attributes.
	 *
	 * @param keyAttributes
	 *            the key-attributes to copy
	 * @param left
	 *            if true the left key attributes are copied, otherwise the right
	 */
	private void copyMappings(Pair<Attribute[], Attribute[]> keyAttributes, boolean left) {
		Attribute[] copyAttributes = left ? keyAttributes.getFirst() : keyAttributes.getSecond();
		for (Attribute attribute : copyAttributes) {
			if (attribute.isNominal()) {
				NominalMapping newMapping = (NominalMapping) attribute.getMapping().clone();
				attribute.setMapping(newMapping);
			}
		}

	}

	/**
	 * Returns a Pair that contains two arrays of attributes of equals lenghts. Attributes in these arrays with the same
	 * index resemble attributes which must be equal during the join operation to match an example. Only if all key
	 * attributes match, the example match. Thus, each returned array defines a key for the example sets, whereby the
	 * the first entry of the pair is for the left example set, the second one for the right example set.
	 */
	private Pair<Attribute[], Attribute[]> getKeyAttributes(ExampleSet leftExampleSet, ExampleSet rightExampleSet)
			throws OperatorException {
		boolean useIdForJoin = getParameterAsBoolean(PARAMETER_USE_ID);
		Pair<Attribute[], Attribute[]> keyAttributes;
		if (!useIdForJoin) {
			List<String[]> parKeyAttributes = getParameterList(PARAMETER_JOIN_ATTRIBUTES);
			int numKeyAttributes = parKeyAttributes.size();
			keyAttributes = new Pair<>(new Attribute[numKeyAttributes], new Attribute[numKeyAttributes]);
			int i = 0;

			// iterate user input
			for (String[] attributePair : parKeyAttributes) {
				// map user input to actual Attribute objects:
				Attribute leftAttribute = leftExampleSet.getAttributes().get(attributePair[0]);
				Attribute rightAttribute = rightExampleSet.getAttributes().get(attributePair[1]);

				// check if attributes could be found:
				if (leftAttribute == null) {
					throw new UserError(this, "join.illegal_key_attribute", attributePair[0], "left", attributePair[1],
							"right");
				} else if (rightAttribute == null) {
					throw new UserError(this, "join.illegal_key_attribute", attributePair[1], "right", attributePair[0],
							"left");
				}

				// check for incompatible types
				if (!Ontology.ATTRIBUTE_VALUE_TYPE.isA(leftAttribute.getValueType(), rightAttribute.getValueType())
						&& !Ontology.ATTRIBUTE_VALUE_TYPE.isA(rightAttribute.getValueType(), leftAttribute.getValueType())) {
					throw new UserError(this, "join.illegal_key_attribute", attributePair[1], "right", attributePair[0],
							"left");
				}

				// add attributes to list
				keyAttributes.getFirst()[i] = leftAttribute;
				keyAttributes.getSecond()[i] = rightAttribute;
				++i;
			}
		} else {
			keyAttributes = new Pair<>(new Attribute[] { leftExampleSet.getAttributes().getId() },
					new Attribute[] { rightExampleSet.getAttributes().getId() });
		}
		return keyAttributes;
	}

	@Override
	protected boolean isKeyAttribute(AttributeRole attributeRole) throws OperatorException {
		String attributeName = attributeRole.getAttribute().getName();
		String attributeRoleName = attributeRole.getSpecialName();

		boolean useIdForJoin = getParameterAsBoolean(PARAMETER_USE_ID);
		if (!useIdForJoin) {
			List<String[]> parKeyAttributes;
			parKeyAttributes = getParameterList(PARAMETER_JOIN_ATTRIBUTES);
			for (String[] keyAttributePair : parKeyAttributes) {
				if (attributeName.equals(keyAttributePair[1])) {
					return true;
				}
			}
		} else {
			return attributeRoleName.equals(Attributes.ID_NAME);
		}
		return false;
	}

	/**
	 * Performs an inner join, i.e. the result table contains all examples from the source example
	 * sets whose key attributes match.
	 *
	 */
	private ExampleSetBuilder performInnerJoin(ExampleSet leftExampleSet, ExampleSet rightExampleSet,
			List<AttributeSource> originalAttributeSources, List<Attribute> unionAttributeList,
			Pair<Attribute[], Attribute[]> keyAttributes) throws ProcessStoppedException {
		ExampleSetBuilder builder = ExampleSets.from(unionAttributeList);

		Attribute[] leftKeyAttributes = null;
		Attribute[] rightKeyAttributes = null;
		Map<DoubleArrayWrapper, List<Example>> rightKeyMapping = null;
		boolean useId = getParameterAsBoolean(PARAMETER_USE_ID);

		if (!useId) {
			// create key mapping for right example set
			leftKeyAttributes = keyAttributes.getFirst();
			rightKeyAttributes = keyAttributes.getSecond();
			rightKeyMapping = createKeyMapping(rightExampleSet, rightKeyAttributes, leftKeyAttributes);
		}

		int progressCounter = 0;
		// iterate over all example from left table and search for matching examples in right table:
		for (Example leftExample : leftExampleSet) {

			List<Example> matchingRightExamples = getMatchingExamples(leftExampleSet, rightExampleSet, leftKeyAttributes,
					rightKeyMapping, useId, leftExample);

			if (matchingRightExamples != null) {
				for (Example rightExample : matchingRightExamples) {
					addCombinedOccurence(originalAttributeSources, unionAttributeList, builder, leftExample, rightExample);
				}
			}

			// trigger operator progress every 100 examples
			++progressCounter;
			if (progressCounter % 100 == 0) {
				getProgress().step(100);
				progressCounter = 0;
			}
		}
		return builder;
	}

	/**
	 * Performs a left join.
	 *
	 */
	private ExampleSetBuilder performLeftJoin(ExampleSet leftExampleSet, ExampleSet rightExampleSet,
			List<AttributeSource> originalAttributeSources, List<Attribute> unionAttributeList,
			Pair<Attribute[], Attribute[]> keyAttributes, Set<DoubleArrayWrapper> matchedExamplesInRightTable)
					throws ProcessStoppedException {
		ExampleSetBuilder builder = ExampleSets.from(unionAttributeList);

		Attribute[] leftKeyAttributes = null;
		Attribute[] rightKeyAttributes = null;
		Map<DoubleArrayWrapper, List<Example>> rightKeyMapping = null;
		boolean useId = getParameterAsBoolean(PARAMETER_USE_ID);

		leftKeyAttributes = keyAttributes.getFirst();
		rightKeyAttributes = keyAttributes.getSecond();
		if (!useId) {
			// create key mapping for right example set
			rightKeyMapping = createKeyMapping(rightExampleSet, rightKeyAttributes, leftKeyAttributes);
		}

		int progressCounter = 0;
		// iterate over all example from left table and search for matching examples in right table:
		for (Example leftExample : leftExampleSet) {
			List<Example> matchingRightExamples = getMatchingExamples(leftExampleSet, rightExampleSet, leftKeyAttributes,
					rightKeyMapping, useId, leftExample);

			if (matchingRightExamples != null) {
				// add combination of left example and all matching right examples
				for (Example rightExample : matchingRightExamples) {
					addCombinedOccurence(originalAttributeSources, unionAttributeList, builder, leftExample, rightExample);
					if (matchedExamplesInRightTable != null) {
						matchedExamplesInRightTable
						.add(new DoubleArrayWrapper(getKeyValues(rightExample, rightKeyAttributes)));
					}
				}
			} else { // no rows with this key in right table
				// insert this row with null values for the right table
				addLeftOnlyOccurence(originalAttributeSources, unionAttributeList, builder, leftExample);
			}
			// trigger operator progress every 100 examples
			++progressCounter;
			if (progressCounter % 100 == 0) {
				getProgress().step(100);
				progressCounter = 0;
			}
		}
		return builder;
	}

	/**
	 * Performs a right join.
	 *
	 */
	private ExampleSetBuilder performRightJoin(ExampleSet leftExampleSet, ExampleSet rightExampleSet,
			List<AttributeSource> originalAttributeSources, List<Attribute> unionAttributeList,
			Pair<Attribute[], Attribute[]> keyAttributes) throws ProcessStoppedException {
		ExampleSetBuilder builder = ExampleSets.from(unionAttributeList);

		Attribute[] leftKeyAttributes = null;
		Attribute[] rightKeyAttributes = null;
		Map<DoubleArrayWrapper, List<Example>> leftKeyMapping = null;
		boolean useId = getParameterAsBoolean(PARAMETER_USE_ID);

		Attribute leftIdAttribute = null;
		Attribute rightIdAttribute = null;
		if (useId) {
			// needed for getting the right id when adding examples which occur only in right table
			leftIdAttribute = leftExampleSet.getAttributes().getId();
			rightIdAttribute = rightExampleSet.getAttributes().getId();
			leftKeyAttributes = new Attribute[] { leftIdAttribute };
			rightKeyAttributes = new Attribute[] { rightIdAttribute };
		} else {
			// create key mapping for right example set
			leftKeyAttributes = keyAttributes.getFirst();
			rightKeyAttributes = keyAttributes.getSecond();
			leftKeyMapping = createKeyMapping(leftExampleSet, leftKeyAttributes, rightKeyAttributes);
		}

		boolean keepBoth = getParameterAsBoolean(PARAMETER_KEEP_BOTH_JOIN_ATTRIBUTES);
		boolean removeDoubleAttributes = getParameterAsBoolean(PARAMETER_REMOVE_DOUBLE_ATTRIBUTES);

		int progressCounter = 0;
		// iterate over all example from left table and search for matching examples in right table:
		for (Example rightExample : rightExampleSet) {
			List<Example> matchingLeftExamples = getMatchingExamples(rightExampleSet, leftExampleSet, rightKeyAttributes,
					leftKeyMapping, useId, rightExample);

			if (matchingLeftExamples != null) {
				// add combination of left example and all matching right examples
				for (Example leftExample : matchingLeftExamples) {
					addCombinedOccurence(originalAttributeSources, unionAttributeList, builder, leftExample, rightExample);
				}
			} else {
				addRightOnlyOccurence(originalAttributeSources, unionAttributeList, builder, rightExample, leftKeyAttributes,
						rightKeyAttributes, keepBoth, removeDoubleAttributes);
			}
			// trigger operator progress every 100 examples
			++progressCounter;
			if (progressCounter % 100 == 0) {
				getProgress().step(100);
				progressCounter = 0;
			}
		}
		return builder;
	}

	/**
	 * Performs an outer join (not to be confused with a full outer join).
	 *
	 */
	private ExampleSetBuilder performOuterJoin(ExampleSet leftExampleSet, ExampleSet rightExampleSet,
			List<AttributeSource> originalAttributeSources, List<Attribute> unionAttributeList,
			Pair<Attribute[], Attribute[]> keyAttributes) throws ProcessStoppedException {
		ExampleSetBuilder builder;

		Attribute[] leftKeyAttributes = keyAttributes.getFirst();
		Attribute[] rightKeyAttributes = keyAttributes.getSecond();

		// perform left join (an outer join is the union of a left join and a right join on the same
		// tables)
		Set<DoubleArrayWrapper> mappedRightExamples = new HashSet<>();
		builder = performLeftJoin(leftExampleSet, rightExampleSet, originalAttributeSources, unionAttributeList,
				keyAttributes, mappedRightExamples);

		boolean keepBoth = getParameterAsBoolean(PARAMETER_KEEP_BOTH_JOIN_ATTRIBUTES);
		boolean removeDoubleAttributes = getParameterAsBoolean(PARAMETER_REMOVE_DOUBLE_ATTRIBUTES);
		int progressCounter = 0;
		for (Example rightExample : rightExampleSet) {
			// perform right join, but add example only if it has not been matched during left join
			// above
			if (!mappedRightExamples.contains(new DoubleArrayWrapper(getKeyValues(rightExample, rightKeyAttributes)))) {
				addRightOnlyOccurence(originalAttributeSources, unionAttributeList, builder, rightExample, leftKeyAttributes,
						rightKeyAttributes, keepBoth, removeDoubleAttributes);
			}
			// trigger operator progress every 100 examples
			++progressCounter;
			if (progressCounter % 100 == 0) {
				getProgress().step(100);
				progressCounter = 0;
			}
		}
		return builder;
	}

	/**
	 * Creates an example which consists of the combination of leftExample an rightExample. Only
	 * those attributes are added, which are present in originalAttributeSources. The newly
	 * constructed example is added to unionTable.
	 */
	private void addCombinedOccurence(List<AttributeSource> originalAttributeSources, List<Attribute> unionAttributeList,
			ExampleSetBuilder builder, Example leftExample, Example rightExample) {
		double[] unionDataRow = new double[unionAttributeList.size()];
		int attributeIndex = 0;
		for (AttributeSource attributeSource : originalAttributeSources) {
			if (attributeSource.getSource() == AttributeSource.FIRST_SOURCE) {
				unionDataRow[attributeIndex] = leftExample.getValue(attributeSource.getAttribute());
			} else if (attributeSource.getSource() == AttributeSource.SECOND_SOURCE) {
				unionDataRow[attributeIndex] = rightExample.getValue(attributeSource.getAttribute());
			}
			attributeIndex++;
		}
		builder.addRow(unionDataRow);
	}

	/**
	 * Creates an example and adds it to unionTable. The example contains all attributes from
	 * leftExample, which are also in originalAttributeSources, and NaN for all attributes which
	 * should normally be taken from a right example.
	 */
	private void addLeftOnlyOccurence(List<AttributeSource> originalAttributeSources, List<Attribute> unionAttributeList,
			ExampleSetBuilder builder, Example leftExample) {
		double[] unionDataRow = new double[unionAttributeList.size()];
		int attributeIndex = 0;
		for (AttributeSource attributeSource : originalAttributeSources) {
			if (attributeSource.getSource() == AttributeSource.FIRST_SOURCE) {
				unionDataRow[attributeIndex] = leftExample.getValue(attributeSource.getAttribute());
			} else if (attributeSource.getSource() == AttributeSource.SECOND_SOURCE) {
				unionDataRow[attributeIndex] = Double.NaN;
			}
			attributeIndex++;
		}
		builder.addRow(unionDataRow);
	}

	/**
	 * Creates an example and adds it to unionTable. The example contains all attributes from
	 * rightExample, which are also in originalAttributeSources, and NaN for all attributes which
	 * should normally be taken from a left example. Exception: if key attributes would be taken
	 * from left example and only one id attribute is kept, instead of NaN the value of the
	 * corresponding attribute in rightExample is taken.
	 */
	private void addRightOnlyOccurence(List<AttributeSource> originalAttributeSources, List<Attribute> unionAttributeList,
			ExampleSetBuilder builder, Example rightExample, Attribute[] leftKeyAttributes, Attribute[] rightKeyAttributes,
			boolean keepBoth, boolean removeDoubleAttributes) {
		double[] unionDataRow = new double[unionAttributeList.size()];
		int attributeIndex = 0;
		Iterator<Attribute> unionIterator = unionAttributeList.iterator();
		for (AttributeSource attributeSource : originalAttributeSources) {
			Attribute unionAttribute = unionIterator.next();
			if (attributeSource.getSource() == AttributeSource.FIRST_SOURCE) {
				// since keys attributes are always taken from left example set, ID value must be
				// fetched
				// from right example set explicitly

				// find key id
				int id = -1;
				for (int i = 0; i < leftKeyAttributes.length; ++i) {
					if (attributeSource.getAttribute() == leftKeyAttributes[i]) {
						id = i;
						break;
					}
				}

				// now use correct key attribute
				if (id >= 0) {
					boolean sameName = leftKeyAttributes[id].getName().equals(rightKeyAttributes[id].getName());
					if (keepBoth && !(removeDoubleAttributes && sameName)) {
						unionDataRow[attributeIndex] = Double.NaN;
					} else {
						if (leftKeyAttributes[id].isNominal()) {
							// consider different mapping in left and right attribute
							Attribute rightAttribute = rightKeyAttributes[id];
							int rightIndex = (int) rightExample.getValue(rightAttribute);
							String valueAsString = rightAttribute.getMapping().mapIndex(rightIndex);
							int leftIndex = unionAttribute.getMapping().mapString(valueAsString);
							unionDataRow[attributeIndex] = leftIndex;
						} else {
							unionDataRow[attributeIndex] = rightExample.getValue(rightKeyAttributes[id]);
						}
					}
				} else {
					unionDataRow[attributeIndex] = Double.NaN;
				}
			} else if (attributeSource.getSource() == AttributeSource.SECOND_SOURCE) {
				unionDataRow[attributeIndex] = rightExample.getValue(attributeSource.getAttribute());
			}
			attributeIndex++;
		}
		builder.addRow(unionDataRow);
	}

	/**
	 * Maps all values of the keyAttributes which occur in exampleSet to a list of matching
	 * examples.
	 *
	 * @param exampleSet
	 *            The example set for whose key attributes the mapping is created
	 * @param keyAttributes
	 *            the attributes which resemble the key attributes
	 * @param matchKeyAttributes
	 *            if not null, the values of nominal keyAttributes are mapped to match the mapping
	 *            of these attributes prior to adding them to the map
	 * @return
	 */
	private Map<DoubleArrayWrapper, List<Example>> createKeyMapping(ExampleSet exampleSet, Attribute[] keyAttributes,
			Attribute[] matchKeyAttributes) {
		Map<DoubleArrayWrapper, List<Example>> keyMapping = new HashMap<>();

		assert keyAttributes.length == matchKeyAttributes.length;

		// create mapping from nominal values of keyAttributes to matchKeyAttributes
		Map<Attribute, Map<Double, Double>> valueMapping = null;
		if (matchKeyAttributes != null) {
			valueMapping = new HashMap<>();
			for (int attributeNumber = 0; attributeNumber < keyAttributes.length; ++attributeNumber) {
				if (keyAttributes[attributeNumber].isNominal()) {
					Map<Double, Double> valueMap = new HashMap<>();
					// TODO: iterate over getMappint().values() rather than relying on the
					// assumption that values appear in increasing order
					for (int valueNumber = 0; valueNumber < keyAttributes[attributeNumber].getMapping()
							.size(); ++valueNumber) {
						String valueString = keyAttributes[attributeNumber].getMapping().mapIndex(valueNumber);
						valueMap.put((double) valueNumber,
								(double) matchKeyAttributes[attributeNumber].getMapping().mapString(valueString));
					}
					valueMapping.put(keyAttributes[attributeNumber], valueMap);
				}
			}
		}

		double[] keyValues;

		for (Example example : exampleSet) {
			boolean continueIteration = false;
			// fetch key values from example
			keyValues = getKeyValues(example, keyAttributes);
			if (valueMapping != null) {
				// remap keyValues to match values of other attributes:
				for (int i = 0; i < keyValues.length; ++i) {
					if (Double.isNaN(keyValues[i])) {
						continueIteration = true;
						break;
					}
					if (keyAttributes[i].isNominal()) {
						keyValues[i] = valueMapping.get(keyAttributes[i]).get(keyValues[i]);
					}
				}
				if (continueIteration) {
					continue;
				}

			}

			// check if this key is in keyMapping. If not, add:
			List<Example> keyExamples = keyMapping.get(new DoubleArrayWrapper(keyValues));
			if (keyExamples != null) {
				// add current example:
				keyExamples.add(example);
			} else {
				// create set and add to keyMapping:
				keyExamples = new LinkedList<>();
				keyExamples.add(example);
				keyMapping.put(new DoubleArrayWrapper(keyValues), keyExamples);
			}
		}
		;
		return keyMapping;
	}

	/**
	 * Gets examples from secondExampleSet which match the values of the keyAttributes from
	 * firstExample. If PARAMETER_USE_ID_FOR_JOIN is true, the standard id-mapping of example sets
	 * is used. If not, secondKeyMapping is used (@see createKeyMapping())
	 *
	 */
	private List<Example> getMatchingExamples(ExampleSet firstExampleSet, ExampleSet secondExampleSet,
			Attribute[] firstKeyAttributes, Map<DoubleArrayWrapper, List<Example>> secondKeyMapping, boolean useId,
			Example referenceExample) {
		// find right examples matching current left example:
		List<Example> matchingExamples = null;
		if (useId) {
			// use existent id mapping of right example set
			Attribute firstIdAttribute = firstExampleSet.getAttributes().getId();
			Attribute secondIdAttribute = secondExampleSet.getAttributes().getId();
			double firstIdValue = referenceExample.getValue(firstIdAttribute);
			// firstIdValue is NaN if the first value in the id column is a missing value
			if (Double.isNaN(firstIdValue)) {
				return null;
			}
			int[] matchingExampleIndices = null;
			if (firstIdAttribute.isNominal()) {
				matchingExampleIndices = secondExampleSet.getExampleIndicesFromId(
						secondIdAttribute.getMapping().getIndex(firstIdAttribute.getMapping().mapIndex((int) firstIdValue)));
			} else {
				matchingExampleIndices = secondExampleSet.getExampleIndicesFromId(firstIdValue);
			}
			if (matchingExampleIndices != null) {
				matchingExamples = new LinkedList<>();
				for (int secondExampleIndex : matchingExampleIndices) {
					Example matchingExample = secondExampleSet.getExample(secondExampleIndex);
					matchingExamples.add(matchingExample);
				}
			}
		} else {
			// use previously created mapping
			double[] leftKeyValues = getKeyValues(referenceExample, firstKeyAttributes);
			matchingExamples = secondKeyMapping.get(new DoubleArrayWrapper(leftKeyValues));
		}
		return matchingExamples;
	}

	/**
	 * Returns an array of doubles, which contains the values of the keyAttributes of example.
	 */
	private double[] getKeyValues(Example example, Attribute[] keyAttributes) {
		int numKeys = keyAttributes.length;
		double[] keyValues = new double[numKeys];
		for (int i = 0; i < numKeys; ++i) {
			keyValues[i] = example.getValue(keyAttributes[i]);
		}
		return keyValues;
	}

	/**
	 * Returns all attributes from the right example which are key attributes.
	 *
	 * As the values of the key attributes of left and right example set are always the same, only
	 * one set of key attributes is necessary. This is taken from the left example set. Thus, the
	 * right key attributes are excluded.
	 */
	@Override
	protected Set<Pair<Integer, Attribute>> getExcludedAttributes(ExampleSet leftExampleSet, ExampleSet rightExampleSet)
			throws OperatorException {
		if (getParameterAsBoolean(PARAMETER_KEEP_BOTH_JOIN_ATTRIBUTES)) {
			return Collections.emptySet();
		} else {
			Attribute[] keyAttributes = getKeyAttributes(leftExampleSet, rightExampleSet).getSecond();
			Set<Pair<Integer, Attribute>> excludedAttributes = new HashSet<>();
			for (Attribute attribute : keyAttributes) {
				excludedAttributes.add(new Pair<>(AttributeSource.SECOND_SOURCE, attribute));
			}
			return excludedAttributes;
		}
	}

	/**
	 * Returns the metadata from all attributes from the right example which are key attributes.
	 *
	 * As the values of the key attributes of left and right example set are always the same, only
	 * one set of key attributes is necessary. This is taken from the left example set metadata.
	 * Thus, the right key attributes are excluded.
	 */
	@Override
	protected Set<Pair<Integer, AttributeMetaData>> getExcludedAttributesMD(ExampleSetMetaData leftExampleSetMD,
			ExampleSetMetaData rightExampleSetMD) throws OperatorException {
		Pair<AttributeMetaData[], AttributeMetaData[]> keyAttributeMD = getKeyAttributesMD(leftExampleSetMD,
				rightExampleSetMD);
		if (keyAttributeMD == null) {
			return Collections.emptySet();
		}
		AttributeMetaData[] keyAttributes = keyAttributeMD.getSecond();
		Set<Pair<Integer, AttributeMetaData>> excludedAttributes = new HashSet<>();
		for (int i = 0; i < keyAttributes.length; ++i) {
			excludedAttributes.add(new Pair<>(AttributeSource.SECOND_SOURCE, keyAttributes[i]));
		}
		return excludedAttributes;
	}

	@Override
	protected boolean isIdNeeded() {
		return getParameterAsBoolean(PARAMETER_USE_ID);
	}

	@Override
	public List<ParameterType> getParameterTypes() {
		List<ParameterType> types = super.getParameterTypes();
		types.add(new ParameterTypeCategory(PARAMETER_JOIN_TYPE, "Specifies which join should be executed.", JOIN_TYPES,
				JOIN_TYPE_INNER, false));
		types.add(
				new ParameterTypeBoolean(PARAMETER_USE_ID, "Indicates if the id attribute is used for join.", true, false));
		ParameterType joinAttributes = new ParameterTypeList(PARAMETER_JOIN_ATTRIBUTES,
				"The attributes which shall be used for join. Attributes which shall be matched must be of the same type.",
				new ParameterTypeAttribute(PARAMETER_LEFT_ATTRIBUTE_FOR_JOIN,
						"The attribute in the left example set to be used for the join.",
						getInputPorts().getPortByName(LEFT_EXAMPLE_SET_INPUT), true),
				new ParameterTypeAttribute(PARAMETER_RIGHT_ATTRIBUTE_FOR_JOIN,
						"The attribute in the left example set to be used for the join.",
						getInputPorts().getPortByName(RIGHT_EXAMPLE_SET_INPUT), true),
				false);
		joinAttributes.registerDependencyCondition(new BooleanParameterCondition(this, PARAMETER_USE_ID, true, false));

		types.add(joinAttributes);

		ParameterType keepBoth = new ParameterTypeBoolean(PARAMETER_KEEP_BOTH_JOIN_ATTRIBUTES,
				"If checked, both columns of a join pair will be kept. Usually this is unneccessary since both attributes are identical.",
				false, true);
		types.add(keepBoth);

		return types;
	}

	@Override
	public ResourceConsumptionEstimator getResourceConsumptionEstimator() {
		return OperatorResourceConsumptionHandler.getResourceConsumptionEstimator(getInputPorts().getPortByIndex(0),
				ExampleSetJoin.class, null);
	}
}
