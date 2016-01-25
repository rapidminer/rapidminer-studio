/**
 * Copyright (C) 2001-2016 by RapidMiner and the contributors
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
import com.rapidminer.operator.OperatorDescription;
import com.rapidminer.operator.OperatorException;
import com.rapidminer.operator.ProcessStoppedException;
import com.rapidminer.operator.clustering.ClusterModel;
import com.rapidminer.operator.clustering.clusterer.RMAbstractClusterer;
import com.rapidminer.tools.RandomGenerator;
import com.rapidminer.tools.math.similarity.DistanceMeasure;


/**
 * This algorithm is the first part of K-Means++ descried in the paper
 * "k-means++: The Advantages of Careful Seeding" by David Arther and Sergei Vassilvitskii
 * 
 * @author Patrick Kalka
 * 
 */
public class KMeanspp extends RMAbstractClusterer {

	/** Short description for GUI */
	public static final String SHORT_DESCRIPTION = "Determine the first k centroids using the K-Means++ heuristic described in \"k-means++: The Advantages of Careful Seeding\" by David Arthur and Sergei Vassilvitskii 2007";

	/** Label for button */
	public static final String PARAMETER_USE_KPP = "determine_good_start_values";

	/** ExampleSet to work on */
	private ExampleSet exampleSet = null;

	/** DistanceMeasure to use */
	private DistanceMeasure measure = null;

	private RandomGenerator generator = null;
	private int examplesize = -1;
	private int minK = 0;

	/**
	 * Initialization of K-Means++
	 * 
	 * @param description
	 * @param anz
	 *            initial Cluster count
	 * @param es
	 *            ExampleSet to work on
	 * @param measure
	 *            DistanceMeasure to use
	 * @throws OperatorException
	 */
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
			boolean again = false;
			checkForStop();

			do {
				checkForStop();
				again = false;
				double[] shortest = new double[examplesize];
				double maxProb = 0;
				int maxPorbId = -1;
				double distSum = 0;
				// sum of shortest path between chosen centroids an all Points
				for (int j = 0; j < examplesize; j++) {
					double minDist = -1;
					Example ex = exampleSet.getExample(j);
					for (Integer id : ret) {
						double dist = measure.calculateDistance(ex, exampleSet.getExample(id));
						if (minDist == -1 || minDist > dist) {
							minDist = dist;
						}
					}
					distSum += minDist;
					shortest[j] = minDist;
				}

				// get maximal Probability
				for (int j = 0; j < examplesize; j++) {
					double prob = Math.pow(shortest[j], 2) / Math.pow(distSum, 2);
					if (prob > maxProb) {
						maxPorbId = j;
						maxProb = prob;
					}
				}

				i = maxPorbId;
				for (Integer id : ret) {
					if (id == i) {
						again = true;
					}
				}
			} while (again);
			ret[anz] = i;
			anz++;
		}

		return ret;
	}

	@Override
	public ClusterModel generateClusterModel(ExampleSet exampleSet) throws OperatorException {
		return null;
	}
}
