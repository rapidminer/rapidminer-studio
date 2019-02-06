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
package com.rapidminer.operator.clustering.clusterer.soft;

import java.util.Collection;
import java.util.LinkedList;
import java.util.List;
import java.util.Random;
import java.util.Vector;

import com.rapidminer.example.Attribute;
import com.rapidminer.example.Example;
import com.rapidminer.example.ExampleSet;
import com.rapidminer.example.Statistics;
import com.rapidminer.example.Tools;
import com.rapidminer.example.table.AttributeFactory;
import com.rapidminer.operator.OperatorCreationException;
import com.rapidminer.operator.OperatorDescription;
import com.rapidminer.operator.OperatorException;
import com.rapidminer.operator.OperatorVersion;
import com.rapidminer.operator.UserError;
import com.rapidminer.operator.clustering.ClusterModel;
import com.rapidminer.operator.clustering.FlatFuzzyClusterModel;
import com.rapidminer.operator.clustering.clusterer.KMeans;
import com.rapidminer.operator.clustering.clusterer.RMAbstractClusterer;
import com.rapidminer.operator.ports.metadata.AttributeMetaData;
import com.rapidminer.parameter.ParameterType;
import com.rapidminer.parameter.ParameterTypeBoolean;
import com.rapidminer.parameter.ParameterTypeCategory;
import com.rapidminer.parameter.ParameterTypeDouble;
import com.rapidminer.parameter.ParameterTypeInt;
import com.rapidminer.parameter.UndefinedParameterError;
import com.rapidminer.parameter.conditions.AboveOperatorVersionCondition;
import com.rapidminer.parameter.conditions.EqualTypeCondition;
import com.rapidminer.tools.Ontology;
import com.rapidminer.tools.OperatorService;
import com.rapidminer.tools.RandomGenerator;
import com.rapidminer.tools.math.VectorMath;
import com.rapidminer.tools.math.similarity.DistanceMeasures;

import Jama.Matrix;


/**
 * This operator represents an implementation of the EM-algorithm.
 *
 * @author Regina Fritsch
 */
public class EMClusterer extends RMAbstractClusterer {

	private static final OperatorVersion BEFORE_MEASURE_PARAM = new OperatorVersion(7, 5, 3);

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
	 * The parameter name for &quot;the quality, which has to be fulfilled for the stopping of the
	 * soft clustering&quot;
	 */
	public static final String PARAMETER_QUALITY = "quality";

	/**
	 * The parameter name for &quot;Indicates if the probabilities will be shown in example
	 * table&quot;
	 */
	public static final String PARAMETER_SHOW_PROBABILITIES = "show_probabilities";

	/** The parameter name for &quot;Indicates the initialization distribution&quot; */
	public static final String PARAMETER_INITIALIZATION_DISTRIBUTION = "inital_distribution";

	/** The parameter name for &quot;List of the different init distributions&quot; */
	public static final String[] INIT_DISTRIBUTION = { "randomly assigned examples", "k-means run", "average parameters" };

	/** The parameter value for &quot;Init distributions randomly assigned&quot; */
	public static final int RANDOMLY_ASSIGNED = 0;

	/** The parameter value for &quot;Init distributions hard clustering&quot; */
	public static final int K_MEANS = 1;

	/** The parameter value for &quot;Init distributions average parameters&quot; */
	public static final int AVERAGE_PARAMETERS = 2;

	/** The parameter name for &quot;Indicates if the example set has correlated attributes&quot; */
	public static final String PARAMETER_CORRELATED = "correlated_attributes";

	public EMClusterer(OperatorDescription description) {
		super(description);
	}

	@Override
	protected Collection<AttributeMetaData> getAdditionalAttributes() {
		List<AttributeMetaData> propAttributes = new LinkedList<AttributeMetaData>();
		try {
			int k = getParameterAsInt(PARAMETER_K);
			for (int i = 0; i < k; i++) {
				AttributeMetaData newAttr = new AttributeMetaData("cluster_" + i + "_probability", Ontology.REAL,
						"cluster_" + i + "_probability");
				propAttributes.add(newAttr);
			}
		} catch (UndefinedParameterError e) {
		}
		return propAttributes;
	}

	/*
	 * Creates the Clustermodel.
	 */
	public ClusterModel createClusterModel(ExampleSet exampleSet) throws OperatorException {
		FlatFuzzyClusterModel bestModel = null;

		int initialMaxRuns = getParameterAsInt(PARAMETER_MAX_RUNS);
		int maxRuns = initialMaxRuns;

		boolean isCorrelated = getParameterAsBoolean(PARAMETER_CORRELATED);
		boolean addAsLabel = addsLabelAttribute();
		boolean removeUnlabeled = getParameterAsBoolean(RMAbstractClusterer.PARAMETER_REMOVE_UNLABELED);
		double quality = getParameterAsDouble(PARAMETER_QUALITY);
		int k = getParameterAsInt(PARAMETER_K);

		// init operator progress
		int maxOptiRuns = getParameterAsInt(PARAMETER_MAX_OPTIMIZATION_STEPS);
		getProgress().setTotal(100);
		int progressOffset = 0;

		int initSpecialSize = exampleSet.getAttributes().specialSize();
		double[][] exampleInClusterProbability = new double[exampleSet.size()][k];

		boolean showProbs = getParameterAsBoolean(PARAMETER_SHOW_PROBABILITIES);

		double max = Double.NEGATIVE_INFINITY;
		int exceptionCounter = 0;

		int[] bestAssignments = null;

		RandomGenerator randomGenerator = RandomGenerator.getGlobalRandomGenerator();

		// the iterations
		for (int iter = 0; iter < maxRuns; iter++) {
			FlatFuzzyClusterModel result = new FlatFuzzyClusterModel(exampleSet, k, addAsLabel, removeUnlabeled);
			FlatFuzzyClusterModel oldResult = null;
			// initialize the model
			try {
				init(exampleSet, result, k, initSpecialSize, exampleInClusterProbability, showProbs, isCorrelated);
			} catch (OperatorCreationException e1) {
				e1.printStackTrace();
			}
			boolean stableState = false;
			double logLikelihoodOld = Double.POSITIVE_INFINITY;
			double logLikelihood = 0;
			// the optimization-steps
			int[] clusterAssignments = new int[exampleSet.size()];
			try {
				for (int optiStep = 0; optiStep < maxOptiRuns && !stableState; optiStep++) {
					getProgress().setCompleted(progressOffset + 100 * (iter * maxOptiRuns + optiStep) / maxRuns / maxOptiRuns);
					stableState = true;
					oldResult = result;
					result = new FlatFuzzyClusterModel(exampleSet, k, addAsLabel, removeUnlabeled);
					// Compute the probabilities for each example with each cluster
					if (isCorrelated) {
						expectationCorrelated(exampleSet, k, exampleInClusterProbability, oldResult);
					} else {
						expectationNonCorrelated(exampleSet, k, exampleInClusterProbability, oldResult);
					}
					// compute the hard-clustering from the soft-clustering (assignments of the
					// examples to the clusters)
					for (int exampleIndex = 0; exampleIndex < exampleSet.size(); exampleIndex++) {
						int bestIndex = bestIndex(exampleIndex, k, exampleInClusterProbability);
						if (bestIndex < 0) {
							bestIndex = randomGenerator.nextInt(result.getNumberOfClusters());
						}
						clusterAssignments[exampleIndex] = bestIndex;
					}
					result.setClusterAssignments(clusterAssignments, exampleSet);
					// Recalculate the values: cluster probabilities, means and standard deviations
					maximization(exampleSet, k, exampleInClusterProbability, result, isCorrelated);
					// test if the quality of the soft-clustering performs the user-defined quality
					logLikelihood = computeLogLikelihood(k, exampleInClusterProbability, result);
					double difference = logLikelihoodOld - logLikelihood;
					if (Math.abs(difference) >= quality) {
						stableState = false;
					}
					logLikelihoodOld = logLikelihood;
				}
			} catch (OperatorException e) {
				throw e;
			} catch (Exception e) {
				exceptionCounter++;
				// If there occurs an exception, don't stop at the first time and if there are some
				// usable models don't discard them.
				if (exceptionCounter > initialMaxRuns) {
					// if there are not enough models, start again without the option correlated
					if (iter - (exceptionCounter - 1) < Math.round(maxRuns * 0.49) && isCorrelated) {
						getLogger().info("Can't compute the inverse of the covariance matrix. Maybe the Matrix is singular. Changing option \"correlated_attributes\" to false.");
						// reset maxRuns and iter, start again with no correlation
						exceptionCounter = 0;
						isCorrelated = false;
						maxRuns = initialMaxRuns;
						iter = -1;
						getProgress().setTotal(200);
						getProgress().setCompleted(100);
						progressOffset = 100;
						continue;
					}
					break;
				}
				// allow one more run
				maxRuns++;
				continue;
			}
			// check if the model of the current iteration is better than the models computed before
			if (Math.abs(logLikelihood) > max) {
				max = Math.abs(logLikelihood);
				bestModel = result;
				bestAssignments = clusterAssignments;
				if (showProbs) {
					setProbabilitiesInTable(exampleSet, exampleInClusterProbability);
					bestModel.setExampleInClusterProbability(exampleInClusterProbability);
				}
			}
			getProgress().setCompleted(progressOffset + (int) (100.0 * (iter + 1) / maxRuns));
		}

		if (bestModel == null) {
			throw new UserError(this, "no_cluster_model_calculated");
		}

		if (addsClusterAttribute()) {
			addClusterAssignments(exampleSet, bestAssignments);
		}

		return bestModel;
	}

	/*
	 * INIT SECTOR
	 */

	/*
	 * Main init method.
	 */
	private void init(ExampleSet exampleSet, FlatFuzzyClusterModel result, int k, int initSpecialSize,
					  double[][] exampleInClusterProbability, boolean showProbs, boolean correlated) throws OperatorException, OperatorCreationException {

		// init means, standard deviations (or covariance matrix) and cluster probabilities
		// according to specified distribution
		int distribution = getParameterAsInt(PARAMETER_INITIALIZATION_DISTRIBUTION);

		switch (distribution) {
			case RANDOMLY_ASSIGNED:
				try {
					// allocate the examples randomly to the clusters
					Random random = RandomGenerator.getRandomGenerator(this);
					int clustersFilled;
					do {
						clustersFilled = 0;
						double[][] clusterMeans = new double[k][exampleSet.getAttributes().size()];
						Attribute[] regularAttributes = exampleSet.getAttributes().createRegularAttributeArray();
						int i = 0;
						for (Example ex : exampleSet) {
							int cluster = random.nextInt(k);
							exampleInClusterProbability[i][cluster] = 1;
							int j = 0;
							for (Attribute attribute : regularAttributes) {
								clusterMeans[cluster][j] += ex.getValue(attribute);
								j++;
							}
							i++;
						}
						// check if there is at least one example in each cluster
						for (i = 0; i < k; i++) {
							// set means in the model (allready not normalized)
							result.setClusterMean(i, clusterMeans[i]);
							for (int j = 0; j < exampleInClusterProbability.length; j++) {
								if (exampleInClusterProbability[j][i] == 1) {
									clustersFilled++;
									break;
								}
							}
						}
					} while (clustersFilled < k);
				} catch (UndefinedParameterError e) {
				}
				// compute means (normalized), stdDev...)
				computeValuesWithClusterMemberships(exampleSet, k, exampleInClusterProbability, result);
				if (correlated) {
					initCovarianceMatrix(exampleSet, exampleInClusterProbability, result, k);
				}
				break;
			case K_MEANS:
				// allocate the examples according to the k-means run to the clusters
				KMeans clusterAlgorithm = OperatorService.createOperator(KMeans.class);
				ExampleSet clusterSet = (ExampleSet) exampleSet.clone();
				clusterAlgorithm.setParameter(KMeans.PARAMETER_K, "" + k);
				clusterAlgorithm.setParameter(RMAbstractClusterer.PARAMETER_ADD_CLUSTER_ATTRIBUTE, "true");
				
				if (getParameterAsBoolean(RandomGenerator.PARAMETER_USE_LOCAL_RANDOM_SEED)) {
					clusterAlgorithm.setParameter(RandomGenerator.PARAMETER_USE_LOCAL_RANDOM_SEED, Boolean.toString(true));
					clusterAlgorithm.setParameter(RandomGenerator.PARAMETER_LOCAL_RANDOM_SEED,
							getParameter(RandomGenerator.PARAMETER_LOCAL_RANDOM_SEED));
				}

				clusterAlgorithm.setPresetMeasure(DistanceMeasures.createMeasure(this));
				clusterAlgorithm.generateClusterModel(clusterSet); // ad a side effect, add cluster
				// attribute to clusterSet

				double[][] clusterMeans = new double[k][exampleSet.getAttributes().size()];
				int exampleIndex = 0;
				Attribute clusterAttribute = clusterSet.getAttributes().getCluster();
				Attribute[] regularAttributes = exampleSet.getAttributes().createRegularAttributeArray();
				for (Example example : clusterSet) {
					int clusterIndex = (int) example.getValue(clusterAttribute);
					exampleInClusterProbability[exampleIndex][clusterIndex] = 1;
					int j = 0;
					for (Attribute attribute : regularAttributes) {
						clusterMeans[clusterIndex][j] += example.getValue(attribute);
						j++;
					}
					exampleIndex++;
				}
				for (int i = 0; i < k; i++) {
					result.setClusterMean(i, clusterMeans[i]);
				}
				// compute means (normalized), stdDev...)
				computeValuesWithClusterMemberships(exampleSet, k, exampleInClusterProbability, result);
				if (correlated) {
					initCovarianceMatrix(exampleSet, exampleInClusterProbability, result, k);
				}
				break;
			case AVERAGE_PARAMETERS:
			default:
				Random random = RandomGenerator.getRandomGenerator(this);
				initAverageParameters(exampleSet, k, exampleInClusterProbability, result, random, correlated);
				break;
		}

		// show probabilities in example table?
		if (showProbs) {
			if (exampleSet.getAttributes().specialSize() == initSpecialSize) {
				for (int i = 0; i < k; i++) {
					String name = "cluster_" + i + "_probability";
					Attribute newAttribute = AttributeFactory.createAttribute(Ontology.REAL);
					newAttribute.setName(name);
					exampleSet.getExampleTable().addAttribute(newAttribute);
					exampleSet.getAttributes().setSpecialAttribute(newAttribute, name);
				}
				setProbabilitiesInTable(exampleSet, exampleInClusterProbability);
			}
		}
	}

	/*
	 * !This method does not work alone!
	 *
	 * Computes the initial mean, standard deviation and cluster probabilities, for given initial
	 * cluster classifications and means already summed up.
	 */
	private void computeValuesWithClusterMemberships(ExampleSet exampleSet, int k, double[][] exampleInClusterProbability,
			FlatFuzzyClusterModel result) {
		// compute means
		for (int i = 0; i < k; i++) {
			int denominator = 0;
			for (int j = 0; j < exampleInClusterProbability.length; j++) {
				if (exampleInClusterProbability[j][i] == 1) {
					denominator++;
				}
			}
			double[] clusterMean = new double[result.getClusterMean(i).length];
			for (int j = 0; j < result.getClusterMean(i).length; j++) {
				clusterMean[j] = result.getClusterMean(i)[j] / denominator;
			}
			result.setClusterMean(i, clusterMean);
		}

		// compute standard deviations (& cluster probabilities)
		for (int i = 0; i < k; i++) {
			int denominator = 0;
			double clusterStDeviation = 0;
			for (int j = 0; j < exampleInClusterProbability.length; j++) {
				if (exampleInClusterProbability[j][i] == 1) {
					double[] helpVector = VectorMath.vectorSubtraction(exampleToArray(exampleSet.getExample(j)),
							result.getClusterMean(i));
					clusterStDeviation += VectorMath.vectorMultiplication(helpVector, helpVector);
					denominator++;
				}
			}
			result.setClusterStandardDeviation(i, clusterStDeviation / denominator);
			result.setClusterProbability(i, (double) denominator / exampleSet.size());
		}
	}

	/*
	 * compute examplesInClusterProbability [P(C_i|x)] to initialize clusterCovarianceMatrix
	 * [Sigma_i]
	 */
	private void initCovarianceMatrix(ExampleSet exampleSet, double[][] exampleInClusterProbability,
			FlatFuzzyClusterModel result, int k) {
		// compute examplesInClusterProbabilities [P(C_i|x)] (the probabilities for each example
		// with each cluster)
		expectationNonCorrelated(exampleSet, k, exampleInClusterProbability, result);
		// init clusterCovarianceMatrix [Sigma_i]
		computeCovarianceMatrix(exampleSet, exampleInClusterProbability, result, k);
		result.clearClusterStandardDeviations();
	}

	/*
	 * Initialize means, standard deviations and cluster probabilities, by computing the averages of
	 * this values over the exampleSet.
	 */
	private void initAverageParameters(ExampleSet exampleSet, int k, double[][] exampleInClusterProbability,
									   FlatFuzzyClusterModel result, Random random, boolean correlated) {
		// various initializations
		double[] max = new double[exampleSet.getAttributes().size()];
		double[] min = new double[max.length];
		double[] average = new double[max.length];
		exampleSet.recalculateAllAttributeStatistics();
		int i = 0;
		for (Attribute attribute : exampleSet.getAttributes()) {
			max[i] = exampleSet.getStatistics(attribute, Statistics.MAXIMUM);
			min[i] = exampleSet.getStatistics(attribute, Statistics.MINIMUM);
			average[i] = exampleSet.getStatistics(attribute, Statistics.AVERAGE);
			i++;
		}

		// make it random (to get different initializations for the different iterations)
		double[] offset = VectorMath.vectorDivision(VectorMath.vectorSubtraction(max, min), k * 2);

		min = VectorMath.vectorAddition(min, getOffset(offset, random));
		average = VectorMath.vectorAddition(average, getOffset(offset, random));
		max = VectorMath.vectorAddition(max, getOffset(offset, random));

		// compute average means, standard deviations
		double[] help = VectorMath.vectorSubtraction(average, min);
		help = VectorMath.vectorDivision(help, k / 2 + 1);
		double[] help2 = VectorMath.vectorSubtraction(max, average);
		help2 = VectorMath.vectorDivision(help2, k / 2 + 1);

		int j = 0;
		for (i = 0; i < k; i++) {
			double[] clusterMean;
			double clusterStDeviation;
			double clusterProbability;
			if (i < k / 2) {
				clusterMean = VectorMath.vectorAddition(VectorMath.vectorMultiplication(help, i + 1), min);
				clusterStDeviation = VectorMath.vectorMultiplication(help, help);
			} else if (i == k / 2 && k % 2 == 1) {
				clusterMean = average;

				double[] help3 = VectorMath.vectorMultiplication(help, -1);
				clusterStDeviation = VectorMath.vectorMultiplication(help3, help3);
				clusterStDeviation += VectorMath.vectorMultiplication(help2, help2);
				clusterStDeviation = clusterStDeviation / 2;
			} else {
				clusterMean = VectorMath.vectorAddition(average, VectorMath.vectorMultiplication(help2, j + 1));
				clusterStDeviation = VectorMath.vectorMultiplication(help2, help2);

				j++;
			}
			// set all cluster probabilities to the same value
			clusterProbability = (double) 1 / k;

			result.setClusterMean(i, clusterMean);
			result.setClusterStandardDeviation(i, clusterStDeviation);
			result.setClusterProbability(i, clusterProbability);
		}

		if (correlated) {
			initCovarianceMatrix(exampleSet, exampleInClusterProbability, result, k);
		}

	}

	/*
	 * Computes a random offset within a range. (range: (+/- input offset)
	 */
	private double[] getOffset(double[] offset, Random random) {
		double multi = 2 * random.nextDouble() - 1; // number between -1 and 1
		return VectorMath.vectorMultiplication(offset, multi);
	}

	/*
	 * END: INIT SECTOR
	 */

	/*
	 * Computes to which cluster an example fits best.
	 */
	protected int bestIndex(int exampleIndex, int k, double[][] exampleInClusterProbability) throws Exception {
		int bestIndex = -1;
		double bestIndexValue = 0;
		for (int i = 0; i < k; i++) {
			if (bestIndexValue < exampleInClusterProbability[exampleIndex][i]) {
				bestIndexValue = exampleInClusterProbability[exampleIndex][i];
				bestIndex = i;
			}
		}
		return bestIndex;
	}

	/*
	 * Computes the probabilities for each example with each cluster (exampleClusterProbs). (with
	 * StdDev)
	 */
	protected void expectationNonCorrelated(ExampleSet exampleSet, int k, double[][] exampleInClusterProbability,
			FlatFuzzyClusterModel oldResult) {
		int j = 0;
		for (Example ex : exampleSet) {
			double sum = 0;
			for (int i = 0; i < k; i++) {
				double[] helpVector = VectorMath.vectorSubtraction(exampleToArray(ex), oldResult.getClusterMean(i));
				double stDev = oldResult.getClusterStandardDeviation(i);
				// stDev must be greater than 0: division by zero
				if (stDev == 0) {
					stDev = 1E-10;
				}
				// formula see: http://jmlr.csail.mit.edu/papers/volume6/banerjee05b/banerjee05b.pdf
				// (page 1725 + 1729)
				exampleInClusterProbability[j][i] = 1
						/ Math.sqrt(Math.pow(2 * Math.PI * stDev, exampleSet.getAttributes().size()))
						* Math.exp(-1 * (VectorMath.vectorMultiplication(helpVector, helpVector) / (2 * stDev)))
						* oldResult.getClusterProbability(i);
				sum += exampleInClusterProbability[j][i];
			}
			for (int i = 0; i < k; i++) {
				exampleInClusterProbability[j][i] = exampleInClusterProbability[j][i] / sum;
			}
			j++;
		}
	}

	/*
	 * Computes the probabilities for each example with each cluster (exampleClusterProbs). (with
	 * covarianceMatrix)
	 */
	protected void expectationCorrelated(ExampleSet exampleSet, int k, double[][] exampleInClusterProbability,
			FlatFuzzyClusterModel oldResult) throws Exception {
		int j = 0;
		for (Example ex : exampleSet) {
			double sum = 0;
			Vector<Integer> problems = new Vector<Integer>();
			for (int i = 0; i < k; i++) {
				double[] helpVector = VectorMath.vectorSubtraction(exampleToArray(ex), oldResult.getClusterMean(i));

				double[][] helpMatrix = new double[helpVector.length][1];
				for (int l = 0; l < helpVector.length; l++) {
					helpMatrix[l][0] = helpVector[l];
				}
				Matrix matrix = new Matrix(helpMatrix);

				matrix = matrix.transpose().times(new Matrix(oldResult.getClusterCovarianceMatrix(i)).inverse()) // invCovMatrix
						.times(matrix);

				double secondPart = Math.exp(matrix.getArray()[0][0] * -0.5);

				double determinant = new Matrix(oldResult.getClusterCovarianceMatrix(i)).det();

				if (determinant < 0) {
					determinant *= -1;
				}
				// this is here(!) only the conditional probability: P(Example_j|Cluster_i) * W_i
				exampleInClusterProbability[j][i] = 1
						/ Math.sqrt(Math.pow(2 * Math.PI, exampleSet.getAttributes().size()) * determinant) * secondPart
						* oldResult.getClusterProbability(i);

				if (exampleInClusterProbability[j][i] == Double.POSITIVE_INFINITY) {
					problems.add(i);
				}
				// sum = P[x] -> probability of a example
				sum += exampleInClusterProbability[j][i];
			}
			// sometimes double is not able to represent the exampleInClusterProbability, that is
			// only the case if the
			// probabilitity is very closely to 1, when this happens the probability is set to 1 and
			// all others to 0.
			for (int i = 0; i < k; i++) {
				if (problems.isEmpty()) {
					exampleInClusterProbability[j][i] = exampleInClusterProbability[j][i] / sum;
				} else {
					if (exampleInClusterProbability[j][i] == Double.POSITIVE_INFINITY) {
						exampleInClusterProbability[j][i] = 1.0;
					} else {
						exampleInClusterProbability[j][i] = 0.0;
					}
				}
			}
			j++;
		}
	}

	/*
	 * Computes the new values of: - cluster means [my_i] AND - clusterprobabilities [P(Cluster_i)]
	 * AND - cluster standard deviation [sigma_i] OR - cluster covariance matrix [Sigma_i] with the
	 * probabilities of each example to each cluster [P(Cluster_i|example)]
	 */
	protected void maximization(ExampleSet exampleSet, int k, double[][] exampleInClusterProbability, FlatFuzzyClusterModel result, boolean correlated) {
		for (int i = 0; i < k; i++) {
			double probabilitySum = 0;
			int j = 0;
			double[] clusterMean = new double[exampleSet.getAttributes().size()];
			for (Example example : exampleSet) {
				probabilitySum += exampleInClusterProbability[j][i];
				clusterMean = VectorMath.vectorAddition(clusterMean,
						VectorMath.vectorMultiplication(exampleToArray(example), exampleInClusterProbability[j][i]));
				j++;
			}
			result.setClusterMean(i, VectorMath.vectorDivision(clusterMean, probabilitySum));
			result.setClusterProbability(i, probabilitySum / exampleSet.size());

			if (!correlated) {
				j = 0;
				double clusterStDeviation = 0;
				for (Example example : exampleSet) {
					double[] helpVector = VectorMath.vectorSubtraction(exampleToArray(example), result.getClusterMean(i));
					clusterStDeviation += exampleInClusterProbability[j][i]
							* VectorMath.vectorMultiplication(helpVector, helpVector);
					j++;
				}
				result.setClusterStandardDeviation(i, clusterStDeviation / probabilitySum);
			}
		}
		if (correlated) {
			computeCovarianceMatrix(exampleSet, exampleInClusterProbability, result, k);
		}
	}

	/*
	 * Computes clusterCovarianceMatrix. [Sigma_i]
	 */
	private void computeCovarianceMatrix(ExampleSet exampleSet, double[][] exampleInClusterProbability,
			FlatFuzzyClusterModel result, int k) {
		for (int i = 0; i < k; i++) {
			Matrix matrix_old = null;
			Matrix matrix = null;
			double probSum = 0;
			int id = 0;
			for (Example example : exampleSet) {
				double[] helpVector = VectorMath.vectorSubtraction(exampleToArray(example), result.getClusterMean(i));
				double[][] helpMatrix = new double[helpVector.length][1];
				for (int j = 0; j < helpVector.length; j++) {
					helpMatrix[j][0] = helpVector[j];
				}
				matrix = new Matrix(helpMatrix);
				matrix = matrix.times(matrix.transpose()).times(exampleInClusterProbability[id][i]);
				probSum += exampleInClusterProbability[id][i];

				if (matrix_old != null) {
					matrix = matrix_old.plus(matrix);
				}
				matrix_old = matrix;
				id++;
			}
			double[][] covarianceMatrix = matrix.getArray();
			covarianceMatrix = VectorMath.matrixDivision(covarianceMatrix, probSum);
			result.setClusterCovarianceMatrix(i, covarianceMatrix);
		}
	}

	/*
	 * Computes the loglikelihood.
	 */
	protected double computeLogLikelihood(int k, double[][] exampleInClusterProbability, FlatFuzzyClusterModel resultModel) {
		double result = 0;
		double temp = 0;
		for (int n = 0; n < exampleInClusterProbability.length; n++) {
			for (int i = 0; i < k; i++) {
				temp += resultModel.getClusterProbability(i) * exampleInClusterProbability[n][i];
			}
			result += Math.log(temp);
		}
		return result;
	}

	/*
	 * Sets the cluster probabilities in the table, according to the actual values in
	 * exampleClusterProbs.
	 */
	private void setProbabilitiesInTable(ExampleSet exampleSet, double[][] exampleInClusterProbability) throws OperatorException {
		int k = getParameterAsInt(PARAMETER_K);
		for (int i = 0; i < k; i++) {
			String name = "cluster_" + i + "_probability";
			int j = 0;
			for (Example ex : exampleSet) {
				ex.setValue(exampleSet.getAttributes().get(name), exampleInClusterProbability[j][i]);
				j++;
			}
		}
	}

	/*
	 * Computes an array of an example. Important for some math operations.
	 */
	private double[] exampleToArray(Example example) {
		double[] result = new double[example.getAttributes().size()];
		int i = 0;
		for (Attribute attribute : example.getAttributes()) {
			result[i] = example.getValue(attribute);
			i++;
		}
		return result;
	}

	@Override
	protected ClusterModel generateInternalClusterModel(ExampleSet exampleSet) throws OperatorException {
		// get parameters
		int k = getParameterAsInt(PARAMETER_K);

		// perform checks
		Tools.isNonEmpty(exampleSet);
		Tools.onlyNonMissingValues(exampleSet, getOperatorClassName(), this, new String[0]);
		Tools.checkAndCreateIds(exampleSet);

		if (exampleSet.size() < k) {
			logWarning("number of clusters (k) = " + k + " > number of objects =" + exampleSet.size());
			throw new UserError(this, 142, k);
		}

		return createClusterModel(exampleSet);

	}

	@Override
	protected boolean handlesInfiniteValues() {
		return false;
	}

	@Override
	public List<ParameterType> getParameterTypes() {
		List<ParameterType> types = new LinkedList<ParameterType>();

		ParameterType type = new ParameterTypeInt(PARAMETER_K, "The number of clusters which should be found.", 2,
				Integer.MAX_VALUE, 2);
		type.setExpert(false);
		types.add(type);

		types.addAll(super.getParameterTypes());

		types.add(new ParameterTypeInt(PARAMETER_MAX_RUNS,
				"The maximal number of runs of this operator with random initialization that are performed.", 1,
				Integer.MAX_VALUE, 5, false));
		types.add(new ParameterTypeInt(PARAMETER_MAX_OPTIMIZATION_STEPS,
				"The maximal number of iterations performed for one run of this operator.", 1, Integer.MAX_VALUE, 100,
				false));
		types.add(new ParameterTypeDouble(PARAMETER_QUALITY,
				"The quality that must be fullfilled before the algorithm stops. (The rising of the loglikelihood that must be undercut)",
				1.0E-15, 1.0E-1, 1.0E-10));

		types.addAll(RandomGenerator.getRandomGeneratorParameters(this));

		types.add(new ParameterTypeBoolean(PARAMETER_SHOW_PROBABILITIES,
				"Insert probabilities for every cluster with every example in the example set.", true));
		types.add(new ParameterTypeCategory(PARAMETER_INITIALIZATION_DISTRIBUTION,
				"Indicates the inital distribution of the centroids.", INIT_DISTRIBUTION, K_MEANS));
		addDistMeasureParams(types);
		types.add(new ParameterTypeBoolean(PARAMETER_CORRELATED,
				"Has to be activated, if the example set contains correlated attributes.", true));

		return types;
	}

	private void addDistMeasureParams(List<ParameterType> types) {
		List<ParameterType> measureParams = DistanceMeasures.getParameterTypesForNumAndDiv(this);
		ParameterType param = measureParams.get(0);
		param.registerDependencyCondition(new AboveOperatorVersionCondition(this, BEFORE_MEASURE_PARAM));
		param.setDefaultValue(DistanceMeasures.DIVERGENCES_TYPE - DistanceMeasures.NUMERICAL_MEASURES_TYPE);
		param.registerDependencyCondition(
				new EqualTypeCondition(this, PARAMETER_INITIALIZATION_DISTRIBUTION, INIT_DISTRIBUTION, false, K_MEANS));
		measureParams.get(2).setDefaultValue(6);
		measureParams.forEach(p -> p.setExpert(true));
		types.addAll(measureParams);
	}

	@Override
	public OperatorVersion[] getIncompatibleVersionChanges() {
		OperatorVersion[] old = super.getIncompatibleVersionChanges();
		OperatorVersion[] newVersions = new OperatorVersion[old.length + 1];
		if (old.length != 0) {
			System.arraycopy(old, 0, newVersions, 0, old.length);
		}
		newVersions[old.length] = BEFORE_MEASURE_PARAM;
		return newVersions;
	}

}
