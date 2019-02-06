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
package com.rapidminer.gui.renderer.models;

import com.rapidminer.gui.renderer.AbstractRenderer;
import com.rapidminer.gui.tools.ExtendedJScrollPane;
import com.rapidminer.operator.IOContainer;
import com.rapidminer.operator.learner.functions.neuralnet.ImprovedNeuralNetModel;
import com.rapidminer.operator.learner.functions.neuralnet.ImprovedNeuralNetVisualizer;
import com.rapidminer.report.Reportable;

import java.awt.Component;


/**
 * 
 * @author Sebastian Land
 */
public class ImprovedNeuralNetModelRenderer extends AbstractRenderer {

	@Override
	public Reportable createReportable(Object renderable, IOContainer ioContainer, int desiredWidth, int desiredHeight) {
		ImprovedNeuralNetModel model = (ImprovedNeuralNetModel) renderable;
		return new ImprovedNeuralNetVisualizer(model, model.getAttributeNames());
	}

	@Override
	public String getName() {
		return "Improved Neural Net";
	}

	@Override
	public Component getVisualizationComponent(Object renderable, IOContainer ioContainer) {
		ImprovedNeuralNetModel model = (ImprovedNeuralNetModel) renderable;
		return new ExtendedJScrollPane(new ImprovedNeuralNetVisualizer(model, model.getAttributeNames()));
	}
}
