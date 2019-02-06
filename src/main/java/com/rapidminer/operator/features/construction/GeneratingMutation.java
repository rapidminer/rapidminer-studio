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
 * The mutation operator for YAGGA. This operator adds single attributes from the original set,
 * creates new ones and deselect single attributes.
 * 
 * @see YAGGA
 * @author Ingo Mierswa, Simon Fischer Exp $
 */
public class GeneratingMutation extends ExampleSetBasedIndividualOperator {

	private List<FeatureGenerator> generators;

	private List<Attribute> originalAttributes;

	private double prob;

	private int maxNumberOfAttributes;

	private RandomGenerator random;

	private String[] unusedFunctions = new String[0];

	public GeneratingMutation(List<Attribute> originalAttributes, double prob, int maxNumberOfAttributes,
			List<FeatureGenerator> generators, RandomGenerator random) {
		this.originalAttributes = originalAttributes;
		this.prob = prob;
		this.maxNumberOfAttributes = maxNumberOfAttributes;
		this.generators = generators;
		this.random = random;
	}

	public void setUnusedFunctions(String[] functions) {
		this.unusedFunctions = functions;
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
		double p = prob < 0 ? 1.0d / clone.getAttributes().size() : prob;
		p /= 4.0d;
		try {
			if ((maxNumberOfAttributes < 0) || (clone.getAttributes().size() < maxNumberOfAttributes)) {
				addOriginalAttribute(clone, p);
			}
			boolean generationPossible = false;
			if ((maxNumberOfAttributes < 0) || (clone.getAttributes().size() <= maxNumberOfAttributes)) {
				generationPossible = addGeneratedAttribute(clone, p);
			}
			deselect(clone, generationPossible ? 2 : 1, p);
		} catch (GenerationException e) {
			individual
					.getExampleSet()
					.getLog()
					.logWarning(
							"GeneratingMutation: Exception occured during generation of attributes, using only original example set instead.");
		}

		if (clone.getNumberOfUsedAttributes() > 0) {
			l.add(new ExampleSetBasedIndividual(clone));
		}
		l.add(individual);
		return l;
	}

	/** Adds a new attribute. Returns true, if generation was possible. */
	private boolean addGeneratedAttribute(AttributeWeightedExampleSet exampleSet, double p) throws Exception {
		if (random.nextDouble() < p) {
			FeatureGenerator generator = FeatureGenerator.selectGenerator(exampleSet, generators, unusedFunctions, random);
			if (generator != null) {
				generator = generator.newInstance();
				Attribute[] args = Tools.getRandomCompatibleAttributes(exampleSet, generator, unusedFunctions, random);
				generator.setArguments(args);
				List<FeatureGenerator> generatorList = new LinkedList<FeatureGenerator>();
				generatorList.add(generator);
				List<Attribute> newAttributes = FeatureGenerator.generateAll(exampleSet.getExampleTable(), generatorList);
				for (Attribute newAttribute : newAttributes) {
					exampleSet.getAttributes().addRegular(newAttribute);
				}
				return true;
			} else {
				return false;
			}
		} else {
			return false;
		}
	}

	private void addOriginalAttribute(AttributeWeightedExampleSet exampleSet, double p) {
		if (random.nextDouble() < p) {
			int i = random.nextInt(originalAttributes.size());
			Attribute originalAttribute = originalAttributes.get(i);
			if (exampleSet.getAttributes().get(originalAttribute.getName()) == null) {
				exampleSet.getAttributes().addRegular(originalAttribute);
			}
		}
	}

	private void deselect(AttributeWeightedExampleSet exampleSet, int m, double p) {
		Iterator<Attribute> i = exampleSet.getAttributes().iterator();
		while (i.hasNext()) {
			i.next();
			if (random.nextDouble() < m * p / originalAttributes.size()) {
				i.remove();
			}
		}
	}
}
