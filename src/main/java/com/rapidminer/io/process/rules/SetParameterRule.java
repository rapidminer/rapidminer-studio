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
package com.rapidminer.io.process.rules;

import com.rapidminer.io.process.XMLImporter;
import com.rapidminer.operator.Operator;
import com.rapidminer.tools.XMLException;

import org.w3c.dom.Element;
import org.w3c.dom.Node;
import org.w3c.dom.NodeList;


/**
 * Rule to set parameters.
 * 
 * @author Simon Fischer
 * 
 */
public class SetParameterRule extends AbstractConditionedParseRule {

	private String attributeName;
	private String value;

	public SetParameterRule(String operatorTypeName, Element element) throws XMLException {
		super(operatorTypeName, element);
		assert (element.getTagName().equals("setParameter"));
		NodeList children = element.getChildNodes();
		for (int i = 0; i < children.getLength(); i++) {
			Node child = children.item(i);
			if (child instanceof Element) {
				Element childElem = (Element) child;
				if (childElem.getTagName().equals("attribute")) {
					attributeName = childElem.getTextContent();
				} else if (childElem.getTagName().equals("value")) {
					value = childElem.getTextContent();
				}
			}
		}
	}

	@Override
	protected String conditionedApply(Operator operator, String operatorTypeName, XMLImporter importer) {
		operator.getParameters().setParameter(attributeName, value);
		return "Parameter <code>" + attributeName + "</code> in <var>" + operator.getName() + "</var> (<code>"
				+ operatorTypeName + "</code>) was set to <code>" + value + "</code>.";
	}

	@Override
	public String toString() {
		return "Set " + operatorTypeName + "." + attributeName + " to " + value;
	}
}
