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
package com.rapidminer.gui.new_plotter.event;

import com.rapidminer.gui.new_plotter.configuration.AxisParallelLineConfiguration;
import com.rapidminer.gui.new_plotter.listener.events.LineFormatChangeEvent;


/**
 * @author Marius Helf
 * @deprecated since 9.2.0
 */
@Deprecated
public class AxisParallelLineConfigurationChangeEvent {

	enum AxisParallelLineConfigurationChangeType {
		FORMAT, LABEL_VISIBLE, VALUE,
	}

	private LineFormatChangeEvent lineFormatChange;
	private AxisParallelLineConfiguration source;
	private AxisParallelLineConfigurationChangeType type;
	private boolean labelVisible;

	public AxisParallelLineConfigurationChangeEvent(AxisParallelLineConfiguration axisParallelLineConfiguration,
			LineFormatChangeEvent e) {
		this.type = AxisParallelLineConfigurationChangeType.FORMAT;
		this.source = axisParallelLineConfiguration;
		this.lineFormatChange = e;
	}

	public AxisParallelLineConfigurationChangeEvent(AxisParallelLineConfiguration axisParallelLineConfiguration,
			boolean labelVisible) {
		this.source = axisParallelLineConfiguration;
		this.type = AxisParallelLineConfigurationChangeType.LABEL_VISIBLE;
		this.labelVisible = labelVisible;
	}

	public AxisParallelLineConfigurationChangeEvent(AxisParallelLineConfiguration axisParallelLineConfiguration, double value) {
		this.type = AxisParallelLineConfigurationChangeType.VALUE;
		this.source = axisParallelLineConfiguration;
	}

	public LineFormatChangeEvent getLineFormatChange() {
		return lineFormatChange;
	}

	public AxisParallelLineConfiguration getSource() {
		return source;
	}

	public AxisParallelLineConfigurationChangeType getType() {
		return type;
	}

	public boolean isLabelVisible() {
		return labelVisible;
	}
}
