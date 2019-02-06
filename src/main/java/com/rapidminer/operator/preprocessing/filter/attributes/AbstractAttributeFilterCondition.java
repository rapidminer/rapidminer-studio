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
package com.rapidminer.operator.preprocessing.filter.attributes;

import com.rapidminer.example.Attribute;
import com.rapidminer.example.Example;
import com.rapidminer.example.set.ConditionCreationException;
import com.rapidminer.operator.UserError;
import com.rapidminer.operator.ports.InputPort;
import com.rapidminer.parameter.ParameterHandler;
import com.rapidminer.parameter.ParameterType;

import java.util.LinkedList;
import java.util.List;


/**
 * @author Sebastian Land
 * 
 */
public abstract class AbstractAttributeFilterCondition implements AttributeFilterCondition {

	/**
	 * All implementing filter conditions have to have an empty constructor.
	 */
	public AbstractAttributeFilterCondition() {};

	@Override
	public ScanResult check(Attribute attribute, Example example) {
		return ScanResult.UNCHECKED;
	}

	@Override
	public ScanResult checkAfterFullScan() {
		return ScanResult.KEEP;
	}

	@Override
	public void init(ParameterHandler operator) throws UserError, ConditionCreationException {}

	@Override
	public boolean isNeedingScan() {
		return false;
	}

	@Override
	public boolean isNeedingFullScan() {
		return false;
	}

	/**
	 * Just returns an empty list. Subclasses might add parameters
	 */
	@Override
	public List<ParameterType> getParameterTypes(ParameterHandler operator, InputPort inPort, int... valueTypes) {
		return new LinkedList<ParameterType>();
	}
}
