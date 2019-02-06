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

import com.rapidminer.operator.ResultObjectAdapter;
import com.rapidminer.tools.Tools;


/**
 * This class provides the data of a generic hierarchical cluster model. Therefore it holds a root
 * node, giving access to the complete cluster hierarchy.
 * 
 * @author Sebastian Land, Ingo Mierswa
 */
public class HierarchicalClusterModel extends ResultObjectAdapter implements ClusterModelInterface {

	private static final long serialVersionUID = 75808296412095255L;

	private HierarchicalClusterNode rootNode;

	/**
	 * Creates a hierarchical cluster model by copying a flat one.
	 * 
	 * @param clusterModel
	 *            the cluster model to copy.
	 */
	public HierarchicalClusterModel(ClusterModel clusterModel) {
		rootNode = new HierarchicalClusterNode("root");
		for (Cluster cluster : clusterModel.getClusters()) {
			rootNode.addSubNode(new HierarchicalClusterLeafNode(cluster.getClusterId(), cluster.getExampleIds()));
		}
	}

	public HierarchicalClusterModel(HierarchicalClusterNode root) {
		this.rootNode = root;
	}

	public HierarchicalClusterNode getRootNode() {
		return rootNode;
	}

	public String getExtension() {
		return "hcm";
	}

	public String getFileDescription() {
		return "Hierarchical Cluster Model";
	}

	@Override
	public String getName() {
		return "Hierarchical Cluster Model";
	}

	@Override
	public String toString() {
		StringBuffer result = new StringBuffer();
		result.append("Number of clusters :" + this.rootNode.getTotalNumberOfSubnodes() + Tools.getLineSeparator());
		result.append("Number of items :" + rootNode.getNumberOfExamplesInSubtree());
		return result.toString();
	}
}
