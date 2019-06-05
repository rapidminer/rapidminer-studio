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

import java.util.List;


/**
 * This filter can be added to the {@link RepositoryManager} to show only a subset of all the {@link Repository Repositories}
 *
 * @author Andreas Timm
 * @since 9.3
 */
public interface RepositoryFilter {

	/**
	 * Will be called when the {@link RepositoryManager}'s save method is invoked
	 */
	void save();

	/**
	 * The actual filtering happens here.
	 * This method will be called every time the {@link com.rapidminer.repository.gui.RepositoryTree} is shown
	 *
	 * @param repositories
	 * 		list of repositories, may be smaller than the original set already due to other applied filters
	 * @return the filtered list of repositories, never {@code null}
	 */
	List<Repository> filter(List<Repository> repositories);

	/**
	 * Reset this filter to show all the available entries. This can be called from RapidMiner in case a filtered-out
	 * {@link Repository} hides a requested element. I.e. "show location of current process" needs to show the containing
	 * {@link Repository} in the {@link com.rapidminer.repository.gui.RepositoryBrowser}. This reset needs to make sure
	 * that this {@link RepositoryFilter} does not filter out anything, just like it does not exist.
	 */
	void reset();

	/**
	 * When registering a {@link RepositoryFilter} on the {@link com.rapidminer.repository.gui.RepositoryBrowser} it will
	 * immediately add a callback through this method so that a change in the filter configuration for instance can use
	 * it to trigger an update of the {@link com.rapidminer.repository.gui.RepositoryBrowser} UI.
	 * This should be triggered every time the filter is changed else the changes will not be reflected in the UI.
	 *
	 * @param notifyForUpdate
	 * 		run this callback to trigger an update of the repository view
	 */
	void notificationCallback(Runnable notifyForUpdate);
}
