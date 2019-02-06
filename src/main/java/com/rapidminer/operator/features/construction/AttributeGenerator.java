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
import com.rapidminer.example.Tools;
import com.rapidminer.example.set.AttributeWeightedExampleSet;
import com.rapidminer.generator.FeatureGenerator;
import com.rapidminer.tools.RandomGenerator;

import java.util.ArrayList;
import java.util.LinkedList;
import java.util.List;


/**
 * This PopulationOperator generates new attributes in an individual's example table. Given a
 * generation probability
 * <tt>pGenerate</p> and the maximal number of new attributes it generates on average
 *  <tt>pGenerate</tt> * <tt>numberOfNewAttributes</tt> new attributes using generators from the
 * list <tt>generatorList</tt> <br/>
 * 
 * This operator can never handle value series but only single attributes.
 * 
 * @author Ingo Mierswa
 */
public class AttributeGenerator extends ExampleSetBasedIndividualOperator {

	/**
	 * Probability to generate a new attribute.
	 */
	private double pGenerate;

	/**
	 * Maximal number of newly generated attributes.
	 */
	private int numberOfNewAttributes;

	/**
	 * The total maximum number of new attributes.
	 */
	private int totalMaxNumberOfAttributes;

	/**
	 * A list of applicable generators.
	 */
	private List<FeatureGenerator> generatorList;

	private RandomGenerator random;

	/**
	 * Creates a new <tt>AttributeGenerator</tt> with given parameters.
	 */
	public AttributeGenerator(double pGenerate, int numberOfNewAttributes, int totalMaxNumberOfAttributes,
			List<FeatureGenerator> generatorList, RandomGenerator random) {
		this.pGenerate = pGenerate;
		this.numberOfNewAttributes = numberOfNewAttributes;
		this.totalMaxNumberOfAttributes = totalMaxNumberOfAttributes;
		this.generatorList = generatorList;
		this.random = random;
	}

	/**
	 * Determines the applicable generators and generates up to <tt>numberOfNewAttributes</tt> new
	 * attributes.
	 */
	@Override
	public List<ExampleSetBasedIndividual> operate(ExampleSetBasedIndividual individual) throws Exception {
		AttributeWeightedExampleSet exampleSet = individual.getExampleSet();
		ArrayList<FeatureGenerator> selectedGeneratorList = new ArrayList<FeatureGenerator>();

		if ((totalMaxNumberOfAttributes < 0) || (exampleSet.getAttributes().size() < totalMaxNumberOfAttributes)) {
			for (int h = 0; h < numberOfNewAttributes; h++) {
				if (random.nextDouble() < pGenerate) {
					// random selection of an applicable generator
					FeatureGenerator generator = FeatureGenerator.selectGenerator(exampleSet, generatorList, new String[0],
							random);
					if (generator != null) {
						generator = generator.newInstance();
						// search necessary features
						Attribute[] args = Tools.getRandomCompatibleAttributes(exampleSet, generator, new String[0], random);
						generator.setArguments(args);
						// add selected feature generator to list
						selectedGeneratorList.add(generator);
					}
				}
			}
			if (selectedGeneratorList.size() > 0) {
				// apply selected generators on the current example set
				List<Attribute> newAttributes = FeatureGenerator.generateAll(exampleSet.getExampleTable(),
						selectedGeneratorList);
				for (Attribute newAttribute : newAttributes) {
					exampleSet.getAttributes().addRegular(newAttribute);
				}
			}
		}

		List<ExampleSetBasedIndividual> result = new LinkedList<ExampleSetBasedIndividual>();
		result.add(new ExampleSetBasedIndividual(exampleSet));
		return result;
	}
}
