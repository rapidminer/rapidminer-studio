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

import com.rapidminer.gui.new_plotter.configuration.LineFormat.LineStyle;
import com.rapidminer.gui.new_plotter.configuration.event.AxisParallelLinesConfigurationChangeEvent;
import com.rapidminer.gui.new_plotter.configuration.event.AxisParallelLinesConfigurationChangeEvent.AxisParallelLineConfigurationsChangeType;
import com.rapidminer.gui.new_plotter.event.AxisParallelLineConfigurationChangeEvent;
import com.rapidminer.gui.new_plotter.listener.AxisParallelLineConfigurationListener;

import java.lang.ref.WeakReference;
import java.util.Iterator;
import java.util.LinkedList;
import java.util.List;


/**
 * A class which defines lines which shall be drawn on an axis. It defines the value on the axis,
 * and if a label will be shown.
 *
 * @author Marius Helf
 * @deprecated since 9.2.0
 */
@Deprecated
public class AxisParallelLinesConfiguration implements AxisParallelLineConfigurationListener, Cloneable {

	private List<AxisParallelLineConfiguration> lineConfigurations = new LinkedList<AxisParallelLineConfiguration>();
	private List<WeakReference<AxisParallelLinesConfigurationListener>> listeners = new LinkedList<WeakReference<AxisParallelLinesConfigurationListener>>();

	public void addLine(double value, boolean labelVisible, LineStyle style, float width) {
		AxisParallelLineConfiguration line = new AxisParallelLineConfiguration(value, labelVisible);
		line.getFormat().setStyle(style);
		line.getFormat().setWidth(width);
		addLine(line);
	}

	/**
	 * Adds the given {@link AxisParallelLineConfiguration} line. If the exact same line already
	 * exists, the line will NOT be added again.
	 *
	 * @param line
	 */
	public void addLine(AxisParallelLineConfiguration line) {
		if (lineConfigurations.contains(line)) {
			return;
		}
		lineConfigurations.add(line);
		line.addAxisParallelLineConfigurationListener(this);
		line.getFormat().addLineFormatListener(line);
		fireLineAdded(line);
	}

	public void removeLine(AxisParallelLineConfiguration line) {
		lineConfigurations.remove(line);
		line.removeAxisParallelLineConfigurationListener(this);
		line.getFormat().removeLineFormatListener(line);
		fireLineRemoved(line);
	}

	public List<AxisParallelLineConfiguration> getLines() {
		return lineConfigurations;
	}

	private void fireLineAdded(AxisParallelLineConfiguration line) {
		fireAxisParallelLinesChanged(new AxisParallelLinesConfigurationChangeEvent(this,
				AxisParallelLineConfigurationsChangeType.LINE_ADDED, line));
	}

	private void fireLineRemoved(AxisParallelLineConfiguration line) {
		fireAxisParallelLinesChanged(new AxisParallelLinesConfigurationChangeEvent(this,
				AxisParallelLineConfigurationsChangeType.LINE_REMOVED, line));
	}

	private void fireAxisParallelLineChanged(AxisParallelLineConfigurationChangeEvent e) {
		fireAxisParallelLinesChanged(new AxisParallelLinesConfigurationChangeEvent(this, e));
	}

	private void fireAxisParallelLinesChanged(AxisParallelLinesConfigurationChangeEvent e) {
		Iterator<WeakReference<AxisParallelLinesConfigurationListener>> it = listeners.iterator();
		while (it.hasNext()) {
			AxisParallelLinesConfigurationListener l = it.next().get();
			if (l != null) {
				l.axisParallelLineConfigurationsChanged(e);
			} else {
				it.remove();
			}
		}
	}

	public void addAxisParallelLinesConfigurationListener(AxisParallelLinesConfigurationListener l) {
		for (WeakReference<AxisParallelLinesConfigurationListener> listenerRef : listeners) {
			if (l.equals(listenerRef.get())) {
				return;
			}
		}
		listeners.add(new WeakReference<AxisParallelLinesConfigurationListener>(l));
	}

	public void removeAxisParallelLinesConfigurationListener(AxisParallelLinesConfigurationListener l) {
		Iterator<WeakReference<AxisParallelLinesConfigurationListener>> it = listeners.iterator();
		while (it.hasNext()) {
			AxisParallelLinesConfigurationListener listener = it.next().get();
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
	public void axisParallelLineConfigurationChanged(AxisParallelLineConfigurationChangeEvent e) {
		fireAxisParallelLineChanged(e);
	}

	@Override
	public AxisParallelLinesConfiguration clone() {
		AxisParallelLinesConfiguration clone = new AxisParallelLinesConfiguration();
		for (AxisParallelLineConfiguration line : lineConfigurations) {
			clone.addLine(line.clone());
		}
		return clone;
	}
}
