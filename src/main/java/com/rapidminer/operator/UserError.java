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

import com.rapidminer.NoBugError;
import com.rapidminer.tools.Tools;


/**
 * Exception class whose instances are thrown due to a user error, for example missing files or
 * wrong operator architecture. <br>
 * In order to create a UserError, do the following:
 * <ul>
 * <li>Open the file <code>UserErrorMessages.properties</code> in the <code>resources</code>
 * directory. Look for an appropriate message. If you find one, remember its id. If not, create a
 * new one in the correct group</li>
 * <li>The entry must include name, short message and long message. The name and long message will
 * be presented to the user literally. The short message will be parsed by
 * <code>java.text.MessageFormat</code>. Especially, any occurrence of curly brackets will be
 * replaced. Be careful with quotes; it might be a good idea to read the documentation of
 * MessageFormat first.</li>
 * <li>Create a UserError by using this id. If the UserError is created because of another
 * exception, e.g. a FileNotFoundException, this exception should be passed to the UserError in the
 * constructor.</li>
 * </ul>
 * <b>Attention!</b><br>
 * Although the current UserErrorMessages.properties only contain numbers, the current
 * implementation supports arbitrary strings as identifiers. Writers of Extensions are encouraged to
 * use Strings for separating their self defined errors from the errors defined in the core. You
 * should prepend the namespace of the extension like this: error.<extensions
 * namespace>.error_id.short = ...
 *
 * @author Simon Fischer, Ingo Mierswa, Sebastian Land
 */
public class UserError extends OperatorException implements NoBugError {

	private static final long serialVersionUID = -8441036860570180869L;

	private final int code;

	private transient Operator operator;

	/**
	 * Creates a new UserError.
	 *
	 * @param operator
	 *            The {@link Operator} in which the exception occured.
	 * @param cause
	 *            The exception that caused the user error. May be null. Using this makes debugging
	 *            a lot easier.
	 * @param code
	 *            The error code referring to a message in the file
	 *            <code>UserErrorMessages.properties</code>
	 * @param arguments
	 *            Arguments for the short or long message.
	 */
	public UserError(Operator operator, Throwable cause, int code, Object... arguments) {
		super(code + "", cause, arguments);
		this.code = code;
		this.operator = operator;
	}

	/** Convenience constructor for messages with no arguments and cause. */
	public UserError(Operator operator, Throwable cause, int code) {
		this(operator, code, new Object[0], cause);
	}

	public UserError(Operator operator, int code, Object... arguments) {
		this(operator, null, code, arguments);
	}

	/** Convenience constructor for messages with no arguments. */
	public UserError(Operator operator, int code) {
		this(operator, null, code, new Object[0]);
	}

	public UserError(Operator operator, Throwable cause, String errorId, Object... arguments) {
		super(errorId, cause, arguments);
		this.code = -1;
		this.operator = operator;
	}

	/**
	 * Convenience constructor for messages with no arguments and cause. This constructor is in fact
	 * equivalent to the call of the above constructor but must kept for compatibility issues for
	 * existing compiled extensions.
	 */
	public UserError(Operator operator, Throwable cause, String errorId) {
		this(operator, cause, errorId, new Object[0]);
	}

	public UserError(Operator operator, String errorId, Object... arguments) {
		this(operator, null, errorId, arguments);
	}

	/** Convenience constructor for messages with no arguments. */
	public UserError(Operator operator, String errorId) {
		this(operator, null, errorId, new Object[0]);
	}

	@Override
	public String getDetails() {
		return OperatorException.getErrorMessage(getErrorIdentifier(),
				"long",
				"Description missing.",
				getArguments());
	}

	@Override
	public String getErrorName() {
		return OperatorException.getErrorMessage(getErrorIdentifier(),
				"name",
				"Unnamed error.",
				getArguments());
	}

	@Override
	public int getCode() {
		return code;
	}

	@Override
	public String getErrorIdentifier() {
		return super.getErrorIdentifier();
	}

	@Override
	public String getHTMLMessage() {
		return "<html>Error in: <b>" + getOperator() + "</b><br>" + Tools.escapeXML(getMessage()) + "</html>";
	}

	/**
	 * Returns the operator which caused this error
	 *
	 * @return the operator associated with this exception
	 */
	public Operator getOperator() {
		return operator;
	}

	/**
	 * Sets the operator which caused this error
	 *
	 * @param operator the operator
	 */
	public void setOperator(Operator operator) {
		this.operator = operator;
	}

	/**
	 * Returns the short message for the given error code
	 *
	 * @param code the error code
	 * @param arguments the arguments for the message
	 * @return the formatted message or "No message."
	 * @deprecated since 9.5.0 use {@link OperatorException#getErrorMessage(String, Object[])} instead
	 */
	@Deprecated
	public static String getErrorMessage(int code, Object[] arguments) {
		return getErrorMessage("" + code, arguments);
	}

	/**
	 * Returns the short message for the given error code
	 *
	 * @param identifier the error identifier
	 * @param arguments the arguments for the message
	 * @return the formatted message or "No message."
	 * @deprecated since 9.5.0 use {@link OperatorException#getErrorMessage(String, Object[])} instead
	 */
	@Deprecated
	public static String getErrorMessage(String identifier, Object[] arguments) {
		return OperatorException.getErrorMessage(identifier, arguments);
	}

	/**
	 * Returns a resource message for the given error code.
	 *
	 * @param key
	 *            one out of &quot;name&quot;, &quot;short&quot;, &quot;long&quot;
	 * @return the unformatted resource string
	 * @deprecated since 9.5.0 use {@link OperatorException#getResourceString} instead
	 */
	@Deprecated
	public static String getResourceString(int code, String key, String deflt) {
		return getResourceString(code + "", key, deflt);
	}

	/**
	 * This returns a resource message of the internationalized error messages identified by an id.
	 * Compared to the legacy method {@link #getResourceString(int, String, String)} this supports a
	 * more detailed identifier. This makes it easier to ensure extensions don't reuse already
	 * defined core errors. It is common sense to add the extensions namespace identifier as second
	 * part of the key, just after error. For example: error.rmx_web.operator.unusable = This
	 * operator {0} is unusable.
	 *
	 * @param id
	 *            The identifier of the error. "error." will be automatically prepended-
	 * @param key
	 *            The part of the error description that should be shown.
	 * @param deflt
	 *            The default if no resource bundle is available.
	 * @deprecated since 9.5.0 use OperatorException#getResourceString instead
	 */
	@Deprecated
	public static String getResourceString(String id, String key, String deflt) {
		return OperatorException.getResourceString(id, key, deflt);
	}

}
