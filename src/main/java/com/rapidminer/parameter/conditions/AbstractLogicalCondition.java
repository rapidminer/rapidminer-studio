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

import java.lang.reflect.Constructor;
import java.lang.reflect.InvocationTargetException;
import java.util.Collection;

import org.w3c.dom.Element;

import com.rapidminer.io.process.XMLTools;
import com.rapidminer.parameter.ParameterHandler;
import com.rapidminer.tools.XMLException;


/**
 * Super class for all logical {@link ParameterCondition} like {@link AndParameterCondition} and
 * {@link OrParameterCondition}.
 *
 * @author Nils Woehler
 * @since 6.5.0
 *
 */
public abstract class AbstractLogicalCondition extends ParameterCondition {

	private static final String ELEMENT_CONDITION = "Condition";
	private static final String ATTRIBUTE_CONDITION_CLASS = "condition-class";

	private ParameterCondition[] conditions;

	public AbstractLogicalCondition(Element element) throws XMLException {
		super(element);
		// get all condition xml-elements
		Element conditionsElement = XMLTools.getChildElement(element, getXMLTag(), true);
		Collection<Element> conditionElements = XMLTools.getChildElements(conditionsElement, ELEMENT_CONDITION);
		conditions = new ParameterCondition[conditionElements.size()];

		// iterate over condition xml-elements
		int idx = 0;
		for (Element conditionElement : conditionElements) {
			// try to construct a condition object
			String className = conditionElement.getAttribute(ATTRIBUTE_CONDITION_CLASS);
			Class<?> conditionClass;
			try {
				conditionClass = Class.forName(className);
				Constructor<?> constructor = conditionClass.getConstructor(Element.class);
				conditions[idx] = (ParameterCondition) constructor.newInstance(conditionElement);
			} catch (ClassNotFoundException | NoSuchMethodException | SecurityException | InstantiationException
					| IllegalAccessException | IllegalArgumentException | InvocationTargetException e) {
				throw new XMLException("Illegal value for attribute " + ATTRIBUTE_CONDITION_CLASS, e);
			}
			++idx;
		}
	}

	public AbstractLogicalCondition(ParameterHandler parameterHandler, boolean becomeMandatory,
			ParameterCondition... conditions) {
		super(parameterHandler, becomeMandatory);
		this.conditions = conditions;
	}

	public AbstractLogicalCondition(ParameterHandler parameterHandler, String conditionParameter, boolean becomeMandatory,
			ParameterCondition... conditions) {
		super(parameterHandler, conditionParameter, becomeMandatory);
		this.conditions = conditions;
	}

	@Override
	public void getDefinitionAsXML(Element element) {
		Element conditionsElement = XMLTools.addTag(element, getXMLTag());
		for (int i = 0; i < conditions.length; ++i) {
			Element conditionElement = XMLTools.addTag(conditionsElement, ELEMENT_CONDITION);
			ParameterCondition condition = conditions[i];
			condition.getDefinitionAsXML(conditionElement);
			conditionElement.setAttribute(ATTRIBUTE_CONDITION_CLASS, condition.getClass().getName());
		}
	}

	/**
	 * @return the conditions for this logical parameter condition
	 */
	protected ParameterCondition[] getConditions() {
		return conditions;
	}

	/**
	 * @return the XML tag for this condition
	 */
	abstract String getXMLTag();

}
