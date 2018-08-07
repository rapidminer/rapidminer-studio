/**
 * Copyright (C) 2001-2018 by RapidMiner and the contributors
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
package com.rapidminer.tools.net;

import java.io.IOException;
import java.net.HttpURLConnection;
import java.net.ProtocolException;
import java.net.URL;
import java.net.URLConnection;

import com.rapidminer.RapidMiner;
import com.rapidminer.tools.I18N;
import com.rapidminer.tools.ParameterService;
import com.rapidminer.tools.WebServiceTools;


/**
 * Follows URLs from HTTPS to HTTP and otherwise if allowed
 * <br>
 * By default cross protocol redirects are followed, but no information (besides IP address, user-agent, etc.) is transferred to the target URL.
 * Use the configuration parameters {@value RapidMiner#RAPIDMINER_FOLLOW_HTTPS_TO_HTTP} and {@value RapidMiner#RAPIDMINER_FOLLOW_HTTP_TO_HTTPS} to block these redirects if needed.
 *
 * @author Jonas Wilms-Pfau
 * @since 8.2.1
 */
public final class UrlFollower {

	/**
	 * HTTP Status-Code 307: Temporary Redirect.
	 **/
	private static final int TEMPORARY_REDIRECT = 307;

	/**
	 * HTTP Status-Code 308: Permanent Redirect.
	 **/
	private static final int PERMANENT_REDIRECT = 308;

	/**
	 * Location header field
	 *
	 * @see <a href="https://tools.ietf.org/html/rfc2616#section-14.30">RFC2616</a>
	 */
	private static final String LOCATION = "Location";

	/**
	 * HTTP Over TLS
	 *
	 * @see <a href="https://tools.ietf.org/html/rfc2818">RFC 2818</a>
	 */
	private static final String PROTOCOL_HTTPS = "https";

	/**
	 * Java system property for maximum number of redirects property
	 *
	 * @see <a href="https://docs.oracle.com/javase/8/docs/api/java/net/doc-files/net-properties.html#MiscHTTP">Networking Properties</a>
	 */
	private static final String MAX_REDIRECTS_PROPERTY = "http.maxRedirects";

	/**
	 * Default {@value MAX_REDIRECTS_PROPERTY} value
	 */
	private static final int MAX_REDIRECTS_DEFAULT = 20;

	/**
	 * Minimum {@value MAX_REDIRECTS_PROPERTY} value
	 */
	private static final int MIN_REDIRECTS = 0;

	/**
	 * User Agent HTTP header field
	 */
	private static final String USER_AGENT = "User-Agent";

	/**
	 * Prevent utility class instantiation.
	 */
	private UrlFollower() {
		throw new AssertionError("utility class");
	}

	/**
	 * Follows the url up to {@value MAX_REDIRECTS_PROPERTY} times
	 *
	 * @param url
	 * 		the url to follow
	 * @return open connection of the last valid redirect target
	 * @throws IOException
	 */
	public static URLConnection follow(URL url) throws IOException {
		return follow(url, getHttpMaxRedirects());
	}

	/**
	 * Follows the url up to maxRedirects times
	 *
	 * @param url
	 * 		the url to follow
	 * @param maxRedirects
	 * 		maximum number of redirects
	 * @return open connection of the last valid redirect target
	 * @throws IOException
	 * 		if an I/O exception occurs.
	 * @throws ForbiddenForwardException
	 * 		in case the url tries to forward to an unacceptable protocol
	 */
	public static URLConnection follow(URL url, int maxRedirects) throws IOException {
		boolean isHttpToHttpsAllowed = Boolean.parseBoolean(ParameterService.getParameterValue(RapidMiner.RAPIDMINER_FOLLOW_HTTP_TO_HTTPS));
		boolean isHttpsToHttpAllowed = Boolean.parseBoolean(ParameterService.getParameterValue(RapidMiner.RAPIDMINER_FOLLOW_HTTPS_TO_HTTP));
		return follow(url, maxRedirects, isHttpToHttpsAllowed, isHttpsToHttpAllowed);
	}

	/**
	 * Follows the url up to maxRedirects times
	 *
	 * @param url
	 * 		the url to follow
	 * @param maxRedirects
	 * 		maximum number of redirects
	 * @param followHttpToHttps
	 * 		if HTTP should follow to HTTPS
	 * @param followHttpsToHttp
	 * 		if HTTPS should follow to HTTP
	 * @return open URLConnection of the last valid redirect
	 * @throws IOException if an I/O exception occurs.
	 * @throws ForbiddenForwardException in case the url tries to forward to an unacceptable protocol
	 */
	public static URLConnection follow(URL url, int maxRedirects, boolean followHttpToHttps, boolean followHttpsToHttp) throws IOException {
		URLConnection conn = url.openConnection();
		WebServiceTools.setURLConnectionDefaults(conn);
		return follow(conn, maxRedirects, followHttpToHttps, followHttpsToHttp);
	}

	/**
	 * Follows the connection if necessary
	 *
	 * @param conn connection to follow
	 * @return open URLConnection of the last valid redirect
	 * @throws IOException if an I/O exception occurs.
	 * @throws ForbiddenForwardException in case the url tries to forward to an unacceptable protocol
	 * @since 9.0.0
	 */
	public static URLConnection follow(URLConnection conn) throws IOException {
		boolean isHttpToHttpsAllowed = Boolean.parseBoolean(ParameterService.getParameterValue(RapidMiner.RAPIDMINER_FOLLOW_HTTP_TO_HTTPS));
		boolean isHttpsToHttpAllowed = Boolean.parseBoolean(ParameterService.getParameterValue(RapidMiner.RAPIDMINER_FOLLOW_HTTPS_TO_HTTP));
		return follow(conn, getHttpMaxRedirects(), isHttpToHttpsAllowed, isHttpsToHttpAllowed);
	}

	/**
	 * Follows the connection and keeps the User-Agent and Protocol (only for {@link #TEMPORARY_REDIRECT}  and {@link #PERMANENT_REDIRECT}).
	 *
	 * @param conn
	 *      connection to follow
	 * @param maxRedirects
	 * 		maximum number of redirects
	 * @param followHttpToHttps
	 * 		if HTTP should follow to HTTPS
	 * @param followHttpsToHttp
	 * 		if HTTPS should follow to HTTP
	 * @return open connection of the last valid redirect target
	 * @throws IOException if an I/O exception occurs.
	 * @throws ForbiddenForwardException in case the url tries to forward to an unacceptable protocol
	 * @since 9.0.0
	 */
	public static URLConnection follow(URLConnection conn, int maxRedirects, boolean followHttpToHttps, boolean followHttpsToHttp) throws IOException {
		maxRedirects = Math.max(MIN_REDIRECTS, maxRedirects);
		if (!(conn instanceof HttpURLConnection)) {
			return conn;
		}
		HttpURLConnection httpConnection = (HttpURLConnection) conn;

		int redirectCount;
		for (redirectCount = 0; redirectCount <= maxRedirects; redirectCount++) {
			if (!isRedirecting(httpConnection)) {
				return httpConnection;
			}
			URL newUrl = new URL(httpConnection.getHeaderField(LOCATION));
			// Check if http <-> https following is allowed
			verifyRedirect(httpConnection.getURL(), newUrl, followHttpToHttps, followHttpsToHttp);

			URLConnection newConnection = newUrl.openConnection();
			if (!(newConnection instanceof HttpURLConnection)) {
				return newConnection;
			}
			HttpURLConnection newHttpConnection = (HttpURLConnection) newConnection;
			// Keep user-agent, timeouts and request method (if needed)
			copyRequestProperties(httpConnection, newHttpConnection);
			httpConnection = newHttpConnection;
		}
		throw new ProtocolException(I18N.getErrorMessage("url_follower.too_many_redirects", redirectCount));
	}

	/**
	 * Verifies if the given redirect is allowed
	 *
	 * @param url the source url
	 * @param newUrl the target url
	 * @param followHttpToHttps is redirect from HTTP to HTTPS allowed
	 * @param followHttpsToHttp is redirect from HTTPS to HTTP allowed
	 * @throws ForbiddenForwardException in case the redirect is not allowed
	 */
	private static void verifyRedirect(URL url, URL newUrl, boolean followHttpToHttps, boolean followHttpsToHttp) throws ForbiddenForwardException {
		boolean fromHttps = url.getProtocol().equalsIgnoreCase(PROTOCOL_HTTPS);
		boolean toHttps = newUrl.getProtocol().equalsIgnoreCase(PROTOCOL_HTTPS);
		if ((!fromHttps && toHttps && !followHttpToHttps)) {
			throw new ForbiddenForwardException(I18N.getErrorMessage("url_follower.http_to_https"));
		}
		if (fromHttps && !toHttps && !followHttpsToHttp) {
			throw new ForbiddenForwardException(I18N.getErrorMessage("url_follower.https_to_http"));
		}
	}

	/**
	 * Copies the User-Agent, Read Timeout, Connection Timeout and if necessary the Request Method
	 *
	 * @param from
	 * 		the source of the properties
	 * @param to
	 * 		the target of the properties
	 * @throws IOException
	 * 		if an error occurred connecting to the server.
	 */
	private static void copyRequestProperties(HttpURLConnection from, HttpURLConnection to) throws IOException {
		// Keep the user agent
		String userAgent = from.getRequestProperty(USER_AGENT);
		if (userAgent != null) {
			to.setRequestProperty(USER_AGENT, userAgent);
		}
		// Keep Read Timeout and Connection Timeout
		to.setReadTimeout(from.getReadTimeout());
		to.setConnectTimeout(from.getConnectTimeout());
		// Keep request method if required
		if (shouldKeepRequestMethod(from)) {
			to.setRequestMethod(from.getRequestMethod());
		}
		// we do not send further information like Cookie or Authorization on redirects
	}

	/**
	 * Checks if the connection response code is {@link #TEMPORARY_REDIRECT} or {@link #PERMANENT_REDIRECT}
	 *
	 * <p>
	 *     {@link HttpURLConnection#HTTP_MOVED_PERM} and {@link HttpURLConnection#HTTP_MOVED_TEMP}
	 *     are also supposed to keep there request method, but this behavior is not widely implemented.
	 * </p>
	 *
	 * @param connection the connection to check
	 * @return true if the current protocol should be kept
	 *
	 * @throws IOException if an error occurred connecting to the server.
	 */
	private static boolean shouldKeepRequestMethod(HttpURLConnection connection) throws IOException {
		return connection.getResponseCode() == TEMPORARY_REDIRECT || connection.getResponseCode() == PERMANENT_REDIRECT;
	}

	/**
	 * Checks if the connection is redirecting
	 *
	 * @param connection the connection to check
	 * @return true if the response code is a redirect code
	 *
	 * @throws IOException if an error occurred connecting to the server.
	 */
	private static boolean isRedirecting(HttpURLConnection connection) throws IOException {
		return connection.getInstanceFollowRedirects() && isRedirectCode(connection.getResponseCode());
	}

	/**
	 * Checks if the response code is a redirect code
	 *
	 * @param responseCode
	 * 		the http response code
	 * @return
	 */
	private static boolean isRedirectCode(int responseCode) {
		switch (responseCode) {
			case HttpURLConnection.HTTP_MOVED_PERM:
			case HttpURLConnection.HTTP_MOVED_TEMP:
			case HttpURLConnection.HTTP_SEE_OTHER:
			case TEMPORARY_REDIRECT:
			case PERMANENT_REDIRECT:
				return true;
			default:
				return false;
		}
	}

	/**
	 * Tries to read the {@value MAX_REDIRECTS_PROPERTY} defaults to {@value MAX_REDIRECTS_DEFAULT}
	 *
	 * @return the value of {@value MAX_REDIRECTS_PROPERTY}, or {@value MAX_REDIRECTS_DEFAULT} if not set
	 */
	public static int getHttpMaxRedirects() {
		int redirects;
		try {
			redirects = Integer.parseInt(System.getProperty(MAX_REDIRECTS_PROPERTY, "" + MAX_REDIRECTS_DEFAULT));
		} catch (Exception e) {
			redirects = MAX_REDIRECTS_DEFAULT;
		}
		return Math.max(MIN_REDIRECTS, redirects);
	}

}
