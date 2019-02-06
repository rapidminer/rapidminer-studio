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
package com.rapidminer.parameter;

import com.rapidminer.operator.Operator;


/**
 * This Exception will be thrown if an Macro was not defined.
 *
 * @author Thilo Kamradt
 *
 */
public class UndefinedMacroError extends UndefinedParameterError {

	private static final long serialVersionUID = 1547250316954515775L;

	/**
	 * The default constructor for missing macro errors with error code 227.
	 *
	 * @param macroKey
	 *            the key of the missing macro
	 */
	public UndefinedMacroError(String parameterKey, String macroKey) {
		super(null, 227, parameterKey, macroKey);
	}

	/**
	 *
	 *
	 * @param operator
	 *            the executing Operator which performs the action or null
	 * @param code
	 *            errorID of the UserErrorMessage which should be shown
	 * @param additionalText
	 *            text to paste in the UserErrorMessage
	 */
	public UndefinedMacroError(Operator operator, String key, String additionalText) {
		super(operator, key, additionalText);
	}

}
