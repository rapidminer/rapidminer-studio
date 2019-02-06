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

import java.awt.Color;
import java.awt.Paint;
import java.awt.geom.Ellipse2D;

import org.jfree.chart.renderer.xy.AbstractXYItemRenderer;
import org.jfree.chart.renderer.xy.XYLineAndShapeRenderer;
import org.jfree.data.xy.XYZDataset;


/**
 * This is the scatter plotter based on JFreeChart.
 * 
 * @author Ingo Mierswa
 * @deprecated since 9.2.0
 */
@Deprecated
public class ScatterPlotter2 extends Abstract2DChartPlotter {

	private static final long serialVersionUID = 6535057074946396896L;

	private static final int POINT_SIZE = 7;

	private static final Color DESELECTED_COLOR = new Color(Color.LIGHT_GRAY.getRed(), Color.LIGHT_GRAY.getGreen(),
			Color.LIGHT_GRAY.getBlue(), 75);

	public ScatterPlotter2(PlotterConfigurationModel settings) {
		super(settings);
	}

	public ScatterPlotter2(PlotterConfigurationModel settings, DataTable dataTable) {
		super(settings, dataTable);
	}

	@Override
	public AbstractXYItemRenderer getItemRenderer(boolean nominal, int size, final double minColor, final double maxColor) {
		if (nominal) {
			// renderer settings
			XYLineAndShapeRenderer renderer = new XYLineAndShapeRenderer(false, true) {

				private static final long serialVersionUID = 8435520335829606084L;

				// public boolean getItemVisible(int series, int item) {
				// String id = getId(series, item);
				// return (id != null && dataTable.isDeselected(id));
				// }

				@Override
				public Paint getItemPaint(int series, int item) {
					String id = getId(series, item);
					if (id != null && dataTable.isDeselected(id)) {
						return DESELECTED_COLOR;
					} else {
						return super.getItemPaint(series, item);
					}
				}

				@Override
				public Paint getItemOutlinePaint(int series, int item) {
					String id = getId(series, item);
					if (id != null && dataTable.isDeselected(id)) {
						return DESELECTED_COLOR;
					} else {
						return super.getItemOutlinePaint(series, item);
					}
				}
			};
			renderer.setBaseOutlinePaint(Color.BLACK);
			renderer.setUseOutlinePaint(true);
			renderer.setDrawOutlines(true);

			if (size > 1) {
				for (int i = 0; i < size; i++) {
					renderer.setSeriesPaint(i, getColorProvider().getPointColor(i / (double) (size - 1)));
					renderer.setSeriesShape(i, new Ellipse2D.Double(-3, -3, POINT_SIZE, POINT_SIZE));
				}
			} else {
				renderer.setSeriesPaint(0, getColorProvider().getPointColor(1.0d));
				renderer.setSeriesShape(0, new Ellipse2D.Double(-3, -3, POINT_SIZE, POINT_SIZE));
			}
			return renderer;
		} else {
			XYLineAndShapeRenderer renderer = new XYLineAndShapeRenderer(false, true) {

				private static final long serialVersionUID = 7869118044747608622L;

				private ColorProvider colorProvider = new ColorProvider();

				// public boolean getItemVisible(int series, int item) {
				// String id = getId(series, item);
				// return (id != null && dataTable.isDeselected(id));
				// }

				@Override
				public Paint getItemPaint(int series, int item) {
					String id = getId(series, item);
					if (id != null && dataTable.isDeselected(id)) {
						return DESELECTED_COLOR;
					} else {
						double colorValue = ((XYZDataset) getPlot().getDataset()).getZValue(series, item);
						double normalized = (colorValue - minColor) / (maxColor - minColor);
						return colorProvider.getPointColor(normalized);
					}
				}

				@Override
				public Paint getItemOutlinePaint(int series, int item) {
					String id = getId(series, item);
					if (id != null && dataTable.isDeselected(id)) {
						return DESELECTED_COLOR;
					} else {
						return super.getItemOutlinePaint(series, item);
					}
				}
			};

			renderer.setBaseOutlinePaint(Color.BLACK);
			renderer.setUseOutlinePaint(true);
			renderer.setDrawOutlines(true);
			renderer.setSeriesShape(0, new Ellipse2D.Double(-3, -3, POINT_SIZE, POINT_SIZE));
			return renderer;
		}
	}

	@Override
	public String getPlotterName() {
		return PlotterConfigurationModel.SCATTER_PLOT;
	}

}
