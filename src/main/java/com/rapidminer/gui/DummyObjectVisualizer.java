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
package com.rapidminer.gui;

import com.rapidminer.ObjectVisualizer;
import com.rapidminer.gui.tools.SwingTools;


/**
 * A dummy visualizer, capable of visualizing anything, but actually doing nothing.
 * 
 * @author Michael Wurst, Ingo Mierswa
 */
public class DummyObjectVisualizer implements ObjectVisualizer {

	@Override
	public void startVisualization(Object objId) {
		SwingTools.showVerySimpleErrorMessage("no_visual_for_obj", objId);
	}

	@Override
	public void stopVisualization(Object objId) {}

	@Override
	public String getTitle(Object objId) {
		return objId.toString();
	}

	@Override
	public boolean isCapableToVisualize(Object id) {
		return true;
	}

	@Override
	public String getDetailData(Object id, String fieldName) {
		return null;
	}

	@Override
	public String[] getFieldNames(Object id) {
		return new String[0];
	}
}
