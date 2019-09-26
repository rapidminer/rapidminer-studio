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

import java.util.Objects;
import java.util.Properties;
import java.util.function.UnaryOperator;
import java.util.logging.Level;
import javax.crypto.Cipher;
import javax.mail.Authenticator;
import javax.mail.PasswordAuthentication;
import javax.mail.Session;

import com.rapidminer.RapidMiner;
import com.rapidminer.tools.mail.connection.MailConnectionHandler;


/**
 * Makes a session based on RapidMiner properties.
 *
 * @author Simon Fischer, Jonas Wilms-Pfau
 *
 */
public class DefaultMailSessionFactory implements MailSessionFactory {

	/** Diffie Hellman key size */
	private static final String DH_SIZE = System.getProperty("jdk.tls.ephemeralDHKeySize");
	/** Check if 256 is allowed */
	private static final boolean AES_256_ALLOWED = isAES256Supported();

	/** Elliptic curve Diffie-Hellman cipher suites recommended by BSI TR-02102-2 */
	private static final String ECDH_CIPHERSUITES = "TLS_ECDHE_ECDSA_WITH_AES_128_CBC_SHA256 "
			+ "TLS_ECDHE_ECDSA_WITH_AES_128_GCM_SHA256 " + "TLS_ECDHE_RSA_WITH_AES_128_CBC_SHA256 "
			+ "TLS_ECDHE_RSA_WITH_AES_128_GCM_SHA256 ";
	private static final String ECDH_UNLIMITED_CIPHERSUITES = "TLS_ECDHE_RSA_WITH_AES_256_CBC_SHA384 "
			+ "TLS_ECDHE_RSA_WITH_AES_256_GCM_SHA384 ";
	/** Diffie-Hellman cipher suites recommended by BSI TR-02102-2 */
	private static final String DH_CIPHERSUITES = "TLS_DHE_DSS_WITH_AES_128_CBC_SHA256 "
			+ "TLS_DHE_DSS_WITH_AES_128_GCM_SHA256 " + "TLS_DHE_RSA_WITH_AES_128_GCM_SHA256 ";
	private static final String DH_UNLIMITED_CIPHERSUITES = "TLS_DHE_DSS_WITH_AES_256_CBC_SHA256 "
			+ "TLS_DHE_DSS_WITH_AES_256_GCM_SHA384 " + "TLS_DHE_RSA_WITH_AES_256_GCM_SHA384 ";
	/** All supported TLS1.2 PFS Ciphersuites */
	private static final String PFS_CIPHER_SUITES = getSupportedPFSCipherSuites();

	private static final String ENABLE_STARTTLS = "mail.smtp.starttls.enable";
	private static final String SSL_PROTOCOLS = "mail.smtp.ssl.protocols";

	@Override
	public Session makeSession() {
		return makeSession(MailUtilities.OLD_MAIL_PROPERTIES, MailUtilities.DECRYPT_WITH_CIPHER_KEY);
	}

	@Override
	public Session makeSession(UnaryOperator<String> properties, UnaryOperator<String> pwDecoder) {
		String host = properties.apply(RapidMiner.PROPERTY_RAPIDMINER_TOOLS_SMTP_HOST);
		if (host == null) {
			LogService.getRoot().log(Level.WARNING,
					"com.rapidminer.tools.DefaultMailSessionFactory.smtp_host_must_be_specified",
					RapidMiner.PROPERTY_RAPIDMINER_TOOLS_SMTP_HOST);
			return null;
		}
		Properties props = new Properties();
		props.put("mail.smtp.connectiontimeout", WebServiceTools.TIMEOUT_URL_CONNECTION);
		props.put("mail.smtp.timeout", WebServiceTools.TIMEOUT_URL_CONNECTION);
		props.put("mail.smtp.host", host);

		// Set the mail sender
		String from = properties.apply(RapidMiner.PROPERTY_RAPIDMINER_TOOLS_MAIL_SENDER);
		if (from != null && from.contains("@")) {
			props.put("mail.from", from.trim());
		} else {
			props.put("mail.from", MailConnectionHandler.DEFAULT_SENDER);
		}
		final String user = Objects.toString(properties.apply(RapidMiner.PROPERTY_RAPIDMINER_TOOLS_SMTP_USER), "");
		props.put("mail.user", user);

		// Allow debug mode
		if (Tools.booleanValue(ParameterService.getParameterValue(RapidMiner.PROPERTY_RAPIDMINER_GENERAL_DEBUGMODE),false)) {
			props.put("mail.debug", "true");
		}

		// Setup Security
		switch (Objects.toString(properties.apply(RapidMiner.PROPERTY_RAPIDMINER_TOOLS_SMTP_SECURITY), "")) {
			case RapidMiner.PROPERTY_RAPIDMINER_TOOLS_SMTP_SECURITY_STARTTLS:
				props.setProperty(ENABLE_STARTTLS, "true");
				break;
			case RapidMiner.PROPERTY_RAPIDMINER_TOOLS_SMTP_SECURITY_STARTTLS_ENFORCE:
				props.setProperty(ENABLE_STARTTLS, "true");
				props.setProperty("mail.smtp.starttls.required", "true");
				break;
			case RapidMiner.PROPERTY_RAPIDMINER_TOOLS_SMTP_SECURITY_STARTTLS_ENFORCE_PFS:
				props.setProperty(ENABLE_STARTTLS, "true");
				props.setProperty("mail.smtp.starttls.required", "true");
				props.setProperty(SSL_PROTOCOLS, "TLSv1.2");
				props.setProperty("mail.smtp.ssl.checkserveridentity", "true");
				props.setProperty("mail.smtp.ssl.ciphersuites", PFS_CIPHER_SUITES);
				break;
			case RapidMiner.PROPERTY_RAPIDMINER_TOOLS_SMTP_SECURITY_TLS:
				props.setProperty("mail.smtp.ssl.enable", "true");
				props.setProperty(SSL_PROTOCOLS, "TLSv1 TLSv1.1 TLSv1.2");
				break;
			case RapidMiner.PROPERTY_RAPIDMINER_TOOLS_SMTP_SECURITY_TLS_PFS:
				props.setProperty("mail.smtp.ssl.enable", "true");
				props.setProperty(SSL_PROTOCOLS, "TLSv1.2");
				props.setProperty("mail.smtp.ssl.checkserveridentity", "true");
				props.setProperty("mail.smtp.ssl.ciphersuites", PFS_CIPHER_SUITES);
				break;
			case RapidMiner.PROPERTY_RAPIDMINER_TOOLS_SMTP_SECURITY_NONE:
			default:
				break;
		}

		// Setup Authentication
		Authenticator authenticator = null;
		String passwd = Objects.toString(properties.apply(RapidMiner.PROPERTY_RAPIDMINER_TOOLS_SMTP_PASSWD), "");
		if (pwDecoder != null) {
			passwd = pwDecoder.apply(passwd);
		}
		if (passwd.length() > 0) {
			props.setProperty("mail.smtp.submitter", user);
			props.setProperty("mail.smtp.auth", "true");

			// Set the Authentication mechanism
			switch (Objects.toString(properties.apply(RapidMiner.PROPERTY_RAPIDMINER_TOOLS_SMTP_AUTHENTICATION), "")) {
				case RapidMiner.PROPERTY_RAPIDMINER_TOOLS_SMTP_AUTHENTICATION_CRAM_MD5:
					props.setProperty("mail.smtp.sasl.enable", "true");
					props.setProperty("mail.smtp.sasl.mechanisms", "CRAM-MD5");
					// Workaround for silent sasl downgrade bug in JavaMail < 1.5.2
					props.setProperty("mail.smtp.auth.mechanisms", "DIGEST-MD5");
					break;
				case RapidMiner.PROPERTY_RAPIDMINER_TOOLS_SMTP_AUTHENTICATION_NTLM:
					props.setProperty("mail.smtp.auth.mechanisms", "NTLM");
					break;
				case RapidMiner.PROPERTY_RAPIDMINER_TOOLS_SMTP_AUTHENTICATION_AUTO:
				default:
					break;
			}

			final String password = passwd;
			authenticator = new Authenticator() {

				@Override
				protected PasswordAuthentication getPasswordAuthentication() {
					return new PasswordAuthentication(user, password);
				}
			};
		}

		String port = properties.apply(RapidMiner.PROPERTY_RAPIDMINER_TOOLS_SMTP_PORT);
		if (port != null) {
			props.setProperty("mail.smtp.port", port);
		}
		return Session.getInstance(props, authenticator);
	}

	/**
	 * Get all PFS cipher suites
	 *
	 * @return the supported PFS cipher suites
	 */
	private static String getSupportedPFSCipherSuites() {
		// Tries to follow the BSI TR-02102-2 recommendation
		// User has to set jdk.tls.ephemeralDHKeySize to 2048 or matched for DH
		StringBuilder cypherSuites = new StringBuilder(ECDH_CIPHERSUITES);
		if (AES_256_ALLOWED) {
			cypherSuites.append(ECDH_UNLIMITED_CIPHERSUITES);
		}
		if ("2048".equals(DH_SIZE) || "matched".equals(DH_SIZE)) {
			cypherSuites.append(DH_CIPHERSUITES);
			if (AES_256_ALLOWED) {
				cypherSuites.append(DH_UNLIMITED_CIPHERSUITES);
			}
		}
		return cypherSuites.toString();
	}

	/**
	 * Check if AES_256 is allowed
	 *
	 * @return {@code true} if AES_256 is allowed
	 */
	private static boolean isAES256Supported() {
		boolean allowed;
		try {
			allowed = Cipher.getMaxAllowedKeyLength("AES") >= 256;
		} catch (Exception e) {
			allowed = false;
		}
		return allowed;
	}

}
