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
package com.rapidminer.operator.features.selection;

import com.rapidminer.operator.features.Individual;
import com.rapidminer.operator.features.Population;
import com.rapidminer.operator.features.PopulationOperator;

import java.util.LinkedList;
import java.util.List;


/**
 * Creates a new population by a deterministical selection of the best individuals.
 * 
 * @author Ingo Mierswa
 */
public class CutSelection implements PopulationOperator {

	private int popSize;

	public CutSelection(int popSize) {
		this.popSize = popSize;
	}

	@Override
	public boolean performOperation(int generation) {
		return true;
	}

	@Override
	public void operate(Population population) {
		population.sort();

		List<Individual> newGeneration = new LinkedList<Individual>();
		int counter = 0;
		for (int i = population.getNumberOfIndividuals() - 1; (i >= 0) && (counter < popSize); i--) {
			newGeneration.add(population.get(i));
			counter++;
		}

		population.clear();
		population.addAllIndividuals(newGeneration);
	}
}
