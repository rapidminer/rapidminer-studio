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

import java.util.Map;
import java.util.function.UnaryOperator;
import java.util.logging.Level;
import javax.mail.Session;

import com.rapidminer.RapidMiner;
import com.rapidminer.operator.MailNotSentException;
import com.rapidminer.tools.cipher.CipherException;
import com.rapidminer.tools.cipher.CipherTools;


/**
 *
 * @author Simon Fischer, Nils Woehler
 *
 */
public class MailUtilities {

	/** @since 9.4.1 */
	static final UnaryOperator<String> OLD_MAIL_PROPERTIES = ParameterService::getParameterValue;

	/** @since 9.4.1 */
	public static final UnaryOperator<String> DECRYPT_WITH_CIPHER_KEY = pwd -> {
		if (CipherTools.isKeyAvailable()) {
			try {
				return CipherTools.decrypt(pwd);
			} catch (CipherException e) {
				// passwd is in plaintext
				LogService.getRoot().log(Level.WARNING,
						"com.rapidminer.tools.DefaultMailSessionFactory.smtp_password_decode_failed");
			}
		} else {
			LogService.getRoot().log(Level.WARNING,
					"com.rapidminer.tools.DefaultMailSessionFactory.smtp_password_cipher_missing");
		}
		return pwd;
	};

	private static MailSessionFactory mailFactory = new DefaultMailSessionFactory();

	public static void setMailSessionFactory(MailSessionFactory mailFactory) {
		MailUtilities.mailFactory = mailFactory;
	}

	/**
	 * @deprecated since 9.4.1; use {@link #sendEmailWithException(String, String, String, Map, UnaryOperator)} instead
	 */
	@Deprecated
	public static void sendEmail(String address, String subject, String content) {
		sendEmail(address, subject, content, null);
	}

	/**
	 * Sends a mail to the given address, using the specified subject and contents. Subject must
	 * contain no whitespace!
	 *
	 * @deprecated since 9.4.1; use {@link #sendEmailWithException(String, String, String, Map, UnaryOperator)} instead
	 */
	@Deprecated
	public static void sendEmail(String address, String subject, String content, Map<String, String> headers) {
		try {
			sendEmailWithException(address, subject, content, headers);
		} catch (MailNotSentException e) {
			// this methods throws no exceptions
		}
	}

	/** @deprecated since 9.4.1; use {@link #sendEmailWithException(String, String, String, Map, UnaryOperator)} instead */
	@Deprecated
	public static void sendEmailWithException(String address, String subject, String content, Map<String, String> headers)
			throws MailNotSentException {
		sendEmailWithException(address, subject, content, headers, OLD_MAIL_PROPERTIES);
	}

	/**
	 * Sends an email using the mail parameters and mail connection properties provided.
	 *
	 * @param address
	 * 		the email address to send to
	 * @param subject
	 * 		the subject of the email
	 * @param content
	 * 		the email content
	 * @param headers
	 * 		additional email headers
	 * @param properties
	 * 		the connection property lookup
	 * @throws MailNotSentException
	 * 		if an error occurs during sending the email
	 * @since 9.4.1
	 */
	public static void sendEmailWithException(String address, String subject, String content, Map<String, String> headers,
											  UnaryOperator<String> properties) throws MailNotSentException {
		try {
			String method = properties.apply(RapidMiner.PROPERTY_RAPIDMINER_TOOLS_MAIL_METHOD);
			int methodIndex = getMailMethodIndex(method);

			MailSender mailSender = null;
			switch (methodIndex) {
				case RapidMiner.PROPERTY_RAPIDMINER_TOOLS_MAIL_METHOD_SMTP:
					mailSender = new MailSenderSMTP();
					break;
				case RapidMiner.PROPERTY_RAPIDMINER_TOOLS_MAIL_METHOD_SENDMAIL:
					mailSender = new MailSenderSendmail();
					break;
				default:
					LogService.getRoot().log(Level.SEVERE,
							"com.rapidminer.tools.MailUtilities.illegal_send_mail_method", method);
					throw new MailNotSentException("Illegal send mail method", "illegal_send_mail_method", method);
			}

			mailSender.sendEmail(address, subject, content, headers, properties);
			LogService.getRoot().log(Level.INFO, "com.rapidminer.tools.MailUtilities.sent_mail_to_adress_with_subject",
					new Object[] { address, subject });
		} catch (Exception e) {
			LogService.getRoot().log(Level.SEVERE, "com.rapidminer.tools.MailUtilities.sending_mail_to_address_error",
					new Object[] { address, e });
			throw new MailNotSentException("Cannot send mail", e, "sending_mail_to_address_error", address, e);
		}
	}

	/**	@deprecated since 9.4.1; use {@link #sendEmailWithException(String, String, String, Map, UnaryOperator)} instead */
	@Deprecated
	public static void sendEmailWithException(String address, String subject, String content) throws MailNotSentException {
		sendEmailWithException(address, subject, content, null);
	}

	/**
	 * Create a standard session with {@link MailSessionFactory#makeSession()}
	 *
	 * @deprecated since 9.4.1; use {@link #makeSession(UnaryOperator)} instead
	 */
	@Deprecated
	public static Session makeSession() {
		return mailFactory.makeSession();
	}

	/**
	 * Create a {@link Session} from the given properties. If the properties are {@code null} or {@link #OLD_MAIL_PROPERTIES},
	 * a default session is created with {@link #makeSession()}. Otherwise,
	 * {@link MailSessionFactory#makeSession(UnaryOperator, UnaryOperator) MailSessionFactory.makeSession(properties, null}
	 * is used.
	 *
	 * @param properties the properties; can be {@code null}
	 * @return the session
	 * @since 9.4.1
	 */
	@SuppressWarnings("deprecation")
	public static Session makeSession(UnaryOperator<String> properties) {
		if (properties == null || properties == OLD_MAIL_PROPERTIES) {
			return makeSession();
		}
		return mailFactory.makeSession(properties, null);
	}

	/**
	 * Find the index (one of {@link RapidMiner#PROPERTY_RAPIDMINER_TOOLS_MAIL_METHOD_SENDMAIL} or
	 * {@link RapidMiner#PROPERTY_RAPIDMINER_TOOLS_MAIL_METHOD_SMTP} of the given method name.
	 * Will return the corresponding index, if the method name is in {@link RapidMiner#PROPERTY_RAPIDMINER_TOOLS_MAIL_METHOD_VALUES},
	 * otherwise returns {@link RapidMiner#PROPERTY_RAPIDMINER_TOOLS_MAIL_METHOD_SMTP}.
	 *
	 * @param method the name of the method
	 * @return the index of the method, or {@link RapidMiner#PROPERTY_RAPIDMINER_TOOLS_MAIL_METHOD_SMTP} if invalid name
	 * @since 9.4.1
	 */
	public static int getMailMethodIndex(String method) {
		int methodIndex = -1;
		if (method != null) {
			try {
				methodIndex = Integer.parseInt(method);
			} catch (NumberFormatException e) {
				for (int i = 0; i < RapidMiner.PROPERTY_RAPIDMINER_TOOLS_MAIL_METHOD_VALUES.length; i++) {
					if (RapidMiner.PROPERTY_RAPIDMINER_TOOLS_MAIL_METHOD_VALUES[i].equals(method)) {
						return i;
					}
				}
			}
		}
		switch (methodIndex) {
			case RapidMiner.PROPERTY_RAPIDMINER_TOOLS_MAIL_METHOD_SENDMAIL:
				return methodIndex;
			case RapidMiner.PROPERTY_RAPIDMINER_TOOLS_MAIL_METHOD_SMTP:
			default:
				return RapidMiner.PROPERTY_RAPIDMINER_TOOLS_MAIL_METHOD_SMTP;
		}
	}
}
