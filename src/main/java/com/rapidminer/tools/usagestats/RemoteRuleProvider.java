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
package com.rapidminer.tools.usagestats;

import java.io.IOException;
import java.io.InputStream;
import java.net.HttpURLConnection;
import java.net.ProtocolException;
import java.net.URL;
import java.util.logging.Level;

import com.rapidminer.studio.internal.RuleProvider;
import com.rapidminer.tools.I18N;
import com.rapidminer.tools.LogService;
import com.rapidminer.tools.WebServiceTools;


/**
 * Simple remote rule provider
 *
 * @author Jonas Wilms-Pfau
 * @since 7.5
 */
class RemoteRuleProvider implements RuleProvider {

	/** the url of the cta service */
	private static final String RULE_URL = I18N.getGUIMessageOrNull("gui.label.cta.json.remote");

	/** HTTP-header location field see rfc7231 7.1.2 */
	private static final String LOCATION = "Location";

	/** Maximum number of url redirects */
	private static final long MAX_REDIRECTS = 10;

	@Override
	public InputStream getRuleJson() {
		try {
			final URL url = new URL(RULE_URL);
			LogService.getRoot().log(Level.FINE, "com.rapidminer.tools.usagestats.RemoteRuleProivder.start");
			return followUrl(url);
		} catch (IOException e) {
			LogService.getRoot().log(Level.WARNING, "com.rapidminer.tools.usagestats.RemoteRuleProivder.failure", e);
			return null;
		}
	}

	/**
	 * Follows the url as long as possible
	 *
	 * @param url
	 *            The url to follow
	 * @return
	 * @throws IOException
	 */
	private InputStream followUrl(URL url) throws IOException {
		return followUrl(url, 0);
	}

	/**
	 * Follows the url as long as possible
	 *
	 * @param url
	 *            The url to follow
	 * @param redirectCount
	 *            Current redirect count, should be 0
	 * @return
	 * @throws IOException
	 */
	private InputStream followUrl(URL url, long redirectCount) throws IOException {
		HttpURLConnection connection = (HttpURLConnection) url.openConnection();
		WebServiceTools.setURLConnectionDefaults(connection);
		switch (connection.getResponseCode()) {
			case HttpURLConnection.HTTP_MOVED_PERM:
			case HttpURLConnection.HTTP_MOVED_TEMP:
			case HttpURLConnection.HTTP_SEE_OTHER:
				if (redirectCount >= MAX_REDIRECTS) {
					throw new ProtocolException("Server redirected too many times (" + redirectCount + ")");
				}
				redirectCount++;
				URL targetUrl = new URL(connection.getHeaderField(LOCATION));
				return followUrl(targetUrl, redirectCount);
			case HttpURLConnection.HTTP_OK:
				return connection.getInputStream();
			default:
				return null;
		}
	}

}
