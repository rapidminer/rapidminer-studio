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

import java.util.Date;
import java.util.Map;
import java.util.Map.Entry;
import java.util.function.UnaryOperator;
import java.util.logging.Level;
import javax.mail.Message;
import javax.mail.MessagingException;
import javax.mail.Session;
import javax.mail.Transport;
import javax.mail.internet.MimeMessage;

import com.rapidminer.RapidMiner;

/**
 * Sends a mail via SMTP.
 *
 * @author Simon Fischer, Ingo Mierswa
 */
public class MailSenderSMTP implements MailSender {

	private static final String SESSION_CREATION_FAILURE = "com.rapidminer.tools.MailSenderSMTP.creating_mail_session_error";

	@Override
	public void sendEmail(String address, String subject, String content, Map<String, String> headers) throws Exception {
		sendEmail(address, subject, content, headers, null);
	}

	@Override
	public void sendEmail(String address, String subject, String content, Map<String, String> headers,
						  UnaryOperator<String> properties) throws Exception {
		Session session = MailUtilities.makeSession(properties);
		if (session == null) {
			LogService.getRoot().log(Level.WARNING, SESSION_CREATION_FAILURE, address);
		}
		MimeMessage msg = new MimeMessage(session);
		msg.setRecipients(Message.RecipientType.TO, address);
		msg.setFrom();
		msg.setSubject(subject, "UTF-8");
		msg.setSentDate(new Date());
		msg.setText(content, "UTF-8");

		if (headers != null) {
			for (Entry<String, String> header : headers.entrySet()) {
				msg.setHeader(header.getKey(), header.getValue());
			}
		}
		Transport.send(msg);
	}

	/**
	 * Test the smtp configuration provided by the properties with the possibility to interrupt it.
	 *
	 * @param properties
	 * 		the property lookup needed to create the SMTP session
	 * @return an interruptable supplier that executes the test
	 * @see com.rapidminer.tools.mail.connection.MailConnectionHandler#test(com.rapidminer.connection.util.TestExecutionContext) MailConnectionHandler.test
	 * @since 9.4.1
	 */
	public InterruptableSupplier<Exception> testEmailWithInterrupt(UnaryOperator<String> properties) {
		Session session = MailUtilities.makeSession(properties);
		if (session == null) {
			LogService.getRoot().log(Level.WARNING, SESSION_CREATION_FAILURE, "test");
			return () -> new NullPointerException(SESSION_CREATION_FAILURE);
		}
		int port;
		try {
			port = Integer.parseInt(properties.apply(RapidMiner.PROPERTY_RAPIDMINER_TOOLS_SMTP_PORT));
		} catch (NullPointerException | NumberFormatException e) {
			return () -> e;
		}
		String host = properties.apply(RapidMiner.PROPERTY_RAPIDMINER_TOOLS_SMTP_HOST);
		String user = properties.apply(RapidMiner.PROPERTY_RAPIDMINER_TOOLS_SMTP_USER);
		String pw = properties.apply(RapidMiner.PROPERTY_RAPIDMINER_TOOLS_SMTP_PASSWD);
		return new InterruptableSupplier<Exception>() {
			private Transport t;
			@Override
			public Exception get() {
				try (Transport transport = session.getTransport("smtp")) {
					t = transport;
					transport.connect(host, port, user, pw);
				} catch (MessagingException e) {
					return e;
				}
				return null;
			}

			@Override
			public void interrupt() {
				if (t != null) {
					try {
						t.close();
					} catch (MessagingException e) {
						// ignore
					}
				}
			}
		};
	}
}
