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
 * Can be used to declare repository action conditions based upon the given list of repository
 * {@link Entry}.
 * 
 * @author Marco Boeck
 * 
 */
public interface RepositoryActionCondition {

	/**
	 * If the condition is met, returns true, otherwise returns false.
	 * <p>
	 * An empty list will always return true!</br> A {@code null} list will always return false!
	 * 
	 * @param entryList
	 *            a list of repository {@link Entry} or {@code null}
	 * @return true if the condition is met; false otherwise
	 */
	public boolean evaluateCondition(List<Entry> entryList);

}
