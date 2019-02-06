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
import com.rapidminer.generator.ConstantGenerator;
import com.rapidminer.generator.FeatureGenerator;
import com.rapidminer.tools.RandomGenerator;

import java.util.LinkedList;
import java.util.List;


/**
 * This PopulationOperator generates new random constant attribute in an individual's example table.
 * 
 * @author Ingo Mierswa Exp $
 */
public class ConstantGeneration extends ExampleSetBasedIndividualOperator {

	/** The probability for generating new constants. */
	private double prob;

	private RandomGenerator random;

	/** Creates a new constant generator. */
	public ConstantGeneration(double prob, RandomGenerator random) {
		this.prob = prob;
		this.random = random;
	}

	@Override
	public List<ExampleSetBasedIndividual> operate(ExampleSetBasedIndividual individual) throws Exception {
		AttributeWeightedExampleSet exampleSet = individual.getExampleSet();
		if (random.nextDouble() < prob) {
			FeatureGenerator generator = new ConstantGenerator(random.nextDoubleInRange(-10.0d, 10.0d));
			List<FeatureGenerator> generatorList = new LinkedList<FeatureGenerator>();
			generatorList.add(generator);
			List<Attribute> newAttributes = FeatureGenerator.generateAll(exampleSet.getExampleTable(), generatorList);
			for (Attribute newAttribute : newAttributes) {
				exampleSet.getAttributes().addRegular(newAttribute);
			}
		}
		List<ExampleSetBasedIndividual> result = new LinkedList<ExampleSetBasedIndividual>();
		result.add(new ExampleSetBasedIndividual(exampleSet));
		return result;
	}
}
