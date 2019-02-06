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
package com.rapidminer.operator.learner.tree;

import com.rapidminer.example.Attribute;
import com.rapidminer.example.ExampleSet;
import com.rapidminer.tools.RandomGenerator;

import java.util.Iterator;


/**
 * Selects a random subset.
 * 
 * @author Ingo Mierswa
 */
public class RandomSubsetPreprocessing implements SplitPreprocessing {

	private RandomGenerator random;

	private double subsetRatio = 0.2;
	private boolean useHeuristicRation;

	public RandomSubsetPreprocessing(boolean useHeuristicRation, double subsetRatio, RandomGenerator random) {
		this.subsetRatio = subsetRatio;
		this.random = random;
		this.useHeuristicRation = useHeuristicRation;
	}

	@Override
	public ExampleSet preprocess(ExampleSet inputSet) {
		ExampleSet exampleSet = (ExampleSet) inputSet.clone();

		double usedSubsetRatio = subsetRatio;
		if (useHeuristicRation) {
			double desiredNumber = Math.floor(Math.log(exampleSet.getAttributes().size()) / Math.log(2) + 1);
			usedSubsetRatio = desiredNumber / exampleSet.getAttributes().size();
		}
		Iterator<Attribute> i = exampleSet.getAttributes().iterator();
		while (i.hasNext()) {
			i.next();
			if (random.nextDouble() > usedSubsetRatio) {
				i.remove();
			}
		}

		// ensure that at least one attribute is left
		if (exampleSet.getAttributes().size() == 0) {
			int index = random.nextInt(inputSet.getAttributes().size());
			int counter = 0;
			for (Attribute attribute : inputSet.getAttributes()) {
				if (counter == index) {
					exampleSet.getAttributes().addRegular(attribute);
					break;
				}
				counter++;
			}
		}

		return exampleSet;
	}
}
