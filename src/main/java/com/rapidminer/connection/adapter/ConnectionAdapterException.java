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
package com.rapidminer.connection.adapter;

import com.rapidminer.connection.configuration.ConnectionConfiguration;
import com.rapidminer.connection.util.ValidationResult;
import com.rapidminer.operator.Operator;
import com.rapidminer.operator.UserError;

/**
 * A special {@link UserError} that contains a {@link ValidationResult}. Used when testing and retrieving
 * {@link ConnectionAdapter ConnectionAdapters}.
 *
 * @author Jan Czogalla
 * @since 9.3
 */
public class ConnectionAdapterException extends UserError {

	private final ValidationResult validation;

	/**
	 * Creates a new {@link ConnectionAdapterException} associated with an {@link Operator},
	 * a {@link ConnectionConfiguration} and a {@link ValidationResult}.
	 *
	 * @param operator
	 * 		the operator that the error occurred in; might be {@code null}
	 * @param configuration
	 * 		the configuration that the error occurred for; must not be {@code null}
	 * @param validation
	 * 		the validation result associated with the error; might be {@code null}
	 */
	public ConnectionAdapterException(Operator operator, ConnectionConfiguration configuration, ValidationResult validation) {
		super(operator, "connection.adapter.validation_error", configuration.getName(), configuration.getType());
		this.validation = validation;
	}

	/** @return the associated {@link ValidationResult}; might be {@code null} */
	public ValidationResult getValidation() {
		return validation;
	}
}
