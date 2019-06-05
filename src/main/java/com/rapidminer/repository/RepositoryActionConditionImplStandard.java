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

import java.util.Arrays;
import java.util.List;

import com.rapidminer.repository.gui.actions.AbstractRepositoryAction;


/**
 * Declares a condition for {@link AbstractRepositoryAction}. If the conditions are met, the action
 * is shown, otherwise it will not be shown.
 * 
 * @author Marco Boeck
 * 
 */
public class RepositoryActionConditionImplStandard implements RepositoryActionCondition {

	/** selection must be of one of the types listed here */
	private final List<Class<?>> requiredSelectionTypeList;

	/** selection repository must be of one of the types listed here */
	private final List<Class<?>> requiredSelectionRepositoryTypeList;

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
	public RepositoryActionConditionImplStandard(List<Class<?>> requiredSelectionTypeList,
			List<Class<?>> requiredSelectionRepositoryTypeList) {
		if (requiredSelectionTypeList == null || requiredSelectionRepositoryTypeList == null) {
			throw new IllegalArgumentException("lists must not be null!");
		}
		this.requiredSelectionTypeList = requiredSelectionTypeList;
		this.requiredSelectionRepositoryTypeList = requiredSelectionRepositoryTypeList;
	}

	/**
	 * Creates a new RepositoryActionCondition which can be used to check if the selected
	 * {@link Entry}s meet the given conditions.
	 *
	 * @param requiredSelectionTypes
	 *            a list with {@link Entry} types. Each selected {@link Entry} must be of one of the
	 *            types on the list or the condition is not met.
	 */
	public RepositoryActionConditionImplStandard(Class<?>[] requiredSelectionTypes) {
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
	public RepositoryActionConditionImplStandard(Class<?>[] requiredSelectionTypes,
			Class<?>[] requiredSelectionRepositoryTypes) {
		if (requiredSelectionTypes == null || requiredSelectionRepositoryTypes == null) {
			throw new IllegalArgumentException("arrays must not be null!");
		}
		this.requiredSelectionTypeList = Arrays.asList(requiredSelectionTypes);
		this.requiredSelectionRepositoryTypeList = Arrays.asList(requiredSelectionRepositoryTypes);
	}

	@Override
	public boolean evaluateCondition(List<Entry> entryList) {
		if (entryList == null) {
			return false;
		}

		for (Entry givenEntry : entryList) {

			// make sure each entry's type is in the requiredTypes list, if not condition evaluates
			// to false
			boolean entryTypeConditionMet = requiredSelectionTypeList.isEmpty() ? true : false;
			for (Class<?> requiredEntry : requiredSelectionTypeList) {
				if (requiredEntry.isAssignableFrom(givenEntry.getClass())) {
					entryTypeConditionMet = true;
					break;
				}
			}
			// wrong entry type
			if (!entryTypeConditionMet) {
				return false;
			}

			// make sure each entry's repository type is in the requiredRepositoryTypes list, if not
			// condition evaluates to false
			// if the repository condition list is empty, the condition is automatically met
			boolean entryRepositoryConditionMet = requiredSelectionRepositoryTypeList.isEmpty() ? true : false;
			for (Class<?> requiredRepository : requiredSelectionRepositoryTypeList) {
				try {
					if (requiredRepository.isAssignableFrom(givenEntry.getLocation().getRepository().getClass())) {
						entryRepositoryConditionMet = true;
						break;
					}
				} catch (RepositoryException e) {
					return false;
				}
			}
			// wrong repository type
			if (!entryRepositoryConditionMet) {
				return false;
			}
		}

		// all conditions have been met, so return true
		return true;
	}

}
