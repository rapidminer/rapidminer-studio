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

import java.io.IOException;
import java.io.InputStream;
import java.lang.reflect.Constructor;
import java.lang.reflect.Method;
import java.net.HttpURLConnection;
import java.net.URL;
import java.net.URLConnection;
import java.util.Map;
import java.util.logging.Level;
import javax.xml.ws.BindingProvider;

import com.fasterxml.jackson.core.JsonParseException;
import com.fasterxml.jackson.databind.DeserializationFeature;
import com.fasterxml.jackson.databind.JsonMappingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.rapidminer.tools.net.UrlFollower;
import com.rapidminer.tools.parameter.ParameterChangeListener;


/**
 * Some utility methods for web services and url connections.
 *
 * @author Simon Fischer, Marco Boeck
 *
 */
public class WebServiceTools {

	// three minutes
	private static final int READ_TIMEOUT = 180000;

	private static final int CHUNKED_SIZE = 4096 * 16;

	public static final String WEB_SERVICE_TIMEOUT = "connection.timeout";

	/** the timeout in ms used for url connections */
	public static int TIMEOUT_URL_CONNECTION;

	static {
		String timeoutStr = ParameterService.getParameterValue(WEB_SERVICE_TIMEOUT);
		if (timeoutStr != null) {
			TIMEOUT_URL_CONNECTION = Integer.parseInt(timeoutStr);
		} else {
			TIMEOUT_URL_CONNECTION = READ_TIMEOUT;
		}

		ParameterService.registerParameterChangeListener(new ParameterChangeListener() {

			@Override
			public void informParameterSaved() {}

			@Override
			public void informParameterChanged(String key, String value) {
				if (WEB_SERVICE_TIMEOUT.equals(key)) {
					if (value != null) {
						TIMEOUT_URL_CONNECTION = Integer.parseInt(value);
					}
				}
			}
		});
	}

	public static void setTimeout(BindingProvider port) {
		setTimeout(port, TIMEOUT_URL_CONNECTION);
	}

	/**
	 * Sets the timeout for this web service client. Every port created by a JAX-WS can be cast to
	 * BindingProvider.
	 */
	public static void setTimeout(BindingProvider port, int timeout) {
		if (port == null) {
			throw new IllegalArgumentException("port must not be null!");
		}

		Map<String, Object> ctxt = port.getRequestContext();
		ctxt.put("com.sun.xml.ws.developer.JAXWSProperties.CONNECT_TIMEOUT", timeout);
		ctxt.put("com.sun.xml.ws.connect.timeout", timeout);
		ctxt.put("com.sun.xml.ws.request.timeout", timeout);
		ctxt.put("com.sun.xml.internal.ws.connect.timeout", timeout);
		ctxt.put("com.sun.xml.internal.ws.request.timeout", timeout);

		// We don't want to use proprietary Sun code
		// ctxt.put(BindingProviderProperties.REQUEST_TIMEOUT, timeout);
		// ctxt.put(BindingProviderProperties.CONNECT_TIMEOUT, timeout);
	}

	/** Pre-authenticates the Web service if password is not null. */
	public static void setCredentials(BindingProvider bp, String username, char[] password) {
		if (password != null && password.length > 0) {
			bp.getRequestContext().put(BindingProvider.USERNAME_PROPERTY, username);
			bp.getRequestContext().put(BindingProvider.PASSWORD_PROPERTY, new String(password));
		}
	}

	/**
	 * Sets some default settings for {@link URLConnection}s, e.g. timeouts.
	 */
	public static void setURLConnectionDefaults(URLConnection connection) {
		setURLConnectionDefaults(connection, false);
	}

	/**
	 * Sets some default settings for {@link URLConnection}s, e.g. timeouts. Allows to specify whether the connection
	 * should have chunked streaming mode enabled (works only for HttpURLConnections).
	 */
	public static void setURLConnectionDefaults(URLConnection connection, boolean isChunked) {
		if (connection == null) {
			throw new IllegalArgumentException("connection must not be null!");
		}

		connection.setConnectTimeout(TIMEOUT_URL_CONNECTION);
		connection.setReadTimeout(READ_TIMEOUT);
		if (connection instanceof HttpURLConnection && isChunked) {
			((HttpURLConnection) connection).setChunkedStreamingMode(CHUNKED_SIZE);
		}
	}

	/**
	 * Opens an {@link InputStream} from the given {@link URL} and calls
	 * {@link #setURLConnectionDefaults(URLConnection)} on the {@link URLConnection} .
	 *
	 * @param url
	 * @return
	 * @throws IOException
	 */
	public static InputStream openStreamFromURL(URL url) throws IOException {
		if (url == null) {
			throw new IllegalArgumentException("url must not be null!");
		}

		URLConnection connection = UrlFollower.follow(url);
		setURLConnectionDefaults(connection);
		return connection.getInputStream();
	}

	/**
	 * Opens an {@link InputStream} from the given {@link URL} and sets the read and connection
	 * timeout provided by timeout.
	 */
	public static InputStream openStreamFromURL(URL url, int timeout) throws IOException {
		if (url == null) {
			throw new IllegalArgumentException("url must not be null!");
		}

		URLConnection connection = url.openConnection();

		connection.setConnectTimeout(timeout);
		connection.setReadTimeout(timeout);

		return connection.getInputStream();
	}

	/**
	 * Tries to create an instance of the defined entity class from a JSON String.
	 *
	 * @param jsonString
	 * 		the JSON as string
	 * @param entityClass
	 * 		the class to parse the json string into
	 * @param failOnUnknown
	 * 		if {@code true}, parsing will fail with a {@link JsonMappingException} if unknown properties are encountered
	 * @return the parsed object
	 * @throws JsonParseException
	 * 		see {@link ObjectMapper#readValue(String, Class)}
	 * @throws JsonMappingException
	 * 		see {@link ObjectMapper#readValue(String, Class)}
	 * @throws IOException
	 * 		see {@link ObjectMapper#readValue(String, Class)}
	 * @since 8.1
	 */
	public static <T> T parseJsonString(String jsonString, Class<T> entityClass, boolean failOnUnknown) throws JsonParseException, JsonMappingException, IOException {
		ObjectMapper mapper = new ObjectMapper();
		mapper.configure(DeserializationFeature.FAIL_ON_UNKNOWN_PROPERTIES, failOnUnknown);
		return mapper.readValue(jsonString, entityClass);
	}

	/** Clears all (Java-)cached credentials for Web services. */
	public static void clearAuthCache() {
		try {
			// this is evil, but there is no official way to clear the authentication cache...
			// use of Reflection API so no sun classes are imported
			Class<?> authCacheValueClass = Class.forName("sun.net.www.protocol.http.AuthCacheValue");
			Class<?> authCacheClass = Class.forName("sun.net.www.protocol.http.AuthCache");
			Class<?> authCacheImplClass = Class.forName("sun.net.www.protocol.http.AuthCacheImpl");
			Constructor<?> authCacheImplConstructor = authCacheImplClass.getConstructor();
			Method setAuthCacheMethod = authCacheValueClass.getMethod("setAuthCache", authCacheClass);
			setAuthCacheMethod.invoke(null, authCacheImplConstructor.newInstance());
		} catch (Throwable t) {
			LogService.getRoot().log(Level.WARNING, "Could not clear auth cache!", t);
		}
	}

}
