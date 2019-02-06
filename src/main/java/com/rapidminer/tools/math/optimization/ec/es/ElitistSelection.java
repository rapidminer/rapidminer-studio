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
package com.rapidminer.tools.math.optimization.ec.es;

import java.util.LinkedList;
import java.util.List;


/**
 * Performs a very elitist selection by just adding the best individual for k times.
 * 
 * @author Ingo Mierswa
 */
public class ElitistSelection implements PopulationOperator {

	private int popSize = 1;

	public ElitistSelection(int popSize) {
		this.popSize = popSize;
	}

	@Override
	public void operate(Population population) {
		List<Individual> newGeneration = new LinkedList<Individual>();
		for (int i = 0; i < this.popSize; i++) {
			if (population.getBestEver() != null) {
				newGeneration.add((Individual) population.getBestEver().clone());
			} else {
				newGeneration.add((Individual) population.get(0).clone());
			}
		}
		population.clear();
		population.addAll(newGeneration);
	}
}
