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
package com.rapidminer.tools.mail.connection;

import java.util.List;
import java.util.Map;
import java.util.function.Predicate;

import com.rapidminer.connection.ConnectionInformation;
import com.rapidminer.connection.util.ConnectionInformationSelector;
import com.rapidminer.connection.util.ConnectionSelectionProvider;
import com.rapidminer.connection.valueprovider.handler.ValueProviderHandlerRegistry;
import com.rapidminer.operator.MailNotSentException;
import com.rapidminer.operator.Operator;
import com.rapidminer.operator.OperatorVersion;
import com.rapidminer.operator.ProcessSetupError;
import com.rapidminer.operator.ProcessSetupError.Severity;
import com.rapidminer.operator.UserError;
import com.rapidminer.operator.ports.metadata.ConnectionInformationMetaData;
import com.rapidminer.operator.ports.metadata.SimpleMetaDataError;
import com.rapidminer.parameter.ParameterType;
import com.rapidminer.parameter.conditions.AboveOperatorVersionCondition;
import com.rapidminer.tools.MailUtilities;
import com.rapidminer.tools.ParameterService;


/**
 * A collection of methods regarding mails using the new {@link ConnectionInformation} mechanism.
 *
 * @author Jan Czogalla
 * @since 9.4.1
 */
public final class MailConnectionUtilities {

	public static final OperatorVersion BEFORE_EMAIL_CONNECTION = new OperatorVersion(9, 4, 0);

	private MailConnectionUtilities() {
		// no instantiation of utility class
	}

	/**
	 * Creates the parameter(s) necessary for an operator using email connections. Sets up a {@link ConnectionInformationSelector}
	 * if necessary (with port creation), adds a transformation rule for connection type checks. If the given operator has
	 * at least one compatibility level, adds a compatibility condition to the parameter
	 *
	 * @param operator
	 * 		the operator to create the parameter(s) for
	 * @param handler
	 * 		the handler, whose type is associated with the parameter(s)
	 * @param <O>
	 * 		type to capture both {@link Operator} and {@link ConnectionSelectionProvider}
	 * @return the list of parameters
	 */
	public static <O extends Operator & ConnectionSelectionProvider> List<ParameterType> getMailParameters(O operator, MailConnectionHandler handler){
		ConnectionInformationSelector selector = operator.getConnectionSelector();
		OperatorVersion[] incompatibleVersionChanges = operator.getIncompatibleVersionChanges();
		boolean addCompConditions = incompatibleVersionChanges != null && incompatibleVersionChanges.length > 0;
		Predicate<Operator> useMailConnection = op -> !addCompConditions || operator.getCompatibilityLevel().isAbove(BEFORE_EMAIL_CONNECTION);
		if (operator.getConnectionSelector() == null) {
			selector = new ConnectionInformationSelector(operator, handler.getType()) {

				@Override
				public ProcessSetupError checkConnectionTypeMatch(Operator operator) {
					if (!useMailConnection.test(operator) && getInput().isConnected() && isConnectionSpecified()) {
						return new SimpleMetaDataError(Severity.WARNING, getInput(), "connection.input_connection_not_used");
					}
					return super.checkConnectionTypeMatch(operator);
				}

				@Override
				public ConnectionInformationMetaData getMetaData() {
					if (useMailConnection.test(operator)) {
						return super.getMetaData();
					}
					return null;
				}

				/** @return {@link MailConnectionHandler#getParameterKey()} */
				@Override
				public String getParameterKey() {
					return handler.getParameterKey();
				}
			};
			operator.setConnectionSelector(selector);
			selector.makeDefaultPortTransformation();
			operator.getTransformer().addRule(ConnectionInformationSelector.makeConnectionCheckTransformation(operator));
		}
		List<ParameterType> types = ConnectionInformationSelector.createParameterTypes(selector);
		if (addCompConditions) {
			AboveOperatorVersionCondition useMailCondition = new AboveOperatorVersionCondition(operator, BEFORE_EMAIL_CONNECTION);
			types.forEach(p -> p.registerDependencyCondition(useMailCondition));
		}
		return types;
	}

	/**
	 * Sends an email from the given {@link Operator} with the specified mail parameters. If the
	 * {@link com.rapidminer.operator.ports.metadata.CompatibilityLevel CompatibilityLevel} is above
	 * {@link MailConnectionUtilities#BEFORE_EMAIL_CONNECTION}, the operators configured email connection is used, otherwise
	 * the old style properties from {@link ParameterService#getParameterValue(String)} are used.
	 *
	 * @param operator
	 * 		the operator the mail should be send from; must not be {@code null}
	 * @throws MailNotSentException if an error occurred
	 */
	@SuppressWarnings("deprecation")
	public static <O extends Operator & ConnectionSelectionProvider> void sendEmail(
			O operator, String to, String subject, String body, Map<String, String> headers) throws MailNotSentException {
		if (operator.getCompatibilityLevel().isAtMost(BEFORE_EMAIL_CONNECTION)) {
			MailUtilities.sendEmailWithException(to, subject, body, headers);
			return;
		}
		try {
			sendEmail(operator.getConnectionSelector().getConnection(), operator, to, subject, body, headers);
		} catch (NullPointerException | UserError e) {
			throw new MailNotSentException("Error while retrieving mail connection", e, "connection.mail.retrieval_error");
		}
	}

	/**
	 * Sends an email with the given {@link ConnectionInformation} and the specified mail parameters.
	 *
	 * @param connection
	 * 		the connection to use to send the mail; if {@code null}, this will throw a {@link MailNotSentException}
	 * @param operator
	 * 		the operator used during injection; can be {@code null}
	 * @throws MailNotSentException
	 * 		if an error occurred
	 */
	public static void sendEmail(ConnectionInformation connection, Operator operator,
								 String to, String subject, String body, Map<String, String> headers) throws MailNotSentException {
		if (connection == null) {
			throw new MailNotSentException("No email connection specified", "connection.mail.no_connection_specified");
		}
		Map<String, String> parameters = ValueProviderHandlerRegistry.getInstance().injectValues(connection, operator, false);
		MailConnectionHandler.convertSMTPParameters(parameters);
		MailUtilities.sendEmailWithException(to, subject, body, headers, parameters::get);
	}
}
