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

import com.rapidminer.gui.look.GenericArrowButton;
import com.rapidminer.gui.look.RapidLookTools;

import java.awt.Adjustable;
import java.awt.Color;
import java.awt.Dimension;
import java.awt.Graphics;
import java.awt.Rectangle;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;
import java.awt.event.MouseListener;

import javax.swing.JButton;
import javax.swing.JComponent;
import javax.swing.JScrollBar;
import javax.swing.UIManager;
import javax.swing.plaf.ComponentUI;
import javax.swing.plaf.basic.BasicScrollBarUI;


/**
 * The UI for scroll bars.
 * 
 * @author Ingo Mierswa
 */
public class ScrollBarUI extends BasicScrollBarUI {

	private class ScrollBarThumbListener extends MouseAdapter {

		@Override
		public void mousePressed(MouseEvent e) {
			if (getThumbBounds().contains(e.getX(), e.getY())) {
				ScrollBarUI.this.thumbIsPressed = true;
				getScrollBar().repaint();
			}
		}

		@Override
		public void mouseReleased(MouseEvent e) {
			ScrollBarUI.this.thumbIsPressed = false;
			getScrollBar().repaint();
		}
	}

	private JButton decreaseButton;

	private JButton increaseButton;

	private boolean thumbIsPressed;

	private MouseListener thumbPressedListener;

	public static ComponentUI createUI(JComponent c) {
		return new ScrollBarUI();
	}

	@Override
	protected void installDefaults() {
		super.installDefaults();
	}

	@Override
	public void uninstallUI(JComponent c) {
		super.uninstallUI(c);
	}

	protected MouseListener createThumbPressedListener() {
		return new ScrollBarThumbListener();
	}

	@Override
	protected void installListeners() {
		super.installListeners();
		if ((this.thumbPressedListener = createThumbPressedListener()) != null) {
			this.scrollbar.addMouseListener(this.thumbPressedListener);
		}
	}

	@Override
	protected void uninstallListeners() {
		if (this.thumbPressedListener != null) {
			this.scrollbar.removeMouseListener(this.thumbPressedListener);
			this.thumbPressedListener = null;
		}
		super.uninstallListeners();
	}

	@Override
	public Dimension getPreferredSize(JComponent c) {
		if (this.scrollbar.getOrientation() == Adjustable.VERTICAL) {
			return new Dimension(18, 53 + 10);
		} else {
			return new Dimension(100, 18);
		}
	}

	@Override
	public Dimension getMinimumSize(JComponent c) {
		if (this.scrollbar.getOrientation() == Adjustable.VERTICAL) {
			return new Dimension(18, 40);
		} else {
			return new Dimension(40, 18);
		}
	}

	@Override
	protected void configureScrollBarColors() {
		super.configureScrollBarColors();
		this.thumbColor = UIManager.getColor("ScrollBar.thumb");
		this.thumbHighlightColor = UIManager.getColor("ScrollBar.thumbHighlight");
	}

	@Override
	protected void paintTrack(Graphics g, JComponent c, Rectangle trackBounds) {
		int x = (int) trackBounds.getX();
		int y = (int) trackBounds.getY();
		int w = (int) trackBounds.getWidth();
		int h = (int) trackBounds.getHeight();

		if (this.scrollbar.getOrientation() == Adjustable.HORIZONTAL) {
			g.setColor(RapidLookTools.getColors().getScrollBarColors()[0][0]);
			g.drawLine(x, 0, x + w - 1, 0);
			g.setColor(RapidLookTools.getColors().getScrollBarColors()[0][1]);
			g.drawLine(x, 1, x + w - 1, 1);
			g.setColor(RapidLookTools.getColors().getScrollBarColors()[0][2]);
			g.drawLine(x, 2, x + w - 1, 2);
			g.setColor(RapidLookTools.getColors().getScrollBarColors()[0][3]);
			g.fillRect(x, 3, w, h - 6);
			g.setColor(RapidLookTools.getColors().getScrollBarColors()[0][2]);
			g.drawLine(x, h - 3, x + w - 1, h - 3);
			g.setColor(RapidLookTools.getColors().getScrollBarColors()[0][1]);
			g.drawLine(x, h - 2, x + w - 1, h - 2);
			g.setColor(RapidLookTools.getColors().getScrollBarColors()[0][0]);
			g.drawLine(x, h - 1, x + w - 1, h - 1);
		} else {
			g.setColor(RapidLookTools.getColors().getScrollBarColors()[0][0]);
			g.drawLine(0, y, 0, y + h - 1);
			g.setColor(RapidLookTools.getColors().getScrollBarColors()[0][1]);
			g.drawLine(1, y, 1, y + h - 1);
			g.setColor(RapidLookTools.getColors().getScrollBarColors()[0][2]);
			g.drawLine(2, y, 2, y + h - 1);
			g.setColor(RapidLookTools.getColors().getScrollBarColors()[0][3]);
			g.fillRect(3, y, w - 6, h);
			g.setColor(RapidLookTools.getColors().getScrollBarColors()[0][2]);
			g.drawLine(w - 3, y, w - 3, h + y - 1);
			g.setColor(RapidLookTools.getColors().getScrollBarColors()[0][1]);
			g.drawLine(w - 2, y, w - 2, h + y - 1);
			g.setColor(RapidLookTools.getColors().getScrollBarColors()[0][0]);
			g.drawLine(w - 1, y, w - 1, h + y - 1);
		}
	}

	@Override
	protected JButton createDecreaseButton(int orientation) {
		if (this.scrollbar.getOrientation() == Adjustable.HORIZONTAL) {
			this.decreaseButton = new GenericArrowButton(orientation, this.scrollbar.getHeight(), 17);
		} else {
			this.decreaseButton = new GenericArrowButton(orientation, 17, this.scrollbar.getWidth());
		}
		return this.decreaseButton;
	}

	@Override
	protected JButton createIncreaseButton(int orientation) {
		if (this.scrollbar.getOrientation() == Adjustable.HORIZONTAL) {
			this.increaseButton = new GenericArrowButton(orientation, this.scrollbar.getHeight(), 17);
		} else {
			this.increaseButton = new GenericArrowButton(orientation, 17, this.scrollbar.getWidth());
		}
		return this.increaseButton;
	}

	@Override
	protected void paintIncreaseHighlight(Graphics g) {
		g.setColor(Color.green);
	}

	@Override
	protected void paintThumb(Graphics g, JComponent c, Rectangle thumbBounds) {
		int x = (int) thumbBounds.getX();
		int y = (int) thumbBounds.getY();
		int w = (int) thumbBounds.getWidth();
		int h = (int) thumbBounds.getHeight();

		if (c.isEnabled() && (w > 0) && (h > 0)) {
			if (this.scrollbar.getOrientation() == Adjustable.HORIZONTAL) {
				h -= 2;
				y++;
				drawHorizThumb(g, x, y, w, h);
			} else {
				w -= 2;
				x++;
				drawVertThumb(g, x, y, w, h);
			}
		}
	}

	private void drawHorizThumb(Graphics g, int x, int y, int w, int h) {
		if (!this.thumbIsPressed) {
			h -= 2;
			y++;
			g.setColor(RapidLookTools.getColors().getScrollBarColors()[1][1]);
			g.drawLine(1 + x, y + 1, x + w - 2, y + 1);
			g.setColor(RapidLookTools.getColors().getScrollBarColors()[1][2]);
			g.drawLine(1 + x, y + 2, x + w - 2, y + 2);
			g.setColor(RapidLookTools.getColors().getScrollBarColors()[1][3]);
			g.drawLine(1 + x, y + 3, x + w - 2, y + 3);
			g.setColor(RapidLookTools.getColors().getScrollBarColors()[1][4]);
			g.drawLine(1 + x, y + 4, x + w - 2, y + 4);
			g.setColor(RapidLookTools.getColors().getScrollBarColors()[1][5]);
			g.fillRect(1 + x, y + 5, w - 2, h - 6);
		} else {
			g.setColor(RapidLookTools.getColors().getScrollBarColors()[1][7]);
			g.drawLine(1 + x, y + 1, x + w - 2, y + 1); // top
			g.drawLine(1 + x, y + 2, x + w - 2, y + 2);
			g.drawLine(1 + x, y + h - 2, x + w - 2, y + h - 2); // down
			g.drawLine(1 + x, y + h - 3, x + w - 2, y + h - 3);
			g.setColor(RapidLookTools.getColors().getScrollBarColors()[1][8]);
			g.drawLine(1 + x, y + 3, x + w - 2, y + 3); // top
			g.drawLine(1 + x, y + h - 4, x + w - 2, y + h - 4); // down
			g.setColor(RapidLookTools.getColors().getScrollBarColors()[1][9]);
			g.drawLine(1 + x, y + 4, x + w - 2, y + 4); // top
			g.drawLine(1 + x, y + h - 5, x + w - 2, y + h - 5); // down
			g.fillRect(1 + x, y + 5, w - 2, h - 10);
		}

		g.setColor(RapidLookTools.getColors().getScrollBarColors()[1][10]);
		g.drawLine(2 + x, y + h - 1, x + w - 3, y + h - 1); // down
		g.drawLine(x, y + 2, x, y + h - 3);
		g.drawLine(x + w - 1, y + 2, x + w - 1, y + h - 3);
		g.setColor(RapidLookTools.getColors().getScrollBarColors()[1][11]);
		g.drawLine(x, y + 1, x + 1, y);
		g.drawLine(x, y + h - 2, x + 1, y + h - 1);
		g.drawLine(x + w - 1, y + h - 2, x + w - 2, y + h - 1);
		g.drawLine(x + w - 1, y + 1, x + w - 2, y);
		g.drawLine(2 + x, y, x + w - 3, y);

		g.setColor(RapidLookTools.getColors().getScrollBarColors()[1][12]);
		g.drawLine(x, y, x, y);
		g.drawLine(x, y + h - 1, x, y + h - 1);
		g.drawLine(x + w - 1, y + h - 1, x + w - 1, y + h - 1);
		g.drawLine(x + w - 1, y, x + w - 1, y);

		int xBase = x + w / 2 - 11;
		int yBase = y + h / 2 - 4;

		if (w > 22) {
			g.setColor(RapidLookTools.getColors().getScrollBarColors()[1][13]);
			g.drawLine(xBase, yBase, xBase, yBase);
			g.drawLine(xBase + 6, yBase, xBase + 6, yBase);
			g.drawLine(xBase + 12, yBase, xBase + 12, yBase);
			g.drawLine(xBase + 18, yBase, xBase + 18, yBase);
			g.drawLine(xBase + 3, yBase + 3, xBase + 3, yBase + 3);
			g.drawLine(xBase + 9, yBase + 3, xBase + 9, yBase + 3);
			g.drawLine(xBase + 15, yBase + 3, xBase + 15, yBase + 3);
			g.drawLine(xBase, yBase + 6, xBase, yBase + 6);
			g.drawLine(xBase + 6, yBase + 6, xBase + 6, yBase + 6);
			g.drawLine(xBase + 12, yBase + 6, xBase + 12, yBase + 6);
			g.drawLine(xBase + 18, yBase + 6, xBase + 18, yBase + 6);

			xBase++;
			yBase++;

			g.setColor(Color.white);
			g.drawLine(xBase, yBase, xBase, yBase);
			g.drawLine(xBase + 6, yBase, xBase + 6, yBase);
			g.drawLine(xBase + 12, yBase, xBase + 12, yBase);
			g.drawLine(xBase + 18, yBase, xBase + 18, yBase);
			g.drawLine(xBase + 3, yBase + 3, xBase + 3, yBase + 3);
			g.drawLine(xBase + 9, yBase + 3, xBase + 9, yBase + 3);
			g.drawLine(xBase + 15, yBase + 3, xBase + 15, yBase + 3);
			g.drawLine(xBase, yBase + 6, xBase, yBase + 6);
			g.drawLine(xBase + 6, yBase + 6, xBase + 6, yBase + 6);
			g.drawLine(xBase + 12, yBase + 6, xBase + 12, yBase + 6);
			g.drawLine(xBase + 18, yBase + 6, xBase + 18, yBase + 6);
		}
	}

	private void drawVertThumb(Graphics g, int x, int y, int w, int h) {
		if (!this.thumbIsPressed) {
			w -= 2;
			x++;

			g.setColor(RapidLookTools.getColors().getScrollBarColors()[1][1]);
			g.drawLine(1 + x, y + 1, x + 1, y + h - 2); // top
			g.setColor(RapidLookTools.getColors().getScrollBarColors()[1][2]);
			g.drawLine(2 + x, y + 1, x + 2, y + h - 2);
			g.setColor(RapidLookTools.getColors().getScrollBarColors()[1][3]);
			g.drawLine(x + 3, y + 1, x + 3, y + h - 2); // top
			g.setColor(RapidLookTools.getColors().getScrollBarColors()[1][4]);
			g.drawLine(4 + x, y + 1, x + 4, y + h - 2); // top
			g.setColor(RapidLookTools.getColors().getScrollBarColors()[1][5]);
			g.fillRect(x + 5, y + 1, w - 6, h - 2);
		} else {
			g.setColor(RapidLookTools.getColors().getScrollBarColors()[1][6]);
			g.fillRect(x + 1, y + 1, w - 2, h - 2);
			g.setColor(RapidLookTools.getColors().getScrollBarColors()[1][7]);
			g.drawLine(1 + x, y + 1, x + 1, y + h - 2); // top
			g.drawLine(2 + x, y + 1, x + 2, y + h - 2);
			g.drawLine(x + w - 2, y + 1, x + w - 2, y + h - 2); // down
			g.drawLine(x + w - 3, y + 1, x + w - 3, y + h - 2);
			g.setColor(RapidLookTools.getColors().getScrollBarColors()[1][8]);
			g.drawLine(3 + x, y + 1, x + 3, y + h - 2); // top
			g.drawLine(x + w - 4, y + 1, x + w - 4, y + h - 2); // down
			g.setColor(RapidLookTools.getColors().getScrollBarColors()[1][9]);
			g.drawLine(4 + x, y + 1, x + 4, y + h - 2); // top
			g.drawLine(x + w - 5, y + 1, x + w - 5, y + h - 2); // down
			g.fillRect(5 + x, y + 1, w - 10, h - 2);
		}

		g.setColor(RapidLookTools.getColors().getScrollBarColors()[1][10]);
		g.drawLine(x + w - 1, y + 2, x + w - 1, y + h - 3); // down
		g.drawLine(x + 2, y, x + w - 3, y);
		g.drawLine(x + 2, y + h - 1, x + w - 3, y + h - 1);
		g.setColor(RapidLookTools.getColors().getScrollBarColors()[1][11]);
		g.drawLine(x + 1, y, x, y + 1);
		g.drawLine(x + w - 2, y, x + w - 1, y + 1);
		g.drawLine(x + w - 2, y + h - 1, x + w - 1, y + h - 2);
		g.drawLine(x + 1, y + h - 1, x, y + h - 2);
		g.drawLine(x, y + 2, x, y + h - 3); // top
		g.setColor(RapidLookTools.getColors().getScrollBarColors()[1][12]);
		g.drawLine(x, y, x, y);
		g.drawLine(x + w - 1, y, x + w - 1, y);
		g.drawLine(x + w - 1, y + h - 1, x + w - 1, y + h - 1);
		g.drawLine(x, y + h - 1, x, y + h - 1);

		if (h > 22) {
			int xBase = x + w / 2 - 4;
			int yBase = y + h / 2 - 10;
			yBase++;
			g.setColor(RapidLookTools.getColors().getScrollBarColors()[1][13]);
			g.drawLine(xBase, yBase, xBase, yBase);
			g.drawLine(xBase + 6, yBase, xBase + 6, yBase);
			g.drawLine(xBase + 3, yBase + 3, xBase + 3, yBase + 3);
			g.drawLine(xBase, yBase + 6, xBase, yBase + 6);
			g.drawLine(xBase + 6, yBase + 6, xBase + 6, yBase + 6);
			g.drawLine(xBase + 3, yBase + 9, xBase + 3, yBase + 9);
			g.drawLine(xBase, yBase + 12, xBase, yBase + 12);
			g.drawLine(xBase + 6, yBase + 12, xBase + 6, yBase + 12);
			g.drawLine(xBase + 3, yBase + 15, xBase + 3, yBase + 15);
			g.drawLine(xBase, yBase + 18, xBase, yBase + 18);
			g.drawLine(xBase + 6, yBase + 18, xBase + 6, yBase + 18);

			xBase++;
			yBase--;
			g.setColor(Color.white);
			g.drawLine(xBase, yBase, xBase, yBase);
			g.drawLine(xBase + 6, yBase, xBase + 6, yBase);
			g.drawLine(xBase + 3, yBase + 3, xBase + 3, yBase + 3);
			g.drawLine(xBase, yBase + 6, xBase, yBase + 6);
			g.drawLine(xBase + 6, yBase + 6, xBase + 6, yBase + 6);
			g.drawLine(xBase + 3, yBase + 9, xBase + 3, yBase + 9);
			g.drawLine(xBase, yBase + 12, xBase, yBase + 12);
			g.drawLine(xBase + 6, yBase + 12, xBase + 6, yBase + 12);
			g.drawLine(xBase + 3, yBase + 15, xBase + 3, yBase + 15);
			g.drawLine(xBase, yBase + 18, xBase, yBase + 18);
			g.drawLine(xBase + 6, yBase + 18, xBase + 6, yBase + 18);
		}
	}

	private JScrollBar getScrollBar() {
		return this.scrollbar;
	}
}
