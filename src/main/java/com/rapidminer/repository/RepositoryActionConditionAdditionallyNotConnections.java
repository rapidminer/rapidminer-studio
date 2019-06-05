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
 * Checks that the sub-condition is satisfied and additionally the entries are not the special connections folder and
 * (if the inside check is activated) not inside the connections folder.
 *
 * @author Gisa Meier
 * @since 9.3
 */
public class RepositoryActionConditionAdditionallyNotConnections implements RepositoryActionCondition {

	private final RepositoryActionCondition subCondition;
	private final boolean checkInside;

	/**
	 * Creates a new RepositoryActionCondition which can be used to check if the selected {@link Entry}s are fulfilling
	 * a subcondition and are not the special connections folder. In case {@link #checkInside} is {@code true} the
	 * action is also forbidden for entries inside the special folder.
	 *
	 * @param requiredSelectionTypes
	 * 		the required selection types for the subcondition
	 * @param allowRepositories
	 * 		if {@code true} the subcondition is {@link RepositoryActionConditionImplStandard} otherwise it is {@link
	 *        RepositoryActionConditionImplStandardNoRepository}
	 */
	public RepositoryActionConditionAdditionallyNotConnections(Class<?>[] requiredSelectionTypes,
															   boolean allowRepositories) {
		this(requiredSelectionTypes, allowRepositories, false);
	}

	/**
	 * Creates a new RepositoryActionCondition which can be used to check if the selected {@link Entry}s are fulfilling
	 * a subcondition and are not the special connections folder. In case {@link #checkInside} is {@code true} the
	 * action is also forbidden for entries inside the special folder.
	 *
	 * @param requiredSelectionTypes
	 * 		the required selection types for the subcondition
	 * @param allowRepositories
	 * 		if {@code true} the subcondition is {@link RepositoryActionConditionImplStandard} otherwise it is {@link
	 *        RepositoryActionConditionImplStandardNoRepository}
	 * @param checkInside
	 * 		also forbid the action when being inside the special folder
	 */
	public RepositoryActionConditionAdditionallyNotConnections(Class<?>[] requiredSelectionTypes,
															   boolean allowRepositories, boolean checkInside) {
		this(allowRepositories ? new RepositoryActionConditionImplStandard(requiredSelectionTypes) :
				new RepositoryActionConditionImplStandardNoRepository(requiredSelectionTypes), checkInside);
	}

	/**
	 * Creates a new RepositoryActionCondition which can be used to check if the selected {@link Entry}s are
	 * fulfilling a sub condition and are not the special connections folder or inside it if the inside check is activated.
	 */
	public RepositoryActionConditionAdditionallyNotConnections(RepositoryActionCondition subCondition, boolean checkInside) {
		this.subCondition = subCondition;
		this.checkInside = checkInside;
	}

	@Override
	public boolean evaluateCondition(List<Entry> entryList) {
		if (!subCondition.evaluateCondition(entryList)) {
			return false;
		}

		for (Entry givenEntry : entryList) {

			// special folders not allowed
			if (givenEntry instanceof Folder && ((Folder) givenEntry).isSpecialConnectionsFolder()) {
				return false;
			}

			if(checkInside){
				// entries inside special folders not allowed
				if (givenEntry instanceof Folder) {
					if (RepositoryTools.isInSpecialConnectionsFolder((Folder) givenEntry)) {
						return false;
					}
				} else if (RepositoryTools.isInSpecialConnectionsFolder(givenEntry.getContainingFolder())) {
					return false;
				}
			}
		}

		// all conditions have been met, so return true
		return true;
	}

}
