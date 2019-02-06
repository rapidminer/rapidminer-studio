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
package com.rapidminer.gui.new_plotter.utility;

import java.util.List;


/**
 * @author Nils Woehler
 * @deprecated since 9.2.0
 */
@Deprecated
public class ListUtility {

	/**
	 * This function shifts the element to a new index in the list. If element is shifted right the
	 * element currently at the new position (if any) and any subsequent elements are shifted to the
	 * right (adds one to their indices). If the element is shifted left the element currently at
	 * the new position (if any) and any subsequent elements up to the old index are shifted to the
	 * right (adds one to their indices). All elements from the right side of the old index are
	 * shifted left. <b> If size of list is one or the old index equals the new index, no changes
	 * will commence. Returns <code>true</code> if changes have been performed, <code>false</code>
	 * otherwise.
	 */
	public static <T> boolean changeIndex(List<T> list, T element, int index) {

		final int oldIndex = list.indexOf(element);
		final int listSize = list.size();
		if (index != oldIndex && oldIndex != -1) {

			if (listSize == 1) {
				return true;
			}

			// remove element
			list.remove(oldIndex);

			// add element at index
			list.add(index, element);

			return true;
		}
		return false;
	}

}
