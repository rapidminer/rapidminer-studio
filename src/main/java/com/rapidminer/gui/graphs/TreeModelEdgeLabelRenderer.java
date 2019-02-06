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

import java.awt.Component;
import java.awt.Dimension;
import java.awt.geom.AffineTransform;
import java.awt.geom.Point2D;

import com.rapidminer.gui.look.Colors;

import edu.uci.ics.jung.algorithms.layout.Layout;
import edu.uci.ics.jung.graph.Graph;
import edu.uci.ics.jung.graph.util.Context;
import edu.uci.ics.jung.graph.util.Pair;
import edu.uci.ics.jung.visualization.Layer;
import edu.uci.ics.jung.visualization.RenderContext;
import edu.uci.ics.jung.visualization.renderers.EdgeLabelRenderer;
import edu.uci.ics.jung.visualization.renderers.Renderer;
import edu.uci.ics.jung.visualization.transform.shape.GraphicsDecorator;


/**
 * This code is the basic edge label renderer from Jung (unfortunately there was no author given in
 * the original source) but it was changed so that the labels are always painted in the center
 * location of the edge.
 *
 * @author Ingo Mierswa
 */
public class TreeModelEdgeLabelRenderer<V, E> implements Renderer.EdgeLabel<V, E> {

	public Component prepareRenderer(RenderContext<V, E> rc, EdgeLabelRenderer graphLabelRenderer, Object value,
			boolean isSelected, E edge) {
		return rc.getEdgeLabelRenderer().<E> getEdgeLabelRendererComponent(rc.getScreenDevice(), value,
				rc.getEdgeFontTransformer().transform(edge), isSelected, edge);
	}

	@Override
	public void labelEdge(RenderContext<V, E> rc, Layout<V, E> layout, E e, String label) {
		if (label == null || label.length() == 0) {
			return;
		}

		Graph<V, E> graph = layout.getGraph();
		// don't draw edge if either incident vertex is not drawn
		Pair<V> endpoints = graph.getEndpoints(e);
		V v1 = endpoints.getFirst();
		V v2 = endpoints.getSecond();
		if (!rc.getVertexIncludePredicate().evaluate(Context.<Graph<V, E>, V> getInstance(graph, v1))
				|| !rc.getVertexIncludePredicate().evaluate(Context.<Graph<V, E>, V> getInstance(graph, v2))) {
			return;
		}

		Point2D p1 = layout.transform(v1);
		Point2D p2 = layout.transform(v2);
		p1 = rc.getMultiLayerTransformer().transform(Layer.LAYOUT, p1);
		p2 = rc.getMultiLayerTransformer().transform(Layer.LAYOUT, p2);
		float x1 = (float) p1.getX();
		float y1 = (float) p1.getY();
		float x2 = (float) p2.getX();
		float y2 = (float) p2.getY();

		GraphicsDecorator g = rc.getGraphicsContext();
		float distX = x2 - x1;
		float distY = y2 - y1;
		double totalLength = Math.sqrt(distX * distX + distY * distY);

		double closeness = rc.getEdgeLabelClosenessTransformer().transform(Context.<Graph<V, E>, E> getInstance(graph, e))
				.doubleValue();

		int posX = (int) (x1 + closeness * distX);
		int posY = (int) (y1 + closeness * distY);

		int xDisplacement = 0;
		int yDisplacement = 0;

		xDisplacement = (int) (rc.getLabelOffset() * (distX / totalLength));
		yDisplacement = (int) (rc.getLabelOffset() * (-distY / totalLength));

		AffineTransform old = g.getTransform();
		AffineTransform xform = new AffineTransform(old);
		xform.translate(posX + xDisplacement, posY + yDisplacement);

		double parallelOffset = 0.0d;
		Component component = prepareRenderer(rc, rc.getEdgeLabelRenderer(), label, rc.getPickedEdgeState().isPicked(e), e);
		Dimension d = component.getPreferredSize();
		xform.translate(-d.width / 2.0d, -(d.height / 2.0d - parallelOffset));
		g.setTransform(xform);
		g.setColor(Colors.WHITE);
		g.draw(component, rc.getRendererPane(), 0, 0, d.width, d.height, true);
		g.setTransform(old);
	}
}
