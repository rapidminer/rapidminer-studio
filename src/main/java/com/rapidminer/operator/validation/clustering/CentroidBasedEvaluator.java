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
package com.rapidminer.operator.validation.clustering;

import java.util.ArrayList;
import java.util.List;
import java.util.logging.Level;

import org.apache.commons.lang.ArrayUtils;

import com.rapidminer.example.Example;
import com.rapidminer.example.ExampleSet;
import com.rapidminer.operator.Operator;
import com.rapidminer.operator.OperatorDescription;
import com.rapidminer.operator.OperatorException;
import com.rapidminer.operator.OperatorVersion;
import com.rapidminer.operator.ValueDouble;
import com.rapidminer.operator.clustering.CentroidClusterModel;
import com.rapidminer.operator.performance.EstimatedPerformance;
import com.rapidminer.operator.performance.PerformanceCriterion;
import com.rapidminer.operator.performance.PerformanceVector;
import com.rapidminer.operator.ports.InputPort;
import com.rapidminer.operator.ports.OutputPort;
import com.rapidminer.operator.ports.metadata.GenerateNewMDRule;
import com.rapidminer.operator.ports.metadata.MetaData;
import com.rapidminer.operator.ports.metadata.PassThroughOrGenerateRule;
import com.rapidminer.operator.ports.metadata.SimplePrecondition;
import com.rapidminer.parameter.ParameterType;
import com.rapidminer.parameter.ParameterTypeBoolean;
import com.rapidminer.parameter.ParameterTypeCategory;
import com.rapidminer.tools.I18N;
import com.rapidminer.tools.LogService;
import com.rapidminer.tools.math.similarity.DistanceMeasure;
import com.rapidminer.tools.math.similarity.divergences.SquaredEuclideanDistance;
import com.rapidminer.tools.math.similarity.numerical.EuclideanDistance;


/**
 * An evaluator for centroid based clustering methods. The average within cluster distance is
 * calculated by averaging the distance between the centroid and all examples of a cluster.
 *
 * @author Sebastian Land, Michael Wurst, Ingo Mierswa
 *
 */
public class CentroidBasedEvaluator extends Operator {

	public static final String PARAMETER_MAIN_CRITERION = "main_criterion";

	public static final String PARAMETER_MAIN_CRITERION_ONLY = "main_criterion_only";

	public static final String PARAMETER_NORMALIZE = "normalize";

	public static final String PARAMETER_MAXIMIZE = "maximize";

	/**
	 * The davies bouldin index for cluster models with empty clusters was infinity for versions <= this version. In
	 * newer versions empty clusters are ignored. If there are <= 1 non-empty clusters the davies bouldin index is NaN.
	 */
	public static final OperatorVersion VERSION_FAILS_ON_EMPTY_CLUSTERS = new OperatorVersion(9, 3, 1);

	private double avgWithinClusterDistance;

	private double daviesBouldin;

	public static final String[] CRITERIA_LIST = {"Avg. within centroid distance", "Davies Bouldin"};

	public static final String[] CRITERIA_LIST_SHORT = {"avg_within_distance", "DaviesBouldin"};

	private InputPort exampleSetInput = getInputPorts().createPort("example set", ExampleSet.class);
	private InputPort clusterModelInput = getInputPorts().createPort("cluster model", CentroidClusterModel.class);
	private InputPort performanceInput = getInputPorts().createPort("performance");
	private OutputPort performanceOutput = getOutputPorts().createPort("performance");
	private OutputPort exampleSetOutput = getOutputPorts().createPort("example set");
	private OutputPort clusterModelOutput = getOutputPorts().createPort("cluster model");

	/**
	 * Constructor for ClusterDensityEvaluator.
	 */
	public CentroidBasedEvaluator(OperatorDescription description) {
		super(description);
		performanceInput.addPrecondition(new SimplePrecondition(performanceInput, new MetaData(PerformanceVector.class),
				false));
		getTransformer().addRule(
				new PassThroughOrGenerateRule(performanceInput, performanceOutput, new MetaData(PerformanceVector.class)));
		getTransformer().addPassThroughRule(exampleSetInput, exampleSetOutput);
		getTransformer().addPassThroughRule(clusterModelInput, clusterModelOutput);
		getTransformer().addRule(new GenerateNewMDRule(performanceOutput, PerformanceVector.class));

		addValue(new ValueDouble(CRITERIA_LIST_SHORT[0], CRITERIA_LIST[0], false) {

			@Override
			public double getDoubleValue() {
				return avgWithinClusterDistance;
			}
		});

		addValue(new ValueDouble(CRITERIA_LIST_SHORT[1], CRITERIA_LIST[1], false) {

			@Override
			public double getDoubleValue() {
				return daviesBouldin;
			}
		});

	}

	@Override
	public boolean shouldAutoConnect(OutputPort port) {
		if (port == clusterModelOutput) {
			return getParameterAsBoolean("keep_cluster_model");
		} else if (port == exampleSetOutput) {
			return getParameterAsBoolean("keep_example_set");
		} else {
			return super.shouldAutoConnect(port);
		}
	}

	@Override
	public void doWork() throws OperatorException {
		CentroidClusterModel clusterModel = clusterModelInput.getData(CentroidClusterModel.class);
		ExampleSet exampleSet = exampleSetInput.getData(ExampleSet.class);

		PerformanceVector performance = performanceInput.getDataOrNull(PerformanceVector.class);
		if (performance == null) {
			performance = new PerformanceVector();
		}

		int mainCriterionIndex = getParameterAsInt(PARAMETER_MAIN_CRITERION);
		boolean onlyMainCriterion = getParameterAsBoolean(PARAMETER_MAIN_CRITERION_ONLY);

		double multFactor = -1.0;
		if (getParameterAsBoolean(PARAMETER_MAXIMIZE)) {
			multFactor = 1.0;
		}

		double divisionFactor = 1.0;
		if (getParameterAsBoolean(PARAMETER_NORMALIZE)) {
			divisionFactor = exampleSet.getAttributes().size();
		}

		// Average Squared within cluster distance 0
		double[] averageWithinDistance = getAverageWithinDistance(clusterModel, exampleSet);

		avgWithinClusterDistance = averageWithinDistance[clusterModel.getNumberOfClusters()];
		PerformanceCriterion withinClusterDist = new EstimatedPerformance(CRITERIA_LIST[0],
				(multFactor * avgWithinClusterDistance) / divisionFactor, 1, false);
		if ((mainCriterionIndex == 0) || !onlyMainCriterion) {
			performance.addCriterion(withinClusterDist);
		}

		for (int i = 0; i < clusterModel.getNumberOfClusters(); i++) {
			PerformanceCriterion withinDistance = new EstimatedPerformance(CRITERIA_LIST[0] + "_cluster_"
					+ clusterModel.getCluster(i).getClusterId(), (multFactor * averageWithinDistance[i]) / divisionFactor,
					1, false);
			if ((mainCriterionIndex == 0) || !onlyMainCriterion) {
				performance.addCriterion(withinDistance);
			}
		}

		// Davies Bouldin 1
		daviesBouldin = getDaviesBouldin(clusterModel, exampleSet, this);
		PerformanceCriterion daviesBouldinCriterion = new EstimatedPerformance(CRITERIA_LIST[1],
				(multFactor * daviesBouldin) / divisionFactor, 1, false);
		if ((mainCriterionIndex == 1) || !onlyMainCriterion) {
			performance.addCriterion(daviesBouldinCriterion);
		}

		performance.setMainCriterionName(CRITERIA_LIST[mainCriterionIndex]);

		performanceOutput.deliver(performance);
		exampleSetOutput.deliver(exampleSet);
		clusterModelOutput.deliver(clusterModel);
	}

	@Override
	public List<ParameterType> getParameterTypes() {
		List<ParameterType> types = super.getParameterTypes();
		types.add(new ParameterTypeCategory(PARAMETER_MAIN_CRITERION, "The main criterion to use", CRITERIA_LIST, 0, false));
		types.add(new ParameterTypeBoolean(PARAMETER_MAIN_CRITERION_ONLY, "return the main criterion only", false));
		types.add(new ParameterTypeBoolean(PARAMETER_NORMALIZE, "divide the criterion by the number of features", false));
		types.add(new ParameterTypeBoolean(PARAMETER_MAXIMIZE, "do not multiply the result by minus one", false));
		return types;
	}

	@Override
	public OperatorVersion[] getIncompatibleVersionChanges() {
		return (OperatorVersion[]) ArrayUtils.add(super.getIncompatibleVersionChanges(),
				VERSION_FAILS_ON_EMPTY_CLUSTERS);
	}

	/**
	 * Calculates and delivers the average within distances for all clusters and the given data set.
	 * The last entry of the resulting array is the average across all clusters.
	 *
	 * @param model
	 * 		the cluster model
	 * @param exampleSet
	 * 		the clustered data
	 * @return the average within distance for all clusters plus the total avg within distance (last entry)
	 * @since 8.2
	 */
	public static double[] getAverageWithinDistance(CentroidClusterModel model, ExampleSet exampleSet) throws OperatorException {
		DistanceMeasure measure = new SquaredEuclideanDistance();
		measure.init(exampleSet);
		int numberOfClusters = model.getNumberOfClusters();

		// counting distances within
		double[] result = new double[numberOfClusters + 1];
		int[] clusterSizes = new int[numberOfClusters];

		int[] clusterIndices = model.getClusterAssignments(exampleSet);

		int i = 0;
		for (Example example : exampleSet) {
			clusterSizes[clusterIndices[i]]++;
			result[clusterIndices[i]] += measure.calculateDistance(example, model.getCentroidCoordinates(clusterIndices[i]));
			i++;
		}

		// averaging by cluster sizes and sum over all
		int totalSize = 0;
		for (i = 0; i < numberOfClusters; i++) {
			result[numberOfClusters] += result[i];
			result[i] /= clusterSizes[i];
			totalSize += clusterSizes[i];
		}
		if (totalSize != 0) {
			result[numberOfClusters] /= totalSize;
		}

		return result;
	}

	/**
	 * Calculates and delivers the Davies Bouldin Index for all clusters and the given data set.
	 *
	 * @param model
	 * 		the cluster model
	 * @param exampleSet
	 * 		the clustered data
	 * @return the Davies Bouldin Index for this clustering
	 * @since 8.2
	 * @deprecated This method returns infinity if any of the clusters are empty. Please use {@link
	 * #getDaviesBouldin(CentroidClusterModel, ExampleSet, Operator)} instead.
	 */
	@Deprecated
	public static double getDaviesBouldin(CentroidClusterModel model, ExampleSet exampleSet) throws OperatorException {
		return getDaviesBouldin(model, exampleSet, null);
	}

	/**
	 * Calculates and delivers the Davies Bouldin Index for all non-empty clusters and the given data set. If there are
	 * <= 1 non-empty clusters the Davies Bouldin Index will be {@code NaN}.
	 * <p>
	 * Will also take empty clusters into account if the compatibility level of the specified Operator is <=
	 * {@link #VERSION_FAILS_ON_EMPTY_CLUSTERS} or if the Operator is null.
	 * This leads to infinite values if the cluster model contains any empty clusters.
	 * </p>
	 *
	 * @param model
	 * 		the cluster model
	 * @param exampleSet
	 * 		the clustered data
	 * @param operator
	 * 		used for compatibility level and logging, can be {@code null}.
	 * @return the Davies Bouldin Index for this clustering
	 * @since 9.3.2
	 */
	public static double getDaviesBouldin(CentroidClusterModel model, ExampleSet exampleSet, Operator operator) throws OperatorException {
		DistanceMeasure measure = new EuclideanDistance();
		measure.init(exampleSet);
		int numberOfClusters = model.getNumberOfClusters();

		// counting distances within
		double[] withinClusterDistance = new double[numberOfClusters];
		int[] clusterSizes = new int[numberOfClusters];

		int[] clusterIndices = model.getClusterAssignments(exampleSet);

		int i = 0;
		for (Example example : exampleSet) {
			clusterSizes[clusterIndices[i]]++;
			withinClusterDistance[clusterIndices[i]] += measure.calculateDistance(example,
					model.getCentroidCoordinates(clusterIndices[i]));
			i++;
		}

		// usedClusters will either contain all clusters or only the non-empty ones
		// depending on the operators compatibility level
		boolean useEmptyClusters = operator == null ||
				operator.getCompatibilityLevel().isAtMost(VERSION_FAILS_ON_EMPTY_CLUSTERS);
		List<Integer> usedClusters = new ArrayList<>(numberOfClusters);

		// calculate the within cluster distances
		for (i = 0; i < numberOfClusters; i++) {
			if (clusterSizes[i] > 0 || useEmptyClusters) {
				withinClusterDistance[i] /= clusterSizes[i];
				usedClusters.add(i);
			}
		}

		if (usedClusters.size() != numberOfClusters) {
			logWarning(operator, "process.error.cluster_performance.empty_clusters",
					"Davies Bouldin Index", numberOfClusters - usedClusters.size());
		}
		if (usedClusters.size() <= 1 && !useEmptyClusters) {
			// the davies bouldin index is undefined for <= 1 clusters
			logWarning(operator, "process.error.cluster_performance.not_enough_clusters",
					"Davies Bouldin Index", usedClusters.size());
			return Double.NaN;
		}

		return calcDaviesBouldin(model, measure, withinClusterDistance, usedClusters);
	}

	private static void logWarning(Operator operator, String key, Object... arguments) {
		String message = I18N.getErrorMessage(key, arguments);
		if (operator != null) {
			operator.logWarning(message);
		} else {
			LogService.getRoot().log(Level.WARNING, message);
		}
	}

	/**
	 * Helper method that is used to calculate the Davies Bouldin Index for the clusters specified by clusterIndices.
	 */
	private static double calcDaviesBouldin(CentroidClusterModel model, DistanceMeasure measure, double[] withinClusterDistance, List<Integer> usedClusters) {
		double result = 0.0;
		for (int i : usedClusters) {
			double max = Double.NEGATIVE_INFINITY;
			for (int j : usedClusters) {
				if (i != j) {
					double val = (withinClusterDistance[i] + withinClusterDistance[j])
							/ measure.calculateDistance(model.getCentroidCoordinates(i), model.getCentroidCoordinates(j));
					if (val > max) {
						max = val;
					}
				}
			}
			result = result + max;
		}
		return result / usedClusters.size();
	}
}
