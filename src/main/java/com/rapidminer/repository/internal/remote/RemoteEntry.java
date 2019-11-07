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

import java.util.List;

import com.rapidminer.repository.Entry;
import com.rapidminer.repository.RepositoryException;
import com.rapidminer.repository.internal.remote.model.AccessRights;
import com.rapidminer.tools.PasswordInputCanceledException;


/**
 * Representation for remote {@link Entry}s. It allows to query the parent {@link RemoteRepository},
 * to retrieve and set {@link AccessRights} and to retrieve its path.
 *
 * @author Nils Woehler
 * @since 6.5.0
 *
 */
public interface RemoteEntry extends Entry {

	/**
	 * @return the {@link RemoteRepository} the {@link RemoteEntry} is part of
	 */
	RemoteRepository getRepository();

	/**
	 * @return an unmodifiable list of {@link AccessRights}
	 * @throws RepositoryException
	 *             in case fetching of {@link AccessRights} fails
	 * @throws PasswordInputCanceledException
	 *             in case the user cancels the password dialog
	 */
	List<AccessRights> getAccessRights() throws RepositoryException, PasswordInputCanceledException;

	/**
	 * Updates the access rights for this entry.
	 *
	 * @param accessRights
	 *            the new {@link AccessRights}
	 * @throws RepositoryException
	 *             in case updating fails (e.g. no permission, server offline, etc.)
	 * @throws PasswordInputCanceledException
	 *             in case the user cancels the password dialog
	 */
	void setAccessRights(List<AccessRights> accessRights) throws RepositoryException, PasswordInputCanceledException;

	/**
	 * @return the entries location as {@link String} within the {@link RemoteRepository}.
	 */
	String getPath();

}
