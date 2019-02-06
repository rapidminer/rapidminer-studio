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
 * A basic container class for 3 objects.
 * 
 * @author Marco Boeck
 */
public class Triple<T, K, U> implements Serializable {

	private static final long serialVersionUID = 1L;

	private T first;

	private K second;

	private U third;

	public Triple(T t, K k, U u) {
		this.setFirst(t);
		this.setSecond(k);
		this.setThird(u);
	}

	public T getFirst() {
		return first;
	}

	public void setFirst(T first) {
		this.first = first;
	}

	public K getSecond() {
		return second;
	}

	public void setSecond(K second) {
		this.second = second;
	}

	public U getThird() {
		return third;
	}

	public void setThird(U third) {
		this.third = third;
	}

	@Override
	public String toString() {
		String tString = (getFirst() == null) ? "null" : getFirst().toString();
		String kString = (getSecond() == null) ? "null" : getSecond().toString();
		String uString = (getThird() == null) ? "null" : getThird().toString();
		return tString + " : " + kString + " : " + uString;
	}

	@Override
	public int hashCode() {
		final int prime = 31;
		int result = 1;
		result = prime * result + ((first == null) ? 0 : first.hashCode());
		result = prime * result + ((second == null) ? 0 : second.hashCode());
		result = prime * result + ((third == null) ? 0 : third.hashCode());
		return result;
	}

	@Override
	public boolean equals(Object obj) {
		if (this == obj) {
			return true;
		}
		if (obj == null || getClass() != obj.getClass()) {
			return false;
		}
		Triple<?, ?, ?> other = (Triple<?, ?, ?>) obj;
		if (first == null) {
			if (other.first != null) {
				return false;
			}
		} else if (!first.equals(other.first)) {
			return false;
		}
		if (second == null) {
			if (other.second != null) {
				return false;
			}
		} else if (!second.equals(other.second)) {
			return false;
		}
		if (third == null) {
			if (other.third != null) {
				return false;
			}
		} else if (!third.equals(other.third)) {
			return false;
		}
		return true;
	}
}
