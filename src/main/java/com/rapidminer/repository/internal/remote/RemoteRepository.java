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
import java.net.URL;
import java.util.List;

import com.rapidminer.gui.tools.VersionNumber;
import com.rapidminer.repository.ConnectionRepository;
import com.rapidminer.repository.RepositoryException;
import com.rapidminer.tools.PasswordInputCanceledException;
import com.rapidminer.tools.usagestats.ActionStatisticsCollector;


/**
 * Interface for a RapidMiner server backed repository. It allows to manipulate the repository content via the
 * {@link RemoteContentManager}, to schedule remote processes via the {@link RemoteScheduler} and to retrieve further
 * server information via the {@link RemoteInfoService}.
 *
 * @author Nils Woehler
 * @since 6.5.0
 *
 */
public interface RemoteRepository extends RemoteFolder, ConnectionRepository {

	String TAG_REMOTE_REPOSITORY = "remoteRepository";

	/** Type of object requested from a server. */
	enum EntryStreamType {
		METADATA, IOOBJECT, PROCESS, BLOB, CONNECTION_INFORMATION, CONNECTION_METADATA
	}

	/** Authentication types */
	enum AuthenticationType {

		BASIC(ActionStatisticsCollector.TYPE_REMOTE_REPOSITORY), // user+password
		SAML(ActionStatisticsCollector.TYPE_REMOTE_REPOSITORY_SAML); // enterprise SSO

		private final String actionStatisticsType; // for usage stat collection

		private AuthenticationType(String actionStatisticsType) {
			this.actionStatisticsType = actionStatisticsType;
		}

		public String getActionStatisticsType() {
			return actionStatisticsType;
		}
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
	 * @deprecated @since 9.5.0 use getClient().getContentManager() instead
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
	 * @deprecated @since 9.5.0 implement necessary access in the {@link BaseServerClient} impl VersionedServerClient
	 */
	@Deprecated
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
	 * @deprecated @since 9.5.0 implement necessary access in the {@link BaseServerClient} impl VersionedServerClient
	 */
	@Deprecated
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
	 * 		the {@link ConnectionListener} to register
	 * @deprecated Only here for legacy compatibility reasons. Use {@link ConnectionRepository#addConnectionListener(com.rapidminer.repository.ConnectionListener)} instead.
	 */
	@Deprecated
	void addConnectionListener(ConnectionListener listener);

	/**
	 * Removes a registered {@link ConnectionListener}.
	 *
	 * @param listener
	 * 		the {@link ConnectionListener} to remove
	 * @deprecated Only here for legacy compatibility reasons. Use {@link ConnectionRepository#removeConnectionListener(com.rapidminer.repository.ConnectionListener)} instead.
	 */
	@Deprecated
	void removeConnectionListener(ConnectionListener listener);

	/**
	 * Checks if the specified file name is blacklisted or not. Will return {@code false} by default.
	 * @param originalFilename the name of the file to be pushed
	 * @return {@code true} if the file is blacklisted, {@code false} otherwise
	 * @since 8.1
	 * @throws IOException if a connection error occurs
	 * @throws RepositoryException if a repository error occurs
	 */
	default boolean isFileExtensionBlacklisted(String originalFilename) throws IOException, RepositoryException {
		return false;
	}

	/**
	 * Return authentication type
	 *
	 * @return
	 */
	AuthenticationType getAuthenticationType();

	/**
	 * Sets the authentication type to {@link AuthenticationType#BASIC} or
	 * {@link AuthenticationType#SAML}
	 *
	 * @param authenticationType
	 *
	 */
	void setAuthenticationType(AuthenticationType authenticationType);


	/**
	 * Load Vault information for a {@link com.rapidminer.connection.ConnectionInformation} in the repositoryLocation
	 *
	 * @param repositoryLocation
	 * 		location of the {@link com.rapidminer.connection.ConnectionInformation}
	 * @return the information available in the vault for injection
	 */
	RemoteVaultEntry[] loadVaultInfo(String repositoryLocation) throws RepositoryException;

	/**
	 * Create a new entry in the vault to add some information to a {@link com.rapidminer.connection.ConnectionInformation}
	 * object that is already stored in the repository
	 *
	 * @param path
	 * 		location of the connection information in the repository
	 * @param entries
	 * 		to be set created data entries containing group and name for injection reference and the value to be injected
	 */
	void createVaultEntry(String path, List<RemoteCreateVaultInformation> entries) throws IOException, RepositoryException;

	/**
	 * Cleans up this repository, used in between process executions Does nothing by default.
	 *
	 * @since 9.5
	 */
	default void cleanup(){}

	/*
	 * Get a client that is compatible with the Server
	 *
	 * @return an implementation based on {@link BaseServerClient}, methods may return
	 * {@link com.rapidminer.repository.internal.remote.exception.NotYetSupportedServiceException}
	 * or {@link com.rapidminer.repository.internal.remote.exception.DeprecatedServiceException}
	 * to be able to use this information to display it to the user or react on it.
	 */
	BaseServerClient getClient();

	/**
	 * Get the version from Server.
	 *
	 * @return the servers {@link VersionNumber} or null
	 */
	VersionNumber getServerVersion();

	/**
	 * Get the version from Server if it is already known, should not connect to get the version number.
	 *
	 * @return the servers {@link VersionNumber} or null
	 */
	VersionNumber getKnownServerVersion();
}
