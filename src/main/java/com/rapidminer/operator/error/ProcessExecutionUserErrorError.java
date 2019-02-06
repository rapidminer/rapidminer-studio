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

import com.rapidminer.Process;
import com.rapidminer.operator.Operator;
import com.rapidminer.operator.UserError;


/**
 * This exception will be thrown if a {@link Process} was executed an inside that process an
 * {@link UserError} occurred. Used for example by the {@code Execute Process} operator.
 *
 * @author Marco Boeck
 * @since 6.5.0
 *
 */
public class ProcessExecutionUserErrorError extends UserError {

	private static final long serialVersionUID = 6442456043729234058L;

	/** the user error from the created process */
	private UserError cause;

	/**
	 * Throw if the parameter of an operator specifies an attribute which cannot be found in the
	 * input data.
	 *
	 * @param operator
	 *            the operator which created the process
	 * @param cause
	 *            the user error which occurred inside the process
	 */
	public ProcessExecutionUserErrorError(Operator operator, UserError cause) {
		super(operator, 971, cause.getOperator() != null ? cause.getOperator().getName() : "unknown", operator.getName(),
				cause.getMessage());
		this.cause = cause;
	}

	/**
	 * Returns the cause for this exception.
	 *
	 * @return the causing {@link UserError}; can be {@code null}
	 */
	public UserError getUserError() {
		return cause;
	}
}
