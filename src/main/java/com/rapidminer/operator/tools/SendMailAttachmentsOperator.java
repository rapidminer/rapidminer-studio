/**
 * Copyright (C) 2001-2020 by RapidMiner and the contributors
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

import java.io.File;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.stream.Collectors;

import org.apache.commons.lang.StringUtils;
import org.apache.commons.mail.EmailAttachment;
import org.apache.commons.mail.EmailConstants;
import org.apache.commons.mail.EmailException;
import org.apache.commons.mail.HtmlEmail;
import org.apache.commons.mail.MultiPartEmail;

import com.rapidminer.connection.util.ConnectionInformationSelector;
import com.rapidminer.connection.util.ConnectionSelectionProvider;
import com.rapidminer.connection.valueprovider.handler.ValueProviderHandlerRegistry;
import com.rapidminer.operator.Operator;
import com.rapidminer.operator.OperatorDescription;
import com.rapidminer.operator.OperatorException;
import com.rapidminer.operator.UserError;
import com.rapidminer.operator.nio.file.FileObject;
import com.rapidminer.operator.ports.InputPortExtender;
import com.rapidminer.operator.ports.metadata.MetaData;
import com.rapidminer.parameter.ParameterType;
import com.rapidminer.parameter.ParameterTypeBoolean;
import com.rapidminer.parameter.ParameterTypeInt;
import com.rapidminer.parameter.ParameterTypeList;
import com.rapidminer.parameter.ParameterTypeString;
import com.rapidminer.parameter.ParameterTypeText;
import com.rapidminer.parameter.TextType;
import com.rapidminer.parameter.conditions.BooleanParameterCondition;
import com.rapidminer.tools.MailUtilities;
import com.rapidminer.tools.mail.connection.MailConnectionHandler;
import com.rapidminer.tools.mail.connection.MailConnectionUtilities;


/**
 * Sends mails with attachments, based on {@link SendMailOperator}
 *
 * @author Jonas Wilms-Pfau
 */
public class SendMailAttachmentsOperator extends Operator implements ConnectionSelectionProvider {

	public static final String PARAMETER_TO = "to";
	public static final String PARAMETER_SUBJECT = "subject";
	public static final String PARAMETER_BODY_PLAIN = "body_plain";
	public static final String PARAMETER_BODY_HTML = "body_html";
	public static final String PARAMETER_USE_HTML = "use_html";
	public static final String PARAMETER_HEADERS = "headers";
	public static final String PARAMETER_FILENAMES = "filenames";

	private static final String DEFAULT_FILENAME = "file";
	private static final String DUPLICATE_SUFFIX = " - 2";
	private static final Pattern DUPLICATE_SUFFIX_PATTERN = Pattern.compile(".* - (\\d+)$");
	private static final String FILE_EXTENSION_SEPARATOR = ".";

	private ConnectionInformationSelector selector;
	private InputPortExtender attachmentExtender = new InputPortExtender("attachment", getInputPorts(), new MetaData(FileObject.class), false);

	public SendMailAttachmentsOperator(OperatorDescription description) {
		super(description);
		attachmentExtender.start();
	}

	@Override
	public void doWork() throws OperatorException {
		try {
			Map<String, String> parameters = ValueProviderHandlerRegistry.getInstance().injectValues(getConnectionSelector().getConnection(), this, false);
			MailConnectionHandler.convertSMTPParameters(parameters);
			MultiPartEmail email;
			if (getParameterAsBoolean(PARAMETER_USE_HTML)) {
				email = new HtmlEmail().setHtmlMsg(getParameterAsString(PARAMETER_BODY_HTML));
			} else {
				email = new MultiPartEmail();
				email.setMsg(getParameterAsString(PARAMETER_BODY_PLAIN));
			}
			email.setMailSession(MailUtilities.makeSession(parameters::get));
			email.setFrom(email.getMailSession().getProperties().getProperty(EmailConstants.MAIL_FROM));
			email.addTo(getParameterAsString(PARAMETER_TO));
			email.setSubject(getParameterAsString(PARAMETER_SUBJECT));

			for (String[] entry : getParameterList(PARAMETER_HEADERS)) {
				email.addHeader(entry[0], entry[1]);
			}

			for (Map.Entry<String, File> entry : getAttachments().entrySet()) {
				EmailAttachment attachment = new EmailAttachment();
				attachment.setName(entry.getKey());
				attachment.setPath(entry.getValue().getPath());
				email.attach(attachment);
			}

			email.send();
		} catch (EmailException e) {
			throw new UserError(this, e, "sending_mail_to_address_error", getParameterAsString(PARAMETER_TO), e.getMessage());
		}
		if (selector != null) {
			selector.passDataThrough();
		}
	}

	@Override
	public ConnectionInformationSelector getConnectionSelector() {
		return selector;
	}

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

		type = new ParameterTypeList(PARAMETER_FILENAMES, "Define the filenames.",
				new ParameterTypeInt("index", "Index of the file.", 1, Integer.MAX_VALUE),
				new ParameterTypeString("filename", "The full filename including the file extension."));
		type.setExpert(false);
		types.add(type);

		type = new ParameterTypeList(PARAMETER_HEADERS, "Additional mail headers",
				new ParameterTypeString("header", "Name of the header"),
				new ParameterTypeString("value", "value of the header"));
		types.add(type);

		return types;
	}

	/**
	 * Builds a unique named attachment map
	 *
	 * @return a map from filename to attachment
	 * @throws OperatorException
	 * 		in case a FileObject cannot be converted to a File
	 */
	private Map<String, File> getAttachments() throws OperatorException {
		List<FileObject> attachments = attachmentExtender.getData(FileObject.class, true);
		List<String> filenames = attachments.stream().map(FileObject::getFilename).collect(Collectors.toList());
		// replace filenames with user entered values
		for (String[] entry : getParameterList(PARAMETER_FILENAMES)) {
			try {
				int index = Integer.parseInt(StringUtils.trimToEmpty(entry[0])) - 1;
				if (index < filenames.size() && index >= 0) {
					filenames.set(index, entry[1]);
				}
			} catch (NumberFormatException e) {
				// cannot happen with normal ui usage, log and ignore setting
				logWarning("\"" + entry[0] + "\" is not a valid filename index.");
			}
		}
		// Trim everything to null and replace null values with default name
		filenames = filenames.stream().map(StringUtils::trimToNull).map(s -> Objects.toString(s, DEFAULT_FILENAME)).collect(Collectors.toList());

		// Rename duplicates
		Map<String, File> namedAttachments = new LinkedHashMap<>();
		for (String filename : filenames) {
			// rename duplicates
			if (namedAttachments.containsKey(filename)) {
				do {
					filename = incrementFilename(filename);
				}
				while (filenames.contains(filename) || namedAttachments.containsKey(filename));
			}
			namedAttachments.put(filename, attachments.get(namedAttachments.size()).getFile());
		}
		return namedAttachments;
	}

	/**
	 * Adds a " - n" to the filename starting with - 2
	 *
	 * @param filename
	 * 		the filename
	 * @return the filename with added  " - n"
	 */
	private static String incrementFilename(String filename) {
		int extPosition = filename.lastIndexOf(FILE_EXTENSION_SEPARATOR);
		String prefix = filename.substring(0, extPosition == -1 ? filename.length() : extPosition);
		String suffix = filename.substring(prefix.length());
		Matcher matcher = DUPLICATE_SUFFIX_PATTERN.matcher(prefix);
		if (matcher.matches()) {
			try {
				int newCount = Integer.parseInt(matcher.group(1)) + 1;
				return prefix.substring(0, matcher.start(1)) + newCount + suffix;
			} catch (NumberFormatException e) {
				//ignore and add - 2
			}
		}
		return prefix + DUPLICATE_SUFFIX + suffix;
	}

}
