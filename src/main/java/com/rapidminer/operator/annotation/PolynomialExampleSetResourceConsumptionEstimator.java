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
package com.rapidminer.operator.annotation;

import com.rapidminer.operator.ports.InputPort;
import com.rapidminer.operator.ports.metadata.ExampleSetMetaData;
import com.rapidminer.operator.ports.metadata.MDInteger;
import com.rapidminer.operator.tools.AttributeSubsetSelector;


/**
 * Evaluates resource consumption based on a simple polynomial function.
 * 
 * @author Simon Fischer
 * 
 */
public class PolynomialExampleSetResourceConsumptionEstimator extends ExampleSetResourceConsumptionEstimator {

	private final PolynomialFunction cpuFunction;
	private final PolynomialFunction memoryFunction;

	public PolynomialExampleSetResourceConsumptionEstimator(InputPort in, AttributeSubsetSelector selector,
			PolynomialFunction cpuFunction, PolynomialFunction memoryFunction) {
		super(in, selector);
		this.cpuFunction = cpuFunction;
		this.memoryFunction = memoryFunction;
	}

	protected int getNumberOfRelevantAttributes(ExampleSetMetaData emd) {
		return emd.getNumberOfRegularAttributes();
	}

	@Override
	public long estimateMemory(ExampleSetMetaData exampleSet) {
		final MDInteger numEx = exampleSet.getNumberOfExamples();
		if (numEx == null) {
			return -1;
		} else if (numEx.getNumber() == 0) {
			return -1;
		}
		final int numAtt = getNumberOfRelevantAttributes(exampleSet);
		return cpuFunction.evaluate(numEx.getNumber(), numAtt);
	}

	@Override
	public long estimateRuntime(ExampleSetMetaData exampleSet) {
		final MDInteger numEx = exampleSet.getNumberOfExamples();
		if (numEx == null) {
			return -1;
		} else if (numEx.getNumber() == 0) {
			return -1;
		}
		final int numAtt = exampleSet.getNumberOfRegularAttributes();
		return memoryFunction.evaluate(numEx.getNumber(), numAtt);
	}

	@Override
	public PolynomialFunction getCpuFunction() {
		return cpuFunction;
	}

	@Override
	public PolynomialFunction getMemoryFunction() {
		return memoryFunction;
	}
}
