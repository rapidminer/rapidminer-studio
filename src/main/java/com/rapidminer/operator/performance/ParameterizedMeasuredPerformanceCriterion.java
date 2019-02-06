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
package com.rapidminer.operator.performance;

import com.rapidminer.operator.OperatorCapability;
import com.rapidminer.parameter.ParameterHandler;
import com.rapidminer.parameter.ParameterType;

import java.util.List;


/**
 * This is the abstract superclass of all {@link PerformanceCriterion}s that are controlled by
 * parameters. This is needed to break the infinite loop during construction time of an operator.
 * 
 * @author Sebastian Land
 */
public abstract class ParameterizedMeasuredPerformanceCriterion extends MeasuredPerformance {

	private static final long serialVersionUID = -4753769242889308333L;

	private ParameterHandler handler;

	public ParameterizedMeasuredPerformanceCriterion(ParameterHandler handler) {
		this.handler = handler;
	}

	public ParameterizedMeasuredPerformanceCriterion(ParameterizedMeasuredPerformanceCriterion criterion) {
		super(criterion);
		this.handler = criterion.handler;
	}

	/**
	 * This method returns the {@link ParameterHandler} that returns the values for the defined
	 * {@link ParameterType}s.
	 */
	protected ParameterHandler getParameterHandler() {
		return handler;
	}

	/**
	 * This method returns all parameters of this performance criterion.
	 */
	public abstract List<ParameterType> getParameterTypes();

	/**
	 * This method has to return whether this criterion can handle the given capability.
	 */
	public abstract boolean supportsCapability(OperatorCapability capability);
}
