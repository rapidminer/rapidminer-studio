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

import java.io.BufferedOutputStream;
import java.io.IOException;
import java.net.Authenticator;
import java.net.InetSocketAddress;
import java.net.PasswordAuthentication;
import java.net.Proxy;
import java.net.SocketAddress;
import java.net.SocketException;
import java.net.URL;
import java.util.LinkedList;
import java.util.List;
import java.util.logging.Level;
import java.util.logging.Logger;

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

	private final List<URLAuthenticator> serverAuthenticators = new LinkedList<>();
	private final List<URLAuthenticator> proxyAuthenticators = new LinkedList<>();
	private URLAuthenticator socksProxyAuthenticator = null;

	private static GlobalAuthenticator THE_INSTANCE = new GlobalAuthenticator();

	static {
		Authenticator.setDefault(THE_INSTANCE);
		refreshProxyAuthenticators();
	}

	@Deprecated
	/**
	 * This method is deprecated use registerServerAuthenticator instead.
	 */
	public static synchronized void register(URLAuthenticator authenticator) {
		registerServerAuthenticator(authenticator);
	}

	/**
	 * This method adds another Authenticator to the GlobalAuthenticator that will be enqueued in
	 * the list of Authenticators that are tried for URLs that need authentification.
	 */
	public static synchronized void registerServerAuthenticator(URLAuthenticator authenticator) {
		THE_INSTANCE.serverAuthenticators.add(authenticator);
	}

	/**
	 * This method adds another Authenticator to the GlobalAuthenticator that will be enqueued in
	 * the list of Authenticators that are tried for Proxy requests for authentification.
	 */
	public static synchronized void registerProxyAuthenticator(URLAuthenticator authenticator) {
		THE_INSTANCE.proxyAuthenticators.add(authenticator);
	}

	/**
	 * This method adds the default ProxyAuthenticators to the GlobalAuthenticator.
	 */
	public static synchronized void refreshProxyAuthenticators() {
		THE_INSTANCE.proxyAuthenticators.clear();
		THE_INSTANCE.socksProxyAuthenticator = new SocksProxyAuthenticator(ProxyAuthenticator.SOCKS);
		registerProxyAuthenticator(new ProxyAuthenticator(ProxyAuthenticator.HTTP));
		registerProxyAuthenticator(new ProxyAuthenticator(ProxyAuthenticator.HTTPS));
		registerProxyAuthenticator(new ProxyAuthenticator(ProxyAuthenticator.FTP));
	}

	@Override
	protected synchronized PasswordAuthentication getPasswordAuthentication() {
		URL url = getRequestingURL();
		try {
			if (SocksProxyAuthenticator.SOCKS5.equals(this.getRequestingProtocol())) {
				return socksProxyAuthenticator.getAuthentication(url);
			}
			RequestorType requestorType = getRequestorType();
			if (requestorType == null) {
				return PasswordDialog.getPasswordAuthentication(url.toString(), false, true);
			}
			List<URLAuthenticator> authenticators;
			String logKey = "com.rapidminer.tools.GlobalAuthenticator.authentication_requested";
			switch (requestorType) {
				case PROXY:
					logKey += "_proxy";
					authenticators = proxyAuthenticators;
					break;
				case SERVER:
					authenticators = serverAuthenticators;
					break;
				default:
					return null;
			}
			LogService.getRoot().log(Level.FINE, logKey, new Object[]{url, authenticators});
			for (URLAuthenticator a : authenticators) {
				PasswordAuthentication auth = a.getAuthentication(url);
				if (auth != null) {
					return auth;
				}
			}
			// this should not happen
			return PasswordDialog.getPasswordAuthentication(url.toString(), false, requestorType == RequestorType.SERVER);
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

	public interface URLAuthenticator {

		/**
		 * This method returns the PasswordAuthentification if this Authenticator is registered for
		 * the given URL. Otherwise null can be returned.
		 */
		public PasswordAuthentication getAuthentication(URL url) throws PasswordInputCanceledException;

		public String getName();
	}

	private static class ProxyAuthenticator implements URLAuthenticator {

		private static final String PROXY_AUTH = "407";
		private static final int STATUS_FIELD = 0;

		public static final String SOCKS = "socks";
		public static final String HTTP = "http";
		public static final String HTTPS = "https";
		public static final String FTP = "ftp";

		private String protocol;
		protected String username = "";
		protected String password = "";

		public ProxyAuthenticator(String protocol) {
			this.protocol = protocol;
		}

		@Override
		public PasswordAuthentication getAuthentication(URL url) throws PasswordInputCanceledException {
			return getAuthentication(url, "auth.proxy", false);
		}

		public PasswordAuthentication getAuthentication(URL url, String i18n, boolean forceRefresh)
				throws PasswordInputCanceledException {
			if (protocol.equals(SOCKS) || url.getProtocol().equals(protocol)) {
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

					String proxyType = protocol.toUpperCase();
					String proxyID = I18N.getMessage(I18N.getGUIBundle(), "gui.dialog.auth.proxy.id", proxyType);
					String proxyURL = getProxyAddress().toString().replaceAll("/", "");
					String authMessage = getAuthMessage();

					PasswordAuthentication passwordAuthentication = PasswordDialog.getPasswordAuthentication(proxyID,
							proxyURL, forceRefresh, true, i18n, proxyType, proxyURL, authMessage);
					if (passwordAuthentication == null) {
						return null;
					}

					username = passwordAuthentication.getUserName();
					password = new String(passwordAuthentication.getPassword());

					// Verify Settings
					passwordAuthentication = verify(url, passwordAuthentication);

					return passwordAuthentication;
				}

				return new PasswordAuthentication(username, password.toCharArray());
			}
			return null;
		}

		/**
		 * Returns a message to make the authentication mechanism transparent to the user
		 *
		 * @return The i18n message for the given authentication scheme
		 */
		private String getAuthMessage() {
			String i18n = "gui.dialog.auth.proxy.unknown";
			if (GlobalAuthenticator.THE_INSTANCE.getRequestingScheme() != null) {
				switch (GlobalAuthenticator.THE_INSTANCE.getRequestingScheme().toLowerCase().trim()) {
					case "basic":
						i18n = "gui.dialog.auth.proxy.basic";
						break;
					case "digest":
						i18n = "gui.dialog.auth.proxy.digest";
						break;
					case "ntlm":
						i18n = "gui.dialog.auth.proxy.ntlm";
						break;
					case "spnego":
						i18n = "gui.dialog.auth.proxy.negotiate";
						break;
					case "negotiate":
						i18n = "gui.dialog.auth.proxy.negotiate";
						break;
					case "kerberos":
						i18n = "gui.dialog.auth.proxy.kerberos";
						break;
					default:
						// i18n already set
						break;
				}
			}
			return I18N.getMessage(I18N.getGUIBundle(), i18n);
		}

		/**
		 * The current ProxyAddress
		 *
		 * @return The SocketAddress as given by the GlobalAuthenticator
		 */
		protected SocketAddress getProxyAddress() {
			return new InetSocketAddress(GlobalAuthenticator.getInstance().getRequestingHost(),
					GlobalAuthenticator.getInstance().getRequestingPort());
		}

		/**
		 * Verify the proxy by accessing the url using the given PasswordAuthentication
		 *
		 * @param url
		 *            The URL to test
		 * @param pA
		 *            username+password to test
		 * @return A valid passwordAuthentication or null
		 * @throws PasswordInputCanceledException
		 */
		protected PasswordAuthentication verify(URL url, PasswordAuthentication pA) throws PasswordInputCanceledException {
			try {
				// make sure to only call foo.bar not foo.bar/destroy/world
				URL safeUrl = new URL(url.getProtocol(), url.getHost(), url.getPort(), "");
				if (safeUrl.openConnection().getHeaderField(STATUS_FIELD).contains(PROXY_AUTH)) {
					username = "";
					password = "";
					pA = getAuthentication(url, "auth.proxy.wrong.credentials", true);
				}
			} catch (IOException e) {
				Logger.getLogger(ProxyAuthenticator.class.getName()).log(Level.SEVERE,
						I18N.getMessage(I18N.getErrorBundle(), "proxy.credentials.verify.failure", e));
			}
			return pA;
		}

		@Override
		public String getName() {
			return "Proxy Authenticator";
		}
	}

	private static class SocksProxyAuthenticator extends ProxyAuthenticator {

		/**
		 * SOCKS implementation is screwed up, so we don't have access to the requested URI
		 */
		public static final String TEST_URL = "https://www.rapidminer.com";
		/**
		 * Magic string for SOCKS proxies
		 *
		 * @see {@link SocksSocketImpl#authenticate(byte, InputStream, BufferedOutputStream, long)}
		 */
		public static final String SOCKS5 = "SOCKS5";

		/**
		 * @param protocol
		 */
		public SocksProxyAuthenticator(String protocol) {
			super(protocol);
		}

		@Override
		/**
		 * Since socks version 4 does not support authentication, we can assume version 5 and use
		 * the Proxy object here instead of sun.net.SocksProxy which also supports v4
		 * <p>
		 * Warning: The behaviour might change with Java 9 use ProxySelector.getDefault().select()
		 * lastElement to receive an SocksProxy
		 * </p>
		 */
		protected PasswordAuthentication verify(URL url, PasswordAuthentication pA) throws PasswordInputCanceledException {
			try {
				if (url == null) {
					url = new URL(TEST_URL);
				}
				// make sure to only call foo.bar not foo.bar/destroy/world
				URL safeUrl = new URL(url.getProtocol(), url.getHost(), url.getPort(), "");
				safeUrl.openConnection(new Proxy(Proxy.Type.SOCKS, getProxyAddress())).connect();
			} catch (SocketException se) {
				username = "";
				password = "";
				Logger.getLogger(SocksProxyAuthenticator.class.getName()).log(Level.FINER, se.getMessage());
				pA = getAuthentication(url, "auth.proxy.wrong.credentials", true);
			} catch (IOException e) {
				Logger.getLogger(SocksProxyAuthenticator.class.getName()).log(Level.SEVERE,
						I18N.getMessage(I18N.getErrorBundle(), "proxy.credentials.verify.failure", e));
			}
			return pA;
		}
	}

}
