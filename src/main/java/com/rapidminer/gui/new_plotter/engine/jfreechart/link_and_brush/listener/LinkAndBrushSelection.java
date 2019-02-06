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
package com.rapidminer.gui.new_plotter.engine.jfreechart.link_and_brush.listener;

import com.rapidminer.gui.new_plotter.data.PlotInstance;
import com.rapidminer.tools.container.Pair;

import java.util.List;

import org.jfree.data.Range;


/**
 * @author Nils Woehler
 * @deprecated since 9.2.0
 */
@Deprecated
public class LinkAndBrushSelection {

	public enum SelectionType {
		ZOOM_IN, ZOOM_OUT, RESTORE_AUTO_BOUNDS, SELECTION, RESTORE_SELECTION, COLOR_ZOOM, COLOR_SELECTION, RESTORE_COLOR
	}

	private final List<Pair<Integer, Range>> domainAxisRanges;
	private final List<Pair<Integer, Range>> valueAxisRanges;
	private final SelectionType type;
	private Double minColorValue;
	private Double maxColorValue;
	private PlotInstance plotInstance;

	/**
	 * 
	 * @param type
	 *            the zooming type
	 * @param domainAxisRanges
	 *            a list of pairs with indices for domain axis and their zoomed ranges
	 * @param rangeAxisRanges
	 *            a list of pairs with indices for range axis and their zoomed ranges
	 */
	public LinkAndBrushSelection(SelectionType type, List<Pair<Integer, Range>> domainAxisRanges,
			List<Pair<Integer, Range>> rangeAxisRanges) {
		this(type, domainAxisRanges, rangeAxisRanges, null, null, null);
	}

	/**
	 * 
	 * @param type
	 *            the zooming type
	 * @param domainAxisRanges
	 *            a list of pairs with indices for domain axis and their zoomed ranges
	 * @param rangeAxisRanges
	 *            a list of pairs with indices for range axis and their zoomed ranges
	 * @param minColorValue
	 *            the min color value
	 * @param maxColorValue
	 *            the max color value
	 */
	public LinkAndBrushSelection(SelectionType type, List<Pair<Integer, Range>> domainAxisRanges,
			List<Pair<Integer, Range>> rangeAxisRanges, Double minColorValue, Double maxColorValue, PlotInstance plotInstance) {
		if (domainAxisRanges == null || rangeAxisRanges == null) {
			throw new IllegalArgumentException("Null range axes are not allowed!");
		}
		this.type = type;
		this.domainAxisRanges = domainAxisRanges;
		this.valueAxisRanges = rangeAxisRanges;
		this.minColorValue = minColorValue;
		this.maxColorValue = maxColorValue;
		this.plotInstance = plotInstance;
	}

	/**
	 * @return the domainRanges
	 */
	public List<Pair<Integer, Range>> getDomainAxisRanges() {
		return domainAxisRanges;
	}

	/**
	 * @return the first new domain axis range. <code>null</code> if list is empty
	 */
	public Pair<Integer, Range> getDomainAxisRange() {
		if (domainAxisRanges.size() > 0) {
			return domainAxisRanges.get(0);
		}
		return null;
	}

	/**
	 * @return the type
	 */
	public SelectionType getType() {
		return type;
	}

	/**
	 * @return the valueRanges
	 */
	public List<Pair<Integer, Range>> getValueAxisRanges() {
		return valueAxisRanges;
	}

	/**
	 * @return the min color value
	 */
	public Double getMinColorValue() {
		return minColorValue;
	}

	/**
	 * @return the max color value
	 */
	public Double getMaxColorValue() {
		return maxColorValue;
	}

	public PlotInstance getPlotInstance() {
		return plotInstance;
	}

	public void setPlotInstance(PlotInstance plotInstance) {
		this.plotInstance = plotInstance;
	}

}
