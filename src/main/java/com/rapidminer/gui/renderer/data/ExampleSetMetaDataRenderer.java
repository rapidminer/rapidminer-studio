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

import com.rapidminer.example.ExampleSet;
import com.rapidminer.gui.renderer.AbstractRenderer;
import com.rapidminer.gui.viewer.metadata.MetaDataStatisticsViewer;
import com.rapidminer.gui.viewer.metadata.model.MetaDataStatisticsModel;
import com.rapidminer.operator.IOContainer;
import com.rapidminer.operator.ports.InputPort;
import com.rapidminer.operator.tools.AttributeSubsetSelector;
import com.rapidminer.parameter.ParameterType;
import com.rapidminer.report.Reportable;

import java.awt.Component;
import java.awt.Dimension;
import java.util.LinkedList;
import java.util.List;


/**
 * A renderer for the meta data view of example sets.
 * 
 * @author Ingo Mierswa, Marco Boeck
 */
public class ExampleSetMetaDataRenderer extends AbstractRenderer {

	private AttributeSubsetSelector subsetSelector = null;

	@Override
	public String getName() {
		return "Meta Data View";
	}

	@Override
	public Component getVisualizationComponent(Object renderable, IOContainer ioContainer) {
		ExampleSet exampleSet = (ExampleSet) renderable;
		return new MetaDataStatisticsViewer(new MetaDataStatisticsModel(exampleSet));
	}

	@Override
	public List<ParameterType> getParameterTypes(InputPort inputPort) {
		List<ParameterType> types = new LinkedList<ParameterType>();

		subsetSelector = new AttributeSubsetSelector(this, inputPort);
		types.addAll(subsetSelector.getParameterTypes());

		return types;
	}

	@Override
	public Reportable createReportable(Object renderable, IOContainer ioContainer, int desiredWidth, int desiredHeight) {
		MetaDataStatisticsViewer viewer = (MetaDataStatisticsViewer) getVisualizationComponent(renderable, ioContainer);
		Dimension dimension = new Dimension(desiredWidth, desiredHeight);
		viewer.setPreferredSize(dimension);
		viewer.setSize(dimension);
		return viewer;
	}
}
