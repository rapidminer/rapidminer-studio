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
package com.rapidminer.gui.flow;

import java.awt.Container;
import java.awt.event.AdjustmentEvent;
import java.awt.event.AdjustmentListener;
import java.awt.event.MouseWheelEvent;
import java.awt.event.MouseWheelListener;

import javax.swing.JScrollPane;
import javax.swing.RepaintManager;
import javax.swing.SwingUtilities;

import com.rapidminer.gui.flow.processrendering.view.ProcessRendererView;


/**
 * Class for handling the scrolling in the {@link ProcessPanel}. Handles normal scrolling and
 * zooming (Ctrl + scroll). For zooming the scrollbars are adjusted such that the mouse pointer
 * stays over the same point in the process canvas.
 *
 * @author Gisa Schaefer
 * @since 7.1.0
 *
 */
class ProcessPanelScroller {

	private final JScrollPane scrollPane;
	private final ProcessRendererView rendererView;

	private boolean zoomed = false;

	private int desiredHorizontalScrollValue = 0;
	private int desiredVerticalScrollValue = 0;

	/** the mouse wheel listener for the process panel */
	private final transient MouseWheelListener wheelListener = new MouseWheelListener() {

		@Override
		public void mouseWheelMoved(final MouseWheelEvent e) {
			if (e.isControlDown() && e.getWheelRotation() != 0) {
				double oldZoomFactor = rendererView.getModel().getZoomFactor();
				if (e.getWheelRotation() < 0) {
					rendererView.getModel().zoomIn();
				} else {
					rendererView.getModel().zoomOut();
				}
				rendererView.getModel().fireProcessZoomChanged();

				// calculate how the scrollbar needs to be adjusted for centered zoom
				double relativeZoomFactor = rendererView.getModel().getZoomFactor() / oldZoomFactor;
				double differenceHorizontal = e.getPoint().getX() * (relativeZoomFactor - 1);
				double differenceVertical = e.getPoint().getY() * (relativeZoomFactor - 1);

				int newX = Math.max(0, (int) (scrollPane.getHorizontalScrollBar().getValue() + differenceHorizontal));
				int newY = Math.max(0, (int) (scrollPane.getVerticalScrollBar().getValue() + differenceVertical));

				scrollPane.getHorizontalScrollBar().setValue(newX);
				scrollPane.getVerticalScrollBar().setValue(newY);

				// prevent flickering when another adjustment of the scrollbars is needed
				RepaintManager.currentManager(scrollPane).markCompletelyClean(scrollPane);

				/**
				 * Setting the value as above does not always work since the scrollbars are not yet
				 * updated to the size changes caused by the zooming. Set flag an values to try
				 * again after the resizing happened.
				 */
				zoomed = true;
				desiredVerticalScrollValue = newY;
				desiredHorizontalScrollValue = newX;
				return;
			}

			Container p = rendererView.getParent();
			if (p != null) {
				p.dispatchEvent(SwingUtilities.convertMouseEvent(rendererView, e, p));
			}
		}

	};

	/**
	 * Creates a handler for scrolling for the view and its surrounding scrollPane.
	 *
	 * @param view
	 *            the {@link ProcessRendererView}
	 * @param scrollPane
	 *            the {@link JScrollPane} containing the view
	 */
	ProcessPanelScroller(final ProcessRendererView view, final JScrollPane scrollPane) {
		this.scrollPane = scrollPane;
		this.rendererView = view;
		rendererView.addMouseWheelListener(wheelListener);

		// add listener to check for size changes of the scrollbar
		scrollPane.getHorizontalScrollBar().addAdjustmentListener(new AdjustmentListener() {

			private int lastValue;

			@Override
			public void adjustmentValueChanged(AdjustmentEvent e) {
				if (lastValue == scrollPane.getHorizontalScrollBar().getMaximum()) {
					return;
				}
				// the maximum value changed, so the process canvas size changed
				lastValue = scrollPane.getHorizontalScrollBar().getMaximum();
				if (zoomed) {
					// set the scrollbar values needed for centered zoom if needed
					if (scrollPane.getHorizontalScrollBar().getValue() != desiredHorizontalScrollValue) {
						scrollPane.getHorizontalScrollBar().setValue(desiredHorizontalScrollValue);
					}
					if (scrollPane.getVerticalScrollBar().getValue() != desiredVerticalScrollValue) {
						scrollPane.getVerticalScrollBar().setValue(desiredVerticalScrollValue);
					}
					zoomed = false;
				}
			}
		});
	}

}
