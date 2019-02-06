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
package com.rapidminer.gui.new_plotter.engine.jfreechart.link_and_brush.axis;

import java.util.Locale;
import java.util.TimeZone;

import org.jfree.chart.axis.DateAxis;
import org.jfree.data.Range;


/**
 * @author Marius Helf, Nils Woehler
 * @deprecated since 9.2.0
 */
@Deprecated
public class CustomDateAxis extends DateAxis implements LinkAndBrushAxis {

	private static final long serialVersionUID = 1;

	private double upperBoundCache = Double.NEGATIVE_INFINITY;
	private double lowerBoundCache = Double.POSITIVE_INFINITY;

	private double upperBoundWithoutMargin = Double.NEGATIVE_INFINITY;
	private double lowerBoundWithoutMargin = Double.POSITIVE_INFINITY;

	public CustomDateAxis() {
		super();
	}

	public CustomDateAxis(String label) {
		super(label);
	}

	public CustomDateAxis(String label, TimeZone zone, Locale locale) {
		super(label, zone, locale);
	}

	@Override
	public void resizeRange(double percent, double anchorValue) {
		if (percent > 0.0) {
			double halfLength = getRange().getLength() * percent / 2;
			Range adjusted = new Range(anchorValue - halfLength, anchorValue + halfLength);
			setRange(adjusted);
		} else {
			restoreAutoRange(true);
		}
	}

	@Override
	public void resizeRange2(double percent, double anchorValue) {
		if (percent > 0.0) {
			double left = anchorValue - getLowerBound();
			double right = getUpperBound() - anchorValue;
			Range adjusted = new Range(anchorValue - left * percent, anchorValue + right * percent);
			setRange(adjusted);
		} else {
			restoreAutoRange(true);
		}
	}

	@Override
	public Range calculateZoomRange(double lowerPercent, double upperPercent, boolean zoomIn) {
		double start = getRange().getLowerBound();
		double length = getRange().getLength();
		Range adjusted = null;
		if (isInverted()) {
			adjusted = new Range(start + (length * (1 - upperPercent)), start + (length * (1 - lowerPercent)));
		} else {
			adjusted = new Range(start + length * lowerPercent, start + length * upperPercent);
		}
		if (zoomIn) {
			setRange(adjusted);
		}
		return adjusted;
	}

	@Override
	public Range restoreAutoRange(boolean zoomOut) {
		if (upperBoundCache < lowerBoundCache) {
			upperBoundCache = lowerBoundCache + 1;
		}
		Range autoRange = new Range(lowerBoundCache, upperBoundCache);
		if (zoomOut) {
			setRange(autoRange);
		}
		return new Range(lowerBoundWithoutMargin, upperBoundWithoutMargin);
	}

	@Override
	public void setLowerBound(double min) {
		if (getRange().getUpperBound() > min) {
			this.lowerBoundCache = min;
			setRange(new Range(min, getRange().getUpperBound()));
		} else {
			this.lowerBoundCache = min + 1.0;
			setRange(new Range(min, min + 1.0));
		}
	}

	@Override
	public void setUpperBound(double max) {
		if (getRange().getLowerBound() < max) {
			this.upperBoundCache = max;
			setRange(new Range(getRange().getLowerBound(), max));
		} else {
			this.upperBoundCache = max - 1.0;
			setRange(max - 1.0, max);
		}
	}

	@Override
	public void saveUpperBound(double max, double maxWOMargin) {
		setUpperBound(max);
		this.upperBoundWithoutMargin = maxWOMargin;
	}

	@Override
	public void saveLowerBound(double min, double minWOMargin) {
		setLowerBound(min);
		this.lowerBoundWithoutMargin = minWOMargin;
	}

}
