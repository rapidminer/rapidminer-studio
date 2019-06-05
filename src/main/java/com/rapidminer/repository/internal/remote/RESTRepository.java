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
package com.rapidminer.repository.internal.remote;

import java.io.IOException;
import java.net.HttpURLConnection;

import com.rapidminer.repository.ConnectionRepository;


/**
 * A marker interface for REST-only repositories. See the remote extension for more information.
 *
 * @since 9.0.0
 * @author Jan Czogalla
 */
public interface RESTRepository extends ConnectionRepository {

	/**
	 * Creates a global search connection to this server if possible. Will return {@code null} if no user or password is set
	 *
	 * @param gsPathInfo
	 * 		the REST api path for the global search
	 * @param subfolder
	 * 		the subfolder to query
	 * @return an URL connection to the queried subfolder or {@code null}
	 * @throws IOException
	 * 		if an I/O error occurs
	 */
	HttpURLConnection getGlobalSearchConnection(String gsPathInfo, String subfolder) throws IOException;

	/**
	 * Returns the prefix of this {@link RESTRepository}. This is necessary, since the repository can represent a subfolder
	 *
	 * @return the prefix of this repository
	 */
	String getPrefix();
}
