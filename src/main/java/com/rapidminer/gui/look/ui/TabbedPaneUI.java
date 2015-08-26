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

import java.awt.Font;
import java.awt.FontMetrics;
import java.awt.GradientPaint;
import java.awt.Graphics;
import java.awt.Graphics2D;
import java.awt.Insets;
import java.awt.Point;
import java.awt.Polygon;
import java.awt.Rectangle;
import java.awt.RenderingHints;
import java.awt.event.MouseEvent;
import java.awt.event.MouseListener;
import java.awt.event.MouseMotionListener;

import javax.swing.JButton;
import javax.swing.JComponent;
import javax.swing.SwingConstants;
import javax.swing.plaf.ColorUIResource;
import javax.swing.plaf.ComponentUI;
import javax.swing.plaf.basic.BasicTabbedPaneUI;


/**
 * The UI for tabbed panes.
 * 
 * @author Ingo Mierswa
 */
public class TabbedPaneUI extends BasicTabbedPaneUI {

	private class TabbedPaneMouseListener implements MouseMotionListener, MouseListener {

		@Override
		public void mouseClicked(MouseEvent e) {}

		@Override
		public void mouseEntered(MouseEvent e) {
			updateMouseOver(e.getPoint());
		}

		@Override
		public void mouseExited(MouseEvent e) {
			updateMouseOver(e.getPoint());
		}

		@Override
		public void mousePressed(MouseEvent e) {
			updateMouseOver(e.getPoint());
		}

		@Override
		public void mouseReleased(MouseEvent e) {
			updateMouseOver(e.getPoint());
		}

		@Override
		public void mouseDragged(MouseEvent e) {}

		@Override
		public void mouseMoved(MouseEvent e) {
			updateMouseOver(e.getPoint());
		}
	}

	private TabbedPaneMouseListener mouseListener = new TabbedPaneMouseListener();

	private int rolloveredTabIndex = -1;

	public static ComponentUI createUI(JComponent c) {
		return new TabbedPaneUI();
	}

	@Override
	protected JButton createScrollButton(int direction) {
		if ((direction != SOUTH) && (direction != NORTH) && (direction != EAST) && (direction != WEST)) {
			throw new IllegalArgumentException("Direction must be one of: " + "SOUTH, NORTH, EAST or WEST");
		}
		return new GenericArrowButton(direction, 17, 17);
	}

	@Override
	protected void installListeners() {
		super.installListeners();
		this.tabPane.addMouseListener(this.mouseListener);
		this.tabPane.addMouseMotionListener(this.mouseListener);
	}

	@Override
	protected void uninstallListeners() {
		super.uninstallListeners();
		this.tabPane.removeMouseListener(this.mouseListener);
		this.tabPane.removeMouseMotionListener(this.mouseListener);
	}

	@Override
	protected void installDefaults() {
		super.installDefaults();
	}

	@Override
	protected MouseListener createMouseListener() {
		return new MouseHandler();
	}

	@Override
	protected Insets getTabInsets(int tabPlacement, int tabIndex) {
		Insets t;
		switch (tabPlacement) {
			case SwingConstants.TOP:
				t = new Insets(1, 8, 1, 8);
				break;
			case SwingConstants.LEFT:
				t = new Insets(3, 12, 2, 12);
				break;
			case SwingConstants.RIGHT:
				t = new Insets(3, 12, 2, 12);
				break;
			case SwingConstants.BOTTOM:
				t = new Insets(1, 8, 1, 8);
				break;
			default:
				t = new Insets(1, 8, 1, 8);
				break;
		}
		return t;
	}

	@Override
	protected Insets getSelectedTabPadInsets(int tabPlacement) {
		Insets t;
		switch (tabPlacement) {
			case SwingConstants.TOP:
				t = new Insets(1, 9, 0, 9);
				break;
			case SwingConstants.LEFT:
				t = new Insets(1, 2, 0, 5);
				break;
			case SwingConstants.RIGHT:
				t = new Insets(1, 5, 0, 2);
				break;
			case SwingConstants.BOTTOM:
				t = new Insets(1, 9, 2, 9);
				break;
			default:
				t = new Insets(1, 9, 0, 9);
				break;
		}
		return t;
	}

	@Override
	protected void paintTabBackground(Graphics g, int tabPlacement, int tabIndex, int x, int y, int w, int h,
			boolean isSelected) {}

	@Override
	protected void paintFocusIndicator(Graphics g, int tabPlacement, Rectangle[] rects, int tabIndex, Rectangle iconRect,
			Rectangle textRect, boolean isSelected) {}

	@Override
	protected void paintTabBorder(Graphics g, int tabPlacement, int tabIndex, int xp, int yp, int mw, int mh,
			boolean isSelected) {
		if (isSelected) {
			paintTabBorderSelected(g, tabPlacement, tabIndex, xp, yp, mw, mh, isSelected);
		} else {
			paintTabBorderFree(g, tabPlacement, tabIndex, xp, yp, mw, mh, isSelected);
		}
	}

	@Override
	protected void paintContentBorder(Graphics g, int tabPlacement, int selectedIndex) {
		int width = this.tabPane.getWidth();
		int height = this.tabPane.getHeight();
		Insets insets = this.tabPane.getInsets();

		int x = insets.left;
		int y = insets.top;
		int w = width - insets.right - insets.left;
		int h = height - insets.top - insets.bottom;

		switch (tabPlacement) {
			case LEFT:
				x += calculateTabAreaWidth(tabPlacement, this.runCount, this.maxTabWidth);
				w -= (x - insets.left);
				break;
			case RIGHT:
				w -= calculateTabAreaWidth(tabPlacement, this.runCount, this.maxTabWidth);
				break;
			case BOTTOM:
				h -= calculateTabAreaHeight(tabPlacement, this.runCount, this.maxTabHeight);
				break;
			case TOP:
			default:
				y += calculateTabAreaHeight(tabPlacement, this.runCount, this.maxTabHeight);
				h -= (y - insets.top);
		}

		g.setColor(RapidLookTools.getColors().getTabbedPaneColors()[0]);
		g.fillRect(x, y, w, h);
	}

	protected void paintTabBorderSelected(Graphics g, int tabPlacement, int tabIndex, int x, int y, int w, int h,
			boolean isSelected) {
		if (tabPlacement == SwingConstants.TOP) {
			paintSelectedTop(g, x, y, w, h);
		} else if (tabPlacement == SwingConstants.LEFT) {
			paintSelectedLeft(g, x, y, w, h);
		} else if (tabPlacement == SwingConstants.RIGHT) {
			paintSelectedRight(g, x, y, w, h);
		} else {
			paintSelectedBottom(g, x, y, w, h);
		}
	}

	private static void paintSelectedRight(Graphics g, int x, int y, int w, int h) {
		g.setColor(RapidLookTools.getColors().getTabbedPaneColors()[2]);
		g.drawLine(x, y + 1, x + w - 11, y + 1);

		g.setColor(RapidLookTools.getColors().getTabbedPaneColors()[3]);
		g.drawLine(x, y, x + w - 15, y);

		ColorUIResource c1 = RapidLookTools.getColors().getTabbedPaneColors()[4];
		g.setColor(c1);

		g.drawLine(w + x - 10, y + 1, w + x - 10, y + 2);
		g.drawLine(w + x - 9, y + 2, w + x - 9, y + 2);
		g.drawLine(w + x - 8, y + 2, w + x - 8, y + 3);
		g.drawLine(w + x - 7, y + 3, w + x - 7, y + 4);
		g.drawLine(w + x - 6, y + 4, w + x - 6, y + 5);

		g.setColor(RapidLookTools.getColors().getTabbedPaneColors()[5]);

		g.drawLine(x, y + 2, x + w - 11, y + 2);
		g.drawLine(x, y + 3, x + w - 9, y + 3);
		g.drawLine(x, y + 4, x + w - 8, y + 4);
		g.drawLine(x, y + 5, x + w - 7, y + 5);

		Graphics2D g2 = (Graphics2D) g;
		g2.setPaint(new GradientPaint(1, y + 6, RapidLookTools.getColors().getTabbedPaneColors()[6], 1, y + h,
				RapidLookTools.getColors().getTabbedPaneColors()[7]));

		int[] xArr = new int[] { x + 4, w + x - 5, w + x - 5, x + 4 };
		int[] yArr = new int[] { y + 6, y + 6, y + h, y + h };
		Polygon p1 = new Polygon(xArr, yArr, 4);

		g2.fillPolygon(p1);

		g.setColor(c1);
		g.drawLine(w + x - 5, y + 6, x + w - 5, y + h - 1);

		g.setColor(RapidLookTools.getColors().getTabbedPaneColors()[1]);
		g.drawLine(x + w - 14, y, x + w - 12, y);
		g.drawLine(w + x - 6, y + 6, x + w - 6, y + 6);
	}

	private static void paintSelectedLeft(Graphics g, int x, int y, int w, int h) {

		g.setColor(RapidLookTools.getColors().getTabbedPaneColors()[2]);
		g.drawLine(x + 10, y + 1, x + w - 1, y + 1);

		g.setColor(RapidLookTools.getColors().getTabbedPaneColors()[3]);
		g.drawLine(x + 11, y, x + w - 1, y);

		ColorUIResource c1 = RapidLookTools.getColors().getTabbedPaneColors()[4];
		g.setColor(c1);

		g.drawLine(x + 9, y + 1, x + 9, y + 2);
		g.drawLine(x + 8, y + 2, x + 8, y + 2);
		g.drawLine(x + 7, y + 2, x + 7, y + 3);
		g.drawLine(x + 6, y + 3, x + 6, y + 4);
		g.drawLine(x + 5, y + 4, x + 5, y + 5);

		g.setColor(RapidLookTools.getColors().getTabbedPaneColors()[5]);

		g.drawLine(x + 10, y + 2, x + w - 5, y + 2);
		g.drawLine(x + 8, y + 3, x + w - 5, y + 3);
		g.drawLine(x + 7, y + 4, x + w - 5, y + 4);
		g.drawLine(x + 6, y + 5, x + w - 5, y + 5);

		Graphics2D g2 = (Graphics2D) g;
		g2.setPaint(new GradientPaint(1, y + 6, RapidLookTools.getColors().getTabbedPaneColors()[6], 1, y + h,
				RapidLookTools.getColors().getTabbedPaneColors()[7]));

		int[] xArr = new int[] { x + 4, w + x - 5, w + x - 5, x + 4 };
		int[] yArr = new int[] { y + 6, y + 6, y + h, y + h };
		Polygon p1 = new Polygon(xArr, yArr, 4);

		g2.fillPolygon(p1);

		g.setColor(c1);
		g.drawLine(x + 4, y + 6, x + 4, y + h - 1);

		g.setColor(RapidLookTools.getColors().getTabbedPaneColors()[1]);
		g.drawLine(x + 11, y, x + 13, y);
		g.drawLine(x + 5, y + 6, x + 5, y + 6);
	}

	private static void paintSelectedTop(Graphics g, int x, int y, int w, int h) {
		g.setColor(RapidLookTools.getColors().getTabbedPaneColors()[1]);
		g.drawLine(x + 11, y, x + w - 12, y);

		g.setColor(RapidLookTools.getColors().getTabbedPaneColors()[2]);
		g.drawLine(x + 10, y + 1, x + w - 11, y + 1);

		g.setColor(RapidLookTools.getColors().getTabbedPaneColors()[3]);
		g.drawLine(x + 13, y, x + w - 14, y);

		ColorUIResource c1 = RapidLookTools.getColors().getTabbedPaneColors()[4];
		g.setColor(c1);

		// left
		g.drawLine(x + 9, y + 1, x + 9, y + 2);
		g.drawLine(x + 8, y + 2, x + 8, y + 2);
		g.drawLine(x + 7, y + 2, x + 7, y + 3);
		g.drawLine(x + 6, y + 3, x + 6, y + 4);
		g.drawLine(x + 5, y + 4, x + 5, y + 5);

		// right
		g.drawLine(w + x - 10, y + 1, w + x - 10, y + 2);
		g.drawLine(w + x - 9, y + 2, w + x - 9, y + 2);
		g.drawLine(w + x - 8, y + 2, w + x - 8, y + 3);
		g.drawLine(w + x - 7, y + 3, w + x - 7, y + 4);
		g.drawLine(w + x - 6, y + 4, w + x - 6, y + 5);

		// inner section
		g.setColor(RapidLookTools.getColors().getTabbedPaneColors()[5]);

		g.drawLine(x + 10, y + 2, x + w - 11, y + 2);
		g.drawLine(x + 8, y + 3, x + w - 9, y + 3);
		g.drawLine(x + 7, y + 4, x + w - 8, y + 4);
		g.drawLine(x + 6, y + 5, x + w - 7, y + 5);

		Graphics2D g2 = (Graphics2D) g;
		g2.setPaint(new GradientPaint(1, y + 6, RapidLookTools.getColors().getTabbedPaneColors()[6], 1, y + h,
				RapidLookTools.getColors().getTabbedPaneColors()[7]));

		int[] xArr = new int[] { x + 4, w + x - 5, x + w - 1, x };
		int[] yArr = new int[] { y + 6, y + 6, y + h, y + h };
		Polygon p1 = new Polygon(xArr, yArr, 4);

		g2.fillPolygon(p1);

		g2.setRenderingHint(RenderingHints.KEY_RENDERING, RenderingHints.VALUE_RENDER_QUALITY);
		g2.setColor(c1);
		g2.drawLine(x + 4, y + 6, x, y + h - 1);
		g2.drawLine(w + x - 5, y + 6, x + w - 1, y + h - 1);
		g2.setRenderingHint(RenderingHints.KEY_RENDERING, RenderingHints.VALUE_RENDER_DEFAULT);

		g.setColor(RapidLookTools.getColors().getTabbedPaneColors()[1]);
		g.drawLine(x + 5, y + 6, x + 5, y + 6);
		g.drawLine(x + w - 6, y + 6, x + w - 6, y + 6);

	}

	private static void paintSelectedBottom(Graphics g, int x, int y, int w, int h) {
		g.setColor(RapidLookTools.getColors().getTabbedPaneColors()[1]);
		g.drawLine(x + 11, y + h - 1, x + w - 12, y + h - 1);
		g.setColor(RapidLookTools.getColors().getTabbedPaneColors()[2]);
		g.drawLine(x + 10, y + h - 2, x + w - 11, y + h - 2);

		g.setColor(RapidLookTools.getColors().getTabbedPaneColors()[3]);
		g.drawLine(x + 13, y + h - 1, x + w - 14, y + h - 1);

		ColorUIResource c1 = RapidLookTools.getColors().getTabbedPaneColors()[4];
		g.setColor(c1);

		// left
		g.drawLine(x + 9, y + h - 2, x + 9, y + h - 3);
		g.drawLine(x + 8, y + h - 3, x + 8, y + h - 3);
		g.drawLine(x + 7, y + h - 3, x + 7, y + h - 4);
		g.drawLine(x + 6, y + h - 4, x + 6, y + h - 5);
		g.drawLine(x + 5, y + h - 5, x + 5, y + h - 6);

		// right
		g.drawLine(w + x - 10, y + h - 2, w + x - 10, y + h - 3);
		g.drawLine(w + x - 9, y + h - 3, w + x - 9, y + h - 3);
		g.drawLine(w + x - 8, y + h - 3, w + x - 8, y + h - 4);
		g.drawLine(w + x - 7, y + h - 4, w + x - 7, y + h - 5);
		g.drawLine(w + x - 6, y + h - 5, w + x - 6, y + h - 6);

		// inner section
		g.setColor(RapidLookTools.getColors().getTabbedPaneColors()[5]);

		g.drawLine(x + 10, y + h - 3, x + w - 11, y + h - 3);
		g.drawLine(x + 8, y + h - 4, x + w - 9, y + h - 4);
		g.drawLine(x + 7, y + h - 5, x + w - 8, y + h - 5);
		g.drawLine(x + 6, y + h - 6, x + w - 7, y + h - 6);

		Graphics2D g2 = (Graphics2D) g;
		g2.setPaint(new GradientPaint(1, y, RapidLookTools.getColors().getTabbedPaneColors()[7], 1, y + h - 6,
				RapidLookTools.getColors().getTabbedPaneColors()[6]));

		int[] xArr = new int[] { x + 4, w + x - 5, x + w - 1, x };
		int[] yArr = new int[] { y + h - 6, y + h - 6, y, y };
		Polygon p1 = new Polygon(xArr, yArr, 4);
		g2.fillPolygon(p1);

		g2.setRenderingHint(RenderingHints.KEY_RENDERING, RenderingHints.VALUE_RENDER_QUALITY);
		g2.setColor(c1);
		g2.drawLine(x, y, x + 4, y + h - 6);
		g2.drawLine(w + x - 1, y, x + w - 5, y + h - 6);
		g2.setRenderingHint(RenderingHints.KEY_RENDERING, RenderingHints.VALUE_RENDER_DEFAULT);
	}

	protected void paintTabBorderFree(Graphics g, int tabPlacement, int tabIndex, int xp, int yp, int mw, int h,
			boolean isSelected) {
		int x = xp + 2;
		int y = yp;
		int w = mw - 4;

		if (tabPlacement == SwingConstants.BOTTOM) {
			g.setColor(RapidLookTools.getColors().getTabbedPaneColors()[8]);
			g.drawLine(x + 4, y + h - 1, x + w - 5, y + h - 1);
			g.drawLine(x, y, x, y + h - 5);
			g.drawLine(x + w - 1, y, x + w - 1, y + h - 4);

			ColorUIResource c1 = RapidLookTools.getColors().getTabbedPaneColors()[9];
			ColorUIResource c2 = RapidLookTools.getColors().getTabbedPaneColors()[20];
			ColorUIResource c3 = RapidLookTools.getColors().getTabbedPaneColors()[10];

			// left
			g.setColor(c3);
			g.drawLine(x + 2, y + h - 1, x, y + h - 3);
			g.setColor(c1);
			g.drawLine(x, y + h - 4, x + 3, y + h - 1);
			g.drawLine(x + 1, y + h - 2, x + 1, y + h - 2);
			g.setColor(c2);
			g.drawLine(x + 3, y + h - 2, x + 1, y + h - 4);

			// right
			g.setColor(c3);
			g.drawLine(x + w - 1, y + h - 3, x + w - 3, y + h - 1);
			g.setColor(c1);
			g.drawLine(x + w - 4, y + h - 1, x + w - 1, y + h - 4);
			g.drawLine(x + w - 2, y + h - 2, x + w - 2, y + h - 2);
			g.setColor(c2);
			g.drawLine(x + w - 4, y + h - 2, x + w - 2, y + h - 4);

			if (tabIndex != this.rolloveredTabIndex) {
				g.setColor(RapidLookTools.getColors().getTabbedPaneColors()[11]);
				g.drawLine(x + 1, y, x + w - 2, y);
				g.setColor(RapidLookTools.getColors().getTabbedPaneColors()[12]);
				g.drawLine(x + 1, y + 1, x + w - 2, y + 1);
				g.setColor(RapidLookTools.getColors().getTabbedPaneColors()[13]);
				g.drawLine(x + 1, y + 2, x + w - 2, y + 2);
				g.setColor(RapidLookTools.getColors().getTabbedPaneColors()[14]);
				g.drawLine(x + 1, y + 3, x + w - 2, y + 3);
				g.setColor(RapidLookTools.getColors().getTabbedPaneColors()[15]);
				g.drawLine(x + 1, y + 4, x + w - 2, y + 4);
				g.setColor(RapidLookTools.getColors().getTabbedPaneColors()[16]);
				g.drawLine(x + 1, y + 5, x + w - 2, y + 5);
				g.setColor(RapidLookTools.getColors().getTabbedPaneColors()[17]);
				g.drawLine(x + 1, y + 6, x + w - 2, y + 6);
				g.setColor(RapidLookTools.getColors().getTabbedPaneColors()[18]);
				g.fillRect(x + 1, y + 7, w - 2, h - 11);
				g.drawLine(x + 2, y + h - 4, x + w - 3, y + h - 4);
				g.drawLine(x + 3, y + h - 3, x + w - 4, y + h - 3);
				g.drawLine(x + 4, y + h - 2, x + w - 5, y + h - 2);
			} else {
				g.setColor(RapidLookTools.getColors().getTabbedPaneColors()[18]);
				g.drawLine(x + 1, y, x + w - 2, y);
				g.setColor(RapidLookTools.getColors().getTabbedPaneColors()[17]);
				g.drawLine(x + 1, y + 1, x + w - 2, y + 1);
				g.drawLine(x + 1, y + 2, x + w - 2, y + 2);
				g.setColor(RapidLookTools.getColors().getTabbedPaneColors()[16]);
				g.drawLine(x + 1, y + 3, x + w - 2, y + 3);
				g.drawLine(x + 1, y + 4, x + w - 2, y + 4);
				g.setColor(RapidLookTools.getColors().getTabbedPaneColors()[15]);
				g.drawLine(x + 1, y + 5, x + w - 2, y + 5);
				g.drawLine(x + 1, y + 6, x + w - 2, y + 6);
				g.setColor(RapidLookTools.getColors().getTabbedPaneColors()[14]);
				g.fillRect(x + 1, y + 7, w - 2, h - 11);
				g.drawLine(x + 2, y + h - 4, x + w - 3, y + h - 4);
				g.drawLine(x + 3, y + h - 3, x + w - 4, y + h - 3);
				g.drawLine(x + 4, y + h - 2, x + w - 5, y + h - 2);
			}
		} else if (tabPlacement == SwingConstants.RIGHT) {
			x -= 2;
			g.setColor(RapidLookTools.getColors().getTabbedPaneColors()[8]);
			g.drawLine(x, y, x + w - 5, y);
			g.drawLine(x + w - 1, y + 4, x + w - 1, y + h - 1);

			ColorUIResource c1 = RapidLookTools.getColors().getTabbedPaneColors()[9];
			ColorUIResource c2 = RapidLookTools.getColors().getTabbedPaneColors()[19];
			ColorUIResource c3 = RapidLookTools.getColors().getTabbedPaneColors()[10];

			// right
			g.setColor(c3);
			g.drawLine(x + w - 1, y + 2, x + w - 3, y);
			g.setColor(c1);
			g.drawLine(x + w - 4, y, x + w - 1, y + 3);
			g.drawLine(x + w - 2, y + 1, x + w - 2, y + 1);
			g.setColor(c2);
			g.drawLine(x + w - 4, y + 1, x + w - 2, y + 3);

			if (tabIndex != this.rolloveredTabIndex) {
				g.setColor(RapidLookTools.getColors().getTabbedPaneColors()[11]);
				g.drawLine(x, y + 1, x + w - 5, y + 1);
				g.setColor(RapidLookTools.getColors().getTabbedPaneColors()[12]);
				g.drawLine(x, y + 2, x + w - 4, y + 2);
				g.setColor(RapidLookTools.getColors().getTabbedPaneColors()[13]);
				g.drawLine(x, y + 3, x + w - 3, y + 3);
				g.setColor(RapidLookTools.getColors().getTabbedPaneColors()[14]);
				g.drawLine(x, y + 4, x + w - 2, y + 4);
				g.setColor(RapidLookTools.getColors().getTabbedPaneColors()[15]);
				g.drawLine(x, y + 5, x + w - 2, y + 5);
				g.setColor(RapidLookTools.getColors().getTabbedPaneColors()[16]);
				g.drawLine(x, y + 6, x + w - 2, y + 6);
				g.setColor(RapidLookTools.getColors().getTabbedPaneColors()[17]);
				g.drawLine(x, y + 7, x + w - 2, y + 7);
				g.setColor(RapidLookTools.getColors().getTabbedPaneColors()[18]);
				g.fillRect(x, y + 8, w - 1, h - 8);
			} else {
				g.setColor(RapidLookTools.getColors().getTabbedPaneColors()[14]);
				g.drawLine(x, y + 1, x + w - 5, y + 1);
				g.setColor(RapidLookTools.getColors().getTabbedPaneColors()[18]);
				g.drawLine(x, y + 2, x + w - 4, y + 2);
				g.setColor(RapidLookTools.getColors().getTabbedPaneColors()[18]);
				g.drawLine(x, y + 3, x + w - 3, y + 3);
				g.setColor(RapidLookTools.getColors().getTabbedPaneColors()[17]);
				g.drawLine(x, y + 4, x + w - 2, y + 4);
				g.setColor(RapidLookTools.getColors().getTabbedPaneColors()[17]);
				g.drawLine(x, y + 5, x + w - 2, y + 5);
				g.setColor(RapidLookTools.getColors().getTabbedPaneColors()[16]);
				g.drawLine(x, y + 6, x + w - 2, y + 6);
				g.setColor(RapidLookTools.getColors().getTabbedPaneColors()[16]);
				g.drawLine(x, y + 7, x + w - 2, y + 7);
				g.setColor(RapidLookTools.getColors().getTabbedPaneColors()[15]);
				g.fillRect(x, y + 8, w - 1, h - 8);
			}
		} else if (tabPlacement == SwingConstants.LEFT) {
			x += 2;
			g.setColor(RapidLookTools.getColors().getTabbedPaneColors()[8]);
			g.drawLine(x + 4, y, x + w - 1, y);
			g.drawLine(x, y + 4, x, y + h - 1);

			ColorUIResource c1 = RapidLookTools.getColors().getTabbedPaneColors()[9];
			ColorUIResource c2 = RapidLookTools.getColors().getTabbedPaneColors()[19];
			ColorUIResource c3 = RapidLookTools.getColors().getTabbedPaneColors()[10];

			// left
			g.setColor(c3);
			g.drawLine(x + 2, y, x, y + 2);
			g.setColor(c1);
			g.drawLine(x, y + 3, x + 3, y);
			g.drawLine(x + 1, y + 1, x + 1, y + 1);
			g.setColor(c2);
			g.drawLine(x + 3, y + 1, x + 1, y + 3);

			if (tabIndex != this.rolloveredTabIndex) {
				g.setColor(RapidLookTools.getColors().getTabbedPaneColors()[11]);
				g.drawLine(x + 4, y + 1, x + w - 1, y + 1);
				g.setColor(RapidLookTools.getColors().getTabbedPaneColors()[12]);
				g.drawLine(x + 3, y + 2, x + w - 1, y + 2);
				g.setColor(RapidLookTools.getColors().getTabbedPaneColors()[13]);
				g.drawLine(x + 2, y + 3, x + w - 1, y + 3);
				g.setColor(RapidLookTools.getColors().getTabbedPaneColors()[14]);
				g.drawLine(x + 1, y + 4, x + w - 1, y + 4);
				g.setColor(RapidLookTools.getColors().getTabbedPaneColors()[15]);
				g.drawLine(x + 1, y + 5, x + w - 1, y + 5);
				g.setColor(RapidLookTools.getColors().getTabbedPaneColors()[16]);
				g.drawLine(x + 1, y + 6, x + w - 1, y + 6);
				g.setColor(RapidLookTools.getColors().getTabbedPaneColors()[17]);
				g.drawLine(x + 1, y + 7, x + w - 1, y + 7);
				g.setColor(RapidLookTools.getColors().getTabbedPaneColors()[18]);
				g.fillRect(x + 1, y + 8, w - 1, h - 8);
			} else {
				g.setColor(RapidLookTools.getColors().getTabbedPaneColors()[14]);
				g.drawLine(x + 4, y + 1, x + w - 1, y + 1);
				g.setColor(RapidLookTools.getColors().getTabbedPaneColors()[18]);
				g.drawLine(x + 3, y + 2, x + w - 1, y + 2);
				g.setColor(RapidLookTools.getColors().getTabbedPaneColors()[18]);
				g.drawLine(x + 2, y + 3, x + w - 1, y + 3);
				g.setColor(RapidLookTools.getColors().getTabbedPaneColors()[17]);
				g.drawLine(x + 1, y + 4, x + w - 1, y + 4);
				g.setColor(RapidLookTools.getColors().getTabbedPaneColors()[17]);
				g.drawLine(x + 1, y + 5, x + w - 1, y + 5);
				g.setColor(RapidLookTools.getColors().getTabbedPaneColors()[16]);
				g.drawLine(x + 1, y + 6, x + w - 1, y + 6);
				g.setColor(RapidLookTools.getColors().getTabbedPaneColors()[16]);
				g.drawLine(x + 1, y + 7, x + w - 1, y + 7);
				g.setColor(RapidLookTools.getColors().getTabbedPaneColors()[15]);
				g.fillRect(x + 1, y + 8, w - 1, h - 8);
			}
		} else { // top
			g.setColor(RapidLookTools.getColors().getTabbedPaneColors()[8]);

			g.drawLine(x + 4, y, x + w - 5, y);
			g.drawLine(x, y + 4, x, y + h - 1);
			g.drawLine(x + w - 1, y + 4, x + w - 1, y + h - 1);

			ColorUIResource c1 = RapidLookTools.getColors().getTabbedPaneColors()[9];
			ColorUIResource c2 = RapidLookTools.getColors().getTabbedPaneColors()[19];
			ColorUIResource c3 = RapidLookTools.getColors().getTabbedPaneColors()[10];

			// left
			g.setColor(c3);
			g.drawLine(x + 2, y, x, y + 2);
			g.setColor(c1);
			g.drawLine(x, y + 3, x + 3, y);
			g.drawLine(x + 1, y + 1, x + 1, y + 1);
			g.setColor(c2);
			g.drawLine(x + 3, y + 1, x + 1, y + 3);

			// right
			g.setColor(c3);
			g.drawLine(x + w - 1, y + 2, x + w - 3, y);
			g.setColor(c1);
			g.drawLine(x + w - 4, y, x + w - 1, y + 3);
			g.drawLine(x + w - 2, y + 1, x + w - 2, y + 1);
			g.setColor(c2);
			g.drawLine(x + w - 4, y + 1, x + w - 2, y + 3);

			if (tabIndex != this.rolloveredTabIndex) {
				g.setColor(RapidLookTools.getColors().getTabbedPaneColors()[11]);
				g.drawLine(x + 4, y + 1, x + w - 5, y + 1);
				g.setColor(RapidLookTools.getColors().getTabbedPaneColors()[12]);
				g.drawLine(x + 3, y + 2, x + w - 4, y + 2);
				g.setColor(RapidLookTools.getColors().getTabbedPaneColors()[13]);
				g.drawLine(x + 2, y + 3, x + w - 3, y + 3);
				g.setColor(RapidLookTools.getColors().getTabbedPaneColors()[14]);
				g.drawLine(x + 1, y + 4, x + w - 2, y + 4);
				g.setColor(RapidLookTools.getColors().getTabbedPaneColors()[15]);
				g.drawLine(x + 1, y + 5, x + w - 2, y + 5);
				g.setColor(RapidLookTools.getColors().getTabbedPaneColors()[16]);
				g.drawLine(x + 1, y + 6, x + w - 2, y + 6);
				g.setColor(RapidLookTools.getColors().getTabbedPaneColors()[17]);
				g.drawLine(x + 1, y + 7, x + w - 2, y + 7);
				g.setColor(RapidLookTools.getColors().getTabbedPaneColors()[18]);
				g.fillRect(x + 1, y + 8, w - 2, h - 8);
			} else {
				g.setColor(RapidLookTools.getColors().getTabbedPaneColors()[14]);
				g.drawLine(x + 4, y + 1, x + w - 5, y + 1);
				g.setColor(RapidLookTools.getColors().getTabbedPaneColors()[18]);
				g.drawLine(x + 3, y + 2, x + w - 4, y + 2);
				g.setColor(RapidLookTools.getColors().getTabbedPaneColors()[18]);
				g.drawLine(x + 2, y + 3, x + w - 3, y + 3);

				// highlight
				g.setColor(RapidLookTools.getColors().getTabbedPaneColors()[21]);
				g.drawLine(x + 3, y + 1, x + w - 4, y + 1);

				g.setColor(RapidLookTools.getColors().getTabbedPaneColors()[17]);
				g.drawLine(x + 1, y + 4, x + w - 2, y + 4);
				g.setColor(RapidLookTools.getColors().getTabbedPaneColors()[17]);
				g.drawLine(x + 1, y + 5, x + w - 2, y + 5);
				g.setColor(RapidLookTools.getColors().getTabbedPaneColors()[16]);
				g.drawLine(x + 1, y + 6, x + w - 2, y + 6);
				g.setColor(RapidLookTools.getColors().getTabbedPaneColors()[16]);
				g.drawLine(x + 1, y + 7, x + w - 2, y + 7);
				g.setColor(RapidLookTools.getColors().getTabbedPaneColors()[15]);
				g.fillRect(x + 1, y + 8, w - 2, h - 8);
			}
		}
	}

	@Override
	protected int getTabLabelShiftY(int tabPlacement, int tabIndex, boolean isSelected) {
		if (isSelected) {
			return 1;
		} else {
			return 0;
		}
	}

	protected void updateMouseOver(Point p) {
		int roi = tabForCoordinate(this.tabPane, (int) p.getX(), (int) p.getY());
		if (this.rolloveredTabIndex != roi) {
			this.rolloveredTabIndex = roi;
			this.tabPane.repaint();
		}
	}

	@Override
	public Insets getContentBorderInsets(int tabPlacement) {
		return new Insets(1, 1, 1, 1);
		// return new Insets(0,0,0,0);
	}

	@Override
	protected void paintText(Graphics g, int tabPlacement, Font font, FontMetrics metrics, int tabIndex, String title,
			Rectangle textRect, boolean isSelected) {
		// otherwise the tabs text would not have AA for some reason even though the rest of the
		// components has AA without this
		((Graphics2D) g).setRenderingHint(RenderingHints.KEY_TEXT_ANTIALIASING, RenderingHints.VALUE_TEXT_ANTIALIAS_ON);
		super.paintText(g, tabPlacement, font, metrics, tabIndex, title, textRect, isSelected);
	}
}
