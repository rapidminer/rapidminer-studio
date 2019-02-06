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
package com.rapidminer.operator.preprocessing.sampling;

import com.rapidminer.example.ExampleSet;
import com.rapidminer.example.set.MappedExampleSet;
import com.rapidminer.operator.OperatorDescription;
import com.rapidminer.operator.OperatorException;
import com.rapidminer.operator.validation.IteratingPerformanceAverage;

import java.util.Random;


/**
 * This operator constructs a bootstrapped sample from the given example set. That means that a
 * sampling with replacement will be performed. The usual sample size is the number of original
 * examples. This operator also offers the possibility to create the inverse example set, i.e. an
 * example set containing all examples which are not part of the bootstrapped example set. This
 * inverse example set might be used for a bootstrapped validation (together with an
 * {@link IteratingPerformanceAverage} operator.
 * 
 * @author Ingo Mierswa, Martin Scholz
 */
public class Bootstrapping extends AbstractBootstrapping {

	public Bootstrapping(OperatorDescription description) {
		super(description);
	}

	@Override
	public int[] createMapping(ExampleSet exampleSet, int size, Random random) throws OperatorException {
		return MappedExampleSet.createBootstrappingMapping(exampleSet, size, random);
	}
}
