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
package com.rapidminer.operator.clustering.clusterer;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.Map;

import com.rapidminer.example.Attribute;
import com.rapidminer.example.Attributes;
import com.rapidminer.example.Example;
import com.rapidminer.example.ExampleSet;
import com.rapidminer.example.Tools;
import com.rapidminer.operator.OperatorDescription;
import com.rapidminer.operator.OperatorException;
import com.rapidminer.operator.OperatorVersion;
import com.rapidminer.operator.UserError;
import com.rapidminer.operator.clustering.CentroidClusterModel;
import com.rapidminer.operator.clustering.ClusterModel;
import com.rapidminer.parameter.ParameterType;
import com.rapidminer.parameter.ParameterTypeBoolean;
import com.rapidminer.parameter.ParameterTypeInt;
import com.rapidminer.tools.RandomGenerator;
import com.rapidminer.tools.math.similarity.DistanceMeasure;
import com.rapidminer.tools.math.similarity.DistanceMeasures;

import de.dfki.madm.operator.KMeanspp;


/**
 * This operator represents an implementation of k-means. This operator will create a cluster
 * attribute if not present yet.
 *
 * The implementation is according to paper of C. Elkan: - Using the Triangle Inequality to
 * Accelerate k-Means - Proceedings of the Twentieth International Conference on Machine Learning
 * (ICML-2003), Washington DC, 2003
 *
 * @author Alexander Arimond
 */

public class FastKMeans extends RMAbstractClusterer {

	/** The parameter name for &quot;the maximal number of clusters&quot; */
	public static final String PARAMETER_K = "k";

	/**
	 * The parameter name for &quot;the maximal number of runs of the k method with random
	 * initialization that are performed&quot;
	 */
	public static final String PARAMETER_MAX_RUNS = "max_runs";

	/**
	 * The parameter name for &quot;the maximal number of iterations performed for one run of the k
	 * method&quot;
	 */
	public static final String PARAMETER_MAX_OPTIMIZATION_STEPS = "max_optimization_steps";

	/**
	 * Overrides the measure specified by the operator parameters. If set to null, parameters will
	 * be used again to determine the measure.
	 */
	@Override
	public void setPresetMeasure(DistanceMeasure me) {
		super.setPresetMeasure(me);
	}

	public FastKMeans(OperatorDescription description) {
		super(description);
	}

	@Override
	protected ClusterModel generateInternalClusterModel(ExampleSet exampleSet) throws OperatorException {
		boolean kpp = getParameterAsBoolean(KMeanspp.PARAMETER_USE_KPP) && getCompatibilityLevel().isAbove(KMeanspp.VERSION_KPP_NOT_WORKING);
		int k = getParameterAsInt(PARAMETER_K);
		int maxOptimizationSteps = getParameterAsInt(PARAMETER_MAX_OPTIMIZATION_STEPS);
		int maxRuns = getParameterAsInt(PARAMETER_MAX_RUNS);
		boolean addAsLabel = addsLabelAttribute();
		boolean removeUnlabeled = getParameterAsBoolean(RMAbstractClusterer.PARAMETER_REMOVE_UNLABELED);
		DistanceMeasure measure = getInitializedMeasure(exampleSet);

		// init operator progress
		getProgress().setTotal(maxRuns);

		// checking and creating ids if necessary
		Tools.checkAndCreateIds(exampleSet);

		// additional checks
		Tools.onlyNonMissingValues(exampleSet, getOperatorClassName(), this, new String[0]);
		if (exampleSet.size() < k) {
			throw new UserError(this, 142, k);
		}

		// extracting attribute names
		Attributes attributes = exampleSet.getAttributes();
		ArrayList<String> attributeNames = new ArrayList<>(attributes.size());
		for (Attribute attribute : attributes) {
			attributeNames.add(attribute.getName());
		}

		RandomGenerator generator = RandomGenerator.getRandomGenerator(this);
		double minimalIntraClusterDistance = Double.POSITIVE_INFINITY;
		CentroidClusterModel bestModel = null;
		int[] bestAssignments = null;

		for (int iter = 0; iter < maxRuns; iter++) {
			CentroidClusterModel model = new CentroidClusterModel(exampleSet, k, attributeNames, measure, addAsLabel,
					removeUnlabeled);

			// init centroids by assigning one single, unique example!
			int i = 0;
			if (kpp) {
				KMeanspp kmpp = new KMeanspp(this, k, exampleSet, measure, generator);

				int[] hilf = kmpp.getStart();
				int i1 = 0;

				for (int id : hilf) {
					double[] as = getAsDoubleArray(exampleSet.getExample(id), attributes);
					model.assignExample(i1, as);
					i1++;
				}
			} else {
				for (Integer index : generator.nextIntSetWithRange(0, exampleSet.size(), k)) {
					model.assignExample(i, getAsDoubleArray(exampleSet.getExample(index), attributes));
					i++;
				}
			}
			model.finishAssign();

			// auxiliary data structures according to paper
			final double[][] l = new double[exampleSet.size()][k];
			final double[] u = new double[exampleSet.size()];
			final boolean[] r = new boolean[exampleSet.size()];

			final double[][] m_old = new double[k][attributes.size()]; // needed for step 4
			final double[] s = new double[k];

			final int[] centroidAssignments = new int[exampleSet.size()];

			final DistanceMatrix centroidDistances = new DistanceMatrix(k);
			computeClusterDistances(centroidDistances, s, model, measure);

			// initialization step (has many distance calculations)
			int x = 0;
			for (Example example : exampleSet) {
				double[] exampleValues = getAsDoubleArray(example, attributes);
				double nearestDistance = measure.calculateDistance(model.getCentroidCoordinates(0), exampleValues);
				l[x][0] = nearestDistance;
				int nearestIndex = 0;
				for (int centroidIndex = 1; centroidIndex < k; centroidIndex++) {
					if (centroidDistances.get(nearestIndex, centroidIndex) >= 2 * nearestDistance) {
						continue;
					}
					final double distance = measure.calculateDistance(model.getCentroidCoordinates(centroidIndex),
							exampleValues);
					l[x][centroidIndex] = distance;
					if (distance < nearestDistance) {
						nearestDistance = distance;
						nearestIndex = centroidIndex;
					}
				}
				centroidAssignments[x] = nearestIndex;
				u[x] = nearestDistance;
				r[x] = false;
				x++;
			}

			// optimization steps (repeat until convergence)
			boolean stable = false;
			for (int step = 0; step < maxOptimizationSteps && !stable; step++) {

				// step 1.
				computeClusterDistances(centroidDistances, s, model, measure);

				x = 0;
				for (Example example : exampleSet) {
					final double[] exampleValue = getAsDoubleArray(example, attributes);

					// step 2.
					if (u[x] <= s[centroidAssignments[x]]) {
					} else {
						// step 3.
						for (int c = 0; c < k; c++) {
							if (c != centroidAssignments[x]  // (i)
									&& u[x] > l[x][c] 			// (ii)
									&& u[x] > 0.5 * centroidDistances.get(centroidAssignments[x], c) // (iii)
							) {
								// step 3a.
								final double d_x_c;   // d(x,c(x))
								if (r[x]) {
									d_x_c = measure.calculateDistance(exampleValue,
											model.getCentroidCoordinates(centroidAssignments[x]));
									l[x][centroidAssignments[x]] = d_x_c;
									u[x] = d_x_c;
									r[x] = false;
								} else {
									d_x_c = u[x];
								}
								// step 3b.
								if (d_x_c > l[x][c] && d_x_c > 0.5 * centroidDistances.get(centroidAssignments[x], c)) {
									final double d_x_c_new = measure.calculateDistance(exampleValue,
											model.getCentroidCoordinates(c)); // d(x,c)
									l[x][c] = d_x_c_new;
									if (d_x_c_new < d_x_c) {
										centroidAssignments[x] = c;
										u[x] = d_x_c_new;
									}
								}
							}
						}

					}
					model.assignExample(centroidAssignments[x], exampleValue);
					x++;
				}

				// step 4
				// first store old c
				for (int c = 0; c < k; c++) {
					m_old[c] = model.getCentroidCoordinates(c);
				}
				// then compute the m(c) - here this is same as step 7
				stable = model.finishAssign();

				// compute all d(c,m(c))
				final double[] mean_distances = new double[k];
				for (int c = 0; c < k; c++) {
					mean_distances[c] = measure.calculateDistance(m_old[c], model.getCentroidCoordinates(c));
				}

				// step 5 & 6
				for (x = 0; x < exampleSet.size(); x++) {
					// step 5
					for (int c = 0; c < k; c++) {
						final double d = l[x][c] - mean_distances[c];
						if (d > 0) {
							l[x][c] = d;
						} else {
							l[x][c] = 0;
						}
					}
					// step 6
					u[x] = u[x] + mean_distances[centroidAssignments[x]];
					r[x] = true;
				}

			}
			// assessing quality of this model
			double distanceSum = 0;
			i = 0;
			for (Example example : exampleSet) {
				double distance = measure.calculateDistance(model.getCentroidCoordinates(centroidAssignments[i]),
						getAsDoubleArray(example, attributes));
				distanceSum += distance * distance;
				i++;
			}
			if (distanceSum < minimalIntraClusterDistance) {
				bestModel = model;
				minimalIntraClusterDistance = distanceSum;
				bestAssignments = centroidAssignments;
			}
			getProgress().step();
		}
		bestModel.setClusterAssignments(bestAssignments, exampleSet);

		if (addsClusterAttribute()) {
			addClusterAssignments(exampleSet, bestAssignments);
		}
		getProgress().complete();

		return bestModel;
	}

	// this is for step 1 of the paper algorithm
	private void computeClusterDistances(DistanceMatrix centroidDistances, double[] s, CentroidClusterModel model,
			DistanceMeasure measure) {
		for (int i = 0; i < model.getNumberOfClusters(); i++) {
			s[i] = Double.POSITIVE_INFINITY;
		}
		for (int i = 0; i < model.getNumberOfClusters(); i++) {
			for (int j = i + 1; j < model.getNumberOfClusters(); j++) {
				final double d = measure.calculateDistance(model.getCentroidCoordinates(i), model.getCentroidCoordinates(j));
				if (d < s[i]) {
					s[i] = d;
				}
				if (d < s[j]) {
					s[j] = d;
				}
				centroidDistances.set(i, j, d);
			}
		}
		for (int i = 0; i < model.getNumberOfClusters(); i++) {
			s[i] = 0.5 * s[i];
		}
	}

	private double[] getAsDoubleArray(Example example, Attributes attributes) {
		double[] values = new double[attributes.size()];
		int i = 0;
		for (Attribute attribute : attributes) {
			values[i] = example.getValue(attribute);
			i++;
		}
		return values;
	}

	@Override
	public Class<? extends ClusterModel> getClusterModelClass() {
		return CentroidClusterModel.class;
	}

	@Override
	protected boolean usesDistanceMeasures() {
		return true;
	}

	@Override
	protected boolean usesPresetMeasure() {
		return true;
	}

	@Override
	protected boolean handlesInfiniteValues() {
		return false;
	}

	@Override
	public List<ParameterType> getParameterTypes() {
		List<ParameterType> types = super.getParameterTypes();
		types.add(new ParameterTypeInt(PARAMETER_K, "The number of clusters which should be detected.", 2, Integer.MAX_VALUE,
				5, false));
		types.add(new ParameterTypeBoolean(KMeanspp.PARAMETER_USE_KPP, KMeanspp.SHORT_DESCRIPTION, true));
		types.addAll(getMeasureParameterTypes());
		types.add(new ParameterTypeInt(PARAMETER_MAX_RUNS,
				"The maximal number of runs of k-Means with random initialization that are performed.", 1, Integer.MAX_VALUE,
				10, false));
		types.add(new ParameterTypeInt(PARAMETER_MAX_OPTIMIZATION_STEPS,
				"The maximal number of iterations performed for one run of k-Means.", 1, Integer.MAX_VALUE, 100, false));
		types.addAll(RandomGenerator.getRandomGeneratorParameters(this));
		return types;
	}

	@Override
	protected Map<String, Object> getMeasureParametersDefaults() {
		return Collections.singletonMap(DistanceMeasures.PARAMETER_MEASURE_TYPES, DistanceMeasures.NUMERICAL_MEASURES_TYPE);
	}

	@Override
	public OperatorVersion[] getIncompatibleVersionChanges() {
		OperatorVersion[] incompatibleVersions = super.getIncompatibleVersionChanges();
		OperatorVersion[] extendedIncompatibleVersions = Arrays.copyOf(incompatibleVersions,
				incompatibleVersions.length + 1);
		extendedIncompatibleVersions[incompatibleVersions.length] = KMeanspp.VERSION_KPP_NOT_WORKING;
		return extendedIncompatibleVersions;
	}
}
