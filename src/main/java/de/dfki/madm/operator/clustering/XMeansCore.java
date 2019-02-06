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
package de.dfki.madm.operator.clustering;

import java.util.ArrayList;

import com.rapidminer.example.Attribute;
import com.rapidminer.example.Attributes;
import com.rapidminer.example.Example;
import com.rapidminer.example.ExampleSet;
import com.rapidminer.example.Tools;
import com.rapidminer.example.set.SplittedExampleSet;
import com.rapidminer.operator.Operator;
import com.rapidminer.operator.OperatorDescription;
import com.rapidminer.operator.OperatorException;
import com.rapidminer.operator.OperatorProgress;
import com.rapidminer.operator.ProcessStoppedException;
import com.rapidminer.operator.UserError;
import com.rapidminer.operator.clustering.Centroid;
import com.rapidminer.operator.clustering.CentroidClusterModel;
import com.rapidminer.operator.clustering.Cluster;
import com.rapidminer.operator.clustering.ClusterModel;
import com.rapidminer.operator.clustering.clusterer.FastKMeans;
import com.rapidminer.operator.clustering.clusterer.KMeans;
import com.rapidminer.operator.clustering.clusterer.RMAbstractClusterer;
import com.rapidminer.operator.clustering.clusterer.XMeans;
import com.rapidminer.tools.RandomGenerator;
import com.rapidminer.tools.math.similarity.DistanceMeasure;

import de.dfki.madm.operator.KMeanspp;


public class XMeansCore extends RMAbstractClusterer {

	private static final int INTERMEDIATE_PROGRESS = 20;

	private ExampleSet exampleSet = null;
	private int examplesize = -1;
	private DistanceMeasure measure = null;
	private int k_min = -1;
	private int k_max = -1;
	private boolean kpp = false;
	private int maxOptimizationSteps = -1;
	private int maxRuns = -1;
	private OperatorDescription description = null;
	private Attributes attributes = null;
	private int dimension = -1;
	private int[] centroidAssignments = null;
	private String ClusteringAlgorithm = "";
	private Operator executingOperator = null;

	/**
	 * Initialization of X-Mean
	 *
	 * @param eSet
	 *            ExamleSet to cluster
	 * @param k_min
	 *            minimal number of cluster
	 * @param k_max
	 *            maximal number of cluster
	 * @param kpp
	 *            using K++-Algorithem to determin the first centroids
	 * @param maxOptimizationSteps
	 *            maximal optimationsteps of k-Means
	 * @param maxRuns
	 *            The maximal number of runs of k-Means with random initialization that are
	 *            performed.
	 * @param description
	 * @param measure
	 *            MeasureType to use
	 * @param cluster_alg
	 *            Clustering Algorithm to use
	 */
	public XMeansCore(ExampleSet eSet, int k_min, int k_max, boolean kpp, int maxOptimizationSteps, int maxRuns,
			OperatorDescription description, DistanceMeasure measure, String cluster_alg) {
		super(description);

		this.exampleSet = eSet;
		this.measure = measure;
		this.k_max = k_max;
		this.k_min = k_min;
		this.kpp = kpp;
		this.maxOptimizationSteps = maxOptimizationSteps;
		this.maxRuns = maxRuns;
		this.description = description;
		this.centroidAssignments = new int[exampleSet.size()];
		this.ClusteringAlgorithm = cluster_alg;
	}

	/**
	 * Running X-Means Algorithm
	 *
	 * @return Clustered Model
	 * @throws OperatorException
	 */
	public ClusterModel doXMean() throws OperatorException {

		examplesize = exampleSet.size();

		measure.init(exampleSet);

		// checking and creating ids if necessary
		Tools.checkAndCreateIds(exampleSet);

		// additional checks
		Tools.onlyNonMissingValues(exampleSet, getOperatorClassName(), this, new String[0]);
		if (exampleSet.size() < k_min) {
			throw new UserError(this, 142, k_min);
		}

		// extracting attribute names
		attributes = exampleSet.getAttributes();
		ArrayList<String> attributeNames = new ArrayList<String>(attributes.size());
		for (Attribute attribute : attributes) {
			attributeNames.add(attribute.getName());
		}

		CentroidClusterModel bestModel = null;

		RMAbstractClusterer kmean = null;

		// get the Clustering Algorithm
		if (this.ClusteringAlgorithm.equals("FastKMeans")) {
			kmean = new FastKMeans(description);
			((FastKMeans) kmean).setPresetMeasure(measure);
		} else if (this.ClusteringAlgorithm.equals("KMeans")) {
			kmean = new KMeans(description);
			((KMeans) kmean).setPresetMeasure(measure);
		} else {
			throw new OperatorException("Unknown kmeans algorithm: " + ClusteringAlgorithm);
		}

		// Set Parameters for Clustering Algorithm
		kmean.setParameter("k", k_min + "");
		kmean.setParameter("max_runs", maxRuns + "");
		kmean.setParameter("max_optimization_steps", maxOptimizationSteps + "");
		kmean.setParameter(KMeanspp.PARAMETER_USE_KPP, kpp + "");
		if (executingOperator != null
				&& executingOperator.getParameterAsBoolean(RandomGenerator.PARAMETER_USE_LOCAL_RANDOM_SEED)) {
			kmean.setParameter(RandomGenerator.PARAMETER_USE_LOCAL_RANDOM_SEED, Boolean.toString(true));
			kmean.setParameter(RandomGenerator.PARAMETER_LOCAL_RANDOM_SEED,
					executingOperator.getParameter(RandomGenerator.PARAMETER_LOCAL_RANDOM_SEED));
		}

		if (this.getCompatibilityLevel().isAbove(XMeans.VERSION_9_0_0_LABEL_ROLE_BUG)) {
			kmean.setParameter(RMAbstractClusterer.PARAMETER_ADD_CLUSTER_ATTRIBUTE, "false");
		}
		kmean.setCompatibilityLevel(getCompatibilityLevel());

		// initialize progress
		OperatorProgress operatorProgress = null;
		if (executingOperator != null && executingOperator.getProgress() != null) {
			operatorProgress = executingOperator.getProgress();
			operatorProgress.setTotal(100);
		}

		// get the first run
		bestModel = (CentroidClusterModel) kmean.generateClusterModel(exampleSet);
		if (operatorProgress != null) {
			operatorProgress.setCompleted(INTERMEDIATE_PROGRESS);
		}

		// save Dimension of data
		dimension = bestModel.getCentroid(0).getCentroid().length;

		// calculate first BIC
		double current_m_BIC = this.calcBIC(bestModel);

		boolean change = true;

		boolean addAsLabel = addsLabelAttribute();
		boolean removeUnlabeled = getParameterAsBoolean(RMAbstractClusterer.PARAMETER_REMOVE_UNLABELED);

		while (bestModel.getCentroids().size() < k_max && change) {
			change = false;
			int array_size = bestModel.getClusters().size();

			CentroidClusterModel[] Children = new CentroidClusterModel[array_size];
			CentroidClusterModel[] Parent = new CentroidClusterModel[array_size];
			SplittedExampleSet splittedSet = SplittedExampleSet.splitByAttribute(exampleSet,
					exampleSet.getAttributes().get("cluster"));

			if (splittedSet.getNumberOfSubsets() < array_size) {
				break;
			}
			int anz = 0;

			// get all Child-cluster
			for (@SuppressWarnings("unused")
			Cluster cl : bestModel.getClusters()) {
				splittedSet.selectSingleSubset(anz);

				kmean.setParameter("k", 2 + "");
				Children[anz] = (CentroidClusterModel) kmean.generateClusterModel(splittedSet);
				kmean.setParameter("k", 1 + "");
				Parent[anz] = (CentroidClusterModel) kmean.generateClusterModel(splittedSet);
				anz++;
			}

			Double[] SaveDiffBic = new Double[array_size];
			boolean[] takeChange = new boolean[array_size];
			int change_anz = 0;
			// check which Children to take
			for (int i = 0; i < Parent.length; i++) {
				double BICc = calcBIC(Children[i]);
				double BICp = calcBIC(Parent[i]);
				if (BICc > BICp) {
					// take Children
					takeChange[i] = true;
					SaveDiffBic[i] = BICc - BICp;
					change_anz++;
				} else {
					takeChange[i] = false;
				}
			}

			CentroidClusterModel model = null;
			if (change_anz + array_size < k_max) {
				// all children are in the limit
				model = new CentroidClusterModel(exampleSet, change_anz + array_size, attributeNames, measure, addAsLabel,
						removeUnlabeled);

				int id = 0;
				for (int i = 0; i < array_size; i++) {
					if (takeChange[i]) {
						for (Centroid z : Children[i].getCentroids()) {
							model.assignExample(id, z.getCentroid());
							id++;
						}
					} else {
						model.assignExample(id, Parent[i].getCentroid(0).getCentroid());
						id++;
					}
				}
			} else {
				// pick the best children
				model = new CentroidClusterModel(exampleSet, k_max, attributeNames, measure, addAsLabel, removeUnlabeled);
				double hilf = 0;
				CentroidClusterModel hilf2 = null;
				// sort
				for (int i = 0; i < takeChange.length - 1; i++) {
					for (int j = i + 1; j < takeChange.length; j++) {
						if (SaveDiffBic[j] > SaveDiffBic[i]) {
							hilf = SaveDiffBic[j];
							SaveDiffBic[j] = SaveDiffBic[i];
							SaveDiffBic[i] = hilf;

							hilf2 = Children[j];
							Children[j] = Children[i];
							Children[i] = hilf2;

							hilf2 = Parent[j];
							Parent[j] = Parent[i];
							Parent[i] = hilf2;
						}
					}
				}

				int id = 0;
				int anz1 = 0;
				for (int i = 0; i < array_size; i++) {
					if (takeChange[i]) {
						for (Centroid z : Children[i].getCentroids()) {
							model.assignExample(id, z.getCentroid());
							id++;
							anz1++;
						}
					} else {
						model.assignExample(id, Parent[i].getCentroid(0).getCentroid());
						id++;
						anz1++;
					}
					if (anz1 >= k_max) {
						break;
					}
				}
			}

			model.finishAssign();

			model = this.assinePoints(model);

			double new_m_BIC = calcBIC(model);

			// check if the new BIC is better than the old
			if (new_m_BIC > current_m_BIC) {
				change = true;
				bestModel = model;
				current_m_BIC = new_m_BIC;
			} else {
				model = null;
			}

			if (operatorProgress != null) {
				if (bestModel.getCentroids().size() > k_max) {
					operatorProgress.complete();
				} else {
					operatorProgress.setCompleted((int) (INTERMEDIATE_PROGRESS
							+ (100.0 - INTERMEDIATE_PROGRESS) * bestModel.getCentroids().size() / k_max));
				}
			}
		}

		if (getCompatibilityLevel().isAbove(XMeans.VERSION_9_0_0_LABEL_ROLE_BUG) && getCompatibilityLevel().isAtMost(XMeans.VERSION_9_1_0_POINTS_COUNTED_TWICE_BUG)) {
			bestModel = assinePoints(bestModel);
		}

		if (addsClusterAttribute()) {
			if (getCompatibilityLevel().isAbove(XMeans.VERSION_9_1_0_POINTS_COUNTED_TWICE_BUG)) {
				centroidAssignments = bestModel.getClusterAssignments(exampleSet);
			}
			addClusterAssignments(exampleSet, centroidAssignments);
		}

		if (operatorProgress != null) {
			operatorProgress.complete();
		}

		return bestModel;
	}

	/**
	 * assign the Points to cluster
	 *
	 * @param model
	 * @return
	 */
	private CentroidClusterModel assinePoints(CentroidClusterModel model) {
		double[] values = new double[attributes.size()];
		int i = 0;
		for (Example example : exampleSet) {
			double[] exampleValues = getAsDoubleArray(example, attributes, values);
			double nearestDistance = measure.calculateDistance(model.getCentroidCoordinates(0), exampleValues);
			int nearestIndex = 0;
			int id = 0;
			for (Centroid cr : model.getCentroids()) {
				double distance = measure.calculateDistance(cr.getCentroid(), exampleValues);
				if (distance < nearestDistance) {
					nearestDistance = distance;
					nearestIndex = id;
				}
				id++;
			}
			centroidAssignments[i] = nearestIndex;
			i++;
		}

		model.setClusterAssignments(centroidAssignments, exampleSet);
		return model;
	}

	/**
	 * Calculate the BIC like in the paper by Dan Pelleg and Andrew Moore
	 *
	 * @param bestModel
	 * @return BIC of the given modell
	 * @throws ProcessStoppedException
	 */
	private double calcBIC(CentroidClusterModel bestModel) throws ProcessStoppedException {
		double loglike = 0;
		int numCenters = bestModel.getNumberOfClusters();
		int numDimensions = bestModel.getCentroidCoordinates(0).length;

		int numParameters = numCenters - 1 + // probabilities
				numCenters * numDimensions + // means
				numCenters; // variance params

		for (Cluster c : bestModel.getClusters()) {
			int current_id = c.getClusterId();
			loglike += logLikelihoodEstimate(c, bestModel.getCentroidCoordinates(current_id), numCenters);
		}

		loglike -= numParameters / 2.0 * Math.log(examplesize);
		return loglike;
	}

	private double[] getAsDoubleArray(Example example, Attributes attributes, double[] values) {
		int i = 0;
		for (Attribute attribute : attributes) {
			values[i] = example.getValue(attribute);
			i++;
		}
		return values;
	}

	private double logLikelihoodEstimate(Cluster c, double[] centroid, int K) {
		double l = 0;
		double R = examplesize;
		double Rn = c.getNumberOfExamples();
		double M = dimension;
		double d = 0;
		double[] values = new double[attributes.size()];

		if (Rn > 1) {
			double sum = 0;

			final Attribute idAttribute = exampleSet.getAttributes().getId();
			boolean idIsNominal = idAttribute.isNominal();
			exampleSet.remapIds();
			for (Object ob : c.getExampleIds()) {
				Example example;
				if (idIsNominal) {
					example = exampleSet.getExampleFromId(idAttribute.getMapping().mapString((String) ob));
				} else {
					example = exampleSet.getExampleFromId(((Double) ob).intValue());
				}
				if (example == null) {
					throw new RuntimeException("Unknown id: " + ob);
				}
				sum += Math.pow(measure.calculateDistance(centroid, getAsDoubleArray(example, attributes, values)), 2);
			}

			d = 1.0 / (Rn - K) * sum;

			l = -(Rn / 2.0) * Math.log(2.0 * Math.PI) - Rn * M / 2.0 * Math.log(d) - (Rn - K) / 2.0 + Rn * Math.log(Rn)
					- Rn * Math.log(R);
		}
		return l;
	}

	@Override
	protected ClusterModel generateInternalClusterModel(ExampleSet exampleSet) throws OperatorException {
		return null;
	}

	/**
	 * The operator in which XMeans is done. Used to display the progress.
	 *
	 * @param executingOperator
	 *            The executing XMeans operator
	 */
	public void setExecutingOperator(Operator executingOperator) {
		this.executingOperator = executingOperator;
	}

}
