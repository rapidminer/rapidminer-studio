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
package com.rapidminer.operator.clustering.clusterer;

import com.rapidminer.example.ExampleSet;
import com.rapidminer.operator.OperatorCapability;
import com.rapidminer.operator.OperatorDescription;
import com.rapidminer.operator.OperatorException;
import com.rapidminer.operator.clustering.CentroidClusterModel;
import com.rapidminer.operator.clustering.ClusterModel;
import com.rapidminer.operator.learner.CapabilityProvider;
import com.rapidminer.operator.ports.metadata.CapabilityPrecondition;
import com.rapidminer.parameter.ParameterType;
import com.rapidminer.parameter.ParameterTypeBoolean;
import com.rapidminer.parameter.ParameterTypeInt;
import com.rapidminer.tools.RandomGenerator;
import com.rapidminer.tools.math.similarity.DistanceMeasure;
import com.rapidminer.tools.math.similarity.DistanceMeasureHelper;
import com.rapidminer.tools.math.similarity.DistanceMeasures;
import de.dfki.madm.operator.ClusteringAlgorithms;
import de.dfki.madm.operator.KMeanspp;
import de.dfki.madm.operator.clustering.XMeansCore;

import java.util.List;


/**
 * This operator represents an implementation of X-Means algorithm. It will create a cluster
 * attribute if not present yet.
 * 
 * The implementation is according to paper of Dan Pelleg an Andrew Moore: - X-means: Extending
 * K-means with Efficient Estimation of the Number of Clusters
 * 
 * @author Patrick Kalka
 */
public class XMeans extends RMAbstractClusterer implements CapabilityProvider {

	/** Maximal number of Clusters */
	public static final String PARAMETER_K_Max = "k_max";

	/** Minimal number of Clusters */
	public static final String PARAMETER_K_Min = "k_min";

	/**
	 * The parameter name for &quot;the maximal number of runs of the k method with random
	 * initialization that are performed&quot;
	 */
	public static final String PARAMETER_MAX_RUNS = "max_runs";

	/**
	 * The parameter name for &quot;the maximal number of iterations performed for one run of the
	 * k-mean&quot;
	 */
	public static final String PARAMETER_MAX_OPTIMIZATION_STEPS = "max_optimization_steps";

	private DistanceMeasureHelper measureHelper = new DistanceMeasureHelper(this);
	OperatorDescription Description = null;

	public XMeans(OperatorDescription description) {
		super(description);

		Description = description;
		getExampleSetInputPort().addPrecondition(new CapabilityPrecondition(this, getExampleSetInputPort()));
	}

	@Override
	public ClusterModel generateClusterModel(ExampleSet eSet) throws OperatorException {

		DistanceMeasure measure = measureHelper.getInitializedMeasure(eSet);
		int k_max = getParameterAsInt(PARAMETER_K_Max);
		int k_min = getParameterAsInt(PARAMETER_K_Min);
		boolean kpp = getParameterAsBoolean(KMeanspp.PARAMETER_USE_KPP);
		String fast_k = getParameterAsString(ClusteringAlgorithms.PARAMETER_CLUSTERING_ALGORITHM);
		int maxOptimizationSteps = getParameterAsInt(PARAMETER_MAX_OPTIMIZATION_STEPS);
		int maxRuns = getParameterAsInt(PARAMETER_MAX_RUNS);

		XMeansCore xm = new XMeansCore(eSet, k_min, k_max, kpp, maxOptimizationSteps, maxRuns, Description, measure, fast_k);

		return xm.doXMean();
	}

	@Override
	public Class<? extends ClusterModel> getClusterModelClass() {
		return CentroidClusterModel.class;
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
	public List<ParameterType> getParameterTypes() {
		List<ParameterType> types = super.getParameterTypes();
		types.add(new ParameterTypeInt(PARAMETER_K_Min, "The minimal number of clusters which should be detected.", 2,
				Integer.MAX_VALUE, 2, false));
		types.add(new ParameterTypeInt(PARAMETER_K_Max, "The maximal number of clusters which should be detected.", 60,
				Integer.MAX_VALUE, 60, false));

		ParameterType type = new ParameterTypeBoolean(KMeanspp.PARAMETER_USE_KPP, KMeanspp.SHORT_DESCRIPTION, false);
		type.setExpert(false);
		types.add(type);

		for (ParameterType a : DistanceMeasures.getParameterTypes(this)) {
			if (a.getKey() == DistanceMeasures.PARAMETER_MEASURE_TYPES) {
				a.setDefaultValue(2);
			}
			types.add(a);
		}

		types.addAll(ClusteringAlgorithms.getParameterTypes(this));
		types.add(new ParameterTypeInt(PARAMETER_MAX_RUNS,
				"The maximal number of runs of k-Means with random initialization that are performed.", 1,
				Integer.MAX_VALUE, 10, false));
		types.add(new ParameterTypeInt(PARAMETER_MAX_OPTIMIZATION_STEPS,
				"The maximal number of iterations performed for one run of k-Means.", 1, Integer.MAX_VALUE, 100, false));
		types.addAll(RandomGenerator.getRandomGeneratorParameters(this));
		return types;
	}
}
