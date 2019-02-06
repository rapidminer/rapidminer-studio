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
package com.rapidminer.gui.renderer.similarity;

import com.rapidminer.example.ExampleSet;
import com.rapidminer.gui.plotter.Plotter;
import com.rapidminer.gui.plotter.PlotterAdapter;
import com.rapidminer.gui.renderer.AbstractRenderer;
import com.rapidminer.gui.viewer.SimilarityKDistanceVisualization;
import com.rapidminer.operator.IOContainer;
import com.rapidminer.operator.similarity.SimilarityMeasureObject;
import com.rapidminer.report.Reportable;

import java.awt.Component;


/**
 * A renderer for the k-distance view of a similarity measure.
 * 
 * @author Ingo Mierswa
 */
public class SimilarityKDistanceRenderer extends AbstractRenderer {

	@Override
	public String getName() {
		return "k-distances";
	}

	private PlotterAdapter createKDistancePlotter(SimilarityMeasureObject sim, ExampleSet exampleSet) {
		return new SimilarityKDistanceVisualization(sim.getDistanceMeasure(), exampleSet);
	}

	@Override
	public Reportable createReportable(Object renderable, IOContainer ioContainer, int width, int height) {
		SimilarityMeasureObject sim = (SimilarityMeasureObject) renderable;
		Plotter plotter = createKDistancePlotter(sim, sim.getExampleSet());
		plotter.getRenderComponent().setSize(width, height);
		return plotter;
	}

	@Override
	public Component getVisualizationComponent(Object renderable, IOContainer ioContainer) {
		SimilarityMeasureObject sim = (SimilarityMeasureObject) renderable;
		return createKDistancePlotter(sim, sim.getExampleSet());

	}
}
