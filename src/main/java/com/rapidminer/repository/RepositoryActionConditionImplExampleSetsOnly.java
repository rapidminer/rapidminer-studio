/**
 * Copyright (C) 2001-2016 by RapidMiner and the contributors
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

import com.rapidminer.operator.ports.metadata.ExampleSetMetaData;
import com.rapidminer.repository.gui.actions.AbstractRepositoryAction;
import com.rapidminer.repository.internal.remote.RemoteIOObjectEntry;

import java.util.List;


/**
 * Declares a condition for {@link AbstractRepositoryAction}. If the conditions are met, the action
 * is shown, otherwise it will not be shown. This condition only evaluates to {@code true} if the
 * selected entries are ExampleSets.
 * 
 * @author Marco Boeck
 * 
 */
public class RepositoryActionConditionImplExampleSetsOnly implements RepositoryActionCondition {

	@Override
	public boolean evaluateCondition(List<Entry> entryList) {
		if (entryList == null) {
			return false;
		}

		for (Entry givenEntry : entryList) {

			// make sure each entry is an ExampleSet, if not condition evaluates to false
			if (givenEntry instanceof IOObjectEntry) {
				IOObjectEntry entry = (IOObjectEntry) givenEntry;
				try {
					if (!(entry.retrieveMetaData() instanceof ExampleSetMetaData)) {
						return false;
					}
				} catch (RepositoryException e) {
					return false;
				}
			} else if (givenEntry instanceof RemoteIOObjectEntry) {
				RemoteIOObjectEntry entry = (RemoteIOObjectEntry) givenEntry;
				try {
					if (!(entry.retrieveMetaData() instanceof ExampleSetMetaData)) {
						return false;
					}
				} catch (RepositoryException e) {
					return false;
				}
			} else {
				return false;
			}
		}

		// all conditions have been met, so return true
		return true;
	}

}
