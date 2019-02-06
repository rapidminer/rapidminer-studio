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

import com.rapidminer.operator.Operator;
import com.rapidminer.parameter.ParameterHandler;

import org.w3c.dom.Document;
import org.w3c.dom.Element;


/**
 * The ParameterCondition interface can be used to define dependencies for parameter types, e.g. to
 * show certain parameters only in cases where another parameter has a specified value.
 * 
 * @author Sebastian Land, Ingo Mierswa
 */
public abstract class ParameterCondition {

	private static final String ELEMENT_CONDITION = "Condition";

	private static final String ATTRIBUTE_CONDITION_PARAMETER = "depends-on";

	private static final String ATTRIBUTE_MANDATORY = "is-becoming-mandatory";

	protected ParameterHandler parameterHandler;

	protected String conditionParameter;

	protected boolean becomeMandatory;

	/**
	 * This constructor is the inverse to {@link #getDefinitionAsXML(Element)} and must be
	 * implemented by the subclasses. It will be called by reflection!
	 * 
	 * Notice that conditions created with this constructor won't work until
	 * {@link #setParameterHandler(ParameterHandler)} or {@link #setOperator(Operator)} is called.
	 */
	public ParameterCondition(Element conditionElement) {
		this.parameterHandler = null;
		loadDefinitionFromXML(conditionElement);
	}

	/**
	 * This constructor can be used when this condition does not depend on any other parameter but
	 * outer conditions as for example the version of the operator.
	 */
	public ParameterCondition(ParameterHandler parameterHandler, boolean becomeMandatory) {
		this.parameterHandler = parameterHandler;
		this.conditionParameter = null;
		this.becomeMandatory = becomeMandatory;
	}

	public ParameterCondition(ParameterHandler parameterHandler, String conditionParameter, boolean becomeMandatory) {
		this.parameterHandler = parameterHandler;
		this.conditionParameter = conditionParameter;
		this.becomeMandatory = becomeMandatory;
	}

	/**
	 * This method sets the parameter handler from which the values to check the condition are
	 * retrieved, this is usually an operator. This can be used if during construction time no
	 * parameterhandler was known.
	 */
	public void setOperator(Operator operator) {
		setParameterHandler(operator);
	}

	/**
	 * This method sets the parameter handler from which the values to check the condition are
	 * retrieved, this is usually an operator. This can be used if during construction time no
	 * parameterhandler was known.
	 *
	 * @since 9.1
	 */
	public void setParameterHandler(ParameterHandler handler) {
		this.parameterHandler = handler;
	}

	/** @since 9.1 */
	public String getConditionParameter() {
		return conditionParameter;
	}

	/**
	 * This returns true if the condition is met and if the ancestor type isn't hidden.
	 */
	public final boolean dependencyMet() {
		// if we can't check: Return always true
		return parameterHandler == null ||
				// sanity checks
				(conditionParameter == null || !parameterHandler.getParameters().getParameterType(conditionParameter).isHidden())
						// otherwise perform check
						&& isConditionFullfilled();
	}

	/**
	 * Subclasses have to implement this method in order to return if the condition is fulfilled.
	 */
	public abstract boolean isConditionFullfilled();

	public boolean becomeMandatory() {
		return becomeMandatory;
	}

	/**
	 * This must return an XML Element that represents this condition.
	 */
	public final Element getDefinitionAsXML(Document document) {
		Element conditionElement = document.createElement(ELEMENT_CONDITION);
		conditionElement.setAttribute(ATTRIBUTE_MANDATORY, becomeMandatory + "");
		conditionElement.setAttribute(ATTRIBUTE_CONDITION_PARAMETER, conditionParameter);

		// add subclasses information
		getDefinitionAsXML(conditionElement);

		return conditionElement;
	}

	/**
	 * Subclasses must override this method and append all their properties to the given element.
	 * From this element they must be able to reload their definition.
	 */
	public void getDefinitionAsXML(Element element) {
		throw new UnsupportedOperationException("This Subclasses " + this.getClass().getCanonicalName()
				+ " of the super type " + ParameterCondition.class.getCanonicalName()
				+ " must override the method getDefinitionAsXML in order to make this work.");
	}

	private void loadDefinitionFromXML(Element element) {
		this.conditionParameter = element.getAttribute(ATTRIBUTE_CONDITION_PARAMETER);
		this.becomeMandatory = Boolean.valueOf(element.getAttribute(ATTRIBUTE_MANDATORY));
	}
}
