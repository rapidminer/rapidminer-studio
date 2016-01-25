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

import java.net.Authenticator;
import java.net.PasswordAuthentication;
import java.net.URL;
import java.util.LinkedList;
import java.util.List;
import java.util.logging.Level;

import com.rapidminer.gui.tools.PasswordDialog;
import com.rapidminer.tools.cipher.CipherException;
import com.rapidminer.tools.cipher.CipherTools;


/**
 * Global authenticator at which multiple other authenticators can register. Authentication requests
 * will be delegated subsequently until an authenticator is found.
 *
 * @author Simon Fischer
 *
 */
public class GlobalAuthenticator extends Authenticator {

	private final List<URLAuthenticator> serverAuthenticators = new LinkedList<URLAuthenticator>();
	private final List<URLAuthenticator> proxyAuthenticators = new LinkedList<URLAuthenticator>();

	private static final GlobalAuthenticator THE_INSTANCE = new GlobalAuthenticator();

	public interface URLAuthenticator {

		/**
		 * This method returns the PasswordAuthentification if this Authenticator is registered for
		 * the given URL. Otherwise null can be returned.
		 */
		public PasswordAuthentication getAuthentication(URL url) throws PasswordInputCanceledException;

		public String getName();
	}

	private static class ProxyAuthenticator implements URLAuthenticator {

		private String protocol;

		public ProxyAuthenticator(String protocol) {
			this.protocol = protocol;
		}

		@Override
		public PasswordAuthentication getAuthentication(URL url) throws PasswordInputCanceledException {
			if (url.getProtocol().equals(protocol)) {
				String username = ParameterService.getParameterValue(protocol + ".proxyUsername");
				String password = ParameterService.getParameterValue(protocol + ".proxyPassword");
				// password is stored encrypted, try to decrypt password
				if (password != null && CipherTools.isKeyAvailable()) {
					try {
						password = CipherTools.decrypt(password);
					} catch (CipherException e) {
						// password is in plaintext
					}
				}
				if (username == null || username.isEmpty() || password == null) {  // empty
					// passwords
					// possibly
					// valid!
					PasswordAuthentication passwordAuthentication = PasswordDialog.getPasswordAuthentication("proxy for "
							+ url.toString(), true, false);
					if (passwordAuthentication == null) {
						return null;
					}
					ParameterService.setParameterValue(protocol + ".proxyUsername", passwordAuthentication.getUserName());
					ParameterService.setParameterValue(protocol + ".proxyPassword",
							new String(passwordAuthentication.getPassword()));
					ParameterService.saveParameters();

					return passwordAuthentication;
				}
				return new PasswordAuthentication(username, password.toCharArray());
			}
			return null;
		}

		@Override
		public String getName() {
			return "Proxy Authenticator";
		}
	}

	static {
		Authenticator.setDefault(THE_INSTANCE);
		registerProxyAuthenticator(new ProxyAuthenticator("http"));
		registerProxyAuthenticator(new ProxyAuthenticator("https"));
		registerProxyAuthenticator(new ProxyAuthenticator("ftp"));
		registerProxyAuthenticator(new ProxyAuthenticator("socks"));
	}

	@Deprecated
	/**
	 * This method is deprecated use registerServerAuthenticator instead.
	 */
	public synchronized static void register(URLAuthenticator authenticator) {
		registerServerAuthenticator(authenticator);
	}

	/**
	 * This method adds another Authenticator to the GlobalAuthenticator that will be enqueued in
	 * the list of Authenticators that are tried for URLs that need authentification.
	 */
	public synchronized static void registerServerAuthenticator(URLAuthenticator authenticator) {
		THE_INSTANCE.serverAuthenticators.add(authenticator);
	}

	/**
	 * This method adds another Authenticator to the GlobalAuthenticator that will be enqueued in
	 * the list of Authenticators that are tried for Proxy requests for authentification.
	 */
	public synchronized static void registerProxyAuthenticator(URLAuthenticator authenticator) {
		THE_INSTANCE.proxyAuthenticators.add(authenticator);
	}

	@Override
	protected synchronized PasswordAuthentication getPasswordAuthentication() {
		URL url = getRequestingURL();
		try {
			switch (getRequestorType()) {
				case PROXY:
					LogService.getRoot().log(Level.FINE,
							"com.rapidminer.tools.GlobalAuthenticator.authentication_requested_proxy",
							new Object[] { url, proxyAuthenticators });
					for (URLAuthenticator a : proxyAuthenticators) {
						PasswordAuthentication auth = a.getAuthentication(url);
						if (auth != null) {
							return auth;
						}
					}
					// this should not be happen
					return PasswordDialog.getPasswordAuthentication(url.toString(), false, false);
				case SERVER:
					LogService.getRoot().log(Level.FINE,
							"com.rapidminer.tools.GlobalAuthenticator.authentication_requested",
							new Object[] { url, serverAuthenticators });
					for (URLAuthenticator a : serverAuthenticators) {
						PasswordAuthentication auth = a.getAuthentication(url);
						if (auth != null) {
							return auth;
						}
					}
			}
			return PasswordDialog.getPasswordAuthentication(url.toString(), false, true);
		} catch (PasswordInputCanceledException e) {
			return null;
		}
	}

	/**
	 * This method is called to cause the loading of the class and the execution of static blocks.
	 */
	public static void init() {}

	public static GlobalAuthenticator getInstance() {
		return THE_INSTANCE;
	}
}
