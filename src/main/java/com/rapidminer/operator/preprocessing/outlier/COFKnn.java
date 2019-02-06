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
package com.rapidminer.operator.preprocessing.outlier;

/**
 * The COFKnn class creates the k nearest Objects for an cof object
 * 
 * @author Motaz K. Saad
 */
public class COFKnn implements Comparable<COFKnn> {

	COFObject cofobject;
	double distance;

	/**
	 * @param values
	 * @param distance
	 */
	public COFKnn(COFObject cofobject, double distance) {
		this.cofobject = cofobject;
		this.distance = distance;
	}

	/**
	 * @return the cofobject
	 */
	public COFObject getCofobject() {
		return cofobject;
	}

	/**
	 * @param cofobject
	 *            the cofobject to set
	 */
	public void setCofobject(COFObject cofobject) {
		this.cofobject = cofobject;
	}

	/**
	 * @return the distance
	 */
	public double getDistance() {
		return distance;
	}

	/**
	 * @param distance
	 *            the distance to set
	 */
	public void setDistance(double distance) {
		this.distance = distance;
	}

	/*
	 * (non-Javadoc)
	 * 
	 * @see java.lang.Comparable#compareTo(java.lang.Object)
	 */
	@Override
	public int compareTo(COFKnn o) {

		if (this.distance < o.getDistance()) {
			return 1;
		}
		if (this.distance > o.getDistance()) {
			return -1;
		} else {
			return 0;
		}
	}
}
