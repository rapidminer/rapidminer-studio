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

import java.util.ArrayList;
import java.util.List;
import java.util.ListIterator;

import com.rapidminer.tools.Tools;


/**
 * <p>
 * This class represents a container for objects within a given distance from another object.
 * </p>
 * 
 * <p>
 * It is a simple data structure with associated query and setting methods enabling a linked list of
 * all distance-Containers for all objects in a search Room. As most objects in a SearchSpace are
 * expected to be at different distances to each other, only a small number of objects is likely to
 * be linked to a single distance Container. However, in very dense clusters the number can increase
 * significantly.
 * </p>
 * 
 * @author Stephan Deutsch, Ingo Mierswa
 */
public class KdistanceContainer {

	/**
	 * The value for the distance of the container, e.g. containing all objects of _this_ distance
	 * from another object.
	 */
	private double distance;

	/**
	 * The SearchObject to which this distance container is associated, this is intended to be a
	 * backward reference in case we look at a container and want to know what the information it
	 * holds is related too. This might never be used, but implemented just in case.
	 */
	// private SearchObject distanceAssociatedObject;

	/**
	 * Representing the number of objects in the distance container (e.g. the cardinality of the set
	 * of objects in distance to the associated SearchObject).
	 */
	private int numberOfObjects;

	/**
	 * This List contains the objects in the container (all objects within distance from the
	 * associated SearchObject.
	 */
	private List<SearchObject> listOfObjects;

	/**
	 * This constructor creates a container for an associated SearchObject so (which has to be
	 * referenced) and sets the other aspects of the contained data to zero; it also creates a list
	 * of Objects.
	 */
	public KdistanceContainer(SearchObject so) {
		this.listOfObjects = new ArrayList<SearchObject>(); // construct a new listOfObjects
		// this.distanceAssociatedObject = so; // set the container to associate the SearchObject so
		this.setDistance(0); // set distance to zero (as we do not yet have any object associated
		this.setNumberOfObjects(0); // accordingly the number of objects is zero as well
	}

	/**
	 * Provides the distance of the container as an integer value.
	 */
	public double getDistance() {
		return this.distance;
	}

	/**
	 * Sets the distance of the container to dist (double type value).
	 * 
	 * @param dist
	 */
	public void setDistance(double dist) {
		this.distance = dist;
	}

	/**
	 * Gives the number of objects in the container (e.g. which are actually in the list, but
	 * without the need to ask the list itself about its size).
	 */
	public int getNumberOfObjects() {
		return this.numberOfObjects;
	}

	/**
	 * Sets the number of objects in the container to (integer) number. This should only be used
	 * carefully as the process to add an object to the container takes care of revising the number
	 * of objects value itself.
	 * 
	 * @param number
	 */
	private void setNumberOfObjects(int number) {
		this.numberOfObjects = number;
	}

	/**
	 * <p>
	 * Adds a SearchObject to the container.
	 * </p>
	 * 
	 * <p>
	 * Attention: As you do this, it has to be checked, if the distance between the associated
	 * SearchObject and the object added to the container is equal to the distance of the objects
	 * already in the container.
	 * </p>
	 * 
	 * <p>
	 * To achieve this, the method checks if the distance delivered as a sanity check parameter is
	 * equal to the distance of the container; if yes, the object is added, else not. A boolean
	 * state on the success is returned.
	 * </p>
	 * 
	 * <p>
	 * if the container is empty, e.g. the first object is added, no checks are necessary and hence
	 * the distance check against the initial zero value is not performed thus not preventing the
	 * addition of the object.
	 * </p>
	 * 
	 * <p>
	 * It is recommended to e.g. add an object <i>so</i> to the list of the objects of a container
	 * associated to an object <i>soA</i> with the following process:
	 * </p>
	 * 
	 * <p>
	 * KdistanceContainer.addObject(so, soA.getDistance(so));
	 * </p>
	 */
	public boolean addObject(SearchObject so, double dist) {
		// first, check if the container is empty, in this case the object can be added
		// without additional checks:
		if (this.listOfObjects.size() == 0) {
			this.listOfObjects.add(so);
			this.setDistance(dist);
			this.setNumberOfObjects(this.listOfObjects.size());
			return true;
		} else { // in the other case (container is not empty)
			if (Tools.isEqual(this.getDistance(), dist)) { // check if distance of container is
															// equal to dist of added object
				this.listOfObjects.add(so); // if yes, then add it
				this.setDistance(dist);
				this.setNumberOfObjects(this.listOfObjects.size());
				return true;
			} else { // if the distances are not equal, do not add the object and return false
				return false;
			}
		}
	}

	/**
	 * Returns an object from the list of objects in the container at position i (check needed if
	 * this returns the appropriate object, as internally, an iterator is started at position i in
	 * the list and the next() element is delivered back...).
	 * 
	 * @param i
	 */
	public SearchObject getObject(int i) {
		return listOfObjects.get(i);
	}

	/**
	 * This method delivers an Iterator on the list of objects of the container positioned at the
	 * beginning of the list.
	 */
	public ListIterator<SearchObject> getListIterator() {
		return listOfObjects.listIterator();
	}
}
