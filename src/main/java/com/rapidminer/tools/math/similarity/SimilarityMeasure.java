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
package com.rapidminer.tools.math.similarity;

/**
 * This empty interface only indicates if a measure is more primary a similarity measure than a
 * distance measure. Nevertheless both measure types have to implement both methods properly, so
 * this interface is only for programmers orientation NOT for testing with instanceof!
 * 
 * @author Sebastian Land
 */
public abstract class SimilarityMeasure extends DistanceMeasure {

	private static final long serialVersionUID = -2138479771882810015L;

	/**
	 * This method returns a boolean whether this measure is a distance measure
	 * 
	 * @return true if is distance
	 */
	@Override
	public final boolean isDistance() {
		return false;
	}
}
