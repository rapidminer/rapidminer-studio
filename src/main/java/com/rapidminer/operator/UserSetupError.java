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
package com.rapidminer.operator;

import com.rapidminer.tools.ValidationUtil;


/**
 * A {@link UserError} that contains a {@link ProcessSetupError} and can be used to either show that as an
 * operator warning or as a user error at runtime.
 *
 * @author Jan Czogalla
 * @since 9.3.1
 */
public class UserSetupError extends UserError {

	private final ProcessSetupError setupError;

	/**
	 * Creates a {@link UserError} wrapper for the given {@link ProcessSetupError}.
	 *
	 * @param operator
	 * 		the operator that this error was created for; can be {@code null}
	 * @param setupError
	 * 		the source setup error; must not be {@code null}
	 */
	public UserSetupError(Operator operator, ProcessSetupError setupError) {
		super(operator, 127, ValidationUtil.requireNonNull(setupError, "setup error").getMessage());
		this.setupError = setupError;
	}

	/**
	 * @return the contained setup error; never {@code null}
	 */
	public ProcessSetupError getSetupError() {
		return setupError;
	}
}
