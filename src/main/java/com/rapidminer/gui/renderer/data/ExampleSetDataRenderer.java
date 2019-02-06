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
package com.rapidminer.gui.renderer.data;

import java.awt.Component;
import java.util.LinkedList;
import java.util.List;

import com.rapidminer.datatable.DataTable;
import com.rapidminer.datatable.DataTableExampleSetAdapter;
import com.rapidminer.example.Attribute;
import com.rapidminer.example.AttributeWeights;
import com.rapidminer.example.ExampleSet;
import com.rapidminer.gui.renderer.AbstractDataTableTableRenderer;
import com.rapidminer.gui.viewer.DataViewer;
import com.rapidminer.operator.IOContainer;
import com.rapidminer.operator.MissingIOObjectException;
import com.rapidminer.operator.UserError;
import com.rapidminer.operator.ports.InputPort;
import com.rapidminer.operator.ports.metadata.ExampleSetMetaData;
import com.rapidminer.operator.ports.metadata.MetaData;
import com.rapidminer.operator.tools.AttributeSubsetSelector;
import com.rapidminer.parameter.ParameterType;
import com.rapidminer.parameter.ParameterTypeInt;
import com.rapidminer.parameter.UndefinedParameterError;


/**
 * A renderer for the data view of example sets.
 *
 * @author Ingo Mierswa
 */
public class ExampleSetDataRenderer extends AbstractDataTableTableRenderer {

	public static final String RENDERER_NAME = "Data View";

	private AttributeSubsetSelector subsetSelector = null;

	@Override
	public String getName() {
		return RENDERER_NAME;
	}

	@Override
	public Component getVisualizationComponent(Object renderable, IOContainer ioContainer) {
		ExampleSet exampleSet = (ExampleSet) renderable;
		return new DataViewer(exampleSet);
	}

	/**
	 * This method is used to create a {@link DataTable} from this example set. The default
	 * implementation returns an instance of {@link DataTableExampleSetAdapter}. The given
	 * IOContainer is used to check if there are compatible attribute weights which would used as
	 * column weights of the returned table. Subclasses might want to override this method in order
	 * to allow for other data tables.
	 */
	@Override
	public DataTable getDataTable(Object renderable, IOContainer container, boolean isRendering) {
		ExampleSet exampleSet = (ExampleSet) renderable;

		try {
			if (subsetSelector != null) {
				exampleSet = subsetSelector.getSubset(exampleSet, false);
			}
		} catch (UndefinedParameterError e1) {
		} catch (UserError e1) {
		}

		AttributeWeights weights = null;
		if (container != null) {
			try {
				weights = container.get(AttributeWeights.class);
				for (Attribute attribute : exampleSet.getAttributes()) {
					double weight = weights.getWeight(attribute.getName());
					if (Double.isNaN(weight)) { // not compatible
						weights = null;
						break;
					}
				}
			} catch (MissingIOObjectException e) {
			}
		}
		return new DataTableExampleSetAdapter(exampleSet, weights);
	}

	@Override
	public List<ParameterType> getParameterTypes(InputPort inputPort) {
		List<ParameterType> types = new LinkedList<>();

		subsetSelector = new AttributeSubsetSelector(this, inputPort);
		types.addAll(subsetSelector.getParameterTypes());

		int maxRow = Integer.MAX_VALUE;
		if (inputPort != null) {
			MetaData metaData = inputPort.getMetaData();
			if (metaData != null) {
				if (metaData instanceof ExampleSetMetaData) {
					ExampleSetMetaData emd = (ExampleSetMetaData) metaData;
					if (emd.getNumberOfExamples().isKnown()) {
						maxRow = emd.getNumberOfExamples().getNumber();
					}
				}
			}
		}
		types.add(new ParameterTypeInt(PARAMETER_MIN_ROW, "Indicates the first row number which should be rendered.", 1,
		        Integer.MAX_VALUE, 1, false));
		types.add(new ParameterTypeInt(PARAMETER_MAX_ROW, "Indicates the last row number which should be rendered.", 1,
		        Integer.MAX_VALUE, maxRow, false));

		return types;
	}
}
