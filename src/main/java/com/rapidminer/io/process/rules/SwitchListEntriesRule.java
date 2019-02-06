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
import com.rapidminer.parameter.ParameterTypeList;
import com.rapidminer.tools.XMLException;

import java.util.List;

import org.w3c.dom.Element;
import org.w3c.dom.Node;
import org.w3c.dom.NodeList;


/**
 * A rule, which switches the sides of entries in a list parameter.
 * 
 * @author Sebastian Land
 */
public class SwitchListEntriesRule extends AbstractConditionedParseRule {

	private String parameter;

	public SwitchListEntriesRule(String operatorTypeName, Element element) throws XMLException {
		super(operatorTypeName, element);
		assert (element.getTagName().equals("switchListEntries"));
		NodeList children = element.getChildNodes();
		for (int i = 0; i < children.getLength(); i++) {
			Node child = children.item(i);
			if (child instanceof Element) {
				Element childElem = (Element) child;
				if (childElem.getTagName().equals("parameter")) {
					parameter = childElem.getTextContent();
				}
			}
		}
	}

	@Override
	protected String conditionedApply(Operator operator, String operatorTypeName, XMLImporter importer) {
		if (operator.getParameters().isSpecified(parameter)) {
			String value = operator.getParameters().getParameterOrNull(parameter);
			if (value != null) {
				List<String[]> list = ParameterTypeList.transformString2List(value);
				for (String[] pair : list) {
					String first = pair[0];
					pair[0] = pair[1];
					pair[1] = first;
				}
				operator.getParameters().setParameter(parameter, ParameterTypeList.transformList2String(list));
				return "Switched sides of the entries of <code>" + parameter + "</code> in <var>" + operator.getName()
						+ "</var>.";
			}
		}
		return null;
	}

	@Override
	public String toString() {
		return "Switch entries of List " + operatorTypeName + "." + parameter;
	}

}
