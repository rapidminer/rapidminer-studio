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
package com.rapidminer.datatable;

/**
 * This is the interface of conditions for FilteredDataTables. They might be stacked in order to
 * decide which examples should be kept.
 * 
 * @author Sebastian Land
 * 
 */
public interface DataTableFilterCondition {

	/**
	 * This method decides if the given data row is kept or filtered out. If return true, the row
	 * will be in the resulting filtered data set. If returns false, the row will be removed in the
	 * resulting set.
	 */
	public boolean keepRow(DataTableRow row);

}
