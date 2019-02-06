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

import com.rapidminer.gui.new_plotter.engine.jfreechart.JFreeChartPlotEngine;
import com.rapidminer.gui.new_plotter.gui.dialog.AddParallelLineDialog;
import com.rapidminer.gui.tools.ResourceAction;

import java.awt.Point;
import java.awt.event.ActionEvent;


/**
 * This action allows the user to add parallel lines (horizontal/vertical) to the current chart.
 * 
 * @author Marco Boeck
 * @deprecated since 9.2.0
 */
@Deprecated
public class AddParallelLineAction extends ResourceAction {

	/** the {@link JFreeChartPlotEngine} instance for this action */
	private JFreeChartPlotEngine engine;

	/** the {@link Point} where the last popup click happened */
	private Point latestPopupLocation;

	/**
	 * the {@link AddParallelLineDialog} instance (used by all {@link AddParallelLineAction}
	 * instances)
	 */
	private static AddParallelLineDialog dialog;

	private static final long serialVersionUID = 7788302558857099622L;

	/**
	 * Creates a new {@link ResourceAction} which opens the add parallel line configuration dialog
	 * when triggered. There a crosshair line can be configured and added to the chart.
	 * 
	 * @param engine
	 */
	public AddParallelLineAction(JFreeChartPlotEngine engine) {
		super(true, "plotter.popup_menu.add_parallel_line");
		this.engine = engine;
	}

	@Override
	public void loggedActionPerformed(ActionEvent e) {
		addParallelLine(engine, latestPopupLocation);
	}

	/**
	 * Sets the location where the popup has been triggered. Used to determine the location of the
	 * line
	 * 
	 * @param location
	 */
	public void setPopupLocation(Point location) {
		latestPopupLocation = location;
	}

	/**
	 * Opens the add parallel line configuration dialog and then adds the line the user specified.
	 * 
	 * @param engine
	 * @param latestPopupLocation
	 */
	public static synchronized void addParallelLine(final JFreeChartPlotEngine engine, Point latestPopupLocation) {
		if (dialog == null) {
			dialog = new AddParallelLineDialog();
		}

		dialog.setMousePosition(latestPopupLocation);
		dialog.setChartEngine(engine);
		dialog.showDialog();
	}

}
