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
package com.rapidminer.gui.animation;

import java.awt.Color;
import java.awt.Graphics2D;
import java.awt.Rectangle;


/**
 * {@link Animation} that displays a loading spinner.
 *
 * @author Gisa Schaefer
 * @since 7.1.0
 */
public class IndeterminateAnimation implements Animation {

	/** the milliseconds per full turn of the animation */
	private static final int MS_PER_TURN = 1500;

	/** the number by which to divide MS_PER_TURN with result NUMBER_OF_CIRCLES */
	private static final int DIVISOR = 125;

	/** the radius of the small circles */
	static final int RADIUS_SMALL_CIRCLES = 9;

	/** the radius of the circle on which the small circles lie */
	private static final int RADIUS_FROM_ORIGIN = 15;

	/** the radius of the circle containing the whole animation */
	static final int OUTER_RADIUS = RADIUS_FROM_ORIGIN + RADIUS_SMALL_CIRCLES;

	/** the number of circles in the animation */
	private static final int NUMBER_OF_CIRCLES = 12;

	/** the bounds of the animation */
	private static final Rectangle BOUNDS = new Rectangle(-OUTER_RADIUS, -OUTER_RADIUS, 2 * OUTER_RADIUS, 2 * OUTER_RADIUS);

	/** the different shades of colors for the small circles */
	static final Color[] SPINNER_COLORS = { new Color(52, 73, 94), new Color(52, 73, 94, 210), new Color(52, 73, 94, 170),
			new Color(52, 73, 94, 130), new Color(52, 73, 94, 90), new Color(52, 73, 94, 50), new Color(52, 73, 94, 10) };

	/** the start time of the animation */
	private final long start;

	public IndeterminateAnimation() {
		start = System.currentTimeMillis();
	}

	@Override
	public boolean isRedrawRequired() {
		return true;
	}

	@Override
	public void draw(Graphics2D graphics) {
		Graphics2D g2 = (Graphics2D) graphics.create();

		// go to start position
		long now = System.currentTimeMillis();
		int position = (int) ((now - start) % MS_PER_TURN) / DIVISOR;
		g2.rotate(Math.PI * 2 / NUMBER_OF_CIRCLES * position - Math.PI / 2);

		// draw the circles
		for (Color spinnerColor : SPINNER_COLORS) {
			g2.setColor(spinnerColor);
			g2.fillOval(RADIUS_FROM_ORIGIN, 0, RADIUS_SMALL_CIRCLES, RADIUS_SMALL_CIRCLES);
			g2.rotate(-Math.PI * 2 / NUMBER_OF_CIRCLES);
		}

		g2.dispose();
	}

	@Override
	public Rectangle getBounds() {
		return BOUNDS;
	}

}
