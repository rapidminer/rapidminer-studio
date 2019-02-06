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
package com.rapidminer.gui.new_plotter.engine.jfreechart.renderer;

import com.rapidminer.gui.new_plotter.engine.jfreechart.RenderFormatDelegate;
import com.rapidminer.gui.new_plotter.engine.jfreechart.dataset.ValueSourceToMultiValueCategoryDatasetAdapter;
import com.rapidminer.gui.new_plotter.utility.DataStructureUtils;

import java.awt.Color;
import java.awt.Graphics2D;
import java.awt.Paint;
import java.awt.Shape;
import java.awt.geom.Rectangle2D;
import java.util.List;

import org.jfree.chart.axis.CategoryAxis;
import org.jfree.chart.axis.ValueAxis;
import org.jfree.chart.plot.CategoryPlot;
import org.jfree.chart.plot.PlotOrientation;
import org.jfree.chart.renderer.category.CategoryItemRendererState;
import org.jfree.chart.renderer.category.ScatterRenderer;
import org.jfree.data.category.CategoryDataset;
import org.jfree.util.ShapeUtilities;


/**
 * This renderer less flat than most other {@link FormattedRenderer}s, since by default the
 * ScatterRenderer draws all items in a category with the same shape and color. This renderer
 * overwrites the drawItem().
 * 
 * Needs a {@link ValueSourceToMultiValueCategoryDatasetAdapter} as dataset.
 * 
 * @author Marius Helf
 * @deprecated since 9.2.0
 */
@Deprecated
public class FormattedScatterRenderer extends ScatterRenderer implements FormattedRenderer {

	private static final long serialVersionUID = 1L;

	private RenderFormatDelegate formatDelegate = new RenderFormatDelegate();

	public FormattedScatterRenderer() {
		super();
	}

	@Override
	public RenderFormatDelegate getFormatDelegate() {
		return formatDelegate;
	}

	@Override
	public Paint getItemPaint(int seriesIdx, int valueIdx) {
		Paint paint = getFormatDelegate().getItemPaint(seriesIdx, valueIdx);
		if (paint == null) {
			paint = super.getItemPaint(seriesIdx, valueIdx);
		}
		return paint;
	}

	@Override
	public Shape getItemShape(int seriesIdx, int valueIdx) {
		Shape shapeFromDelegate = getFormatDelegate().getItemShape(seriesIdx, valueIdx);
		if (shapeFromDelegate == null) {
			return super.getItemShape(seriesIdx, valueIdx);
		} else {
			return shapeFromDelegate;
		}
	}

	/**
	 * This function is taken directly from JFreeChart with adjustments to draw differently colored
	 * items.
	 * 
	 * When updating JFreeChart this function must probably be adapted.
	 * 
	 */
	@Override
	public void drawItem(Graphics2D g2, CategoryItemRendererState state, Rectangle2D dataArea, CategoryPlot plot,
			CategoryAxis domainAxis, ValueAxis rangeAxis, CategoryDataset dataset, int row, int column, int pass) {

		// do nothing if item is not visible
		if (!getItemVisible(row, column)) {
			return;
		}
		int visibleRow = state.getVisibleSeriesIndex(row);
		if (visibleRow < 0) {
			return;
		}
		int visibleRowCount = state.getVisibleSeriesCount();

		PlotOrientation orientation = plot.getOrientation();

		ValueSourceToMultiValueCategoryDatasetAdapter dataSet = (ValueSourceToMultiValueCategoryDatasetAdapter) dataset;
		List<Double> values = dataSet.getValues(row, column);
		if (values == null) {
			return;
		}
		int valueCount = values.size();
		for (int i = 0; i < valueCount; i++) {
			// current data point...
			double x1;
			if (getUseSeriesOffset()) {
				x1 = domainAxis.getCategorySeriesMiddle(column, dataset.getColumnCount(), visibleRow, visibleRowCount,
						getItemMargin(), dataArea, plot.getDomainAxisEdge());
			} else {
				x1 = domainAxis.getCategoryMiddle(column, getColumnCount(), dataArea, plot.getDomainAxisEdge());
			}
			Number n = values.get(i);
			int idx = dataSet.getValueIndex(row, column, i);
			double value = n.doubleValue();
			double y1 = rangeAxis.valueToJava2D(value, dataArea, plot.getRangeAxisEdge());

			Shape shape = getItemShape(row, idx);
			if (orientation == PlotOrientation.HORIZONTAL) {
				shape = ShapeUtilities.createTranslatedShape(shape, y1, x1);
			} else if (orientation == PlotOrientation.VERTICAL) {
				shape = ShapeUtilities.createTranslatedShape(shape, x1, y1);
			}
			if (getItemShapeFilled(row, column)) {
				if (getUseFillPaint()) {
					g2.setPaint(getItemFillPaint(row, column));
				} else {
					g2.setPaint(getItemPaint(row, idx));
				}
				g2.fill(shape);
			}
			if (getDrawOutlines()) {
				if (getUseOutlinePaint()) {
					g2.setPaint(getItemOutlinePaint(row, column));
				} else {
					g2.setPaint(getItemPaint(row, idx));
				}
				g2.setStroke(getItemOutlineStroke(row, column));
				g2.draw(shape);
			}
		}
	}

	@Override
	public Paint getItemOutlinePaint(int seriesIdx, int valueIdx) {
		if (getFormatDelegate().isItemSelected(seriesIdx, valueIdx)) {
			return super.getItemOutlinePaint(seriesIdx, valueIdx);
		} else {
			return DataStructureUtils.setColorAlpha(Color.LIGHT_GRAY, 20);
		}
	}
}
