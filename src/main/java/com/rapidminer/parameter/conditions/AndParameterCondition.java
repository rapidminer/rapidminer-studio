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
package com.rapidminer.parameter.conditions;

import org.w3c.dom.Element;

import com.rapidminer.parameter.ParameterHandler;
import com.rapidminer.tools.XMLException;


/**
 *
 * A parameter condition which allows to combine multiple operator conditions with a logical AND
 * (&&).
 *
 * @author Nils Woehler
 * @since 6.5.0
 *
 */
public class AndParameterCondition extends AbstractLogicalCondition {

	private static final String ELEMENT_CONDITIONS = "AndSubConditions";

	public AndParameterCondition(Element element) throws XMLException {
		super(element);
	}

	public AndParameterCondition(ParameterHandler parameterHandler, boolean becomeMandatory,
			ParameterCondition... conditions) {
		super(parameterHandler, becomeMandatory, conditions);
	}

	public AndParameterCondition(ParameterHandler parameterHandler, String conditionParameter, boolean becomeMandatory,
			ParameterCondition... conditions) {
		super(parameterHandler, conditionParameter, becomeMandatory, conditions);
	}

	@Override
	public boolean isConditionFullfilled() {
		boolean fulfilled = true;
		for (int i = 0; i < getConditions().length; ++i) {
			fulfilled &= getConditions()[i].isConditionFullfilled();
		}
		return fulfilled;
	}

	@Override
	String getXMLTag() {
		return ELEMENT_CONDITIONS;
	}
}
