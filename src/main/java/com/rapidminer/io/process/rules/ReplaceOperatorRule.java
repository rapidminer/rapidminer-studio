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

import java.util.LinkedList;
import java.util.List;

import org.w3c.dom.Element;
import org.w3c.dom.Node;
import org.w3c.dom.NodeList;

import com.rapidminer.io.process.XMLImporter;
import com.rapidminer.operator.ExecutionUnit;
import com.rapidminer.operator.Operator;
import com.rapidminer.operator.OperatorChain;
import com.rapidminer.operator.OperatorCreationException;
import com.rapidminer.parameter.UndefinedParameterError;
import com.rapidminer.tools.OperatorService;
import com.rapidminer.tools.XMLException;


/**
 * This rule replaces the specified operator by the replaceOperator. Inner rules are applied on the
 * original operator, so that they might be used for adapting parameter settings.
 *
 * @author Sebastian Land
 */
public class ReplaceOperatorRule extends AbstractConditionedParseRule {

	private String replacementName;
	private final List<ParseRule> parseRules = new LinkedList<ParseRule>();

	public ReplaceOperatorRule(String operatorTypeName, Element element) throws XMLException {
		super(operatorTypeName, element);
		assert element.getTagName().equals("replaceOperator");
		NodeList children = element.getChildNodes();
		for (int i = 0; i < children.getLength(); i++) {
			Node child = children.item(i);
			if (child instanceof Element) {
				Element childElement = (Element) child;
				if (childElement.getTagName().equals("replacement")) {
					replacementName = childElement.getTextContent();
				} else if (childElement.getTagName().equals("condition")) {
					parseCondition(childElement);
				} else {
					parseRules.add(XMLImporter.constructRuleFromElement(operatorTypeName, childElement));
				}
			}
		}
	}

	@Override
	protected String conditionedApply(Operator operator, String operatorTypeName, XMLImporter importer) {
		// determining containing subprocess
		OperatorChain parent = operator.getParent();
		int subprocess = 0;
		int operatorIndex = -1;
		for (subprocess = 0; subprocess < parent.getNumberOfSubprocesses(); subprocess++) {
			int i = 0;
			for (Operator currentOperator : parent.getSubprocess(subprocess).getOperators()) {
				if (currentOperator == operator) {
					operatorIndex = i;
					break;
				}
				i++;
			}
			if (operatorIndex > -1) {
				break;
			}
		}

		try {
			// applying subsequent changes parameter etc.
			StringBuilder builder = new StringBuilder("Replaced operator <code>" + operatorTypeName + "</code> by <code>"
					+ replacementName + "</code>.");

			if (!parseRules.isEmpty()) {
				builder.append(" In <code>" + replacementName + "</code>, the following modifications were applied:<ul>");
				for (ParseRule rule : parseRules) {
					String result = rule.apply(operator, null, importer);
					if (result != null) {
						builder.append("<li>" + result + "</li>");
					}
				}
				builder.append("</ul>");
			}

			// replacing operator
			Operator replacement = OperatorService.createOperator(replacementName);
			for (String key : operator.getParameters().getDefinedKeys()) {
				try {
					replacement.setParameter(key, operator.getParameters().getParameter(key));
				} catch (UndefinedParameterError e) {
				}
			}
			operator.remove();
			replacement.rename(operator.getName());
			parent.getSubprocess(subprocess).addOperator(replacement, operatorIndex);

			if (operator instanceof OperatorChain) {
				OperatorChain oldChain = (OperatorChain) operator;
				OperatorChain newChain = (OperatorChain) replacement;
				for (int i = 0; i < oldChain.getNumberOfSubprocesses(); i++) {
					ExecutionUnit oldUnit = oldChain.getSubprocess(i);
					ExecutionUnit newUnit = newChain.getSubprocess(i);
					newUnit.stealOperatorsFrom(oldUnit);
				}
			}
			return (builder.toString());
		} catch (OperatorCreationException e) {
			return ("Failed to create replacement operator " + replacementName + " for deprecated " + operatorTypeName + ".");
		}
	}

}
