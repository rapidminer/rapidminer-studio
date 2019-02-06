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
package com.rapidminer.gui.new_plotter.engine.jfreechart.legend;

import java.awt.GradientPaint;
import java.awt.Graphics2D;
import java.awt.LinearGradientPaint;
import java.awt.Paint;
import java.awt.Shape;
import java.awt.geom.Point2D;
import java.awt.geom.Rectangle2D;

import org.jfree.chart.title.LegendGraphic;
import org.jfree.ui.RectangleAnchor;
import org.jfree.util.ShapeUtilities;


/**
 * A {@link LegendGraphic} which correctly translates gradients, such that they begin at the correct
 * position instead of the left screen edge.
 * 
 * @author Marius Helf
 * @deprecated since 9.2.0
 */
@Deprecated
public class CustomLegendGraphic extends LegendGraphic {

	private static final long serialVersionUID = 1L;

	public CustomLegendGraphic(Shape shape, Paint fillPaint) {
		super(shape, fillPaint);
	}

	@Override
	public void draw(Graphics2D g2, Rectangle2D area) {

		area = trimMargin(area);
		drawBorder(g2, area);
		area = trimBorder(area);
		area = trimPadding(area);

		if (isLineVisible()) {
			Point2D location = RectangleAnchor.coordinates(area, getShapeLocation());
			Shape aLine = ShapeUtilities
					.createTranslatedShape(getLine(), getShapeAnchor(), location.getX(), location.getY());
			g2.setPaint(getLinePaint());
			g2.setStroke(getLineStroke());
			g2.draw(aLine);
		}

		if (isShapeVisible()) {
			Point2D location = RectangleAnchor.coordinates(area, getShapeLocation());

			Shape s = ShapeUtilities.createTranslatedShape(getShape(), getShapeAnchor(), location.getX(), location.getY());
			if (isShapeFilled()) {
				Paint p = getFillPaint();
				if (p instanceof GradientPaint) {
					GradientPaint gp = (GradientPaint) getFillPaint();
					p = getFillPaintTransformer().transform(gp, s);
				} else if (p instanceof LinearGradientPaint) {
					LinearGradientPaint gradient = (LinearGradientPaint) p;
					Rectangle2D bounds = s.getBounds2D();
					p = getTranslatedLinearGradientPaint(gradient, new Point2D.Double(bounds.getMinX(), bounds.getMinY()),
							new Point2D.Double(bounds.getMaxX(), bounds.getMaxY()), false);
				}
				g2.setPaint(p);
				g2.fill(s);
			}
			if (isShapeOutlineVisible()) {
				g2.setPaint(getOutlinePaint());
				g2.setStroke(getOutlineStroke());
				g2.draw(s);
			}
		}

	}

	private static LinearGradientPaint getTranslatedLinearGradientPaint(LinearGradientPaint gradient, Point2D startPoint,
			Point2D endPoint, boolean vertical) {
		if (vertical) {
			return new LinearGradientPaint(0f, (float) startPoint.getY(), 0f, (float) endPoint.getY(),
					gradient.getFractions(), gradient.getColors());
		} else {
			return new LinearGradientPaint((float) startPoint.getX(), 0f, (float) endPoint.getX(), 0f,
					gradient.getFractions(), gradient.getColors());
		}
	}
}
