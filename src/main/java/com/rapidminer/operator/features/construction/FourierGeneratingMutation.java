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
import com.rapidminer.generator.AttributePeak;
import com.rapidminer.generator.FeatureGenerator;
import com.rapidminer.generator.GenerationException;
import com.rapidminer.generator.SinusFactory;
import com.rapidminer.operator.OperatorException;
import com.rapidminer.tools.RandomGenerator;

import java.util.Iterator;
import java.util.LinkedList;
import java.util.List;


/**
 * The mutation operator for FourierGGA. This operator adds single attributes from the original set,
 * creates new ones and deselect single attributes. For each freshly added attribute a fourier
 * generation will will be performed.
 * 
 * @see FourierGGA
 * @author Ingo Mierswa ingomierswa Exp $
 */
public class FourierGeneratingMutation extends ExampleSetBasedIndividualOperator {

	private List<FeatureGenerator> generators;

	private List<Attribute> originalAttributes;

	private double p;

	private int numberOfConstructed;

	private int numberOfOriginal;

	private SinusFactory factory = null;

	private String[] unusableFunctions = new String[0];

	private RandomGenerator random;

	public FourierGeneratingMutation(List<Attribute> originalAttributes, double p, List<FeatureGenerator> generators,
			int numberOfConstructed, int numberOfOriginal, int maxPeaks, int adaptionType, int attributesPerPeak,
			double epsilon, String[] unusableFunctions, RandomGenerator random) {
		this.originalAttributes = originalAttributes;
		this.p = p;
		this.generators = generators;
		this.numberOfConstructed = numberOfConstructed;
		this.numberOfOriginal = numberOfOriginal;
		this.factory = new SinusFactory(maxPeaks);
		this.factory.setAdaptionType(adaptionType);
		this.factory.setEpsilon(epsilon);
		this.factory.setAttributePerPeak(attributesPerPeak);
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
		LinkedList<ExampleSetBasedIndividual> l = new LinkedList<ExampleSetBasedIndividual>();
		AttributeWeightedExampleSet clone = new AttributeWeightedExampleSet(individual.getExampleSet());

		try {
			int numberOriginal = addOriginalAttribute(clone);
			int numberCreated = addGeneratedAttribute(clone);
			deselect(clone, numberOriginal + numberCreated);
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
	private int addGeneratedAttribute(AttributeWeightedExampleSet exampleSet) throws OperatorException {
		int counter = 0;
		for (int k = 0; k < numberOfConstructed; k++) {
			if (random.nextDouble() < p) {
				FeatureGenerator generator = FeatureGenerator.selectGenerator(exampleSet, generators, unusableFunctions,
						random);
				if (generator != null) {
					generator = generator.newInstance();
					Attribute[] args = Tools.getRandomCompatibleAttributes(exampleSet, generator, unusableFunctions, random);
					generator.setArguments(args);
					List<FeatureGenerator> generatorList = new LinkedList<FeatureGenerator>();
					generatorList.add(generator);

					List<Attribute> newAttributes = FeatureGenerator
							.generateAll(exampleSet.getExampleTable(), generatorList);
					for (Attribute newAttribute : newAttributes) {
						exampleSet.getAttributes().addRegular(newAttribute);
					}
					counter += newAttributes.size();

					Iterator<Attribute> i = newAttributes.iterator();
					List<AttributePeak> sinAttributes = new LinkedList<AttributePeak>();
					Attribute label = exampleSet.getAttributes().getLabel();
					while (i.hasNext()) {
						Attribute current = i.next();
						if (current.isNumerical() && (current.getConstruction().indexOf("sin") == -1)) {
							List<AttributePeak> peaks = factory.getAttributePeaks(exampleSet, label, current);
							sinAttributes.addAll(peaks);
						}
					}

					if (sinAttributes.size() > 0) {
						factory.generateSinusFunctions(exampleSet, sinAttributes, random);
					}
					counter += sinAttributes.size();
				}
			}
		}
		return counter;
	}

	private int addOriginalAttribute(AttributeWeightedExampleSet exampleSet) throws GenerationException, OperatorException {
		int counter = 0;
		for (int k = 0; k < numberOfOriginal; k++) {
			if (random.nextDouble() < p) {
				int i = random.nextInt(originalAttributes.size());
				Attribute originalAttribute = originalAttributes.get(i);
				if (exampleSet.getAttributes().getRegular(originalAttribute.getName()) == null) {
					exampleSet.getAttributes().addRegular(originalAttribute);
					counter++;
				}

				// add sinus functions of the original attribute
				List<AttributePeak> peaks = factory.getAttributePeaks(exampleSet, exampleSet.getAttributes().getLabel(),
						originalAttribute);
				if (peaks.size() > 0) {
					factory.generateSinusFunctions(exampleSet, peaks, random);
				}
				counter += peaks.size();
			}
		}
		return counter;
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
