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
package com.rapidminer.operator.learner.meta;

import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.atomic.AtomicInteger;

import com.rapidminer.example.Attribute;
import com.rapidminer.example.Example;
import com.rapidminer.example.ExampleSet;
import com.rapidminer.example.set.Partition;
import com.rapidminer.example.set.SplittedExampleSet;
import com.rapidminer.example.table.AttributeFactory;
import com.rapidminer.example.table.NominalMapping;
import com.rapidminer.operator.Model;
import com.rapidminer.operator.OperatorCapability;
import com.rapidminer.operator.OperatorDescription;
import com.rapidminer.operator.OperatorException;
import com.rapidminer.operator.UserError;
import com.rapidminer.operator.learner.meta.HierarchicalMultiClassModel.Node;
import com.rapidminer.parameter.ParameterType;
import com.rapidminer.parameter.ParameterTypeList;
import com.rapidminer.parameter.ParameterTypeString;
import com.rapidminer.tools.RandomGenerator;


/**
 * This is a meta learner for classifying multiple classes using a hierarchical approach. For a
 * higher number of classes this might prove more accurate than the one versus all or one versus one
 * approach. If applying the models in a binary tree like structure, less models need to be stored
 * in memory as well as less applications have to be performed before having a result.
 * 
 * @author Tobias Malbrecht, Sebastian Land
 */
public class HierarchicalMultiClassLearner extends AbstractMetaLearner {

	public static final String PARAMETER_HIERARCHY = "hierarchy";

	public static final String PARAMETER_PARENT_CLASS = "parent_class";

	public static final String PARAMETER_CHILD_CLASS = "child_class";

	public HierarchicalMultiClassLearner(OperatorDescription description) {
		super(description);
	}

	@Override
	public Model learn(ExampleSet inputSet) throws OperatorException {
		Attribute labelAttribute = inputSet.getAttributes().getLabel();

		// check if label attribute's value set is equal to defined classes
		checkCompatibility(labelAttribute);

		// create model hierarchy / tree
		List<String[]> hierarchyEntryPairs = getParameterList(PARAMETER_HIERARCHY);
		Map<String, Node> nodeMap = new HashMap<String, Node>();
		Set<Node> innerNodes = new HashSet<Node>();
		for (String[] entryPair : hierarchyEntryPairs) {
			String parentClass = entryPair[0];
			String childClass = entryPair[1];
			Node parentNode = nodeMap.get(parentClass);
			if (parentNode == null) {
				parentNode = new Node(parentClass);
			}
			Node childNode = nodeMap.get(childClass);
			if (childNode == null) {
				childNode = new Node(childClass);
			}
			parentNode.addChild(childNode);
			nodeMap.put(parentClass, parentNode);
			nodeMap.put(childClass, childNode);
			innerNodes.add(childNode);
		}

		// root node is single node that is not inner
		Node root = null;
		for (Node node : nodeMap.values()) {
			if (!innerNodes.contains(node)) {
				if (root == null) {
					root = node;
				} else {
					throw new UserError(this, 220, root.getClassName(), node.getClassName());
				}
			}
		}
		if (root == null) {
			throw new UserError(this, 221);
		}

		// check if each node has at least 2 children or is leaf
		for (Node node : nodeMap.values()) {
			if (node.getChildren().size() == 1) {
				throw new UserError(this, 222, node.getClassName(), node.getChildren().size());
			}
		}

		computeModel(root, inputSet, labelAttribute);

		return new HierarchicalMultiClassModel(inputSet, root);
	}

	private void checkCompatibility(Attribute labelAttribute) throws UserError {
		Set<String> values = new HashSet<String>(labelAttribute.getMapping().getValues());

		// add all left hand side
		List<String[]> hierarchy = getParameterList(PARAMETER_HIERARCHY);
		for (String[] pair : hierarchy) {
			values.add(pair[0]);
		}

		String rootValue = null;
		for (String[] pair : hierarchy) {
			// check if right hand side value is either defined as right hand side or is original
			// label value
			if (!values.contains(pair[1])) {
				throw new UserError(this, 219, pair[1]);
			}
			// check if each left hand side is assigned as right hand side except the root
			if (!values.contains(pair[0])) {
				if (rootValue == null) {
					rootValue = pair[0];
				} else {
					throw new UserError(this, 220, pair[0], rootValue);
				}
			}
		}
	}

	/**
	 * This method will first create a working label column and after this run through the tree
	 * recursivly.
	 */
	private void computeModel(HierarchicalMultiClassModel.Node rootNode, ExampleSet exampleSet, Attribute originalLabel)
			throws OperatorException {
		// create working label with copy of original label values
		exampleSet.getAttributes().setSpecialAttribute(originalLabel, "label_original");
		Attribute workingLabel = AttributeFactory.createAttribute(originalLabel.getName() + "_working",
				originalLabel.getValueType());
		exampleSet.getExampleTable().addAttribute(workingLabel);
		exampleSet.getAttributes().addRegular(workingLabel);
		exampleSet.getAttributes().setLabel(workingLabel);

		// create partition for recursive learning
		int[] partitions = new int[exampleSet.size()];

		int i = 0;
		int lastLeafId = -1;
		for (Example example : exampleSet) {
			double value = example.getValue(originalLabel);
			example.setValue(workingLabel, value);
			partitions[i] = (int) value;
			if (partitions[i] > lastLeafId) {
				lastLeafId = partitions[i];
			}
			i++;
		}

		AtomicInteger nonLeafCounter = new AtomicInteger(lastLeafId);
		setParitionIdRecursivly(rootNode, nonLeafCounter, lastLeafId, workingLabel);

		// recursively walk through hierarchy and learn
		computeModelRecursivly(rootNode, partitions, nonLeafCounter.get(), exampleSet);

		// remove working_label again
		exampleSet.getAttributes().remove(workingLabel);
		exampleSet.getAttributes().setLabel(originalLabel);
		exampleSet.getExampleTable().removeAttribute(workingLabel);
	}

	/**
	 * This will set the partition id by either taking the mapping value of the original label
	 * mapping if the node is a leaf, or the next free integer available after the highest entry in
	 * the mapping.
	 */
	private void setParitionIdRecursivly(Node node, AtomicInteger nonLeafCounter, int maxLeafId, Attribute workingLabel) {
		if (node.isLeaf()) {
			node.setPartitionId(workingLabel.getMapping().mapString(node.getClassName()));
		} else {
			for (Node child : node.getChildren()) {
				setParitionIdRecursivly(child, nonLeafCounter, maxLeafId, workingLabel);
				node.setPartitionId(nonLeafCounter.incrementAndGet());
			}
		}
	}

	/**
	 * This method will learn the model tree bottom up by splitting the example set into the
	 * partitions defined by the partitions array and use the ones defined by the child nodes.
	 * 
	 * @throws OperatorException
	 */
	private void computeModelRecursivly(Node node, int[] partitions, int numberOfPartitions, ExampleSet exampleSet)
			throws OperatorException {
		if (node.isLeaf()) {
			return;
		} else {
			// first learn all models below
			for (Node child : node.getChildren()) {
				computeModelRecursivly(child, partitions, numberOfPartitions, exampleSet);
			}

			// then it is assured that there exist partitions with the index of the child nodes. Now
			// use these examples
			SplittedExampleSet trainSet = new SplittedExampleSet(exampleSet, new Partition(partitions, numberOfPartitions));
			Attribute workingLabel = trainSet.getAttributes().getLabel();
			workingLabel.setMapping((NominalMapping) workingLabel.getMapping().clone());
			workingLabel.getMapping().clear();
			for (Node child : node.getChildren()) {
				trainSet.selectSingleSubset(child.getPartitionId());
				int nodeLabelIndex = workingLabel.getMapping().mapString(child.getClassName());
				for (Example example : trainSet) {
					example.setValue(workingLabel, nodeLabelIndex);
				}
			}

			// select all participating subsets
			trainSet.clearSelection();
			for (Node child : node.getChildren()) {
				trainSet.selectAdditionalSubset(child.getPartitionId());
			}

			// learn model by applying inner learner
			Model model = applyInnerLearner(trainSet);
			node.setModel(model);

			// then replace partition entries of all child nodes with own
			int partitionId = node.getPartitionId();
			for (Node child : node.getChildren()) {
				int childPartitionId = child.getPartitionId();
				for (int i = 0; i < partitions.length; i++) {
					if (partitions[i] == childPartitionId) {
						partitions[i] = partitionId;
					}
				}
			}
		}
	}

	@Override
	public boolean supportsCapability(OperatorCapability capability) {
		switch (capability) {
			case NUMERICAL_LABEL:
			case NO_LABEL:
			case UPDATABLE:
			case FORMULA_PROVIDER:
			case BINOMINAL_LABEL:
			case ONE_CLASS_LABEL:
				return false;
			default:
				return true;
		}
	}

	@Override
	public List<ParameterType> getParameterTypes() {
		List<ParameterType> types = super.getParameterTypes();
		ParameterType type = new ParameterTypeList(PARAMETER_HIERARCHY, "The hierarchy...", new ParameterTypeString(
				PARAMETER_PARENT_CLASS, "The parent class.", false), new ParameterTypeString(PARAMETER_CHILD_CLASS,
				"The child class.", false));
		type.setPrimary(true);
		types.add(type);
		types.addAll(RandomGenerator.getRandomGeneratorParameters(this));
		return types;
	}

}
