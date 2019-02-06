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
package com.rapidminer.tools.container;

/**
 * Helper class which stores a {@link String} and a count value and implements the
 * {@link Comparable} interface.
 * 
 * @author Marco Boeck
 */
public class ValueAndCount implements Comparable<ValueAndCount> {

	private String value;
	private int count;

	/**
	 * Creates a new {@link ValueAndCount} instance.
	 * 
	 * @param value
	 * @param count
	 */
	public ValueAndCount(String value, int count) {
		this.value = value;
		this.count = count;
	}

	/**
	 * Returns the {@link String} value.
	 * 
	 * @return
	 */
	public String getValue() {
		return value;
	}

	/**
	 * Returns the count.
	 * 
	 * @return
	 */
	public int getCount() {
		return count;
	}

	@Override
	public int compareTo(ValueAndCount o) {
		int result = -1 * Double.compare(this.count, o.count);
		if (result == 0) {
			if (this.value == null) {
				if (o.value == null) {
					return 0;
				} else {
					return -1;
				}
			} else {
				return this.value.compareTo(o.value);
			}
		} else {
			return result;
		}
	}

	@Override
	public int hashCode() {
		final int prime = 31;
		int result = 1;
		result = prime * result + count;
		result = prime * result + (value == null ? 0 : value.hashCode());
		return result;
	}

	@Override
	public boolean equals(Object obj) {
		if (this == obj) {
			return true;
		}
		if (obj == null) {
			return false;
		}
		if (getClass() != obj.getClass()) {
			return false;
		}
		ValueAndCount other = (ValueAndCount) obj;
		if (count != other.count) {
			return false;
		}
		if (value == null) {
			if (other.value != null) {
				return false;
			}
		} else if (!value.equals(other.value)) {
			return false;
		}
		return true;
	}

	@Override
	public String toString() {
		return getValue() + " (" + getCount() + ")";
	}
}
