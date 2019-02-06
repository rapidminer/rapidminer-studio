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
package de.dfki.madm.paren.gui.renderer.models;

import java.awt.Component;

import com.rapidminer.gui.renderer.AbstractRenderer;
import com.rapidminer.gui.tools.ExtendedJScrollPane;
import com.rapidminer.operator.IOContainer;
import com.rapidminer.report.Reportable;

import de.dfki.madm.paren.operator.learner.functions.neuralnet.AutoMLPImprovedNeuralNetModel;
import de.dfki.madm.paren.operator.learner.functions.neuralnet.AutoMLPImprovedNeuralNetVisualizer;


/**
 *
 * @author Sebastian Land, modified by Syed Atif Mehdi (01/09/2010)
 */

public class AutoMLPImprovedNeuralNetModelRenderer extends AbstractRenderer {

	@Override
	public Reportable createReportable(Object renderable, IOContainer ioContainer, int desiredWidth, int desiredHeight) {
		AutoMLPImprovedNeuralNetModel model = (AutoMLPImprovedNeuralNetModel) renderable;
		return new AutoMLPImprovedNeuralNetVisualizer(model, model.getAttributeNames());
	}

	@Override
	public String getName() {
		return "AutoMLP Improved Neural Net";
	}

	@Override
	public Component getVisualizationComponent(Object renderable, IOContainer ioContainer) {
		AutoMLPImprovedNeuralNetModel model = (AutoMLPImprovedNeuralNetModel) renderable;
		return new ExtendedJScrollPane(new AutoMLPImprovedNeuralNetVisualizer(model, model.getAttributeNames()));
	}
}
