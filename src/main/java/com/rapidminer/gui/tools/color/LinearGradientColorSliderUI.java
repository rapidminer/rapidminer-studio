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
import java.awt.Graphics2D;
import java.util.List;
import javax.swing.JComponent;
import javax.swing.plaf.ComponentUI;

import com.rapidminer.gui.look.Colors;


/**
 * The UI for a linear gradient color slider.
 *
 * @author Marco Boeck
 * @since 9.2.0
 */
public class LinearGradientColorSliderUI extends ColorSliderUI {


	@Override
	protected void drawColors(Graphics2D g2) {
		int x = ColorSlider.X_OFFSET;
		int y = 0;
		int width = colorSlider.getBarWidth();
		int height = colorSlider.getHeight() - ColorSlider.BOTTOM_OFFSET;

		List<ColorPoint> colorPoints = colorSlider.getColorPoints();
		if (colorPoints.isEmpty()) {
			g2.setColor(Color.WHITE);
			g2.fillRect(x, y, width, height);

			g2.setColor(Colors.TEXTFIELD_BORDER);
			g2.drawRect(x, y, width, height);

			g2.dispose();
			return;
		} else if (colorPoints.size() == 1) {
			Color c1 = colorPoints.get(0).getColor();
			g2.setColor(c1 != null ? c1 : Color.WHITE);
			g2.fillRect(x, y, width, height);

			g2.setColor(Colors.TEXTFIELD_BORDER);
			g2.drawRect(x, y, width, height);

			g2.dispose();
			return;
		}

		int pixel = ColorSlider.X_OFFSET;
		boolean isFirst;
		boolean isLast;
		for (int i = 0; i < colorPoints.size() - 1; i++) {
			ColorPoint p1 = colorPoints.get(i);
			ColorPoint p2 = colorPoints.get(i + 1);
			Color c1 = p1.getColor();
			Color c2 = p2.getColor();
			double rangeWidth = (p2.getPoint() - p1.getPoint()) * width;
			int pixelS1 = (int) Math.floor(p1.getPoint() * width) + ColorSlider.X_OFFSET;
			isFirst = i == 0;
			isLast = i == colorPoints.size() - 2;
			if (c1 == null) {
				c1 = Color.WHITE;
			}
			if (c2 == null) {
				c2 = Color.WHITE;
			}

			// anything before first stop is painted in color of first stop
			g2.setColor(c1);
			while (isFirst && pixel < pixelS1) {
				g2.drawLine(pixel, y, pixel, height);
				pixel++;
			}

			// now everything between stops
			int lastRangePixel = (int) Math.floor(rangeWidth);
			for (int relativePixel = 0; relativePixel < lastRangePixel; relativePixel++, pixel++) {
				int rDiff = c1.getRed() - c2.getRed();
				int gDiff = c1.getGreen() - c2.getGreen();
				int bDiff = c1.getBlue() - c2.getBlue();
				int aDiff = c1.getAlpha() - c2.getAlpha();

				int red = (int) (c1.getRed() - (rDiff / (rangeWidth + 1)) * relativePixel);
				int green = (int) (c1.getGreen() - (gDiff / (rangeWidth + 1)) * relativePixel);
				int blue = (int) (c1.getBlue() - (bDiff / (rangeWidth + 1)) * relativePixel);
				int alpha = (int) (c1.getAlpha() - (aDiff / (rangeWidth + 1)) * relativePixel);

				g2.setColor(new Color(red, green, blue, alpha));
				g2.drawLine(pixel, y, pixel, height);
			}

			// if last stop is < 1.0 (aka does no go until the end), fill with last color
			g2.setColor(c2);
			while (isLast && pixel < colorSlider.getWidth() - ColorSlider.X_OFFSET) {
				g2.drawLine(pixel, y, pixel, height);
				pixel++;
			}
		}

		g2.setColor(Colors.TEXTFIELD_BORDER);
		g2.drawRect(x, y, width, height);

		g2.dispose();
	}

	public static ComponentUI createUI(JComponent c) {
		return new LinearGradientColorSliderUI();
	}
}
