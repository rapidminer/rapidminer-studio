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

import java.io.Serializable;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.LinkedHashMap;
import java.util.LinkedList;
import java.util.List;

import com.rapidminer.example.Attribute;
import com.rapidminer.example.Example;
import com.rapidminer.example.ExampleSet;
import com.rapidminer.example.set.Partition;
import com.rapidminer.example.set.SplittedExampleSet;
import com.rapidminer.operator.Model;
import com.rapidminer.operator.OperatorException;
import com.rapidminer.operator.learner.PredictionModel;


/**
 * This model of the hierarchical learner. This stores single models at each step to divide the
 * examples into the single branches of the binary model tree.
 *
 * @author Tobias Malbrecht, Sebastian Land
 */
public class HierarchicalMultiClassModel extends PredictionModel implements MetaModel {

	public static class Node implements Serializable {

		private static final long serialVersionUID = 1L;

		private final String className;
		private int partitionId;

		private final LinkedHashMap<String, Node> children = new LinkedHashMap<String, Node>();
		private final List<Node> childrenList = new ArrayList<Node>();

		private Node parent = null;

		private Model model = null;

		public Node(String className) {
			this.className = className;
		}

		/**
		 * Returns the children in order of insertion
		 */
		public List<Node> getChildren() {
			return childrenList;
		}

		/**
		 * Adds a child node.
		 */
		public void addChild(Node child) {
			childrenList.add(child);
			children.put(child.getClassName(), child);
			child.setParent(this);
		}

		/**
		 * Sets the parent of this node. Only the root node may have a null parent.
		 */
		public void setParent(Node parent) {
			this.parent = parent;
		}

		public boolean isRoot() {
			return parent == null;
		}

		public void setPartitionId(int partition) {
			this.partitionId = partition;
		}

		public int getPartitionId() {
			return partitionId;
		}

		public Node getParent() {
			return this.parent;
		}

		public String getClassName() {
			return this.className;
		}

		public boolean isLeaf() {
			return children.isEmpty();
		}

		public void setModel(Model model) {
			this.model = model;
		}

		public Model getModel() {
			return this.model;
		}

		public Node getChild(String label) {
			return children.get(label);
		}

	}

	private static final long serialVersionUID = -5792943818860734082L;

	private final Node root;

	public HierarchicalMultiClassModel(ExampleSet exampleSet, Node root) {
		super(exampleSet, null, null);
		this.root = root;
	}

	@Override
	public ExampleSet performPrediction(ExampleSet exampleSet, Attribute predictedLabel) throws OperatorException {
		ExampleSet applySet = (ExampleSet) exampleSet.clone();

		// defining arrays for transferring information over recursive calls
		double[] confidences = new double[applySet.size()];
		int[] outcomes = new int[applySet.size()];
		int[] depths = new int[applySet.size()];

		Arrays.fill(outcomes, root.getPartitionId());
		Arrays.fill(confidences, 1d);

		// applying predictions recursively
		performPredictionRecursivly(applySet, root, confidences, outcomes, depths, 0, root.getPartitionId() + 1);

		// retrieving prediction attributes
		Attribute labelAttribute = getTrainingHeader().getAttributes().getLabel();
		int numberOfLabels = labelAttribute.getMapping().size();
		Attribute[] confidenceAttributes = new Attribute[numberOfLabels];
		for (int i = 0; i < numberOfLabels; i++) {
			confidenceAttributes[i] = exampleSet.getAttributes().getConfidence(labelAttribute.getMapping().mapIndex(i));
		}

		// assigning final outcome and confidences
		int i = 0;
		for (Example example : exampleSet) {
			// setting label according to outcome
			example.setValue(predictedLabel, outcomes[i]);

			// calculating confidences
			double confidence = Math.pow(confidences[i], 1d / depths[i]);
			double defaultConfidence = (1d - confidence) / numberOfLabels;

			// setting confidences
			for (int j = 0; j < numberOfLabels; j++) {
				example.setValue(confidenceAttributes[j], defaultConfidence);
			}
			example.setValue(confidenceAttributes[outcomes[i]], confidence);

			i++;
		}
		return exampleSet;
	}

	/**
	 * This method will apply all the nodes recursively. For each node it will be called when
	 * descending the learner hierarchy. The outcomes array stores the information to which node
	 * each example of the applySet has been assigned. Each node's model will be applied to the
	 * subset of a partitioned example set according to the node's partition id. After the
	 * classification has been performed, the examples will be assigned the partion id's of the
	 * child nodes, to whose class the examples where classified.
	 *
	 * It is very important that after each application the predicted label and the confidences are
	 * removed explicitly to avoid a memory leak in the memory table!
	 *
	 * Confidences are multiplied with the outcome every application.
	 */
	private void performPredictionRecursivly(ExampleSet applySet, Node node, double[] confidences, int[] outcomes,
			int[] depths, int depth, int numberOfPartitions) throws OperatorException {
		if (!node.isLeaf()) {
			// creating partitioned example set
			SplittedExampleSet splittedSet = new SplittedExampleSet(applySet, new Partition(outcomes, numberOfPartitions));
			splittedSet.selectSingleSubset(node.getPartitionId());

			// applying
			ExampleSet currentResultSet = node.getModel().apply(splittedSet);

			// assign each example a child node regarding to the classification outcome
			int resultIndex = 0;
			Attribute predictionAttribute = currentResultSet.getAttributes().getPredictedLabel();
			for (Example example : currentResultSet) {
				int parentIndex = splittedSet.getActualParentIndex(resultIndex);

				// extracting data
				String label = example.getValueAsString(predictionAttribute);
				confidences[parentIndex] *= example.getConfidence(label);

				// setting outcome index according to referenced child node: if child is leaf, this
				// is equivalent to class index
				outcomes[parentIndex] = node.getChild(label).getPartitionId();
				depths[parentIndex] = depth;

				resultIndex++;
			}
			// deleting new columns from example table
			PredictionModel.removePredictedLabel(currentResultSet);

			// now go through children and apply their subset
			for (Node child : node.getChildren()) {
				performPredictionRecursivly(applySet, child, confidences, outcomes, depths, depth + 1, numberOfPartitions);
			}
		}
	}

	@Override
	public List<String> getModelNames() {
		List<Node> nodes = new LinkedList<Node>();
		collectNodes(root, nodes);

		List<String> names = new ArrayList<String>(nodes.size());
		for (Node node : nodes) {
			if (!node.isLeaf()) {
				names.add(node.getClassName());
			}
		}
		return names;
	}

	private void collectNodes(Node node, List<Node> nodes) {
		nodes.add(node);
		if (!node.isLeaf()) {
			for (Node child : node.getChildren()) {
				collectNodes(child, nodes);
			}
		}
	}

	@Override
	public List<Model> getModels() {
		List<Node> nodes = new LinkedList<Node>();
		collectNodes(root, nodes);

		List<Model> names = new ArrayList<Model>(nodes.size());
		for (Node node : nodes) {
			if (!node.isLeaf()) {
				names.add(node.getModel());
			}
		}
		return names;
	}
}
