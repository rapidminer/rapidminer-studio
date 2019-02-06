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

import java.awt.Color;
import java.awt.Dimension;
import java.awt.Paint;
import java.awt.Rectangle;
import java.awt.Shape;
import java.awt.Stroke;
import java.awt.geom.AffineTransform;
import java.awt.geom.Point2D;
import java.awt.geom.Rectangle2D;
import java.util.Map;
import javax.swing.Icon;
import javax.swing.JComponent;

import com.rapidminer.example.Attribute;
import com.rapidminer.gui.plotter.ColorProvider;
import com.rapidminer.operator.learner.tree.Tree;
import com.rapidminer.operator.learner.tree.TreePredictionModel;

import edu.uci.ics.jung.algorithms.layout.Layout;
import edu.uci.ics.jung.graph.Graph;
import edu.uci.ics.jung.graph.util.Context;
import edu.uci.ics.jung.visualization.Layer;
import edu.uci.ics.jung.visualization.RenderContext;
import edu.uci.ics.jung.visualization.renderers.Renderer;
import edu.uci.ics.jung.visualization.transform.MutableTransformer;
import edu.uci.ics.jung.visualization.transform.MutableTransformerDecorator;
import edu.uci.ics.jung.visualization.transform.shape.GraphicsDecorator;


/**
 * This class is used for rendering the nodes of a tree model.
 *
 * @author Ingo Mierswa
 *
 * @param <V>
 * @param <E>
 */
public class TreeModelNodeRenderer<V, E> implements Renderer.Vertex<V, E> {

	private static final int FREQUENCY_BAR_MIN_HEIGHT = 1;
	private static final int FREQUENCY_BAR_MAX_HEIGHT = 22;
	private static final int FREQUENCY_BAR_OFFSET_X = 5;
	private static final int FREQUENCY_BAR_OFFSET_Y = 6;

	private TreeModelGraphCreator graphCreator;
	private TreePredictionModel model;
	private int maxLeafSize;

	public TreeModelNodeRenderer(TreeModelGraphCreator graphCreator, int maxLeafSize) {
		this.graphCreator = graphCreator;
		this.model = graphCreator.getModel();
		this.maxLeafSize = maxLeafSize;
	}

	@Override
	public void paintVertex(RenderContext<V, E> rc, Layout<V, E> layout, V v) {
		Graph<V, E> graph = layout.getGraph();
		if (rc.getVertexIncludePredicate().evaluate(Context.<Graph<V, E>, V> getInstance(graph, v))) {
			paintIconForVertex(rc, v, layout);
		}
	}

	/**
	 * Paint <code>v</code>'s icon on <code>g</code> at <code>(x,y)</code>.
	 */
	protected void paintIconForVertex(RenderContext<V, E> rc, V v, Layout<V, E> layout) {
		GraphicsDecorator g = rc.getGraphicsContext();
		boolean vertexHit = true;
		// get the shape to be rendered
		Shape shape = rc.getVertexShapeTransformer().transform(v);

		Point2D p = layout.transform(v);
		p = rc.getMultiLayerTransformer().transform(Layer.LAYOUT, p);
		float x = (float) p.getX();
		float y = (float) p.getY();
		// create a transform that translates to the location of
		// the vertex to be rendered
		AffineTransform xform = AffineTransform.getTranslateInstance(x, y);
		// transform the vertex shape with xtransform
		shape = xform.createTransformedShape(shape);

		vertexHit = vertexHit(rc, shape);
		// rc.getViewTransformer().transform(shape).intersects(deviceRectangle);

		if (vertexHit) {
			if (rc.getVertexIconTransformer() != null) {
				Icon icon = rc.getVertexIconTransformer().transform(v);
				if (icon != null) {

					g.draw(icon, rc.getScreenDevice(), shape, (int) x, (int) y);

				} else {
					paintShapeForVertex(rc, v, shape);
				}
			} else {
				paintShapeForVertex(rc, v, shape);
			}
		}
	}

	protected boolean vertexHit(RenderContext<V, E> rc, Shape s) {
		JComponent vv = rc.getScreenDevice();
		Rectangle deviceRectangle = null;
		if (vv != null) {
			Dimension d = vv.getSize();
			deviceRectangle = new Rectangle(0, 0, d.width, d.height);
		}
		if (deviceRectangle != null) {
			MutableTransformer vt = rc.getMultiLayerTransformer().getTransformer(Layer.VIEW);
			if (vt instanceof MutableTransformerDecorator) {
				vt = ((MutableTransformerDecorator) vt).getDelegate();
			}
			return vt.transform(s).intersects(deviceRectangle);
		} else {
			return false;
		}
	}

	protected void paintShapeForVertex(RenderContext<V, E> rc, V v, Shape shape) {
		GraphicsDecorator g = rc.getGraphicsContext();
		Paint oldPaint = g.getPaint();
		Paint fillPaint = rc.getVertexFillPaintTransformer().transform(v);
		if (fillPaint != null) {
			g.setPaint(fillPaint);
			g.fill(shape);
			g.setPaint(oldPaint);
		}
		Paint drawPaint = rc.getVertexDrawPaintTransformer().transform(v);
		if (drawPaint != null) {
			g.setPaint(drawPaint);
			Stroke oldStroke = g.getStroke();
			Stroke stroke = rc.getVertexStrokeTransformer().transform(v);
			if (stroke != null) {
				g.setStroke(stroke);
			}
			g.draw(shape);
			g.setPaint(oldPaint);
			g.setStroke(oldStroke);

		}

		// leaf: draw frequency colors if nominal
		if (graphCreator.isLeaf((String) v)) {
			Attribute label = model.getTrainingHeader().getAttributes().getLabel();
			if (label.isNominal()) {
				Tree tree = graphCreator.getTree((String) v);
				Map<String, Integer> countMap = tree.getCounterMap();
				int numberOfLabels = countMap.size();
				int frequencySum = tree.getFrequencySum();

				double height = tree.getFrequencySum() / (double) maxLeafSize
						* (FREQUENCY_BAR_MAX_HEIGHT - FREQUENCY_BAR_MIN_HEIGHT) + FREQUENCY_BAR_MIN_HEIGHT;
				double width = shape.getBounds().getWidth() - 2 * FREQUENCY_BAR_OFFSET_X - 1;
				double xPos = shape.getBounds().getX() + FREQUENCY_BAR_OFFSET_X;
				double yPos = shape.getBounds().getY() + shape.getBounds().getHeight() - FREQUENCY_BAR_OFFSET_Y - height;
				ColorProvider colorProvider = new ColorProvider();
				for (String labelValue : countMap.keySet()) {
					int count = tree.getCount(labelValue);
					double currentWidth = (double) count / (double) frequencySum * width;
					Rectangle2D.Double frequencyRect = new Rectangle2D.Double(xPos, yPos, currentWidth, height);
					int counter = label.getMapping().mapString(labelValue);
					g.setColor(colorProvider.getPointColor((double) counter / (double) (numberOfLabels - 1)));
					g.fill(frequencyRect);

					g.setColor(Color.BLACK);
					xPos += currentWidth;
				}

				g.setPaint(oldPaint);
			}
		}
	}
}
