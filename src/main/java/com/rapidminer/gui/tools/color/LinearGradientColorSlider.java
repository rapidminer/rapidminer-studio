/**
 * Copyright (C) 2001-2019 by RapidMiner and the contributors
 *
 * Complete list of developers available at our web site:
 *
 * http://rapidminer.com
 *
 * This program is free software: you can redistribute it and/or modify it under the terms of the GNU Affero General
 * Public License as published by the Free Software Foundation, either version 3 of the License, or (at your option) any
 * later version.
 *
 * This program is distributed in the hope that it will be useful, but WITHOUT ANY WARRANTY; without even the implied
 * warranty of MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the GNU Affero General Public License for more
 * details.
 *
 * You should have received a copy of the GNU Affero General Public License along with this program. If not, see
 * http://www.gnu.org/licenses/.
 */
package com.rapidminer.gui.tools.color;

import java.awt.Color;
import java.awt.Polygon;
import java.util.List;

import com.rapidminer.tools.container.Pair;


/**
 * A control component for choosing linear gradients between 2 or more gradient points via a slider.
 *
 * @author Marco Boeck
 * @since 9.2.0
 */
public class LinearGradientColorSlider extends ColorSlider {

	private static final String UI_CLASS = "LinearGradientColorSliderUI";


	/**
	 * Create a new linear gradient color slider instance.
	 *
	 * @param colorPoints
	 * 		the preset gradient points to start with, can be {@code null}
	 * @param minAmountOfGradientPoints
	 * 		the minimum number of gradient points that the user must keep
	 * @param maxAmountOfGradientPoints
	 * 		the maximum number of gradient points hat the user can add
	 */
	public LinearGradientColorSlider(List<ColorPoint> colorPoints, int minAmountOfGradientPoints, int maxAmountOfGradientPoints) {
		super(colorPoints, minAmountOfGradientPoints, maxAmountOfGradientPoints);
	}

	@Override
	public String getUIClassID() {
		return UI_CLASS;
	}

	@Override
	double getRelativeXForAbsoluteX(int absoluteX) {
		return (absoluteX - X_OFFSET) / (double) (getBarWidth());
	}

	@Override
	Polygon getShapeForColorPoint(ColorPoint colorPoint, boolean hoverShape) {
		int length = 12;
		int shapeOffset = 7;
		double x = colorPoint.getPoint() * getBarWidth();
		int[] xPoints;
		if (hoverShape) {
			xPoints = new int[] {
					(int) (x - length / 1.5 + X_OFFSET),
					(int) (x - length / 1.5 + X_OFFSET),
					(int) (x + length / 1.5 + X_OFFSET),
					(int) (x + length / 1.5 + X_OFFSET)
			};
		} else {
			xPoints = new int[] {
					(int) (x - length / 2d + X_OFFSET),
					(int) (x + X_OFFSET),
					(int) (x + length / 2d + X_OFFSET)
			};
		}
		int[] YPoints;
		if (hoverShape) {
			YPoints = new int[] {
					getHeight(),
					0,
					0,
					getHeight()
			};
		} else {
			YPoints = new int[]{
					getHeight() - BOTTOM_OFFSET - 2 + shapeOffset,
					getHeight() - 14 - BOTTOM_OFFSET - 2 + shapeOffset,
					getHeight() - BOTTOM_OFFSET - 2 + shapeOffset
			};
		}
		return new Polygon(xPoints, YPoints, hoverShape ? 4 : 3);
	}

	@Override
	protected boolean tryAddingPoint(int x) {
		if (canAddPoint()) {
			double newPoint = x / (double) (getBarWidth());
			newPoint = Math.round(newPoint * 100.0) / 100.0;
			Pair<ColorPoint, ColorPoint> points = getColorPointsAroundPixel(x);
			ColorPoint newGradientPoint;
			int newIndex;
			if (points.getFirst() != null) {
				newGradientPoint = new ColorPoint(newPoint, Color.WHITE);
				newIndex = colorPoints.indexOf(points.getFirst()) + 1;
			} else if (points.getSecond() != null) {
				newGradientPoint = new ColorPoint(newPoint, Color.WHITE);
				newIndex = colorPoints.indexOf(points.getSecond());
			} else {
				newGradientPoint = new ColorPoint(newPoint, Color.WHITE);
				newIndex = 0;
			}
			colorPoints.add(newIndex, newGradientPoint);
			setHoveredPoint(newGradientPoint);
			return true;
		}

		return false;
	}

	@Override
	protected void rearrangePoints() {
		// not needed
	}

	@Override
	protected Pair<ColorPoint, ColorPoint> getColorPointsAroundPixel(int x) {
		Pair<ColorPoint, ColorPoint> pair = new Pair<>(null, null);
		for (ColorPoint colorPoint : colorPoints) {
			if (getBarWidth() * colorPoint.getPoint() <= x) {
				pair.setFirst(colorPoint);
			} else {
				pair.setSecond(colorPoint);
				// we can end here because the stops are sorted by increasing point numbers. First one to the right is correct
				break;
			}
		}

		return pair;
	}

	@Override
	protected double getMinPoint() {
		return 0.0;
	}

	@Override
	protected double getMaxPoint() {
		return 1.0;
	}

}
