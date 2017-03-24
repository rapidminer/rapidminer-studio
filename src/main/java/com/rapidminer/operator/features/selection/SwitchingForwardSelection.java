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
package com.rapidminer.operator.features.selection;

import com.rapidminer.operator.features.Individual;
import com.rapidminer.operator.features.IndividualOperator;

import java.util.LinkedList;
import java.util.List;


/**
 * This PopulationOperator realises forward selection, i.e. creates a list of clones of each
 * individual and switches on one attribute in each of the clones. In contrast to the normal forward
 * selection this one actually selects attributes instead of completely adding them. Although this
 * might use more memory for simple selection tasks this might be necessary in case of
 * FeatureOperators which generate new attributes.
 * 
 * @author Ingo Mierswa ingomierswa Exp $
 */
public class SwitchingForwardSelection extends IndividualOperator {

	@Override
	public List<Individual> operate(Individual individual) {
		double[] weights = individual.getWeightsClone();
		List<Individual> l = new LinkedList<Individual>();
		for (int i = 0; i < weights.length; i++) {
			if (weights[i] == 0.0d) {
				double[] clone = individual.getWeightsClone();
				clone[i] = 1.0d;
				l.add(new Individual(clone));
			}
		}
		return l;
	}
}
