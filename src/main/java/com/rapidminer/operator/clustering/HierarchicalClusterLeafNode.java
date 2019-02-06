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

import java.util.Collection;
import java.util.LinkedList;


/**
 * This class is an extension of the HierarchicalClusterNode, representing the leaf nodes of a
 * hierarchical cluster tree. These Nodes are the only nodes containing the links to examples by
 * storing example ids.
 * 
 * @author Sebastian Land
 */
public class HierarchicalClusterLeafNode extends HierarchicalClusterNode {

	private static final long serialVersionUID = -8181571984420396236L;

	private Collection<Object> exampleIds;

	public HierarchicalClusterLeafNode(String clusterId) {
		super(clusterId);
	}

	public HierarchicalClusterLeafNode(String clusterId, Collection<Object> exampleIds) {
		super(clusterId);
		this.exampleIds = exampleIds;
	}

	public HierarchicalClusterLeafNode(int clusterId, Collection<Object> exampleIds) {
		this(clusterId + "", exampleIds);
	}

	public HierarchicalClusterLeafNode(int clusterId, Object exampleId) {
		super(clusterId + "");
		this.exampleIds = new LinkedList<Object>();
		this.exampleIds.add(exampleId);
	}

	/**
	 * Returns an empty collection since it is leaf
	 */
	@Override
	public Collection<HierarchicalClusterNode> getSubNodes() {
		return new LinkedList<HierarchicalClusterNode>();
	}

	/**
	 * Get the number of subnodes: Is always 0 since this node is leaf
	 */
	@Override
	public int getNumberOfSubNodes() {
		return 0;
	}

	@Override
	public double getDistance() {
		return 0d;
	}

	/**
	 * Get all objects (as representend by their IDs) in this leaf.
	 */
	@Override
	public Collection<Object> getExampleIdsInSubtree() {
		return exampleIds;
	}

	@Override
	public int getNumberOfExamplesInSubtree() {
		return exampleIds.size();
	}

	public void addSubNode(HierarchicalClusterLeafNode node) {}

	@Override
	public String toString() {
		return getClusterId() + ":" + getNumberOfExamplesInSubtree();
	}

}
