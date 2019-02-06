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
package com.rapidminer.repository;


import java.util.EventListener;

import com.rapidminer.tools.Observer;


/**
 * Listener for the {@link RepositoryManager} which fires when a {@link Repository} was added to it / removed from it.
 * This replaces the {@link RepositoryManager#addObserver(Observer, boolean)} method from version 8.1 going forward.
 *
 * @author Marco Boeck
 * @since 8.1
 */
public interface RepositoryManagerListener extends EventListener {

	/**
	 * Fired when the repository was added to the repository manager.
	 *
	 * @param repository
	 *         the repository that was added
	 */
	void repositoryWasAdded(Repository repository);

	/**
	 * Fired when the repository for this listener was removed from the repository manager.
	 *
	 * @param repository
	 *         the repository that was removed
	 */
	void repositoryWasRemoved(Repository repository);

}