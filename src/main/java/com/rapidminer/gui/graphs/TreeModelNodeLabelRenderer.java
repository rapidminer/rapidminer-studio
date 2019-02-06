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
import java.awt.Point;
import java.awt.Shape;
import java.awt.geom.AffineTransform;
import java.awt.geom.Point2D;
import java.awt.geom.Rectangle2D;

import edu.uci.ics.jung.algorithms.layout.Layout;
import edu.uci.ics.jung.graph.Graph;
import edu.uci.ics.jung.graph.util.Context;
import edu.uci.ics.jung.visualization.Layer;
import edu.uci.ics.jung.visualization.RenderContext;
import edu.uci.ics.jung.visualization.renderers.Renderer;
import edu.uci.ics.jung.visualization.renderers.VertexLabelRenderer;
import edu.uci.ics.jung.visualization.transform.BidirectionalTransformer;
import edu.uci.ics.jung.visualization.transform.shape.GraphicsDecorator;
import edu.uci.ics.jung.visualization.transform.shape.ShapeTransformer;
import edu.uci.ics.jung.visualization.transform.shape.TransformingGraphics;


/**
 * This renderer is used for rendering the labels of the tree model nodes.
 *
 * @author Ingo Mierswa
 *
 * @param <V>
 *            the type for vertices
 * @param <E>
 *            the type for edges
 */
public class TreeModelNodeLabelRenderer<V, E> implements Renderer.VertexLabel<V, E> {

	/** Used for positioning the label inside of a node, */
	public static class InsidePositioner implements Positioner {

		@Override
		public Position getPosition(float x, float y, Dimension d) {
			int cx = d.width / 2;
			int cy = d.height / 2;
			if (x > cx && y > cy) {
				return Position.NW;
			}
			if (x > cx && y < cy) {
				return Position.SW;
			}
			if (x < cx && y > cy) {
				return Position.NE;
			}
			return Position.SE;
		}
	}

	/** Used for positioning the label outside of a node, */
	public static class OutsidePositioner implements Positioner {

		@Override
		public Position getPosition(float x, float y, Dimension d) {
			int cx = d.width / 2;
			int cy = d.height / 2;
			if (x > cx && y > cy) {
				return Position.SE;
			}
			if (x > cx && y < cy) {
				return Position.NE;
			}
			if (x < cx && y > cy) {
				return Position.SW;
			}
			return Position.NW;
		}
	}

	private static final int LEAF_LABEL_OFFSET_Y = -15;

	protected Position position = Position.CNTR;
	private Positioner positioner = new OutsidePositioner();

	private TreeModelGraphCreator graphCreator;

	public TreeModelNodeLabelRenderer(TreeModelGraphCreator graphCreator) {
		this.graphCreator = graphCreator;
	}

	/**
	 * @return the position
	 */
	@Override
	public Position getPosition() {
		return position;
	}

	/**
	 * @param position
	 *            the position to set
	 */
	@Override
	public void setPosition(Position position) {
		this.position = position;
	}

	public Component prepareRenderer(RenderContext<V, E> rc, VertexLabelRenderer graphLabelRenderer, Object value,
			boolean isSelected, V vertex) {
		return rc.getVertexLabelRenderer().<V> getVertexLabelRendererComponent(rc.getScreenDevice(), value,
				rc.getVertexFontTransformer().transform(vertex), isSelected, vertex);
	}

	/**
	 * Labels the specified vertex with the specified label. Uses the font specified by this
	 * instance's <code>VertexFontFunction</code>. (If the font is unspecified, the existing font
	 * for the graphics context is used.) If vertex label centering is active, the label is centered
	 * on the position of the vertex; otherwise the label is offset slightly.
	 */
	@Override
	public void labelVertex(RenderContext<V, E> rc, Layout<V, E> layout, V v, String label) {
		Graph<V, E> graph = layout.getGraph();
		if (rc.getVertexIncludePredicate().evaluate(Context.<Graph<V, E>, V> getInstance(graph, v)) == false) {
			return;
		}
		Point2D pt = layout.transform(v);
		pt = rc.getMultiLayerTransformer().transform(Layer.LAYOUT, pt);

		float x = (float) pt.getX();
		float y = (float) pt.getY();

		Component component = prepareRenderer(rc, rc.getVertexLabelRenderer(), label, rc.getPickedVertexState().isPicked(v),
				v);
		GraphicsDecorator g = rc.getGraphicsContext();
		Dimension d = component.getPreferredSize();
		AffineTransform xform = AffineTransform.getTranslateInstance(x, y);

		Shape shape = rc.getVertexShapeTransformer().transform(v);
		shape = xform.createTransformedShape(shape);
		if (rc.getGraphicsContext() instanceof TransformingGraphics) {
			BidirectionalTransformer transformer = ((TransformingGraphics) rc.getGraphicsContext()).getTransformer();
			if (transformer instanceof ShapeTransformer) {
				ShapeTransformer shapeTransformer = (ShapeTransformer) transformer;
				shape = shapeTransformer.transform(shape);
			}
		}
		Rectangle2D bounds = shape.getBounds2D();

		Point p = null;
		if (position == Position.AUTO) {
			Dimension vvd = rc.getScreenDevice().getSize();
			if (vvd.width == 0 || vvd.height == 0) {
				vvd = rc.getScreenDevice().getPreferredSize();
			}
			p = getAnchorPoint(bounds, d, positioner.getPosition(x, y, vvd));
		} else {
			p = getAnchorPoint(bounds, d, position);
		}

		if (graphCreator.isLeaf((String) v) && !graphCreator.getModel().getRoot().isNumerical()) {
			// shift the label if there is a frequency bar
			p.setLocation(p.x, p.y + LEAF_LABEL_OFFSET_Y);
		}
		g.draw(component, rc.getRendererPane(), p.x, p.y, d.width, d.height, true);
	}

	protected Point getAnchorPoint(Rectangle2D vertexBounds, Dimension labelSize, Position position) {
		double x;
		double y;
		int offset = 5;
		switch (position) {

			case N:
				x = vertexBounds.getCenterX() - labelSize.width / 2.0d;
				y = vertexBounds.getMinY() - offset - labelSize.height;
				return new Point((int) x, (int) y);

			case NE:
				x = vertexBounds.getMaxX() + offset;
				y = vertexBounds.getMinY() - offset - labelSize.height;
				return new Point((int) x, (int) y);

			case E:
				x = vertexBounds.getMaxX() + offset;
				y = vertexBounds.getCenterY() - labelSize.height / 2.0d;
				return new Point((int) x, (int) y);

			case SE:
				x = vertexBounds.getMaxX() + offset;
				y = vertexBounds.getMaxY() + offset;
				return new Point((int) x, (int) y);

			case S:
				x = vertexBounds.getCenterX() - labelSize.width / 2.0d;
				y = vertexBounds.getMaxY() + offset;
				return new Point((int) x, (int) y);

			case SW:
				x = vertexBounds.getMinX() - offset - labelSize.width;
				y = vertexBounds.getMaxY() + offset;
				return new Point((int) x, (int) y);

			case W:
				x = vertexBounds.getMinX() - offset - labelSize.width;
				y = vertexBounds.getCenterY() - labelSize.height / 2.0d;
				return new Point((int) x, (int) y);

			case NW:
				x = vertexBounds.getMinX() - offset - labelSize.width;
				y = vertexBounds.getMinY() - offset - labelSize.height;
				return new Point((int) x, (int) y);

			case CNTR:
				x = vertexBounds.getCenterX() - labelSize.width / 2.0d;
				y = vertexBounds.getCenterY() - labelSize.height / 2.0d;
				return new Point((int) x, (int) y);

			default:
				return new Point();
		}

	}

	/**
	 * @return the positioner
	 */
	@Override
	public Positioner getPositioner() {
		return positioner;
	}

	/**
	 * @param positioner
	 *            the positioner to set
	 */
	@Override
	public void setPositioner(Positioner positioner) {
		this.positioner = positioner;
	}
}
