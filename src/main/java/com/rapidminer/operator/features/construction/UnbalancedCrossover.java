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
import com.rapidminer.operator.features.selection.SelectionCrossover;

import java.util.Iterator;
import java.util.LinkedList;
import java.util.List;
import java.util.Random;


/**
 * This <tt>PopulationOperator</tt> applies a crossover on two example sets. Crossover type can be
 * ONE_POINT, UNIFORM, or SHUFFLE. In difference to SelectionCrossover the attribute vectors can
 * have different lengths. <br>
 * This crossover type should only be used for SINGLE_VALUEs, i.e. attributes without a block number
 * (blocknumber can and should be assigned to value series attributes)!
 * 
 * @author Ingo Mierswa Exp $
 */
public class UnbalancedCrossover extends ExampleSetBasedSelectionCrossover {

	private static class AttributeWeightContainer {

		private Attribute attribute;

		private double weight;

		public AttributeWeightContainer(Attribute attribute, double weight) {
			this.attribute = attribute;
			this.weight = weight;
		}

		public Attribute getAttribute() {
			return attribute;
		}

		public double getWeight() {
			return weight;
		}

		@Override
		public String toString() {
			return attribute.getName() + "(" + weight + ")";
		}
	}

	private Random random;

	/**
	 * Creates a new generating crossover with the given type which will be applied with the given
	 * probability.
	 */
	public UnbalancedCrossover(int type, double prob, Random random) {
		super(type, prob, random, 1, Integer.MAX_VALUE, -1);
		this.random = random;
	}

	/** Applies the crossover. Works directly on the given example sets. */
	@Override
	public void crossover(AttributeWeightedExampleSet es1, AttributeWeightedExampleSet es2) {
		LinkedList<AttributeWeightContainer> dummyList1 = new LinkedList<AttributeWeightContainer>();
		LinkedList<AttributeWeightContainer> dummyList2 = new LinkedList<AttributeWeightContainer>();
		int maxSize = Math.max(es1.getAttributes().size(), es2.getAttributes().size());
		if (maxSize < 2) {
			return;
		}

		switch (getType()) {
			case SelectionCrossover.ONE_POINT:
				int splitPoint = 1 + random.nextInt(maxSize - 2);
				Iterator<Attribute> it = es1.getAttributes().iterator();
				int counter = 0;
				while (it.hasNext()) {
					Attribute attribute = it.next();
					if (counter > splitPoint) {
						double weight = es1.getWeight(attribute);
						it.remove();
						dummyList1.add(new AttributeWeightContainer(attribute, weight));
					}
					counter++;
				}

				it = es2.getAttributes().iterator();
				counter = 0;
				while (it.hasNext()) {
					Attribute attribute = it.next();
					if (counter > splitPoint) {
						double weight = es2.getWeight(attribute);
						it.remove();
						dummyList2.add(new AttributeWeightContainer(attribute, weight));
					}
					counter++;
				}
				break;
			case SelectionCrossover.UNIFORM:
				it = es1.getAttributes().iterator();
				while (it.hasNext()) {
					Attribute attribute = it.next();
					if (random.nextBoolean()) {
						double weight = es1.getWeight(attribute);
						dummyList1.add(new AttributeWeightContainer(attribute, weight));
						it.remove();
					}
				}

				it = es2.getAttributes().iterator();
				while (it.hasNext()) {
					Attribute attribute = it.next();
					if (random.nextBoolean()) {
						double weight = es2.getWeight(attribute);
						dummyList2.add(new AttributeWeightContainer(attribute, weight));
						it.remove();
					}
				}
				break;
			case SelectionCrossover.SHUFFLE:
				double prob1 = (double) (random.nextInt(es1.getAttributes().size() - 1) + 1)
						/ (double) es1.getAttributes().size();
				it = es1.getAttributes().iterator();
				while (it.hasNext()) {
					Attribute attribute = it.next();
					if (random.nextDouble() < prob1) {
						double weight = es1.getWeight(attribute);
						dummyList1.add(new AttributeWeightContainer(attribute, weight));
						it.remove();
					}
				}
				double prob2 = (double) (random.nextInt(es2.getAttributes().size() - 1) + 1)
						/ (double) es2.getAttributes().size();
				it = es2.getAttributes().iterator();
				while (it.hasNext()) {
					Attribute attribute = it.next();
					if (random.nextDouble() < prob2) {
						double weight = es2.getWeight(attribute);
						dummyList2.add(new AttributeWeightContainer(attribute, weight));
						it.remove();
					}
				}
				break;
			default:
				break;
		}

		mergeAttributes(es1, dummyList2);
		mergeAttributes(es2, dummyList1);
	}

	private void mergeAttributes(AttributeWeightedExampleSet exampleSet, List<AttributeWeightContainer> attributeWeights) {
		Iterator<AttributeWeightContainer> i = attributeWeights.iterator();
		while (i.hasNext()) {
			AttributeWeightContainer attributeWeight = i.next();
			Attribute attribute = attributeWeight.getAttribute();
			if (exampleSet.getAttributes().get(attribute.getName()) == null) {
				exampleSet.getAttributes().addRegular(attribute);
			}
			exampleSet.setWeight(attribute, attributeWeight.getWeight());
		}
	}
}
