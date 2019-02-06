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

import com.rapidminer.io.process.XMLTools;
import com.rapidminer.parameter.ParameterHandler;
import com.rapidminer.parameter.UndefinedParameterError;
import com.rapidminer.tools.XMLException;

import org.w3c.dom.Element;


/**
 * This condition checks if a type parameter (category) has a certain value.
 * 
 * @author Sebastian Land, Ingo Mierswa
 */
public class EqualTypeCondition extends ParameterCondition {

	private static final String ELEMENT_POSSIBLE_OPTIONS = "PossibleOptions";
	private static final String ELEMENT_POSSIBLE_OPTION = "PossibleOption";
	private static final String ELEMENT_FULFILLING_OPTION = "FulfillingOption";
	private static final String ELEMENT_FULFILLING_OPTIONS = "FulfillingOptions";

	private int[] fulfillingOptions;
	private String[] possibleOptions;

	public EqualTypeCondition(Element element) throws XMLException {
		super(element);

		// possible options
		Element possibleOptionsElement = XMLTools.getChildElement(element, ELEMENT_POSSIBLE_OPTIONS, true);
		possibleOptions = XMLTools.getChildTagsContentAsStringArray(possibleOptionsElement, ELEMENT_POSSIBLE_OPTION);

		Element fulfillingOptionsElement = XMLTools.getChildElement(element, ELEMENT_FULFILLING_OPTIONS, true);
		fulfillingOptions = XMLTools.getChildTagsContentAsIntArray(fulfillingOptionsElement, ELEMENT_FULFILLING_OPTION);
	}

	public EqualTypeCondition(ParameterHandler handler, String conditionParameter, String[] options,
			boolean becomeMandatory, int... types) {
		super(handler, conditionParameter, becomeMandatory);
		this.fulfillingOptions = types;
		this.possibleOptions = options;
	}

	@Override
	public boolean isConditionFullfilled() {
		boolean equals = false;
		int isType;
		try {
			isType = parameterHandler.getParameterAsInt(conditionParameter);
		} catch (UndefinedParameterError e) {
			return false;
		}
		for (int type : fulfillingOptions) {
			equals |= isType == type;
		}
		return equals;
	}

	@Override
	public String toString() {
		StringBuilder builder = new StringBuilder();
		if (fulfillingOptions.length > 1) {
			builder.append(conditionParameter.replace('_', ' ') + " \u2208 {");
			for (int i = 0; i < fulfillingOptions.length; i++) {
				builder.append(possibleOptions[fulfillingOptions[i]]);
				if (i + 1 < fulfillingOptions.length) {
					builder.append(", ");
				}
			}
			builder.append("}");
		} else if (fulfillingOptions.length > 0) {
			builder.append(conditionParameter.replace('_', ' ') + " = " + possibleOptions[fulfillingOptions[0]]);
		}
		return builder.toString();
	}

	@Override
	public void getDefinitionAsXML(Element element) {
		Element possibleOptionsElement = XMLTools.addTag(element, ELEMENT_POSSIBLE_OPTIONS);
		for (String value : possibleOptions) {
			XMLTools.addTag(possibleOptionsElement, ELEMENT_POSSIBLE_OPTION, value);
		}

		Element fulfillingOptionsElement = XMLTools.addTag(element, ELEMENT_FULFILLING_OPTIONS);
		for (int value : fulfillingOptions) {
			XMLTools.addTag(fulfillingOptionsElement, ELEMENT_FULFILLING_OPTION, value + "");
		}

	}
}
