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
package com.rapidminer.operator.features.construction;

import com.rapidminer.example.Attribute;
import com.rapidminer.example.set.AttributeWeightedExampleSet;

import java.util.LinkedList;
import java.util.List;
import java.util.Random;


/**
 * Inverts the used bit for every feature of every example set with a given fixed probability.
 * 
 * @author Ingo Mierswa Exp $
 */
public class ExampleSetBasedSelectionMutation extends ExampleSetBasedIndividualOperator {

	private double probability;

	private Random random;

	private int minNumber;

	private int maxNumber;

	private int exactNumber;

	public ExampleSetBasedSelectionMutation(double probability, Random random, int minNumber, int maxNumber, int exactNumber) {
		this.probability = probability;
		this.random = random;
		this.minNumber = minNumber;
		this.maxNumber = maxNumber;
		this.exactNumber = exactNumber;
	}

	@Override
	public List<ExampleSetBasedIndividual> operate(ExampleSetBasedIndividual individual) {
		List<ExampleSetBasedIndividual> l = new LinkedList<ExampleSetBasedIndividual>();
		AttributeWeightedExampleSet clone = new AttributeWeightedExampleSet(individual.getExampleSet());
		double prob = probability < 0 ? 1.0d / clone.getAttributes().size() : probability;
		for (Attribute attribute : clone.getAttributes()) {
			if (random.nextDouble() < prob) {
				clone.flipAttributeUsed(attribute);
			}
		}

		int numberOfFeatures = clone.getNumberOfUsedAttributes();
		if (numberOfFeatures > 0) {
			if (exactNumber > 0) {
				if (numberOfFeatures == exactNumber) {
					l.add(new ExampleSetBasedIndividual(clone));
				}
			} else {
				if (((maxNumber < 1) || (numberOfFeatures <= maxNumber)) && (numberOfFeatures >= minNumber)) {
					l.add(new ExampleSetBasedIndividual(clone));
				}
			}
		}

		// add also original ES
		l.add(individual);
		return l;
	}
}
