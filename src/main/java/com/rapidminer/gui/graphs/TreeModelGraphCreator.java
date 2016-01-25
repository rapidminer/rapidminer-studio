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
package com.rapidminer.gui.graphs;

import com.rapidminer.gui.tools.SwingTools;
import com.rapidminer.operator.learner.tree.Edge;
import com.rapidminer.operator.learner.tree.SplitCondition;
import com.rapidminer.operator.learner.tree.Tree;
import com.rapidminer.operator.learner.tree.TreeModel;
import edu.uci.ics.jung.graph.DelegateForest;
import edu.uci.ics.jung.graph.DirectedOrderedSparseMultigraph;
import edu.uci.ics.jung.graph.Graph;
import edu.uci.ics.jung.visualization.renderers.Renderer.EdgeLabel;
import edu.uci.ics.jung.visualization.renderers.Renderer.Vertex;
import edu.uci.ics.jung.visualization.renderers.Renderer.VertexLabel;

import java.util.HashMap;
import java.util.Iterator;
import java.util.Map;

import org.apache.commons.collections15.Factory;


/**
 * Creates a graph model for a learned tree model.
 * 
 * @author Ingo Mierswa
 */
public class TreeModelGraphCreator extends GraphCreatorAdaptor {

	private Factory<String> edgeFactory = new Factory<String>() {

		int i = 0;

		@Override
		public String create() {
			return "E" + i++;
		}
	};

	private Factory<String> vertexFactory = new Factory<String>() {

		int i = 0;

		@Override
		public String create() {
			return "V" + i++;
		}
	};

	private TreeModel model;

	private Map<String, Tree> vertexMap = new HashMap<String, Tree>();

	private Map<String, SplitCondition> edgeMap = new HashMap<String, SplitCondition>();

	private Map<String, Double> edgeStrengthMap = new HashMap<String, Double>();

	public TreeModelGraphCreator(TreeModel model) {
		this.model = model;
	}

	public Tree getTree(String id) {
		return vertexMap.get(id);
	}

	@Override
	public String getVertexName(String object) {
		Tree node = vertexMap.get(object);
		String name = "";
		if (node != null) {
			if (node.isLeaf()) {
				name = node.getLabel();
			} else {
				Iterator<Edge> e = node.childIterator();
				while (e.hasNext()) {
					SplitCondition condition = e.next().getCondition();
					name = condition.getAttributeName();
					break;
				}
			}
		}
		return name;
	}

	@Override
	public String getVertexToolTip(String object) {
		Tree tree = vertexMap.get(object);
		if (tree != null) {
			StringBuffer result = new StringBuffer();
			if (tree.isLeaf()) {
				String labelString = tree.getLabel();
				if (labelString != null) {
					result.append("<html><b>Class:</b>&nbsp;" + labelString + "<br>");
					result.append("<b>Size:</b>&nbsp;" + tree.getFrequencySum() + "<br>");
					result.append("<b>Class frequencies:</b>&nbsp;"
							+ SwingTools.transformToolTipText(tree.getCounterMap().toString()) + "</html>");
				}
			} else {
				result.append("<html><b>Subtree Size:</b>&nbsp;" + tree.getSubtreeFrequencySum() + "</html>");
			}
			return result.toString();
		} else {
			return null;
		}
	}

	@Override
	public String getEdgeName(String object) {
		SplitCondition condition = edgeMap.get(object);
		if (condition != null) {
			return condition.getRelation() + " " + condition.getValueString();
		} else {
			return null;
		}
	}

	@Override
	public boolean isLeaf(String object) {
		Tree tree = vertexMap.get(object);
		if (tree != null) {
			return tree.isLeaf();
		} else {
			return false;
		}
	}

	@Override
	public Graph<String, String> createGraph() {
		// edu.uci.ics.jung.graph.Tree<String,String> treeGraph = new DelegateTree<String,String>();
		DirectedOrderedSparseMultigraph<String, String> treeGraph = new DirectedOrderedSparseMultigraph<String, String>();

		Tree root = this.model.getRoot();
		treeGraph.addVertex("Root");
		vertexMap.put("Root", root);
		addTree(treeGraph, root, "Root");

		return new DelegateForest<String, String>(treeGraph);
		// return treeGraph;
	}

	private void addTree(DirectedOrderedSparseMultigraph<String, String> treeGraph, Tree node, String parentName) {
		Iterator<Edge> e = node.childIterator();
		double edgeWeightSum = 0.0d;
		while (e.hasNext()) {
			Edge edge = e.next();
			Tree child = edge.getChild();
			edgeWeightSum += child.getSubtreeFrequencySum();
		}

		e = node.childIterator();
		while (e.hasNext()) {
			Edge edge = e.next();
			Tree child = edge.getChild();
			SplitCondition condition = edge.getCondition();
			String childName = vertexFactory.create();
			String edgeName = edgeFactory.create();
			vertexMap.put(childName, child);
			edgeMap.put(edgeName, condition);
			edgeStrengthMap.put(edgeName, child.getSubtreeFrequencySum() / edgeWeightSum);
			treeGraph.addEdge(edgeName, parentName, childName);
			addTree(treeGraph, child, childName);
		}
	}

	/**
	 * Returns the model.
	 */
	public TreeModel getModel() {
		return model;
	}

	@Override
	public Vertex<String, String> getVertexRenderer() {
		int maxSize = -1;
		Tree root = model.getRoot();
		maxSize = getMaximumLeafSize(root, maxSize);
		return new TreeModelNodeRenderer<String, String>(this, maxSize);
	}

	private int getMaximumLeafSize(Tree tree, int max) {
		if (tree.isLeaf()) {
			return Math.max(max, tree.getFrequencySum());
		} else {
			Iterator<Edge> e = tree.childIterator();
			int maximum = max;
			while (e.hasNext()) {
				Edge edge = e.next();
				Tree child = edge.getChild();
				maximum = Math.max(maximum, getMaximumLeafSize(child, maximum));
			}
			return maximum;
		}
	}

	@Override
	public EdgeLabel<String, String> getEdgeLabelRenderer() {
		return new TreeModelEdgeLabelRenderer<String, String>();
	}

	@Override
	public VertexLabel<String, String> getVertexLabelRenderer() {
		return new TreeModelNodeLabelRenderer<String, String>(this);
	}

	@Override
	public boolean isEdgeLabelDecorating() {
		return true;
	}

	@Override
	public int getMinLeafHeight() {
		return 26;
	}

	@Override
	public int getMinLeafWidth() {
		return 40;
	}

	@Override
	public boolean isBold(String id) {
		return isLeaf(id);
	}

	@Override
	public boolean isRotatingEdgeLabels() {
		return false;
	}

	@Override
	public double getEdgeStrength(String id) {
		Double value = edgeStrengthMap.get(id);
		if (value == null) {
			return 1.0d;
		} else {
			if (Double.isNaN(value)) {
				return 1.0d;
			} else {
				return value;
			}
		}
	}

	@Override
	public Object getObject(String id) {
		return vertexMap.get(id);
	}

	/** Returns 0 (for other values the edge label painting will not work). */
	@Override
	public int getLabelOffset() {
		return 0;
	}
}
