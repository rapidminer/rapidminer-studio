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

import com.rapidminer.repository.gui.actions.AbstractRepositoryAction;

import java.util.List;


/**
 * Declares a condition for {@link AbstractRepositoryAction}. If the entry is a configurable
 * repository, the action is shown, otherwise it will not be shown.
 * 
 * @author Marco Boeck
 * 
 */
public class RepositoryActionConditionImplConfigRepository implements RepositoryActionCondition {

	/**
	 * Creates a new RepositoryActionCondition which can be used to check if the selected
	 * {@link Entry}s are of the type {@link Repository} and are configurable.
	 */
	public RepositoryActionConditionImplConfigRepository() {

	}

	@Override
	public boolean evaluateCondition(List<Entry> entryList) {
		if (entryList == null) {
			return false;
		}

		for (Entry givenEntry : entryList) {

			// make sure each entry's type is a repository, if not condition evaluates to false
			if (!(givenEntry instanceof Repository)) {
				return false;
			}
			// we know it's a repository by now, so check if it's configurable
			Repository repo = (Repository) givenEntry;
			if (!repo.isConfigurable()) {
				return false;
			}
		}

		// all conditions have been met, so return true
		return true;
	}

}
