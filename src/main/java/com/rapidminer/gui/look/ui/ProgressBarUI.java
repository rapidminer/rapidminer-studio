/**
 * Copyright (C) 2001-2015 by RapidMiner and the contributors
 *
 * Complete list of developers available at our web site:
 *
 *      http://rapidminer.com
 *
 * This program is free software: you can redistribute it and/or modify
 * it under the terms of the GNU Affero General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU Affero General Public License for more details.
 *
 * You should have received a copy of the GNU Affero General Public License
 * along with this program.  If not, see http://www.gnu.org/licenses/.
 */
package com.rapidminer.gui.look.ui;

import com.rapidminer.gui.look.RapidLookTools;
import com.rapidminer.gui.look.painters.CashedPainter;

import java.awt.Color;
import java.awt.FontMetrics;
import java.awt.Graphics;
import java.awt.Rectangle;
import java.awt.geom.AffineTransform;

import javax.swing.JComponent;
import javax.swing.SwingConstants;
import javax.swing.UIManager;
import javax.swing.plaf.ComponentUI;
import javax.swing.plaf.basic.BasicProgressBarUI;


/**
 * The UI for progress bars.
 * 
 * @author Ingo Mierswa
 */
public class ProgressBarUI extends BasicProgressBarUI {

	private Rectangle bouncingBox;

	public static ComponentUI createUI(JComponent x) {
		return new ProgressBarUI();
	}

	@Override
	protected void installDefaults() {
		super.installDefaults();
	}

	@Override
	protected void paintDeterminate(Graphics g, JComponent c) {
		boolean vertical = (this.progressBar.getOrientation() == SwingConstants.VERTICAL);
		int w = c.getWidth();
		int h = c.getHeight();
		int amountFull = getAmountFull(this.progressBar.getInsets(), w, h);

		g.setColor((Color) UIManager.get("ProgressBar.background"));
		g.fillRect(1, 1, w - 2, h - 2);

		int x;
		int y;
		int width;
		int height;
		if (vertical) {
			x = 3;
			y = 4;
			width = w - 6;
			height = h - 9;
		} else {
			x = 4;
			y = 3;
			width = w - 9;
			height = h - 6;
		}

		int amount = (amountFull / 10);

		for (int i = 0; i < amount; i++) {
			if (vertical) {
				int newY = h - i * 10 - 12;
				drawVerticalBlock(g, x, newY, width, height);
			} else {
				int newX = x - 1 + i * 10;
				drawHorizontalBlock(g, newX, y, width, height);
			}
		}

		drawString(g, vertical, w, h);
	}

	private void drawVerticalBlock(Graphics g, int x, int y, int w, int h) {
		g.setColor(RapidLookTools.getColors().getProgressBarColors()[0][0]);
		g.drawLine(x, y + 1, x, y + 7);
		g.setColor(RapidLookTools.getColors().getProgressBarColors()[0][1]);
		g.drawLine(x + 1, y + 1, x + 1, y + 7);
		g.setColor(RapidLookTools.getColors().getProgressBarColors()[0][2]);
		g.drawLine(x + 1, y, x + 1, y + 8);
		g.setColor(RapidLookTools.getColors().getProgressBarColors()[0][3]);
		g.drawLine(x + 2, y, x + 2, y + 8);
		g.setColor(RapidLookTools.getColors().getProgressBarColors()[0][4]);
		g.drawLine(x + 2, y + 1, x + 2, y + 7);

		if (w > 0) {
			g.setColor(RapidLookTools.getColors().getProgressBarColors()[0][5]);
			g.drawLine(x + 3, y, x + w - 4, y);
			g.setColor(RapidLookTools.getColors().getProgressBarColors()[0][6]);
			g.fillRect(x + 3, y + 1, w - 6, 7);
			g.setColor(RapidLookTools.getColors().getProgressBarColors()[0][7]);
			g.drawLine(x + 3, y + 8, x + w - 4, y + 8);
		}

		g.setColor(RapidLookTools.getColors().getProgressBarColors()[0][8]);
		g.drawLine(x + w - 3, y, x + w - 3, y + 8);
		g.setColor(RapidLookTools.getColors().getProgressBarColors()[0][9]);
		g.drawLine(x + w - 3, y + 1, x + w - 3, y + 7);
		g.setColor(RapidLookTools.getColors().getProgressBarColors()[0][10]);
		g.drawLine(x + w - 2, y, x + w - 2, y + 8);
		g.setColor(RapidLookTools.getColors().getProgressBarColors()[0][11]);
		g.drawLine(x + w - 2, y + 1, x + w - 2, y + 7);
		g.setColor(RapidLookTools.getColors().getProgressBarColors()[0][12]);
		g.drawLine(x + w - 1, y + 1, x + w - 1, y + 7);
	}

	private void drawHorizontalBlock(Graphics g, int x, int y, int w, int h) {
		g.setColor(RapidLookTools.getColors().getProgressBarColors()[0][0]);
		g.drawLine(x + 1, y, x + 7, y);
		g.setColor(RapidLookTools.getColors().getProgressBarColors()[0][2]);
		g.drawLine(x, y + 1, x + 8, y + 1);
		g.setColor(RapidLookTools.getColors().getProgressBarColors()[0][1]);
		g.drawLine(x + 1, y + 1, x + 7, y + 1);
		g.setColor(RapidLookTools.getColors().getProgressBarColors()[0][2]);
		g.drawLine(x, y + 2, x + 8, y + 2);
		g.setColor(RapidLookTools.getColors().getProgressBarColors()[0][3]);
		g.drawLine(x + 1, y + 2, x + 7, y + 2);

		if (h > 0) {
			g.setColor(RapidLookTools.getColors().getProgressBarColors()[0][7]);
			g.drawLine(x, y + 3, x, y + h - 4);
			g.setColor(RapidLookTools.getColors().getProgressBarColors()[0][6]);
			g.fillRect(x + 1, y + 3, 7, h - 6);
			g.setColor(RapidLookTools.getColors().getProgressBarColors()[0][5]);
			g.drawLine(x + 8, y + 3, x + 8, y + h - 4);
		}

		g.setColor(RapidLookTools.getColors().getProgressBarColors()[0][8]);
		g.drawLine(x, y + h - 3, x + 8, y + h - 3);
		g.setColor(RapidLookTools.getColors().getProgressBarColors()[0][9]);
		g.drawLine(x + 1, y + h - 3, x + 7, y + h - 3);
		g.setColor(RapidLookTools.getColors().getProgressBarColors()[0][10]);
		g.drawLine(x, y + h - 2, x + 8, y + h - 2);
		g.setColor(RapidLookTools.getColors().getProgressBarColors()[0][11]);
		g.drawLine(x + 1, y + h - 2, x + 7, y + h - 2);
		g.setColor(RapidLookTools.getColors().getProgressBarColors()[0][12]);
		g.drawLine(x + 1, y + h - 1, x + 7, y + h - 1);
	}

	private void drawString(Graphics g, boolean vertical, int w, int h) {
		if (this.progressBar.isStringPainted()) {
			FontMetrics fontSizer = this.progressBar.getFontMetrics(this.progressBar.getFont());
			int stringWidth = fontSizer.stringWidth(this.progressBar.getString());
			int stringHeight = fontSizer.getHeight();

			if (!vertical) {
				g.setColor(new Color(220, 220, 220, 140));
				g.fillRoundRect((w - stringWidth) / 2 - 5, (h - stringHeight) / 2 - 2, stringWidth + 10, stringHeight + 3,
						12, 6);
				g.setColor((Color) UIManager.get("ProgressBar.foreground"));
				g.drawString(this.progressBar.getString(), (w - stringWidth) / 2, (h + stringHeight) / 2 - 3);
			} else {
				g.setColor(new Color(220, 220, 220, 100));
				g.fillRoundRect((w - stringHeight) / 2 - 2, (h - stringWidth) / 2 - 5, stringHeight + 3, stringWidth + 10,
						6, 12);
				AffineTransform rotate = AffineTransform.getRotateInstance(Math.PI / 2);
				g.setFont(this.progressBar.getFont().deriveFont(rotate));
				g.setColor((Color) UIManager.get("ProgressBar.foreground"));
				g.drawString(this.progressBar.getString(), (w - stringHeight) / 2 + 4, (h - stringWidth) / 2 + 2);
			}
		}
	}

	@Override
	protected void paintIndeterminate(Graphics g, JComponent c) {
		this.bouncingBox = getBox(this.bouncingBox);
		boolean vertical = (this.progressBar.getOrientation() == SwingConstants.VERTICAL);
		int w = c.getWidth();
		int h = c.getHeight();
		if (this.bouncingBox != null) {
			CashedPainter.drawProgressBar(c, g, vertical, true, (int) this.bouncingBox.getX(),
					(int) this.bouncingBox.getY(), (int) this.bouncingBox.getWidth(), (int) this.bouncingBox.getHeight());
		}
		drawString(g, vertical, w, h);
	}
}
