/**
 * Copyright (C) 2001-2017 by RapidMiner and the contributors
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
package com.rapidminer.operator.features;

import java.util.LinkedList;
import java.util.List;


/**
 * A <tt>PopulationOperator</tt> that modifies a population by modifying all individuals
 * independently. The modifications can be made each per attribute block or for single attributes.
 * 
 * @author Simon Fischer, Ingo Mierswa Exp $ <br>
 */
public abstract class IndividualOperator implements PopulationOperator {

	/**
	 * Subclasses must implement this method providing a list of new individuals.
	 * <tt>individual</tt> will be removed from the population so it might be useful to return a
	 * list of size 1 containing only the modified <tt>individual</tt>. If the original individual
	 * should also be part of the new population it must also be added to the result list.
	 */
	public abstract List<Individual> operate(Individual individual) throws Exception;

	/**
	 * Operates on all individuals, removes the original individuals and adds the new ones.
	 */
	@Override
	public void operate(Population pop) throws Exception {
		List<Individual> newIndividuals = new LinkedList<Individual>();
		for (int i = 0; i < pop.getNumberOfIndividuals(); i++) {
			List<Individual> individuals = operate(pop.get(i));
			newIndividuals.addAll(individuals);
		}
		pop.clear();
		pop.addAllIndividuals(newIndividuals);
	}

	/** The default implementation returns true for every generation. */
	@Override
	public boolean performOperation(int generation) {
		return true;
	}
}
