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
package com.rapidminer.gui.plotter.charts;

import com.rapidminer.datatable.DataTable;
import com.rapidminer.gui.plotter.ColorProvider;
import com.rapidminer.gui.plotter.PlotterConfigurationModel;

import java.awt.Paint;

import org.jfree.chart.renderer.PaintScale;
import org.jfree.chart.renderer.xy.AbstractXYItemRenderer;
import org.jfree.chart.renderer.xy.XYBlockRenderer;
import org.jfree.ui.RectangleAnchor;


/**
 * This is the block chart plotter based on JFreeChart.
 * 
 * @author Ingo Mierswa
 * @deprecated since 9.2.0
 */
@Deprecated
public class BlockChartPlotter extends Abstract2DChartPlotter {

	public BlockChartPlotter(PlotterConfigurationModel settings, DataTable dataTable) {
		super(settings, dataTable);

	}

	public BlockChartPlotter(PlotterConfigurationModel settings) {
		super(settings);
	}

	private static final long serialVersionUID = -5231467872475202473L;

	public static class BlockPaintScale implements PaintScale {

		private ColorProvider colorProvider = new ColorProvider(true);

		private double minColor;

		private double maxColor;

		public BlockPaintScale(double minColor, double maxColor) {
			this.minColor = minColor;
			this.maxColor = maxColor;
		}

		@Override
		public Paint getPaint(double z) {
			double normalized = (z - minColor) / (maxColor - minColor);
			return colorProvider.getPointColor(normalized);

			/*
			 * float z01 = (float) z / 198 ;
			 * 
			 * float B = 1;
			 * 
			 * float S = (float) Math.max(0.1, Math.abs(2*z01 - 1) );
			 * 
			 * float hRed = 0.0f; float hBlue = 0.7f; float H = hBlue - z01*(hBlue-hRed);
			 * 
			 * // get HSB color, no transparency Color c = Color.getHSBColor( H,S,B );
			 * 
			 * // adjust transparency here: try 150 instead of 255 // will lead to same grid effect
			 * return new Color(c.getRed(),c.getGreen(),c.getBlue(), 255);
			 */
		}

		@Override
		public double getUpperBound() {
			return maxColor;
		}

		@Override
		public double getLowerBound() {
			return minColor;
		}
	}

	@Override
	public AbstractXYItemRenderer getItemRenderer(boolean nominal, int size, double minColor, double maxColor) {
		XYBlockRenderer renderer = new XYBlockRenderer();
		renderer.setPaintScale(new BlockPaintScale(minColor, maxColor));
		renderer.setBlockAnchor(RectangleAnchor.CENTER);

		// if Block dimension is increased (e.g 1.2x1.2), the grid effect gets bigger
		// so it could be that blocks are overlapping a little
		// but if Block dimension is decreased (e.g. 0.9x0.9), each rectangle seems to have
		// a less-transparent border (you have to zoom-in to notice), and that could be the cause of
		// the grid effect.
		// renderer.setBlockHeight(1.0);
		// renderer.setBlockWidth(1.0);

		return renderer;
	}

	@Override
	public String getPlotterName() {
		return PlotterConfigurationModel.BLOCK_PLOT;
	}
}
