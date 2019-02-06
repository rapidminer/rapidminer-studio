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
package com.rapidminer.gui.plotter.mathplot;

import com.rapidminer.datatable.DataTable;
import com.rapidminer.gui.plotter.PlotterConfigurationModel;

import org.math.plot.Plot2DPanel;
import org.math.plot.PlotPanel;


/**
 * The abstract super class for all 2D plotters using the JMathPlot library.
 * 
 * @author Ingo Mierswa
 * @deprecated since 9.2.0
 */
@Deprecated
public abstract class JMathPlotter2D extends JMathPlotter {

	/**
	 * 
	 */
	private static final long serialVersionUID = -2547520708373816637L;

	public JMathPlotter2D(PlotterConfigurationModel settings) {
		super(settings);
	}

	public JMathPlotter2D(PlotterConfigurationModel settings, DataTable dataTable) {
		super(settings, dataTable);
	}

	@Override
	protected PlotPanel createPlotPanel() {
		return new Plot2DPanel();
	}

	@Override
	public int getNumberOfOptionIcons() {
		return 4;
	}
}
