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
package com.rapidminer;

/**
 * Interface managing the visualization of objects. This might be a dialog showing the feature
 * values ({@link com.rapidminer.gui.ExampleVisualizer}) or more sophisticated methods like
 * displaying a text or playing a piece of music. Please note that GUI components should not be
 * constructed in the contstructor but in the method {@link #startVisualization(Object)} in order to
 * ensure that the visualizer can be constructed also in environments where graphical user
 * interfaces are not allowed.
 * 
 * @author Michael Wurst, Ingo Mierswa
 */
public interface ObjectVisualizer {

	public void startVisualization(Object objId);

	public void stopVisualization(Object objId);

	public String getTitle(Object objId);

	public boolean isCapableToVisualize(Object objId);

	public String getDetailData(Object objId, String fieldName);

	public String[] getFieldNames(Object objId);

}
