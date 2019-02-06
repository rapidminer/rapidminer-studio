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
package com.rapidminer.io.process.conditions;

import com.rapidminer.operator.Operator;

import org.w3c.dom.Element;
import org.w3c.dom.Node;
import org.w3c.dom.NodeList;


/**
 * 
 * @author Sebastian Land
 */
public class ParameterEqualsCondition implements ParseRuleCondition {

	protected String parameterKey;
	protected String parameterValue;

	public ParameterEqualsCondition(Element element) {
		NodeList children = element.getChildNodes();
		for (int i = 0; i < children.getLength(); i++) {
			Node child = children.item(i);
			if (child instanceof Element) {
				Element childElem = (Element) child;
				if (childElem.getTagName().equals("parameter")) {
					parameterKey = childElem.getTextContent();
				} else if (childElem.getTagName().equals("value")) {
					parameterValue = childElem.getTextContent();
				}
			}
		}
	}

	@Override
	public boolean isSatisfied(Operator operator) {
		if (parameterValue.equals("")) {
			return (!operator.getParameters().isSpecified(parameterKey))
					|| operator.getParameters().getParameterAsSpecified(parameterKey).equals(parameterValue);
		}

		return operator.getParameters().isSpecified(parameterKey)
				&& operator.getParameters().getParameterAsSpecified(parameterKey).equals(parameterValue);
	}

	@Override
	public String toString() {
		return parameterKey + "='" + parameterValue + "'";
	}
}
