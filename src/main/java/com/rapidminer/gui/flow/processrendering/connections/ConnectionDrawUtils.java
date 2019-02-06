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
package com.rapidminer.gui.flow.processrendering.connections;

import java.awt.BasicStroke;
import java.awt.Color;
import java.awt.Graphics2D;
import java.awt.RenderingHints;
import java.awt.Shape;
import java.awt.geom.Ellipse2D;
import java.awt.geom.Point2D;

import javax.swing.ImageIcon;

import com.rapidminer.gui.flow.processrendering.draw.ProcessDrawUtils;
import com.rapidminer.gui.flow.processrendering.draw.ProcessDrawer;
import com.rapidminer.gui.flow.processrendering.model.ProcessRendererModel;
import com.rapidminer.gui.tools.SwingTools;
import com.rapidminer.operator.ports.OutputPort;
import com.rapidminer.operator.ports.Port;


/**
 * Utility class for drawing a connection deletion icon in the middle on a connection.
 *
 * @author Nils Woehler
 * @since 7.1.0
 */
public final class ConnectionDrawUtils {

	/** the stroke for the circle containing the trash icon */
	private static final BasicStroke CIRCLE_STROKE = new BasicStroke(2f, BasicStroke.CAP_BUTT, BasicStroke.JOIN_MITER);

	/** the black trash icon */
	private static final ImageIcon DELETE_ICON = SwingTools.createIcon("32/delete.png");

	/** the gray trash icon */
	private static final ImageIcon DELETE_ICON_DISABLED = ProcessDrawUtils.getDisabledIcon(DELETE_ICON);

	/** the size of the image */
	private static final int IMAGE_SIZE = 16;

	/** half the size of the image */
	private static final int HALF_IMAGE_SIZE = IMAGE_SIZE / 2;

	/** the diameter of the circle */
	private static final int CIRCLE_DIAMETER = 24;

	/** the width of the circle - one smaller than the diameter */
	private static final int CIRCLE_WIDTH = CIRCLE_DIAMETER - 1;

	/** half the height of the circle */
	private static final int CIRCLE_RADIUS = CIRCLE_DIAMETER / 2;

	/** color for the disabled icon and surrounding circle */
	private static final Color DISABLED_COLOR = new Color(240, 152, 150);

	/** color for the enabled icon and surrounding circle */
	private static final Color ENABLED_COLOR = new Color(227, 60, 49);

	/**
	 * Renders a trash icon in the middle of a connection.
	 *
	 * @param from
	 *            the port from which the connection starts
	 * @param enableTrashSymbol
	 *            if {@code true} the black trash icon is used, otherwise the gray one
	 * @param g2
	 *            the graphics for rendering
	 * @param model
	 *            the model providing port locations
	 * @return the circle in which the icon was drawn
	 */
	public static final Shape renderConnectionRemovalIcon(OutputPort from, boolean enableTrashSymbol, final Graphics2D g2,
			ProcessRendererModel model) {
		// no icon when dragging is in progress
		if (model.isDragStarted() || model.isImportDragged()) {
			return null;
		}

		if (from == null) {
			return null;
		}
		Port to = from.getDestination();
		if (to == null) {
			return null;
		}
		Point2D fromPoint = ProcessDrawUtils.createPortLocation(from, model);
		Point2D toPoint = ProcessDrawUtils.createPortLocation(to, model);

		if (fromPoint == null || toPoint == null) {
			return null;
		}

		fromPoint = new Point2D.Double(fromPoint.getX() + ProcessDrawer.PORT_SIZE / 2, fromPoint.getY());
		toPoint = new Point2D.Double(toPoint.getX() - ProcessDrawer.PORT_SIZE / 2, toPoint.getY());

		// calculate middle point of connection
		int cx = (int) ((fromPoint.getX() + toPoint.getX()) / 2);
		int cy = (int) ((fromPoint.getY() + toPoint.getY()) / 2);

		// circle around middle point
		Shape circle = new Ellipse2D.Double(cx - CIRCLE_RADIUS, cy - CIRCLE_RADIUS, CIRCLE_WIDTH, CIRCLE_WIDTH);

		g2.setRenderingHints(ProcessDrawer.HI_QUALITY_HINTS);
		g2.setStroke(CIRCLE_STROKE);

		// draw white background of the circle
		g2.setColor(Color.WHITE);
		g2.fill(circle);

		// draw the circle and the trash symbol
		g2.setRenderingHint(RenderingHints.KEY_INTERPOLATION, RenderingHints.VALUE_INTERPOLATION_BILINEAR);
		if (enableTrashSymbol) {
			g2.setColor(ENABLED_COLOR);
			g2.draw(circle);
			g2.drawImage(DELETE_ICON.getImage(), cx - HALF_IMAGE_SIZE, cy - HALF_IMAGE_SIZE, IMAGE_SIZE, IMAGE_SIZE, null);
		} else {
			g2.setColor(Color.LIGHT_GRAY);
			g2.draw(circle);
			g2.drawImage(DELETE_ICON_DISABLED.getImage(), cx - HALF_IMAGE_SIZE, cy - HALF_IMAGE_SIZE, IMAGE_SIZE, IMAGE_SIZE, null);
		}
		return circle;

	}
}
