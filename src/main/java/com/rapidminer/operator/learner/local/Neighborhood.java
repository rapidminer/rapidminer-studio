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
import com.rapidminer.parameter.UndefinedParameterError;
import com.rapidminer.tools.container.Tupel;
import com.rapidminer.tools.math.container.GeometricDataCollection;

import java.io.Serializable;
import java.util.Collection;
import java.util.List;


/**
 * This interface provides methods to construct a neighborhood from a specified geometric data
 * collection. The center of neighborhood is specified to.
 * 
 * Additionally, each subclass implementing Neighboorhood must be able to init themself from the
 * parameter they specify in their getParameterTypes method.
 * 
 * @author Sebastian Land
 * 
 */
public interface Neighborhood extends Serializable {

	public <T extends Serializable> Collection<Tupel<Double, T>> getNeighbourhood(GeometricDataCollection<T> samples,
			double[] probePoint);

	public void init(ParameterHandler handler) throws UndefinedParameterError;

	public List<ParameterType> getParameterTypes(ParameterHandler handler);
}
