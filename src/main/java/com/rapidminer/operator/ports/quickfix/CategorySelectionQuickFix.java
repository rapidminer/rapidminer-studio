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

import com.rapidminer.parameter.ParameterHandler;

import java.util.Arrays;


/**
 * This is a quickfix for setting an alternative option in a category selection parameter
 * 
 * @author Sebastian Land
 */
public class CategorySelectionQuickFix extends DictionaryQuickFix {

	private ParameterHandler handler;
	private String parameter;

	public CategorySelectionQuickFix(ParameterHandler handler, String parameter, String[] alternativeValues,
			String currentValue, String description) {
		super(parameter, Arrays.asList(alternativeValues), currentValue, description);
		this.handler = handler;
		this.parameter = parameter;
	}

	@Override
	public void insertChosenOption(String chosenOption) {
		handler.setParameter(parameter, chosenOption);
	}

}
