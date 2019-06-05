/**
 * Copyright (C) 2001-2019 by RapidMiner and the contributors
 *
 * Complete list of developers available at our web site:
 *
 * http://rapidminer.com
 *
 * This program is free software: you can redistribute it and/or modify it under the terms of the GNU Affero General
 * Public License as published by the Free Software Foundation, either version 3 of the License, or (at your option) any
 * later version.
 *
 * This program is distributed in the hope that it will be useful, but WITHOUT ANY WARRANTY; without even the implied
 * warranty of MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the GNU Affero General Public License for more
 * details.
 *
 * You should have received a copy of the GNU Affero General Public License along with this program. If not, see
 * http://www.gnu.org/licenses/.
 */
package com.rapidminer.repository;

import java.util.List;


/**
 * Checks that the sub-condition is satisfied and additionally the entry is either a repository that supports connections, or the special Connections folder.
 *
 * @author Marco Boeck
 * @since 9.3.0
 */
public class RepositoryActionConditionRepositoryAndConnections implements RepositoryActionCondition {

	private final RepositoryActionCondition subCondition;

	/**
	 * Creates a new RepositoryActionCondition which can be used to check if the selected {@link Entry}s are fulfilling
	 * a subcondition and are either a repository or the special connections folder.
	 *
	 * @param requiredSelectionTypes
	 * 		the required selection types for the subcondition
	 */
	public RepositoryActionConditionRepositoryAndConnections(Class<?>[] requiredSelectionTypes) {
		this(new RepositoryActionConditionImplStandard(requiredSelectionTypes));
	}

	/**
	 * Creates a new RepositoryActionCondition which can be used to check if the selected {@link Entry}s are
	 * fulfilling a sub condition and are not the special connections folder or inside it if the inside check is activated.
	 */
	public RepositoryActionConditionRepositoryAndConnections(RepositoryActionCondition subCondition) {
		this.subCondition = subCondition;
	}

	@Override
	public boolean evaluateCondition(List<Entry> entryList) {
		if (!subCondition.evaluateCondition(entryList)) {
			return false;
		}

		for (Entry givenEntry : entryList) {

			if (givenEntry instanceof Repository) {
				if (!((Repository) givenEntry).supportsConnections()) {
					// if the repository does not support connections, we can abort straight away
					return false;
				}
			} else if (!((Folder) givenEntry).isSpecialConnectionsFolder()) {
				// likewise if the folder is not the special Connections folder, we can also abort
				return false;
			}
		}

		// all conditions have been met, so return true
		return true;
	}

}
