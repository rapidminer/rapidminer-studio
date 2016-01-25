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

import com.rapidminer.RapidMiner;
import com.rapidminer.tools.cipher.CipherException;
import com.rapidminer.tools.cipher.CipherTools;

import java.util.Properties;
import java.util.logging.Level;

import javax.mail.Authenticator;
import javax.mail.PasswordAuthentication;
import javax.mail.Session;


/**
 * Makes a session based on RapidMiner properties.
 * 
 * @author Simon Fischer
 * 
 */
public class DefaultMailSessionFactory implements MailSessionFactory {

	@Override
	public Session makeSession() {
		String host = ParameterService.getParameterValue(RapidMiner.PROPERTY_RAPIDMINER_TOOLS_SMTP_HOST);
		if (host == null) {
			// LogService.getRoot().warning("Must specify SMTP host in "+RapidMiner.PROPERTY_RAPIDMINER_TOOLS_SMTP_HOST+" to use SMTP.");
			LogService.getRoot().log(Level.WARNING,
					"com.rapidminer.tools.DefaultMailSessionFactory.smtp_host_must_be_specified",
					RapidMiner.PROPERTY_RAPIDMINER_TOOLS_SMTP_HOST);
			return null;
		} else {
			Properties props = new Properties();
			props.put("mail.smtp.host", host);
			props.put("mail.from", "no-reply@rapidminer.com");
			final String user = ParameterService.getParameterValue(RapidMiner.PROPERTY_RAPIDMINER_TOOLS_SMTP_USER);
			props.put("mail.user", user);
			Authenticator authenticator = null;
			final String passwd;
			try {
				if (CipherTools.isKeyAvailable()) {
					passwd = CipherTools.decrypt(ParameterService
							.getParameterValue(RapidMiner.PROPERTY_RAPIDMINER_TOOLS_SMTP_PASSWD));
				} else {
					passwd = "";
					LogService.getRoot().log(Level.WARNING,
							"com.rapidminer.tools.DefaultMailSessionFactory.smtp_password_cipher_missing");
				}
				if (passwd.length() > 0) {
					props.setProperty("mail.smtp.submitter", user);
					props.setProperty("mail.smtp.auth", "true");
					authenticator = new Authenticator() {

						@Override
						protected PasswordAuthentication getPasswordAuthentication() {
							return new PasswordAuthentication(user, passwd);
						}
					};
				}
			} catch (CipherException e) {
				LogService.getRoot().log(Level.WARNING,
						"com.rapidminer.tools.DefaultMailSessionFactory.smtp_password_decode_failed");
			}
			String port = ParameterService.getParameterValue(RapidMiner.PROPERTY_RAPIDMINER_TOOLS_SMTP_PORT);
			if (port != null) {
				props.setProperty("mail.smtp.port", port);
			}
			return Session.getInstance(props, authenticator);
		}
	}

}
