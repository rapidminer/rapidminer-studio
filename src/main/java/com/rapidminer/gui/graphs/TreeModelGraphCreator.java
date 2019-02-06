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
package com.rapidminer.gui.graphs;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Set;

import org.apache.commons.collections15.Factory;

import com.rapidminer.gui.tools.SwingTools;
import com.rapidminer.operator.learner.tree.Edge;
import com.rapidminer.operator.learner.tree.NominalSplitCondition;
import com.rapidminer.operator.learner.tree.SplitCondition;
import com.rapidminer.operator.learner.tree.Tree;
import com.rapidminer.operator.learner.tree.TreePredictionModel;
import com.rapidminer.tools.Tools;

import edu.uci.ics.jung.graph.DelegateForest;
import edu.uci.ics.jung.graph.DirectedOrderedSparseMultigraph;
import edu.uci.ics.jung.graph.Graph;
import edu.uci.ics.jung.visualization.renderers.Renderer.EdgeLabel;
import edu.uci.ics.jung.visualization.renderers.Renderer.Vertex;
import edu.uci.ics.jung.visualization.renderers.Renderer.VertexLabel;


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

	private TreePredictionModel model;

	private Map<String, Tree> vertexMap = new HashMap<>();

	private Map<String, SplitCondition> edgeMap = new HashMap<>();

	private Map<String, Double> edgeStrengthMap = new HashMap<>();

	private Map<String, List<String>> pathToRootMap = new HashMap<>();

	public TreeModelGraphCreator(TreePredictionModel model) {
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
			return createTooltip(tree);
		} else {
			return null;
		}
	}

	@Override
	public String getEdgeName(String object) {
		SplitCondition condition = edgeMap.get(object);
		if (condition != null) {
			if (condition instanceof NominalSplitCondition) {
				return condition.getValueString();
			} else {
				return condition.getRelation() + " " + condition.getValueString();
			}
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
	public boolean isEdgeOnSelectedPath(Set<String> selectedVertexes, String id) {
		// both edges and vertexes are in the same map
		return isVertexOnSelectedPath(selectedVertexes, id);
	}

	@Override
	public boolean isVertexOnSelectedPath(Set<String> selectedVertexes, String id) {
		boolean onPath = false;
		for (String selectedVertex : selectedVertexes) {
			List<String> pathElements = pathToRootMap.get(selectedVertex);
			if (pathElements != null) {
				onPath = pathElements.contains(id);
			}

			if (onPath) {
				return true;
			}
		}

		return false;
	}

	@Override
	public Graph<String, String> createGraph() {
		DirectedOrderedSparseMultigraph<String, String> treeGraph = new DirectedOrderedSparseMultigraph<String, String>();

		Tree root = this.model.getRoot();
		treeGraph.addVertex("Root");
		vertexMap.put("Root", root);
		addTree(treeGraph, root, "Root", new ArrayList<>());

		return new DelegateForest<String, String>(treeGraph);
	}

	private void addTree(DirectedOrderedSparseMultigraph<String, String> treeGraph, Tree node, String parentName,
			List<String> currentParentList) {
		Iterator<Edge> e = node.childIterator();
		double edgeWeightSum = model.getRoot().getSubtreeFrequencySum();

		e = node.childIterator();
		while (e.hasNext()) {
			Edge edge = e.next();
			Tree child = edge.getChild();
			SplitCondition condition = edge.getCondition();
			String childName = vertexFactory.create();
			String edgeName = edgeFactory.create();
			currentParentList.add(parentName);
			currentParentList.add(edgeName);
			pathToRootMap.put(edgeName, currentParentList);
			pathToRootMap.put(childName, currentParentList);

			vertexMap.put(childName, child);
			edgeMap.put(edgeName, condition);
			edgeStrengthMap.put(edgeName, child.getSubtreeFrequencySum() / edgeWeightSum);
			treeGraph.addEdge(edgeName, parentName, childName);
			addTree(treeGraph, child, childName, new ArrayList<>(currentParentList));

			// siblings would use the same list with wrong edges if we did not copy
			currentParentList = new ArrayList<String>(currentParentList);
			currentParentList.remove(edgeName);
		}
	}

	/**
	 * Returns the model.
	 */
	public TreePredictionModel getModel() {
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
		return 45;
	}

	@Override
	public int getMinLeafWidth() {
		return 70;
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

	/**
	 * Create the tooltip for a tree node.
	 *
	 * @param tree
	 *            the tree for which to create the tooltip
	 * @return the HTML-formatted tooltip string
	 */
	private String createTooltip(Tree tree) {
		StringBuilder sb = new StringBuilder();

		if (tree.isLeaf()) {
			String labelString = tree.getLabel();
			if (labelString != null) {
				sb.append(
						"<html><div style=\"font-size: 10px; font-family: 'Open Sans'\"><p style=\"font-size: 110%; text-align: center; font-family: 'Open Sans Semibold'\"><b>"
								+ labelString + "</b><hr NOSHADE style=\"color: '#000000'; width: 95%; \"/></p>");
				if (!tree.isNumerical()) {
					sb.append(SwingTools.transformToolTipText(formatCounterMap(tree.getCounterMap()), false, 200, false, false)
							+ "<br/>");
				}
				sb.append("Number of items:&nbsp;" + tree.getFrequencySum() + "<br/>");
				sb.append("Ratio of total:&nbsp;"
						+ Tools.formatPercent((double) tree.getFrequencySum() / model.getRoot().getSubtreeFrequencySum()));
				sb.append("</div></html>");
			}
		} else {
			sb.append("<html><div style=\"font-size: 10px; font-family: 'Open Sans'\">");
			sb.append("<p>" + tree.getSubtreeFrequencySum() + " items in subtree</p><br/>");
			sb.append("Ratio of total:&nbsp;" + Tools
					.formatPercent((double) tree.getSubtreeFrequencySum() / model.getRoot().getSubtreeFrequencySum()));
			sb.append("</div></html>");
		}

		return sb.toString();
	}

	/**
	 * Displays the tree counterMap contents nicely formatted for humans.
	 *
	 * @param counterMap
	 *            the counter map of the tree to format
	 * @return the formatted string
	 */
	private static String formatCounterMap(Map<String, Integer> counterMap) {
		StringBuilder sb = new StringBuilder();

		sb.append("Distribution: ");
		int size = counterMap.size();
		for (Entry<String, Integer> item : counterMap.entrySet()) {
			sb.append(item.getValue());
			sb.append(' ');
			sb.append("<i>");
			sb.append(item.getKey());
			sb.append("</i>");
			if (--size > 0) {
				sb.append(',').append(' ');
			}
		}

		return sb.toString();
	}
}
