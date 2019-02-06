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

import java.util.Objects;

/**
 * Simple wrapper {@link RuntimeException} for {@link OperatorException OperatorExceptions}.
 * Implements {@link OperatorRuntimeException} for improved error handling.
 * The given {@link OperatorException} will be the cause with no parent stack trace and provides the error message.
 *
 * @author Jan Czogalla
 * @since 8.2
 */
public class WrapperOperatorRuntimeException extends RuntimeException implements OperatorRuntimeException {

	private final OperatorException operatorException;

	public WrapperOperatorRuntimeException(OperatorException operatorException) {
		super(operatorException);
		this.operatorException = Objects.requireNonNull(operatorException);
		// ignore stack trace of this throwable, just use cause
		this.setStackTrace(new StackTraceElement[0]);
	}

	@Override
	public String getMessage() {
		return operatorException.getMessage();
	}

	@Override
	public OperatorException toOperatorException() {
		return operatorException;
	}
}
