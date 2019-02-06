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
package com.rapidminer.operator.preprocessing.filter;

import com.rapidminer.example.ExampleSet;
import com.rapidminer.example.set.SortedExampleSet;
import com.rapidminer.operator.OperatorDescription;
import com.rapidminer.operator.OperatorException;
import com.rapidminer.operator.annotation.ResourceConsumptionEstimator;
import com.rapidminer.operator.preprocessing.AbstractDataProcessing;
import com.rapidminer.parameter.ParameterType;
import com.rapidminer.tools.OperatorResourceConsumptionHandler;
import com.rapidminer.tools.RandomGenerator;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;


/**
 * This operator creates a new, shuffled ExampleSet by making creating a shuffled view using a
 * {@link SortedExampleSet}
 * 
 * @author Sebastian Land, Ingo Mierswa
 */
public class PermutationOperator extends AbstractDataProcessing {

	public PermutationOperator(OperatorDescription description) {
		super(description);
	}

	@Override
	public ExampleSet apply(ExampleSet exampleSet) throws OperatorException {

		ArrayList<Integer> indicesCollection = new ArrayList<Integer>(exampleSet.size());
		for (int i = 0; i < exampleSet.size(); i++) {
			indicesCollection.add(i);
		}

		Collections.shuffle(indicesCollection, RandomGenerator.getRandomGenerator(this));

		int[] indices = new int[exampleSet.size()];
		for (int i = 0; i < exampleSet.size(); i++) {
			indices[i] = indicesCollection.get(i);
		}

		return new SortedExampleSet(exampleSet, indices);
	}

	@Override
	public List<ParameterType> getParameterTypes() {
		List<ParameterType> types = super.getParameterTypes();

		types.addAll(RandomGenerator.getRandomGeneratorParameters(this));

		return types;
	}

	@Override
	public boolean writesIntoExistingData() {
		return false;
	}

	@Override
	public ResourceConsumptionEstimator getResourceConsumptionEstimator() {
		return OperatorResourceConsumptionHandler.getResourceConsumptionEstimator(getInputPort(), PermutationOperator.class,
				null);
	}
}
