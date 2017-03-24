/**
 * Copyright (C) 2001-2017 by RapidMiner and the contributors
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
package com.rapidminer.operator.clustering.clusterer;

import com.rapidminer.operator.clustering.HierarchicalClusterNode;

import java.util.Map;


/**
 * This class provides the complete linkage method for the AgglomerativeClustering.
 * 
 * @author Sebastian Land
 */
public class CompleteLinkageMethod extends AbstractLinkageMethod {

	public CompleteLinkageMethod(DistanceMatrix matrix, int[] clusterIds) {
		super(matrix, clusterIds);
	}

	@Override
	public void updateDistances(DistanceMatrix matrix, int updatedRow, int unionedRow,
			Map<Integer, HierarchicalClusterNode> clusterMap) {
		for (int y = 0; y < matrix.getHeight(); y++) {
			matrix.set(updatedRow, y, Math.max(matrix.get(updatedRow, y), matrix.get(unionedRow, y)));
		}
	}
}
