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

import com.rapidminer.operator.Operator;
import com.rapidminer.operator.ProcessStoppedException;

import java.util.Enumeration;
import java.util.ListIterator;
import java.util.Vector;


/**
 * SearchSpace is a class for building a room full of SearchObjects (see class definition) and
 * provides various methods to place those objects into the SearchSpace (by associating those
 * Objects to the list of objects in the SearchSpace) as well as to do some Outlier Tests on those
 * Objects.
 * 
 * @author Stephan Deutsch, Ingo Mierswa
 */
public class SearchSpace {

	/**
	 * This variable holds the number of dimensions for the Searchroom. As
	 * {@link SearchObject#dimensions} hold their own number of dimensions per instance of that
	 * class, the dimensions of the SearchSpace and its associated SearchObjects must not assumed to
	 * be equal. E.g. a SearchObject can have more or fewer dimensions than the SearchSpace.
	 * Consistency checks should be performed as necessary, but not as mandatory.
	 */
	private int dimensions;

	/**
	 * The list of SearchObjects in the SearchSpace (as a Vector class).
	 * 
	 */
	private Vector<SearchObject> listOfObjects;

	/**
	 * holds the minimum value of the dimensions of all SearchObjects in the SearchSpace and is
	 * updated automatically as SearchObjects are added to the SearchSpace. This is to provide
	 * meta-data for statistical analysis over the SearchSpace.
	 */
	private double[] minimumVectorValue;

	/**
	 * holds the maximum value of the dimensions of all SearchObjects in the SearchSpace and is
	 * updated automatically as SearchObjects are added to the SearchSpace. This is to provide
	 * meta-data for statistical analysis over the SearchSpace.
	 */
	private double[] maximumVectorValue;

	/**
	 * Holds the range (interval) value of the dimensions of all SearchObjects in the SearchSpace
	 * and is updated automatically as SearchObjects are added to the SearchSpace. This is to
	 * provide meta-data for statistical analysis over the SearchSpace.
	 */
	private double[] rangeVectorValue;

	/**
	 * The lower bound for a potential MinPts search (e.g. a LOF search).
	 */
	// private int minPtsLowerBound;

	/**
	 * The upper bound for a potential MinPts search (e.g. a LOF search).
	 */
	// private int minPtsUpperBound;

	/**
	 * This constructor creates a SearchSpace with (integer) <i>dim</i> dimensions and initializes
	 * all fields in the instance of that Class with zero values where appropriate.
	 */
	public SearchSpace(int dim) {
		this.dimensions = dim;
		this.createListOfObjects(); // create a list of Objects mapped to this.listOfObjects
		// initialize the SearchRooms Vektor parameters by creating arrays and filling them with
		// zeros
		this.minimumVectorValue = new double[dim];
		this.maximumVectorValue = new double[dim];
		this.rangeVectorValue = new double[dim];
		for (int i = 0; i < this.dimensions; i++) {
			this.minimumVectorValue[i] = 0;
			this.maximumVectorValue[i] = 0;
			this.rangeVectorValue[i] = 0;
		}
	}

	/**
	 * This constructor creates a SearchSpace with (integer) <i>2</i> dimensions as a default and
	 * initializes all fields in the instance of that Class with zero values where appropriate.
	 */
	public SearchSpace() { // construct a searchroom with at least 2 dimensions
		this(2);
	}

	/**
	 * This constructor creates a SearchSpace with (integer) <i>dim</i> dimensions and initializes
	 * all fields in the instance of that Class with zero values where appropriate.
	 */
	public SearchSpace(int dim, int minptslb, int minptsub) {
		this(dim);
		// this.minPtsLowerBound = minptslb;
		// this.minPtsUpperBound = minptsub;
	}

	/**
	 * Returns the (integer) number of objects in the Searchroom (associated with it via
	 * {@link #addObject(SearchObject)} to the room) as an integer value as we overall do not expect
	 * the searchroom to hold more than 2 billion objects.
	 * 
	 */
	public int getNumberOfObjects() {
		return this.listOfObjects.size();
	}

	/**
	 * Sets the minimum value of all SearchObjects in a SearchSpace to a value for a dimension dim.
	 * 
	 * @param dim
	 * @param value
	 */
	void setMinimumVectorValue(int dim, double value) {
		this.minimumVectorValue[dim] = value;
	}

	/**
	 * Returns the minimum value of all SearchObjects in a SearchSpace for a dimension dim.
	 * 
	 * @param dim
	 */
	double getMinimumVectorValue(int dim) {
		return this.minimumVectorValue[dim];
	}

	/**
	 * Sets the maximum value of all SearchObjects in a SearchSpace to a value for a dimension dim.
	 * 
	 * @param dim
	 * @param value
	 */
	void setMaximumVectorValue(int dim, double value) {
		this.maximumVectorValue[dim] = value;
	}

	/**
	 * Returns the maximum value of all SearchObjects in a SearchSpace for a dimension dim.
	 * 
	 * @param dim
	 */
	double getMaximumVectorValue(int dim) {
		return this.maximumVectorValue[dim];
	}

	/**
	 * Sets the range value (maximum - minimum) of all SearchObjects in a SearchSpace to a value for
	 * a dimension dim.
	 * 
	 * @param dim
	 * @param value
	 */
	void setRangeVectorValue(int dim, double value) {
		this.rangeVectorValue[dim] = value;
	}

	/**
	 * Returns the range value (maximum - minimum) of all SearchObjects in a SearchSpace for a
	 * dimension dim.
	 * 
	 * @param dim
	 */
	double getRangeVectorValue(int dim) {
		return this.rangeVectorValue[dim];
	}

	/**
	 * <p>
	 * Sets the number of dimensions for the SearchSpace to dim.
	 * </p>
	 * <p>
	 * <em>Attention</em>: This is a value that the SearchSpace keeps for the purpose of consistency
	 * checks for all SearchObjects (as each SearchObject has its own number of dimensions and not
	 * all the dimensions of the SearchObjects need to be the same - to give implementation
	 * freedom).
	 * </p>
	 * 
	 * @param dim
	 */
	public void setDimensions(int dim) {
		this.dimensions = dim;
	}

	/**
	 * Returns the number of dimensions of the SearchSpace.
	 */
	public int getDimensions() {
		return this.dimensions;
	}

	/**
	 * Creates a listOfObjects (e.g. a new Vector Class instance within the SearchSpace) and is used
	 * by a constructor.
	 * 
	 */
	void createListOfObjects() {
		this.listOfObjects = new Vector<>();
	}

	/** Delivers the list of objects. */
	public Vector<SearchObject> getSearchObjects() {
		return listOfObjects;
	}

	/**
	 * This method returns the outlierstatus of the Searchobject (element at index i) in the
	 * SearchSpace from the Searchroom's listOfObjects.
	 * 
	 * @param i
	 * @return the boolean outlier status
	 */
	public boolean getSearchObjectOutlierStatus(int i) {
		SearchObject so = this.listOfObjects.elementAt(i);
		return so.getOutlierStatus();
	}

	/**
	 * This adds a SearchObject to the SearchSpace.
	 * 
	 * <p>
	 * It prints a warning to STDOUT in case the dimensions of the SearchObject and SearchSpace are
	 * incompatible, but as the SearchSpace can perform some operations over SearchObjects with
	 * different dimensions, this is not a showstopper.
	 * 
	 * <p>
	 * The method also automatically updates the min/max/range information the SearchSpace knows for
	 * itself.
	 * 
	 * @param objectToAdd
	 */
	public void addObject(SearchObject objectToAdd) {
		this.listOfObjects.addElement(objectToAdd); // add the object of type SearchObject to the
													// SearchSpace
		for (int i = 0; i < this.getDimensions(); i++) {
			if (this.getMinimumVectorValue(i) > objectToAdd.getVektor(i)) {
				this.setMinimumVectorValue(i, objectToAdd.getVektor(i));
			}
			if (this.getMaximumVectorValue(i) < objectToAdd.getVektor(i)) {
				this.setMaximumVectorValue(i, objectToAdd.getVektor(i));
			}
			this.setRangeVectorValue(i, this.getMaximumVectorValue(i) - this.getMinimumVectorValue(i));
		}
	}

	/**
	 * This method returns a SearchObject with the i-th index in the listOfObjects; the result has
	 * to be casted to SearchObject (Vector Class speciality, as it returns only a JAVA Object Class
	 * object). This is better than to access the listOfObjects directly, but sadly I do not use it
	 * consistently. Maybe in the cleaning-up, this will be changed.
	 * 
	 * @param index
	 */
	public SearchObject getObject(int index) {
		return this.listOfObjects.elementAt(index);
	}

	/**
	 * This method returns an Enumeration of all SearchObjects from a SearchSpace.
	 */
	public Enumeration<SearchObject> getObjects() {
		return this.listOfObjects.elements();
	}

	/**
	 * Checks the dimensional integrity of the Searchroom and returns an array if int values for
	 * each object with 0 for equal dimensions of room and object, -1 for less dimensions in the
	 * room than object thinks it has and +1 for more dimensions in the room than object has.
	 * 
	 * <p>
	 * Method prints to the STDOUT a message on whether the overall integrity is given (all objects
	 * have the same dimensions as the searchroom. ATTN: this checks only those objects in the
	 * search room, e.g. which have been added to it using {@link #addObject(SearchObject)}.
	 */
	int[] dimensionsIntegrityCheck() {
		SearchObject sobject;
		int number = this.getNumberOfObjects();
		int[] range = new int[number];
		int checker = 0;
		for (int i = 0; i < number; i++) {
			sobject = this.listOfObjects.elementAt(i); // cast this to SearchObject from Vector
														// class...??
			if (sobject.getDimensions() != this.dimensions) {
				if (sobject.getDimensions() < this.dimensions) {
					range[i] = 1;
				}
				range[i] = -1;
			} else {
				range[i] = 0;
			}
			checker = checker + range[i];
		}
		return range;
	}

	/**
	 * This method resets the Outlier Status for all Objects in the Search room to have a clean
	 * start or to have a new identification of outliers with a separate method. As this zeros all
	 * boolean outlier statuses of all objects associated to this Searchroom and also zeros all
	 * outlier smooth factors, a current status list should be drawn down and stored somewhere
	 * before using this method.
	 * 
	 * ATTN: As this only uses references to Objects associated to a Searchroom, in case more than
	 * one Searchroom uses a (fraction) range of objects, this might override the results from other
	 * detections for those objects. But it is encouraged to associate objects to only one
	 * SearchSpace and use duplications of objects with similar vektors in other SearchRooms.
	 */
	public void resetOutlierStatus() {
		SearchObject sobject;
		for (int i = 0; i < this.getNumberOfObjects(); i++) {
			sobject = this.listOfObjects.elementAt(i); // cast this to SearchObject from Vector
														// class...??
			sobject.setOutlierStatus(false);
			sobject.setOutlierFactor(0);
		}
	}

	/**
	 * BruteForce Radius Search to determine the outlier status of an object rObject of the type
	 * SearchObject this method takes d and p as parameters acc. to distance based DB(p,D)-Outlier
	 * (Knorr, Ng) and identifies an object as being an outlier, if more than a proportion p of the
	 * objects is more than distance D from rObject away.
	 * 
	 * The simplest approach is to make a radius search for rObject and compare its distance to all
	 * other objects step by step with D (in this case d). If more than M = N(1-p) objects are
	 * within d, than rObject is not an Outlier, else it is. Although this is an approach with
	 * O(N^2) for all objects (it is O(N) for rObject), this prunes the search as soon as more than
	 * M objects are within d from rObject to get some improvement.
	 */
	public void radiusODSearch(double d, double p, SearchObject rObject, int kindOfDistance) {
		int number = this.getNumberOfObjects(); // set N (number) to number of Objects in Search
												// Room
		long m = Math.round(number * (1 - p)); // set M for Objects in Search room
		int counter = 0; // counter for objects within radius distance d

		for (int i = 0; i < number; i++) { // search through the whole list
			if (rObject.getDistance(this.listOfObjects.elementAt(i), kindOfDistance) < d) {
				counter = counter + 1; // increase counter if Object(i) is within d from rObject
				if (counter > m) {
					break; // prune if we already have more than m objects within d from rObject
				}
			}
		}

		if (counter > m) { // ok, probably not the best way, but works
			rObject.setOutlierStatus(false);
		} else {
			rObject.setOutlierStatus(true);
		}
		/*
		 * as we expect to have a radius search for all objects, we store the outlier status in
		 * rObject and after the overall search simply ask all objects whether they are thinking
		 * they are now outliers or not :-)
		 */
	}

	/**
	 * This method invokes the class method radiusODSearch on all objects in the SearchSpace
	 * (associated to this Searchroom via the listOfObjects vektor). radiusODSearch does a brute
	 * force distance Outlier test based on the parameters d and p for DB(p,d)-Outliers acc. to
	 * Knorr and Ng's approach to unify statistical Outlier tests. The result of the Outliertest is
	 * stored in the Objects themselves, e.g. each SearchObject knows its Outlier status (set
	 * recently, e.g. by this search) and can tell it by using the SearchObject's class method
	 * getOutlierStatus() (see there!)
	 * 
	 * Added feature: prints progress on STDOUT for each 10% segment (app.) one hash "#" is printed
	 * to show progress if brute force should hit complexity boundaries (e.g. with a lot of
	 * dimensions as well as lots of objects). This also prints the parameters d and p and N for
	 * better understanding
	 * 
	 */
	public void allRadiusSearch(double d, double p, int kindOfDistance) {
		int n = this.getNumberOfObjects();
		int segment = 10;
		for (int i = 0; i < n; i++) {
			this.radiusODSearch(d, p, this.listOfObjects.elementAt(i), kindOfDistance); // invoke on
																						// all
																						// objects
																						// in list
			if (100 * i / n > segment) {
				segment = segment + 10;
			}
		}
	}

	/**
	 * Returns the average distances measures for the objects in the SearchSpace, calculating:
	 * 
	 * <p>
	 * mean distance
	 * <p>
	 * standard deviation
	 * <p>
	 * variance
	 * 
	 * The calculation is time consuming and should only be invoked if the data set is parsed for
	 * the first time (to get a feeling on it for statistical choices of parameters p and d for e.g.
	 * DB(p,d)-Outliers). It parses the objects matrix upper half to build an array of distances
	 * between objects (without doubling and without the distances of objects to themselves) which
	 * should be (n^2-n)/2 distances of value.
	 * 
	 * @return double[3] of mean, variance and standard deviation
	 */
	public double[] getAverageDistanceMeasures(int kindOfDistance) {
		double meanDistance = 0; // mean distance between objects in the SearchSpace
		double standardDeviationOfDistance = 0; // standard deviation of objects in the SearchSpace
		double varianceOfDistance = 0; // variance of distance in the SearchSpace
		double distance = 0;
		double sumOfDistance = 0;
		double[] distances = new double[(this.getNumberOfObjects() * this.getNumberOfObjects() - this.getNumberOfObjects()) / 2];
		double counter = 0; // counts number of distances
		SearchObject so; // reference to a searchObject

		// first we have to calculate the mean distance between objects
		for (int i = 0; i < this.getNumberOfObjects(); i++) {
			so = this.listOfObjects.elementAt(i);
			for (int j = i; j < this.getNumberOfObjects(); j++) {
				if (i != j) {
					distance = so.getDistance(this.listOfObjects.elementAt(j), kindOfDistance);
					sumOfDistance = sumOfDistance + distance;
					distances[(int) counter] = distance;
					counter = counter + 1;
				}
			}
		}
		meanDistance = sumOfDistance / counter;

		// now lets get the variance
		for (int k = 0; k < counter; k++) {
			varianceOfDistance = varianceOfDistance + Math.pow((distances[k] - meanDistance), 2);
		}
		varianceOfDistance = varianceOfDistance / counter;
		standardDeviationOfDistance = Math.sqrt(varianceOfDistance);
		double distMeasures[] = { meanDistance, varianceOfDistance, standardDeviationOfDistance };
		return distMeasures;
	}

	/**
	 * Returns the average LOF measures for the objects in the SearchSpace, calculating:
	 * 
	 * <p>
	 * mean LOF
	 * <p>
	 * standard deviation
	 * <p>
	 * variance
	 * 
	 * 
	 * @return double[3] of mean, variance and standard deviation
	 */
	public double[] getAverageLOFMeasures() {
		double meanLOF = 0; // mean LOF of objects in the SearchSpace
		double standardDeviationOfLOF = 0; // standard deviation of Lof of objects in the
											// SearchSpace
		double varianceOfLOF = 0; // variance of LOF in the SearchSpace
		double sumOfLOF = 0; // calculation variable
		SearchObject so; // reference to a searchObject

		// first we have to calculate the mean LOF of all objects
		for (int i = 0; i < this.getNumberOfObjects(); i++) {
			so = this.listOfObjects.elementAt(i);
			sumOfLOF += so.getOutlierFactor();
		}
		meanLOF = sumOfLOF / this.getNumberOfObjects();

		// now lets get the variance
		for (int k = 0; k < this.getNumberOfObjects(); k++) {
			so = this.listOfObjects.elementAt(k);
			varianceOfLOF = varianceOfLOF + Math.pow((so.getOutlierFactor() - meanLOF), 2);
		}
		varianceOfLOF = varianceOfLOF / this.getNumberOfObjects();

		// and the standard deviation
		standardDeviationOfLOF = Math.sqrt(varianceOfLOF);
		double lofMeasures[] = { meanLOF, varianceOfLOF, standardDeviationOfLOF };
		return lofMeasures;
	}

	/**
	 * This method returns the maximum Outlier Factor of all SearchObjects in the SearchSpace. Attn:
	 * Due to initializing, the outlier factors should be greater or equal to zero.
	 */
	public double getMaximumOutlierFactor() {
		double maxOutlierFactor = 0;
		for (int i = 0; i < this.getNumberOfObjects(); i++) {
			SearchObject so = this.getObject(i);
			if (maxOutlierFactor < so.getOutlierFactor()) {
				maxOutlierFactor = so.getOutlierFactor();
			}
		}
		return maxOutlierFactor;
	}

	/**
	 * <p>
	 * This method processes a sequential search over the SearchSpace for a SearchObject so (named p
	 * here to be in line with the literature).
	 * </p>
	 * 
	 * <p>
	 * As a result of the search a structure of k-distance-Containers is build and listed within the
	 * SearchObject. Each container for a distance of an object or a number of objects o in relation
	 * to p is filled with all the objects within that distance. The containers are sorted in a
	 * linked list in the SearchObject by increasing distance. Just imagine it like p being a
	 * submarine sending a ping and listing all echos in radiuses (=distance) with the echos stored
	 * in a band (=container) if they are on the same radius.
	 * </p>
	 * 
	 * @param so
	 */
	public void findKdistanceContainers(SearchObject so, int kindOfDistance) {
		SearchObject obj; // an iterator reference for the i-th object
		double distance; // the distance between the so object and the obj object
		ListIterator<KdistanceContainer> li; // an iterator over the list of containers for so
		KdistanceContainer container; // a reference for a container out of so's container list
		int index; // index to know where we are in the list, as we use a while loop
		boolean added; // flag on whether we already added an obj to the/a container

		for (int i = 0; i < this.getNumberOfObjects(); i++) { // for all objects in the SearchSpace
			obj = this.listOfObjects.elementAt(i); // let obj be the i-th object
			if (obj == so) { // if the obj is the so-object, then do not look at it
				continue; // get to the next object i+1
			} // else do all the useful stuff

			distance = so.getDistance(obj, kindOfDistance); // the distance between so and i-th
															// object

			/**
			 * the process now should be as follows: (1) get an iterator over all kd containers of
			 * object so starting at the beginning (2) iterate over that container list until you
			 * find one (a) of equal distance (b) of greater distance (3) in case of (a) insert the
			 * obj into the container with the same distance (4) in case of (b) create a new
			 * container with that distance and add the obj into it (5) in case there has not been a
			 * container with equal distance or a container with greater distance, create a
			 * container at the end of the list.
			 * 
			 * This works, because each time we walk from left to right through the container list
			 * and add an object into an existing container / or we add it into a new container in
			 * the list or at the end, by creating a list of containers with sorted growing
			 * distances: (cont(d1), cont(d2), ... cont(dn), with d1 < d2 < ... < dn)
			 * 
			 */

			li = so.getKdContainerListIterator(); // we are getting an iterator on the container
													// list
			index = -1; // we set our counting index at element zero (after the first ++)
			added = false; // we have not yet added any obj to a container
			while (li.hasNext()) { // as long as there are containers in so's list
				container = li.next(); // take the next container from the list
				index++; // and increase the parallel indexing accordingly

				if (container.getDistance() == distance) { // if the distances are equal, do (3)
					container.addObject(obj, distance);
					added = true; // we added one obj to the container
					break; // and want to leave the while-loop
				}
				if (container.getDistance() > distance) { // if there's a container with greater
															// distance
					KdistanceContainer newcontainer = new KdistanceContainer(so); // create a new
																					// container
					so.addKdContainer(index, newcontainer); // and add him at the index point
															// (shifting the remainder of the list
															// right)
					newcontainer.addObject(obj, distance); // add the obj to the new container in
															// the list
					added = true; // we added one obj to the container
					break; // and want to leave the while loop
				}
			} // else we continue to walk through the container list with the iterator

			if (!added) { // if we have not yet added a container, one has to go to the end of the
							// list
				KdistanceContainer newcontainer = new KdistanceContainer(so); // create one
				so.addKdContainer(newcontainer); // add it at the end of so's container list
				newcontainer.addObject(obj, distance); // add the obj to the container
			} // all cases (3), (4) and (5) have either been handled

		} // continue with the for-loop and take the next object from the Searchroom
	}

	/**
	 * Finds and fills all K distance containers for all objects in the Search Room by invoking the
	 * process of finding all k distance containers for one Search Object.
	 * 
	 * @param kindOfDistance
	 * @param operator
	 *            if this is NOT <code>null</code>, will call {@link Operator#checkForStop()}.
	 * @throws ProcessStoppedException
	 *             only if the the operator parameter was not <code>null</code> and a stop request
	 *             was issued
	 */
	public void findAllKdContainers(int kindOfDistance, Operator operator) throws ProcessStoppedException {
		for (int i = 0; i < this.getNumberOfObjects(); i++) {
			if (operator != null) {
				operator.checkForStop();
			}
			this.findKdistanceContainers(this.listOfObjects.elementAt(i), kindOfDistance);
		}
	}

	/**
	 * <p>
	 * Some deeper magic to compute all the LOFs for the objects in the searchroom up to MinPtsUB =
	 * kMax! The LOF output is only done up from kMin!
	 * </p>
	 * 
	 * <p>
	 * This one is heavily documented in the source, so if you are interested on how it is done,
	 * have a look at the source for the method.
	 * </p>
	 * 
	 * @param kMin
	 * @param kMax
	 * @param operator
	 *            if this is NOT <code>null</code>, will call {@link Operator#checkForStop()}.
	 * @throws ProcessStoppedException
	 *             only if the the operator parameter was not <code>null</code> and a stop request
	 *             was issued
	 */
	public void computeLOF(int kMin, int kMax, Operator operator) throws ProcessStoppedException {
		/*
		 * What we do in this step is (1) to scan the k-distance containers for all objects to find
		 * the k-distances for that object and to store it in the object's array
		 * 
		 * (2) to compute the k-lrd for each object, we need the k-distance for each object,
		 * therefore this has to be a separate loop, looking exactly the same...
		 * 
		 * (3) to compute the k-LOFs for each object, we take the average relation of the k-lrd of
		 * the objects in p's k-neighbourhood and the k-lrd of p.
		 */
		int sumCardinality; // count up the container contents
		int k; // counter for the k-steps (e.g. finding the k-distances
		double sumdistance; // sumdistance for the r-distance summing up for lrd calculation
		double lrd; // lrd value (placeholder)
		double lof; // lof value (placeholder)

		// (1) for all objects in the search room
		for (int i = 0; i < this.getNumberOfObjects(); i++) {
			if (operator != null) {
				operator.checkForStop();
			}
			SearchObject so = this.listOfObjects.elementAt(i); // get the object p (so)

			sumCardinality = 0; // set the value to zero for each new object browse
			k = 1; // for each object start k at 1 for 1-distance

			// for this object so now browse through its containers
			ListIterator<KdistanceContainer> li = so.getKdContainerListIterator(); // first get an
																					 // iterator
																					 // over the
																// container list
			// iterate over the container list
			while (li.hasNext() && k <= kMax) { // for all containers in the list
				KdistanceContainer container = li.next(); // get the container
				sumCardinality = sumCardinality + container.getNumberOfObjects(); // add container
																					// objects to #
																					// in distance

				/*
				 * we have to find a solution to push the items in a zero-distance container
				 * (contains all objects in the same spot (which each have a zero-distance container
				 * with all the respective objects in the same spot)) to the next k-distance,
				 * because the second condition for k-distance is: at most k-1 items (not counting
				 * p) should be < distance than k-distance. For items in zero distance, this cannot
				 * be true, because only p should be in 0-distance of itself -> thus the 1-distance
				 * has to be the next distance, making the following situation: at least 1 object is
				 * <= 1-distance; at most 0 objects without p < 1-distance.
				 */
				// if (container.getDistance() != 0) {
				while (k <= sumCardinality && k <= kMax) {
					so.setKDistance(k, container.getDistance()); // the k-distance is the container
																	// distance
					so.setCardN(k, sumCardinality);
					k++; // increase k
				}
				// }
			} // all containers iterated
		} // all objects conducted

		// (2) for all objects in the SearchSpace
		for (int i = 0; i < this.getNumberOfObjects(); i++) {
			if (operator != null) {
				operator.checkForStop();
			}
			SearchObject so = this.listOfObjects.elementAt(i); // get an object

			sumCardinality = 0; // set the value to zero for each new object browse
			k = 1; // for each object start k at 1 for 1-distance
			sumdistance = 0;

			// for this object now browse again through its containers
			ListIterator<KdistanceContainer> li = so.getKdContainerListIterator();
			// first get an iterator over the container list

			// we look to compute the local k-reachability density, which is the reciprocal of
			// the average k-reachability-distance for the object in its k-neighbourhood
			// it is calculated by taking the maximum of the k-distance of each object of the
			// neighbourhood
			// and the distance between the object and the objects in the neighbourhood and
			// averaging it

			/*
			 * The good thing is, that the lrd_k(p) = 1 / ( sum_kn(p) r-distance_k(p,o) / card_k(p)
			 * ) meaning that the k-lrd is the reciprocal of the average of the k-r-distances of p's
			 * k-neighbourhood (containing all the objects o).
			 * 
			 * As the k+1 neighbourhood contains all the k-neighbourhood, we can do this in a loop
			 * and while we iterate through the loop, we only need to increase the sum of the
			 * k-r-distances and the cardinality of the neighbourhoods to sequentially calculate the
			 * k-lrds step by step.
			 * 
			 * We only have to look, that for a k-distance = k+1-distance, of course the lrd is the
			 * same and we cannot increase the bespoken numbers in this case, but just copy the lrd.
			 */

			while (li.hasNext() && k <= kMax) { // for all containers in the list until MinPtsUB is
												// reached
				KdistanceContainer container = li.next(); // get the container
				/**
				 * now that we have the container, in this container is a number of objects. We add
				 * this number to the increasing number of the sum of objects in the containers
				 * looked at so far, so that we have the number of objects in all the containers
				 * until the container with this distance in the loop, this equals the cardinality
				 * of the set of objects within k-distance for the given k in this part of the loop.
				 * We need this to get the average r-distance to compute the lrd.
				 */
				sumCardinality = sumCardinality + container.getNumberOfObjects(); // ok, now
																					// increase the
																					// cardinality

				/*
				 * now we look into the container and for each object o in the container, we choose
				 * the reachability-distance. This is the maximum of the k-distance of o (we get
				 * this by asking the object of its k-distance using the k iteration value from the
				 * loop) and the actual distance between so and o. We get this from the container,
				 * as all o's in the container have container's distance to so (so we do not need to
				 * compute it again, which can be time consuming depending on the dimensions of the
				 * objects).
				 * 
				 * Afterwards we add the l-reachability distance of the object o to the sumdistance.
				 * 
				 * As last step, we calculate the lrd by using the cardinality of all objects in
				 * k-neighbour- hood for so (sumCardinality) as a divisor to the sum of reachability
				 * distances. Of this we take the reciprocal and store it in lrd_k for so.
				 */
				boolean calcLRD = false; // in each container we want to compute the lrd, so reset
				// the trigger on whether we already have the lrd, we don't (yet)
				lrd = 0; // initialize with zero to be sure (we can than see mistakes)
				while (k <= sumCardinality && k <= kMax) { // of course, we stop as we reach
															// MinPtsUB

					// as the lrd_k is the same for all k-distances with the same objects, we only
					// need compute once
					if (!calcLRD) {
						ListIterator<SearchObject> lobj = container.getListIterator(); // get an
																						 // iterator
																						 // for the
																			// container
						while (lobj.hasNext()) { // and iterate over it
							SearchObject sobj = lobj.next(); // get the object o
																			// (sobj)
							// now increase the sum of reachability distances with the rd of sobj
							sumdistance = sumdistance + Math.max(container.getDistance(), sobj.getKDistance(k));
						}
						lrd = 1 / (sumdistance / sumCardinality);
						calcLRD = true; // set, that we now have an lrd calculated, so do not do it
										// again
					}

					so.setLRD(k, lrd); // and can here set it for the k-distance (k)

					k++; // increase k to the next distance
				} // now we have sorted through all steps in k-distances which can be made with one
					// container
					// as we have to remember, that k-distance can be k+1 distance in some cases,
					// etc...
			} // now we have finished with the container
		}

		// (3) for all objects in the search room
		for (int i = 0; i < this.getNumberOfObjects(); i++) {
			if (operator != null) {
				operator.checkForStop();
			}
			SearchObject so = this.listOfObjects.elementAt(i); // get the object p (so)

			sumCardinality = 0; // set the value to zero for each new object browse
			k = 1; // for each object start k at 1 for 1-distance
			// sumlrdrelations = 0; // set the sum of the lrd(o)/lrd(p) to zero

			double[] sumlrdratio = new double[kMax + 1]; // store all growing sumlrd ratios in this
															// array
			for (int u = 0; u <= kMax; u++) {
				sumlrdratio[u] = 0;
			}

			// for this object so now browse through its containers
			ListIterator<KdistanceContainer> li = so.getKdContainerListIterator(); // first get an
																					 // iterator
																					 // over the
																// container list
			// iterate over the container list
			while (li.hasNext() && k <= kMax) { // for all containers in the list
				KdistanceContainer container = li.next(); // get the container
				sumCardinality = sumCardinality + container.getNumberOfObjects(); // add container
																					// objects to #
																					// in distance

				boolean calcLOF = false; // for each container's object list calculate the LOF only
											// once, not yet calc'ed
				lof = 0; // set lof to zero for the time being

				while (k <= sumCardinality && k <= kMax) {

					if (!calcLOF) { // if we haven't calculated the LOF yet, we should do it

						ListIterator<SearchObject> lobj = container.getListIterator(); // get an
																						 // iterator
																						 // over the
																			// container
						while (lobj.hasNext()) {
							SearchObject sobj = lobj.next(); // get the next object
																			// from the container
							for (int j = 1; j <= kMax; j++) { // explaination for this see below...
								double lrd2 = so.getLRD(j);
								double lrd3 = sobj.getLRD(j);
								if (!(Double.isInfinite(lrd2) || Double.isInfinite(lrd3))) {
									// for a huge number of duplicates the k-lrd becomes infinite.
									// In this case we need to skip the sum-step because it is
									// mathematically undefined.
									sumlrdratio[j] = sumlrdratio[j] + lrd3 / lrd2;
								}
							}
							// sumlrdrelations = sumlrdrelations + sobj.getlrd(k)/so.getlrd(k);
							// this has been taken out, because it has been wrong approach
							// left in as a remembering
						}

						// lof = sumlrdrelations / sumCardinality;
						// this has been taken out, because it has been wrong approach
						// left in as a remembering

						/*
						 * This has been changed, because we need to take the respective lrds for
						 * all the objects in the MinPts-neighbourhood, but with the _MinPts_ index
						 * for _all_ and not step by step growing indices. Hence we compute the
						 * lrd_upsumming relations (lrd_MinPts(o)/lrd_MinPts(p)) in the growing loop
						 * for all MinPts's and store them in sumlrdratio[MinPts] and take the LOF
						 * from that by dividing through |N_MinPts(p)| cardinality (which is the
						 * step by step summed up from the containers.
						 */
						lof = sumlrdratio[k] / sumCardinality;
						calcLOF = true;
					}

					so.setLOF(k, lof); // set the k-LOF for so to lof (we keep the k-LOFs, to
										// analyse e.g.
					if (k >= kMin && so.getOutlierFactor() <= lof) {
						so.setOutlierFactor(lof); // if this k-LOF is maximal, set ooutlier status
													// to this...
						// but only take those into account for k-dists > kMin!
					}
					k++; // increase k
				}
			} // all containers iterated
		} // all objects conducted
	}

	/**
	 * This function computes the D^k_n Outliers according to Ramaswamy, Rastogi and Shim which
	 * computes the top-n D^k-Outliers, the outliers (= objects) with the maximum distance to the
	 * k-th nearest neighbors.
	 * 
	 * Please be aware that this function requires the findAllKdContainers method has to be run
	 * first, else it will simply stop or will not work.
	 * 
	 * @param dk
	 * @param n
	 * @param operator
	 *            if this is NOT <code>null</code>, will call {@link Operator#checkForStop()}.
	 * @throws ProcessStoppedException
	 *             only if the the operator parameter was not <code>null</code> and a stop request
	 *             was issued
	 */
	public void computeDKN(int dk, int n, Operator operator) throws ProcessStoppedException {
		Vector<SearchObject> listofDKNcandidates = new Vector<>();
		int minDKNdistindex = 0;
		double minD = 0;
		int sumCardinality;
		int k;
		int kMax = dk; // do not look for k-distances over k
		double minDistInList = 0; // the smallest distance in the candidates list

		/*
		 * This has three steps: (1) get the k-distances from the containerinformation (like in LOF)
		 * and store it in the SearchObjects (2) browse through all the SearchObjects in the room
		 * and sort those with the max dk-distance into the candidates (3) push the information in
		 * the candidates list into the SearchObjects Outlier status variables (fields)
		 */

		/*
		 * First (like in LOF algorithm), get the real k-distances from the containers and store the
		 * information in the kdistance-Vektor of each SearchObject.
		 */
		for (int i = 0; i < this.getNumberOfObjects(); i++) {
			if (operator != null) {
				operator.checkForStop();
			}
			SearchObject so = this.listOfObjects.elementAt(i); // get the object p (so)

			sumCardinality = 0; // set the value to zero for each new object browse
			k = 1; // for each object start k at 1 for 1-distance

			// for this object so now browse through its containers
			ListIterator<KdistanceContainer> li = so.getKdContainerListIterator();
			// first get an iterator over the container list
			// iterate over the container list
			while (li.hasNext() && k <= kMax) { // for all containers in the list
				KdistanceContainer container = li.next(); // get the container
				sumCardinality = sumCardinality + container.getNumberOfObjects(); // add container
																					// objects to #
																					// in distance

				/*
				 * we have to find a solution to push the items in a zero-distance container
				 * (contains all objects in the same spot (which each have a zero-distance container
				 * with all the respective objects in the same spot)) to the next k-distance,
				 * because the second condition for k-distance is: at most k-1 items (not counting
				 * p) should be < distance than k-distance. For items in zero distance, this cannot
				 * be true, because only p should be in 0-distance of itself -> thus the 1-distance
				 * has to be the next distance, making the following situation: at least 1 object is
				 * <= 1-distance; at most 0 objects without p < 1-distance.
				 */
				// if (container.getDistance() != 0) {
				while (k <= sumCardinality && k <= kMax) {
					so.setKDistance(k, container.getDistance()); // the k-distance is the container
																	// distance
					so.setCardN(k, sumCardinality);
					k++; // increase k
				}
				// }
			} // all containers iterated
		} // all objects conducted

		/*
		 * In the second step, get the actual list of DKN candidates from the k-distances of all the
		 * SearchObjects.
		 */
		for (int i = 0; i < this.getNumberOfObjects(); i++) {
			if (operator != null) {
				operator.checkForStop();
			}
			// get the next SearchObject
			SearchObject so = this.listOfObjects.elementAt(i);
			// 1. if the candidates list is empty, simply add the element to the candidates list
			if (listofDKNcandidates.size() == 0) {
				listofDKNcandidates.add(so); // add the candidate to the list
			} else {
				// 2.1 if there are already elements in the list, check if it is no more than n
				// elements
				if (listofDKNcandidates.size() <= n + 1) {
					listofDKNcandidates.add(so); // add the candidate to the list
				} else {
					// 2.2 if the list is already full and only if the new candidate has more
					// distance
					if (so.getKDistance(dk) > minDistInList) {
						listofDKNcandidates.remove(minDKNdistindex); // remove the candidate with
																		// minimal distance in list
						listofDKNcandidates.add(so); // add the new candidate to the list
					}
				}
			}

			// 3. iterate through the candidates list to find the actual smallest distance and the
			// respective index
			for (int j = 0; j < listofDKNcandidates.size(); j++) {
				SearchObject sobj = listofDKNcandidates.elementAt(j); // get the reference to the
																		// candidate
				minD = sobj.getKDistance(dk); // set minD to candidates' distance
				if (j == 0) { // if first in list, simply initialize with candidates values
					minDistInList = minD; // for minimal distance
					minDKNdistindex = j; // and the index of the minimal distance
				} else { // if not first in list, we have initialized data to compare with
					if (minDistInList > minD) { // if actual candidate's distance is smaller
						minDistInList = minD; // set this to be the new minimal distance
						minDKNdistindex = j; // and set the index of that minimal distance
					}
				}
			} // now we know what the minDistInList is and have the new minDKNdistindex

		} // now we get the next SearchObject of the for loop (see above)

		/*
		 * In the last step, mark all SearchObjects in the top-n List as the Outliers. Maybe later
		 * enhance by sorting and rank the top-n...
		 */
		for (int z = 0; z < listofDKNcandidates.size(); z++) {
			SearchObject sobj2 = listofDKNcandidates.elementAt(z);
			sobj2.setOutlierStatus(true);
		}
	} // end of computeDKN method
}
