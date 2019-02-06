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
package com.rapidminer.gui.new_plotter.engine.jfreechart.actions;

import com.rapidminer.gui.new_plotter.configuration.AxisParallelLineConfiguration;
import com.rapidminer.gui.new_plotter.configuration.RangeAxisConfig;
import com.rapidminer.gui.new_plotter.engine.jfreechart.JFreeChartPlotEngine;
import com.rapidminer.gui.tools.ResourceAction;

import java.awt.event.ActionEvent;
import java.util.LinkedList;
import java.util.List;


/**
 * This action allows the user to remove all parallel lines (horizontal/vertical) from the current
 * chart.
 * 
 * @author Marco Boeck
 * @deprecated since 9.2.0
 */
@Deprecated
public class ClearParallelLinesAction extends ResourceAction {

	/** the {@link JFreeChartPlotEngine} instance for this action */
	private JFreeChartPlotEngine engine;

	private static final long serialVersionUID = 7788302558857099622L;

	/**
	 * Creates a new {@link ClearParallelLinesAction}.
	 * 
	 * @param engine
	 */
	public ClearParallelLinesAction(JFreeChartPlotEngine engine) {
		super(true, "plotter.popup_menu.clear_parallel_lines");
		this.engine = engine;
	}

	@Override
	public void loggedActionPerformed(ActionEvent e) {
		clearParallelLines(engine);
	}

	/**
	 * Removes all parallel lines from the current chart.
	 */
	public static synchronized void clearParallelLines(final JFreeChartPlotEngine engine) {
		// remove lines from domain
		List<AxisParallelLineConfiguration> domainLines = engine.getPlotInstance().getMasterPlotConfiguration()
				.getDomainConfigManager().getCrosshairLines().getLines();
		List<AxisParallelLineConfiguration> clonedListOfDomainLines = new LinkedList<AxisParallelLineConfiguration>(
				domainLines);
		for (int i = 0; i < clonedListOfDomainLines.size(); i++) {
			AxisParallelLineConfiguration line = clonedListOfDomainLines.get(i);
			engine.getPlotInstance().getMasterPlotConfiguration().getDomainConfigManager().getCrosshairLines()
					.removeLine(line);
		}

		// remove lines from RangeAxisConfigs
		for (RangeAxisConfig config : engine.getPlotInstance().getMasterPlotConfiguration().getRangeAxisConfigs()) {
			List<AxisParallelLineConfiguration> rangeAxisLines = config.getCrossHairLines().getLines();
			List<AxisParallelLineConfiguration> clonedListOfRangeAxisLines = new LinkedList<AxisParallelLineConfiguration>(
					rangeAxisLines);
			for (int i = 0; i < clonedListOfRangeAxisLines.size(); i++) {
				AxisParallelLineConfiguration line = clonedListOfRangeAxisLines.get(i);
				config.getCrossHairLines().removeLine(line);
			}
		}
	}

}
