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

import java.util.LinkedHashMap;

import com.rapidminer.datatable.DataTable;
import com.rapidminer.datatable.DataTableExampleSetAdapter;
import com.rapidminer.example.Attribute;
import com.rapidminer.example.AttributeWeights;
import com.rapidminer.example.ExampleSet;
import com.rapidminer.gui.plotter.Plotter;
import com.rapidminer.gui.plotter.PlotterConfigurationModel;
import com.rapidminer.gui.renderer.AbstractDataTablePlotterRenderer;
import com.rapidminer.operator.IOContainer;
import com.rapidminer.operator.MissingIOObjectException;


/**
 * A renderer for the plot view of example sets.
 *
 * @author Ingo Mierswa
 * @deprecated since 9.2.0
 */
@Deprecated
public class ExampleSetPlotRenderer extends AbstractDataTablePlotterRenderer {

	@Override
	public LinkedHashMap<String, Class<? extends Plotter>> getPlotterSelection() {
		return PlotterConfigurationModel.DATA_SET_PLOTTER_SELECTION;
	}

	/**
	 * This method is used to create a {@link DataTable} from this example set. The default
	 * implementation returns an instance of {@link DataTableExampleSetAdapter}. The given
	 * IOContainer is used to check if there are compatible attribute weights which would used as
	 * column weights of the returned table. Subclasses might want to override this method in order
	 * to allow for other data tables.
	 */
	@Override
	public DataTable getDataTable(Object renderable, IOContainer container) {
		ExampleSet exampleSet = (ExampleSet) renderable;
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
		return new DataTableExampleSetAdapter(exampleSet, weights, false);
	}
}
