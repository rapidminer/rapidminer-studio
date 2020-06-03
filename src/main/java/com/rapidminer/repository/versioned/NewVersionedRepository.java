/**
 * Copyright (C) 2001-2020 by RapidMiner and the contributors
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
package com.rapidminer.repository.versioned;


/**
 * Interface signalling new repositories based on the new VersionedRepository project. This is the versioned
 * variant. Also has a method to access the base URL to help identify the RapidMiner AI Hub it belongs to.
 *
 * @author Marco Boeck
 * @since 9.7
 */
public interface NewVersionedRepository {

	/**
	 * Gets the base URL of the RapidMiner AI Hub this versioned repository is tied to.
	 *
	 * @return the base url, e.g. 'https://192.168.0.1:8080', never {@code null}
	 */
	String getBaseUrl();

	/**
	 * The name of the repo, same as {@link com.rapidminer.repository.Repository#getName()}.
	 *
	 * @return the name, never {@code null}
	 */
	String getName();

	/**
	 * Gets status information for this versioned repository. This information is static and does not query the Git
	 * server itself, it is updated when a query is triggered by the user, though.
	 *
	 * @return the status object, never {@code null}
	 */
	VersionedRepositoryStatus getStatus();

	/**
	 * Gets the encryption context key that is used by the repository for encrypting files in it. See {@link
	 * com.rapidminer.tools.encryption.EncryptionProvider}.
	 *
	 * @return the encryption context key or {@code null}, in which case no encryption will be used (and e.g. passwords
	 * would be stored as-is, i.e. unencrypted)
	 */
	String getEncryptionContext();

	/**
	 * Gets whether this repo is read-only or not. This is the case when the connected user has only read permissions.
	 *
	 * @return {@code true} if it is read-only; {@code false} otherwise
	 */
	boolean isReadOnly();
}
