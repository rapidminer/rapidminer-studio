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

import java.net.URL;

import com.rapidminer.repository.CustomRepositoryFactory;
import com.rapidminer.repository.RepositoryException;
import com.rapidminer.repository.RepositoryManager;
import com.rapidminer.repository.internal.remote.RemoteRepository.AuthenticationType;


/**
 * The {@link RemoteRepositoryFactory} is a {@link CustomRepositoryFactory} which is extended by
 * some methods to check the connection to a {@link RemoteRepository} and to create a
 * {@link RemoteRepository} instance.
 *
 * @author Nils Woehler
 * @since 6.5.0
 *
 */
public interface RemoteRepositoryFactory extends CustomRepositoryFactory {

	/**
	 * Checks if the provided configuration works. If it is working, <code>null</code> will be
	 * returned. If it is not working, an error message will be returned.
	 *
	 * @param name
	 *            the repository name
	 * @param repositoryURL
	 *            the URL of the Server repository
	 * @param userName
	 *            the username
	 * @param password
	 *            the password
	 * @param authenticationType
	 *            {@link AuthenticationType#BASIC} or {@link AuthenticationType#SAML}
	 * @return If the provided configuration is working, <code>null</code> will be returned. If it
	 *         is not working, an error message will be returned.
	 */
	String checkConfiguration(String name, String repositoryURL, String userName, char[] password, AuthenticationType authenticationType);

	/**
	 * Creates a new {@link RemoteRepository} instance for the provided parameters
	 *
	 * @param baseUrl
	 *            the Server base URL
	 * @param alias
	 *            the repository alias
	 * @param username
	 *            the username
	 * @param password
	 *            the password
	 * @param authenticationType
	 *            {@link AuthenticationType#BASIC} or {@link AuthenticationType#SAML}
	 * @param shouldSave
	 *            defines whether the {@link RepositoryManager} should save the
	 *            {@link RemoteRepository} when storing {@link Repository} configurations
	 * @return the created {@link RemoteRepository} instance
	 * @throws RepositoryException
	 *             if connection to the {@link RemoteRepository} isn't possible with the provided
	 *             parameters
	 */
	RemoteRepository create(URL baseUrl, String alias, String username, char[] password, AuthenticationType authenticationType, boolean shouldSave)
	        throws RepositoryException;

	/**
	 * Checks if the provided configuration works. If it is working, <code>null</code> will be
	 * returned. If it is not working, an error message will be returned.
	 *
	 * @param name
	 *            the repository name
	 * @param repositoryURL
	 *            the URL of the Server repository
	 * @param userName
	 *            the username
	 * @param password
	 *            the password
	 * @return If the provided configuration is working, <code>null</code> will be returned. If it
	 *         is not working, an error message will be returned.
	 *
	 * @deprecated removing in favor of
	 *             {@link RemoteRepositoryFactory#checkConfiguration(String, String, String, char[], RemoteRepository.AuthenticationType)}
	 */
	 @Deprecated
	String checkConfiguration(String name, String repositoryURL, String userName, char[] password);

	/**
	 * Creates a new {@link RemoteRepository} instance for the provided parameters
	 *
	 * @param baseUrl
	 *            the Server base URL
	 * @param alias
	 *            the repository alias
	 * @param username
	 *            the username
	 * @param password
	 *            the password
	 * @param shouldSave
	 *            defines whether the {@link RepositoryManager} should save the
	 *            {@link RemoteRepository} when storing {@link Repository} configurations
	 * @return the created {@link RemoteRepository} instance
	 * @throws RepositoryException
	 *             if connection to the {@link RemoteRepository} isn't possible with the provided
	 *             parameters
	 *
	 * @deprecated removing in favor of
	 *             {@link RemoteRepositoryFactory#create(URL, String, String, char[], RemoteRepository.AuthenticationType, boolean)}
	 */
	 @Deprecated
	RemoteRepository create(URL baseUrl, String alias, String username, char[] password, boolean shouldSave)
		throws RepositoryException;

}
