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
 * Same as {@link RepositoryActionConditionImplStandard}, but also does not show the action in case
 * of entry being a {@link Repository}.
 * 
 * @author Marco Boeck
 */
public class RepositoryActionConditionImplStandardNoRepository extends RepositoryActionConditionImplStandard {

	/**
	 * Creates a new RepositoryActionCondition which can be used to check if the selected
	 * {@link Entry}s meet the given conditions.
	 * 
	 * @param requiredSelectionTypeList
	 *            a list with {@link Entry} types. Each selected {@link Entry} must be of one of the
	 *            types on the list or the condition is not met.
	 * @param requiredSelectionRepositoryTypeList
	 *            a list with {@link Repository} types. Each selected {@link Entry} must be of the
	 *            types on the list or the condition is not met.
	 */
	public RepositoryActionConditionImplStandardNoRepository(List<Class<?>> requiredSelectionTypeList,
			List<Class<?>> requiredSelectionRepositoryTypeList) {
		super(requiredSelectionTypeList, requiredSelectionRepositoryTypeList);
	}

	/**
	 * Creates a new RepositoryActionCondition which can be used to check if the selected
	 * {@link Entry}s meet the given conditions.
	 *
	 * @param requiredSelectionTypes
	 *            a list with {@link Entry} types. Each selected {@link Entry} must be of one of the
	 *            types on the list or the condition is not met.
	 */
	public RepositoryActionConditionImplStandardNoRepository(Class<?>[] requiredSelectionTypes) {
		this(requiredSelectionTypes, new Class<?>[0]);
	}

	/**
	 * Creates a new RepositoryActionCondition which can be used to check if the selected
	 * {@link Entry}s meet the given conditions.
	 * 
	 * @param requiredSelectionTypes
	 *            a list with {@link Entry} types. Each selected {@link Entry} must be of one of the
	 *            types on the list or the condition is not met.
	 * @param requiredSelectionRepositoryTypes
	 *            a list with {@link Repository} types. Each selected {@link Entry} must be of the
	 *            types on the list or the condition is not met.
	 */
	public RepositoryActionConditionImplStandardNoRepository(Class<?>[] requiredSelectionTypes,
			Class<?>[] requiredSelectionRepositoryTypes) {
		super(requiredSelectionTypes, requiredSelectionRepositoryTypes);
	}

	@Override
	public boolean evaluateCondition(List<Entry> entryList) {
		if (entryList == null) {
			return false;
		}

		for (Entry givenEntry : entryList) {
			if (givenEntry instanceof Repository) {
				return false;
			}
		}
		return super.evaluateCondition(entryList);
	}

}
