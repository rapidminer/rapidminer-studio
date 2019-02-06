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
import java.util.Objects;


/**
 * Simple POJO for color points with a value and a color.
 *
 * @author Marco Boeck
 * @since 9.2.0
 */
public class ColorPoint {

	private double point;
	private Color color;


	/**
	 * Creates a new instance with the given parameters.
	 *
	 * @param point
	 *         the point identifying the location, >= 0
	 * @param color
	 *         the color for this point. If {@code null}, will fall back to white
	 */
	public ColorPoint(double point, Color color) {
		if (point < 0.0) {
			point = 0.0;
		}
		if (color == null) {
			color = Color.WHITE;
		}

		setPoint(point);
		setColor(color);
	}


	/**
	 * @return the point identifying the location
	 */
	public double getPoint() {
		return point;
	}

	/**
	 * Set the point.
	 * @param point the point, must be >= 0
	 */
	public void setPoint(double point) {
		if (point < 0.0) {
			throw new IllegalArgumentException("point must not be < 0!");
		}

		this.point = point;
	}

	/**
	 * @return the color for this point, never {@code null}
	 */
	public Color getColor() {
		return color;
	}

	public void setColor(Color color) {
		if (color == null) {
			throw new IllegalArgumentException("color must not be null!");
		}

		this.color = color;
	}

	@Override
	public boolean equals(Object o) {
		if (this == o) {
			return true;
		}
		if (o == null || getClass() != o.getClass()) {
			return false;
		}
		ColorPoint that = (ColorPoint) o;
		return Double.compare(that.point, point) == 0 &&
				Objects.equals(color, that.color);
	}

	@Override
	public int hashCode() {
		return Objects.hash(point, color);
	}

	@Override
	public String toString() {
		return "ColorPoint{" +
				"point=" + getPoint() +
				", color=" + getColor() +
				'}';
	}
}
