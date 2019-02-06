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
package com.rapidminer.operator.error;

import com.rapidminer.example.Attribute;
import com.rapidminer.operator.Operator;
import com.rapidminer.operator.UserError;
import com.rapidminer.tools.Ontology;


/**
 * This exception will be thrown if the attribute selected in the parameters of an operator is of
 * the wrong type
 *
 *
 * @author Joao Pedro Pinheiro
 *
 * @since 7.3.0
 */
public class AttributeWrongTypeError extends UserError {

	/** Error code for when the attribute is of the wrong type */
	public static final int ATTRIBUTE_WRONG_TYPE = 120;

	private static final long serialVersionUID = 1L;

	/**
	 * Throw if the parameter of an operator specifies an attribute which is of the wrong type
	 *
	 * @param operator
	 *            the operator in question
	 * @param attribute
	 *            the attribute that caused the error to be thrown
	 * @param valueTypes
	 *            the accepted parameter types for the operator in question
	 */
	public AttributeWrongTypeError(Operator operator, Attribute attribute, int... valueTypes) {
		super(operator, ATTRIBUTE_WRONG_TYPE, attribute.getName(),
				Ontology.ATTRIBUTE_VALUE_TYPE.mapIndexToDisplayName(attribute.getValueType()), makeReadable(valueTypes));
	}

	/**
	 * Method to create a string from the indexes of value types
	 *
	 * @param types
	 *            The value types to be converted to human-readable format
	 * @return the readable string, ready to be displayed
	 */
	private static String makeReadable(int... types) {
		String readable = "";
		if (types.length > 0) {
			readable += Ontology.ATTRIBUTE_VALUE_TYPE.mapIndexToDisplayName(types[0]);
		}

		for (int i = 1; i < types.length; i++) {
			readable += " or " + Ontology.ATTRIBUTE_VALUE_TYPE.mapIndexToDisplayName(types[i]);
		}

		return readable;
	}
}
