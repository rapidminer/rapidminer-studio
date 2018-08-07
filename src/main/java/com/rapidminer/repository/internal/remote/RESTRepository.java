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
