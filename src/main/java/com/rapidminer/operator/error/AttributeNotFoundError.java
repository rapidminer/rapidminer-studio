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

import com.rapidminer.operator.Operator;
import com.rapidminer.operator.UserError;


/**
 * This exception will be thrown if an attribute was specified in the parameters of an operator but
 * was not found in the data.
 *
 * @author Marco Boeck
 * @since 6.5.0
 *
 */
public class AttributeNotFoundError extends ParameterError {

	/** if the attribute could neither be found in regular nor in special attributes */
	public static final int ATTRIBUTE_NOT_FOUND = 160;

	/** if the attribute could not be found in regular attributes */
	public static final int ATTRIBUTE_NOT_FOUND_IN_REGULAR = 164;

	private static final long serialVersionUID = 4107157631726397970L;

	private final String attributeName;

	/**
	 * Throw if the parameter of an operator specifies an attribute which cannot be found in the
	 * input data.
	 *
	 * @param operator
	 *            the operator in question
	 * @param key
	 *            the parameter key for which the error occured
	 * @param attributeName
	 *            the name of the attribute
	 */
	public AttributeNotFoundError(Operator operator, String key, String attributeName) {
		this(operator, ATTRIBUTE_NOT_FOUND, key, attributeName);
	}

	/**
	 * Throw if the parameter of an operator specifies an attribute which cannot be found in the
	 * input data.
	 *
	 * @param operator
	 *            the operator in question
	 * @param code
	 *            the error code, see class constants
	 * @param key
	 *            the parameter key for which the error occured
	 * @param attributeName
	 *            the name of the attribute
	 */
	public AttributeNotFoundError(Operator operator, int code, String key, String attributeName) {
		super(operator, code, key, new Object[]{attributeName});
		if (attributeName == null) {
			this.attributeName = "";
		} else {
			this.attributeName = attributeName;
		}
	}

	/**
	 * @return the name of the attribute which was not found
	 */
	public String getAttributeName() {
		return attributeName;
	}

}
