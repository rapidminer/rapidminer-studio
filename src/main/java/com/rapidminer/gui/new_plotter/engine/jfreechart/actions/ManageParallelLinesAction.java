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
import com.rapidminer.gui.new_plotter.gui.dialog.ManageParallelLinesDialog;
import com.rapidminer.gui.tools.ResourceAction;

import java.awt.event.ActionEvent;


/**
 * This action opens a dialog which can be used to manage existing parallel lines of the current
 * chart.
 * 
 * @author Marco Boeck
 * @deprecated since 9.2.0
 */
@Deprecated
public class ManageParallelLinesAction extends ResourceAction {

	/** the {@link JFreeChartPlotEngine} instance for this action */
	private JFreeChartPlotEngine engine;

	/**
	 * the {@link ManageParallelLinesDialog} instance (used by all {@link ManageParallelLinesAction}
	 * instances)
	 */
	private static ManageParallelLinesDialog dialog;

	private static final long serialVersionUID = 7788302558857099622L;

	/**
	 * Creates a new {@link ResourceAction} which opens the {@link ManageParallelLinesDialog} when
	 * triggered.
	 * 
	 * @param engine
	 */
	public ManageParallelLinesAction(JFreeChartPlotEngine engine) {
		super(true, "plotter.popup_menu.manage_parallel_lines");
		this.engine = engine;
	}

	@Override
	public void loggedActionPerformed(ActionEvent e) {
		manageParallelLines(engine);
	}

	/**
	 * Opens the manage parallel lines dialog.
	 * 
	 * @param engine
	 */
	public static synchronized void manageParallelLines(final JFreeChartPlotEngine engine) {
		if (dialog == null) {
			dialog = new ManageParallelLinesDialog();
		}

		dialog.setChartEngine(engine);
		dialog.showDialog();
	}

}
