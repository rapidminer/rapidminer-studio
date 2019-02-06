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
package com.rapidminer.operator.learner.local;

import com.rapidminer.parameter.ParameterHandler;
import com.rapidminer.parameter.ParameterType;
import com.rapidminer.parameter.ParameterTypeDouble;
import com.rapidminer.parameter.ParameterTypeInt;
import com.rapidminer.parameter.UndefinedParameterError;
import com.rapidminer.tools.container.Tupel;
import com.rapidminer.tools.math.container.GeometricDataCollection;

import java.io.Serializable;
import java.util.Collection;
import java.util.LinkedList;
import java.util.List;


/**
 * This neighborhood adds all points, which are inside the specified distance. If less than the
 * minimal number of neighbors are found, the neighborhood distance is increased until the minimal
 * number of neighbors are found.
 * 
 * @author Sebastian Land
 * 
 */
public class AtLeastNeighborhood implements Neighborhood {

	private static final long serialVersionUID = -9140050953901279562L;
	public static final String PARAMETER_AT_LEAST = "at_least";
	public static final String PARAMETER_DISTANCE = "distance";

	private double distance;
	private int minK;

	@Override
	public <T extends Serializable> Collection<Tupel<Double, T>> getNeighbourhood(GeometricDataCollection<T> samples,
			double[] probePoint) {
		return samples.getNearestValueDistances(distance, minK, probePoint);
	}

	@Override
	public List<ParameterType> getParameterTypes(ParameterHandler handler) {
		List<ParameterType> types = new LinkedList<ParameterType>();
		ParameterType type = new ParameterTypeDouble(PARAMETER_DISTANCE,
				"Specifies the size of the neighborhood. All points within this distance are added.", 0,
				Double.POSITIVE_INFINITY, 10d);
		type.setExpert(false);
		types.add(type);
		type = new ParameterTypeInt(PARAMETER_AT_LEAST,
				"If the neighborhood count is less than this number, the distance is increased until this number is met.",
				0, Integer.MAX_VALUE, 20);
		type.setExpert(false);
		types.add(type);
		return types;
	}

	@Override
	public void init(ParameterHandler handler) throws UndefinedParameterError {
		this.distance = handler.getParameterAsDouble(PARAMETER_DISTANCE);
		this.minK = handler.getParameterAsInt(PARAMETER_AT_LEAST);
	}

	@Override
	public String toString() {
		return "At Least Neighborhoor";
	}
}
