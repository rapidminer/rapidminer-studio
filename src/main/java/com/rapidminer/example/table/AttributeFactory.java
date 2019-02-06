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

import com.rapidminer.example.Attribute;
import com.rapidminer.tools.Ontology;

import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.atomic.AtomicInteger;


/**
 * This class is used to create and clone attributes. It should be used to create attributes instead
 * of directly creating them by using constructors. Additionally, it provides some helper methods
 * for attribute creation purposes (name creation, block numbers,...).
 * 
 * @author Ingo Mierswa Exp $
 */
public class AttributeFactory {

	/** The prefix of the name of generated attributes. */
	private static final String GENSYM_PREFIX = "gensym";

	/**
	 * The current highest id counters for generated attribute names. The counter will be increased
	 * each time an attribute name is generated more than once.
	 */
	private static Map<String, AtomicInteger> nameCounters = new HashMap<String, AtomicInteger>();

	static {
		resetNameCounters();
	}

	/** Creates a simple single attribute depending on the given value type. */
	public static Attribute createAttribute(String name, int valueType) {
		String attributeName = (name != null) ? new String(name) : createName();  // we copy the name
																					// if the
																					// underlying
																					// char array
																					// value is
																					// larger than
																					// needed
		if (Ontology.ATTRIBUTE_VALUE_TYPE.isA(valueType, Ontology.DATE_TIME)) {
			return new DateAttribute(attributeName, valueType);
		} else if (Ontology.ATTRIBUTE_VALUE_TYPE.isA(valueType, Ontology.BINOMINAL)) {
			return new BinominalAttribute(attributeName);
		} else if (Ontology.ATTRIBUTE_VALUE_TYPE.isA(valueType, Ontology.NOMINAL)) {
			return new PolynominalAttribute(attributeName, valueType);
		} else if (Ontology.ATTRIBUTE_VALUE_TYPE.isA(valueType, Ontology.NUMERICAL)) {
			return new NumericalAttribute(attributeName, valueType);
		} else {
			throw new RuntimeException("AttributeFactory: cannot create attribute with value type '"
					+ Ontology.ATTRIBUTE_VALUE_TYPE.mapIndex(valueType) + "' (" + valueType + ")!");
		}
	}

	/**
	 * Creates a simple single attribute depending on the given value type. The name is randomly
	 * created. This attribute can also be used for generators to define their desired input
	 * attributes for compatibility checks.
	 */
	public static Attribute createAttribute(int valueType) {
		return createAttribute(createName(), valueType);
	}

	/** Creates a simple attribute depending on the given value type. */
	public static Attribute createAttribute(int valueType, int blockType, String constructionDescription) {
		Attribute attribute = createAttribute(valueType);
		attribute.setBlockType(blockType);
		attribute.setConstruction(constructionDescription);
		return attribute;
	}

	/** Creates a simple attribute depending on the given value type. */
	public static Attribute createAttribute(String name, int valueType, int blockType) {
		Attribute attribute = createAttribute(name, valueType);
		attribute.setBlockType(blockType);
		return attribute;
	}

	// ================================================================================

	/**
	 * Simple clone factory method for attributes. Invokes
	 * {@link #createAttribute(Attribute att, String name)} with name = null.
	 */
	public static Attribute createAttribute(Attribute attribute) {
		return createAttribute(attribute, null);
	}

	/**
	 * Simple clone factory method for attributes. Returns the clone of the given attribute and sets
	 * the function name to the given one if not null. In this case the attribute is used as an
	 * argument of returned attribute. This method might be usefull for example to create a
	 * prediction attribute with the same properties as the original label attribute.
	 */
	public static Attribute createAttribute(Attribute attribute, String functionName) {
		Attribute result = (Attribute) attribute.clone();
		if (functionName == null) {
			result.setName(attribute.getName());
		} else {
			result.setName(functionName + "(" + attribute.getName() + ")");
			result.setConstruction(functionName + "(" + attribute.getName() + ")");
		}
		return result;
	}

	// ================================================================================
	// changes the value type of the given attribute
	// ================================================================================

	/**
	 * Changes the value type of the given attribute and returns a new attribute with the same
	 * properties but the new value type. Since values within examples are not altered it is not
	 * suggested to use this method to change attributes within an exampleset in use. Operators
	 * should create a new attribute to ensure parallel executability.
	 */
	public static Attribute changeValueType(Attribute attribute, int valueType) {
		Attribute result = createAttribute(attribute.getName(), valueType);
		if (attribute.isNominal() && result.isNominal()) {
			result.setMapping(attribute.getMapping());
		}
		result.setTableIndex(attribute.getTableIndex());
		return result;
	}

	// ================================================================================
	// helper methods
	// ================================================================================

	/** Resets the counters for the generated attribute names. */
	public static void resetNameCounters() {
		nameCounters.clear();
	}

	/** Creates a new unsused attribute name. */
	public static String createName() {
		return createName(GENSYM_PREFIX);
	}

	/** Creates a new unsused attribute name with a given prefix. */
	public static String createName(String prefix) {
		AtomicInteger counter = nameCounters.get(prefix);
		if (counter == null) {
			nameCounters.put(prefix, new AtomicInteger(1));
			return prefix;
		} else {
			return prefix + counter.getAndIncrement();
		}
	}
}
