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

import java.awt.Cursor;
import java.awt.Dimension;
import java.awt.Graphics;
import java.awt.Graphics2D;
import java.awt.Insets;

import javax.swing.JButton;
import javax.swing.JSplitPane;
import javax.swing.border.Border;
import javax.swing.plaf.ColorUIResource;
import javax.swing.plaf.basic.BasicButtonUI;
import javax.swing.plaf.basic.BasicSplitPaneDivider;
import javax.swing.plaf.basic.BasicSplitPaneUI;

import com.rapidminer.gui.look.Colors;


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
			return new Dimension(getDividerSize(), 5);
		} else {
			return new Dimension(5, getDividerSize());
		}
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

		Graphics2D g2 = (Graphics2D) g.create();

		if (this.horizontal) {
			// left + right
			g2.setColor(Colors.SPLITPANE_BORDER);
			g2.fillRect(0, 0, 5, h);

			g2.setColor(Colors.SPLITPANE_DOTS);
			g2.fillRect(1, h / 2 - 12, 1, 24);
			g2.fillRect(w - 2, h / 2 - 12, 1, 24);
		} else {
			// top + bottom
			g2.setColor(Colors.SPLITPANE_BORDER);
			g2.fillRect(0, 0, w, 5);

			g2.setColor(Colors.SPLITPANE_DOTS);
			g2.fillRect(w / 2 - 12, 1, 24, 1);
			g2.fillRect(w / 2 - 12, h - 2, 24, 1);
		}

		g2.dispose();

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

		// return b;
		return null;
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
		// return b;
		return null;
	}
}
