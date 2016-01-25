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
package com.rapidminer.gui.graphs;

import java.awt.Shape;
import java.awt.font.FontRenderContext;
import java.awt.geom.Rectangle2D;
import java.awt.geom.RoundRectangle2D;

import org.apache.commons.collections15.Transformer;


/**
 * The extended vertex shaper for the {@link GraphViewer}.
 * 
 * @author Ingo Mierswa
 */
public class ExtendedVertexShapeTransformer<V> implements Transformer<V, Shape> {

	private GraphCreator<V, ?> graphCreator;

	public ExtendedVertexShapeTransformer(GraphCreator<V, ?> graphCreator) {
		this.graphCreator = graphCreator;
	}

	@Override
	public Shape transform(V object) {
		if (graphCreator.isLeaf(object)) {
			// leaf
			String text = graphCreator.getVertexName(object);
			Rectangle2D stringBounds = GraphViewer.VERTEX_BOLD_FONT.getStringBounds(text, new FontRenderContext(null, false,
					false));
			float width = (float) stringBounds.getWidth();
			float height = (float) stringBounds.getHeight();
			int minWidth = graphCreator.getMinLeafWidth();
			int minHeight = graphCreator.getMinLeafHeight();
			width = Math.max(width, minWidth);
			height = Math.max(height, minHeight);
			return new Rectangle2D.Float(-width / 2.0f - 6.0f, -height / 2.0f - 2.0f, width + 8.0f, height + 4.0f);
		} else {
			// inner nodes
			String text = graphCreator.getVertexName(object);
			Rectangle2D stringBounds = GraphViewer.VERTEX_PLAIN_FONT.getStringBounds(text, new FontRenderContext(null,
					false, false));
			float width = (float) stringBounds.getWidth();
			float height = (float) stringBounds.getHeight();
			RoundRectangle2D.Float shape = new RoundRectangle2D.Float(-width / 2.0f - 6.0f, -height / 2.0f - 4.0f,
					width + 10.0f, height + 8.0f, 10.0f, 10.0f);
			return shape;
		}
	}
}
