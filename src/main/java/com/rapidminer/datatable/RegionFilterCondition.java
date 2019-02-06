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

import com.rapidminer.tools.math.container.Range;

import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Map.Entry;


/**
 * This FilterCondition is a multidimensional equivalent to the RangeFilterCondition.
 * 
 * @author Sebastian Land
 */
public class RegionFilterCondition implements DataTableFilterCondition {

	public static final class Region {

		Map<Integer, Range> delimiters = new LinkedHashMap<Integer, Range>();

		/**
		 * This method will return true if this region is restricted by adding the given range in
		 * the given dimension. If it remains unchanged, because the range covers this region
		 * completely, this method will return false;
		 */
		public boolean addRestrictingRange(int dimension, Range range) {
			Range existing = delimiters.get(dimension);
			if (existing == null) {
				delimiters.put(dimension, range);
				return true;
			} else {
				if (existing.contains(range) && !existing.equals(range)) {
					delimiters.put(dimension, range);
					return true;
				}
			}
			return false;
		}

		public boolean containsRow(DataTableRow row) {
			for (Entry<Integer, Range> pair : delimiters.entrySet()) {
				if (!pair.getValue().contains(row.getValue(pair.getKey()))) {
					return false;
				}
			}
			return true;
		}

		@Override
		public boolean equals(Object arg) {
			if (arg instanceof Region) {
				Region region = ((Region) arg);
				boolean equal = region.delimiters.size() == delimiters.size();
				for (Integer key : delimiters.keySet()) {
					equal &= delimiters.get(key).equals(region.delimiters.get(key));
					if (!equal) {
						break;
					}
				}
				return equal;
			}
			return false;
		}
	}

	private Region region;

	public RegionFilterCondition(Region region) {
		this.region = region;
	}

	@Override
	public boolean keepRow(DataTableRow row) {
		return region.containsRow(row);
	}

	/**
	 * This returns a region object, which stores all ranged in all dimensions. They might be
	 * successively added by calling the addRange method.
	 */
	public static final Region createRegion() {
		return new Region();
	}
}
