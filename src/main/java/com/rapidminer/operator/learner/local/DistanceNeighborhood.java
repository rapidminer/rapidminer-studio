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
import com.rapidminer.parameter.UndefinedParameterError;
import com.rapidminer.tools.container.Tupel;
import com.rapidminer.tools.math.container.GeometricDataCollection;

import java.io.Serializable;
import java.util.Collection;
import java.util.Collections;
import java.util.List;


/**
 * @author Sebastian Land
 * 
 */
public class DistanceNeighborhood implements Neighborhood {

	private static final long serialVersionUID = -4880619185491306844L;

	public static final String PARAMETER_DISTANCE = "fixed_distance";

	private double withinDistance;

	@Override
	public <T extends Serializable> Collection<Tupel<Double, T>> getNeighbourhood(GeometricDataCollection<T> samples,
			double[] probePoint) {
		return samples.getNearestValueDistances(withinDistance, probePoint);
	}

	@Override
	public List<ParameterType> getParameterTypes(ParameterHandler handler) {
		ParameterType type = new ParameterTypeDouble(PARAMETER_DISTANCE,
				"Specifies the size of the neighborhood. All points within this distance are added.", 0,
				Double.POSITIVE_INFINITY, 5);
		type.setExpert(false);
		return Collections.singletonList(type);
	}

	@Override
	public void init(ParameterHandler handler) throws UndefinedParameterError {
		this.withinDistance = handler.getParameterAsDouble(PARAMETER_DISTANCE);
	}

	@Override
	public String toString() {
		return "Distance Neighborhood";
	}
}
