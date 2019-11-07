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
package com.rapidminer;

import com.rapidminer.operator.OperatorException;
import com.rapidminer.tools.Tools;


/**
 * An exception caused outside an operator which is <i>not</i> a bug, but caused by the user.
 * 
 * Unfortunately, this class doubles most of the code of {@link com.rapidminer.operator.UserError}.
 * 
 * @author Simon Fischer, Ingo Mierswa
 * @deprecated since 9.5.0 not used anywhere
 */
@Deprecated
public class NoOpUserError extends Exception implements NoBugError {

	private static final long serialVersionUID = -686838060355434724L;

	private int code;

	/**
	 * Creates a new NoOpUserError.
	 * 
	 * @param cause
	 *            The exception that caused the user error. May be null. Using this makes debugging
	 *            a lot easier.
	 * @param code
	 *            The error code referring to a message in the file
	 *            <code>UserErrorMessages.properties</code>
	 * @param arguments
	 *            Arguments for the short message.
	 */
	public NoOpUserError(Throwable cause, int code, Object[] arguments) {
		super(OperatorException.getErrorMessage("" + code, arguments), cause);
		this.code = code;
	}

	/** Convenience constructor for messages with no arguments and cause. */
	public NoOpUserError(Throwable cause, int code) {
		this(code, new Object[0], cause);
	}

	public NoOpUserError(int code, Object[] arguments) {
		this(null, code, arguments);
	}

	/** Convenience constructor for messages with no arguments. */
	public NoOpUserError(int code) {
		this(null, code, new Object[0]);
	}

	/** Convenience constructor for messages with exactly one argument. */
	public NoOpUserError(int code, Object argument1) {
		this(null, code, new Object[] { argument1 });
	}

	/**
	 * Convenience constructor for messages with exactly one arguments and cause.
	 */
	public NoOpUserError(Throwable cause, int code, Object argument1) {
		this(cause, code, new Object[] { argument1 });
	}

	/** Convenience constructor for messages with exactly two arguments. */
	public NoOpUserError(int code, Object argument1, Object argument2) {
		this(null, code, new Object[] { argument1, argument2 });
	}

	@Override
	public String getDetails() {
		return OperatorException.getResourceString("" + code, "long", "Description missing.");
	}

	@Override
	public String getErrorName() {
		return OperatorException.getResourceString("" + code, "name", "Unnamed error.");
	}

	@Override
	public int getCode() {
		return code;
	}

	@Override
	public String getHTMLMessage() {
		return "<html>Error occured:<br>" + Tools.escapeXML(getMessage()) + "<hr>" + Tools.escapeXML(getDetails())
				+ "</html>";
	}
}
