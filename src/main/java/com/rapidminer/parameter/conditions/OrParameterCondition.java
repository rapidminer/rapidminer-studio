/**
 * Copyright (C) 2001-2017 by RapidMiner and the contributors
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
package com.rapidminer.parameter.conditions;

import org.w3c.dom.Element;

import com.rapidminer.parameter.ParameterHandler;
import com.rapidminer.tools.XMLException;


/**
 *
 * @author Sebastian Land
 *
 */
public class OrParameterCondition extends AbstractLogicalCondition {

	public static final String ELEMENT_CONDITIONS = "SubConditions";

	public OrParameterCondition(Element element) throws XMLException {
		super(element);
	}

	public OrParameterCondition(ParameterHandler parameterHandler, boolean becomeMandatory, ParameterCondition... conditions) {
		super(parameterHandler, becomeMandatory, conditions);
	}

	public OrParameterCondition(ParameterHandler parameterHandler, String conditionParameter, boolean becomeMandatory,
			ParameterCondition... conditions) {
		super(parameterHandler, conditionParameter, becomeMandatory, conditions);
	}

	@Override
	public boolean isConditionFullfilled() {
		for (int i = 0; i < getConditions().length; ++i) {
			if (getConditions()[i].isConditionFullfilled()) {
				return true;
			}
		}
		return false;
	}

	@Override
	String getXMLTag() {
		return ELEMENT_CONDITIONS;
	}

}
