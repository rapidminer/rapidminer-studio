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
package com.rapidminer.tools.expression;



/**
 * Exception that happens while parsing a string expression or checking its syntax. Can contain a
 * {@link ExpressionParsingException} or a subclass determining the reason for the parsing error.
 * See {@link ExpressionParsingException} for standard marker subclasses.
 *
 * @author Gisa Schaefer
 * @since 6.5.0
 */
public class ExpressionException extends Exception {

	private static final long serialVersionUID = 1566969757994992388L;
	private final int errorLine;

	/**
	 * Creates an ExpressionException with a line where the error happened and an associated error
	 * message.
	 *
	 * @param message
	 *            the error message
	 * @param line
	 *            the line where the error occurred
	 */
	public ExpressionException(String message, int line) {
		super(message);
		errorLine = line;
	}

	/**
	 * Creates an ExpressionException with associated error message and unknown line for where the
	 * error happened.
	 *
	 * @param message
	 *            the error message
	 */
	public ExpressionException(String message) {
		this(message, -1);
	}

	/**
	 * Creates an ExpressionException with the cause, associated error message and unknown line for
	 * where the error happened.
	 *
	 * @param cause
	 *            the cause
	 */
	public ExpressionException(ExpressionParsingException cause) {
		super(cause.getMessage(), cause);
		if (cause.getErrorContext() != null) {
			errorLine = cause.getErrorContext().getStart().getLine();
		} else {
			errorLine = -1;
		}
	}

	/**
	 * @return the line of the error or -1 if the error line is unknown
	 */
	public int getErrorLine() {
		return errorLine;

	}

	/**
	 * Returns only the first sentence of the error message. Does not return where exactly the error
	 * lies as {{@link #getMessage()} may.
	 *
	 * @return the first sentence of the error message
	 */
	public String getShortMessage() {
		String message = super.getMessage();
		return message == null ? null : message.split("\n")[0];
	}

}
