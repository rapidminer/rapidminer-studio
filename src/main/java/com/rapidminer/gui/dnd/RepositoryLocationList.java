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
package com.rapidminer.gui.dnd;

import java.util.LinkedList;
import java.util.List;

import com.rapidminer.repository.RepositoryLocation;


/**
 * Container class for handling of multiple {@link RepositoryLocation}s
 *
 * @author Adrian Wilke
 * @deprecated since 8.1.2
 * @see TransferableRepositoryEntry
 */
@Deprecated
public class RepositoryLocationList {

	private List<RepositoryLocation> repositoryLocations = new LinkedList<>();

	/** Add single repository location. */
	public void add(RepositoryLocation repositoryLocation) {
		repositoryLocations.add(repositoryLocation);
	}

	/** Gets all repository locations. */
	public List<RepositoryLocation> getAll() {
		return repositoryLocations;
	}

	/**
	 * Removes locations from list, which are already included in others.
	 *
	 * If there are any problems requesting a repository, the input is returned.
	 *
	 * Example: [/1/2/3, /1, /1/2] becomes [/1].
	 */
	public void removeIntersectedLocations() {
		repositoryLocations = RepositoryLocation.removeIntersectedLocations(repositoryLocations);
	}

	/** Returns a String representation of this list. */
	@Override
	public String toString() {
		return repositoryLocations.toString();
	}
}
