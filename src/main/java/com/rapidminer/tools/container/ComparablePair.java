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
 * A basic container class for a comparable pair of comparable objects.
 *
 * @author Sabrina Kirstein
 */
public class ComparablePair<T extends Comparable<T>, K extends Comparable<K>> extends Pair<T, K> implements
Comparable<Pair<T, K>> {

	private static final long serialVersionUID = 1L;

	public ComparablePair(T t, K k) {
		super(t, k);
	}

	@Override
	public int compareTo(Pair<T, K> o) {

		// if the first element is equal, compare the second element
		int result = getFirst().compareTo(o.getFirst());
		if (result == 0) {
			return getSecond().compareTo(o.getSecond());
		}
		return result;
	}

}
