/**
 * Copyright (C) 2001-2017 by RapidMiner and the contributors
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
package com.rapidminer.gui.renderer;

import com.rapidminer.gui.viewer.AverageVectorViewer;
import com.rapidminer.operator.IOContainer;
import com.rapidminer.report.Reportable;
import com.rapidminer.tools.math.AverageVector;

import java.awt.Component;


/**
 * 
 * @author Simon Fischer
 */
public class AverageVectorRenderer extends AbstractRenderer {

	@Override
	public Reportable createReportable(Object renderable, IOContainer ioContainer, int desiredWidth, int desiredHeight) {
		AverageVector vector = (AverageVector) renderable;
		return new AverageVectorViewer(vector, ioContainer);
	}

	@Override
	public String getName() {
		return "Average Vector";
	}

	@Override
	public Component getVisualizationComponent(Object renderable, IOContainer ioContainer) {
		AverageVector vector = (AverageVector) renderable;
		return new AverageVectorViewer(vector, ioContainer);
	}
}
