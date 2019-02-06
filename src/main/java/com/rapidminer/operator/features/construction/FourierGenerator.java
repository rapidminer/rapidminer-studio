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
import com.rapidminer.generator.AttributePeak;
import com.rapidminer.generator.SinusFactory;
import com.rapidminer.tools.RandomGenerator;

import java.util.LinkedList;
import java.util.List;


/**
 * This PopulationOperator generates new attributes in an individual's example table. Each piecewise
 * continous function can be synthesized with help of a Fourier synthesis, i.e. with a sum of
 * trigonometric function with different frequencies and phases. This pop op searches for the best
 * frequencies and construct the corresponding trigonometric functions as new attributes.
 * 
 * For each attribute <code>a</code> of the individuals the following is done:
 * <ol>
 * <li>The label is seen as function of the attribute <code>a</code></li>
 * <li>A fast fourier transform is performed to search the <code>k</code> biggest frequencies
 * <code>f_k</code>in this attributes space (and their phase differences <code>p_k</code>).</li>
 * <li>For each of these <code>k</code> frequencies a new attribute is generated:
 * <code>sin(f_k * a) + p_k</code>.</li>
 * </ol>
 * 
 * @author Ingo Mierswa
 */
public class FourierGenerator extends ExampleSetBasedIndividualOperator {

	/** The sinus factory. */
	private SinusFactory factory = null;

	private int startGenerations = 0;

	private int applyInGeneration = 1;

	private RandomGenerator random;

	/** Creates a new fourier generator. */
	public FourierGenerator(int maxPeaks, int adaptionType, int attributesPerPeak, double epsilon, RandomGenerator random) {
		this.factory = new SinusFactory(maxPeaks);
		factory.setAdaptionType(adaptionType);
		factory.setEpsilon(epsilon);
		factory.setAttributePerPeak(attributesPerPeak);
		this.random = random;
	}

	public void setStartGenerations(int startGenerations) {
		this.startGenerations = startGenerations;
	}

	public void setApplyInGeneration(int applyInGeneration) {
		this.applyInGeneration = applyInGeneration;
	}

	@Override
	public boolean performOperation(int generation) {
		return (generation <= startGenerations) || ((applyInGeneration != 0) && ((generation % applyInGeneration) == 0));
	}

	@Override
	public List<ExampleSetBasedIndividual> operate(ExampleSetBasedIndividual individual) throws Exception {
		AttributeWeightedExampleSet exampleSet = individual.getExampleSet();
		Attribute label = exampleSet.getAttributes().getLabel();
		List<AttributePeak> attributes = new LinkedList<AttributePeak>();

		for (Attribute current : exampleSet.getAttributes()) {
			// must be numeric and not contain sin!
			if (current.isNumerical() && (current.getConstruction().indexOf("sin") == -1)) {
				List<AttributePeak> peaks = factory.getAttributePeaks(exampleSet, label, current);
				attributes.addAll(peaks);
			}
		}

		if (attributes.size() > 0) {
			factory.generateSinusFunctions(exampleSet, attributes, random);
		}

		// return example set as result
		List<ExampleSetBasedIndividual> result = new LinkedList<ExampleSetBasedIndividual>();
		result.add(new ExampleSetBasedIndividual(exampleSet));
		return result;
	}
}
