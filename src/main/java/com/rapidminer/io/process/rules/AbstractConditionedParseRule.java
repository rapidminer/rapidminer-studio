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
import com.rapidminer.io.process.conditions.ParameterEqualsCondition;
import com.rapidminer.io.process.conditions.ParameterUnequalsCondition;
import com.rapidminer.io.process.conditions.ParseRuleCondition;
import com.rapidminer.operator.Operator;
import com.rapidminer.tools.LogService;
import com.rapidminer.tools.XMLException;

import java.util.LinkedList;
import java.util.List;
import java.util.logging.Level;

import org.w3c.dom.Element;
import org.w3c.dom.Node;
import org.w3c.dom.NodeList;


/**
 * This is the super class of all ParseRules with depend on one or more conditions.
 * 
 * @author Sebastian Land
 * 
 */
public abstract class AbstractConditionedParseRule extends AbstractParseRule {

	private final List<ParseRuleCondition> conditions = new LinkedList<ParseRuleCondition>();

	public AbstractConditionedParseRule(String operatorTypeName, Element element) throws XMLException {
		super(operatorTypeName, element);
		NodeList children = element.getChildNodes();
		for (int i = 0; i < children.getLength(); i++) {
			Node child = children.item(i);
			if (child instanceof Element) {
				Element childElem = (Element) child;
				if (childElem.getTagName().equals("condition")) {
					parseCondition(childElem);
				}
			}
		}
	}

	protected void parseCondition(Element childElem) {
		NodeList conditionNodes = childElem.getChildNodes();
		for (int j = 0; j < conditionNodes.getLength(); j++) {
			Node conditionNode = conditionNodes.item(j);
			if (conditionNode instanceof Element) {
				Element conditionElem = (Element) conditionNode;
				if (conditionElem.getTagName().equals("parameter_equals")) {
					conditions.add(new ParameterEqualsCondition((Element) conditionNode));
				} else if (conditionElem.getTagName().equals("parameter_unequals")) {
					conditions.add(new ParameterUnequalsCondition((Element) conditionNode));
				} else {
					// LogService.getRoot().warning("Unknown condition: "+conditionElem.getTagName());
					LogService.getRoot().log(Level.WARNING,
							"com.rapidminer.io.process.rules.AbstractConditionedParseRule.unknown_condition",
							conditionElem.getTagName());
				}
			}
		}
	}

	@Override
	protected String apply(Operator operator, String operatorTypeName, XMLImporter importer) {
		for (ParseRuleCondition condition : conditions) {
			if (!condition.isSatisfied(operator)) {
				return null;
			}
		}
		return conditionedApply(operator, operatorTypeName, importer);
	}

	protected abstract String conditionedApply(Operator operator, String operatorTypeName, XMLImporter importer);
}
