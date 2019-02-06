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
package com.rapidminer.gui.new_plotter.configuration;

import com.rapidminer.gui.new_plotter.event.AxisParallelLineConfigurationChangeEvent;
import com.rapidminer.gui.new_plotter.listener.AxisParallelLineConfigurationListener;
import com.rapidminer.gui.new_plotter.listener.events.LineFormatChangeEvent;

import java.lang.ref.WeakReference;
import java.util.Iterator;
import java.util.LinkedList;
import java.util.List;


/**
 * A class which configures a line which is parallel to one of the plot axes.
 *
 * @author Marius Helf
 * @deprecated since 9.2.0
 */
@Deprecated
public class AxisParallelLineConfiguration implements LineFormatListener, Cloneable {

	LineFormat format = new LineFormat();
	private boolean labelVisible = true;
	private double value;

	private List<WeakReference<AxisParallelLineConfigurationListener>> listeners = new LinkedList<WeakReference<AxisParallelLineConfigurationListener>>();

	/**
	 * Creates a new {@link AxisParallelLineConfiguration}.
	 */
	public AxisParallelLineConfiguration(double value, boolean labelVisible) {
		this.value = value;
	}

	public double getValue() {
		return value;
	}

	public void setValue(double value) {
		if (this.value != value) {
			this.value = value;
			fireAxisParallelLineConfigurationChanged(new AxisParallelLineConfigurationChangeEvent(this, value));
		}
	}

	public boolean isLabelVisible() {
		return labelVisible;
	}

	public LineFormat getFormat() {
		return format;
	}

	public void setLabelVisible(boolean labelVisible) {
		if (labelVisible != this.labelVisible) {
			this.labelVisible = labelVisible;
			fireAxisParallelLineConfigurationChanged(new AxisParallelLineConfigurationChangeEvent(this, labelVisible));
		}
	}

	@Override
	public void lineFormatChanged(LineFormatChangeEvent e) {
		fireAxisParallelLineConfigurationChanged(new AxisParallelLineConfigurationChangeEvent(this, e));
	}

	private void fireAxisParallelLineConfigurationChanged(AxisParallelLineConfigurationChangeEvent e) {
		Iterator<WeakReference<AxisParallelLineConfigurationListener>> it = listeners.iterator();
		while (it.hasNext()) {
			AxisParallelLineConfigurationListener l = it.next().get();
			if (l != null) {
				l.axisParallelLineConfigurationChanged(e);
			} else {
				it.remove();
			}
		}
	}

	@Override
	public AxisParallelLineConfiguration clone() {
		AxisParallelLineConfiguration clone = new AxisParallelLineConfiguration(this.value, this.labelVisible);
		clone.format = format.clone();
		return clone;
	}

	@Override
	public String toString() {
		return "Line (value: " + value + ")";
	}

	public void addAxisParallelLineConfigurationListener(AxisParallelLineConfigurationListener l) {
		listeners.add(new WeakReference<AxisParallelLineConfigurationListener>(l));
	}

	public void removeAxisParallelLineConfigurationListener(AxisParallelLineConfigurationListener l) {
		Iterator<WeakReference<AxisParallelLineConfigurationListener>> it = listeners.iterator();
		while (it.hasNext()) {
			AxisParallelLineConfigurationListener listener = it.next().get();
			if (l != null) {
				if (listener != null && listener.equals(l)) {
					it.remove();
				}
			} else {
				it.remove();
			}
		}
	}

	@Override
	public boolean equals(Object obj) {
		if (!(obj instanceof AxisParallelLineConfiguration)) {
			return false;
		}

		AxisParallelLineConfiguration line = (AxisParallelLineConfiguration) obj;
		if (!checkDoubleForEquality(this.getValue(), line.getValue())) {
			return false;
		}
		if (this.isLabelVisible() != line.isLabelVisible()) {
			return false;
		}
		if (!this.getFormat().getColor().equals(line.getFormat().getColor())) {
			return false;
		}
		if (!this.getFormat().getStyle().equals(line.getFormat().getStyle())) {
			return false;
		}
		// for normal width values, this should be fine. Breaks with huge width values,
		// but who would be using widths in the 10.000s range?
		float floatEepsilon = 0.00001f;
		if (Math.abs(this.getFormat().getWidth() - line.getFormat().getWidth()) > floatEepsilon) {
			return false;
		}

		return true;
	}

	/**
	 * Returns true if equality has been found; false otherwise. Note that this function is NOT
	 * perfect, passing in values very close to zero where one is positive and the other is negative
	 * will fail the check, despite them being very close together. But in this case that should not
	 * be too much of a problem.
	 *
	 * @param expected
	 * @param actual
	 * @return
	 */
	private boolean checkDoubleForEquality(double expected, double actual) {
		if (expected == actual) {
			return true;
		}

		if (Double.isNaN(expected) && !Double.isNaN(actual)) {
			return false;
		}

		if (!Double.isNaN(expected) && Double.isNaN(actual)) {
			return false;
		}

		final double MAX_RELATIVE_ERROR = 0.000000001;
		double relativeError;
		if (Math.abs(actual) > Math.abs(expected)) {
			relativeError = Math.abs((expected - actual) / actual);
		} else {
			relativeError = Math.abs((expected - actual) / expected);
		}
		if (relativeError > MAX_RELATIVE_ERROR) {
			return false;
		} else {
			return true;
		}
	}
}
