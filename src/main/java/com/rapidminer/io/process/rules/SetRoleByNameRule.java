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
import com.rapidminer.operator.ExecutionUnit;
import com.rapidminer.operator.Operator;
import com.rapidminer.operator.OperatorCreationException;
import com.rapidminer.operator.preprocessing.filter.ChangeAttributeRole;
import com.rapidminer.parameter.UndefinedParameterError;
import com.rapidminer.tools.OperatorService;
import com.rapidminer.tools.XMLException;

import org.w3c.dom.Element;
import org.w3c.dom.Node;
import org.w3c.dom.NodeList;


/**
 * This rule will insert a SetRole operator. The target role is defined by tag, while the name of
 * the attribute is retrieved from a parameter, which again is defined by a tag.
 * 
 * @author Simon Fischer
 * 
 */
public class SetRoleByNameRule extends AbstractConditionedParseRule {

	private String parameterName;
	private String targetRole;

	public SetRoleByNameRule(String operatorTypeName, Element element) throws XMLException {
		super(operatorTypeName, element);
		NodeList children = element.getChildNodes();
		for (int i = 0; i < children.getLength(); i++) {
			Node child = children.item(i);
			if (child instanceof Element) {
				Element childElem = (Element) child;
				if (childElem.getTagName().equals("role")) {
					targetRole = childElem.getTextContent();
				} else if (childElem.getTagName().equals("parameter")) {
					parameterName = childElem.getTextContent();
				}
			}
		}
	}

	@Override
	protected String conditionedApply(Operator operator, String operatorTypeName, XMLImporter importer) {
		if (operator.getParameters().isSpecified(parameterName)) {
			try {
				String attributeName = operator.getParameterAsString(parameterName);

				ChangeAttributeRole setRoleOp = OperatorService.createOperator(ChangeAttributeRole.class);

				// inserting operator into process
				ExecutionUnit process = operator.getExecutionUnit();
				int operatorIndex = process.getOperators().indexOf(operator);
				process.addOperator(setRoleOp, operatorIndex + 1);

				// setting parameter
				setRoleOp.setParameter(ChangeAttributeRole.PARAMETER_NAME, attributeName);
				setRoleOp.setParameter(ChangeAttributeRole.PARAMETER_TARGET_ROLE, targetRole);
				return "Inserted operator for explicitly setting attribute <code>" + attributeName
						+ "</code> to role <code>" + targetRole + "</code>";
			} catch (UndefinedParameterError e) {
				return null;
			} catch (OperatorCreationException e) {
				return null;
			}
		}
		return null;
	}

	@Override
	public String toString() {
		return "Set role of attribute specified in the parameter " + parameterName + " to " + targetRole + ".";
	}
}
