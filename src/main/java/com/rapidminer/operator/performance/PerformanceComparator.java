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
package com.rapidminer.operator.performance;

import java.io.Serializable;
import java.util.Comparator;


/**
 * Compares two {@link PerformanceVector}s.
 * 
 * @author Ingo Mierswa ingomierswa Exp $
 */
public interface PerformanceComparator extends Comparator<PerformanceVector>, Serializable {

	/** Returns a negative value iff pv2 is better than pv1. */
	@Override
	public int compare(PerformanceVector pv1, PerformanceVector pv2);

}
