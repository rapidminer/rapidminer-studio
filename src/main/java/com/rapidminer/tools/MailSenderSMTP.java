/**
 * Copyright (C) 2001-2016 by RapidMiner and the contributors
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
import java.util.logging.Level;

import javax.mail.Message;
import javax.mail.Session;
import javax.mail.Transport;
import javax.mail.internet.MimeMessage;


/**
 * Sends a mail via SMTP.
 * 
 * @author Simon Fischer, Ingo Mierswa
 */
public class MailSenderSMTP implements MailSender {

	@Override
	public void sendEmail(String address, String subject, String content, Map<String, String> headers) throws Exception {
		Session session = MailUtilities.makeSession();
		if (session == null) {
			// LogService.getRoot().warning("Unable to create mail session. Not sending mail to "+address+".");
			LogService.getRoot().log(Level.WARNING, "com.rapidminer.tools.MailSenderSMTP.creating_mail_session_error",
					address);
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
}
