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

import java.util.List;

import com.rapidminer.example.Attribute;
import com.rapidminer.example.Example;
import com.rapidminer.example.ExampleSet;
import com.rapidminer.example.Tools;
import com.rapidminer.example.table.AttributeFactory;
import com.rapidminer.operator.OperatorDescription;
import com.rapidminer.operator.OperatorException;
import com.rapidminer.operator.clustering.ClusterModel;
import com.rapidminer.parameter.ParameterType;
import com.rapidminer.parameter.ParameterTypeInt;
import com.rapidminer.tools.Ontology;
import com.rapidminer.tools.RandomGenerator;


/**
 * Returns a random clustering. Note that this algorithm does not garantuee that all clusters are
 * non-empty. This operator will create a cluster attribute if not present yet.
 *
 * @author Sebastian Land
 */

public class RandomClustering extends RMAbstractClusterer {

	public static final String PARAMETER_NUMBER_OF_CLUSTERS = "number_of_clusters";

	public RandomClustering(OperatorDescription description) {
		super(description);
	}

	@Override
	public ClusterModel generateClusterModel(ExampleSet exampleSet) throws OperatorException {
		// checking and creating ids if necessary
		Tools.checkAndCreateIds(exampleSet);

		// init operator progress
		getProgress().setTotal(addsClusterAttribute() ? exampleSet.size() : exampleSet.size() * 2);

		// generating assignment
		RandomGenerator random = RandomGenerator.getRandomGenerator(this);
		int clusterAssignments[] = new int[exampleSet.size()];
		int k = getParameterAsInt(PARAMETER_NUMBER_OF_CLUSTERS);
		int counter = 0;
		for (int i = 0; i < exampleSet.size(); i++) {
			clusterAssignments[i] = random.nextInt(k);

			++counter;
			if (counter % 100 == 0) {
				getProgress().step(100);
				counter = 0;
			}
		}
		getProgress().setCompleted(exampleSet.size());

		ClusterModel model = new ClusterModel(exampleSet, k,
				getParameterAsBoolean(RMAbstractClusterer.PARAMETER_ADD_AS_LABEL),
				getParameterAsBoolean(RMAbstractClusterer.PARAMETER_REMOVE_UNLABELED));
		model.setClusterAssignments(clusterAssignments, exampleSet);

		// generating cluster attribute
		if (addsClusterAttribute()) {
			Attribute cluster = AttributeFactory.createAttribute("cluster", Ontology.NOMINAL);
			exampleSet.getExampleTable().addAttribute(cluster);
			exampleSet.getAttributes().setCluster(cluster);
			int i = 0;
			counter = 0;
			for (Example example : exampleSet) {
				example.setValue(cluster, "cluster_" + clusterAssignments[i]);
				i++;
				if (counter % 100 == 0) {
					getProgress().step(100);
					counter = 0;
				}
			}
		}
		getProgress().complete();

		return model;
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
