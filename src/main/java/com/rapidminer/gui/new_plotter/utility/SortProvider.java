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
 * @author Marius Helf
 * @deprecated since 9.2.0
 */
@Deprecated
public interface SortProvider {

	public enum SortCriterion {
		NUMERICAL, NOMINAL, NONE
	}

	public enum SortOrder {
		NONE, ASCENDING, DESCENDING
	}

	public void setSortCriterion(SortCriterion sortCriterion);

	public void setSortOrder(SortOrder sortOrder);

	public SortCriterion getSortCriterion();

	public SortOrder getSortOrder();

	/**
	 * Sorts the provided list of values. Might change the original list.
	 */
	public List<Double> sortValues(List<Double> values);

	/**
	 * Sorts the provided list of {@link ValueRange}s. Might change the original list.
	 */
	public List<ValueRange> sortGroups(List<ValueRange> valueGroups);

	public SortProvider clone();
}
