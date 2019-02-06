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
package com.rapidminer.gui.flow.processrendering.draw;

import java.awt.Graphics2D;

import com.rapidminer.gui.flow.processrendering.model.ProcessRendererModel;
import com.rapidminer.gui.flow.processrendering.view.ProcessEventDecorator;
import com.rapidminer.gui.flow.processrendering.view.ProcessRendererView;
import com.rapidminer.gui.flow.processrendering.view.RenderPhase;
import com.rapidminer.operator.ExecutionUnit;


/**
 * Implementations of this interface can be registered to decorate the process renderer drawing
 * process.
 * <p>
 * <strong>Attention:</strong> Drawing should work on a headless server, so implementations should
 * take extra care to not make any components which do not support headless mode. This especially
 * implies that implementing both this interface and {@link ProcessEventDecorator} should be
 * avoided.
 * </p>
 *
 * @author Marco Boeck
 * @since 6.4.0
 * @see ProcessDrawer#addDecorator(ProcessDrawDecorator,
 *      com.rapidminer.gui.flow.processrendering.view.RenderPhase)
 *
 */
public interface ProcessDrawDecorator {

	/**
	 * Draws the decoration during the {@link RenderPhase} specified while registering. This method
	 * is called when {@link ProcessRendererView#paintComponent(java.awt.Graphics)} was called.
	 *
	 * @param process
	 *            the process which can be decorated.
	 * @param g2
	 *            the graphics context to draw upon. Coordinates start at (0,0) aka the top left
	 *            corner and extend to {@link ProcessRendererModel#getProcessSize(ExecutionUnit)}
	 * @param model
	 *            the model backing the process rendering
	 */
	public void draw(final ExecutionUnit process, final Graphics2D g2, final ProcessRendererModel model);

	/**
	 * Prints the decoration during the {@link RenderPhase} specified while registering. This method
	 * is called when {@link ProcessRendererView#printComponent(java.awt.Graphics)} was called.
	 *
	 * @param process
	 *            the process which can be decorated.
	 * @param g2
	 *            the graphics context to draw upon. Coordinates start at (0,0) aka the top left
	 *            corner and extend to {@link ProcessRendererModel#getProcessSize(ExecutionUnit)}
	 * @param model
	 *            the model backing the process rendering
	 */
	public void print(final ExecutionUnit process, final Graphics2D g2, final ProcessRendererModel model);
}
