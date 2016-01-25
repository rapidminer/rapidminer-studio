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

import com.rapidminer.operator.clustering.ClusterModel;
import com.rapidminer.operator.clustering.HierarchicalClusterModel;
import com.rapidminer.operator.clustering.HierarchicalClusterNode;
import edu.uci.ics.jung.graph.DelegateTree;
import edu.uci.ics.jung.graph.Graph;
import edu.uci.ics.jung.graph.Tree;

import java.util.HashMap;
import java.util.Map;

import org.apache.commons.collections15.Factory;


/**
 * The graph model creator for cluster models.
 * 
 * @author Ingo Mierswa
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

	private Map<String, HierarchicalClusterNode> vertexMap = new HashMap<String, HierarchicalClusterNode>();

	private ClusterModelObjectViewer objectViewer;

	public ClusterModelGraphCreator(ClusterModel clusterModel) {
		this(new HierarchicalClusterModel(clusterModel));
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
		HierarchicalClusterNode node = vertexMap.get(id);
		String name = "";
		if (node != null) {
			name = node.getClusterId();
		}
		return name;
	}

	@Override
	public String getVertexToolTip(String id) {
		HierarchicalClusterNode node = vertexMap.get(id);
		String tip = "";
		if (node != null) {
			tip = "<html><b>Id:</b>&nbsp;" + node.getClusterId();
		}
		return tip;
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
}
