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
import java.util.HashMap;
import java.util.LinkedList;
import java.util.List;
import java.util.Map.Entry;

import com.rapidminer.example.Attribute;
import com.rapidminer.example.Example;
import com.rapidminer.example.ExampleSet;
import com.rapidminer.operator.Model;
import com.rapidminer.operator.OperatorException;
import com.rapidminer.operator.learner.PredictionModel;


/**
 * @author Tobias Malbrecht, Sebastian Land
 */
@Deprecated
public class HierarchicalModel extends PredictionModel implements MetaModel {

	public static class Node implements Serializable {

		private static final long serialVersionUID = 1L;

		private final String className;

		private final List<Node> children = new ArrayList<Node>();

		private Node parent = null;

		private Model model = null;

		public Node(String className) {
			this.className = className;
		}

		public List<Node> getChildren() {
			return children;
		}

		public void addChild(Node child) {
			children.add(child);
			child.setParent(this);
		}

		public void setParent(Node parent) {
			this.parent = parent;
		}

		public Node getParent() {
			return this.parent;
		}

		public String getClassName() {
			return this.className;
		}

		public List<String> getChildrenClasses() {
			List<String> childrenClasses = new ArrayList<String>();
			for (Node child : children) {
				childrenClasses.add(child.getClassName());
				childrenClasses.addAll(child.getChildrenClasses());
			}
			return childrenClasses;
		}

		public List<String> getLeaveClasses() {
			List<String> leaveClasses = new ArrayList<String>();
			for (Node child : children) {
				leaveClasses.addAll(child.getLeaveClasses());
			}
			if (children.size() == 0) {
				leaveClasses.add(className);
			}
			return leaveClasses;
		}

		public void setModel(Model model) {
			this.model = model;
		}

		public Model getModel() {
			return this.model;
		}

	}

	private static final long serialVersionUID = -5792943818860734082L;

	private final Node root;

	public HierarchicalModel(ExampleSet exampleSet, Node root) {
		super(exampleSet, null, null);
		this.root = root;
	}

	/**
	 * Progress not implemented, because class is deprecated
	 */
	@Override
	public ExampleSet performPrediction(ExampleSet exampleSet, Attribute predictedLabel) throws OperatorException {
		ExampleSet eSet = (ExampleSet) exampleSet.clone();
		int numberOfClasses = getLabel().getMapping().getValues().size();

		HashMap<String, Integer> classIndexMap = new HashMap<String, Integer>(numberOfClasses);
		for (String currentClass : getLabel().getMapping().getValues()) {
			classIndexMap.put(currentClass, getLabel().getMapping().mapString(currentClass));
		}

		double[][] confidenceMatrix = new double[eSet.size()][numberOfClasses];
		for (int i = 0; i < confidenceMatrix.length; i++) {
			for (int j = 0; j < confidenceMatrix[i].length; j++) {
				confidenceMatrix[i][j] = 1;
			}
		}
		performPrediction(eSet, predictedLabel, root, confidenceMatrix, classIndexMap);

		int counter = 0;
		for (Example example : exampleSet) {
			double predictedValue = 0;
			double maxConfidence = 0;
			// double sumConfidence = 0;
			// for (int i = 0; i < confidenceMatrix[counter].length; i++) {
			// sumConfidence += confidenceMatrix[counter][i];
			// }
			for (Entry<String, Integer> entry : classIndexMap.entrySet()) {
				// confidenceMatrix[counter][entry.getValue()] /= sumConfidence;
				example.setConfidence(entry.getKey(), confidenceMatrix[counter][entry.getValue()]);
				if (confidenceMatrix[counter][entry.getValue()] > maxConfidence) {
					maxConfidence = confidenceMatrix[counter][entry.getValue()];
					predictedValue = entry.getValue();
				}
			}
			example.setPredictedLabel(predictedValue);
			counter++;
		}
		return exampleSet;
	}

	public void performPrediction(ExampleSet eSet, Attribute predictedLabel, Node node, double[][] confidenceMatrix,
			HashMap<String, Integer> classIndexMap) throws OperatorException {
		if (node.getModel() != null && node.getChildren().size() > 0) {
			System.err.println("Predicting " + node.getClassName());
			eSet = node.getModel().apply(eSet);

			int counter = 0;
			for (Example example : eSet) {
				for (Node child : node.getChildren()) {
					double confidence = example.getConfidence(child.getClassName());
					for (String className : child.getLeaveClasses()) {
						confidenceMatrix[counter][classIndexMap.get(className)] *= confidence;
					}
				}
				counter++;
			}
			PredictionModel.removePredictedLabel(eSet);
		}
		for (Node child : node.getChildren()) {
			performPrediction(eSet, predictedLabel, child, confidenceMatrix, classIndexMap);
		}
	}

	@Override
	public List<String> getModelNames() {
		List<String> names = new LinkedList<String>();
		return names;
	}

	@Override
	public List<Model> getModels() {
		return Arrays.asList();
	}
}
