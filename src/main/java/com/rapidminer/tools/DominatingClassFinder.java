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
package com.rapidminer.tools;

import java.util.Collection;
import java.util.HashSet;
import java.util.LinkedList;
import java.util.List;
import java.util.Set;
import java.util.logging.Level;


/**
 * @author Nils Woehler
 * 
 */
public class DominatingClassFinder<T> {

	/**
	 * @return Returns the next dominating or parent class found in the list of available classes.
	 */
	public Class<? extends T> findNextDominatingClass(Class<? extends T> clazz,
			Collection<Class<? extends T>> availableClasses) {

		Set<Class<? extends T>> candidates = new HashSet<Class<? extends T>>();

		// fetch candidates
		for (Class<? extends T> candidate : availableClasses) {
			if (candidate.isAssignableFrom(clazz)) {
				candidates.add(candidate);
			}
		}

		// iterate over candidates and extract dominated candidates
		boolean dominatedClassesFound = true;
		while (dominatedClassesFound) {
			dominatedClassesFound = false;
			List<Class<? extends T>> dominatedList = new LinkedList<Class<? extends T>>();

			// if more than one candidate is left...
			if (candidates.size() != 1) {

				// iterate over all candidate pairs and add all candidates that are dominated by
				// other candidates into a list
				for (Class<? extends T> candidate : candidates) {
					for (Class<? extends T> comperable : candidates) {
						if (candidate != comperable) {
							if (comperable.isAssignableFrom(candidate)) {
								dominatedList.add(candidate);
								dominatedClassesFound = true;
							}
						}
					}
				}
			}

			// if dominates classes have been found set them as new candidates for the next
			// iteration
			if (dominatedClassesFound) {
				candidates.clear();
				candidates.addAll(dominatedList);
			}
		}
		// this loop should break with only one candidate left, BUT: theoretically there can be more
		// than one

		// if this is the case, log an error...
		if (candidates.size() > 1) {
			LogService
					.getRoot()
					.log(Level.INFO,
							"com.rapidminer.tools.DominatingClassFinder.more_than_one_renderable_candidate_for_the_result_of_classname",
							clazz.getName());
		}

		// and select the first candidate found
		if (candidates.isEmpty()) {
			return null;
		} else {
			return candidates.iterator().next();
		}
	}

}
