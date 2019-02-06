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

/**
 * This class only indicates that this model is providing information for plotting a dendogram.
 * 
 * @author Sebastian Land
 */
public class DendogramHierarchicalClusterModel extends HierarchicalClusterModel {

	private static final long serialVersionUID = 941706772535944222L;

	public DendogramHierarchicalClusterModel(ClusterModel clusterModel) {
		super(clusterModel);
	}

	public DendogramHierarchicalClusterModel(HierarchicalClusterNode root) {
		super(root);
	}
}
