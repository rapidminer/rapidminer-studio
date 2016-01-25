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

import java.net.MalformedURLException;
import java.net.URL;
import java.util.HashMap;
import java.util.Map;
import java.util.logging.Level;

import org.apache.commons.httpclient.HttpClient;
import org.apache.xmlrpc.XmlRpcException;
import org.apache.xmlrpc.client.XmlRpcClient;
import org.apache.xmlrpc.client.XmlRpcClientConfigImpl;
import org.apache.xmlrpc.client.XmlRpcCommonsTransportFactory;


/**
 * Handles XMLRPC connections to BugZilla.
 * 
 * @author Marco Boeck
 */
public class XmlRpcHandler {

	public static final String BUGZILLA_URL = "http://bugs.rapid-i.com";

	private static final String BUGZILLA_APPENDIX = "xmlrpc.cgi";

	/**
	 * Handles the login to a given BugZilla XmlRpc server.
	 * 
	 * @param serverURL
	 *            the URL to the server, e.g. "http://my.bug-server.com"
	 * @param login
	 *            the BugZilla login
	 * @param password
	 *            the BugZilla password
	 * @return the logged in XmlRpcClient instance
	 * @throws MalformedURLException
	 * @throws XmlRpcException
	 */
	public static synchronized XmlRpcClient login(String serverURL, String login, char[] password)
			throws MalformedURLException, XmlRpcException {
		String server;
		if (serverURL.endsWith("/")) {
			server = serverURL + BUGZILLA_APPENDIX;
		} else {
			server = serverURL + "/" + BUGZILLA_APPENDIX;
		}

		HttpClient httpClient = new HttpClient();
		XmlRpcClient rpcClient = new XmlRpcClient();
		XmlRpcCommonsTransportFactory factory = new XmlRpcCommonsTransportFactory(rpcClient);
		XmlRpcClientConfigImpl config = new XmlRpcClientConfigImpl();

		factory.setHttpClient(httpClient);
		rpcClient.setTransportFactory(factory);
		config.setServerURL(new URL(server));
		rpcClient.setConfig(config);

		// map of the login data
		Map<String, String> loginMap = new HashMap<String, String>();
		loginMap.put("login", login);
		loginMap.put("password", new String(password));
		loginMap.put("rememberlogin", "true");

		Map resultMap = (Map) rpcClient.execute("User.login", new Object[] { loginMap });
		// LogService.getRoot().fine("Logged into BugZilla at '" + serverURL + "' as user '" +
		// resultMap.get("id") + "'.");
		LogService.getRoot().log(Level.FINE, "com.rapidminer.tools.XmlRpcHandler.logged_into_bugzilla",
				new Object[] { serverURL, resultMap.get("id") });

		for (int i = 0; i < password.length; i++) {
			password[i] = 0;
		}

		return rpcClient;
	}

}
