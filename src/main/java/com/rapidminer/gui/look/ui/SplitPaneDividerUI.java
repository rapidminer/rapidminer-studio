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

import java.awt.Color;
import java.awt.Cursor;
import java.awt.Dimension;
import java.awt.Graphics;
import java.awt.Insets;

import javax.swing.JButton;
import javax.swing.JSplitPane;
import javax.swing.border.Border;
import javax.swing.plaf.ColorUIResource;
import javax.swing.plaf.basic.BasicButtonUI;
import javax.swing.plaf.basic.BasicSplitPaneDivider;
import javax.swing.plaf.basic.BasicSplitPaneUI;


/**
 * The UI for split pane dividers.
 * 
 * 
 * @author Ingo Mierswa
 */
public final class SplitPaneDividerUI extends BasicSplitPaneDivider {

	private static final long serialVersionUID = 1003893412018250065L;

	private boolean horizontal;

	public SplitPaneDividerUI(BasicSplitPaneUI ui) {
		super(ui);
	}

	@Override
	public Dimension getPreferredSize() {
		if (this.orientation == JSplitPane.HORIZONTAL_SPLIT) {
			return new Dimension(getDividerSize(), 9);
		}
		return new Dimension(9, getDividerSize());
	}

	@Override
	public void paint(Graphics g) {

		if (this.orientation == JSplitPane.HORIZONTAL_SPLIT) {
			this.horizontal = true;
		} else {
			this.horizontal = false;
		}

		int w = this.getWidth();
		int h = this.getHeight();

		if (!this.horizontal) {
			g.setColor(new ColorUIResource(249, 249, 249));
			g.drawLine(0, 0, w, 0);
			g.setColor(new ColorUIResource(245, 245, 245));
			g.drawLine(0, 1, w, 1);
			g.setColor(new ColorUIResource(242, 242, 242));
			g.drawLine(0, 2, w, 2);

			g.setColor(new ColorUIResource(238, 238, 238));
			g.fillRect(0, 3, w, h - 8);
			g.setColor(new ColorUIResource(234, 234, 234));
			g.drawLine(0, h - 5, w, h - 5);
			g.setColor(new ColorUIResource(229, 229, 229));
			g.drawLine(0, h - 4, w, h - 4);
			g.setColor(new ColorUIResource(227, 227, 227));
			g.drawLine(0, h - 3, w, h - 3);
			g.setColor(new ColorUIResource(225, 225, 225));
			g.drawLine(0, h - 2, w, h - 2);
			g.setColor(new ColorUIResource(221, 221, 221));
			g.drawLine(0, h - 1, w, h - 1);
		} else {
			g.setColor(new ColorUIResource(120, 120, 120));
			g.drawLine(0, 0, 0, h);
			g.setColor(new ColorUIResource(253, 253, 253));
			g.drawLine(1, 0, 1, h);
			g.setColor(new ColorUIResource(243, 243, 243));
			g.drawLine(2, 0, 2, h);
			g.setColor(new ColorUIResource(238, 238, 238));
			g.fillRect(3, 0, w - 8, h);
			g.setColor(new ColorUIResource(234, 234, 234));
			g.drawLine(w - 5, 0, w - 5, h);
			g.setColor(new ColorUIResource(229, 229, 229));
			g.drawLine(w - 4, 0, w - 4, h);
			g.setColor(new ColorUIResource(227, 227, 227));
			g.drawLine(w - 3, 0, w - 3, h);
			g.setColor(new ColorUIResource(225, 225, 225));
			g.drawLine(w - 2, 0, w - 2, h);
			g.setColor(new ColorUIResource(221, 221, 221));
			g.drawLine(w - 1, 0, w - 1, h);
		}
		super.paint(g);
	}

	@Override
	protected JButton createRightOneTouchButton() {
		JButton b = new JButton() {

			private static final long serialVersionUID = -4417470163618451783L;

			@Override
			public void setBorder(Border border) {}

			@Override
			public void paint(Graphics g) {
				int w = this.getWidth();
				int h = this.getHeight();

				g.setColor(new ColorUIResource(235, 235, 235));
				g.fillRect(0, 0, w, h);

				g.setColor(new ColorUIResource(100, 100, 100));

				if (SplitPaneDividerUI.this.horizontal) {
					int picW = w - 2;
					int baseX = 2;
					int baseY = 2;

					int[] xArr = new int[] { baseX, baseX + picW, baseX };
					int[] yArr = new int[] { baseY, baseY + picW, baseY + 2 * picW };
					g.fillPolygon(xArr, yArr, 3);
				} else {
					int picH = h - 3;
					int baseX = 2;
					int baseY = 2;

					int[] xArr = new int[] { baseX, baseX + 2 * picH + 1, baseX + picH };
					int[] yArr = new int[] { baseY, baseY, baseY + picH + 1 };

					g.fillPolygon(xArr, yArr, 3);
				}
			}

			@Override
			public Border getBorder() {
				return null;
			}

			@Override
			public Insets getInsets() {
				return new Insets(0, 0, 0, 0);
			}

			@Override
			public Insets getMargin() {
				return new Insets(0, 0, 0, 0);
			}

		};
		b.setCursor(Cursor.getPredefinedCursor(Cursor.HAND_CURSOR));
		b.setFocusPainted(false);
		b.setRolloverEnabled(false);
		b.setBorderPainted(false);
		b.setRequestFocusEnabled(false);

		return b;
	}

	protected JButton createLeftOneTouchButton1() {
		JButton b = new JButton() {

			private static final long serialVersionUID = 8292762190701962839L;

			@Override
			public void paint(Graphics g) {
				g.setColor(Color.white);
				g.fillRect(this.getX(), this.getY(), this.getWidth(), this.getHeight());
			}
		};
		return b;
	}

	@Override
	protected JButton createLeftOneTouchButton() {
		JButton b = new JButton() {

			private static final long serialVersionUID = -3146624146067334027L;

			@Override
			public void setBorder(Border border) {}

			@Override
			public void updateUI() {
				setUI(new BasicButtonUI());
			}

			@Override
			public void paint(Graphics g) {
				int w = this.getWidth();
				int h = this.getHeight();

				g.setColor(new ColorUIResource(235, 235, 235));
				g.fillRect(0, 0, w, h);
				g.setColor(new ColorUIResource(100, 100, 100));

				if (SplitPaneDividerUI.this.horizontal) {
					int picW = w - 2;
					int baseX = 2;
					int baseY = 2;

					int[] xArr = new int[] { baseX + picW, baseX, baseX + picW };
					int[] yArr = new int[] { baseY, baseY + picW, baseY + 2 * picW };

					g.fillPolygon(xArr, yArr, 3);
				} else {
					int picH = h - 1;
					int baseX = 2;
					int baseY = 2;

					int[] xArr = new int[] { baseX, baseX + 2 * picH + 1, baseX + picH };
					int[] yArr = new int[] { baseY + picH, baseY + picH, baseY - 1 };

					g.fillPolygon(xArr, yArr, 3);
				}
			}

			@Override
			public Border getBorder() {
				return null;
			}

			@Override
			public Insets getInsets() {
				return new Insets(0, 0, 0, 0);
			}

			@Override
			public Insets getMargin() {
				return new Insets(0, 0, 0, 0);
			}

		};
		b.setCursor(Cursor.getPredefinedCursor(Cursor.HAND_CURSOR));
		b.setFocusPainted(false);
		b.setRolloverEnabled(false);
		b.setBorderPainted(false);
		b.setRequestFocusEnabled(false);
		return b;
	}
}
