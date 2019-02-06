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
import java.util.concurrent.atomic.AtomicInteger;

import com.rapidminer.tools.container.Pair;


/**
 * A control component for choosing any amount of distinct color points via a slider.
 *
 * @author Marco Boeck
 * @since 9.2.0
 */
public class DistinctColorSlider extends ColorSlider {

	private static final String UI_CLASS = "DistinctColorSliderUI";


	/**
	 * Create a new distinct color slider instance.
	 *
	 * @param colorPoints
	 * 		the preset color points to start with, can be {@code null}
	 * @param minAmountOfColors
	 * 		the minimum number of color points that the user must keep
	 * @param maxAmountOfColors
	 * 		the maximum number of color points hat the user can add
	 */
	public DistinctColorSlider(List<ColorPoint> colorPoints, int minAmountOfColors, int maxAmountOfColors) {
		super(colorPoints, minAmountOfColors, maxAmountOfColors);
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
		double x = colorPoint.getPoint() * getBarWidth();
		int length = 10;
		int shapeOffset = 10;
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
					getHeight() - BOTTOM_OFFSET - 2 + shapeOffset,
					0,
					0,
					getHeight() - BOTTOM_OFFSET - 2 + shapeOffset
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
			Pair<ColorPoint, ColorPoint> points = getColorPointsAroundPixel(x);
			int newIndex;
			if (points.getFirst() != null) {
				newIndex = colorPoints.indexOf(points.getFirst()) + 1;
			} else if (points.getSecond() != null) {
				newIndex = colorPoints.indexOf(points.getSecond());
			} else {
				newIndex = 0;
			}

			ColorPoint newColorPoint = new ColorPoint(0d, Color.BLACK);
			colorPoints.add(newIndex, newColorPoint);
			setHoveredPoint(newColorPoint);

			return true;
		}

		return false;
	}

	@Override
	protected void rearrangePoints() {
		if (colorPoints.isEmpty()) {
			return;
		}

		double stepSize = 100d / colorPoints.size();
		AtomicInteger counter = new AtomicInteger(1);
		colorPoints.forEach(c ->  {
			if (c.equals(draggedPoint)) {
				// just increment counter, ignore it here
				counter.getAndIncrement();
			} else {
				double step = stepSize / 100;
				c.setPoint(counter.getAndIncrement() * step - (step / 2));
			}
		});
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
