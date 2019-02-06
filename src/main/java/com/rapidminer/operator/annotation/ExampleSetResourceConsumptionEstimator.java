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
import com.rapidminer.operator.ports.metadata.MetaData;
import com.rapidminer.operator.tools.AttributeSubsetSelector;


/**
 * Computes resource consumption based on an example set taken from a given port.
 * 
 * @author Simon Fischer
 * 
 */
public abstract class ExampleSetResourceConsumptionEstimator implements ResourceConsumptionEstimator {

	private InputPort inputPort;

	private AttributeSubsetSelector selector;

	public ExampleSetResourceConsumptionEstimator(InputPort inputPort, AttributeSubsetSelector selector) {
		super();
		this.inputPort = inputPort;
		this.selector = selector;
	}

	public abstract long estimateMemory(ExampleSetMetaData exampleSet);

	public abstract long estimateRuntime(ExampleSetMetaData exampleSet);

	@Override
	public long estimateMemoryConsumption() {
		final ExampleSetMetaData exampleSet = getExampleSet();
		if (exampleSet == null) {
			return -1;
		} else {
			return estimateMemory(exampleSet);
		}
	}

	@Override
	public long estimateRuntime() {
		final ExampleSetMetaData exampleSet = getExampleSet();
		if (exampleSet == null) {
			return -1;
		} else {
			return estimateRuntime(exampleSet);
		}
	}

	protected ExampleSetMetaData getExampleSet() {
		final MetaData md = inputPort.getMetaData();
		if (md instanceof ExampleSetMetaData) {
			if (selector != null) {
				return selector.getMetaDataSubset((ExampleSetMetaData) md, false);
			} else {
				return (ExampleSetMetaData) md;
			}
		} else {
			return null;
		}
	}
}
