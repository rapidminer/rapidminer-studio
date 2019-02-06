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
package com.rapidminer.gui.renderer.weights;

import com.rapidminer.datatable.DataTable;
import com.rapidminer.example.AttributeWeights;
import com.rapidminer.gui.renderer.AbstractDataTableTableRenderer;
import com.rapidminer.operator.IOContainer;
import com.rapidminer.operator.ports.InputPort;
import com.rapidminer.parameter.ParameterType;
import com.rapidminer.parameter.ParameterTypeRegexp;
import com.rapidminer.parameter.UndefinedParameterError;

import java.util.LinkedList;
import java.util.List;
import java.util.regex.Pattern;


/**
 * A renderer for the table view of attribute weights.
 * 
 * @author Ingo Mierswa
 */
public class AttributeWeightsTableRenderer extends AbstractDataTableTableRenderer {

	public static final String PARAMETER_ATTRIBUTE_SELECTION = "include_attributes";

	@Override
	public DataTable getDataTable(Object renderable, IOContainer ioContainer, boolean isRendering) {
		AttributeWeights weights = (AttributeWeights) renderable;

		if (!isRendering) {
			// use parameters only during rendering
			AttributeWeights clonedWeights = (AttributeWeights) weights.clone();
			try {
				Pattern pattern = Pattern.compile(getParameterAsString(PARAMETER_ATTRIBUTE_SELECTION));
				for (String attributeName : weights.getAttributeNames()) {
					if (!pattern.matcher(attributeName).matches()) {
						clonedWeights.removeAttributeWeight(attributeName);
					}
				}
			} catch (UndefinedParameterError e) {
			}

			return clonedWeights.createDataTable();
		} else {
			return weights.createDataTable();
		}
	}

	@Override
	public List<ParameterType> getParameterTypes(InputPort inputPort) {
		List<ParameterType> types = new LinkedList<ParameterType>();
		types.add(new ParameterTypeRegexp(PARAMETER_ATTRIBUTE_SELECTION,
				"This regular expression is used to specify which attributes will be included in the weights table.", ".*"));

		return types;
	}
}
