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
 * This interface defines loading and saving methods which are used by the {@link RepositoryManager}
 * to retrieve and store the {@link Repository}-configuration. Custom implementations should replace
 * the default provider by calling {@link RepositoryManager#setProvider(RepositoryProvider)}.
 *
 * @author Marcel Michel
 *
 */
public interface RepositoryProvider {

	/**
	 * The loading mechanism will be triggered during the {@link RepositoryManager#init()} method.
	 * The implementation should read the configuration and return parsed {@link Repository}s as
	 * {@link List}.
	 *
	 * @return the loaded {@link Repository} entries as {@link List}
	 */
	public List<Repository> load();

	/**
	 * The saving mechanism will be triggered after every
	 * {@link RepositoryManager#addRepository(Repository)} call. The implementation should save the
	 * delivered {@link Repository} entries.
	 *
	 * @param repositories
	 *            the {@link Repository} entries which should be saved
	 */
	public void save(List<Repository> repositories);

}
