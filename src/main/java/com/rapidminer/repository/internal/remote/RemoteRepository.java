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
package com.rapidminer.repository.internal.remote;

import java.io.IOException;
import java.net.HttpURLConnection;
import java.net.URL;
import java.util.List;

import com.rapidminer.repository.Repository;
import com.rapidminer.repository.RepositoryException;
import com.rapidminer.tools.PasswordInputCanceledException;


/**
 * Interface for a RapidMiner server backed repository. It allows to manupilate the repository
 * content via the {@link RemoteContentManager}, to schedule remote processes via the
 * {@link RemoteScheduler} and to retrieve further server information via the
 * {@link RemoteInfoService}.
 *
 * @author Nils Woehler
 * @since 6.5.0
 *
 */
public interface RemoteRepository extends Repository, RemoteFolder {

	public static final String TAG_REMOTE_REPOSITORY = "remoteRepository";

	/** Type of object requested from a server. */
	public static enum EntryStreamType {
		METADATA, IOOBJECT, PROCESS, BLOB
	}

	/**
	 * @return the repository base URL
	 */
	URL getBaseUrl();

	/**
	 * @return the repository alias
	 */
	String getAlias();

	/**
	 * @return the username for this {@link RemoteRepository}
	 */
	String getUsername();

	/**
	 * Sets the username for this {@link RemoteRepository}.
	 *
	 * @param username
	 *            the username
	 */
	void setUsername(String username);

	/**
	 * Sets the password for this {@link RemoteRepository}.
	 *
	 * @param password
	 *            the password
	 */
	void setPassword(char[] password);

	/**
	 * @param pwCanceled
	 *            sets the state of password input canceled
	 */
	void setPasswortInputCanceled(boolean pwCanceled);

	/**
	 * @return whether the password input dialog has been canceled by the user
	 */
	boolean isPasswordInputCanceled();

	/**
	 * @return whether the {@link RemoteRepository} is connected (online) or disconneted (offline).
	 */
	boolean isConnected();

	/**
	 * Checks if the server is reachable at the moment with the current login; repeatedly shows the
	 * login dialog if the login is wrong. Checks if a sufficient server version was found.
	 *
	 * @return {@code true} if the server is reachable at the moment with the current password and a
	 *         sufficient server version was found, {@code false} if the login dialog was canceled
	 * @throws RepositoryException
	 *             if the connection failed
	 */
	boolean isReachable() throws RepositoryException;

	/**
	 * Returns the cached {@link RemoteInfoService} if present, otherwise fetches it from the RM
	 * server. If fetching from the server fails, checks the password and tries again.
	 *
	 * @return the {@link RemoteInfoService} if it can be accessed. If the queried Server cannot be
	 *         reached or has no {@link RemoteInfoService} or the login dialog was canceled
	 *         <code>null</code> is returned.
	 */
	RemoteInfoService getInfoService();

	/**
	 * Returns the {@link RemoteScheduler} which allows to run processes on the
	 * {@link RemoteRepository}.
	 *
	 * @return the remote scheduler
	 * @throws RepositoryException
	 *             if the connection failed
	 * @throws PasswordInputCanceledException
	 *             if the login dialog was canceled
	 */
	RemoteScheduler getScheduler() throws RepositoryException, PasswordInputCanceledException;

	/**
	 * Returns the repository content manager. If getting it fails checks the password and tries
	 * again. Throws a {@link PasswordInputCanceledException} if the login dialog was canceled.
	 *
	 * @return the remote content manager
	 * @throws RepositoryException
	 *             if the connection failed
	 * @throws PasswordInputCanceledException
	 *             if the login dialog was canceled
	 */
	RemoteContentManager getContentManager() throws RepositoryException, PasswordInputCanceledException;

	/**
	 * Deletes the cached {@link RemoteContentManager} and creates a new one.
	 *
	 * @throws RepositoryException
	 *             in case the content manager could not be created
	 */
	void resetContentManager() throws RepositoryException, PasswordInputCanceledException;

	/**
	 * Use this function only if there are no query parameters. Use
	 * {@link #getHTTPConnection(String, String, boolean)} otherwise.
	 *
	 * @param pathInfo
	 *            should look like 'RAWS/...' without a '/' in front. Furthermore the pathInfo
	 *            should NOT be encoded. This will be done by this function.
	 * @param preAuthHeader
	 *            if {@code true} the username and password is added to the connection if present,
	 *            if {@code false} it is checked if the RM server is reachable
	 * @return the connection
	 * @throws IOException
	 * @throws RepositoryException
	 */
	HttpURLConnection getHTTPConnection(String pathInfo, boolean preAuthHeader) throws IOException, RepositoryException;

	/**
	 * Creates a connection to the Server using the arguments.
	 *
	 * @param pathInfo
	 *            should look like 'RAWS/...' without a '/' in front. Furthermore the pathInfo
	 *            should NOT be encoded. This will be done by this function.
	 * @param query
	 *            should look like this '?format=PARAM1'. The query parameters should be encoded
	 *            with URLEncoder before passing them to this function <br/>
	 *            (e.g. String query = "?format="+URLEncoder.encode("binmeta", "UTF-8");).
	 * @param preAuthHeader
	 *            if {@code true} the username and password is added to the connection if present,
	 *            if {@code false} it is checked if the RM server is reachable
	 *
	 * @return the connection
	 * @throws IOException
	 * @throws RepositoryException
	 *             if preAuthHeader is {@code false} and checking if the server is reachable failed
	 *             or if the login dialog was canceled during this check
	 */
	HttpURLConnection getHTTPConnection(String pathInfo, String query, boolean preAuthHeader)
	        throws IOException, RepositoryException;

	/**
	 * @return the allowed connection typeIds of this remote repository
	 */
	List<String> getTypeIds();

	/** Defines which connection type ids are installed at this remote repository */
	void setTypeIds(List<String> typeIds) throws RepositoryException;

	/**
	 * Registers a {@link ConnectionListener}.
	 *
	 * @param listener
	 *            the {@link ConnectionListener} to register
	 */
	void addConnectionListener(ConnectionListener listener);

	/**
	 * Removes a registered {@link ConnectionListener}.
	 *
	 * @param listener
	 *            the {@link ConnectionListener} to remove
	 */
	void removeConnectionListener(ConnectionListener listener);

}
