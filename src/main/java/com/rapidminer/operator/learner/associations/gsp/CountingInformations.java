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
package com.rapidminer.operator.learner.associations.gsp;

import java.io.Serializable;
import java.util.ArrayList;


/**
 * Simple data holding class to avoid shifting to much information over stack
 * 
 * @author Sebastian Land
 * 
 */
public class CountingInformations implements Serializable {

	private static final long serialVersionUID = 8189264534462569310L;

	public double windowSize;
	public double maxGap;
	public double minGap;
	public ArrayList<Sequence> allCandidates;
	public boolean[] candidateCounter;

	public CountingInformations(boolean[] candidateCounter, ArrayList<Sequence> allCandidates, double windowSize,
			double maxGap, double minGap) {
		this.candidateCounter = candidateCounter;
		this.windowSize = windowSize;
		this.maxGap = maxGap;
		this.minGap = minGap;
		this.allCandidates = allCandidates;
	}
}
