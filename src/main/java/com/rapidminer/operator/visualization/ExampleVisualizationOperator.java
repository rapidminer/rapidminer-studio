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
package com.rapidminer.operator.visualization;

import com.rapidminer.example.Attribute;
import com.rapidminer.example.ExampleSet;
import com.rapidminer.gui.ExampleVisualizer;
import com.rapidminer.operator.AbstractExampleSetProcessing;
import com.rapidminer.operator.OperatorDescription;
import com.rapidminer.operator.OperatorException;
import com.rapidminer.operator.UserError;
import com.rapidminer.tools.ObjectVisualizerService;


/**
 * Remembers the given example set and uses the ids provided by this set for the query for the
 * corresponding example and the creation of a generic example visualizer. This visualizer simply
 * displays the attribute values of the example. Adding this operator is often necessary to enable
 * the visualization of single examples in the provided plotter components.
 * 
 * @author Ingo Mierswa
 * @deprecated since 9.4.0
 */
@Deprecated
public class ExampleVisualizationOperator extends AbstractExampleSetProcessing {

	public ExampleVisualizationOperator(OperatorDescription description) {
		super(description);
	}

	@Override
	public ExampleSet apply(ExampleSet exampleSet) throws OperatorException {
		Attribute idAttribute = exampleSet.getAttributes().getId();
		if (idAttribute == null) {
			throw new UserError(this, 113, "Id");
		}
		ObjectVisualizerService.addObjectVisualizer(exampleSet, new ExampleVisualizer(exampleSet));

		return exampleSet;
	}

	@Override
	public boolean writesIntoExistingData() {
		return false;
	}
}
