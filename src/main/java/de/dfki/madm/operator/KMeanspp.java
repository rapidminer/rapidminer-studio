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
package de.dfki.madm.operator;

import com.rapidminer.example.Example;
import com.rapidminer.example.ExampleSet;
import com.rapidminer.operator.Operator;
import com.rapidminer.operator.OperatorDescription;
import com.rapidminer.operator.OperatorException;
import com.rapidminer.operator.OperatorVersion;
import com.rapidminer.operator.ProcessStoppedException;
import com.rapidminer.operator.clustering.ClusterModel;
import com.rapidminer.operator.clustering.clusterer.RMAbstractClusterer;
import com.rapidminer.tools.RandomGenerator;
import com.rapidminer.tools.math.similarity.DistanceMeasure;


/**
 * This algorithm is the first part of K-Means++ described in the paper "k-means++: The Advantages of
 * Careful Seeding" by David Arthur and Sergei Vassilvitskii
 *
 * @author Patrick Kalka
 */
public class KMeanspp extends RMAbstractClusterer {

	/**
	 * Short description for GUI
	 */
	public static final String SHORT_DESCRIPTION = "Determine the first k centroids using the K-Means++ heuristic described in \"k-means++: The Advantages of Careful Seeding\" by David Arthur and Sergei Vassilvitskii 2007";

	/**
	 * Label for button
	 */
	public static final String PARAMETER_USE_KPP = "determine_good_start_values";

	/**
	 * KMeans++ (Use good start values) was not working before 9.0.2
	 */
	public static final OperatorVersion VERSION_KPP_NOT_WORKING = new OperatorVersion(9, 0, 1);

	/**
	 * the calling operator
	 */
	private Operator caller = this;

	/**
	 * ExampleSet to work on
	 */
	private ExampleSet exampleSet = null;

	/**
	 * DistanceMeasure to use
	 */
	private DistanceMeasure measure = null;

	private RandomGenerator generator = null;
	private int examplesize = -1;
	private int minK = 0;

	/**
	 * Initialization of K-Means++
	 *
	 * @param parent
	 * 		The calling operator
	 * @param anz
	 * 		Initial Cluster count
	 * @param es
	 * 		ExampleSet to work on
	 * @param measure
	 * 		DistanceMeasure to use
	 * @throws OperatorException
	 */
	public KMeanspp(Operator parent, int anz, ExampleSet es, DistanceMeasure measure,
					RandomGenerator generator) throws OperatorException {
		this(parent.getOperatorDescription(), anz, es, measure, generator);
		caller = parent;
	}

	/**
	 * Initialization of K-Means++
	 *
	 * @param description
	 * @param anz
	 * 		initial Cluster count
	 * @param es
	 * 		ExampleSet to work on
	 * @param measure
	 * 		DistanceMeasure to use
	 * @throws OperatorException
	 * @deprecated use {@link KMeanspp(Operator,int,ExampleSet,DistanceMeasure,RandomGenerator) instead
	 */
	@Deprecated
	public KMeanspp(OperatorDescription description, int anz, ExampleSet es, DistanceMeasure measure,
					RandomGenerator generator) throws OperatorException {
		super(description);

		this.minK = anz;
		this.exampleSet = es;
		this.examplesize = es.size();
		this.measure = measure;
		this.generator = generator;
	}

	/**
	 * start the algorithm
	 *
	 * @return array with Ids of the centroids
	 * @throws ProcessStoppedException
	 */
	public int[] getStart() throws ProcessStoppedException {
		boolean additionalCheckForPossibleInfiniteLoop = caller.getCompatibilityLevel().isAbove(VERSION_KPP_NOT_WORKING);
		int[] ret = new int[minK];
		int i = 0;
		int anz = 0;

		// take the first Centroid at random
		for (Integer index : generator.nextIntSetWithRange(0, exampleSet.size(), 1)) {
			ret[anz] = index;
			anz++;
			i = index;
		}

		while (anz < minK) {
			caller.checkForStop();

			do {
				caller.checkForStop();
				double[] shortest = new double[examplesize];
				double maxProb = -1;
				int maxProbId = -1;
				double distSum = 0;
				// sum of shortest path between chosen centroids and all Points
				for (int j = 0; j < examplesize; j++) {
					double minDist = -1;
					Example ex = exampleSet.getExample(j);
					for (Integer id : ret) {
						double dist = measure.calculateDistance(ex, exampleSet.getExample(id));
						// if the distance can not be calculated it returns NaN
						if (!Double.isNaN(dist) && (minDist < 0 || minDist > dist)) {
							minDist = dist;
						}
					}
					distSum += minDist;
					shortest[j] = minDist;
				}

				// get maximal Probability
				for (int j = 0; j < examplesize; j++) {
					double prob = Math.pow(shortest[j], 2) / Math.pow(distSum, 2);
					// 0/0 = Double.NaN
					if (Double.isNaN(prob)) {
						prob = 0;
					}
					if (prob > maxProb && (!additionalCheckForPossibleInfiniteLoop || !contains(ret, j, anz))) {
						maxProbId = j;
						maxProb = prob;
					}
				}
				i = maxProbId;
			} while (contains(ret, i, anz));
			ret[anz] = i;
			anz++;
		}

		return ret;
	}

	/**
	 * Check if the parameter searchValue is an element of the ins array. Only check the first elements up to maxIndex - 1
	 *
	 * @param ints
	 * 		array of int elements to be checked for containment of the searchValue parameter
	 * @param searchValue
	 * 		check if this element exists in the ints array
	 * @param maxIndex
	 * 		check only the elements 0..maxIndex of the ints array
	 * @return true if searchValue was found, false if searchValue was not found in the ints array
	 */
	private static boolean contains(int[] ints, int searchValue, int maxIndex) {
		for (int i = 0; i < Math.min(maxIndex, ints.length); i++) {
			if (ints[i] == searchValue) {
				return true;
			}
		}
		return false;
	}

	@Override
	protected ClusterModel generateInternalClusterModel(ExampleSet exampleSet) throws OperatorException {
		return null;
	}

}
