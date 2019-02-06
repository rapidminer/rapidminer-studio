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

import com.rapidminer.operator.ports.Port;


/**
 * Subclass of a {@link UserError} which is thrown when the cause of the error is a {@link Port}.
 *
 * @author Marco Boeck
 * @since 6.5.0
 *
 */
public class PortUserError extends UserError {

	private static final long serialVersionUID = 1090006774683438376L;

	/** the port that caused the error */
	private transient Port port;

	/** the expected data type for this port, can be null */
	private transient Class<?> expectedType;

	/** the actually delivered data type for this port, can be null */
	private transient Class<?> actualType;

	/**
	 * Creates a new user error for a port.
	 *
	 * @param port
	 *            The {@link Port} which caused the error.
	 * @param code
	 *            The error code referring to a message in the file
	 *            <code>UserErrorMessages.properties</code>
	 * @param arguments
	 *            Arguments for the short or long message.
	 */
	public PortUserError(Port port, int code, Object... arguments) {
		super(port.getPorts().getOwner().getOperator(), code, arguments);
		this.port = port;
	}

	/**
	 * Creates a new user error for a port.
	 *
	 * @param port
	 *            The {@link Port} which caused the error.
	 * @param errorId
	 *            The error id referring to a message in the file
	 *            <code>UserErrorMessages.properties</code>
	 * @param arguments
	 *            Arguments for the short or long message.
	 */
	public PortUserError(Port port, String errorId, Object... arguments) {
		super(port.getPorts().getOwner().getOperator(), errorId, arguments);
		this.port = port;
	}

	/**
	 * @return the port which caused the error.
	 */
	public Port getPort() {
		return port;
	}

	/**
	 * Setter for the port which causes the error.
	 *
	 * @param port
	 *            the port
	 */
	public void setPort(Port port) {
		this.port = port;
	}

	/**
	 * Sets the expected data type.
	 *
	 * @param expectedType
	 *            the type
	 */
	public void setExpectedType(Class<?> expectedType) {
		this.expectedType = expectedType;
	}

	/**
	 * @return the expected data type for this port. Can be {@code null}
	 */
	public Class<?> getExpectedType() {
		return expectedType;
	}

	/**
	 * Sets the actual delivered data type.
	 *
	 * @param actualType
	 *            the type
	 */
	public void setActualType(Class<?> actualType) {
		this.actualType = actualType;
	}

	/**
	 * @return the actual data type delivered for this port. Can be {@code null}
	 */
	public Class<?> getActualType() {
		return actualType;
	}
}
