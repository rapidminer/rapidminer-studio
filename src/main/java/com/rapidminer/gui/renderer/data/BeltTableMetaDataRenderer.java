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
import java.awt.Dimension;

import com.rapidminer.adaption.belt.IOTable;
import com.rapidminer.gui.renderer.AbstractRenderer;
import com.rapidminer.gui.viewer.metadata.BeltMetaDataStatisticsViewer;
import com.rapidminer.gui.viewer.metadata.model.BeltMetaDataStatisticsModel;
import com.rapidminer.operator.IOContainer;
import com.rapidminer.report.Reportable;


/**
 * A renderer for the meta data view of example sets.
 * 
 * @author Ingo Mierswa, Marco Boeck, Gisa Meier
 * @since 9.7.0
 */
public class BeltTableMetaDataRenderer extends AbstractRenderer {


	@Override
	public String getName() {
		return "Meta Data View";
	}

	@Override
	public Component getVisualizationComponent(Object renderable, IOContainer ioContainer) {
		IOTable exampleSet = (IOTable) renderable;
		return new BeltMetaDataStatisticsViewer(new BeltMetaDataStatisticsModel(exampleSet));
	}

	@Override
	public Reportable createReportable(Object renderable, IOContainer ioContainer, int desiredWidth, int desiredHeight) {
		BeltMetaDataStatisticsViewer viewer = (BeltMetaDataStatisticsViewer) getVisualizationComponent(renderable, ioContainer);
		Dimension dimension = new Dimension(desiredWidth, desiredHeight);
		viewer.setPreferredSize(dimension);
		viewer.setSize(dimension);
		return viewer;
	}
}
