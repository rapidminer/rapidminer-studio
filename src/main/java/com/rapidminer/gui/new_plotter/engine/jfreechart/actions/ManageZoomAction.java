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
import com.rapidminer.gui.new_plotter.gui.dialog.ManageZoomDialog;
import com.rapidminer.gui.tools.ResourceAction;

import java.awt.event.ActionEvent;


/**
 * This action allows the user to manually zoom in/do a selection on the current chart.
 * 
 * @author Marco Boeck
 * @deprecated since 9.2.0
 */
@Deprecated
public class ManageZoomAction extends ResourceAction {

	/** the {@link JFreeChartPlotEngine} instance for this action */
	private JFreeChartPlotEngine engine;

	/**
	 * the {@link AddParallelLineDialog} instance (used by all {@link AddParallelLineAction}
	 * instances)
	 */
	private static ManageZoomDialog dialog;

	private static final long serialVersionUID = 7788302558857099622L;

	/**
	 * Creates a new {@link ResourceAction} which opens the manage zoom dialog when triggered. With
	 * its help, the user can precisely define the zoom/selection area.
	 * 
	 * @param engine
	 */
	public ManageZoomAction(JFreeChartPlotEngine engine) {
		super(true, "plotter.popup_menu.manage_zoom");
		this.engine = engine;
	}

	@Override
	public void loggedActionPerformed(ActionEvent e) {
		manageZoom(engine);
	}

	/**
	 * Opens the manage zoom dialog.
	 * 
	 * @param engine
	 */
	public static synchronized void manageZoom(final JFreeChartPlotEngine engine) {
		if (dialog == null) {
			dialog = new ManageZoomDialog();
		}

		dialog.setChartEngine(engine);
		dialog.showDialog();
	}

}
