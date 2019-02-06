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
package com.rapidminer.operator.ports.quickfix;

import com.rapidminer.operator.ports.metadata.ExampleSetMetaData;
import com.rapidminer.parameter.ParameterHandler;
import com.rapidminer.tools.Ontology;


/**
 * @author Simon Fischer
 */
public class AttributeSelectionQuickFix extends DictionaryQuickFix {

	private final ParameterHandler handler;
	private final String parameterName;

	public AttributeSelectionQuickFix(ExampleSetMetaData metaData, String parameterName, ParameterHandler handler,
			String currentValue) {
		this(metaData, parameterName, handler, currentValue, Ontology.VALUE_TYPE);
	}

	public AttributeSelectionQuickFix(ExampleSetMetaData metaData, String parameterName, ParameterHandler handler,
			String currentValue, int mustBeOfType) {
		super(parameterName, metaData.getAttributeNamesByType(mustBeOfType), currentValue, handler.getParameters()
				.getParameterType(parameterName).getDescription());
		this.handler = handler;
		this.parameterName = parameterName;
	}

	@Override
	public void insertChosenOption(String chosenOption) {
		handler.setParameter(parameterName, chosenOption);
	}
}
