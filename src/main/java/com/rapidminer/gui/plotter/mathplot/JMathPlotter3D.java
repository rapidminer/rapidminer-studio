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

import org.math.plot.Plot3DPanel;
import org.math.plot.PlotPanel;


/**
 * The abstract super class for all 3D plotters using the JMathPlot library.
 * 
 * @author Ingo Mierswa
 * @deprecated since 9.2.0
 */
@Deprecated
public abstract class JMathPlotter3D extends JMathPlotter {

	private static final long serialVersionUID = -8695197842788069313L;

	public JMathPlotter3D(PlotterConfigurationModel settings) {
		super(settings);
	}

	public JMathPlotter3D(PlotterConfigurationModel settings, DataTable dataTable) {
		super(settings, dataTable);
	}

	@Override
	public PlotPanel createPlotPanel() {
		return new Plot3DPanel();
	}

	@Override
	public int getNumberOfOptionIcons() {
		return 5;
	}
}
