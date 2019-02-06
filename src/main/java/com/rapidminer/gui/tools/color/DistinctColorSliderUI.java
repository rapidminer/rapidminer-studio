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
import java.awt.RenderingHints;
import java.awt.geom.Rectangle2D;
import java.util.List;
import javax.swing.JComponent;
import javax.swing.plaf.ComponentUI;

import com.rapidminer.gui.look.Colors;


/**
 * The UI for a distinct color slider.
 *
 * @author Marco Boeck
 * @since 9.2.0
 */
public class DistinctColorSliderUI extends ColorSliderUI {


	public DistinctColorSliderUI() {
		drawValues = false;
	}

	@Override
	protected void drawColors(Graphics2D g2) {
		int x = ColorSlider.X_OFFSET;
		int y = 0;
		int width = colorSlider.getBarWidth();
		int height = colorSlider.getHeight() - ColorSlider.BOTTOM_OFFSET;

		List<ColorPoint> colorPoints = colorSlider.getColorPoints();
		int numberOfColors = colorPoints.size();
		if (numberOfColors == 0) {
			// indicate null color
			g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
			g2.setColor(Colors.TEXTFIELD_BORDER);
			int barWidthQuarter = width / 4 + 1;
			int barHeightHalf = height / 2;
			g2.drawLine(x, y + barHeightHalf, x + barWidthQuarter, y + height);
			g2.drawLine(x, y, x + 2 * barWidthQuarter, y +height);
			g2.drawLine(x + barWidthQuarter, y, x + 3 * barWidthQuarter, y +height);
			g2.drawLine(x + 2 * barWidthQuarter, y, x + 4 * barWidthQuarter - 1, y + height);
			g2.drawLine(x + 3 * barWidthQuarter, y, x + 4 * barWidthQuarter - 1, y + height / 2);
			g2.drawRect(x, y, width, height);

			g2.dispose();
			return;
		} else if (numberOfColors == 1) {
			Color c1 = colorPoints.get(0).getColor();
			g2.setColor(c1 != null ? c1 : Color.WHITE);
			g2.fillRect(x, y, width, height);

			g2.setColor(Colors.TEXTFIELD_BORDER);
			g2.drawRect(x, y, width, height);

			g2.dispose();
			return;
		}

		double pixel = ColorSlider.X_OFFSET;
		double colorWidth = width / (double) numberOfColors;
		int maxPixel = colorSlider.getWidth() - ColorSlider.X_OFFSET;
		for (ColorPoint p : colorPoints) {
			g2.setColor(p.getColor());
			if (pixel + colorWidth > maxPixel) {
				colorWidth = maxPixel - pixel;
			}
			g2.fill(new Rectangle2D.Double(pixel, y, colorWidth, height));
			pixel += colorWidth;
		}

		g2.setColor(Colors.TEXTFIELD_BORDER);
		g2.drawRect(x, y, width, height);

		g2.dispose();
	}

	public static ComponentUI createUI(JComponent c) {
		return new DistinctColorSliderUI();
	}
}
