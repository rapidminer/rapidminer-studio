/**
 * Copyright (C) 2001-2018 by RapidMiner and the contributors
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

import java.util.HashMap;
import java.util.List;
import java.util.Map;

import com.rapidminer.RapidMiner;
import com.rapidminer.operator.MailNotSentException;
import com.rapidminer.operator.Operator;
import com.rapidminer.operator.OperatorDescription;
import com.rapidminer.operator.OperatorException;
import com.rapidminer.operator.OperatorVersion;
import com.rapidminer.operator.ProcessSetupError.Severity;
import com.rapidminer.operator.SimpleProcessSetupError;
import com.rapidminer.operator.UserError;
import com.rapidminer.operator.ports.DummyPortPairExtender;
import com.rapidminer.parameter.ParameterType;
import com.rapidminer.parameter.ParameterTypeBoolean;
import com.rapidminer.parameter.ParameterTypeList;
import com.rapidminer.parameter.ParameterTypeString;
import com.rapidminer.parameter.ParameterTypeText;
import com.rapidminer.parameter.TextType;
import com.rapidminer.parameter.conditions.AboveOperatorVersionCondition;
import com.rapidminer.parameter.conditions.BooleanParameterCondition;
import com.rapidminer.tools.MailUtilities;
import com.rapidminer.tools.ParameterService;


/**
 *
 * @author Simon Fischer, Nils Woehler
 *
 */
public class SendMailOperator extends Operator {

	public static final OperatorVersion VERSION_SWAPPED_INPUT_PORTS = new OperatorVersion(5, 2, 6);

	private DummyPortPairExtender through = new DummyPortPairExtender("through", getInputPorts(), getOutputPorts());

	public static final String PARAMETER_TO = "to";
	public static final String PARAMETER_SUBJECT = "subject";
	public static final String PARAMETER_BODY_PLAIN = "body_plain";
	public static final String PARAMETER_BODY_HTML = "body_html";
	public static final String PARAMETER_USE_HTML = "use_html";

	public static final String PARAMETER_HEADERS = "headers";

	public static final String PARAMETER_THROW_ERROR = "ignore_errors";

	public SendMailOperator(OperatorDescription description) {
		super(description);
		through.start();
		getTransformer().addRule(through.makePassThroughRule());
	}

	@Override
	protected void performAdditionalChecks() {
		super.performAdditionalChecks();

		String method = ParameterService.getParameterValue(RapidMiner.PROPERTY_RAPIDMINER_TOOLS_MAIL_METHOD);
		if (method.equals(RapidMiner.PROPERTY_RAPIDMINER_TOOLS_MAIL_METHOD_VALUES[0])) { // sendmail
			String command = ParameterService.getParameterValue(RapidMiner.PROPERTY_RAPIDMINER_TOOLS_SENDMAIL_COMMAND);
			if (command == null || command.equals("")) {
				addError(new SimpleProcessSetupError(Severity.WARNING, getPortOwner(), "no_send_mail_command"));
			}
		} else if (method.equals(RapidMiner.PROPERTY_RAPIDMINER_TOOLS_MAIL_METHOD_VALUES[1])) { // smtp
			String user = ParameterService.getParameterValue(RapidMiner.PROPERTY_RAPIDMINER_TOOLS_SMTP_USER);
			if (user == null || user.equals("")) {
				addError(new SimpleProcessSetupError(Severity.WARNING, getPortOwner(), "no_smtp_mail_user_set"));
			}

			String passwd = ParameterService.getParameterValue(RapidMiner.PROPERTY_RAPIDMINER_TOOLS_SMTP_PASSWD);
			if (passwd == null || passwd.equals("")) {
				addError(new SimpleProcessSetupError(Severity.WARNING, getPortOwner(), "no_smtp_mail_passwd_set"));
			}

			String host = ParameterService.getParameterValue(RapidMiner.PROPERTY_RAPIDMINER_TOOLS_SMTP_HOST);
			if (host == null || host.equals("")) {
				addError(new SimpleProcessSetupError(Severity.WARNING, getPortOwner(), "no_smtp_mail_host_set"));
			}

			String port = ParameterService.getParameterValue(RapidMiner.PROPERTY_RAPIDMINER_TOOLS_SMTP_PORT);
			if (port == null || port.equals("")) {
				addError(new SimpleProcessSetupError(Severity.WARNING, getPortOwner(), "no_smtp_mail_port_set"));
			}
		}

	}

	@Override
	public void doWork() throws OperatorException {
		String to = getParameterAsString(PARAMETER_TO);
		String subject = getParameterAsString(PARAMETER_SUBJECT);

		Map<String, String> headers = new HashMap<String, String>();
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
		if (getCompatibilityLevel().isAtMost(VERSION_SWAPPED_INPUT_PORTS)) {
			MailUtilities.sendEmail(to, subject, body, headers);
		} else {
			if (!getParameterAsBoolean(PARAMETER_THROW_ERROR)) {
				try {
					MailUtilities.sendEmailWithException(to, subject, body, headers);
				} catch (MailNotSentException e) {
					Exception cause = (Exception) (e.getArguments().length > 0 && e.getArguments()[0] instanceof Exception
							? e.getArguments()[0] : null);
					String message = cause != null ? cause.getMessage() : "";
					Object[] args = "sending_mail_to_address_error".equals(e.getErrorKey()) ? new Object[] { to, message }
							: e.getArguments();
					throw new UserError(this, e.getCause(), e.getErrorKey(), args);
				}
			} else {
				MailUtilities.sendEmail(to, subject, body, headers);
			}
		}
		through.passDataThrough();
	}

	@Override
	public List<ParameterType> getParameterTypes() {
		final List<ParameterType> types = super.getParameterTypes();
		types.add(new ParameterTypeString(PARAMETER_TO, "Receiver of the email.", false, false));
		types.add(new ParameterTypeString(PARAMETER_SUBJECT, "Subject the email.", false, false));

		types.add(new ParameterTypeBoolean(PARAMETER_USE_HTML, "Format text as HTML?.", false, false));

		ParameterType type = new ParameterTypeText(PARAMETER_BODY_PLAIN, "Body of the email.", TextType.PLAIN, true);
		type.registerDependencyCondition(new BooleanParameterCondition(this, PARAMETER_USE_HTML, true, false));
		type.setExpert(false);
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
		return new OperatorVersion[] { VERSION_SWAPPED_INPUT_PORTS };
	}

}
