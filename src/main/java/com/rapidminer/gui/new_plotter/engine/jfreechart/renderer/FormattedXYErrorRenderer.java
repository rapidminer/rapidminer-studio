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

import com.rapidminer.gui.new_plotter.configuration.SeriesFormat;
import com.rapidminer.gui.new_plotter.engine.jfreechart.RenderFormatDelegate;
import com.rapidminer.gui.new_plotter.listener.RenderFormatDelegateChangeListener;
import com.rapidminer.gui.new_plotter.utility.DataStructureUtils;

import java.awt.BasicStroke;
import java.awt.Color;
import java.awt.Paint;
import java.awt.Shape;

import org.jfree.chart.renderer.xy.XYErrorRenderer;


/**
 * @author Marius Helf
 * @deprecated since 9.2.0
 */
@Deprecated
public class FormattedXYErrorRenderer extends XYErrorRenderer implements FormattedRenderer,
		RenderFormatDelegateChangeListener {

	private static final long serialVersionUID = 1L;

	private RenderFormatDelegate formatDelegate = new RenderFormatDelegate();

	public FormattedXYErrorRenderer() {
		super();
		formatDelegate.addListener(this);
	}

	@Override
	public RenderFormatDelegate getFormatDelegate() {
		return formatDelegate;
	}

	@Override
	public Paint getItemPaint(int seriesIdx, int valueIdx) {
		Paint paintFromDelegate = getFormatDelegate().getItemPaint(seriesIdx, valueIdx);
		if (paintFromDelegate == null) {
			return super.getItemPaint(seriesIdx, valueIdx);
		} else {
			return paintFromDelegate;
		}
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

	@Override
	public void renderFormatDelegateChanged(RenderFormatDelegate source) {
		SeriesFormat seriesFormat = source.getSeriesFormat();
		if (seriesFormat != null) {
			setErrorStroke(new BasicStroke(Math.round(seriesFormat.getLineWidth() / 2.0), BasicStroke.CAP_SQUARE,
					BasicStroke.JOIN_MITER));
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
