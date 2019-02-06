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
package com.rapidminer.tools;

/**
 * Class used to represent an object together with a double weight.
 * 
 * @author Michael Wurst
 * 
 */
public class WeightedObject<E> implements Comparable<WeightedObject<E>> {

	/** The object. */
	private final E object;

	/** The associated weight. */
	private final double weight;

	public WeightedObject(E object, double weight) {
		this.object = object;
		this.weight = weight;
	}

	@Override
	public int compareTo(WeightedObject<E> objToCompare) {
		if (this.getWeight() > objToCompare.getWeight()) {
			return 1;
		} else if (this.getWeight() < objToCompare.getWeight()) {
			return -1;
		} else if (this.hashCode() > objToCompare.hashCode()) {
			return 1;
		} else if (this.hashCode() < objToCompare.hashCode()) {
			return -1;
		} else {
			return 0;
		}
	}

	/**
	 * Returns the object.
	 * 
	 * @return Object
	 */
	public E getObject() {
		return object;
	}

	/**
	 * Returns the weight.
	 * 
	 * @return double
	 */
	public double getWeight() {
		return weight;
	}

	@Override
	public String toString() {
		return object.toString() + ":" + weight;
	}

	@Override
	public boolean equals(Object obj) {
		if (this == obj) {
				return true;
		}
		if (obj instanceof WeightedObject) {
			return ((WeightedObject<?>) obj).getObject().equals(getObject());
		} else {
			return false;
		}
	}

	@Override
	public int hashCode() {
		return object.hashCode();
	}
}
