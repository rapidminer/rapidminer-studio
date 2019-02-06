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
package com.rapidminer.gui.graphs;

import java.awt.Shape;
import java.awt.font.FontRenderContext;
import java.awt.geom.Ellipse2D;
import java.awt.geom.Rectangle2D;

import org.apache.commons.collections15.Transformer;


/**
 * The extended vertex shaper for the {@link GraphViewer}.
 *
 * @author Ingo Mierswa, Marco Boeck
 */
public class ExtendedVertexShapeTransformer<V> implements Transformer<V, Shape> {

	private static final FontRenderContext FONT_RENDERER_CONTEXT = new FontRenderContext(null, true, false);

	private static final double CIRCLE_DIAMETER = 250;

	private static final double CIRCLE_MIN_DIAMETER = 10;

	private GraphCreator<V, ?> graphCreator;

	public ExtendedVertexShapeTransformer(GraphCreator<V, ?> graphCreator) {
		this.graphCreator = graphCreator;
	}

	@Override
	public Shape transform(V object) {
		if (graphCreator.isVertexCircle(object)) {
			double scale = graphCreator.getVertexScale(object);
			double width = CIRCLE_DIAMETER;
			double height = CIRCLE_DIAMETER;
			width = Math.max(scaleDiameterOfCircle(width, scale), CIRCLE_MIN_DIAMETER);
			height = Math.max(scaleDiameterOfCircle(height, scale), CIRCLE_MIN_DIAMETER);
			return new Ellipse2D.Double(-width / 2, -height / 2, width, height);
		} else {
			if (graphCreator.isLeaf(object)) {
				// leaf
				String text = graphCreator.getVertexName(object);
				Rectangle2D stringBounds = GraphViewer.VERTEX_BOLD_FONT.getStringBounds(text, FONT_RENDERER_CONTEXT);
				float width = (float) stringBounds.getWidth() + 20;
				float height = (float) stringBounds.getHeight() + 20;
				int minWidth = graphCreator.getMinLeafWidth();
				int minHeight = graphCreator.getMinLeafHeight();
				width = Math.max(width, minWidth);
				height = Math.max(height, minHeight);
				return new Rectangle2D.Float(-width / 2.0f - 6.0f, -height / 2.0f - 2.0f, width + 8.0f, height + 4.0f);
			} else {
				// inner nodes
				String text = graphCreator.getVertexName(object);
				Rectangle2D stringBounds = GraphViewer.VERTEX_BOLD_FONT.getStringBounds(text, FONT_RENDERER_CONTEXT);
				float width = (float) stringBounds.getWidth() + 20;
				float height = (float) stringBounds.getHeight() + 10;
				Rectangle2D.Float shape = new Rectangle2D.Float(-width / 2.0f - 6.0f, -height / 2.0f - 4.0f, width + 10.0f,
						height + 8.0f);
				return shape;
			}
		}
	}

	/**
	 * Scales the area of the circle according to the given scaling factor. Because humans suck at
	 * distinguishing actual circle areas, the diameter is scaled also with the Smooth function.
	 *
	 * @param d
	 *            the diameter of the circle to scale
	 * @param scale
	 *            must be between 0 and 1
	 * @return the scaled diameter
	 */
	private static double scaleDiameterOfCircle(double d, double scale) {
		return Math.sqrt(smoothstepFunction(scale) * Math.pow(d / 2, 2)) * 2;
	}

	/**
	 * Applies the Smoothstep function to the input.
	 *
	 * https://en.wikipedia.org/wiki/Smoothstep
	 *
	 * @param d
	 *            the input value
	 * @return the output of the function
	 */
	private static double smoothstepFunction(double d) {
		return 3 * Math.pow(d, 2) - 2 * Math.pow(d, 3);
	}
}
