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
package com.rapidminer.gui.graphs.plugins;

import java.awt.Point;
import java.awt.event.InputEvent;
import java.awt.event.MouseEvent;
import java.awt.event.MouseListener;
import java.awt.event.MouseMotionListener;
import java.awt.geom.Point2D;

import edu.uci.ics.jung.algorithms.layout.Layout;
import edu.uci.ics.jung.visualization.VisualizationViewer;
import edu.uci.ics.jung.visualization.control.PickingGraphMousePlugin;
import edu.uci.ics.jung.visualization.picking.PickedState;


/**
 * Does the same as {@link PickingGraphMousePlugin}, but also offers a toggle to disable rectangle
 * selection of nodes.
 *
 * @author Marco Boeck
 * @since 7.5.0
 *
 */
public class ExtendedPickingGraphMousePlugin<V, E> extends PickingGraphMousePlugin<V, E>
		implements MouseListener, MouseMotionListener {

	protected boolean rectangleSelectionEnabled;

	public ExtendedPickingGraphMousePlugin() {
		super(InputEvent.BUTTON1_MASK, InputEvent.BUTTON1_MASK | InputEvent.CTRL_MASK);
	}

	/**
	 * If the mouse is over a picked vertex, drag all picked vertices with the mouse. If the mouse
	 * is not over a Vertex, draw the rectangle to select multiple Vertices
	 *
	 */
	@Override
	@SuppressWarnings("unchecked")
	public void mouseDragged(MouseEvent e) {
		if (locked == false) {
			VisualizationViewer<V, E> vv = (VisualizationViewer<V, E>) e.getSource();
			if (vertex != null) {
				Point p = e.getPoint();
				Point2D graphPoint = vv.getRenderContext().getMultiLayerTransformer().inverseTransform(p);
				Point2D graphDown = vv.getRenderContext().getMultiLayerTransformer().inverseTransform(down);
				Layout<V, E> layout = vv.getGraphLayout();
				double dx = graphPoint.getX() - graphDown.getX();
				double dy = graphPoint.getY() - graphDown.getY();
				PickedState<V> ps = vv.getPickedVertexState();

				for (V v : ps.getPicked()) {
					Point2D vp = layout.transform(v);
					vp.setLocation(vp.getX() + dx, vp.getY() + dy);
					layout.setLocation(v, vp);
				}
				down = p;

			} else {
				if (rectangleSelectionEnabled) {
					Point2D out = e.getPoint();
					if (e.getModifiers() == this.addToSelectionModifiers || e.getModifiers() == modifiers) {
						rect.setFrameFromDiagonal(down, out);
					}
				}
			}
			if (vertex != null) {
				e.consume();
			}
			vv.repaint();
		}
	}

	/**
	 * @return Returns whether selecting nodes via selection rectangle is enabled.
	 */
	public boolean isRectangleSelectionEnabled() {
		return rectangleSelectionEnabled;
	}

	/**
	 * @param rectangleSelectionEnabled
	 *            Set whether selecting nodes via selection rectangle is enabled.
	 */
	public void setRectangleSelectionEnabled(boolean rectangleSelectionEnabled) {
		this.rectangleSelectionEnabled = rectangleSelectionEnabled;
	}

	/**
	 * <p>
	 * Only does something if {@link #isRectangleSelectionEnabled()} is {@code true}.
	 * </p>
	 * {@inheritDoc}
	 */
	@Override
	protected void pickContainedVertices(VisualizationViewer<V, E> vv, Point2D down, Point2D out, boolean clear) {
		// only do something if rectangleSelectionEnabled is true
		if (rectangleSelectionEnabled) {
			super.pickContainedVertices(vv, down, out, clear);
		}
	}
}
