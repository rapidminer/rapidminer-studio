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
package com.rapidminer.operator.validation;

import com.rapidminer.example.ExampleSet;
import com.rapidminer.example.set.MappedExampleSet;
import com.rapidminer.operator.OperatorCapability;
import com.rapidminer.operator.OperatorDescription;
import com.rapidminer.operator.OperatorException;
import com.rapidminer.operator.visualization.ProcessLogOperator;

import java.util.Random;


/**
 * <p>
 * This validation operator performs several bootstrapped samplings (sampling with replacement) on
 * the input set and trains a model on these samples. The remaining samples, i.e. those which were
 * not sampled, build a test set on which the model is evaluated. This process is repeated for the
 * specified number of iterations after which the average performance is calculated.
 * </p>
 * 
 * <p>
 * The basic setup is the same as for the usual cross validation operator. The first inner operator
 * must provide a model and the second a performance vector. Please note that this operator does not
 * regard example weights, i.e. weights specified in a weight column.
 * </p>
 * 
 * <p>
 * This validation operator provides several values which can be logged by means of a
 * {@link ProcessLogOperator}. All performance estimation operators of RapidMiner provide access to
 * the average values calculated during the estimation. Since the operator cannot ensure the names
 * of the delivered criteria, the ProcessLog operator can access the values via the generic value
 * names:
 * </p>
 * <ul>
 * <li>performance: the value for the main criterion calculated by this validation operator</li>
 * <li>performance1: the value of the first criterion of the performance vector calculated</li>
 * <li>performance2: the value of the second criterion of the performance vector calculated</li>
 * <li>performance3: the value of the third criterion of the performance vector calculated</li>
 * <li>for the main criterion, also the variance and the standard deviation can be accessed where
 * applicable.</li>
 * </ul>
 * 
 * @author Ingo Mierswa
 */
public class WeightedBootstrappingValidation extends AbstractBootstrappingValidation {

	public WeightedBootstrappingValidation(OperatorDescription description) {
		super(description);
	}

	@Override
	protected int[] createMapping(ExampleSet exampleSet, int size, Random random) throws OperatorException {
		return MappedExampleSet.createWeightedBootstrappingMapping(exampleSet, size, random);
	}

	@Override
	public boolean supportsCapability(OperatorCapability capability) {
		return true;
	}
}
