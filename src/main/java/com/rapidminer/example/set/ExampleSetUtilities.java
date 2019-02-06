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

import java.util.Arrays;
import java.util.Comparator;
import java.util.List;

import com.rapidminer.example.Attribute;
import com.rapidminer.example.AttributeRole;
import com.rapidminer.example.Attributes;
import com.rapidminer.example.ExampleSet;
import com.rapidminer.operator.Operator;
import com.rapidminer.operator.UserError;
import com.rapidminer.tools.Ontology;


/**
 * This class provides global utility methods for regular operations on ExampleSets or their
 * components.
 *
 * @author Dominik Halfkann
 */
public class ExampleSetUtilities {

	/**
	 * This {@link Comparator} compares {@link AttributeRole}s according to their Role. It can be
	 * used to sort the (special) Attributes of an {@link ExampleSet}.
	 *
	 * It enforces the following order for Attributes: ID -> Label -> Prediction -> Confidence ->
	 * Cluster -> Weight -> Other Special Attributes -> Other Regular Attributes
	 */
	public static final Comparator<AttributeRole> SPECIAL_ATTRIBUTES_ROLE_COMPARATOR = new Comparator<AttributeRole>() {

		private List<String> priorityList = Arrays.asList(new String[] { Attributes.ID_NAME, Attributes.LABEL_NAME,
				Attributes.PREDICTION_NAME, Attributes.CONFIDENCE_NAME, Attributes.CLUSTER_NAME, Attributes.WEIGHT_NAME });

		@Override
		public int compare(AttributeRole a1, AttributeRole a2) {
			// the lower the priority, the earlier the attribute is being sorted
			// special attributes should come before regular attributes
			int priorityAttribute1 = a1.isSpecial() ? 1000 : 2000;
			int priorityAttribute2 = a2.isSpecial() ? 1000 : 2000;

			// if the attribute role is in the priority list, use special priority
			if (a1.isSpecial() && priorityList.contains(a1.getSpecialName())) {
				priorityAttribute1 = priorityList.indexOf(a1.getSpecialName());
			}
			if (a2.isSpecial() && priorityList.contains(a2.getSpecialName())) {
				priorityAttribute2 = priorityList.indexOf(a2.getSpecialName());
			}

			// special priority for roles that start with "confidence_"
			if (a1.isSpecial() && a1.getSpecialName().startsWith(Attributes.CONFIDENCE_NAME + "_")) {
				priorityAttribute1 = priorityList.indexOf(Attributes.CONFIDENCE_NAME);
			}
			if (a2.isSpecial() && a2.getSpecialName().startsWith(Attributes.CONFIDENCE_NAME + "_")) {
				priorityAttribute2 = priorityList.indexOf(Attributes.CONFIDENCE_NAME);
			}

			return priorityAttribute1 - priorityAttribute2;
		}

	};

	/** Determines how to compare the sets of Attributes. */
	public static enum SetsCompareOption {
		/** Both sets of Attributes must be the same. */
		EQUAL,
		/** The second set of Attributes must be a subset of the first one. */
		ALLOW_SUBSET,
		/** The second set of Attributes must be a superset of the first one. */
		ALLOW_SUPERSET,
		/** Just compare matching Attributes from both sets (intersection). */
		USE_INTERSECTION
	}

	/** Determines how to compare the matching Attributes regarding their types. */
	public static enum TypesCompareOption {
		/**
		 * An Attribute from the second set must be of the same type as the corresponding Attribute
		 * from the first set.
		 */
		EQUAL,
		/**
		 * An Attribute from the second set must be a subtype of the corresponding Attribute from
		 * the first set.
		 */
		ALLOW_SUBTYPES,
		/**
		 * An Attribute from the second set must be a supertype of the corresponding Attribute from
		 * the first set.
		 */
		ALLOW_SUPERTYPES,
		/**
		 * An Attribute from the second set must have the same parent as the corresponding Attribute
		 * from the first set.
		 */
		ALLOW_SAME_PARENTS,
		/** ignores different Types of the Attributes */
		DONT_CARE
	}

	/**
	 * Check if two sets of Attributes are matching. Throws an {@link UserError} if they are not
	 * equal with regard to the specified {@link SetsCompareOption} and {@link TypesCompareOption}.
	 */
	public static void checkAttributesMatching(Operator op, Attributes originalAttributes, Attributes comparedAttributes,
			SetsCompareOption compareSets, TypesCompareOption compareTypes) throws UserError {

		for (Attribute originalAttribute : originalAttributes) {
			if (comparedAttributes.contains(originalAttribute)) {
				Attribute comparedAttribute = comparedAttributes.get(originalAttribute.getName());
				int originalValueType = originalAttribute.getValueType();
				int comparedValueType = comparedAttribute.getValueType();
				if (originalValueType != comparedValueType) {
					Ontology valueTypes = Ontology.ATTRIBUTE_VALUE_TYPE;
					if (compareTypes == TypesCompareOption.ALLOW_SUBTYPES) {
						if (!valueTypes.isA(comparedValueType, originalValueType)) {
							throw new UserError(op, 964, comparedAttribute.getName(),
									Ontology.VALUE_TYPE_NAMES[comparedValueType],
									Ontology.VALUE_TYPE_NAMES[originalValueType]);
						}
					} else if (compareTypes == TypesCompareOption.ALLOW_SUPERTYPES) {
						if (!valueTypes.isA(originalValueType, comparedValueType)) {
							throw new UserError(null, 965, comparedAttribute.getName(),
									Ontology.VALUE_TYPE_NAMES[comparedValueType],
									Ontology.VALUE_TYPE_NAMES[originalValueType]);
						}
					} else if (compareTypes == TypesCompareOption.ALLOW_SAME_PARENTS) {
						/*
						 * Calculate parents. If one parent is equal to ATTRIBUITE_VALUE or less (no
						 * parent at all) then take the origin value.
						 */
						int parentOriginal = valueTypes.getParent(originalValueType);
						parentOriginal = parentOriginal <= Ontology.ATTRIBUTE_VALUE ? originalValueType : parentOriginal;
						int parentCompared = valueTypes.getParent(comparedValueType);
						parentCompared = parentCompared <= Ontology.ATTRIBUTE_VALUE ? comparedValueType : parentCompared;

						if (!valueTypes.isA(parentCompared, parentOriginal)) {
							throw new UserError(op, 965, comparedAttribute.getName(),
									Ontology.VALUE_TYPE_NAMES[comparedValueType],
									Ontology.VALUE_TYPE_NAMES[originalValueType]);
						}
					} else if (compareTypes == TypesCompareOption.EQUAL) {
						throw new UserError(op, 963, comparedAttribute.getName(),
								Ontology.VALUE_TYPE_NAMES[originalValueType], Ontology.VALUE_TYPE_NAMES[comparedValueType]);
					}
				}
			} else {
				if (compareSets == SetsCompareOption.EQUAL) {
					throw new UserError(op, 960, originalAttribute.getName());
				}
				if (compareSets == SetsCompareOption.ALLOW_SUPERSET) {
					throw new UserError(op, 962, originalAttribute.getName());
				}
			}
		}

		for (Attribute comparedAttribute : comparedAttributes) {
			if (!originalAttributes.contains(comparedAttribute)) {
				if (compareSets == SetsCompareOption.EQUAL) {
					throw new UserError(op, 960, comparedAttribute.getName());
				}
				if (compareSets == SetsCompareOption.ALLOW_SUBSET) {
					throw new UserError(op, 961, comparedAttribute.getName());
				}
			}
		}

	}
}
