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

import java.util.ArrayList;
import java.util.LinkedList;
import java.util.List;
import java.util.Random;


/**
 * Crossover operator for the used bitlists of example sets. An example set is selected with a given
 * fixed propability and a mating partner is determined randomly. Crossover can be either one point,
 * uniform or shuffled. Please note that shuffled crossover first uniformly determines the number of
 * attributes which should be swapped. Therefore uniform and shuffle crossover are not equivalent. <br>
 * 
 * Only useful if all example sets have the same (number of) attributes.
 * 
 * @author Simon Fischer, Ingo Mierswa Exp $
 */
public class ExampleSetBasedSelectionCrossover implements ExampleSetBasedPopulationOperator {

	public static final String[] CROSSOVER_TYPES = { "one_point", "uniform", "shuffle" };

	public static final int ONE_POINT = 0;

	public static final int UNIFORM = 1;

	public static final int SHUFFLE = 2;

	private int type;

	private double prob;

	private Random random;

	private int minNumber;

	private int maxNumber;

	private int exactNumber;

	public ExampleSetBasedSelectionCrossover(int type, double prob, Random random, int minNumber, int maxNumber,
			int exactNumber) {
		this.prob = prob;
		this.type = type;
		this.random = random;
		this.minNumber = minNumber;
		this.maxNumber = maxNumber;
		this.exactNumber = exactNumber;
	}

	/** The default implementation returns true for every generation. */
	@Override
	public boolean performOperation(int generation) {
		return true;
	}

	public int getType() {
		return type;
	}

	public void crossover(AttributeWeightedExampleSet es1, AttributeWeightedExampleSet es2) {
		switch (type) {
			case ONE_POINT:
				int n = 1 + random.nextInt(es1.getAttributes().size() - 1);
				int counter = 0;
				for (Attribute attribute : es1.getAttributes()) {
					if (counter >= n) {
						boolean dummy = es1.isAttributeUsed(attribute);
						es1.setAttributeUsed(attribute, es2.isAttributeUsed(attribute));
						es2.setAttributeUsed(attribute, dummy);
					}
					counter++;
				}
				break;
			case UNIFORM:
				boolean[] swap = new boolean[es1.getAttributes().size()];
				for (int i = 0; i < swap.length; i++) {
					swap[i] = random.nextBoolean();
					;
				}
				swapAttributes(es1, es2, swap);
				break;
			case SHUFFLE:
				swap = new boolean[es1.getAttributes().size()];
				List<Integer> indices = new ArrayList<Integer>();
				for (int i = 0; i < swap.length; i++) {
					indices.add(i);
				}
				if (indices.size() > 0) {
					int toSwap = random.nextInt(indices.size() - 1) + 1;
					for (int i = 0; i < toSwap; i++) {
						swap[indices.remove(random.nextInt(indices.size()))] = true;
					}
				}
				swapAttributes(es1, es2, swap);
				break;
			default:
				break;
		}
	}

	private void swapAttributes(AttributeWeightedExampleSet es1, AttributeWeightedExampleSet es2, boolean[] swap) {
		int index = 0;
		for (Attribute attribute : es1.getAttributes()) {
			if (swap[index++]) {
				boolean dummy = es1.isAttributeUsed(attribute);
				es1.setAttributeUsed(attribute, es2.isAttributeUsed(attribute));
				es2.setAttributeUsed(attribute, dummy);
			}
		}
	}

	@Override
	public void operate(ExampleSetBasedPopulation population) {
		if (population.getNumberOfIndividuals() < 2) {
			return;
		}

		LinkedList<AttributeWeightedExampleSet> matingPool = new LinkedList<AttributeWeightedExampleSet>();
		for (int i = 0; i < population.getNumberOfIndividuals(); i++) {
			matingPool.add((AttributeWeightedExampleSet) population.get(i).getExampleSet().clone());
		}

		List<ExampleSetBasedIndividual> l = new LinkedList<ExampleSetBasedIndividual>();

		while (matingPool.size() > 1) {
			AttributeWeightedExampleSet p1 = matingPool.remove(random.nextInt(matingPool.size()));
			AttributeWeightedExampleSet p2 = matingPool.remove(random.nextInt(matingPool.size()));

			if (random.nextDouble() < prob) {
				crossover(p1, p2);

				int numberOfFeatures = p1.getNumberOfUsedAttributes();
				if (numberOfFeatures > 0) {
					if (exactNumber > 0) {
						if (numberOfFeatures == exactNumber) {
							l.add(new ExampleSetBasedIndividual(p1));
						}
					} else {
						if (((maxNumber < 1) || (numberOfFeatures <= maxNumber)) && (numberOfFeatures >= minNumber)) {
							l.add(new ExampleSetBasedIndividual(p1));
						}
					}
				}

				numberOfFeatures = p2.getNumberOfUsedAttributes();
				if (numberOfFeatures > 0) {
					if (exactNumber > 0) {
						if (numberOfFeatures == exactNumber) {
							l.add(new ExampleSetBasedIndividual(p2));
						}
					} else {
						if (((maxNumber < 1) || (numberOfFeatures <= maxNumber)) && (numberOfFeatures >= minNumber)) {
							l.add(new ExampleSetBasedIndividual(p2));
						}
					}
				}
			}
		}

		population.addAllIndividuals(l);
	}
}
