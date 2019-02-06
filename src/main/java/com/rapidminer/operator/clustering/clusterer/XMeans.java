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

import java.util.Collections;
import java.util.List;
import java.util.Map;

import org.apache.commons.lang.ArrayUtils;

import com.rapidminer.example.ExampleSet;
import com.rapidminer.operator.OperatorDescription;
import com.rapidminer.operator.OperatorException;
import com.rapidminer.operator.OperatorVersion;
import com.rapidminer.operator.clustering.CentroidClusterModel;
import com.rapidminer.operator.clustering.ClusterModel;
import com.rapidminer.parameter.ParameterType;
import com.rapidminer.parameter.ParameterTypeBoolean;
import com.rapidminer.parameter.ParameterTypeInt;
import com.rapidminer.tools.RandomGenerator;
import com.rapidminer.tools.math.similarity.DistanceMeasure;
import com.rapidminer.tools.math.similarity.DistanceMeasures;

import de.dfki.madm.operator.ClusteringAlgorithms;
import de.dfki.madm.operator.KMeanspp;
import de.dfki.madm.operator.clustering.XMeansCore;


/**
 * This operator represents an implementation of X-Means algorithm. It will create a cluster
 * attribute if not present yet.
 *
 * The implementation is according to paper of Dan Pelleg an Andrew Moore: - X-means: Extending
 * K-means with Efficient Estimation of the Number of Clusters
 *
 * @author Patrick Kalka
 */
public class XMeans extends RMAbstractClusterer {

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


	/**
	 * After this version the Operator parameters are propagated to the internal operators. There is also a change in
	 * the behaviour of the internal operators, that affects the final result.
	 */
	public static final OperatorVersion VERSION_9_0_0_LABEL_ROLE_BUG = new OperatorVersion(9, 0, 0);

	/**
	 * After this version the points counted twice bug that was introduced with the {@link #VERSION_9_0_0_LABEL_ROLE_BUG} fix should no longer appear.
	 */
	public static final OperatorVersion VERSION_9_1_0_POINTS_COUNTED_TWICE_BUG = new OperatorVersion(9, 0, 3);

	OperatorDescription Description = null;

	public XMeans(OperatorDescription description) {
		super(description);

		Description = description;
	}

	@Override
	protected ClusterModel generateInternalClusterModel(ExampleSet eSet) throws OperatorException {

		DistanceMeasure measure = getInitializedMeasure(eSet);
		int k_max = getParameterAsInt(PARAMETER_K_Max);
		int k_min = getParameterAsInt(PARAMETER_K_Min);
		boolean kpp = getParameterAsBoolean(KMeanspp.PARAMETER_USE_KPP);
		String fast_k = getParameterAsString(ClusteringAlgorithms.PARAMETER_CLUSTERING_ALGORITHM);
		int maxOptimizationSteps = getParameterAsInt(PARAMETER_MAX_OPTIMIZATION_STEPS);
		int maxRuns = getParameterAsInt(PARAMETER_MAX_RUNS);


		XMeansCore xm = new XMeansCore(eSet, k_min, k_max, kpp, maxOptimizationSteps, maxRuns, Description, measure, fast_k);

		xm.setCompatibilityLevel(this.getCompatibilityLevel());

		if (this.getCompatibilityLevel().isAbove(VERSION_9_0_0_LABEL_ROLE_BUG)) {
			xm.setParameter(RMAbstractClusterer.PARAMETER_ADD_AS_LABEL, getParameter(RMAbstractClusterer.PARAMETER_ADD_AS_LABEL));
			xm.setParameter(RMAbstractClusterer.PARAMETER_ADD_CLUSTER_ATTRIBUTE, getParameter(RMAbstractClusterer.PARAMETER_ADD_CLUSTER_ATTRIBUTE));
		}
		xm.setExecutingOperator(this);


		return xm.doXMean();
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
	protected boolean handlesInfiniteValues() {
		return false;
	}

	@Override
	public List<ParameterType> getParameterTypes() {
		List<ParameterType> types = super.getParameterTypes();
		types.add(new ParameterTypeInt(PARAMETER_K_Min, "The minimal number of clusters which should be detected.", 2,
				Integer.MAX_VALUE, 3, false));
		types.add(new ParameterTypeInt(PARAMETER_K_Max, "The maximal number of clusters which should be detected.", 3,
				Integer.MAX_VALUE, 60, false));

		ParameterType type = new ParameterTypeBoolean(KMeanspp.PARAMETER_USE_KPP, KMeanspp.SHORT_DESCRIPTION, true);
		type.setExpert(false);
		types.add(type);

		types.addAll(getMeasureParameterTypes());

		types.addAll(ClusteringAlgorithms.getParameterTypes(this));
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
		return (OperatorVersion[]) ArrayUtils.addAll(super.getIncompatibleVersionChanges(),
				new OperatorVersion[]{VERSION_9_0_0_LABEL_ROLE_BUG, KMeanspp.VERSION_KPP_NOT_WORKING, VERSION_9_1_0_POINTS_COUNTED_TWICE_BUG});
	}
}
