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
package com.rapidminer.operator.clustering.clusterer;

import com.rapidminer.operator.clustering.HierarchicalClusterNode;

import java.util.Map;


/**
 * This class provides the basic functionality for all linkage methods of agglomerative clustering.
 * It stores a distance matrix between all clusters and returns the next agglomeration as the
 * minimum of all distances. To save time needed to copy the matrix if two clusters are joined, it
 * is not resized, instead one row and column is not used anymore. The other row and column are
 * updated by the agglomeration methods.
 * 
 * @author Sebastian Land
 */
public abstract class AbstractLinkageMethod {

	private DistanceMatrix matrix;

	private boolean[] isDeletedData;

	private int[] clusterIds;

	public AbstractLinkageMethod(DistanceMatrix matrix, int[] clusterIds) {
		this.matrix = matrix;
		this.clusterIds = clusterIds;
		this.isDeletedData = new boolean[matrix.getHeight()];
	}

	public Agglomeration getNextAgglomeration(int nextClusterId, Map<Integer, HierarchicalClusterNode> clusterMap) {
		// searching for miniumum
		double minimalDistance = Double.POSITIVE_INFINITY;
		int minimalX = -1;
		int minimalY = -1;
		for (int x = 0; x < matrix.getWidth(); x++) {
			if (!isDeletedData[x]) {
				// searching right upper triangle of distance matrix
				for (int y = x + 1; y < matrix.getHeight(); y++) {
					if (!isDeletedData[y]) {
						double value = matrix.get(x, y);
						if (value <= minimalDistance) {
							minimalX = x;
							minimalY = y;
							minimalDistance = value;
						}
					}
				}
			}
		}
		// constructing agglomeration
		Agglomeration agglomeration = new Agglomeration(clusterIds[minimalX], clusterIds[minimalY], minimalDistance);

		// deleting y row, updating the other and rename with new cluster id
		updateDistances(matrix, minimalX, minimalY, clusterMap);
		isDeletedData[minimalY] = true;
		clusterIds[minimalX] = nextClusterId;
		return agglomeration;
	}

	public abstract void updateDistances(DistanceMatrix matrix, int updatedRow, int unionedRow,
			Map<Integer, HierarchicalClusterNode> clusterMap);
}
