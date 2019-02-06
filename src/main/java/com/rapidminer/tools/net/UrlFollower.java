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
package com.rapidminer.tools.net;

import java.io.IOException;
import java.io.OutputStream;
import java.io.OutputStreamWriter;
import java.net.HttpURLConnection;
import java.net.MalformedURLException;
import java.net.ProtocolException;
import java.net.URL;
import java.net.URLConnection;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Objects;
import java.util.logging.Level;

import com.rapidminer.RapidMiner;
import com.rapidminer.tools.I18N;
import com.rapidminer.tools.LogService;
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
	 * Protected header fields
	 */
	private static final List<String> CROSS_DOMAIN_BLACKLIST = Collections.unmodifiableList(Arrays.asList("authorization", "cookie", "cookie2"));

	/**
	 * Requests methods that may contain a body
	 */
	private static final List<String> BODY_REQUEST_METHODS = Collections.unmodifiableList(Arrays.asList("POST", "PUT", "PATCH", "DELETE"));


	/**
	 * RequestBody, contains body data for HTTP Requests
	 *
	 * @since 9.1.0
	 * @author Jonas Wilms-Pfau
	 */
	private interface RequestBody {
		/**
		 * Returns the length of the bodyData
		 * @return number of bytes in the body
		 */
		long getSize();

		/**
		 * Writes the body data into the URL Connection output stream
		 *
		 * @param out the URLConnection output Stream
		 * @throws IOException if an I/O error occurs
		 */
		void writeBody(OutputStream out) throws IOException;
	}

	/**
	 * RequestBody for simple String data, limited to 2 Gibibyte
	 *
	 * @since 9.1.0
	 * @author Jonas Wilms-Pfau
	 */
	private static final class StringRequestBody implements RequestBody {
		private final String bodyData;

		private StringRequestBody(String bodyData){
			this.bodyData = String.valueOf(bodyData);
		}

		@Override
		public long getSize() {
			return bodyData.length();
		}

		@Override
		public void writeBody(OutputStream out) throws IOException {
			OutputStreamWriter writer = new OutputStreamWriter(out);
			writer.write(bodyData);
			writer.flush();
		}
	}

	/**
	 * RequestBody for simple byte data, limited to 2 Gibibyte
	 *
	 * @since 9.1.0
	 * @author Jonas Wilms-Pfau
	 */
	private static final class ByteRequestBody implements RequestBody {
		private final byte[] postData;

		private ByteRequestBody(byte[] postData){
			this.postData = postData == null ? new byte[0] : postData;
		}

		@Override
		public long getSize() {
			return postData.length;
		}

		@Override
		public void writeBody(OutputStream out) throws IOException {
			out.write(postData);
		}
	}

	/**
	 * Builder for UrlFollower
	 *
	 * @author Jonas Wilms-Pfau
	 * @since 9.1
	 */
	@SuppressWarnings("unchecked")
	public static final class Builder<T extends URLConnection> {

		private final T httpConnection;
		private Boolean isHttpToHttpsAllowed = null;
		private Boolean isHttpsToHttpAllowed = null;
		private Integer maxRedirects = null;
		private Map<String, List<String>> requestProperties = null;
		private List<String> crossDomainBlacklist = null;
		private IOException exception = null;
		private RequestBody requestBody = null;

		/**
		 * Creates a new Builder from the given connection
		 *
		 * @param connection
		 * 		the connection
		 */
		private Builder(T connection) {
			this.httpConnection = connection;
		}

		/**
		 * Creates a new Builder from the given connection
		 *
		 * @param connection
		 * 		a {@link HttpURLConnection} or {@link javax.net.ssl.HttpsURLConnection HttpsURLConnection}
		 * @return a new builder
		 */
		public static <T extends URLConnection> Builder<T> from(T connection) {
			return new Builder<>(connection);
		}

		/**
		 * Creates a new Builder from the given url
		 *
		 * @param url
		 * 		a http or https url
		 * @return a new builder
		 * @throws ClassCastException
		 * 		if the url is not of the given type
		 * @throws NullPointerException
		 * 		if url is {@code null}
		 */
		public static <T extends URLConnection> Builder<T> from(URL url) {
			T conn = null;
			IOException exception = null;
			try {
				conn = (T) url.openConnection();
				WebServiceTools.setURLConnectionDefaults(conn);
			} catch (IOException e) {
				exception = e;
			}
			return from(conn).withException(exception);
		}

		/**
		 * Creates a new Builder from the given url
		 *
		 * @param url
		 * 		a http or https url string
		 * @return a new builder
		 * @throws ClassCastException
		 * 		if the url is not of type {@link T}
		 * @throws NullPointerException
		 * 		if url is {@code null}
		 * @throws IllegalArgumentException
		 * 		if url is not a valid {@link URL}
		 */
		public static <T extends URLConnection> Builder<T> from(String url) {
			final URL urlObject;
			try {
				urlObject = new URL(url);
			} catch (MalformedURLException e) {
				throw new IllegalArgumentException(e.getMessage(), e);
			}
			return Builder.from(urlObject);
		}

		/**
		 * Allow or forbid redirects from HTTP to HTTPS
		 * <p>Default: {@link RapidMiner#RAPIDMINER_FOLLOW_HTTP_TO_HTTPS} setting</p>
		 *
		 * @param httpToHttpsAllowed
		 * 		{@code true} if redirects are allowed, {@code false} if not
		 * @return this builder
		 */
		public Builder<T> httpToHttpsAllowed(boolean httpToHttpsAllowed) {
			isHttpToHttpsAllowed = httpToHttpsAllowed;
			return this;
		}

		/**
		 * Allow or forbid redirects from HTTPS to HTTP
		 * <p>Default: {@link RapidMiner#RAPIDMINER_FOLLOW_HTTPS_TO_HTTP} setting</p>
		 *
		 * @param httpsToHttpAllowed
		 * 		{@code true} if redirects are allowed, {@code false} if not
		 * @return this builder
		 */
		public Builder<T> httpsToHttpAllowed(boolean httpsToHttpAllowed) {
			isHttpsToHttpAllowed = httpsToHttpAllowed;
			return this;
		}

		/**
		 * Sets the maximum Number of Redirects
		 * <p>Default: {@link UrlFollower#getHttpMaxRedirects()}</p>
		 *
		 * @param maxRedirects
		 * 		maximum number of redirects
		 * @return this builder
		 */
		public Builder<T> setMaxRedirects(int maxRedirects) {
			this.maxRedirects = maxRedirects;
			return this;
		}

		/**
		 * Replaces the current request properties with the given ones
		 *
		 * <p>Default: Request Properties of the connection without Authorization and Cookie header!</p>
		 *
		 * @param requestProperties
		 * 		request properties
		 * @return this builder
		 * @see URLConnection#getRequestProperties
		 * @see #suppressHeadersCrossDomain(List)
		 */
		public Builder<T> withRequestProperties(Map<String, List<String>> requestProperties) {
			if (requestProperties == null) {
				this.requestProperties = null;
				return this;
			}
			this.requestProperties = new HashMap<>();
			requestProperties.forEach((key, valueList) -> this.requestProperties.put(key, valueList != null ? new ArrayList<>(valueList) : null));
			return this;
		}

		/**
		 * Adds a request Property, same as {@link HttpURLConnection#addRequestProperty(String, String)}
		 * <p>
		 * But allows to keep "Authorization" and "Cookie" header if used together with {@link
		 * #suppressHeadersCrossDomain(List)}
		 * </p>
		 *
		 * @param key
		 * 		the http header field
		 * @param value
		 * 		the http header value
		 * @return this builder
		 * @see HttpURLConnection#addRequestProperty(String, String)
		 */
		public Builder<T> addRequestProperty(String key, String value) {
			if (requestProperties == null) {
				requestProperties = new HashMap<>();
			}
			requestProperties.putIfAbsent(key, new ArrayList<>());
			requestProperties.get(key).add(value);
			return this;
		}

		/**
		 * Adds a request Property, same as {@link HttpURLConnection#addRequestProperty(String, String)}
		 * <p>
		 * But allows to keep "Authorization" and "Cookie" header if used together with {@link
		 * #suppressHeadersCrossDomain(List)}
		 * </p>
		 *
		 * @param key
		 * 		the http header field
		 * @param value
		 * 		the http header value
		 * @return this builder
		 * @see HttpURLConnection#addRequestProperty(String, String)
		 */
		public Builder<T> setRequestProperty(String key, String value) {
			if (requestProperties != null) {
				requestProperties.remove(key);
			}
			return addRequestProperty(key, value);
		}

		/**
		 * Defines which headers should be removed after a redirect to another {@link URL#getHost host}
		 * <p>Default {@link UrlFollower#CROSS_DOMAIN_BLACKLIST}</p>
		 *
		 * @param crossDomainBlacklist
		 * 		a case insensitive list of http header
		 * @return this builder
		 */
		public Builder<T> suppressHeadersCrossDomain(List<String> crossDomainBlacklist) {
			this.crossDomainBlacklist = crossDomainBlacklist;
			return this;
		}

		/**
		 * Adds post data, only used if the connection post method is set
		 *
		 * <p>This already sets {@link HttpURLConnection#setDoOutput(boolean) setDoOutput(true)} and the "Content-Length" header</p>
		 *
		 * @param bodyData String to post
		 * @return this builder
		 */
		public Builder<T> withRequestBody(String bodyData){
			return withRequestBody(new StringRequestBody(bodyData));
		}

		/**
		 * Adds post data, only used if the connection post method is set
		 *
		 * <p>This already sets {@link HttpURLConnection#setDoOutput(boolean) setDoOutput(true)} and the "Content-Length" header</p>
		 * @param bodyData bytes to post
		 * @return this builder
		 */
		public Builder<T> withRequestBody(byte[] bodyData){
			return withRequestBody(new ByteRequestBody(bodyData));
		}

		/**
		 * Adds post data, only used if the connection post method is set
		 *
		 * @param postBody RequestBody
		 * @return this builder
		 */
		private Builder<T> withRequestBody(RequestBody postBody){
			this.requestBody = postBody;
			return this;
		}

		/**
		 * Executes the redirect with the given settings
		 *
		 * @return open connection of the last valid redirect target, or the original connection if redirects are not
		 * allowed
		 * @throws IOException
		 * 		if an I/O exception occurs.
		 * @throws ForbiddenForwardException
		 * 		in case the url tries to forward to an unacceptable protocol
		 * @throws NullPointerException
		 * 		if the connection is {@code null}
		 * @throws ClassCastException
		 * 		if the result is not of type {@link T}
		 */
		public T follow() throws IOException {
			if (exception != null) {
				throw exception;
			}
			Objects.requireNonNull(httpConnection);
			return (T) UrlFollower.follow(httpConnection, maxRedirects, isHttpToHttpsAllowed, isHttpsToHttpAllowed, requestProperties, crossDomainBlacklist, requestBody, false);
		}

		/**
		 * Defines an exception that occurred during the building process
		 *
		 * @param exception
		 * 		the encountered exception
		 */
		private Builder<T> withException(IOException exception) {
			if (this.exception == null) {
				this.exception = exception;
			} else if (exception != null) {
				this.exception.addSuppressed(exception);
			}
			return this;
		}
	}

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
	 * 		if an I/O exception occurs.
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
	 * <p>Don't use this method if authentication is needed</p>
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
		return follow(conn, maxRedirects, followHttpToHttps, followHttpsToHttp, null, CROSS_DOMAIN_BLACKLIST, null, true);
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
	 * @param requestProperties
	 *       user defined http headers
	 * @param crossDomainBlacklist
	 *       http headers to exclude on cross domain request
	 * @param useJavaRedirect
	 *       use java redirect mechanism for same protocol redirects
	 * @return open connection of the last valid redirect target
	 * @throws IOException if an I/O exception occurs.
	 * @throws ForbiddenForwardException in case the url tries to forward to an unacceptable protocol
	 */
	private static URLConnection follow(URLConnection conn, Integer maxRedirects, Boolean followHttpToHttps, Boolean followHttpsToHttp, Map<String, List<String>> requestProperties, List<String> crossDomainBlacklist, RequestBody postData, boolean useJavaRedirect) throws IOException {
		maxRedirects = maxRedirects != null ? maxRedirects : getHttpMaxRedirects();
		maxRedirects = Math.max(MIN_REDIRECTS, maxRedirects);
		followHttpToHttps = followHttpToHttps != null ? followHttpToHttps : Boolean.parseBoolean(ParameterService.getParameterValue(RapidMiner.RAPIDMINER_FOLLOW_HTTP_TO_HTTPS));
		followHttpsToHttp = followHttpsToHttp != null ? followHttpsToHttp : Boolean.parseBoolean(ParameterService.getParameterValue(RapidMiner.RAPIDMINER_FOLLOW_HTTPS_TO_HTTP));
		crossDomainBlacklist = new ArrayList<>(crossDomainBlacklist != null ? crossDomainBlacklist : CROSS_DOMAIN_BLACKLIST);
		crossDomainBlacklist.replaceAll(String::toLowerCase);

		if (!(conn instanceof HttpURLConnection)) {
			return conn;
		}
		HttpURLConnection httpConnection = (HttpURLConnection) conn;
		if(!httpConnection.getInstanceFollowRedirects()){
			return httpConnection;
		}

		HashMap<String, List<String>> mutableRequestProperties = new HashMap<>();
		if (requestProperties == null) {
			requestProperties = new HashMap<>();
		}
		try {
			applyRequestProperties(httpConnection, requestProperties);
			requestProperties.forEach((key, value) -> {
				if (value != null) {
					mutableRequestProperties.put(key, new ArrayList<>(value));
				}
			});
			requestProperties = mutableRequestProperties;
			// add request properties from the connection object
			// it might contain values that are missing from the request properties
			httpConnection.getRequestProperties().forEach((key, value) -> {
				if (value != null) {
					mutableRequestProperties.putIfAbsent(key, new ArrayList<>(value));
				}
			});
		} catch (IllegalStateException e) {
			LogService.getRoot().log(Level.WARNING, "com.rapidminer.tools.net.UrlFollower.already_connected_properties", e);
		}

		int redirectCount;
		for (redirectCount = 0; redirectCount <= maxRedirects; redirectCount++) {
			httpConnection.setInstanceFollowRedirects(useJavaRedirect);
			addRequestBody(httpConnection, requestProperties, postData);
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
			copyRequestProperties(httpConnection, newHttpConnection, requestProperties, crossDomainBlacklist);
			httpConnection = newHttpConnection;
		}
		throw new ProtocolException(I18N.getErrorMessage("url_follower.too_many_redirects", redirectCount));
	}

	/**
	 * Adds a request body if needed
	 *
	 * @param httpConnection the connection
	 * @param requestProperties http header
	 * @param requestBody the request body
	 * @throws IOException if an I/O error occurs.
	 */
	private static void addRequestBody(HttpURLConnection httpConnection, Map<String, List<String>> requestProperties, RequestBody requestBody) throws IOException {
		if (requestBody != null && BODY_REQUEST_METHODS.contains(httpConnection.getRequestMethod().toUpperCase(Locale.ENGLISH))) {
			if (!requestProperties.containsKey("Content-Length")) {
				httpConnection.setRequestProperty("Content-Length", "" + requestBody.getSize());
			}
			try {
				if (!httpConnection.getDoOutput()) {
					httpConnection.setDoOutput(true);
				}
				try (OutputStream out = httpConnection.getOutputStream()) {
					requestBody.writeBody(out);
				}
			} catch (IllegalStateException e) {
				LogService.getRoot().log(Level.WARNING, "com.rapidminer.tools.net.UrlFollower.already_connected_body", e);
			}
		}
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
	 * Applies the request properties on the connection
	 *
	 * @param connection
	 * 		the connection
	 * @param requestProperties
	 * 		the request properties
	 */
	private static void applyRequestProperties(HttpURLConnection connection, Map<String, ? extends List<String>> requestProperties) {
		requestProperties.forEach((key, valueList) -> {
			for (int i = 0; i < valueList.size(); i++) {
				if (i == 0) {
					connection.setRequestProperty(key, valueList.get(i));
				} else {
					connection.addRequestProperty(key, valueList.get(i));
				}
			}
		});
	}

	/**
	 * Copies the User-Agent, Read Timeout, Connection Timeout and if necessary the Request Method
	 *
	 * @param from
	 * 		the source of the properties
	 * @param to
	 * 		the target of the properties
	 * @param requestProperties
	 * 		the requestProperties
	 * @param crossDomainBlacklist
	 * 		blacklist to filter the requestProperties in case of a host change
	 * @throws IOException
	 * 		if an error occurred connecting to the server.
	 */
	private static void copyRequestProperties(HttpURLConnection from, HttpURLConnection to, Map<String, ? extends List<String>> requestProperties, List<String> crossDomainBlacklist) throws IOException {
		if (!from.getURL().getHost().equals(to.getURL().getHost())) {
			// remove blacklisted header fields
			requestProperties.keySet().removeIf(key -> crossDomainBlacklist.contains(key.toLowerCase(Locale.ENGLISH)));
		}

		// Keep all (except for Authorization and Cookie) headers in case of a redirect on the same site
		applyRequestProperties(to, requestProperties);

		// Keep Read Timeout and Connection Timeout
		to.setReadTimeout(from.getReadTimeout());
		to.setConnectTimeout(from.getConnectTimeout());
		// Keep request method if required
		if (shouldKeepRequestMethod(from)) {
			to.setRequestMethod(from.getRequestMethod());
		}
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
		switch (connection.getResponseCode()) {
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
