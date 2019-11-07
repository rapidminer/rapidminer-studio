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

import static com.rapidminer.connection.util.ParameterUtility.getCPBuilder;
import static com.rapidminer.connection.util.ParameterUtility.validateParameterValue;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Objects;
import java.util.concurrent.ExecutionException;
import java.util.function.UnaryOperator;
import javax.mail.AuthenticationFailedException;
import javax.mail.MessagingException;
import javax.swing.SwingWorker;

import org.apache.commons.lang.StringUtils;

import com.rapidminer.RapidMiner;
import com.rapidminer.connection.ConnectionHandler;
import com.rapidminer.connection.ConnectionInformation;
import com.rapidminer.connection.ConnectionInformationBuilder;
import com.rapidminer.connection.configuration.ConfigurationParameter;
import com.rapidminer.connection.configuration.ConnectionConfiguration;
import com.rapidminer.connection.configuration.ConnectionConfigurationBuilder;
import com.rapidminer.connection.util.TestExecutionContext;
import com.rapidminer.connection.util.TestResult;
import com.rapidminer.connection.util.TestResult.ResultType;
import com.rapidminer.connection.util.ValidationResult;
import com.rapidminer.connection.valueprovider.handler.ValueProviderHandlerRegistry;
import com.rapidminer.connection.valueprovider.handler.ValueProviderUtils;
import com.rapidminer.gui.tools.MultiSwingWorker;
import com.rapidminer.gui.tools.ProgressThreadStoppedException;
import com.rapidminer.tools.InterruptableSupplier;
import com.rapidminer.tools.MailSenderSMTP;
import com.rapidminer.tools.MailUtilities;
import com.rapidminer.tools.OperatorService;
import com.rapidminer.tools.ParameterService;
import com.rapidminer.tools.ValidationUtil;


/**
 * {@link ConnectionHandler} for mail accounts. Mainly used in the {@link com.rapidminer.operator.tools.SendMailOperator SendMailOperator}
 * or in the {@link com.rapidminer.operator.ProcessRootOperator ProcessRootOperator}.
 *
 * @author Jan Czogalla
 * @since 9.4.1
 */
public enum MailConnectionHandler implements ConnectionHandler {

	SEND(OperatorService.RAPID_MINER_CORE_NAMESPACE + ':' + "mail_send", getSMTPType(), "mail_sender");

	public static final String GROUP_MAIL = "mail";
	public static final String GROUP_SENDMAIL = "sendmail";
	public static final String GROUP_SMTP = "smtp";

	public static final String PARAMETER_MAIL_METHOD = "method";
	public static final String PARAMETER_MAIL_SENDER = "sender";
	public static final String DEFAULT_SENDER = "no-reply@rapidminer.com";
	public static final String PARAMETER_SENDMAIL_COMMAND = "command";
	public static final String PARAMETER_MAILTYPE_SERVER = "host";
	public static final String PARAMETER_MAILTYPE_PORT = "port";
	public static final String PARAMETER_MAILTYPE_USER_NAME = "user";
	public static final String PARAMETER_MAILTYPE_PASSWORD = "passwd";
	public static final String PARAMETER_MAILTYPE_SECURITY = "security";
	public static final String PARAMETER_MAILTYPE_AUTHENTICATION = "authentication";

	public static final String PROPERTY_TOOLS = "tools";
	public static final String PROPERTY_RAPIDMINER_TOOLS_PREFIX = "rapidminer." + PROPERTY_TOOLS + ".";
	private static final String PROPERTY_RAPIDMINER_SMTP_PREFIX =  PROPERTY_RAPIDMINER_TOOLS_PREFIX + GROUP_SMTP + '.';

	private static final String[] MAILTYPE_PARAMETERS = {PARAMETER_MAILTYPE_SERVER, PARAMETER_MAILTYPE_PORT, PARAMETER_MAILTYPE_USER_NAME,
			PARAMETER_MAILTYPE_PASSWORD, PARAMETER_MAILTYPE_SECURITY, PARAMETER_MAILTYPE_AUTHENTICATION};

	private static final String DEFAULT_SENDER_VALUE = ValueProviderUtils.wrapIntoPlaceholder(GROUP_SMTP + '.' + PARAMETER_MAILTYPE_USER_NAME);
	private static final String I18N_KEY_INVALID_METHOD = "validation.mail.invalid_mail_method";
	private static final String I18N_KEY_INVALID_PORT = "validation.mail.invalid_port";

	private static final String I18N_KEY_TEST_AUTHENTICATION_FAILED = "test.mail.authentication_failed";
	private static final String I18N_KEY_TEST_MESSAGE_ERROR = "test.mail.message_error";
	private static final String I18N_KEY_TEST_SESSION_CREATION_ERROR = "test.mail.session_creation_error";


	private String mailType;
	private String type;
	private String parameterKey;

	/**
	 * @param type
	 * 		the connection type; can be {@code null}, then will be set to
	 * 		{@value OperatorService#RAPID_MINER_CORE_NAMESPACE} {@code :<mailType>}
	 * @param mailType
	 * 		the mail type; used for the connection type as well as the main parameter group; must not be {@code null} or empty
	 * @param parameterKey
	 * 		the operator parameter key provided through a
	 *        {@link com.rapidminer.connection.util.ConnectionInformationSelector ConnectionInformationSelector};
	 * 		can be {@code null}, then will be set to {@code <mailType>_connection}
	 */
	MailConnectionHandler(String type, String mailType, String parameterKey) {
		this.mailType = mailType;
		this.type = Objects.toString(type, OperatorService.RAPID_MINER_CORE_NAMESPACE + ':' + mailType);
		this.parameterKey = Objects.toString(parameterKey, mailType.toUpperCase() + "_connection");
	}

	@Override
	public ConnectionInformation createNewConnectionInformation(String name) {
		ConnectionConfigurationBuilder confBuilder = new ConnectionConfigurationBuilder(name, getType());
		if (this == SEND) {
			confBuilder.withKeys(GROUP_MAIL, Arrays.asList(getCPBuilder(PARAMETER_MAIL_METHOD).disable()
							.withValue(RapidMiner.PROPERTY_RAPIDMINER_TOOLS_MAIL_METHOD_VALUES[RapidMiner.PROPERTY_RAPIDMINER_TOOLS_MAIL_METHOD_SMTP]).build(),
					getCPBuilder(PARAMETER_MAIL_SENDER).withValue(DEFAULT_SENDER_VALUE).build()));
			confBuilder.withKeys(GROUP_SENDMAIL, Collections.singletonList(getCPBuilder(PARAMETER_SENDMAIL_COMMAND).disable().build()));
		}
		confBuilder.withKeys(getMailTypeGroup(), Arrays.asList(getCP(PARAMETER_MAILTYPE_SERVER), getCP(PARAMETER_MAILTYPE_PORT),
				getCP(PARAMETER_MAILTYPE_USER_NAME), getCPEn(PARAMETER_MAILTYPE_PASSWORD),
				getCPBuilder(PARAMETER_MAILTYPE_SECURITY).withValue(RapidMiner.PROPERTY_RAPIDMINER_TOOLS_SMTP_SECURITY_NONE).build(),
				getCPBuilder(PARAMETER_MAILTYPE_AUTHENTICATION).withValue(RapidMiner.PROPERTY_RAPIDMINER_TOOLS_SMTP_AUTHENTICATION_AUTO).build()));
		ConnectionInformationBuilder infBuilder = new ConnectionInformationBuilder(confBuilder.build());
		return infBuilder.build();
	}

	@Override
	public void initialize() {
		// noop
	}

	@Override
	public boolean isInitialized() {
		return true;
	}

	@Override
	public String getType() {
		return type;
	}

	public String getMailTypeGroup() {
		return mailType;
	}

	public String getParameterKey() {
		return parameterKey;
	}

	/**
	 * {@inheritDoc}
	 * <p>
	 * Checks that a valid mail method is selected and all parameters for the selected method are set
	 * (either directly or through injection). If needed, checks that the port is actually a number.
	 */
	@Override
	public ValidationResult validate(ConnectionInformation connection) {
		if (connection == null) {
			return ValidationResult.nullable();
		}
		Map<String, String> errorMap = new LinkedHashMap<>();
		ConnectionConfiguration config = connection.getConfiguration();

		boolean skipTypeParameters = false;
		if (this == SEND) {
			skipTypeParameters = validateSendSpecific(config, errorMap);
		}

		if (!skipTypeParameters) {
			for (String smtpParameter : MAILTYPE_PARAMETERS) {
				validateParameterValue(getMailTypeGroup(), smtpParameter, config, errorMap::put);
			}
			String portKey = getMailTypeGroup() + '.' + PARAMETER_MAILTYPE_PORT;
			if (!errorMap.containsKey(portKey)) {
				ConfigurationParameter portParam = config.getParameter(portKey);
				if (!portParam.isInjected() && !portParam.getValue().matches("[0-9]+")) {
					errorMap.put(portKey, I18N_KEY_INVALID_PORT);
				}
			}
		}

		if (errorMap.isEmpty()) {
			return ValidationResult.success(ValidationResult.I18N_KEY_SUCCESS);
		}
		return ValidationResult.failure(ValidationResult.I18N_KEY_FAILURE, errorMap);
	}

	/**
	 * Checks the parameters specific to the {@link #SEND} instance, namely {@value #PARAMETER_MAIL_METHOD} and
	 * {@value #PARAMETER_MAIL_SENDER}. Returns {@code true}, if the selected mail method is not SMTP.
	 *
	 * @param config
	 * 		the configuration
	 * @param errorMap
	 * 		the error map
	 * @return whether to skip the type parameters or not
	 */
	private static boolean validateSendSpecific(ConnectionConfiguration config, Map<String, String> errorMap) {
		String methodKey = GROUP_MAIL + '.' + PARAMETER_MAIL_METHOD;
		ConfigurationParameter methodParam = config.getParameter(methodKey);
		validateParameterValue(methodKey, methodParam, errorMap::put);
		int methodIndex = -1;
		// first part implies methodParam != null
		if (errorMap.isEmpty() && !methodParam.isInjected()) {
			String method = methodParam.getValue();
			errorMap.put(methodKey, I18N_KEY_INVALID_METHOD);
			for (int i = 0; i < RapidMiner.PROPERTY_RAPIDMINER_TOOLS_MAIL_METHOD_VALUES.length; i++) {
				if (RapidMiner.PROPERTY_RAPIDMINER_TOOLS_MAIL_METHOD_VALUES[i].equals(method)) {
					errorMap.remove(methodKey);
					methodIndex = i;
					break;
				}
			}
		}
		String senderKey = GROUP_MAIL + '.' + PARAMETER_MAIL_SENDER;
		ConfigurationParameter senderParam = config.getParameter(senderKey);
		if (senderParam == null || !ValidationUtil.isValueSet(senderParam)) {
			errorMap.put(senderKey, ValidationResult.I18N_KEY_VALUE_MISSING);
		}
		if (methodIndex == RapidMiner.PROPERTY_RAPIDMINER_TOOLS_MAIL_METHOD_SENDMAIL) {
			validateParameterValue(GROUP_SENDMAIL, PARAMETER_SENDMAIL_COMMAND, config, errorMap::put);
		}
		return methodIndex != RapidMiner.PROPERTY_RAPIDMINER_TOOLS_MAIL_METHOD_SMTP;
	}

	@Override
	public TestResult test(TestExecutionContext<ConnectionInformation> testContext) {
		if (testContext == null || testContext.getSubject() == null) {
			return TestResult.nullable();
		}
		InterruptableSupplier<Exception> emailTest;
		Map<String, String> properties = ValueProviderHandlerRegistry.getInstance().injectValues(testContext.getSubject(), null, false);
		switch (this) {
			case SEND:
				convertSMTPParameters(properties);
				if (MailUtilities.getMailMethodIndex(properties.get(RapidMiner.PROPERTY_RAPIDMINER_TOOLS_MAIL_METHOD)) != RapidMiner.PROPERTY_RAPIDMINER_TOOLS_MAIL_METHOD_SMTP) {
					return TestResult.nullable();
				}
				emailTest = new MailSenderSMTP().testEmailWithInterrupt(properties::get);
				break;
			default:
				return TestResult.nullable();
		}
		Exception e;
		switch (MailUtilities.getMailMethodIndex(properties.get(RapidMiner.PROPERTY_RAPIDMINER_TOOLS_MAIL_METHOD))) {
			case RapidMiner.PROPERTY_RAPIDMINER_TOOLS_MAIL_METHOD_SMTP:
				SwingWorker<Exception, Void> swingWorker = new MultiSwingWorker<Exception, Void>() {

					@Override
					protected Exception doInBackground() throws Exception {
						return emailTest.get();
					}
				};
				swingWorker.execute();
				while (true) {
					if (swingWorker.isDone()) {
						try {
							e = swingWorker.get();
							break;
						} catch (InterruptedException interrupt) {
							// should not happen
							Thread.currentThread().interrupt();
						} catch (ExecutionException exe) {
							// should not happen
						}
						return TestResult.nullable();
					}
					try {
						testContext.checkCancelled();
					} catch (ProgressThreadStoppedException stopped) {
						emailTest.interrupt();
						return TestResult.nullable();
					}
					try {
						Thread.sleep(500);
					} catch (InterruptedException interrupt) {
						Thread.currentThread().interrupt();
						// ignore
					}
				}
				break;
			case RapidMiner.PROPERTY_RAPIDMINER_TOOLS_MAIL_METHOD_SENDMAIL:
			default:
				return new TestResult(ResultType.NOT_SUPPORTED, TestResult.I18N_KEY_SUCCESS, Collections.emptyMap());
		}
		if (e == null) {
			return TestResult.success(TestResult.I18N_KEY_SUCCESS);
		}
		String failureKey = I18N_KEY_TEST_MESSAGE_ERROR;
		if (e instanceof AuthenticationFailedException) {
			failureKey = I18N_KEY_TEST_AUTHENTICATION_FAILED;
		} else if (e instanceof NullPointerException) {
			failureKey = I18N_KEY_TEST_SESSION_CREATION_ERROR;
		} else if (e instanceof MessagingException && ((MessagingException) e).getNextException() != null) {
			e = ((MessagingException) e).getNextException();
		}
		return TestResult.failure(failureKey, e.getMessage());
	}

	/**
	 * Creates a {@link ConnectionInformation} with the given name from the old {@link RapidMiner} settings
	 * of type {@link #SEND}. Can be used to convert the current settings to the new mechanism and store it
	 * in the repository or to have it as a stand-in when in need of using the old default methods.
	 */
	public static ConnectionInformation getSettingsConnection(String name) {
		name = StringUtils.defaultIfEmpty(name, "Default");
		ConnectionInformation settingsConnection = SEND.createNewConnectionInformation(name);
		ConnectionConfiguration config = settingsConnection.getConfiguration();

		String sender = StringUtils.defaultIfEmpty(ParameterService.getParameterValue(
				RapidMiner.PROPERTY_RAPIDMINER_TOOLS_MAIL_SENDER), DEFAULT_SENDER);
		setParameter(config, GROUP_MAIL, PARAMETER_MAIL_SENDER, sender);

		String method = ParameterService.getParameterValue(RapidMiner.PROPERTY_RAPIDMINER_TOOLS_MAIL_METHOD);
		int methodIndex = MailUtilities.getMailMethodIndex(method);
		setParameter(config, GROUP_MAIL, PARAMETER_MAIL_METHOD, RapidMiner.PROPERTY_RAPIDMINER_TOOLS_MAIL_METHOD_VALUES[methodIndex]);
		if (methodIndex == RapidMiner.PROPERTY_RAPIDMINER_TOOLS_MAIL_METHOD_SENDMAIL) {
			String command = StringUtils.trimToNull(ParameterService.getParameterValue(RapidMiner.PROPERTY_RAPIDMINER_TOOLS_SENDMAIL_COMMAND));
			if (command != null) {
				setParameter(config, GROUP_SENDMAIL, PARAMETER_SENDMAIL_COMMAND, command);
			}
			return settingsConnection;
		}

		for (String parameterKey : MAILTYPE_PARAMETERS) {
			String parameter = StringUtils.stripToNull(ParameterService.getParameterValue(PROPERTY_RAPIDMINER_SMTP_PREFIX + parameterKey));
			if (parameter != null) {
				if (PARAMETER_MAILTYPE_PASSWORD.equals(parameterKey)) {
					parameter = MailUtilities.DECRYPT_WITH_CIPHER_KEY.apply(parameter);
				}
				setParameter(config, SEND.getMailTypeGroup(), parameterKey, parameter);
			}
		}

		return settingsConnection;
	}

	/**
	 * For each parameter that is set, duplicate the value to be available through the original RapidMiner property keys.
	 * Should be used before sending mails with {@link MailUtilities#sendEmailWithException(String, String, String, Map, UnaryOperator)}.
	 * <p>
	 * <strong>Note:</strong> This should only be used for {@link #SEND} connections
	 */
	public static void convertSMTPParameters(Map<String, String> properties) {
		ArrayList<String> propKeys = new ArrayList<>(properties.keySet());
		propKeys.removeIf(key -> key.indexOf('.') == -1);
		propKeys.forEach(key -> properties.put(PROPERTY_RAPIDMINER_TOOLS_PREFIX + key, properties.get(key)));
	}

	/**
	 * Set the parameter specified by group and key to the given value, if the parameter exists
	 * in the {@link ConnectionConfiguration}.
	 */
	private static void setParameter(ConnectionConfiguration config, String group, String key, String value) {
		ConfigurationParameter parameter = config.getParameter(group + '.' + key);
		if (parameter != null) {
			parameter.setValue(value);
		}
	}

	/** Shorthand for getting an unencrypted {@link ConfigurationParameter} */
	private static ConfigurationParameter getCP(String name) {
		return getCPBuilder(name).build();
	}

	/** Shorthand for getting an encrypted {@link ConfigurationParameter} */
	private static ConfigurationParameter getCPEn(String name) {
		return getCPBuilder(name, true).build();
	}

	/** helper method to reference {@link #GROUP_SMTP} in enum constructor */
	private static String getSMTPType() {
		return GROUP_SMTP;
	}
}