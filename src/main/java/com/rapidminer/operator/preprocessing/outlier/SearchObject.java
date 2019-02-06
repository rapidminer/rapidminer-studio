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

import java.util.LinkedList;
import java.util.List;
import java.util.ListIterator;


/**
 * The SearchObject class creates SearchObjects which handle the representation of objects from the
 * test data set in the core of the outlier operators. Such an object is able to store all relevant
 * coordinates, dimensions, etc. for an object (e.g. from an Example from a RapidMiner ExampleSet)
 * as well as perform various operations, such as radius search to other objects.
 * 
 * @author Stephan Deutsch, Ingo Mierswa
 */
public class SearchObject {

	/**
	 * Number of dimensions of the <tt>SearchObject</tt> as an internal integer value
	 */
	private int dimensions;

	/**
	 * The vector of the Object (e.g. its vector in the common sense and not a JAVA language Vector
	 * class) with (double) value array [0,...dimensions] for the value of each dimension for the
	 * vektor.
	 */
	private double vector[];

	/**
	 * The boolean Outlier status of the Object it holds after a yes/no state Outlier test has been
	 * conducted.
	 */
	private boolean outlierStatus;

	/**
	 * The Outlier factor as a double value in case the Object shall know the results of non-trivial
	 * state outlier tests (such as LOF, CBLOF, etc.)
	 */
	private double outlierFactor;

	/**
	 * The label of the object to differentiate it in further analysis.
	 */
	private String label;

	/**
	 * <p>
	 * List (Linked List) of k-distance object containers for an object. In this list, in ascending
	 * order, the subsets of objects are stored, which are in the same distance from the
	 * SearchObject.
	 * </p>
	 * 
	 * <p>
	 * This is (sort of) representing the objects on a radius around the SearchObject and objects at
	 * the same difference (on the same radius) are stored in the same container. Thsi data
	 * structure is very important to compute the actual k-distance neighbourhoods afterwards, as
	 * this neighbourhoods require a certain (at least) number of objects to be within a given
	 * distance and a certain smaller (= at most) number of objects to be within a smaller distance.
	 * </p>
	 */
	private List<KdistanceContainer> listOfkDContainers;

	/**
	 * The array of all k-distances of an object, e.g. kDistance[2] represents the 2-distance, thus
	 * the array needs to be initialized with an n+1 dimension, as the 0-distance is not used and
	 * Java counts arrays from 0...n-1 for n-dimensional arrays.
	 */
	private double[] kDistance;

	/**
	 * The array of all local reachability densities of an object, e.g. lrd[2] represents the
	 * 2-density, thus the array needs to be initialized with an n+1 dimension, as the 0-distance is
	 * not used and Java counts arrays from 0...n-1 for n-dimensional arrays.
	 */
	private double[] lrd;

	/**
	 * The array of all cardinalities of the k-Neighbourhoods of an object, e.g. cardN[2] represents
	 * the number of objects in the 2-Neighbourhood (|N_k(p)|), thus the array needs to be
	 * initialized with an n+1 dimension, as the 0-distance is not used and Java counts arrays from
	 * 0...n-1 for n-dimensional arrays.
	 */
	private int[] cardN;

	/**
	 * <p>
	 * The array of all LOFs of an object for MinPts=k, e.g. localOutlierFactor[3] represents the
	 * LOF for MinPts=3, thus the array needs to be initialized with an n+1 dimension, as the
	 * 0-distance is not used and Java counts arrays from 0...n-1 for n-dimensional arrays.
	 * </p>
	 * 
	 * <p>
	 * Please be aware, that usually for the MinPts-LOF check, the maximum LOF will be choosen for
	 * all LOF[MinPts] between MinPtsLowerBound and MinPtsUpperBound. This value will be stored in
	 * the SearchObjects OutlierFactor variable, as there's already some methods to automatically
	 * print those.
	 * </p>
	 */
	private double[] localOutlierFactor;

	/**
	 * A lower bound for MinPts for the SearchObject.
	 */
	// private int minPtsLowerBound;

	/**
	 * An upper bound for MinPts for the SearchObject.
	 */
	private int minPtsUpperBound;

	/**
	 * Constructor creates a new instance of <tt>SearchObject</tt> class and initializes the object
	 * with integer <i>dim</i> dimensions and the String label <i>l</i>. Each dimension vektor is
	 * set to (double) ZERO and Outlier status is set to false and Outlier Factor is set to ZERO as
	 * well.
	 */
	public SearchObject(int dim, String l) {
		this.dimensions = dim; // set dimensions to dim
		this.vector = new double[this.dimensions]; // construct a vector of floats with dimension
													// dim
		for (int i = 0; i < dim; i++) { // fill that vector with zero's to be sure there's no funny
										// numbers in it later
			this.vector[i] = 0;
		}
		this.setOutlierStatus(false); // as long as we do not know, this is not an outlier
		this.setOutlierFactor(0); // hence it also gets an Outlier factor of zero
		this.label = l;
		this.listOfkDContainers = new LinkedList<KdistanceContainer>(); // create a new list for the
																		// kdContainers
	}

	/**
	 * Constructor creates a new instance of <tt>SearchObject</tt> class and initializes the object
	 * with integer <i>2</i> dimensions and the String label <i>not labeled object</i>. Each
	 * dimension vektor is set to (double) ZERO and Outlier status is set to false and Outlier
	 * Factor is set to ZERO as well. this is only a default constructor and should not be used for
	 * 2-dimensional objects. The class does not provide sufficient consistency checks to entirely
	 * rely on default construction.
	 */
	public SearchObject() {
		this(2, "not labeled object");
	}

	/**
	 * Constructor creates a new instance of <tt>SearchObject</tt> class and initializes the object
	 * with integer <i>dim</i> dimensions and the String label <i>l</i> and an (integer)
	 * MinPts-Range. Each dimension vektor is set to (double) ZERO and Outlier status is set to
	 * false and Outlier Factor is set to ZERO as well.
	 * 
	 * @param dim
	 * @param l
	 * @param minptslb
	 * @param minptsub
	 */
	public SearchObject(int dim, String l, int minptslb, int minptsub) {
		this(dim, l); // first create the object with dim and label using that constructor
		this.cardN = new int[minptsub + 1];
		this.kDistance = new double[minptsub + 1];
		this.lrd = new double[minptsub + 1];
		this.localOutlierFactor = new double[minptsub + 1];
		// this.minPtsLowerBound = minptslb;
		this.minPtsUpperBound = minptsub;

		// the index in the future use will be 1,... <n+1 ! but we initialize the zero index as well
		for (int i = 0; i < this.minPtsUpperBound + 1; i++) { // initialize all with zero to be
																// sure...
			this.cardN[i] = 0;
			this.kDistance[i] = 0;
			this.lrd[i] = 0;
			this.localOutlierFactor[i] = 0;
		}
	}

	/**
	 * <p>
	 * Changes the number of dimensions for an object and copies the values of the old vector for
	 * the object into the new vektor (which is initialized with the new dimension number).
	 * </p>
	 * 
	 * <p>
	 * <em>Attention</em>: If the new dimension number is less than the old number, only the values
	 * of the relevant new domain range are copied. If the new vector has more dimensions, all the
	 * old are copied and the new ones are initialized with ZERO. Those should afterwards be
	 * initialized with the {@link #setVektor(int, double)} method in a proper manner.
	 * </p>
	 * 
	 * <p>
	 * The safest way to change the dimensions of an object is to create a new one with the new
	 * dimensions and to copy the vektor values and all other relevant data and to initialize the
	 * additional dimensions with the proper values.
	 * </p>
	 */
	public void setDimensions(int dim) {
		double[] changeVektor = new double[this.dimensions]; // create a new vektor to hold the
																// existing one
		int oldDimensions = this.dimensions; // store the old number of dimensions
		this.dimensions = dim; // set the number of dimensions for this object to new dim value
		for (int j = 0; j < oldDimensions; j++) {
			changeVektor[j] = this.vector[j]; // store all the old vektor values in changeVektor
		}
		this.vector = new double[this.dimensions]; // create a new this.vektor with the new
													// dimensions
		for (int i = 0; i < this.dimensions; i++) {
			if (i < oldDimensions) { // as long as it is within old dimension range, copy value
				this.vector[i] = changeVektor[i];
			} else {
				this.vector[i] = 0; // else initialize with ZERO
			}
		} // as you can see from the loop, if new vector has less dimensions, only the relevant are
			// copied
	}

	/**
	 * Provides the (integer) number of dimensions of the Object. Remark: some methods actually use
	 * the this.dimensions reference which is used by this, but this method would be able to provide
	 * the dimensions externally.
	 */
	public int getDimensions() {
		return (this.dimensions);
	}

	/**
	 * Sets the label of the object to (String) <i>l</i>.
	 */
	public void setLabel(String l) {
		this.label = l;
	}

	/**
	 * Returns the label of the object (e.g. its "name" for other purposes)
	 */
	public String getLabel() {
		return (this.label);
	}

	/**
	 * Sets the vector for the object to (double) <i>value</i> for the dimension (integer)
	 * <i>dim</i>, with this method subsequently all dimensions of an objects vector can be set.
	 * 
	 * @param dim
	 * @param value
	 */
	public void setVektor(int dim, double value) {
		this.vector[dim] = value;
	}

	/**
	 * Returns the value of the object's vektor with dimension (integer) <i>dim</i>.
	 * 
	 * @param dim
	 */
	public double getVektor(int dim) {
		return (this.vector[dim]);
	}

	/**
	 * Sets a BOOLEAN Outlier Status for the object to store the results of Outlier tests according
	 * to a yes/no Outlier state (e.g. DB(p,D) Outliers and others.
	 * 
	 * @param status
	 */
	public void setOutlierStatus(boolean status) {
		this.outlierStatus = status;
	}

	/**
	 * Provides the BOOLEAN Outlier status of an Object (-> the status has to be set through a test,
	 * so the user should see that the status is only set by methods providing a consistent view on
	 * the outlier test, else this has only the meaning of the accidentally stored status (default
	 * should be ZERO ;-).
	 */
	public boolean getOutlierStatus() {
		return (this.outlierStatus);
	}

	/**
	 * Sets a (double) Outlier <i>factor</i> to store smooth Outlier status information, such as
	 * local outlier factors and others.
	 * 
	 * @param factor
	 */
	public void setOutlierFactor(double factor) {
		this.outlierFactor = factor;
	}

	/**
	 * Returns the Outlier factor of an object.
	 */
	public double getOutlierFactor() {
		return (this.outlierFactor);
	}

	/**
	 * <p>
	 * Returns the euclidian (metric) distance between two SearchObjects by looking at the object's
	 * vektors and returning the length of the substracted vector between the two object's vectors.
	 * </p>
	 * 
	 * <p>
	 * The method checks if both objects have the same dimensions and for ensuring smooth program
	 * execution takes the mimimum number of dimensions of the two objects. So it looks at a higher
	 * dimensional object as if it has only as many dimensions as the object with fewer
	 * dimensionality. ATTENTION: This - of course - creates different distance as if the object
	 * with maximum dimensions would be taken as the reference and the missing dimensions of the
	 * object with fewer dimensions would be set to zero.
	 * </p>
	 * 
	 * <p>
	 * It would be expected that an integrity check would be performed before using the distance
	 * functions from any functions utilizing this distance. E.g.
	 * {@link SearchSpace#dimensionsIntegrityCheck()} provides such an integrity check for a search
	 * room's dimensions (although that function does not check object to object integrity
	 * separately).
	 * </p>
	 */
	public double getDistanceEuclidian(SearchObject toObject) {
		double distance = 0;
		int dim_of_toObject = toObject.getDimensions();
		int minimumDimensions = 0;
		minimumDimensions = Math.min(this.dimensions, dim_of_toObject); // if both are equal, we can
																		// take the equal min
		for (int i = 0; i < minimumDimensions; i++) {
			distance = distance + Math.pow((this.getVektor(i) - toObject.getVektor(i)), 2);
		}
		return (Math.sqrt(distance));
	}

	/**
	 * This method returns the distance between two objects according to a specification on which
	 * distance shall be computed (at the moment the method supports EUCLIDIAN distance (int
	 * kindOfDistance = 1) and COSINE distance (int kindOfDistance = 2) and the following similar
	 * distances: SQUARED (0) (the squared value of the metric/euclidian distance, INV_COSINE (3)
	 * the inversted cosine (actually the sine) distance which is simply 1-cos, and ANGLE_RADIANT
	 * (4) the angle between the objects related to zero coordinates in the actual n-dimensional
	 * euclidian coordinate system (ARC COSINE in radiant between [0 ; pi]).
	 * 
	 * <p>
	 * The method substitutes the distance method
	 * 
	 * @link #getDistance(SearchObject) which is only capable to compute the EUCLIDIAN distance.
	 * 
	 *       <p>
	 *       The parameter (int) kindOfDistance defines the kind of distance to compute, Attn.: If
	 *       no kind of distance is specified properly, EUCLIDIAN is set as a default to prevent
	 *       malfunction. A Warning is printed to STDOUT accordingly.
	 * 
	 *       <p>
	 *       The first parameter, however, as in the older getDistance function, is the SearchObject
	 *       to which the distances is to be measured.
	 * 
	 *       <p>
	 *       For further information: The difference between EUCLIDIAN distance and COSINE distance
	 *       is as follows:
	 *       <p>
	 *       d_euclidian(X,Y)=SQUARE_ROOT(SUM_i((x_i - y_i)^2)) and
	 *       <p>
	 *       d_cosine(X,Y)=SUM_i(x_i * y_i) / (SQUARE_ROOT(SUM_i(x_i)) * SQUARE_ROOT(SUM_i(y_i)))
	 *       <p>
	 *       Or in other words, while euclidian distance is measuring the metric distance between
	 *       two vectors equalling the norm of the subtraction of the two vectors, the cosine
	 *       distance is measuring the cosine of the angle between the two vectors. The cosine
	 *       distance is used especially for measuring the similarity between texts represented by
	 *       their vectorized term structure (e.g. using Term Frequency or Inverse Term Frequency -
	 *       TF/IDF) for the purpose of Information Retreival.
	 *       <p>
	 *       inverted cosine distance is supported by computing 1-cos distance, as with cosine in
	 *       the interval between [1; 1/2*pi] is monotonic and falling, from [1;0] and the largest
	 *       angles actually have the smallest value, it might very well be useful to invert the
	 *       scala to sine distance (1-cos distance) for reflecting increasing angles resulting in
	 *       increasing values for the distance used. Attn: the effect in this case decellerates,
	 *       e.g. the larger angles have less difference in distance values, hence a grouping of
	 *       objects kind of explodes in the middle and gets denser in the outer ring.
	 *       <p>
	 *       Therefore, in addition, the actual angle in radiant is introduced. With this kind of
	 *       distance, the direct angle between obejects is used, resulting in a linear monotonic
	 *       growing distance representation.
	 *       <p>
	 *       Overall, the user should decide on which kind of distance is to be used depending on
	 *       the actual application, as some distance measures can have VERY funny effects is used
	 *       in the wrong way.
	 * 
	 * @param toObject
	 * @param kindOfDistance
	 */
	public double getDistance(SearchObject toObject, int kindOfDistance) {
		double distance = 0;
		int SQUARED = 1; // squared value of the euclidian distance will be used
		int EUCLIDIAN = 0; // euclidian (metric) distance will be used
		int COSINE = 2; // cosine distance will be used
		int INV_COSINE = 3; // 1-cos distance will be used
		int ANGLE_RADIANT = 4; // the angle in radiant will be used

		// check if the distance modifier is properly set, if not, fall back to euclidian as default
		// and log to STDOUT
		if (kindOfDistance != COSINE && kindOfDistance != SQUARED && kindOfDistance != INV_COSINE
				&& kindOfDistance != ANGLE_RADIANT) {
			if (kindOfDistance != EUCLIDIAN) {
				kindOfDistance = EUCLIDIAN;
			}
		}

		// check, if the dimensions of the objects are ok (the same) and computation can go ahead,
		// else fix this first
		int dim_of_toObject = toObject.getDimensions();
		int minimumDimensions = 0;
		minimumDimensions = Math.min(this.dimensions, dim_of_toObject); // if both are equal, we can
																		// take the equal min

		// if the euclidian distance is sought for, compute and return
		if (kindOfDistance == EUCLIDIAN || kindOfDistance == SQUARED) {
			for (int i = 0; i < minimumDimensions; i++) {
				distance = distance + Math.pow((this.getVektor(i) - toObject.getVektor(i)), 2);
			}
			// if distance is squared, simply return the distance value, else for euclidian return
			// the square-root value
			return (kindOfDistance == SQUARED ? distance : Math.sqrt(distance));
		}

		/*
		 * else, we assume that cosine distance or inverted cosine distance or angle in radiant is
		 * sought for and compute this and return
		 */
		double sumOfProductsxiyi = 0;
		double sumxisquared = 0;
		double sumyisquared = 0;

		for (int i = 0; i < minimumDimensions; i++) {
			sumOfProductsxiyi = sumOfProductsxiyi + (this.getVektor(i) * toObject.getVektor(i));
			sumxisquared = sumxisquared + Math.pow(this.getVektor(i), 2);
			sumyisquared = sumyisquared + Math.pow(toObject.getVektor(i), 2);
		}

		distance = sumOfProductsxiyi / (Math.sqrt(sumxisquared) * Math.sqrt(sumyisquared));

		if (kindOfDistance == COSINE) {
			return distance; // if COSINE, simply return the computed cosine distance
		} else {
			if (kindOfDistance == INV_COSINE) {
				return 1 - distance; // if inverted COSINE, return 1-cos (equals sin) of the cosine
										// distance
			} else {
				return Math.acos(distance); // if the Angle is looked for, return it using arcus
											// cosine function
				// according to JAVA Math-Class documentation, the result of acos() is in radiant!
			}
		}

	}

	/**
	 * Adds a new KdContainer to the SearchObject at index in the container list.
	 * 
	 */
	public void addKdContainer(int index) {
		KdistanceContainer container = new KdistanceContainer(this);
		this.listOfkDContainers.add(index, container);
	}

	/**
	 * Adds a new KdContainer to the SearchObject at index in the container list and also sets the
	 * distance value of the container to dist.
	 * 
	 */
	public void addKdContainer(int index, double dist) {
		KdistanceContainer container = new KdistanceContainer(this);
		container.setDistance(dist);
		this.listOfkDContainers.add(index, container);
	}

	/**
	 * Adds an existing KdContainer to the container list at position index.
	 * 
	 * @param index
	 * @param kd
	 */
	public void addKdContainer(int index, KdistanceContainer kd) {
		this.listOfkDContainers.add(index, kd);
	}

	/**
	 * Adds an existing KdContainer to the container lost at the end of the list.
	 * 
	 * @param kd
	 */
	public void addKdContainer(KdistanceContainer kd) {
		this.listOfkDContainers.add(kd);
	}

	/**
	 * Adds a new KdContainer to the SearchObject at the end of the container list.
	 * 
	 */
	public void addKdContainer() {
		KdistanceContainer container = new KdistanceContainer(this);
		this.listOfkDContainers.add(container);
	}

	/**
	 * returns a ListIterator for the list of containers in the SearchObject.
	 */
	public ListIterator<KdistanceContainer> getKdContainerListIterator() {
		return listOfkDContainers.listIterator();
	}

	/**
	 * Sets the k-distance for the SearchObject for k to dist.
	 * 
	 * @param k
	 * @param dist
	 */
	public void setKDistance(int k, double dist) {
		this.kDistance[k] = dist;
	}

	/**
	 * Returns the k-distance for the SearchObject for k.
	 * 
	 * @param k
	 */
	public double getKDistance(int k) {
		return this.kDistance[k];
	}

	/**
	 * Sets the local reachability density for k for a SearchObject for k to lrdvalue.
	 * 
	 * @param k
	 * @param lrdvalue
	 */
	public void setLRD(int k, double lrdvalue) {
		this.lrd[k] = lrdvalue;
	}

	/**
	 * Returns the local reachability density for k for a SearchObject.
	 * 
	 * @param k
	 */
	public double getLRD(int k) {
		return this.lrd[k];
	}

	/**
	 * Sets the cardinality for k-neighbourhood (|N_k(p)|) for a SearchObject for k to card.
	 * 
	 * @param k
	 * @param card
	 */
	public void setCardN(int k, int card) {
		this.cardN[k] = card;
	}

	/**
	 * Returns the cardinality for k-neighbourhood (|N_k(p)|) for a SearchObject for k.
	 * 
	 * @param k
	 */
	public int getCardN(int k) {
		return this.cardN[k];
	}

	/**
	 * Sets the k-LOF for a SearchObject to lof for k.
	 * 
	 * @param k
	 * @param lof
	 */
	public void setLOF(int k, double lof) {
		this.localOutlierFactor[k] = lof;
	}

	/**
	 * Returns the k-LOF for a SearchObject for k.
	 * 
	 * @param k
	 */
	public double getLOF(int k) {
		return this.localOutlierFactor[k];
	}
}
