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
package com.rapidminer.gui.new_plotter.configuration.event;

import com.rapidminer.gui.new_plotter.configuration.AxisParallelLineConfiguration;
import com.rapidminer.gui.new_plotter.configuration.AxisParallelLinesConfiguration;
import com.rapidminer.gui.new_plotter.event.AxisParallelLineConfigurationChangeEvent;


/**
 * @author Marius Helf
 * @deprecated since 9.2.0
 */
@Deprecated
public class AxisParallelLinesConfigurationChangeEvent {

	public enum AxisParallelLineConfigurationsChangeType {
		LINE_ADDED, LINE_REMOVED, LINE_CHANGED,
	}

	private AxisParallelLineConfigurationsChangeType type;
	private AxisParallelLinesConfiguration source;
	private AxisParallelLineConfiguration lineConfiguration;

	public AxisParallelLinesConfigurationChangeEvent(AxisParallelLinesConfiguration source,
			AxisParallelLineConfigurationsChangeType type, AxisParallelLineConfiguration line) {
		this.source = source;
		this.type = type;
		this.lineConfiguration = line;
	}

	public AxisParallelLinesConfigurationChangeEvent(AxisParallelLinesConfiguration source,
			AxisParallelLineConfigurationChangeEvent e) {
		this.type = AxisParallelLineConfigurationsChangeType.LINE_CHANGED;
		this.source = source;
	}

	public AxisParallelLineConfigurationsChangeType getType() {
		return type;
	}

	public AxisParallelLinesConfiguration getSource() {
		return source;
	}

	public AxisParallelLineConfiguration getLineConfiguration() {
		return lineConfiguration;
	}
}
