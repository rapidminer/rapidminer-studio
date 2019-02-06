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
package de.dfki.madm.operator;

import java.util.LinkedList;
import java.util.List;

import com.rapidminer.operator.Operator;
import com.rapidminer.parameter.ParameterType;
import com.rapidminer.parameter.ParameterTypeCategory;


public class ClusteringAlgorithms {

	public static final String PARAMETER_CLUSTERING_ALGORITHM = "clustering_algorithm";

	/** Clustering Algorithms to use */
	public static final String[] CLUST_ALG = new String[] { "KMeans", "FastKMeans" };

	public static List<ParameterType> getParameterTypes(Operator parameterHandler) {
		List<ParameterType> list = new LinkedList<ParameterType>();
		list.add(new ParameterTypeCategory(PARAMETER_CLUSTERING_ALGORITHM, "Clustering Algorithm", CLUST_ALG, 0, false));
		return list;
	}
}
