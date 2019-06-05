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
import java.net.HttpURLConnection;
import java.net.MalformedURLException;
import java.net.ProtocolException;
import java.net.URL;
import java.net.URLConnection;

import org.junit.Assert;
import org.junit.Test;


/***
 * Tests for the UrlFollower class
 *
 * @author Jonas Wilms-Pfau
 * @since 9.0.0
 */
public class UrlFollowerTest {

	private static final URL SECURE_UNSECURE_SECURE_REDIRECT = createUrl("https://redirects.rapidminer.com/test-process/httpstohttp");
	private static final URL UNSECURE_SECURE_REDIRECT = createUrl("http://redirects.rapidminer.com/test-process/httptohttps");
	private static final URL SECURE_TARGET = createUrl("https://s3.amazonaws.com/rapidminer.dev-test/names.csv");

	@Test
	public void followTwoRedirectsSuccess() throws IOException {
		Assert.assertEquals(HttpURLConnection.HTTP_OK, ((HttpURLConnection) UrlFollower.follow(SECURE_UNSECURE_SECURE_REDIRECT, 2, true, true)).getResponseCode());
	}

	@Test
	public void followNoRedirectSuccess() throws IOException {
		Assert.assertEquals(HttpURLConnection.HTTP_OK, ((HttpURLConnection) UrlFollower.follow(SECURE_TARGET, 0, false, false)).getResponseCode());
	}

	@Test
	public void disableRedirects() throws IOException {
		HttpURLConnection connection = (HttpURLConnection) UNSECURE_SECURE_REDIRECT.openConnection();
		connection.setInstanceFollowRedirects(false);
		Assert.assertEquals(HttpURLConnection.HTTP_MOVED_TEMP, ((HttpURLConnection) UrlFollower.follow(connection)).getResponseCode());
	}

	@Test
	public void followHttpToHttps() throws IOException {
		URLConnection conn = UNSECURE_SECURE_REDIRECT.openConnection();
		conn.setRequestProperty("User-Agent", "RapidMiner Studio");
		UrlFollower.follow(conn, 1, true, false);
	}

	@Test(expected = ForbiddenForwardException.class)
	public void followHttpToHttpsFailure() throws IOException {
		UrlFollower.follow(SECURE_UNSECURE_SECURE_REDIRECT, 2, true, false);
	}

	@Test(expected = ProtocolException.class)
	public void tooManyRedirects() throws IOException {
		UrlFollower.follow(SECURE_UNSECURE_SECURE_REDIRECT, 1, true, true);
	}

	/**
	 * Helper method for url creation
	 *
	 * @param url the url string
	 * @return the URL or null
	 */
	private static URL createUrl(String url) {
		try {
			return new URL(url);
		} catch (MalformedURLException mau) {
			return null;
		}
	}

}
