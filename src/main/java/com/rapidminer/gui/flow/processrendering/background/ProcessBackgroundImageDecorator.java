/**
 * Copyright (C) 2001-2016 by RapidMiner and the contributors
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

import java.awt.Graphics2D;

import com.rapidminer.gui.flow.processrendering.draw.ProcessDrawDecorator;
import com.rapidminer.gui.flow.processrendering.model.ProcessRendererModel;
import com.rapidminer.gui.flow.processrendering.view.ProcessRendererView;
import com.rapidminer.gui.flow.processrendering.view.RenderPhase;
import com.rapidminer.gui.tools.ProgressThread;
import com.rapidminer.gui.tools.ProgressThreadListener;
import com.rapidminer.operator.ExecutionUnit;


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

	/** triggers if the image has been loaded */
	private final ProgressThreadListener loadListener;

	/** draws process (free-flowing) annotations behind operators */
	private ProcessDrawDecorator processBackgroundImageDrawer = new ProcessDrawDecorator() {

		@Override
		public void draw(final ExecutionUnit process, final Graphics2D g2, final ProcessRendererModel rendererModel) {
			draw(process, g2, rendererModel, false);
		}

		@Override
		public void print(final ExecutionUnit process, final Graphics2D g2, final ProcessRendererModel rendererModel) {
			draw(process, g2, rendererModel, true);
		}

		/**
		 * Draws the background annoations.
		 */
		private void draw(final ExecutionUnit process, final Graphics2D g2, final ProcessRendererModel rendererModel,
				final boolean printing) {

			// background annotations
			ProcessBackgroundImage image = rendererModel.getBackgroundImage(process);
			if (image != null) {
				Graphics2D g2D = (Graphics2D) g2.create();

				int x = image.getX();
				int y = image.getY();
				int w = image.getWidth();
				int h = image.getHeight();

				// center image now if desired
				if (x == -1) {
					double processWidth = rendererModel.getProcessWidth(process);
					x = (int) ((processWidth - w) / 2);
				}
				if (y == -1) {
					double processHeight = rendererModel.getProcessHeight(process);
					y = (int) ((processHeight - h) / 2);
				}

				g2D.drawImage(image.getImage(loadListener), x, y, w, h, null);
				g2D.dispose();
			}
		}
	};

	/**
	 * Creates a new process background image decorator
	 *
	 * @param view
	 *            the process renderer instance
	 */
	public ProcessBackgroundImageDecorator(final ProcessRendererView view) {
		this.view = view;
		this.loadListener = new ProgressThreadListener() {

			@Override
			public void threadFinished(ProgressThread thread) {
				// make sure process is rendered again
				view.getModel().fireMiscChanged();
			}
		};
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
