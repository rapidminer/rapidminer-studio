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
package com.rapidminer.gui.look.ui;

import java.awt.FontMetrics;
import java.awt.GradientPaint;
import java.awt.Graphics;
import java.awt.Graphics2D;
import java.awt.Paint;
import java.awt.RenderingHints;
import java.awt.geom.Line2D;
import java.awt.geom.Path2D;
import java.awt.geom.RoundRectangle2D;

import javax.swing.JComponent;
import javax.swing.plaf.ComponentUI;
import javax.swing.plaf.basic.BasicProgressBarUI;

import com.rapidminer.gui.look.Colors;
import com.rapidminer.gui.look.RapidLookAndFeel;
import com.rapidminer.gui.look.RapidLookTools;


/**
 * The UI for progress bars.
 *
 * @author Marco Boeck
 */
public class ProgressBarUI extends BasicProgressBarUI {

	/** the speed factor of the indeterminate animation */
	private static final double ANIMATION_SPEED = 0.03;

	/** the length of each individual part of the intermediate animation */
	private static final int ANIMATION_BAR_LENGTH = 20;

	public static ComponentUI createUI(JComponent x) {
		return new ProgressBarUI();
	}

	@Override
	protected void installDefaults() {
		super.installDefaults();
	}

	@Override
	protected int getBoxLength(int availableLength, int otherDimension) {
		return availableLength;
	}

	@Override
	protected void paintDeterminate(Graphics g, JComponent c) {
		boolean compressed = Boolean.parseBoolean(String.valueOf(progressBar
				.getClientProperty(RapidLookTools.PROPERTY_PROGRESSBAR_COMPRESSED)));

		int y = 0;
		int x = 0;
		int w;
		int h;
		if (compressed) {
			x = (int) (c.getWidth() * 0.67);
			w = (int) (c.getWidth() * 0.33);
			y = 3;
			h = c.getHeight() - 6;
		} else {
			w = c.getWidth();
			h = c.getHeight() / 2;
		}

		int amountFull = getAmountFull(progressBar.getInsets(), w, h);

		Graphics2D g2 = (Graphics2D) g;
		g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);

		if (c.isOpaque()) {
			if (c.getParent() != null) {
				g2.setColor(c.getParent().getBackground());
			} else {
				g2.setColor(c.getBackground());
			}
			g2.fillRect(x, y, c.getWidth(), c.getHeight());
		}

		g2.setColor(Colors.PROGRESSBAR_BACKGROUND);
		g2.fillRoundRect(x + 1, y + 1, w - 2, h - 2, RapidLookAndFeel.CORNER_DEFAULT_RADIUS,
				RapidLookAndFeel.CORNER_DEFAULT_RADIUS);

		g2.setColor(Colors.PROGRESSBAR_BORDER);
		g2.drawRoundRect(x + 1, y + 1, w - 2, h - 2, RapidLookAndFeel.CORNER_DEFAULT_RADIUS,
				RapidLookAndFeel.CORNER_DEFAULT_RADIUS);

		Paint gp = new GradientPaint(x, y + 3, Colors.PROGRESSBAR_DETERMINATE_FOREGROUND_GRADIENT_START, x, h - 5,
				Colors.PROGRESSBAR_DETERMINATE_FOREGROUND_GRADIENT_END);
		g2.setPaint(gp);
		g2.fillRoundRect(x + 3, y + 3, amountFull - 5, h - 5, RapidLookAndFeel.CORNER_DEFAULT_RADIUS / 2,
				RapidLookAndFeel.CORNER_DEFAULT_RADIUS / 2);

		drawString(g2, w, h, compressed);
	}

	@Override
	protected void paintIndeterminate(Graphics g, JComponent c) {
		boolean compressed = Boolean.parseBoolean(String.valueOf(progressBar
				.getClientProperty(RapidLookTools.PROPERTY_PROGRESSBAR_COMPRESSED)));

		int y = 0;
		int x = 0;
		int w;
		int h;
		if (compressed) {
			x = (int) (c.getWidth() * 0.67);
			w = (int) (c.getWidth() * 0.33);
			y = 3;
			h = c.getHeight() - 6;
		} else {
			w = c.getWidth();
			h = c.getHeight() / 2;
		}

		Graphics2D g2 = (Graphics2D) g;
		g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);

		if (c.isOpaque()) {
			if (c.getParent() != null) {
				g2.setColor(c.getParent().getBackground());
			} else {
				g2.setColor(c.getBackground());
			}
			g2.fillRect(x, y, c.getWidth(), c.getHeight());
		}

		g2.setColor(Colors.PROGRESSBAR_BACKGROUND);
		g2.fillRoundRect(x + 1, y + 1, w - 2, h - 2, RapidLookAndFeel.CORNER_DEFAULT_RADIUS,
				RapidLookAndFeel.CORNER_DEFAULT_RADIUS);

		g2.setColor(Colors.PROGRESSBAR_BORDER);
		g2.drawRoundRect(x + 1, y + 1, w - 2, h - 2, RapidLookAndFeel.CORNER_DEFAULT_RADIUS,
				RapidLookAndFeel.CORNER_DEFAULT_RADIUS);

		// make sure we don't draw over the boundaries
		RoundRectangle2D clipRect = new RoundRectangle2D.Double(x + 3, y + 3, w - 5, h - 5,
				RapidLookAndFeel.CORNER_DEFAULT_RADIUS / 2, RapidLookAndFeel.CORNER_DEFAULT_RADIUS / 2);
		g2.setClip(clipRect);

		for (double xCoord = x + -4 * ANIMATION_BAR_LENGTH + System.currentTimeMillis() * ANIMATION_SPEED
				% (2 * ANIMATION_BAR_LENGTH); xCoord < x + w + 2 * ANIMATION_BAR_LENGTH;) {
			g2.setColor(Colors.PROGRESSBAR_INDETERMINATE_FOREGROUND_1);
			g2.fill(createIntermediateShape(xCoord, ANIMATION_BAR_LENGTH, h));
			xCoord += ANIMATION_BAR_LENGTH;
			g2.setColor(Colors.PROGRESSBAR_INDETERMINATE_FOREGROUND_2);
			g2.fill(createIntermediateShape(xCoord, ANIMATION_BAR_LENGTH, h));
			xCoord += ANIMATION_BAR_LENGTH;
		}
		g2.setClip(null);

		drawString(g2, w, h, compressed);
	}

	private void drawString(Graphics2D g2, int w, int h, boolean compressed) {
		if (progressBar.isStringPainted()) {
			// need to reduce font size to fit available space.
			// DO NOT CALL THIS EVERY TIME AS IT'S TREMENDOUSLY EXPENSIVE!!!
			if (compressed && progressBar.getFont().getSize() != 11) {
				progressBar.setFont(progressBar.getFont().deriveFont(11f));
			}
			FontMetrics fontSizer = progressBar.getFontMetrics(progressBar.getFont());

			String displayString = progressBar.getString();
			if (displayString == null || displayString.trim().isEmpty()) {
				return;
			}

			int stringHeight = fontSizer.getHeight();
			int stringWidth = fontSizer.stringWidth(displayString);

			// if string is too wide, cut beginning off until it fits
			while (stringWidth > w * 2) {
				displayString = displayString.substring(0, (int) (displayString.length() * 0.9));
				stringWidth = fontSizer.stringWidth(displayString);
			}

			g2.setColor(Colors.TEXT_FOREGROUND);
			if (compressed) {
				g2.drawString(displayString, (int) Math.max(0, w / 0.33 - w - stringWidth - 5), h - (h - stringHeight) / 2
						- 1);
			} else {
				g2.drawString(displayString, w - stringWidth, h + stringHeight - 1);
			}
		}
	}

	/**
	 * Creates the shape for a single part of the intermediate bar.
	 *
	 * @param x
	 * @param width
	 * @param h
	 * @param verticval
	 * @return
	 */
	private Path2D createIntermediateShape(double x, double width, double h) {
		int offset = 10;

		Path2D path = new Path2D.Double();
		path.append(new Line2D.Double(x, h, x + offset, 0), true);
		path.append(new Line2D.Double(x + offset, 0, x + width + offset, 0), true);
		path.append(new Line2D.Double(x + width + offset, 0, x + width, h), true);
		path.append(new Line2D.Double(x + width, h, x, h), true);

		return path;
	}
}
