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

import java.util.Iterator;
import java.util.List;

import com.rapidminer.example.Attribute;
import com.rapidminer.example.Example;
import com.rapidminer.example.ExampleSet;
import com.rapidminer.example.Tools;
import com.rapidminer.operator.OperatorCapability;
import com.rapidminer.operator.OperatorDescription;
import com.rapidminer.operator.OperatorException;
import com.rapidminer.operator.clustering.ClusterModel;
import com.rapidminer.parameter.ParameterType;
import com.rapidminer.parameter.ParameterTypeInt;
import com.rapidminer.tools.RandomGenerator;


/**
 * Returns a random clustering. Note that this algorithm does not garantuee that all clusters are
 * non-empty. This operator will create a cluster attribute if not present yet.
 *
 * @author Sebastian Land
 */

public class RandomClustering extends RMAbstractClusterer {

	public static final String PARAMETER_NUMBER_OF_CLUSTERS = "number_of_clusters";

	private static final int OPERATOR_PROGRESS_STEPS = 10_000;

	public RandomClustering(OperatorDescription description) {
		super(description);
	}

	@Override
	protected ClusterModel generateInternalClusterModel(ExampleSet exampleSet) throws OperatorException {
		// checking and creating ids if necessary
		Tools.checkAndCreateIds(exampleSet);

		boolean addsClusterAttribute = addsClusterAttribute();

		// init operator progress
		getProgress().setTotal(exampleSet.size());

		// generating assignment
		RandomGenerator random = RandomGenerator.getRandomGenerator(this);
		int clusterAssignments[] = new int[exampleSet.size()];
		int k = getParameterAsInt(PARAMETER_NUMBER_OF_CLUSTERS);
		ClusterModel model = new ClusterModel(exampleSet, k, addsLabelAttribute(),
				getParameterAsBoolean(RMAbstractClusterer.PARAMETER_REMOVE_UNLABELED));
		Attribute targetAttribute = null;
		Iterator<Example> exIter = null;
		if (addsClusterAttribute) {
			// generating cluster attribute
			targetAttribute = addClusterAttribute(exampleSet);
			exIter = exampleSet.iterator();
		}
		for (int i = 0; i < exampleSet.size(); i++) {
			clusterAssignments[i] = random.nextInt(k);
			if (addsClusterAttribute) {
				exIter.next().setValue(targetAttribute, "cluster_" + clusterAssignments[i]);
			}
			if (i % OPERATOR_PROGRESS_STEPS == 0) {
				getProgress().setCompleted(i);
			}
		}
		model.setClusterAssignments(clusterAssignments, exampleSet);

		getProgress().complete();

		return model;
	}

	@Override
	public boolean supportsCapability(OperatorCapability capability) {
		switch (capability) {
			case WEIGHTED_EXAMPLES:
			case MISSING_VALUES:
				return true;
			default:
				return super.supportsCapability(capability);
		}
	}

	@Override
	protected boolean supportsNominalValues() {
		return true;
	}

	@Override
	protected void checkNoNfiniteValues(ExampleSet exampleSet) throws OperatorException {}

	@Override
	protected boolean checksForExamples() {
		return getCompatibilityLevel().isAbove(BEFORE_EMPTY_CHECKS);
	}

	@Override
	protected boolean checksForRegularAttributes() {
		return false;
	}

	@Override
	protected boolean affectedByEmptyCheck() {
		return true;
	}

	@Override
	public List<ParameterType> getParameterTypes() {
		List<ParameterType> types = super.getParameterTypes();

		ParameterType type = new ParameterTypeInt(PARAMETER_NUMBER_OF_CLUSTERS, "Specifies the desired number of clusters.",
				2, Integer.MAX_VALUE, 3);
		type.setExpert(false);
		types.add(type);

		types.addAll(RandomGenerator.getRandomGeneratorParameters(this));
		return types;
	}
}
