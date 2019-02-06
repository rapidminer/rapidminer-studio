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

import java.util.Arrays;
import java.util.LinkedList;
import java.util.List;

import com.rapidminer.example.Attribute;
import com.rapidminer.example.Example;
import com.rapidminer.example.ExampleSet;
import com.rapidminer.example.Tools;
import com.rapidminer.example.table.NominalMapping;
import com.rapidminer.operator.OperatorCapability;
import com.rapidminer.operator.OperatorDescription;
import com.rapidminer.operator.OperatorException;
import com.rapidminer.operator.clustering.ClusterModel;
import com.rapidminer.operator.learner.functions.kernel.jmysvm.examples.SVMExample;
import com.rapidminer.operator.learner.functions.kernel.jmysvm.kernel.Kernel;
import com.rapidminer.operator.learner.functions.kernel.jmysvm.kernel.KernelDot;
import com.rapidminer.operator.learner.functions.kernel.jmysvm.kernel.KernelNeural;
import com.rapidminer.operator.learner.functions.kernel.jmysvm.kernel.KernelPolynomial;
import com.rapidminer.operator.learner.functions.kernel.jmysvm.kernel.KernelRadial;
import com.rapidminer.parameter.ParameterType;
import com.rapidminer.parameter.ParameterTypeCategory;
import com.rapidminer.parameter.ParameterTypeDouble;
import com.rapidminer.parameter.ParameterTypeInt;
import com.rapidminer.parameter.conditions.EqualTypeCondition;


/**
 * An implementation of Support Vector Clustering based on {@rapidminer.cite BenHur/etal/2001a}.
 * This operator will create a cluster attribute if not present yet.
 *
 * @author Stefan Rueping, Ingo Mierswa, Michael Wurst, Sebastian Land
 */
public class SVClustering extends RMAbstractClusterer {

	public static final String MIN_PTS_NAME = "min_pts";

	/** The parameter name for &quot;The SVM kernel type&quot; */
	public static final String PARAMETER_KERNEL_TYPE = "kernel_type";

	/** The parameter name for &quot;The SVM kernel parameter gamma (radial).&quot; */
	public static final String PARAMETER_KERNEL_GAMMA = "kernel_gamma";

	/** The parameter name for &quot;The SVM kernel parameter degree (polynomial).&quot; */
	public static final String PARAMETER_KERNEL_DEGREE = "kernel_degree";

	/** The parameter name for &quot;The SVM kernel parameter a (neural).&quot; */
	public static final String PARAMETER_KERNEL_A = "kernel_a";

	/** The parameter name for &quot;The SVM kernel parameter b (neural).&quot; */
	public static final String PARAMETER_KERNEL_B = "kernel_b";

	/** The parameter name for &quot;Size of the cache for kernel evaluations im MB &quot; */
	public static final String PARAMETER_KERNEL_CACHE = "kernel_cache";

	/** The parameter name for &quot;Precision on the KKT conditions&quot; */
	public static final String PARAMETER_CONVERGENCE_EPSILON = "convergence_epsilon";

	/** The parameter name for &quot;Stop after this many iterations&quot; */
	public static final String PARAMETER_MAX_ITERATIONS = "max_iterations";

	/** The parameter name for &quot;The fraction of allowed outliers.&quot; */
	public static final String PARAMETER_P = "p";

	/**
	 * The parameter name for &quot;Use this radius instead of the calculated one (-1 for calculated
	 * radius).&quot;
	 */
	public static final String PARAMETER_R = "r";

	/**
	 * The parameter name for &quot;The number of virtual sample points to check for
	 * neighborship.&quot;
	 */
	public static final String PARAMETER_NUMBER_SAMPLE_POINTS = "number_sample_points";
	/** The kernels which can be used from RapidMiner for the mySVM / myKLR. */
	private static final String[] KERNEL_TYPES = { "dot", "radial", "polynomial", "neural" };

	/** Indicates a linear kernel. */
	public static final int KERNEL_DOT = 0;

	/** Indicates a rbf kernel. */
	public static final int KERNEL_RADIAL = 1;

	/** Indicates a polynomial kernel. */
	public static final int KERNEL_POLYNOMIAL = 2;

	/** Indicates a neural net kernel. */
	public static final int KERNEL_NEURAL = 3;

	protected static final int UNASSIGNED = -1;

	public static final int NOISE = 0;

	public static final String NOISE_CLUSTER_DESCRIPTION = "Outliers";

	private static final int OPERATOR_PROGRESS_STEPS = 10;

	private static final double INTERMEDIATE_PROGRESS = 20.0;

	private double paramR;

	private int numSamplePoints;

	public SVClustering(OperatorDescription description) {
		super(description);
	}

	@Override
	protected boolean checksForExamples() {
		return true;
	}

	@Override
	protected ClusterModel generateInternalClusterModel(ExampleSet exampleSet) throws OperatorException {
		// checking and creating ids if necessary
		Tools.checkAndCreateIds(exampleSet);

		// additional checks
		Tools.onlyNonMissingValues(exampleSet, getOperatorClassName(), this, new String[0]);

		// creating kernel
		int kernelType = getParameterAsInt(PARAMETER_KERNEL_TYPE);
		int cacheSize = getParameterAsInt(PARAMETER_KERNEL_CACHE);
		paramR = getParameterAsDouble(PARAMETER_R);
		numSamplePoints = getParameterAsInt(PARAMETER_NUMBER_SAMPLE_POINTS);
		Kernel kernel = createKernel(kernelType);
		if (kernelType == KERNEL_RADIAL) {
			((KernelRadial) kernel).setGamma(getParameterAsDouble(PARAMETER_KERNEL_GAMMA));
		} else if (kernelType == KERNEL_POLYNOMIAL) {
			((KernelPolynomial) kernel).setDegree(getParameterAsInt(PARAMETER_KERNEL_DEGREE));
		} else if (kernelType == KERNEL_NEURAL) {
			((KernelNeural) kernel).setParameters(getParameterAsDouble(PARAMETER_KERNEL_A),
					getParameterAsDouble(PARAMETER_KERNEL_B));
		}
		SVCExampleSet svmExamples = new SVCExampleSet(exampleSet, false);
		kernel.init(svmExamples, cacheSize);

		// initialize progress
		getProgress().setTotal(100);

		// creating kernel using SVClusteringAlgorithm
		SVClusteringAlgorithm clustering = new SVClusteringAlgorithm(this, kernel, svmExamples);
		clustering.train();
		getProgress().setCompleted((int) INTERMEDIATE_PROGRESS);

		// doing neighborhood search for density estimation
		int nextClusterId = 0;
		int minPts = getParameterAsInt(MIN_PTS_NAME);
		int[] clusterAssignments = new int[exampleSet.size()];
		Arrays.fill(clusterAssignments, UNASSIGNED);

		int i = 0;
		for (Example example : exampleSet) {
			if (clusterAssignments[i] == UNASSIGNED) {
				LinkedList<Integer> neighbours = getNeighbours(exampleSet, example, i, clusterAssignments, clustering);
				if (neighbours.size() >= minPts) {
					nextClusterId++;
					clusterAssignments[i] = nextClusterId;
					for (int exampleIndex : neighbours) {
						clusterAssignments[exampleIndex] = nextClusterId;
					}
					while (neighbours.size() > 0) {
						// Take the first index from the queue and fetch indexed example
						int index = neighbours.poll().intValue();
						Example neighbourExample = exampleSet.getExample(index);

						// Find its neighbours and if the density is sufficient
						// recurse through them and
						// assign it the current cluster id
						LinkedList<Integer> neighboursRecursive = getNeighbours(exampleSet, neighbourExample, index,
								clusterAssignments, clustering);
						if (neighboursRecursive.size() >= minPts) {
							for (int recursiveIndex : neighboursRecursive) {
								// If already identified as noise, just assign
								// it, if its unclassified, add to queue
								if (clusterAssignments[recursiveIndex] == UNASSIGNED) {
									neighbours.add(recursiveIndex);
								}
								clusterAssignments[recursiveIndex] = nextClusterId;
							}
						}
					}
				} else {
					clusterAssignments[i] = NOISE;
				}
			}
			if (i++ % OPERATOR_PROGRESS_STEPS == 0) {
				getProgress().setCompleted(
						(int) (INTERMEDIATE_PROGRESS + (100.0 - INTERMEDIATE_PROGRESS) * i / exampleSet.size()));
			}
		}
		ClusterModel model = new ClusterModel(exampleSet, nextClusterId + 1, addsLabelAttribute(),
				getParameterAsBoolean(PARAMETER_REMOVE_UNLABELED));
		model.setClusterAssignments(clusterAssignments, exampleSet);

		if (addsClusterAttribute()) {
			addClusterAssignments(exampleSet, clusterAssignments);
			Attribute targetAttribute;
			if (addsLabelAttribute() && operatorCanAddLabel()) {
				targetAttribute = exampleSet.getAttributes().getLabel();
			} else {
				targetAttribute = exampleSet.getAttributes().getCluster();
			}
			try {
				Tools.replaceValue(targetAttribute, "cluster_" + NOISE, "noise");
			} catch (RuntimeException e){
				// ignore, because there might be no noise cluster
				// this will not interfere with attribute type checking, since RMAbstractClusterer#addClusterAttribute
				// guarantees this attribute to be nominal.
			}
		}
		return model;
	}

	protected LinkedList<Integer> getNeighbours(ExampleSet exampleSet, Example centroid, int centroidIndex,
			final int[] assignments, SVClusteringAlgorithm clustering) {
		LinkedList<Integer> neighbors = new LinkedList<Integer>();
		double maxRadius = paramR < 0 ? clustering.getR() : paramR;

		int i = 0;
		Attribute[] regularAttributes = centroid.getAttributes().createRegularAttributeArray();
		for (Example example : exampleSet) {
			if (i != centroidIndex && assignments[i] == UNASSIGNED) {
				double[] directions = new double[regularAttributes.length];
				int x = 0;
				for (Attribute attribute : regularAttributes) {
					directions[x++] = example.getValue(attribute) - centroid.getValue(attribute);
				}
				boolean addAsNeighbor = true;
				for (int j = 0; j < numSamplePoints; j++) {
					if (addAsNeighbor) {
						double[] virtualExample = new double[directions.length];
						x = 0;
						for (Attribute attribute : regularAttributes) {
							virtualExample[x] = centroid.getValue(attribute)
									+ (j + 1) * directions[x] / (numSamplePoints + 1);
							x++;
						}
						SVMExample svmExample = new SVMExample(virtualExample);
						double currentRadius = clustering.predict(svmExample);
						if (currentRadius > maxRadius) {
							addAsNeighbor = false;
							break;
						}
					} else {
						break;
					}
				}
				if (addAsNeighbor) {
					neighbors.add(i);
				}
			}
			i++;
		}
		return neighbors;
	}

	/**
	 * Creates a new kernel of the given type. The kernel type has to be one out of KERNEL_DOT,
	 * KERNEL_RADIAL, KERNEL_POLYNOMIAL, or KERNEL_NEURAL.
	 */
	public static Kernel createKernel(int kernelType) {
		switch (kernelType) {
			case KERNEL_RADIAL:
				return new KernelRadial();
			case KERNEL_POLYNOMIAL:
				return new KernelPolynomial();
			case KERNEL_NEURAL:
				return new KernelNeural();
			default:
				return new KernelDot();
		}
	}

	@Override
	public boolean supportsCapability(OperatorCapability capability) {
		switch (capability) {
			case BINOMINAL_ATTRIBUTES:
			case POLYNOMINAL_ATTRIBUTES:
				return false;
			default:
				return true;
		}
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
	public List<ParameterType> getParameterTypes() {
		List<ParameterType> types = super.getParameterTypes();

		ParameterType type;

		type = new ParameterTypeInt(MIN_PTS_NAME, "The minimal number of points in each cluster.", 0, Integer.MAX_VALUE, 2);
		type.setExpert(false);
		types.add(type);

		type = new ParameterTypeCategory(PARAMETER_KERNEL_TYPE, "The SVM kernel type", KERNEL_TYPES, KERNEL_RADIAL);
		type.setExpert(false);
		types.add(type);

		type = new ParameterTypeDouble(PARAMETER_KERNEL_GAMMA, "The SVM kernel parameter gamma (radial).", 0.0d,
				Double.POSITIVE_INFINITY, 1.0d);
		type.registerDependencyCondition(
				new EqualTypeCondition(this, PARAMETER_KERNEL_TYPE, KERNEL_TYPES, true, KERNEL_RADIAL));
		type.setExpert(false);
		types.add(type);
		type = new ParameterTypeInt(PARAMETER_KERNEL_DEGREE, "The SVM kernel parameter degree (polynomial).", 0,
				Integer.MAX_VALUE, 2);
		type.registerDependencyCondition(
				new EqualTypeCondition(this, PARAMETER_KERNEL_TYPE, KERNEL_TYPES, true, KERNEL_POLYNOMIAL));
		type.setExpert(false);
		types.add(type);
		type = new ParameterTypeDouble(PARAMETER_KERNEL_A, "The SVM kernel parameter a (neural).", Double.NEGATIVE_INFINITY,
				Double.POSITIVE_INFINITY, 1.0d);
		type.registerDependencyCondition(
				new EqualTypeCondition(this, PARAMETER_KERNEL_TYPE, KERNEL_TYPES, true, KERNEL_NEURAL));
		type.setExpert(false);
		types.add(type);
		type = new ParameterTypeDouble(PARAMETER_KERNEL_B, "The SVM kernel parameter b (neural).", Double.NEGATIVE_INFINITY,
				Double.POSITIVE_INFINITY, 0.0d);
		type.registerDependencyCondition(
				new EqualTypeCondition(this, PARAMETER_KERNEL_TYPE, KERNEL_TYPES, true, KERNEL_NEURAL));
		type.setExpert(false);
		types.add(type);

		types.add(new ParameterTypeInt(PARAMETER_KERNEL_CACHE, "Size of the cache for kernel evaluations im MB ", 0,
				Integer.MAX_VALUE, 200));
		type = new ParameterTypeDouble(PARAMETER_CONVERGENCE_EPSILON, "Precision on the KKT conditions", 0.0d,
				Double.POSITIVE_INFINITY, 1e-3);
		types.add(type);
		types.add(new ParameterTypeInt(PARAMETER_MAX_ITERATIONS, "Stop after this many iterations", 1, Integer.MAX_VALUE,
				100000));
		type = new ParameterTypeDouble(PARAMETER_P, "The fraction of allowed outliers.", 0, 1, 0.0d);
		types.add(type);
		type = new ParameterTypeDouble(PARAMETER_R,
				"Use this radius instead of the calculated one (-1 for calculated radius).", -1.0d, Double.POSITIVE_INFINITY,
				-1.0d);
		types.add(type);
		types.add(new ParameterTypeInt(PARAMETER_NUMBER_SAMPLE_POINTS,
				"The number of virtual sample points to check for neighborship.", 1, Integer.MAX_VALUE, 20));

		return types;
	}
}
