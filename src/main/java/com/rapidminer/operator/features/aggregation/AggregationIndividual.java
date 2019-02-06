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
package com.rapidminer.operator.features.aggregation;

import com.rapidminer.example.Attribute;
import com.rapidminer.example.ExampleSet;
import com.rapidminer.example.set.AttributeWeightedExampleSet;
import com.rapidminer.generator.FeatureGenerator;
import com.rapidminer.generator.GenerationException;
import com.rapidminer.operator.performance.PerformanceVector;

import java.util.HashMap;
import java.util.Iterator;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;


/**
 * Individuals contain the feature information and the fitness.
 * 
 * @author Ingo Mierswa ingomierswa Exp $
 */
public class AggregationIndividual {

	private final int[] individual;

	private PerformanceVector fitness = null;

	private double crowdingDistance;

	public AggregationIndividual(int[] individual) {
		this.individual = individual;
	}

	public double getCrowdingDistance() {
		return crowdingDistance;
	}

	public void setCrowdingDistance(double crowdingDistance) {
		this.crowdingDistance = crowdingDistance;
	}

	public int[] getIndividual() {
		return individual;
	}

	public void setPerformance(PerformanceVector fitness) {
		this.fitness = fitness;
	}

	public PerformanceVector getPerformance() {
		return fitness;
	}

	public ExampleSet createExampleSet(ExampleSet originalExampleSet, Attribute[] allAttributes, FeatureGenerator generator)
			throws GenerationException {
		AttributeWeightedExampleSet es = new AttributeWeightedExampleSet(originalExampleSet, null);
		Map<Integer, List<String>> mergeMap = new HashMap<Integer, List<String>>();
		for (int i = 0; i < individual.length; i++) {
			if (individual[i] == 0) {
				es.setWeight(allAttributes[i], 1.0d);
			} else {
				es.setWeight(allAttributes[i], 0.0d);
				if (individual[i] > 0) {
					List<String> mergeList = mergeMap.get(individual[i]);
					if (mergeList != null) {
						mergeList.add(allAttributes[i].getName());
					} else {
						mergeList = new LinkedList<String>();
						mergeList.add(allAttributes[i].getName());
						mergeMap.put(individual[i], mergeList);
					}
				}
			}
		}

		Iterator<Integer> i = mergeMap.keySet().iterator();
		while (i.hasNext()) {
			List<String> mergeList = mergeMap.get(i.next());
			if (mergeList.size() == 1) {
				es.setWeight(es.getAttributes().getRegular(mergeList.get(0)), 1.0d);
			} else if (mergeList.size() > 1) {
				addNewMergedAttribute(es, mergeList, generator);
			}
		}

		ExampleSet result = es.createCleanClone();
		return result;
	}

	/**
	 * The given list must contain the original attribute names which should be merged by the global
	 * FeatureGenerator.
	 */
	private void addNewMergedAttribute(AttributeWeightedExampleSet es, List<String> mergeList, FeatureGenerator generator)
			throws GenerationException {
		Attribute mergeAttribute = null;
		Iterator<String> i = mergeList.iterator();
		while (i.hasNext()) {
			Attribute currentAttribute = es.getAttributes().getRegular(i.next());
			if (mergeAttribute == null) {
				mergeAttribute = currentAttribute;
			} else {
				generator = generator.newInstance();
				Attribute[] args = new Attribute[] { mergeAttribute, currentAttribute };
				generator.setArguments(args);
				List<FeatureGenerator> generatorList = new LinkedList<FeatureGenerator>();
				generatorList.add(generator);
				List<Attribute> newAttributes = FeatureGenerator.generateAll(es.getExampleTable(), generatorList);
				mergeAttribute = newAttributes.get(0);
			}
		}
		es.getAttributes().addRegular(mergeAttribute);
	}
}
