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
package com.rapidminer.gui.renderer.performance;

import java.awt.BorderLayout;
import java.awt.Component;
import java.util.Iterator;
import java.util.List;
import java.util.Map;

import javax.swing.BorderFactory;
import javax.swing.JPanel;

import com.rapidminer.gui.look.Colors;
import com.rapidminer.gui.renderer.AbstractRenderer;
import com.rapidminer.gui.viewer.ROCChartPlotter;
import com.rapidminer.operator.IOContainer;
import com.rapidminer.operator.visualization.ROCComparison;
import com.rapidminer.report.Reportable;
import com.rapidminer.tools.math.ROCData;


/**
 *
 * @author Sebastian Land
 */
public class ROCComparisonRenderer extends AbstractRenderer {

	@Override
	public Reportable createReportable(Object renderable, IOContainer ioContainer, int desiredWidth, int desiredHeight) {
		ROCComparison model = (ROCComparison) renderable;
		ROCChartPlotter plotter = new ROCChartPlotter();
		Iterator<Map.Entry<String, List<ROCData>>> e = model.getRocData().entrySet().iterator();
		while (e.hasNext()) {
			Map.Entry<String, List<ROCData>> entry = e.next();
			plotter.addROCData(entry.getKey(), entry.getValue());
		}
		return plotter;
	}

	@Override
	public String getName() {
		return "ROC Comparison";
	}

	@Override
	public Component getVisualizationComponent(Object renderable, IOContainer ioContainer) {
		ROCComparison model = (ROCComparison) renderable;
		ROCChartPlotter plotter = new ROCChartPlotter();
		Iterator<Map.Entry<String, List<ROCData>>> e = model.getRocData().entrySet().iterator();
		while (e.hasNext()) {
			Map.Entry<String, List<ROCData>> entry = e.next();
			plotter.addROCData(entry.getKey(), entry.getValue());
		}

		JPanel panel = new JPanel(new BorderLayout());
		JPanel innerPanel = new JPanel(new BorderLayout());
		innerPanel.add(plotter);
		innerPanel.setBorder(BorderFactory.createMatteBorder(10, 10, 5, 5, Colors.WHITE));
		panel.add(innerPanel, BorderLayout.CENTER);
		return panel;
	}
}
