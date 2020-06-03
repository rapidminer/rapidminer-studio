/**
 * Copyright (C) 2001-2020 by RapidMiner and the contributors
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
import java.util.ArrayList;
import java.util.List;

import com.rapidminer.adaption.belt.IOTable;
import com.rapidminer.adaption.belt.TableViewingTools;
import com.rapidminer.datatable.DataTable;
import com.rapidminer.datatable.DataTableExampleSetAdapter;
import com.rapidminer.example.Attribute;
import com.rapidminer.example.AttributeWeights;
import com.rapidminer.example.ExampleSet;
import com.rapidminer.gui.renderer.AbstractDataTableTableRenderer;
import com.rapidminer.gui.viewer.BeltTableDataViewer;
import com.rapidminer.operator.IOContainer;
import com.rapidminer.operator.MissingIOObjectException;
import com.rapidminer.operator.UserError;
import com.rapidminer.operator.ports.InputPort;
import com.rapidminer.operator.ports.metadata.ExampleSetMetaData;
import com.rapidminer.operator.ports.metadata.MetaData;
import com.rapidminer.operator.tools.AttributeSubsetSelector;
import com.rapidminer.parameter.ParameterType;
import com.rapidminer.parameter.ParameterTypeInt;


/**
 * A renderer for the data view of {@link IOTable}s.
 *
 * @author Ingo Mierswa, Gisa Meier
 * @since 9.7.0
 */
public class BeltTableDataRenderer extends AbstractDataTableTableRenderer {

	public static final String RENDERER_NAME = "Data View";

	private AttributeSubsetSelector subsetSelector = null;

	@Override
	public String getName() {
		return RENDERER_NAME;
	}

	@Override
	public Component getVisualizationComponent(Object renderable, IOContainer ioContainer) {
		IOTable table = (IOTable) renderable;
		return new BeltTableDataViewer(table);
	}


	/**
	 * Used by reporting extension. Works the same as {@link ExampleSetDataRenderer#getDataTable} by wrapping the table
	 * into an {@link ExampleSet}.
	 */
	@Override
	public DataTable getDataTable(Object renderable, IOContainer container, boolean isRendering) {
		IOTable table = (IOTable) renderable;
		// no subset selector for belt tables yet, view as example set for reporting extension
		ExampleSet exampleSet = TableViewingTools.getView(table);

		try {
			if (subsetSelector != null) {
				exampleSet = subsetSelector.getSubset(exampleSet, false);
			}
		} catch (UserError e1) {
			//ignore
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
				//ignore
			}
		}
		return new DataTableExampleSetAdapter(exampleSet, weights);
	}

	/**
	 * Used by reporting extension. Does the same as {@link ExampleSetDataRenderer#getParameterTypes}.
	 */
	@Override
	public List<ParameterType> getParameterTypes(InputPort inputPort) {
		subsetSelector = new AttributeSubsetSelector(this, inputPort);
		List<ParameterType> types = new ArrayList<>(subsetSelector.getParameterTypes());

		int maxRow = Integer.MAX_VALUE;
		if (inputPort != null) {
			MetaData metaData = inputPort.getMetaData();
			if (metaData instanceof ExampleSetMetaData) {
				ExampleSetMetaData emd = (ExampleSetMetaData) metaData;
				if (emd.getNumberOfExamples().isKnown()) {
					maxRow = emd.getNumberOfExamples().getNumber();
				}
			}
		}
		types.add(new ParameterTypeInt(PARAMETER_MIN_ROW, "Indicates the first row number which should be rendered."
				, 1,
				Integer.MAX_VALUE, 1, false));
		types.add(new ParameterTypeInt(PARAMETER_MAX_ROW, "Indicates the last row number which should be rendered.", 1,
				Integer.MAX_VALUE, maxRow, false));

		return types;
	}

}
