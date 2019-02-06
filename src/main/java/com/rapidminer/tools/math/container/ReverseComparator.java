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
package com.rapidminer.tools.math.container;

import java.io.Serializable;
import java.util.Comparator;


/**
 * This comparator reverses the sort direction for a given comparator.
 * 
 * @author Sebastian Land
 * @param <F>
 *            the type of the comparable
 */
public class ReverseComparator<F> implements Comparator<F>, Serializable {

	private static final long serialVersionUID = 3141613972925245131L;

	private Comparator<? super F> comparator;

	public ReverseComparator(Comparator<? super F> comp) {
		this.comparator = comp;
	}

	@Override
	public int compare(F o1, F o2) {
		return -comparator.compare(o1, o2);
	}
}
