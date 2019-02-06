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
package com.rapidminer.gui.renderer.performance;

import com.rapidminer.gui.renderer.AbstractRenderer;
import com.rapidminer.gui.viewer.PerformanceVectorViewer;
import com.rapidminer.operator.IOContainer;
import com.rapidminer.operator.performance.PerformanceCriterion;
import com.rapidminer.operator.performance.PerformanceVector;
import com.rapidminer.operator.ports.InputPort;
import com.rapidminer.parameter.ParameterType;
import com.rapidminer.parameter.ParameterTypeString;
import com.rapidminer.parameter.UndefinedParameterError;
import com.rapidminer.report.Reportable;

import java.awt.Component;
import java.util.List;


/**
 * This is the renderer for the performance vector table (confusion matrix etc.)
 * 
 * @author Ingo Mierswa
 */
public class PerformanceVectorRenderer extends AbstractRenderer {

	private static final String PARAMETER_CRITERION = "criterion";

	@Override
	public String getName() {
		return "Performance";
	}

	@Override
	public Reportable createReportable(Object renderable, IOContainer ioContainer, int desiredWidth, int desiredHeight) {
		PerformanceVector performanceVector = (PerformanceVector) renderable;
		String criterionName = null;
		try {
			criterionName = getParameterAsString(PARAMETER_CRITERION);
		} catch (UndefinedParameterError e) {
			// do nothing
		}

		PerformanceCriterion criterion = null;
		if (criterionName != null) {
			criterion = performanceVector.getCriterion(criterionName);
		}

		if (criterion == null) {
			criterion = performanceVector.getMainCriterion();
		}

		return criterion;
	}

	@Override
	public Component getVisualizationComponent(Object renderable, IOContainer ioContainer) {
		PerformanceVector performanceVector = (PerformanceVector) renderable;
		return new PerformanceVectorViewer(performanceVector, ioContainer);
	}

	@Override
	public List<ParameterType> getParameterTypes(InputPort inputPort) {
		List<ParameterType> types = super.getParameterTypes(inputPort);
		types.add(new ParameterTypeString(PARAMETER_CRITERION,
				"Indicates which criterion should be reported (empty: use main criterion).", true));
		return types;
	}
}
