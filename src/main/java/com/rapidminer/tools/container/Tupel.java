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

import java.io.Serializable;


/**
 * This class can be used to build pairs of typed objects and sort them. ATTENTION!! This class is
 * not usable for hashing since only the first version is used as hash entry. To use a hash function
 * on a tupel, use Pair!
 * 
 * @author Sebastian Land
 */
public class Tupel<T1 extends Comparable<T1>, T2> implements Comparable<Tupel<T1, T2>>, Serializable {

	private static final long serialVersionUID = 9219166123756517965L;

	private T1 t1;

	private T2 t2;

	public Tupel(T1 t1, T2 t2) {
		this.t1 = t1;
		this.t2 = t2;
	}

	public T1 getFirst() {
		return t1;
	}

	public T2 getSecond() {
		return t2;
	}

	@Override
	public int compareTo(Tupel<T1, T2> o) {
		return t1.compareTo(o.getFirst());
	}

	@Override
	public boolean equals(Object o) {
		if (this == o) {
			return true;
		}
		if (!(o instanceof Tupel)) {
			return false;
		}
		Tupel<?, ?> a = (Tupel<?, ?>) o;
		if (!this.t1.equals(a.t1)) {
			return false;
		}
		return true;
	}

	@Override
	public int hashCode() {
		return this.t1.hashCode();
	}

	@Override
	public String toString() {
		return "(" + t1 + ", " + t2 + ")";
	}
}
