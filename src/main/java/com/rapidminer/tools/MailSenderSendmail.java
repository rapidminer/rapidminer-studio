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
package com.rapidminer.tools;

import java.io.PrintStream;
import java.util.Map;
import java.util.function.UnaryOperator;
import java.util.logging.Level;

import com.rapidminer.RapidMiner;


/**
 * Sends a mail via sendmail.
 *
 * @author Simon Fischer, Ingo Mierswa
 */
public class MailSenderSendmail implements MailSender {

	@Override
	public void sendEmail(String address, String subject, String content, Map<String, String> headers) throws Exception {
		sendEmail(address, subject, content, headers, MailUtilities.OLD_MAIL_PROPERTIES);
	}

	@Override
	public void sendEmail(String address, String subject, String content, Map<String, String> headers, UnaryOperator<String> properties) throws Exception {
		String command = properties.apply(RapidMiner.PROPERTY_RAPIDMINER_TOOLS_SENDMAIL_COMMAND);
		if (command == null || command.isEmpty()) {
			LogService.getRoot().log(Level.WARNING, "com.rapidminer.tools.MailSenderSendMail.specify_sendmail_command");
			return;
		}
		LogService.getRoot().log(Level.FINE, "com.rapidminer.tools.MailSenderSendMail.executing_command", command);
		if (headers != null && !headers.isEmpty()) {
			LogService.getRoot().log(Level.WARNING,
					"com.rapidminer.tools.MailSenderSendMail.ignoring_mail_headers_for_sendmail");
		}
		Process sendmail = Runtime.getRuntime().exec(new String[] { command, address });
		try (PrintStream out = new PrintStream(sendmail.getOutputStream())) {
			out.println("Subject: " + subject);
			String from = properties.apply(RapidMiner.PROPERTY_RAPIDMINER_TOOLS_MAIL_SENDER);
			if (from != null && !from.trim().isEmpty()) {
				out.println("From: " + from.trim());
			} else {
				out.println("From: RapidMiner");
			}
			out.println("To: " + address);
			out.println();
			out.println(content);
		}
		Tools.waitForProcess(null, sendmail, command);
	}
}
