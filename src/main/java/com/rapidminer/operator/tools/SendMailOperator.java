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
package com.rapidminer.operator.tools;

import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import com.rapidminer.RapidMiner;
import com.rapidminer.connection.util.ConnectionInformationSelector;
import com.rapidminer.connection.util.ConnectionSelectionProvider;
import com.rapidminer.connection.util.TestResult.ResultType;
import com.rapidminer.connection.util.ValidationResult;
import com.rapidminer.gui.RapidMinerGUI;
import com.rapidminer.gui.properties.SettingsDialog;
import com.rapidminer.operator.MailNotSentException;
import com.rapidminer.operator.Operator;
import com.rapidminer.operator.OperatorDescription;
import com.rapidminer.operator.OperatorException;
import com.rapidminer.operator.OperatorVersion;
import com.rapidminer.operator.ProcessSetupError.Severity;
import com.rapidminer.operator.SimpleProcessSetupError;
import com.rapidminer.operator.UserError;
import com.rapidminer.operator.ports.DummyPortPairExtender;
import com.rapidminer.operator.ports.quickfix.AbstractQuickFix;
import com.rapidminer.operator.ports.quickfix.QuickFix;
import com.rapidminer.parameter.ParameterType;
import com.rapidminer.parameter.ParameterTypeBoolean;
import com.rapidminer.parameter.ParameterTypeList;
import com.rapidminer.parameter.ParameterTypeString;
import com.rapidminer.parameter.ParameterTypeText;
import com.rapidminer.parameter.TextType;
import com.rapidminer.parameter.conditions.AboveOperatorVersionCondition;
import com.rapidminer.parameter.conditions.BooleanParameterCondition;
import com.rapidminer.tools.mail.connection.MailConnectionHandler;
import com.rapidminer.tools.mail.connection.MailConnectionUtilities;


/**
 *
 * @author Simon Fischer, Nils Woehler
 *
 */
public class SendMailOperator extends Operator implements ConnectionSelectionProvider {

	public static final OperatorVersion VERSION_SWAPPED_INPUT_PORTS = new OperatorVersion(5, 2, 6);


	public static final String PARAMETER_TO = "to";
	public static final String PARAMETER_SUBJECT = "subject";
	public static final String PARAMETER_BODY_PLAIN = "body_plain";
	public static final String PARAMETER_BODY_HTML = "body_html";
	public static final String PARAMETER_USE_HTML = "use_html";

	public static final String PARAMETER_HEADERS = "headers";

	public static final String PARAMETER_THROW_ERROR = "ignore_errors";

	/**
	 * Mapping of setting properties to process setup error keys
	 * @since 9.4.1
	 */
	private static final Map<String, String> MISSING_MAIL_SETTING;
	static {
		Map<String, String> missingMailSetting = new HashMap<>();
		missingMailSetting.put(RapidMiner.PROPERTY_RAPIDMINER_TOOLS_MAIL_METHOD, "invalid_mail_method");
		missingMailSetting.put(RapidMiner.PROPERTY_RAPIDMINER_TOOLS_MAIL_SENDER, "invalid_mail_sender");
		missingMailSetting.put(RapidMiner.PROPERTY_RAPIDMINER_TOOLS_SENDMAIL_COMMAND, "no_send_mail_command");
		missingMailSetting.put(RapidMiner.PROPERTY_RAPIDMINER_TOOLS_SMTP_USER, "no_smtp_mail_user_set");
		missingMailSetting.put(RapidMiner.PROPERTY_RAPIDMINER_TOOLS_SMTP_PASSWD, "no_smtp_mail_passwd_set");
		missingMailSetting.put(RapidMiner.PROPERTY_RAPIDMINER_TOOLS_SMTP_HOST, "no_smtp_mail_host_set");
		missingMailSetting.put(RapidMiner.PROPERTY_RAPIDMINER_TOOLS_SMTP_PORT, "no_smtp_mail_port_set");
		MISSING_MAIL_SETTING = Collections.unmodifiableMap(missingMailSetting);
	}

	/**
	 * {@link QuickFix} to set mail settings
	 *
	 * @since 9.4.1
	 */
	private static final QuickFix OPEN_SETTINGS_QUICKFIX = new AbstractQuickFix(AbstractQuickFix.MAX_RATING, true, "open_mail_settings") {
		@Override
		public void apply() {
			new SettingsDialog(MailConnectionHandler.PROPERTY_TOOLS).setVisible(true);
			if (RapidMinerGUI.getMainFrame().VALIDATE_AUTOMATICALLY_ACTION.isSelected()) {
				RapidMinerGUI.getMainFrame().validateProcess(false);
			}
		}
	};

	private DummyPortPairExtender through = new DummyPortPairExtender("through", getInputPorts(), getOutputPorts());

	/** @since 9.4.1 */
	private ConnectionInformationSelector selector;

	public SendMailOperator(OperatorDescription description) {
		super(description);
		through.start();
		getTransformer().addRule(through.makePassThroughRule());
	}

	@Override
	protected void performAdditionalChecks() {
		super.performAdditionalChecks();
		if (getCompatibilityLevel().isAbove(MailConnectionUtilities.BEFORE_EMAIL_CONNECTION)) {
			return;
		}
		// use new mechanism to check settings
		ValidationResult valResult = MailConnectionHandler.SEND.validate(MailConnectionHandler.getSettingsConnection(null));
		if (valResult.getType() == ResultType.SUCCESS) {
			return;
		}
		valResult.getParameterErrorMessages().keySet().stream().findFirst()
				.map(key -> MailConnectionHandler.PROPERTY_RAPIDMINER_TOOLS_PREFIX + key)
				.map(MISSING_MAIL_SETTING::get)
				.map(errorKey -> new SimpleProcessSetupError(Severity.WARNING, getPortOwner(), Collections.singletonList(OPEN_SETTINGS_QUICKFIX), errorKey))
				.ifPresent(this::addError);
	}

	@Override
	public void doWork() throws OperatorException {
		String to = getParameterAsString(PARAMETER_TO);
		String subject = getParameterAsString(PARAMETER_SUBJECT);

		Map<String, String> headers = new HashMap<>();
		for (String[] entry : getParameterList(PARAMETER_HEADERS)) {
			headers.put(entry[0], entry[1]);
		}
		String body;
		if (getParameterAsBoolean(PARAMETER_USE_HTML)) {
			body = getParameterAsString(PARAMETER_BODY_HTML);
			headers.put("Content-Type", "text/html");
		} else {
			body = getParameterAsString(PARAMETER_BODY_PLAIN);
		}
		boolean throwOnError = getCompatibilityLevel().isAbove(VERSION_SWAPPED_INPUT_PORTS) && !getParameterAsBoolean(PARAMETER_THROW_ERROR);
		try {
			MailConnectionUtilities.sendEmail(this, to, subject, body, headers);
		} catch (MailNotSentException sendException) {
			if (throwOnError) {
				if (sendException.getCause() instanceof UserError) {
					throw (UserError) sendException.getCause();
				}
				Object[] arguments = sendException.getArguments();
				Exception cause = (Exception) (arguments.length > 0 && arguments[0] instanceof Exception ? arguments[0] : null);
				String message = cause != null ? cause.getMessage() : "";
				Object[] args = "sending_mail_to_address_error".equals(sendException.getErrorKey()) ? new Object[]{to, message} : arguments;
				throw new UserError(this, sendException.getCause(), sendException.getErrorKey(), args);
			}
		}
		if (selector != null) {
			selector.passDataThrough();
		}
		through.passDataThrough();
	}

	/** @since 9.4.1 */
	@Override
	public ConnectionInformationSelector getConnectionSelector() {
		return selector;
	}

	/** @since 9.4.1 */
	@Override
	public void setConnectionSelector(ConnectionInformationSelector selector) {
		this.selector = selector;
	}

	@Override
	public List<ParameterType> getParameterTypes() {
		final List<ParameterType> types = super.getParameterTypes();
		types.addAll(MailConnectionUtilities.getMailParameters(this, MailConnectionHandler.SEND));
		types.add(new ParameterTypeString(PARAMETER_TO, "Receiver of the email.", false, false));
		types.add(new ParameterTypeString(PARAMETER_SUBJECT, "Subject the email.", false, false));

		types.add(new ParameterTypeBoolean(PARAMETER_USE_HTML, "Format text as HTML?.", false, false));

		ParameterType type = new ParameterTypeText(PARAMETER_BODY_PLAIN, "Body of the email.", TextType.PLAIN, true);
		type.registerDependencyCondition(new BooleanParameterCondition(this, PARAMETER_USE_HTML, true, false));
		type.setExpert(false);
		type.setPrimary(true);
		types.add(type);

		ParameterTypeText typeText = new ParameterTypeText(PARAMETER_BODY_HTML, "Body of the email in HTML format.",
				TextType.HTML, true);
		typeText.setTemplateText("<html>\n" + "	<head>\n" + "		<title>RapidMiner Mail Message</title>\n"
				+ "	</head>\n" + "	<body>\n" + "		<p>\n" + "		</p>\n" + "	</body>\n" + "</html>\n");
		typeText.registerDependencyCondition(new BooleanParameterCondition(this, PARAMETER_USE_HTML, true, true));
		typeText.setExpert(false);
		types.add(typeText);

		type = new ParameterTypeList(PARAMETER_HEADERS, "Additional mail headers",
				new ParameterTypeString("header", "Name of the header"),
				new ParameterTypeString("value", "value of the header"));
		type.setExpert(true);
		types.add(type);

		type = new ParameterTypeBoolean(PARAMETER_THROW_ERROR,
				"If set errors will be logged only. Otherwise the process will be stopped and an error will be shown.",
				false);
		type.setExpert(false);
		types.add(type);
		type.registerDependencyCondition(new AboveOperatorVersionCondition(this, VERSION_SWAPPED_INPUT_PORTS));

		return types;
	}

	@Override
	public OperatorVersion[] getIncompatibleVersionChanges() {
		return new OperatorVersion[] { VERSION_SWAPPED_INPUT_PORTS, MailConnectionUtilities.BEFORE_EMAIL_CONNECTION };
	}

}
