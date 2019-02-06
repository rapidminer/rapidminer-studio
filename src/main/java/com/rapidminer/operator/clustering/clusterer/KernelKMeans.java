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
import java.util.Collections;
import java.util.List;

import com.rapidminer.example.Attribute;
import com.rapidminer.example.Attributes;
import com.rapidminer.example.Example;
import com.rapidminer.example.ExampleSet;
import com.rapidminer.example.Tools;
import com.rapidminer.operator.OperatorCapability;
import com.rapidminer.operator.OperatorDescription;
import com.rapidminer.operator.OperatorException;
import com.rapidminer.operator.ProcessSetupError.Severity;
import com.rapidminer.operator.UserError;
import com.rapidminer.operator.clustering.ClusterModel;
import com.rapidminer.operator.ports.metadata.ExampleSetMetaData;
import com.rapidminer.operator.ports.metadata.MetaData;
import com.rapidminer.operator.ports.metadata.MetaDataInfo;
import com.rapidminer.operator.ports.metadata.SimplePrecondition;
import com.rapidminer.operator.ports.quickfix.ParameterSettingQuickFix;
import com.rapidminer.operator.ports.quickfix.QuickFix;
import com.rapidminer.parameter.ParameterType;
import com.rapidminer.parameter.ParameterTypeBoolean;
import com.rapidminer.parameter.ParameterTypeInt;
import com.rapidminer.tools.RandomGenerator;
import com.rapidminer.tools.math.kernels.Kernel;


/**
 * This operator is an implementation of kernel k means. Kernel K Means uses kernels to estimate
 * distance between objects and clusters. Because of the nature of kernels it is necessary to sum
 * over all elements of a cluster to calculate one distance. So this algorithm is quadratic in
 * number of examples and returns NO CentroidClusterModel, as its older brother KMeans does. This
 * operator will create a cluster attribute if not present yet.
 *
 * @author Sebastian Land
 */
public class KernelKMeans extends RMAbstractClusterer {

	/** The parameter name for &quot;the maximal number of clusters&quot; */
	public static final String PARAMETER_K = "k";

	/** The parameter name for &quot;the decision if exampleweights should be used &quot; */
	public static final String PARAMETER_USE_WEIGHTS = "use_weights";

	/**
	 * The parameter name for &quot;the maximal number of iterations performed for one run of the k
	 * method&quot;
	 */
	public static final String PARAMETER_MAX_OPTIMIZATION_STEPS = "max_optimization_steps";

	private static final int OPERATOR_PROGRESS_STEPS = 200;
	private List<? extends QuickFix> algoWeightQuickFix = Collections
			.singletonList(new ParameterSettingQuickFix(this, PARAMETER_USE_WEIGHTS, Boolean.toString(true)));

	public KernelKMeans(OperatorDescription description) {
		super(description);
		getExampleSetInputPort()
				.addPrecondition(new SimplePrecondition(getExampleSetInputPort(), new ExampleSetMetaData(), false) {

					@Override
					public void makeAdditionalChecks(MetaData received) {
						if (!(received instanceof ExampleSetMetaData)) {
							return;
						}
						ExampleSetMetaData emd = (ExampleSetMetaData) received;
						if (emd.hasSpecial(Attributes.WEIGHT_NAME) == MetaDataInfo.YES
								&& !getParameterAsBoolean(PARAMETER_USE_WEIGHTS)) {
							createError(Severity.WARNING, algoWeightQuickFix, "learner_does_not_support_weights");
						}
					}
				});
	}

	@Override
	protected boolean checksForRegularAttributes() {
		return getCompatibilityLevel().isAbove(BEFORE_EMPTY_CHECKS);
	}

	@Override
	protected boolean affectedByEmptyCheck() {
		return true;
	}

	@Override
	protected ClusterModel generateInternalClusterModel(ExampleSet exampleSet) throws OperatorException {
		int k = getParameterAsInt(PARAMETER_K);
		int maxOptimizationSteps = getParameterAsInt(PARAMETER_MAX_OPTIMIZATION_STEPS);
		boolean useExampleWeights = getParameterAsBoolean(PARAMETER_USE_WEIGHTS);
		Kernel kernel = Kernel.createKernel(this);

		// init operator progress
		getProgress().setTotal(100);

		// checking and creating ids if necessary
		Tools.checkAndCreateIds(exampleSet);

		// additional checks
		Tools.onlyNonMissingValues(exampleSet, getOperatorClassName(), this, new String[0]);

		if (exampleSet.size() < k) {
			throw new UserError(this, 142, k);
		}

		// extracting attribute names
		Attributes attributes = exampleSet.getAttributes();
		ArrayList<String> attributeNames = new ArrayList<String>(attributes.size());
		for (Attribute attribute : attributes) {
			attributeNames.add(attribute.getName());
		}
		Attribute weightAttribute = attributes.getWeight();

		RandomGenerator generator = RandomGenerator.getRandomGenerator(this);

		ClusterModel model = new ClusterModel(exampleSet, k, addsLabelAttribute(),
				getParameterAsBoolean(RMAbstractClusterer.PARAMETER_REMOVE_UNLABELED));
		// init centroids
		int[] clusterAssignments = new int[exampleSet.size()];

		for (int i = 0; i < exampleSet.size(); i++) {
			clusterAssignments[i] = generator.nextIntInRange(0, k);
		}

		// run optimization steps
		boolean stable = false;
		for (int step = 0; step < maxOptimizationSteps && !stable; step++) {
			// calculating cluster kernel properties
			double[] clusterWeights = new double[k];
			double[] clusterKernelCorrection = new double[k];
			int i = 0;
			for (Example firstExample : exampleSet) {
				double firstExampleWeight = useExampleWeights ? firstExample.getValue(weightAttribute) : 1d;
				double[] firstExampleValues = getAsDoubleArray(firstExample, attributes);
				clusterWeights[clusterAssignments[i]] += firstExampleWeight;
				int j = 0;
				for (Example secondExample : exampleSet) {
					if (clusterAssignments[i] == clusterAssignments[j]) {
						double secondExampleWeight = useExampleWeights ? secondExample.getValue(weightAttribute) : 1d;
						clusterKernelCorrection[clusterAssignments[i]] += firstExampleWeight * secondExampleWeight
								* kernel.calculateDistance(firstExampleValues, getAsDoubleArray(secondExample, attributes));
					}
					j++;
				}
				i++;
			}
			for (int z = 0; z < k; z++) {
				clusterKernelCorrection[z] /= clusterWeights[z] * clusterWeights[z];
			}

			// assign examples to new centroids
			int[] newClusterAssignments = new int[exampleSet.size()];
			i = 0;
			for (Example example : exampleSet) {
				double[] exampleValues = getAsDoubleArray(example, attributes);
				double exampleKernelValue = kernel.calculateDistance(exampleValues, exampleValues);
				double nearestDistance = Double.POSITIVE_INFINITY;
				int nearestIndex = 0;
				for (int clusterIndex = 0; clusterIndex < k; clusterIndex++) {
					double distance = 0;
					// iterating over all examples in cluster to get kernel distance
					int j = 0;
					for (Example clusterExample : exampleSet) {
						if (clusterAssignments[j] == clusterIndex) {
							distance += (useExampleWeights ? clusterExample.getValue(weightAttribute) : 1d)
									* kernel.calculateDistance(getAsDoubleArray(clusterExample, attributes), exampleValues);
						}
						j++;
					}
					distance *= -2d / clusterWeights[clusterIndex];
					// copy in outer loop
					distance += exampleKernelValue;
					distance += clusterKernelCorrection[clusterIndex];
					if (distance < nearestDistance) {
						nearestDistance = distance;
						nearestIndex = clusterIndex;
					}
				}
				newClusterAssignments[i] = nearestIndex;
				i++;

				// trigger operator progress
				if (i % OPERATOR_PROGRESS_STEPS == 0) {
					getProgress()
							.setCompleted((int) (100.0 * (step + (double) i / exampleSet.size()) / maxOptimizationSteps));
				}
			}

			// finishing assignment
			stable = true;
			for (int j = 0; j < exampleSet.size() && stable; j++) {
				stable &= newClusterAssignments[j] == clusterAssignments[j];
			}
			clusterAssignments = newClusterAssignments;

			// trigger operator progress
			getProgress().setCompleted((int) (100.0 * (step + 1.0) / maxOptimizationSteps));
		}

		// setting last clustering into model
		model.setClusterAssignments(clusterAssignments, exampleSet);

		if (addsClusterAttribute()) {
			addClusterAssignments(exampleSet, clusterAssignments);
		}

		getProgress().complete();
		return model;
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
	public boolean supportsCapability(OperatorCapability capability) {
		if (capability == OperatorCapability.WEIGHTED_EXAMPLES) {
			return true;
		}
		return super.supportsCapability(capability);
	}

	@Override
	public List<ParameterType> getParameterTypes() {
		List<ParameterType> types = super.getParameterTypes();
		types.add(new ParameterTypeBoolean(PARAMETER_USE_WEIGHTS, "Indicates if the weight attribute should be used.", false,
				false));
		types.add(new ParameterTypeInt(PARAMETER_K, "The number of clusters which should be detected.", 2, Integer.MAX_VALUE,
				5, false));
		types.add(new ParameterTypeInt(PARAMETER_MAX_OPTIMIZATION_STEPS,
				"The maximal number of iterations performed for one run of k-Means.", 1, Integer.MAX_VALUE, 100, false));

		types.addAll(RandomGenerator.getRandomGeneratorParameters(this));

		types.addAll(Kernel.getParameters(this));
		return types;
	}
}
