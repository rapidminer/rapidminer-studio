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
package com.rapidminer.operator.ports.metadata;

import com.rapidminer.operator.ports.InputPort;
import com.rapidminer.parameter.ParameterHandler;
import com.rapidminer.parameter.UndefinedParameterError;


/**
 * This precondition wraps around another precondition and performs the check only if a parameter
 * combination is fulfilled. If not, no checks will be performed. This might be used for parameter
 * dependency aware checking.
 * 
 * @author Sebastian Land
 * 
 */
public class ParameterConditionedPrecondition extends AbstractPrecondition {

	private final Precondition condition;
	private final String parameterKey;
	private final String parameterValue;
	private final ParameterHandler handler;

	public ParameterConditionedPrecondition(InputPort inputPort, Precondition condition, ParameterHandler handler,
			String parameterKey, String parameterValue) {
		super(inputPort);
		this.condition = condition;
		this.parameterKey = parameterKey;
		this.parameterValue = parameterValue;
		this.handler = handler;
	}

	@Override
	public void assumeSatisfied() {
		condition.assumeSatisfied();
	}

	@Override
	public void check(MetaData metaData) {
		try {
			if (handler.getParameterAsString(parameterKey).equals(parameterValue)) {
				condition.check(metaData);
			}
		} catch (UndefinedParameterError e) {
			// condition not applicable
		}
	}

	@Override
	public String getDescription() {
		return condition.getDescription();
	}

	@Override
	public boolean isCompatible(MetaData input, CompatibilityLevel level) {
		return condition.isCompatible(input, level);
	}

	@Override
	public MetaData getExpectedMetaData() {
		return condition.getExpectedMetaData();
	}

}
