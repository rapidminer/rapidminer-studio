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
import com.rapidminer.generator.GenerationException;
import com.rapidminer.tools.RandomGenerator;

import java.util.Iterator;
import java.util.LinkedList;
import java.util.List;


/**
 * The mutation operator for directed GGAs. This operator adds single attributes from the original
 * set, creates new ones and deselect single attributes. The number of attributes remains until
 * longer or shorter example sets have proven to perform better.
 *
 * @see DirectedGGA
 * @author Ingo Mierswa ingomierswa Exp $
 */
public class DirectedGeneratingMutation extends ExampleSetBasedIndividualOperator {

	private List<FeatureGenerator> generators;

	private Attribute[] originalAttributes;

	private double p;

	private int maxGeneratedAttributes = 2;

	private int maxAddedOriginalAttributes = 2;

	private String[] unusableFunctions = new String[0];

	private RandomGenerator random;

	public DirectedGeneratingMutation(Attribute[] originalAttributes, double p, List<FeatureGenerator> generators,
			int maxGeneratedAttributes, int maxAddedOriginalAttributes, String[] unusableFunctions, RandomGenerator random) {
		this.originalAttributes = originalAttributes;
		this.p = p / (maxGeneratedAttributes + maxAddedOriginalAttributes);
		this.generators = generators;
		this.maxGeneratedAttributes = maxGeneratedAttributes;
		this.maxAddedOriginalAttributes = maxAddedOriginalAttributes;
		this.unusableFunctions = unusableFunctions;
		this.random = random;
	}

	/**
	 * Performs one of the following three mutations:
	 * <ul>
	 * <li>add a newly generated attribute</li>
	 * <li>add an original attribute</li>
	 * <li>remove an attribute</li>
	 * </ul>
	 */
	@Override
	public List<ExampleSetBasedIndividual> operate(ExampleSetBasedIndividual individual) throws Exception {
		List<ExampleSetBasedIndividual> l = new LinkedList<ExampleSetBasedIndividual>();
		AttributeWeightedExampleSet clone = new AttributeWeightedExampleSet(individual.getExampleSet());

		try {
			addOriginalAttribute(clone);
			addGeneratedAttribute(clone);
			deselect(clone, maxGeneratedAttributes + maxAddedOriginalAttributes);

			if (clone.getNumberOfUsedAttributes() > 0) {
				l.add(new ExampleSetBasedIndividual(clone));
			}
		} catch (GenerationException e) {
			individual
					.getExampleSet()
					.getLog()
					.logWarning(
							"DirectedGGA: Exception occured during generation of attributes, using only original example set instead.");
		}
		l.add(individual);
		return l;
	}

	private void addGeneratedAttribute(AttributeWeightedExampleSet exampleSet) throws GenerationException {
		for (int i = 0; i < maxGeneratedAttributes; i++) {
			if (random.nextDouble() < p) {
				FeatureGenerator generator = FeatureGenerator.selectGenerator(exampleSet, generators, unusableFunctions,
						random);
				if (generator != null) {
					generator = generator.newInstance();
					Attribute[] args = Tools.getWeightedCompatibleAttributes(exampleSet, generator, unusableFunctions,
							random);
					generator.setArguments(args);

					List<FeatureGenerator> generatorList = new LinkedList<FeatureGenerator>();
					generatorList.add(generator);
					List<Attribute> result = FeatureGenerator.generateAll(exampleSet.getExampleTable(), generatorList);
					double weightSum = 0.0d;
					for (int j = 0; j < args.length; j++) {
						weightSum += exampleSet.getWeight(args[j]);
					}
					weightSum /= args.length;

					for (Attribute newAttribute : result) {
						exampleSet.getAttributes().addRegular(newAttribute);
					}

					Iterator<Attribute> a = result.iterator();
					while (a.hasNext()) {
						exampleSet.setWeight(a.next(), weightSum);
					}
				}
			}
		}
	}

	private void addOriginalAttribute(AttributeWeightedExampleSet exampleSet) {
		for (int j = 0; j < maxAddedOriginalAttributes; j++) {
			if (random.nextDouble() < p) {
				Attribute originalAttribute = originalAttributes[random.nextInt(originalAttributes.length)];
				double avgWeight = Tools.getAverageWeight(exampleSet);
				if (!exampleSet.getAttributes().contains(originalAttribute)) {
					exampleSet.getAttributes().addRegular(originalAttribute);
					exampleSet.setWeight(originalAttribute, avgWeight);
				}
			}
		}
	}

	private void deselect(AttributeWeightedExampleSet exampleSet, int numberNew) {
		double[] probs = Tools.getInverseProbabilitiesFromWeights(exampleSet.getAttributes().createRegularAttributeArray(),
				exampleSet);
		Iterator<Attribute> i = exampleSet.getAttributes().iterator();
		int index = 0;
		while (i.hasNext()) {
			i.next();
			if (random.nextDouble() < p * probs[index++] * numberNew) {
				i.remove();
			}
		}
	}
}
