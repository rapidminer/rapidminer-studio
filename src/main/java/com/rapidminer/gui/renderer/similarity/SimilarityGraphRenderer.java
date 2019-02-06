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
package com.rapidminer.gui.renderer.similarity;

import com.rapidminer.gui.graphs.GraphCreator;
import com.rapidminer.gui.graphs.SimilarityGraphCreator;
import com.rapidminer.gui.renderer.AbstractGraphRenderer;
import com.rapidminer.operator.IOContainer;
import com.rapidminer.operator.similarity.SimilarityMeasureObject;


/**
 * A renderer for the graph view of a similarity measure.
 * 
 * @author Ingo Mierswa
 */
public class SimilarityGraphRenderer extends AbstractGraphRenderer {

	@Override
	public GraphCreator<String, String> getGraphCreator(Object renderable, IOContainer ioContainer) {
		SimilarityMeasureObject sim = (SimilarityMeasureObject) renderable;
		return new SimilarityGraphCreator(sim.getDistanceMeasure(), sim.getExampleSet());
	}
}
