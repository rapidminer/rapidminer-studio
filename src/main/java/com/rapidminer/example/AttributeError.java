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

package com.rapidminer.example;

import java.io.Serializable;

import com.rapidminer.operator.OperatorException;
import com.rapidminer.operator.UserError;


/**
 * POJO to capture the information on an attribute related runtime exception. Can create an {@link OperatorException}
 * from the captured information.
 *
 * @author Jan Czogalla
 * @since 8.2
 */
class AttributeError implements Serializable {

	String baseKey;
	String name;
	boolean isRole;
	boolean isUserError;

	/**
	 * Creates an {@link OperatorException} and associated the specified stacktrace with it. Can differentiate between
	 * {@link UserError} and {@link OperatorException}.
	 *
	 * @param stackTrace
	 * 		the stacktrace to be attached to the returned excpetion
	 * @return an operator exception or user error
	 */
	OperatorException toOperatorException(StackTraceElement[] stackTrace) {
		String actualKey = baseKey + (isRole ? "_role" : "");
		OperatorException exception;
		if (isUserError) {
			exception = new UserError(null, actualKey, name);
		} else {
			exception = new OperatorException(actualKey, null, name);
		}
		exception.setStackTrace(stackTrace);
		return exception;
	}
}
