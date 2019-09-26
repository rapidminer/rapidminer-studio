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

import java.util.HashMap;
import java.util.Map;

import org.apache.commons.collections15.Factory;

import com.rapidminer.operator.clustering.ClusterModel;
import com.rapidminer.operator.clustering.HierarchicalClusterModel;
import com.rapidminer.operator.clustering.HierarchicalClusterNode;
import com.rapidminer.tools.ObjectVisualizerService;
import com.rapidminer.tools.Tools;

import edu.uci.ics.jung.graph.DelegateTree;
import edu.uci.ics.jung.graph.Graph;
import edu.uci.ics.jung.graph.Tree;


/**
 * The graph model creator for cluster models.
 *
 * @author Ingo Mierswa, Marco Boeck
 */
public class ClusterModelGraphCreator extends GraphCreatorAdaptor {

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

	private HierarchicalClusterModel clusterModel;

	private Map<String, HierarchicalClusterNode> vertexMap = new HashMap<>();

	private Map<String, Double> ratioMap = new HashMap<>();

	private ClusterModelObjectViewer objectViewer;

	public ClusterModelGraphCreator(ClusterModel clusterModel) {
		this(new HierarchicalClusterModel(clusterModel));
		// reuse visualizer
		ObjectVisualizerService.addObjectVisualizer(this.clusterModel, ObjectVisualizerService.getVisualizerForObject(clusterModel));
	}

	public ClusterModelGraphCreator(HierarchicalClusterModel clusterModel) {
		this.clusterModel = clusterModel;
		this.objectViewer = new ClusterModelObjectViewer(clusterModel);
	}

	@Override
	public Graph<String, String> createGraph() {
		Tree<String, String> graph = new DelegateTree<String, String>();
		if (clusterModel.getRootNode() == null) {
			return graph;
		}

		HierarchicalClusterNode root = clusterModel.getRootNode();
		graph.addVertex("Root");
		vertexMap.put("Root", root);

		for (HierarchicalClusterNode subNode : clusterModel.getRootNode().getSubNodes()) {
			createGraph(graph, "Root", subNode);
		}
		return graph;
	}

	private void createGraph(Graph<String, String> graph, String parentName, HierarchicalClusterNode node) {
		String childName = vertexFactory.create();
		vertexMap.put(childName, node);
		graph.addEdge(edgeFactory.create(), parentName, childName);
		for (HierarchicalClusterNode subNode : node.getSubNodes()) {
			createGraph(graph, childName, subNode);
		}
	}

	@Override
	public String getEdgeName(String id) {
		return null;
	}

	@Override
	public String getVertexName(String id) {
		return getClusterName(vertexMap.get(id));
	}

	@Override
	public String getVertexToolTip(String id) {
		HierarchicalClusterNode node = vertexMap.get(id);
		if (node != null) {
			return createTooltip(id, node);
		} else {
			return null;
		}
	}

	@Override
	public Object getObject(String id) {
		return vertexMap.get(id);
	}

	@Override
	public GraphObjectViewer getObjectViewer() {
		return objectViewer;
	}

	/** Returns the shape of the edges. */
	@Override
	public int getEdgeShape() {
		return EDGE_SHAPE_QUAD_CURVE;
	}

	@Override
	public boolean isVertexCircle(String id) {
		return true;
	}

	@Override
	public double getVertexScale(String id) {
		Double ratio = ratioMap.get(id);
		if (ratio == null) {
			HierarchicalClusterNode node = vertexMap.get(id);
			ratio = (double) node.getNumberOfExamplesInSubtree() / clusterModel.getRootNode().getNumberOfExamplesInSubtree();
			ratioMap.put(id, ratio);
		}
		return ratio;
	}

	/**
	 * Create the tooltip for a cluster node.
	 *
	 * @param id
	 *            the id of the vertex
	 * @param node
	 *            the node for which to create the tooltip
	 * @return the HTML-formatted tooltip string
	 */
	private String createTooltip(String id, HierarchicalClusterNode node) {
		StringBuilder sb = new StringBuilder();

		String idString = getClusterName(node);
		Double ratio = ratioMap.get(id);
		if (ratio == null) {
			ratio = (double) node.getNumberOfExamplesInSubtree() / clusterModel.getRootNode().getNumberOfExamplesInSubtree();
			ratioMap.put(id, ratio);
		}
		sb.append("<html><div style=\"font-size: 10px; font-family: 'Open Sans'\">");
		sb.append("<p style=\"font-size: 110%; text-align: center; font-family: 'Open Sans Semibold'\"><b>" + idString
				+ "</b><hr NOSHADE style=\"color: '#000000'; width: 95%; \"/></p><br/>");
		sb.append("Number of items:&nbsp;" + node.getNumberOfExamplesInSubtree() + "<br/>");
		sb.append("Ratio of total:&nbsp;" + Tools.formatPercent(ratio));
		sb.append("</div></html>");

		return sb.toString();
	}

	private static String getClusterName(HierarchicalClusterNode node) {
		if (node != null) {
			String name = node.getClusterId();
			if ("root".equals(name)) {
				name = "root set";
			}
			return name;
		} else {
			return "";
		}
	}
}
