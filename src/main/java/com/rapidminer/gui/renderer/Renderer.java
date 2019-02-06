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
package com.rapidminer.gui.renderer;

import com.rapidminer.gui.actions.export.PrintableComponent;
import com.rapidminer.operator.IOContainer;
import com.rapidminer.operator.ports.InputPort;
import com.rapidminer.parameter.ParameterHandler;
import com.rapidminer.parameter.Parameters;
import com.rapidminer.report.Reportable;

import java.awt.Component;


/**
 * This is the renderer interface. A renderer is a visualization component for all types of objects. In addition, it
 * should also deliver an object of the interface {@link Reportable} in order to support automatic reporting actions.
 * <p>
 * <strong>Note:</strong> If your renderer contains components that require clean-up or should interrupt long-running
 * calculations if the visualization component is no longer needed (i.e. the result tab has been closed), these
 * components should implement the {@link com.rapidminer.gui.CleanupRequiringComponent} interface. The component
 * hierarchy is traversed and all implementations of the interface are notified that the renderer is no longer needed.
 * </p>
 *
 * @author Ingo Mierswa
 */
public interface Renderer extends ParameterHandler {

	String getName();

	// TODO: Find a solution for non existing IOContainer

	/**
	 * <p>
	 * <strong>Note:</strong> If your visualization component contains components that require clean-up or should
	 * interrupt
	 * long-running calculations if the visualization component is no longer needed (i.e. the result tab has been
	 * closed), these components should implement the {@link com.rapidminer.gui.CleanupRequiringComponent} interface.
	 * The component hierarchy is traversed and all implementations of the interface are notified that the renderer is
	 * no longer needed.
	 * </p>
	 *
	 * @return the {@link Component} that visualizes the renderable. If if should be printable/exportable the component
	 * should extend the {@link PrintableComponent} interface.
	 */
	Component getVisualizationComponent(Object renderable, IOContainer ioContainer);

	// TODO: Find a solution for non existing IOContainer
	Reportable createReportable(Object renderable, IOContainer ioContainer, int desiredWidth, int desiredHeight);

	@Override
	String toString();

	Parameters getParameters(InputPort inputPort);

	/**
	 * This method overrides all existing parameters. It must be used to ensure, that input Port
	 * referencing attributes are connected to the correct port, since they are only created once
	 * and might be initialized from another operator.
	 */
	void updateParameters(InputPort inputPort);
}
