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
package com.rapidminer.operator.clustering;

import java.util.Comparator;
import java.util.LinkedList;
import java.util.List;
import java.util.PriorityQueue;

import com.rapidminer.example.Attributes;
import com.rapidminer.example.ExampleSet;
import com.rapidminer.operator.Operator;
import com.rapidminer.operator.OperatorDescription;
import com.rapidminer.operator.OperatorException;
import com.rapidminer.operator.UserError;
import com.rapidminer.operator.ports.InputPort;
import com.rapidminer.operator.ports.OutputPort;
import com.rapidminer.operator.ports.metadata.AttributeMetaData;
import com.rapidminer.operator.ports.metadata.ExampleSetMetaData;
import com.rapidminer.operator.ports.metadata.ExampleSetPassThroughRule;
import com.rapidminer.operator.ports.metadata.GenerateNewMDRule;
import com.rapidminer.operator.ports.metadata.SetRelation;
import com.rapidminer.parameter.ParameterType;
import com.rapidminer.parameter.ParameterTypeBoolean;
import com.rapidminer.parameter.ParameterTypeInt;
import com.rapidminer.tools.Ontology;


/**
 * Creates a flat cluster model from a hierarchical one by expanding nodes in the order of their
 * distance until the desired number of clusters is reached.
 *
 * @author Sebastian Land
 */
public class FlattenClusterModel extends Operator {

	public static final String PARAMETER_NUMBER_OF_CLUSTER = "number_of_clusters";
	public static final String PARAMETER_REMOVE_UNLABELED = "remove_unlabeled";
	public static final String PARAMETER_ADD_AS_LABEL = "add_as_label";

	private InputPort hierarchicalInput = getInputPorts().createPort("hierarchical", HierarchicalClusterModel.class);
	private InputPort exampleSetInput = getInputPorts().createPort("example set", ExampleSet.class);
	private OutputPort flatOutput = getOutputPorts().createPort("flat");
	private OutputPort exampleSetOutput = getOutputPorts().createPort("example set");

	public FlattenClusterModel(OperatorDescription description) {
		super(description);
		getTransformer().addRule(new GenerateNewMDRule(flatOutput, ClusterModel.class));
		getTransformer().addRule(new ExampleSetPassThroughRule(exampleSetInput, exampleSetOutput, SetRelation.EQUAL) {

			@Override
			public ExampleSetMetaData modifyExampleSet(ExampleSetMetaData metaData) {
				String targetName = addsLabelAttribute() ? Attributes.LABEL_NAME : Attributes.CLUSTER_NAME;
				metaData.addAttribute(new AttributeMetaData(targetName, Ontology.NOMINAL, targetName));
				return metaData;
			}
		});
	}

	@Override
	public void doWork() throws OperatorException {
		HierarchicalClusterModel hierarchicalModel = hierarchicalInput.getData(HierarchicalClusterModel.class);
		ExampleSet exampleSet = exampleSetInput.getData(ExampleSet.class);
		flatOutput.deliver(flatten(hierarchicalModel, exampleSet));
	}

	public ClusterModel flatten(HierarchicalClusterModel model, ExampleSet exampleSet) throws OperatorException {
		HierarchicalClusterNode root = model.getRootNode();
		int numberOfClusters = getParameterAsInt(PARAMETER_NUMBER_OF_CLUSTER);

		// creating priorityQueue using reversing comparator
		PriorityQueue<HierarchicalClusterNode> queue = new PriorityQueue<HierarchicalClusterNode>(numberOfClusters,
				new Comparator<HierarchicalClusterNode>() {

			@Override
			public int compare(HierarchicalClusterNode o1, HierarchicalClusterNode o2) {
				int value = -1 * Double.compare(o1.getDistance(), o2.getDistance());
				if (value != 0) {
					return value;
				} else {
					return -1 * Double.compare(o1.getNumberOfExamplesInSubtree(), o2.getNumberOfExamplesInSubtree());
				}
			}
		});

		// Iteratively descend within graph by splitting at greatest node until queue is full or
		// enough leafs are collected
		LinkedList<HierarchicalClusterNode> leafs = new LinkedList<HierarchicalClusterNode>();
		queue.add(root);
		while (queue.size() < numberOfClusters - leafs.size()) {
			HierarchicalClusterNode topNode = queue.poll();
			if (topNode == null) {
				throw new UserError(null, 142, PARAMETER_NUMBER_OF_CLUSTER);
			}
			if (topNode.getSubNodes().size() > 0) {
				queue.addAll(topNode.getSubNodes());
			} else {
				leafs.add(topNode);
			}
		}
		queue.addAll(leafs);

		// construct flat cluster model from nodes
		ClusterModel flatModel = new ClusterModel(exampleSet, numberOfClusters, addsLabelAttribute(),
				getParameterAsBoolean(PARAMETER_REMOVE_UNLABELED));
		int i = 0;
		for (HierarchicalClusterNode node : queue) {
			Cluster flatCluster = flatModel.getCluster(i);
			for (Object exampleId : node.getExampleIdsInSubtree()) {
				flatCluster.assignExample(exampleId);
			}
			i++;
		}

		// delivering adapted example set
		if (exampleSetOutput.isConnected()) {
			exampleSetOutput.deliver(flatModel.apply((ExampleSet) exampleSet.clone()));
		}
		return flatModel;
	}

	/**
	 * Indicates whether this operator adds the cluster attribute as a label attribute.
	 *
	 * @since 7.6
	 */
	private boolean addsLabelAttribute() {
		return getParameterAsBoolean(PARAMETER_ADD_AS_LABEL);
	}

	@Override
	public List<ParameterType> getParameterTypes() {
		List<ParameterType> types = super.getParameterTypes();
		ParameterType type = new ParameterTypeInt(PARAMETER_NUMBER_OF_CLUSTER,
				"Specifies how many flat clusters should be created.", 1, Integer.MAX_VALUE, 3);
		type.setExpert(false);
		types.add(type);

		type = new ParameterTypeBoolean(PARAMETER_ADD_AS_LABEL, "Should the cluster values be added as label.", false);
		type.setExpert(false);
		types.add(type);

		type = new ParameterTypeBoolean(PARAMETER_REMOVE_UNLABELED, "Delete the unlabeled examples.", false);
		type.setExpert(false);
		types.add(type);
		return types;
	}
}
