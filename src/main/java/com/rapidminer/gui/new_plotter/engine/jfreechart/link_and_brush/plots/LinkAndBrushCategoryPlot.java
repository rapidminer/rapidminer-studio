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
package com.rapidminer.gui.new_plotter.engine.jfreechart.link_and_brush.plots;

import com.rapidminer.gui.new_plotter.engine.jfreechart.link_and_brush.axis.LinkAndBrushAxis;
import com.rapidminer.tools.container.Pair;

import java.awt.geom.Point2D;
import java.util.LinkedList;
import java.util.List;

import org.jfree.chart.axis.ValueAxis;
import org.jfree.chart.plot.CategoryPlot;
import org.jfree.chart.plot.PlotRenderingInfo;
import org.jfree.data.Range;


/**
 * @author Nils Woehler
 * @deprecated since 9.2.0
 */
@Deprecated
public class LinkAndBrushCategoryPlot extends CategoryPlot implements LinkAndBrushPlot {

	private static final long serialVersionUID = 1L;

	@Override
	public List<Pair<Integer, Range>> calculateDomainAxesZoom(double lowerPercent, double upperPercent, boolean zoomIn) {

		List<Pair<Integer, Range>> newRanges = new LinkedList<Pair<Integer, Range>>();

		// domain axis cant be zoomed thus returning empty list

		return newRanges;
	}

	@Override
	public List<Pair<Integer, Range>> calculateRangeAxesZoom(double lowerPercent, double upperPercent,
			PlotRenderingInfo info, Point2D source, boolean zoomIn) {

		List<Pair<Integer, Range>> newRanges = new LinkedList<Pair<Integer, Range>>();

		for (int i = 0; i < getRangeAxisCount(); i++) {
			ValueAxis rangeAxis = getRangeAxis(i);
			if (rangeAxis != null) {
				if (rangeAxis instanceof LinkAndBrushAxis) {
					Range calculateZoomRange = ((LinkAndBrushAxis) rangeAxis).calculateZoomRange(lowerPercent, upperPercent,
							zoomIn);
					newRanges.add(new Pair<Integer, Range>(i, calculateZoomRange));
				} else if (zoomIn) {
					rangeAxis.zoomRange(lowerPercent, upperPercent);
				}
			}
		}

		return newRanges;
	}

	@Override
	public List<Pair<Integer, Range>> restoreAutoDomainAxisBounds(boolean zoomOut) {

		List<Pair<Integer, Range>> newRanges = new LinkedList<Pair<Integer, Range>>();

		// domain axis cant be zoomed thus returning empty list

		return newRanges;
	}

	@Override
	public List<Pair<Integer, Range>> restoreAutoRangeAxisBounds(boolean zoomOut) {

		List<Pair<Integer, Range>> newRanges = new LinkedList<Pair<Integer, Range>>();

		for (int i = 0; i < getRangeAxisCount(); i++) {
			ValueAxis rangeAxis = getRangeAxis(i);
			if (rangeAxis != null) {
				if (rangeAxis instanceof LinkAndBrushAxis) {
					Range zoomedOutRange = ((LinkAndBrushAxis) rangeAxis).restoreAutoRange(zoomOut);
					newRanges.add(new Pair<Integer, Range>(i, zoomedOutRange));
				}
			}
		}

		return newRanges;
	}

}
