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
package com.rapidminer.gui.new_plotter;

import com.rapidminer.gui.new_plotter.listener.events.ConfigurationChangeEvent;

import java.util.LinkedList;
import java.util.List;


/**
 * @author Marius Helf
 * @deprecated since 9.2.0
 */
@Deprecated
public class PlotConfigurationQuickFix {

	private List<ConfigurationChangeEvent> changeList = new LinkedList<ConfigurationChangeEvent>();

	public PlotConfigurationQuickFix() {}

	public PlotConfigurationQuickFix(ConfigurationChangeEvent change) {
		addChange(change);
	}

	public List<ConfigurationChangeEvent> getChanges() {
		return changeList;
	}

	public void addChange(ConfigurationChangeEvent change) {
		changeList.add(change);
	}
}
