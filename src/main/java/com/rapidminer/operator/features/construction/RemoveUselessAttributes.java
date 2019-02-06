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
import com.rapidminer.example.Statistics;
import com.rapidminer.example.set.AttributeWeightedExampleSet;

import java.util.Iterator;
import java.util.LinkedList;
import java.util.List;


/**
 * This individual operator removes all attributes from the example set which has weight 0 or the
 * same minimum and maximum values.
 * 
 * @author Ingo Mierswa
 */
public class RemoveUselessAttributes extends ExampleSetBasedIndividualOperator {

	@Override
	public List<ExampleSetBasedIndividual> operate(ExampleSetBasedIndividual individual) throws Exception {
		AttributeWeightedExampleSet exampleSet = individual.getExampleSet();
		AttributeWeightedExampleSet clone = new AttributeWeightedExampleSet(exampleSet);
		clone.recalculateAllAttributeStatistics();

		Iterator<Attribute> i = clone.getAttributes().iterator();
		while (i.hasNext()) {
			Attribute attribute = i.next();
			double weight = clone.getWeight(attribute);
			if (weight == 0.0d) {
				i.remove();
			} else if (!attribute.isNominal()) {
				double min = clone.getStatistics(attribute, Statistics.MINIMUM);
				double max = clone.getStatistics(attribute, Statistics.MAXIMUM);
				if (min == max) {
					// remove constant attributes if they are 0 or 1
					if ((min == 0.0d) || (max == 1.0d)) {
						i.remove();
					}
				}
			}
		}

		LinkedList<ExampleSetBasedIndividual> l = new LinkedList<ExampleSetBasedIndividual>();
		if (clone.getNumberOfUsedAttributes() > 0) {
			l.add(new ExampleSetBasedIndividual(clone));
		} else {
			exampleSet.getLog().logWarning(
					"No attributes left after removing useless attributes! Using original example set.");
			l.add(new ExampleSetBasedIndividual(exampleSet));
		}
		return l;
	}
}
