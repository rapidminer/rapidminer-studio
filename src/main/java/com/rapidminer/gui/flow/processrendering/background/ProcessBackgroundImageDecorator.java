/**
 * Copyright (C) 2001-2020 by RapidMiner and the contributors
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
package com.rapidminer.gui.flow.processrendering.background;

import com.rapidminer.gui.flow.processrendering.draw.ProcessDrawDecorator;
import com.rapidminer.gui.flow.processrendering.view.ProcessRendererView;
import com.rapidminer.gui.flow.processrendering.view.RenderPhase;


/**
 * This class handles the draw decorator registered to the {@link ProcessRendererView} for process
 * background images.
 *
 * @author Marco Boeck
 * @since 7.0.0
 *
 */
public final class ProcessBackgroundImageDecorator {

	/** the process renderer */
	private final ProcessRendererView view;

	/**
	 * draws process (free-flowing) annotations behind operators
	 */
	private ProcessDrawDecorator processBackgroundImageDrawer;


	/**
	 * Creates a new process background image decorator
	 *
	 * @param view
	 *            the process renderer instance
	 */
	public ProcessBackgroundImageDecorator(final ProcessRendererView view) {
		this.view = view;
		this.processBackgroundImageDrawer =  new ProcessBackgroundDrawDecorator(thread -> view.getModel().fireMiscChanged());
	}

	/**
	 * Registers the event hooks and draw decorators to the process renderer.
	 */
	void registerDecorators() {
		view.addDrawDecorator(processBackgroundImageDrawer, RenderPhase.BACKGROUND);
	}

	/**
	 * Removes the event hooks and draw decorators from the process renderer.
	 */
	void unregisterDecorators() {
		view.removeDrawDecorator(processBackgroundImageDrawer, RenderPhase.BACKGROUND);
	}

}
