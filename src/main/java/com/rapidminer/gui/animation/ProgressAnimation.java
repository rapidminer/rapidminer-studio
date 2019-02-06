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

import java.awt.BasicStroke;
import java.awt.Color;
import java.awt.Font;
import java.awt.Graphics2D;
import java.awt.Rectangle;
import java.awt.RenderingHints;
import java.awt.geom.Rectangle2D;
import java.awt.image.BufferedImage;

import com.rapidminer.tools.FontTools;


/**
 * {@link Animation} that displays the progress of a {@link ProgressProvider}. If the progress is 0
 * a {@link IndeterminateAnimation} is displayed.
 *
 * @author Gisa Schaefer
 * @since 7.1.0
 */
public class ProgressAnimation implements Animation {

	/** the maximal angle. Not 360 because the stroke is too big. Using 360 the last 10% all look like 100% */
	private static final int FULL_ANGLE = 350;

	/** the angle where the arc starts. Slightly offset to the right due to big stroke size */
	private static final int START_ANGLE = 88;

	/**
	 * the maximal text width, width of the maximal square inside a circle with radius
	 * IndeterminateAnimation.OUTER_RADIUS minus a padding
	 */
	private static final int MAXIMAL_TEXT_WIDTH = (int) Math.sqrt(Math.pow(2.0 * IndeterminateAnimation.OUTER_RADIUS, 2) / 2)
			- 9;

	/** the line width of the arc line, an even number */
	private static final int LINE_WIDTH = IndeterminateAnimation.RADIUS_SMALL_CIRCLES / 2 * 2;

	/** the stroke used for the arc */
	private static final BasicStroke STROKE = new BasicStroke(LINE_WIDTH);

	/** the width of the arc when it is full */
	private static final int CIRCLE_DIAMETER = 2 * IndeterminateAnimation.OUTER_RADIUS - LINE_WIDTH;

	private static final Color ANIMATION_COLOR = IndeterminateAnimation.SPINNER_COLORS[0];

	/** the default font for the text inside the arc */
	private static final Font DEFAULT_FONT = FontTools.getFont(Font.SANS_SERIF, Font.BOLD, 17);

	/** the font with the maximal size such that it fits into the arc */
	private static Font platformSpecificFont;

	/** the text width of a two-digit number inside the arc */
	private static double textWidth = MAXIMAL_TEXT_WIDTH;

	/** the text width of a number inside the arc */
	private static double textHeight = MAXIMAL_TEXT_WIDTH;

	static {
		initializeFontMeasures();
	}

	private final IndeterminateAnimation indeterminateAnimation;
	private final ProgressProvider progressProvider;

	private int lastDrawnProgress = -1;

	public ProgressAnimation(ProgressProvider progressProvider) {
		indeterminateAnimation = new IndeterminateAnimation();
		this.progressProvider = progressProvider;
	}

	@Override
	public boolean isRedrawRequired() {
		int currentProgress = progressProvider.getProgress();
		if (currentProgress == 0) {
			return indeterminateAnimation.isRedrawRequired();
		}
		return currentProgress != lastDrawnProgress;
	}

	@Override
	public void draw(Graphics2D graphics) {
		Graphics2D g2 = (Graphics2D) graphics.create();
		int currentProgress = progressProvider.getProgress();
		if (currentProgress == 0) {
			indeterminateAnimation.draw(g2);
		} else {
			lastDrawnProgress = currentProgress;

			// draw arc
			g2.setColor(ANIMATION_COLOR);
			g2.setStroke(STROKE);
			// rendering hint that prevents arc from wobbling
			g2.setRenderingHint(RenderingHints.KEY_STROKE_CONTROL, RenderingHints.VALUE_STROKE_PURE);
			g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
			g2.setRenderingHint(RenderingHints.KEY_RENDERING, RenderingHints.VALUE_RENDER_QUALITY);
			g2.drawArc(-CIRCLE_DIAMETER / 2, -CIRCLE_DIAMETER / 2, CIRCLE_DIAMETER, CIRCLE_DIAMETER, START_ANGLE,
					-FULL_ANGLE * currentProgress / 100);

			// print progress if not 100
			if (currentProgress < 100) {
				g2.setFont(platformSpecificFont);
				final String text = "" + currentProgress;
				double textX = -textWidth / 2;
				if (currentProgress < 10) {
					textX = textX / 2;
				}
				g2.drawString(text, (int) textX, (int) textHeight / 2);
			}

			g2.dispose();
		}
	}

	@Override
	public Rectangle getBounds() {
		return indeterminateAnimation.getBounds();
	}

	/**
	 * Finds the font that fits into the progress arc. Stores the associated height and width.
	 */
	private static void initializeFontMeasures() {
		platformSpecificFont = DEFAULT_FONT;
		Graphics2D testGraphics = new BufferedImage(10, 10, BufferedImage.TYPE_INT_ARGB).createGraphics();
		float[] possibleFontSizes = { 25, 24, 23, 22, 21, 20, 19, 18, 17, 16, 15, 14, 13, 12, 11, 10 };

		for (float size : possibleFontSizes) {
			Font deriveFont = DEFAULT_FONT.deriveFont(size);
			testGraphics.setFont(deriveFont);
			String testString = "54";
			Rectangle2D stringBounds = deriveFont.getStringBounds(testString, testGraphics.getFontRenderContext());

			if (stringBounds.getWidth() <= MAXIMAL_TEXT_WIDTH) {
				platformSpecificFont = deriveFont;
				textWidth = stringBounds.getWidth();
				textHeight = deriveFont.createGlyphVector(testGraphics.getFontRenderContext(), testString).getVisualBounds()
						.getHeight();
				break;
			}

		}
	}

}
